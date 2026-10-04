package dev.strataindustria.oil;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.fluid.FluidBuckets;
import dev.strataindustria.fluid.FluidPipes;
import dev.strataindustria.fluid.FluidPort;
import dev.strataindustria.heat.HeatConsumer;
import dev.strataindustria.heat.HeatIntake;
import dev.strataindustria.heat.HeatPipeBlock;
import dev.strataindustria.heat.HeatPort;
import dev.strataindustria.processing.OilStillRecipe;
import dev.strataindustria.registry.Tier6BlockEntities;
import dev.strataindustria.registry.Tier6Fluids;
import dev.strataindustria.registry.Tier6Items;
import dev.strataindustria.registry.Tier6Recipes;
import dev.strataindustria.registry.Tier6Sounds;
import dev.strataindustria.tanning.FluidAmount;
import java.util.List;
import java.util.Locale;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
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
 * The oil still (tier 6 spec 5.5): a batch still on any heat source. One crude tank takes buckets or pipes on
 * any face; the three product tanks give naphtha, diesel and heavy oil to pipes and tanks on the faces the
 * wrench leaves open. A batch takes 1200 ticks at full heat; the gas it makes vents as a puff.
 */
public class OilStillBlockEntity extends BlockEntity implements MenuProvider, HeatConsumer, HeatPort, FluidPort {
    public static final int MIN_TEMPERATURE = 400, HEAT = 30, CAPACITY = 4000, BUCKET = 1000;
    public static final int PRODUCTS = 3, TANKS = PRODUCTS + 1, PUSH_TICKS = 4, PUSH_AMOUNT = 250;
    /** The products leave warm; the pipes have to take it. */
    public static final float PRODUCT_TEMPERATURE = 60.0f;
    public static final int DATA_STATUS = 0, DATA_TEMPERATURE = 1, DATA_HEAT = 2, DATA_LIMIT = 3, DATA_PROGRESS = 4,
            DATA_AMOUNT = 5, DATA_FLUID = DATA_AMOUNT + TANKS, DATA_COUNT = DATA_FLUID + TANKS;

    public enum Status {
        EMPTY, NO_RECIPE, TANK_FULL, NEEDS_HEAT, DISTILLING;

        public String key() {
            return StrataIndustria.MOD_ID + ".oil_still.status." + name().toLowerCase(Locale.ROOT);
        }
    }

