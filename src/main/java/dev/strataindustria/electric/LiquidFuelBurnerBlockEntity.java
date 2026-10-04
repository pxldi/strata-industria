package dev.strataindustria.electric;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.fluid.FluidPort;
import dev.strataindustria.heat.Heat;
import dev.strataindustria.heat.HeatEmitter;
import dev.strataindustria.heat.HeatNetwork;
import dev.strataindustria.heat.HeatPipeBlock;
import dev.strataindustria.heat.HeatPort;
import dev.strataindustria.ironworks.AirBlast;
import dev.strataindustria.power.LiquidFuel;
import dev.strataindustria.registry.Tier4Sounds;
import dev.strataindustria.registry.Tier5BlockEntities;
import dev.strataindustria.registry.Tier5Sounds;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Spec 7.5: a firebox that burns a fluid from the {@code liquid_fuel} data map (creosote: 16 HU per mB, up to
 * 1350 °C, 30 HU/t) out of a 4000 mB tank. It offers heat the way a firebox does and rises and falls at
 * 5 °C a second, takes a blower for 150 °C more and half again the output, but unlike a firebox it burns
 * only what its consumers take: with no demand the fuel stays in the tank ("Idle: no heat demand").
 */
public class LiquidFuelBurnerBlockEntity extends BlockEntity implements HeatPort, FluidPort, GeneratorBlock.Generator {
    public static final int CAPACITY = 4000, BUCKET = 1000;
    /** 5 °C a second, per tick. */
    public static final float RATE = 5.0f / 20.0f;
    /** What a blower adds to the fuel's maximum, as on the firebox. */
    public static final int BLOWER_TEMPERATURE = 150;
    private static final int SOUND_INTERVAL = 60;

    private final HeatEmitter emitter = new HeatEmitter();
    private Fluid fluid = Fluids.EMPTY;
    private int amount;
    /** HU left over from the last mB burnt. */
    private double bank;
    private float temperature = Heat.AMBIENT;
    private boolean burning;
    private boolean blown;
    private int taken;
    private int age;

    public LiquidFuelBurnerBlockEntity(BlockPos pos, BlockState state) {
        super(Tier5BlockEntities.LIQUID_FUEL_BURNER.get(), pos, state);
    }

    public Fluid fluid() {
        return amount > 0 ? fluid : Fluids.EMPTY;
    }

    public int amount() {
        return amount;
    }

    public float temperature() {
        return temperature;
    }

    /** HU/t its consumers took last tick. */
    public int taken() {
        return taken;
    }

    public boolean burning() {
        return burning;
    }

    private LiquidFuel.@Nullable Burn burn() {
        return amount > 0 ? LiquidFuel.burnOf(fluid) : null;
    }

    /** HU left in the tank and the burn in progress. */
    private double available() {
        LiquidFuel.Burn burn = burn();
        return bank + (burn == null ? 0 : amount * burn.huPerMb());
    }

    // ------------------------------------------------------------------ fuel

    @Override
    public boolean connectsFluid(Direction side) {
        return true;
    }

    @Override
    public int fill(Direction side, Fluid fluid, int amount, float pressure, boolean simulate) {
        if (LiquidFuel.burnOf(fluid) == null || this.amount > 0 && !this.fluid.isSame(fluid)) return 0;
        int take = Math.max(0, Math.min(amount, CAPACITY - this.amount));
        if (!simulate && take > 0) {
            this.fluid = fluid;
            this.amount += take;
            setChanged();
        }
        return take;
    }

