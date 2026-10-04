package dev.strataindustria.electric;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.fluid.FluidPort;
import dev.strataindustria.power.ElectricNetwork;
import dev.strataindustria.power.ElectricNetworks;
import dev.strataindustria.power.ElectricSource;
import dev.strataindustria.power.ElectricTier;
import dev.strataindustria.power.LiquidFuel;
import dev.strataindustria.power.StatusLight;
import dev.strataindustria.registry.Tier5BlockEntities;
import dev.strataindustria.registry.Tier5Sounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
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
 * Spec 7.3: burns a fluid from the {@code liquid_fuel} data map (creosote 8 J per mB) in an 8000 mB tank.
 * LV gives up to 32 J/t, MV up to 128 J/t, and it burns only what the network takes. Fuel comes in on any
 * face from a pipe, or by bucket.
 */
public class CombustionGeneratorBlockEntity extends BlockEntity implements ElectricSource, FluidPort, GeneratorBlock.Generator {
    public static final int CAPACITY = 8000, BUCKET = 1000;
    /** Ticks the flame stays lit after the last burn, so the front does not flicker on a bursty load. */
    private static final int AFTERGLOW = 10;
    private static final int SOUND_INTERVAL = 60;

    private Fluid fluid = Fluids.EMPTY;
    private int amount;
    /** J left over from the last mB burnt. */
    private double charge;
    private double produced;
    private int glow;
    private boolean lit;
    private int age;

    public CombustionGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(Tier5BlockEntities.COMBUSTION_GENERATOR.get(), pos, state);
    }

    @Override
    public ElectricTier tier() {
        return getBlockState().getValue(GeneratorBlock.TIER);
    }

    public Fluid fluid() {
        return amount > 0 ? fluid : Fluids.EMPTY;
    }

    public int amount() {
        return amount;
    }

    /** J left in the tank and the burn in progress. */
    private double available() {
        LiquidFuel fuel = amount > 0 ? LiquidFuel.of(fluid) : null;
        return charge + (fuel == null ? 0 : amount * fuel.joulesPerMb());
    }

    // ------------------------------------------------------------------ fuel

    @Override
    public boolean connectsFluid(Direction side) {
        return true;
    }

    @Override
    public int fill(Direction side, Fluid fluid, int amount, float pressure, boolean simulate) {
        if (LiquidFuel.of(fluid) == null || this.amount > 0 && !this.fluid.isSame(fluid)) return 0;
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

    // ------------------------------------------------------------------ power

    @Override
    public boolean connectsElectric(Direction side) {
        return true;
    }

    @Override
    public double maxOutput() {
        return Math.min(tier().maxPower(), available());
    }

    @Override
    public void extract(double amount) {
        produced = amount;
        if (amount <= 0) return;
        LiquidFuel fuel = LiquidFuel.of(fluid);
        // Burn whole mB, keeping the rest of each one as charge for the next tick.
        while (charge < amount && this.amount > 0 && fuel != null) {
            this.amount--;
            charge += fuel.joulesPerMb();
        }
        charge = Math.max(0, charge - amount);
        if (this.amount == 0) fluid = Fluids.EMPTY;
        setChanged();
    }

    @Override
    public void tick(ServerLevel level, BlockPos pos, BlockState state) {
        age++;
        if (produced > 0) {
            if (glow == 0) level.playSound(null, pos, Tier5Sounds.COMBUSTION_GENERATOR_IGNITE.get(), SoundSource.BLOCKS, 0.7f, tier() == ElectricTier.MV ? 1.1f : 1.0f);
            glow = AFTERGLOW;
        } else if (glow > 0) {
            glow--;
        }
        if (glow > 0 && age % SOUND_INTERVAL == 0) {
            level.playSound(null, pos, Tier5Sounds.COMBUSTION_GENERATOR_RUN.get(), SoundSource.BLOCKS, 0.45f, tier() == ElectricTier.MV ? 1.1f : 1.0f);
        }
        ElectricNetwork.Report report = ElectricNetworks.report(level, pos);
        StatusLight light;
        if (report.status().fault()) light = StatusLight.ERROR;
        else if (available() <= 0 && glow == 0) light = StatusLight.OFF;
        else light = produced > 0 ? StatusLight.RUN : StatusLight.WAIT;
        produced = 0;
        BlockState next = state.setValue(GeneratorBlock.STATUS, light).setValue(GeneratorBlock.ACTIVE, glow > 0);
        if (next != state) level.setBlock(pos, next, Block.UPDATE_CLIENTS);
    }

    @Override
    public void animate(Level level, BlockPos pos, BlockState state, RandomSource random) {
        if (random.nextInt(4) != 0) return;
        level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.2, pos.getY() + 1.0,
                pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.2, 0.0, 0.03, 0.0);
    }

    @Override
    public Component readout() {
        String key = StrataIndustria.MOD_ID + ".combustion_generator.";
        if (amount <= 0) return Component.translatable(key + "empty");
        return Component.translatable(key + "tank", fluid.getFluidType().getDescription(), amount, CAPACITY);
    }

    // ------------------------------------------------------------------ lifecycle

    @Override
    public void onLoad() {
        super.onLoad();
        ElectricNetworks.markDirty(level, worldPosition);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        ElectricNetworks.markDirty(level, worldPosition);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        amount = in.getIntOr("amount", 0);
        charge = in.getDoubleOr("charge", 0.0);
        fluid = in.read("fluid", BuiltInRegistries.FLUID.byNameCodec()).orElse(Fluids.EMPTY);
        if (fluid.isSame(Fluids.EMPTY)) amount = 0;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("amount", amount);
        out.putDouble("charge", charge);
        if (amount > 0) out.store("fluid", BuiltInRegistries.FLUID.byNameCodec(), fluid);
    }
}