    private final HeatIntake intake = new HeatIntake(MIN_TEMPERATURE, HEAT);
    /** Tank 0 takes crude, tanks 1 to 3 hold the products in the order of the recipe's results. */
    private final Fluid[] fluid = new Fluid[TANKS];
    private final int[] amount = new int[TANKS];
    /** Per face: 0 = auto, 1 to 3 = only that product leaves here, 4 = nothing leaves (spec 5.5). */
    private final int[] faceMode = new int[6];
    private float progress;
    private int needed;
    private int batches;
    private Status status = Status.EMPTY;
    private Fluid fullFluid = Fluids.EMPTY;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            if (index >= DATA_AMOUNT && index < DATA_FLUID) return amount[index - DATA_AMOUNT];
            if (index >= DATA_FLUID && index < DATA_COUNT) {
                int tank = index - DATA_FLUID;
                return amount[tank] > 0 ? BuiltInRegistries.FLUID.getId(fluid[tank]) : 0;
            }
            return switch (index) {
                case DATA_STATUS -> status.ordinal();
                case DATA_TEMPERATURE -> Math.round(intake.temperature());
                case DATA_HEAT -> intake.heat();
                case DATA_LIMIT -> intake.limit();
                case DATA_PROGRESS -> needed <= 0 ? 0 : Math.round(Math.min(1.0f, progress / needed) * 1000);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public OilStillBlockEntity(BlockPos pos, BlockState state) {
        super(Tier6BlockEntities.OIL_STILL.get(), pos, state);
        java.util.Arrays.fill(fluid, Fluids.EMPTY);
    }

    // ------------------------------------------------------------------ tick

    public static void serverTick(Level level, BlockPos pos, BlockState state, OilStillBlockEntity still) {
        ServerLevel server = (ServerLevel) level;
        still.intake.roll();
        Status before = still.status;
        still.status = still.work(server, pos);
        if (still.status != before || still.status == Status.DISTILLING) still.setChanged();
        if (server.getGameTime() % PUSH_TICKS == 0) still.pushProducts(server, state);
        boolean lit = still.status == Status.DISTILLING;
        if (state.getValue(OilStillBlock.LIT) != lit) level.setBlock(pos, state.setValue(OilStillBlock.LIT, lit), Block.UPDATE_ALL);
    }

    private Status work(ServerLevel level, BlockPos pos) {
        needed = 0;
        fullFluid = Fluids.EMPTY;
        OilStillRecipe recipe = recipe(level, fluid[0], amount[0]);
        if (recipe == null) {
            progress = 0;
            return amount[0] > 0 ? Status.NO_RECIPE : Status.EMPTY;
        }
        needed = recipe.ticks();
        if (!fits(recipe)) return Status.TANK_FULL;
        if (intake.heat() <= 0 || intake.temperature() < recipe.minTemperature()) return Status.NEEDS_HEAT;
        // A short supply of heat distils more slowly.
        progress += intake.share();
        if (level.getGameTime() % 40 == 0) {
            level.playSound(null, pos, Tier6Sounds.OIL_STILL_BOIL.get(), SoundSource.BLOCKS, 0.5f, 0.9f + level.getRandom().nextFloat() * 0.2f);
        }
        if (progress >= recipe.ticks()) {
            progress = 0;
            finish(level, pos, recipe);
        }
        return Status.DISTILLING;
    }

    private void finish(ServerLevel level, BlockPos pos, OilStillRecipe recipe) {
        amount[0] -= recipe.input().amount();
        if (amount[0] <= 0) {
            amount[0] = 0;
            fluid[0] = Fluids.EMPTY;
        }
        for (int i = 0; i < recipe.results().size(); i++) {
            FluidAmount result = recipe.results().get(i);
            fluid[1 + i] = result.fluid();
            amount[1 + i] += result.amount();
        }
        batches++;
        if (recipe.vented() > 0) {
            // The gas that is not kept goes up as a grey puff off the gooseneck.
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 3, 0.15, 0.05, 0.15, 0.01);
            level.playSound(null, pos, Tier6Sounds.OIL_STILL_VENT.get(), SoundSource.BLOCKS, 0.6f, 1.0f);
        }
        level.gameEvent(null, GameEvent.BLOCK_CHANGE, pos);
    }

    /** The recipe that distils {@code what}, when {@code mb} of it is in the tank. */
    private static @Nullable OilStillRecipe recipe(ServerLevel level, Fluid what, int mb) {
        if (mb <= 0) return null;
        for (RecipeHolder<OilStillRecipe> holder : level.recipeAccess().recipeMap().byType(Tier6Recipes.OIL_STILL.get())) {
            OilStillRecipe recipe = holder.value();
            if (recipe.input().fluid().isSame(what) && mb >= recipe.input().amount()) return recipe;
        }
        return null;
    }

    /** Whether the products of {@code recipe} fit; otherwise {@link #fullFluid} names the one that does not. */
    private boolean fits(OilStillRecipe recipe) {
        List<FluidAmount> results = recipe.results();
        for (int i = 0; i < results.size() && i < PRODUCTS; i++) {
            FluidAmount result = results.get(i);
            int tank = 1 + i;
            if (amount[tank] > 0 && !fluid[tank].isSame(result.fluid()) || amount[tank] + result.amount() > CAPACITY) {
                fullFluid = result.fluid();
                return false;
            }
        }
        return true;
    }

