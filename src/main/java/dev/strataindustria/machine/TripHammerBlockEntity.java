package dev.strataindustria.machine;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.forge.ForgeBlockEntity;
import dev.strataindustria.heat.Heat;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.power.KineticConsumer;
import dev.strataindustria.power.KineticNetworks;
import dev.strataindustria.power.KineticState;
import dev.strataindustria.registry.ModBlockEntities;
import dev.strataindustria.registry.ModDataComponents;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.smithing.AnvilBlockEntity;
import dev.strataindustria.smithing.AnvilRecipe;
import dev.strataindustria.smithing.Smithing;
import dev.strataindustria.smithing.SmithingPattern;
import dev.strataindustria.smithing.SmithingProgress;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The trip hammer (spec 8.4): it replays a recorded smithing pattern on the anvil in front of it. It
 * takes hot workpieces from a forge beside it or the anvil, or from hoppers, and strikes once per 10
 * ticks at 16 RPM. A workpiece that cools goes back to the forge with its progress.
 */
public class TripHammerBlockEntity extends BaseContainerBlockEntity implements KineticConsumer, WorldlyContainer {
    public static final int PATTERN = 0, INPUT = 1;
    public static final int IMPACT = 8, MIN_SPEED = 8;
    public static final float TICKS_PER_HIT = 10.0f;
    private static final float SWING_TICKS = 8.0f;
    public static final int DATA_STATUS = 0, DATA_HITS = 1, DATA_TOTAL = 2, DATA_COUNT = 3;

    public enum Status {
        NO_ANVIL, NO_PATTERN, NOT_TURNING, TOO_SLOW, WAITING, WORKING, OUTPUT_FULL, ANVIL_BUSY, OUTDATED_PATTERN;

        public String key() {
            return StrataIndustria.MOD_ID + ".trip_hammer." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private static final int[] INPUT_SLOTS = {INPUT};

    private NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private final KineticState kinetic = new KineticState();
    private Status status = Status.NO_PATTERN;
    private float timer;
    private int hitsDone;
    private int hitsTotal;
    /** Game time of the last blow, for the renderer's swing. */
    private long lastHit = -100;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_STATUS -> status.ordinal();
                case DATA_HITS -> hitsDone;
                case DATA_TOTAL -> hitsTotal;
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

    public TripHammerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRIP_HAMMER.get(), pos, state);
    }

    private Direction facing() {
        return getBlockState().getValue(TripHammerBlock.FACING);
    }

    public BlockPos anvilPos() {
        return worldPosition.relative(facing());
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TripHammerBlockEntity hammer) {
        Status before = hammer.status;
        hammer.status = hammer.work((ServerLevel) level);
        if (hammer.status != before) hammer.setChanged();
    }

    private Status work(ServerLevel level) {
        if (!(level.getBlockEntity(anvilPos()) instanceof AnvilBlockEntity anvil)) return Status.NO_ANVIL;
        SmithingPattern pattern = items.get(PATTERN).get(ModDataComponents.SMITHING_PATTERN.get());
        // Without a pattern the hammer still knows one job: pressing the slag out of a raw bloom.
        if (pattern == null) pattern = bloomPattern(level);
        if (pattern == null) return Status.NO_PATTERN;
        Optional<RecipeHolder<?>> holder = level.recipeAccess().byKey(pattern.recipe());
        if (holder.isEmpty() || !(holder.get().value() instanceof AnvilRecipe recipe)) return Status.NO_PATTERN;
        hitsTotal = AnvilBlockEntity.blowsFor(recipe, anvil.input().isEmpty() ? items.get(INPUT) : anvil.input());

        // A finished piece goes into a container under the anvil, or onto the anvil top.
        ItemStack done = anvil.getItem(AnvilBlockEntity.OUTPUT);
        if (!done.isEmpty()) {
            if (!deliver(level, anvil, done)) return Status.OUTPUT_FULL;
            anvil.setItem(AnvilBlockEntity.OUTPUT, ItemStack.EMPTY);
        }

        float rpm = kinetic.rpm();
        ItemStack piece = anvil.getItem(AnvilBlockEntity.INPUT);
        if (piece.isEmpty()) {
            hitsDone = 0;
            if (rpm < MIN_SPEED) return rpm <= 0 ? Status.NOT_TURNING : Status.TOO_SLOW;
            ItemStack fetched = fetch(level, anvil, recipe);
            if (fetched.isEmpty()) return Status.WAITING;
            anvil.setItem(AnvilBlockEntity.INPUT, fetched);
            if (!anvil.select(pattern.recipe())) return Status.ANVIL_BUSY;
            timer = 0;
            return Status.WORKING;
        }
        SmithingProgress progress = piece.get(ModDataComponents.SMITHING_PROGRESS.get());
        if (!recipe.matches(new SingleRecipeInput(piece), level) || progress != null && !progress.recipe().equals(pattern.recipe())) {
            return Status.ANVIL_BUSY;
        }
        if (progress == null && !anvil.select(pattern.recipe())) return Status.ANVIL_BUSY;
        hitsDone = progress == null ? 0 : progress.blows();
        if (rpm < MIN_SPEED) return rpm <= 0 ? Status.NOT_TURNING : Status.TOO_SLOW;
        if (hitsDone >= hitsTotal) return Status.ANVIL_BUSY;
        if (Heat.get(piece, level) < AnvilBlockEntity.workingTemperature(piece)) {
            // Back into the forge to heat up again, progress and all.
            if (returnToForge(level, anvil, piece)) anvil.setItem(AnvilBlockEntity.INPUT, ItemStack.EMPTY);
            return Status.WAITING;
        }

        timer += rpm / 16.0f;
        if (timer < TICKS_PER_HIT) return Status.WORKING;
        timer = 0;
                AnvilBlockEntity.MachineHit result = anvil.machineBlow(0);
        if (result == AnvilBlockEntity.MachineHit.STRUCK || result == AnvilBlockEntity.MachineHit.DONE) {
            hitsDone++;
            lastHit = level.getGameTime();
            sync();
        }
        if (result == AnvilBlockEntity.MachineHit.DONE) {
            hitsDone = 0;
            Journal.awardNear(level, worldPosition, Journal.TRIP_HAMMER);
        }
        return result == AnvilBlockEntity.MachineHit.REFUSED ? Status.ANVIL_BUSY : Status.WORKING;
    }