    @Override
    public InteractionResult useBucket(Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack) {
        @Nullable Fluid carried = LiquidFuel.fluidOfBucket(stack);
        if (carried == null || fill(Direction.UP, carried, BUCKET, 0, true) < BUCKET) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            fill(Direction.UP, carried, BUCKET, 0, false);
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
            level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
        }
        return InteractionResult.SUCCESS;
    }

    // ------------------------------------------------------------------ heat

    @Override
    public boolean connectsHeat(Direction side) {
        return true;
    }

    @Override
    public void tick(ServerLevel level, BlockPos pos, BlockState state) {
        age++;
        blown = blower(level, pos);
        LiquidFuel.Burn burn = burn();
        emitter.refresh(level, pos);
        List<HeatNetwork.Route> targets = emitter.targets(level, pos);
        float max = burn == null ? Heat.AMBIENT : burn.maxTemperature() + (blown ? BLOWER_TEMPERATURE : 0);
        boolean wanting = burn != null && available() > 0 && emitter.wants(level, targets, max);
        if (wanting && !burning) {
            level.playSound(null, pos, Tier4Sounds.FIREBOX_LIGHT.get(), SoundSource.BLOCKS, 0.7f, 1.1f);
        }
        burning = wanting;
        if (wanting) {
            temperature = temperature > max ? Math.max(max, temperature - RATE) : Math.min(max, temperature + RATE);
        } else {
            temperature = Math.max(Heat.AMBIENT, temperature - RATE);
        }

        taken = 0;
        if (burn != null && temperature > Heat.AMBIENT + 1) {
            int rating = blown ? burn.huPerTick() * 3 / 2 : burn.huPerTick();
            int output = wanting ? (int) Math.min(rating, available()) : 0;
            taken = Math.max(0, Math.min(output, HeatNetwork.deliver(level, targets, temperature, output)));
            burnUp(burn, taken);
        } else if (burn == null && temperature > Heat.AMBIENT + 1) {
            HeatNetwork.deliver(level, targets, temperature, 0);
        }
        emitter.glow(level, temperature >= HeatPipeBlock.GLOWS_FROM && burning);
        if (burning && age % SOUND_INTERVAL == 0) {
            level.playSound(null, pos, Tier5Sounds.LIQUID_FUEL_BURNER_RUN.get(), SoundSource.BLOCKS, 0.5f, blown ? 1.15f : 1.0f);
        }
        if (state.getValue(LiquidFuelBurnerBlock.LIT) != burning) {
            level.setBlock(pos, state.setValue(LiquidFuelBurnerBlock.LIT, burning), Block.UPDATE_ALL);
        }
        if (burning || taken > 0) setChanged();
    }

    /** Burns whole mB for {@code heat} HU, keeping the rest of each one in the bank for the next tick. */
    private void burnUp(LiquidFuel.Burn burn, double heat) {
        if (heat <= 0) return;
        while (bank < heat && amount > 0) {
            amount--;
            bank += burn.huPerMb();
        }
        bank = Math.max(0, bank - heat);
        if (amount == 0) fluid = Fluids.EMPTY;
    }

    /** Whether a running blower blows into any face (spec 7.5). */
    private static boolean blower(Level level, BlockPos pos) {
        for (Direction side : Direction.values()) {
            if (level.getBlockEntity(pos.relative(side)) instanceof AirBlast blast && blast.airOut(side.getOpposite()) > 0) return true;
        }
        return false;
    }

    @Override
    public void animate(Level level, BlockPos pos, BlockState state, RandomSource random) {
        if (!state.getValue(LiquidFuelBurnerBlock.LIT)) return;
        Direction front = state.getValue(LiquidFuelBurnerBlock.FACING);
        double x = pos.getX() + 0.5 + front.getStepX() * 0.52, z = pos.getZ() + 0.5 + front.getStepZ() * 0.52;
        double across = (random.nextDouble() - 0.5) * 0.3;
        x += front.getStepZ() * across;
        z += front.getStepX() * across;
        if (random.nextInt(3) == 0) level.addParticle(ParticleTypes.SMALL_FLAME, x, pos.getY() + 0.45, z, 0, 0.004, 0);
        if (random.nextInt(8) == 0) level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.02, pos.getZ() + 0.5, 0, 0.02, 0);
    }

    @Override
    public Component readout() {
        String key = StrataIndustria.MOD_ID + ".liquid_fuel_burner.";
        if (amount <= 0 && !burning && temperature <= Heat.AMBIENT + 1) return Component.translatable(key + "empty");
        Component tank = amount > 0 ? fluid.getFluidType().getDescription() : Component.translatable(key + "no_fuel");
        if (!burning) return Component.translatable(key + "idle", tank, amount, CAPACITY, Math.round(temperature));
        return Component.translatable(key + "burning", tank, amount, CAPACITY, Math.round(temperature), taken);
    }

    // ------------------------------------------------------------------ lifecycle

    /** The pipes it was heating cool. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) emitter.release(level);
    }

    /** For game tests: start at this temperature without waiting for the climb. */
    public void preheat(float temperature) {
        this.temperature = temperature;
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        amount = in.getIntOr("amount", 0);
        bank = in.getDoubleOr("bank", 0.0);
        temperature = in.getFloatOr("temperature", Heat.AMBIENT);
        fluid = in.read("fluid", BuiltInRegistries.FLUID.byNameCodec()).orElse(Fluids.EMPTY);
        if (fluid.isSame(Fluids.EMPTY)) amount = 0;
        emitter.setPipesHot(in.getBooleanOr("pipes_hot", false));
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("amount", amount);
        out.putDouble("bank", bank);
        out.putFloat("temperature", temperature);
        if (amount > 0) out.store("fluid", BuiltInRegistries.FLUID.byNameCodec(), fluid);
        out.putBoolean("pipes_hot", emitter.pipesHot());
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