    /** Pushes each product into any pipe network or tank on a face that takes it. */
    private void pushProducts(ServerLevel level, BlockState state) {
        for (int product = 0; product < PRODUCTS; product++) {
            int tank = 1 + product;
            for (Direction face : Direction.values()) {
                if (amount[tank] <= 0) break;
                if (!faceTakes(face, product)) continue;
                FluidPipes.Network network = FluidPipes.find(level, worldPosition, face);
                if (network.isEmpty()) continue;
                int moved = FluidPipes.push(level, network, fluid[tank], Math.min(amount[tank], PUSH_AMOUNT), PRODUCT_TEMPERATURE, 0).moved();
                if (moved <= 0) continue;
                amount[tank] -= moved;
                if (amount[tank] <= 0) {
                    amount[tank] = 0;
                    fluid[tank] = Fluids.EMPTY;
                }
                setChanged();
            }
        }
    }

    // ------------------------------------------------------------------ state

    public Status status() {
        return status;
    }

    /** The product that has no room, while the still is stopped by a full tank. */
    public Fluid fullFluid() {
        return fullFluid;
    }

    /** Batches finished since it was placed. */
    public int batches() {
        return batches;
    }

    public Fluid fluid(int tank) {
        return amount[tank] > 0 ? fluid[tank] : Fluids.EMPTY;
    }

    public int amount(int tank) {
        return amount[tank];
    }

    /** The heat that came in last tick, for tests. */
    public HeatIntake intake() {
        return intake;
    }

    /** Fills a tank directly, for game tests. */
    public void setTank(int tank, Fluid fluid, int mb) {
        this.fluid[tank] = mb > 0 ? fluid : Fluids.EMPTY;
        this.amount[tank] = Math.max(0, Math.min(CAPACITY, mb));
    }

    // ------------------------------------------------------------------ faces

    /** Whether product {@code product} (0 based) may leave through {@code face}. */
    public boolean faceTakes(Direction face, int product) {
        int mode = faceMode[face.get3DDataValue()];
        return mode == 0 || mode == product + 1;
    }

    public int faceMode(Direction face) {
        return faceMode[face.get3DDataValue()];
    }

    /** Steps a face to its next setting (auto, naphtha, diesel, heavy oil, none) and returns it. */
    public int cycleFace(Direction face) {
        int i = face.get3DDataValue();
        faceMode[i] = (faceMode[i] + 1) % (PRODUCTS + 2);
        setChanged();
        return faceMode[i];
    }

    // ------------------------------------------------------------------ fluids

    @Override
    public boolean connectsFluid(Direction side) {
        return true;
    }

    /** The crude tank only; it locks to the first fluid it gets and unlocks when empty. */
    @Override
    public int fill(Direction side, Fluid want, int mb, float pressure, boolean simulate) {
        if (mb <= 0 || want.isSame(Fluids.EMPTY) || !distils(want) || amount[0] > 0 && !fluid[0].isSame(want)) return 0;
        int take = Math.max(0, Math.min(mb, CAPACITY - amount[0]));
        if (!simulate && take > 0) {
            fluid[0] = want;
            amount[0] += take;
            setChanged();
        }
        return take;
    }

    private boolean distils(Fluid want) {
        if (!(level instanceof ServerLevel server)) return want.isSame(Tier6Fluids.CRUDE_OIL.source().get());
        for (RecipeHolder<OilStillRecipe> holder : server.recipeAccess().recipeMap().byType(Tier6Recipes.OIL_STILL.get())) {
            if (holder.value().input().fluid().isSame(want)) return true;
        }
        return false;
    }

    /** The bucket item that carries {@code want}, or null: crude oil pours, the refined liquids come in carried buckets. */
    public static @Nullable ItemStack bucketOf(Fluid want) {
        if (want.isSame(Tier6Fluids.CRUDE_OIL.source().get())) return new ItemStack(Tier6Items.CRUDE_OIL_BUCKET.get());
        if (want.isSame(Tier6Fluids.NAPHTHA.source().get())) return new ItemStack(Tier6Items.NAPHTHA_BUCKET.get());
        if (want.isSame(Tier6Fluids.DIESEL.source().get())) return new ItemStack(Tier6Items.DIESEL_BUCKET.get());
        if (want.isSame(Tier6Fluids.HEAVY_OIL.source().get())) return new ItemStack(Tier6Items.HEAVY_OIL_BUCKET.get());
        return null;
    }