    /** The built-in bloom refining shape: without a pattern the hammer still presses the slag out of a raw bloom. */
    private static @Nullable SmithingPattern bloomPattern(ServerLevel level) {
        ResourceKey<Recipe<?>> id = ResourceKey.create(Registries.RECIPE, StrataIndustria.id("anvil/bloom_refining"));
        if (level.recipeAccess().byKey(id).isEmpty()) return null;
        return new SmithingPattern(id, BuiltInRegistries.ITEM.getKey(Items.IRON_INGOT));
    }

    /** A hot workpiece for {@code recipe}: from the hammer's own slot, else from a forge beside the hammer or the anvil. */
    private ItemStack fetch(ServerLevel level, AnvilBlockEntity anvil, AnvilRecipe recipe) {
        ItemStack own = items.get(INPUT);
        if (ready(level, own, recipe)) {
            setChanged();
            return own.split(recipe.count());
        }
        for (ForgeBlockEntity forge : forges(level)) {
            for (int i = ForgeBlockEntity.FIRST_HEAT_SLOT; i < ForgeBlockEntity.FIRST_HEAT_SLOT + ForgeBlockEntity.HEAT_SLOTS; i++) {
                ItemStack stack = forge.getItem(i);
                if (!ready(level, stack, recipe)) continue;
                ItemStack taken = stack.split(recipe.count());
                forge.setChanged();
                return taken;
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean ready(ServerLevel level, ItemStack stack, AnvilRecipe recipe) {
        return !stack.isEmpty() && stack.getCount() >= recipe.count() && recipe.matches(new SingleRecipeInput(stack), level)
                && Heat.get(stack, level) >= AnvilBlockEntity.workingTemperature(stack);
    }

    private boolean returnToForge(ServerLevel level, AnvilBlockEntity anvil, ItemStack piece) {
        for (ForgeBlockEntity forge : forges(level)) {
            for (int i = ForgeBlockEntity.FIRST_HEAT_SLOT; i < ForgeBlockEntity.FIRST_HEAT_SLOT + ForgeBlockEntity.HEAT_SLOTS; i++) {
                if (!forge.getItem(i).isEmpty()) continue;
                forge.setItem(i, piece.copy());
                return true;
            }
        }
        return false;
    }

    /** Forges touching the trip hammer or the anvil. */
    private List<ForgeBlockEntity> forges(ServerLevel level) {
        List<ForgeBlockEntity> found = new ArrayList<>();
        for (BlockPos centre : List.of(worldPosition, anvilPos())) {
            for (Direction side : Direction.Plane.HORIZONTAL) {
                if (level.getBlockEntity(centre.relative(side)) instanceof ForgeBlockEntity forge && !found.contains(forge)) found.add(forge);
            }
        }
        return found;
    }

    private boolean deliver(ServerLevel level, AnvilBlockEntity anvil, ItemStack done) {
        BlockPos below = anvilPos().below();
        if (level.getBlockEntity(below) instanceof Container container) {
            ItemStack left = HopperBlockEntity.addItem(null, container, done.copy(), Direction.UP);
            if (!left.isEmpty()) return false;
            container.setChanged();
            return true;
        }
        Block.popResource(level, anvilPos().above(), done.copy());
        return true;
    }

    public Status status() {
        return status;
    }

    /** 1 at the moment of a blow, falling to 0 as the arm lifts again, for the renderer. */
    public float swing(long gameTime, float partialTick) {
        float since = gameTime - lastHit + partialTick;
        return since < 0 || since > SWING_TICKS ? 0.0f : 1.0f - since / SWING_TICKS;
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    // ------------------------------------------------------------------ kinetics

    @Override
    public boolean connects(Direction side) {
        return side == Direction.UP || side == facing().getOpposite();
    }

    @Override
    public KineticState kinetic() {
        return kinetic;
    }

    @Override
    public int impact() {
        return IMPACT;
    }

    @Override
    public int minSpeed() {
        return MIN_SPEED;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        KineticNetworks.markDirty(level, worldPosition);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        KineticNetworks.markDirty(level, worldPosition);
    }

    // ------------------------------------------------------------------ container

    public static boolean isRecordedPattern(ItemStack stack) {
        return stack.is(ModItems.SMITHING_PATTERN.get()) && stack.has(ModDataComponents.SMITHING_PATTERN.get());
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return INPUT_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == INPUT;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == INPUT || isRecordedPattern(stack);
    }

    @Override
    public int getMaxStackSize() {
        return 16;
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container." + StrataIndustria.MOD_ID + ".trip_hammer");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new TripHammerMenu(id, inventory, worldPosition, this, data);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(in, items);
        timer = in.getFloatOr("timer", 0.0f);
        lastHit = in.getLongOr("last_hit", -100L);
        kinetic.load(in);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        ContainerHelper.saveAllItems(out, items);
        out.putFloat("timer", timer);
        out.putLong("last_hit", lastHit);
        kinetic.save(out);
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