    /** A full bucket poured in by hand, or an empty one filled from the fullest product tank. */
    public InteractionResult useBucket(Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack) {
        if (stack.is(Items.BUCKET)) return takeBucket(player, hand, -1) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        @Nullable Fluid carried = FluidBuckets.fluidOf(stack);
        if (carried == null || fill(Direction.UP, carried, BUCKET, 0, true) < BUCKET) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            fill(Direction.UP, carried, BUCKET, 0, false);
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
            level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Fills an empty bucket from product tank {@code product} (0 based), or from the fullest one when it is
     * negative. Uses the bucket in {@code hand}; from the screen ({@code hand} null) it takes one out of the inventory.
     */
    public boolean takeBucket(Player player, @Nullable InteractionHand hand, int product) {
        if (level == null || level.isClientSide()) return product >= 0 || hand != null;
        int tank = product >= 0 ? 1 + product : fullestProduct();
        if (tank < 1 || amount[tank] < BUCKET) return false;
        ItemStack filled = bucketOf(fluid[tank]);
        if (filled == null) return false;
        ItemStack held = hand == null ? ItemStack.EMPTY : player.getItemInHand(hand);
        if (hand != null && !held.is(Items.BUCKET)) return false;
        if (hand == null) {
            int slot = player.getInventory().findSlotMatchingItem(new ItemStack(Items.BUCKET));
            if (slot < 0) return false;
            player.getInventory().removeItem(slot, 1);
            if (!player.getInventory().add(filled)) net.minecraft.world.Containers.dropItemStack(level, player.getX(), player.getY(), player.getZ(), filled);
        } else {
            player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, filled));
        }
        amount[tank] -= BUCKET;
        if (amount[tank] <= 0) {
            amount[tank] = 0;
            fluid[tank] = Fluids.EMPTY;
        }
        setChanged();
        level.playSound(null, worldPosition, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
        return true;
    }

    private int fullestProduct() {
        int best = -1;
        for (int tank = 1; tank < TANKS; tank++) if (amount[tank] >= BUCKET && (best < 0 || amount[tank] > amount[best])) best = tank;
        return best;
    }

    // ------------------------------------------------------------------ heat

    @Override
    public boolean connectsHeat(Direction side) {
        return true;
    }

    @Override
    public int heatDemand(float temperature) {
        return intake.demand(temperature, status == Status.DISTILLING || status == Status.NEEDS_HEAT);
    }

    @Override
    public int offerHeat(float temperature, int heat) {
        return intake.offer(temperature, heat);
    }

    @Override
    public void heatRoute(int pipes, @Nullable HeatPipeBlock limitedBy) {
        intake.route(limitedBy);
    }

    // ------------------------------------------------------------------ menu

    @Override
    public Component getDisplayName() {
        return Component.translatable("container." + StrataIndustria.MOD_ID + ".oil_still");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new OilStillMenu(id, inventory, worldPosition, this, data);
    }

    // ------------------------------------------------------------------ storage

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        progress = in.getFloatOr("progress", 0.0f);
        batches = in.getIntOr("batches", 0);
        for (int i = 0; i < 6; i++) faceMode[i] = in.getIntOr("face" + i, 0);
        for (int i = 0; i < TANKS; i++) {
            amount[i] = in.getIntOr("tank" + i, 0);
            fluid[i] = in.read("fluid" + i, BuiltInRegistries.FLUID.byNameCodec()).orElse(Fluids.EMPTY);
            if (fluid[i].isSame(Fluids.EMPTY)) amount[i] = 0;
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putFloat("progress", progress);
        out.putInt("batches", batches);
        for (int i = 0; i < 6; i++) if (faceMode[i] != 0) out.putInt("face" + i, faceMode[i]);
        for (int i = 0; i < TANKS; i++) {
            out.putInt("tank" + i, amount[i]);
            if (amount[i] > 0) out.store("fluid" + i, BuiltInRegistries.FLUID.byNameCodec(), fluid[i]);
        }
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
