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
import dev.strataindustria.smithing.AnvilBlockEntity;
import dev.strataindustria.smithing.AnvilRecipe;
import dev.strataindustria.smithing.Smithing;
import dev.strataindustria.smithing.ShapeMachine;
import dev.strataindustria.smithing.ShapeSelector;
import dev.strataindustria.smithing.SmithingProgress;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceKey;
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
 * The trip hammer (spec 8.4): it works the shape picked with its screen's button on the anvil in front of it. It
 * takes hot workpieces from a forge beside it or the anvil, or from hoppers, and strikes once per 10
 * ticks at 16 RPM. A workpiece that cools goes back to the forge with its progress.
 */
public class TripHammerBlockEntity extends BaseContainerBlockEntity implements KineticConsumer, WorldlyContainer, ShapeMachine {
    /** Slot 0 held a pattern before the shape button. It stays empty, so a saved hammer still loads. */
    private static final int RETIRED = 0;
    public static final int INPUT = 1;
    public static final int IMPACT = 8, MIN_SPEED = 8;
    public static final float TICKS_PER_HIT = 10.0f;
    private static final float SWING_TICKS = 8.0f;
    public static final int DATA_STATUS = 0, DATA_HITS = 1, DATA_TOTAL = 2, DATA_SHAPE = 3, DATA_COUNT = 4;

    public enum Status {
        NO_ANVIL, NOT_TURNING, TOO_SLOW, WAITING, WORKING, OUTPUT_FULL, ANVIL_BUSY;

        public String key() {
            return StrataIndustria.MOD_ID + ".trip_hammer." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private static final int[] INPUT_SLOTS = {INPUT};

    private NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private final KineticState kinetic = new KineticState();
    private Status status = Status.WAITING;
    private final ShapeSelector shape = new ShapeSelector();
    /** The shape this hammer last set on the anvil, so a change of shape restarts its own work and never somebody's hand work. */
    private @Nullable ResourceKey<Recipe<?>> workingKey;
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
                case DATA_SHAPE -> shapeId();
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
        if (!items.get(RETIRED).isEmpty()) {
            Block.popResource(level, worldPosition.above(), items.get(RETIRED));
            items.set(RETIRED, ItemStack.EMPTY);
            setChanged();
        }
        if (!(level.getBlockEntity(anvilPos()) instanceof AnvilBlockEntity anvil)) return Status.NO_ANVIL;

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
            hitsTotal = 0;
            if (rpm < MIN_SPEED) return rpm <= 0 ? Status.NOT_TURNING : Status.TOO_SLOW;
            Fetched fetched = fetch(level);
            if (fetched == null) return Status.WAITING;
            hitsTotal = AnvilBlockEntity.blowsFor(fetched.holder().value(), fetched.stack());
            anvil.setItem(AnvilBlockEntity.INPUT, fetched.stack());
            if (!anvil.select(fetched.holder().id())) return Status.ANVIL_BUSY;
            workingKey = fetched.holder().id();
            timer = 0;
            return Status.WORKING;
        }
        Optional<RecipeHolder<AnvilRecipe>> holder = shape.resolve(level, piece);
        if (holder.isEmpty()) return Status.ANVIL_BUSY;
        ResourceKey<Recipe<?>> key = holder.get().id();
        hitsTotal = AnvilBlockEntity.blowsFor(holder.get().value(), piece);
        SmithingProgress progress = piece.get(ModDataComponents.SMITHING_PROGRESS.get());
        if (progress != null && !progress.recipe().equals(key)) {
            // Only work this hammer began restarts when its shape changes.
            if (!progress.recipe().equals(workingKey)) return Status.ANVIL_BUSY;
            piece.remove(ModDataComponents.SMITHING_PROGRESS.get());
            progress = null;
        }
        if (progress == null && !anvil.select(key)) return Status.ANVIL_BUSY;
        workingKey = key;
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

    /** A workpiece taken off its shelf, with the shape it will be worked into. */
    private record Fetched(ItemStack stack, RecipeHolder<AnvilRecipe> holder) {}

    /** A hot workpiece the shape takes: from the hammer's own slot, else from a forge beside the hammer or the anvil. */
    private @Nullable Fetched fetch(ServerLevel level) {
        ItemStack own = items.get(INPUT);
        Optional<RecipeHolder<AnvilRecipe>> holder = ready(level, own);
        if (holder.isPresent()) {
            setChanged();
            return new Fetched(own.split(holder.get().value().count()), holder.get());
        }
        for (ForgeBlockEntity forge : forges(level)) {
            for (int i = ForgeBlockEntity.FIRST_HEAT_SLOT; i < ForgeBlockEntity.FIRST_HEAT_SLOT + ForgeBlockEntity.HEAT_SLOTS; i++) {
                ItemStack stack = forge.getItem(i);
                holder = ready(level, stack);
                if (holder.isEmpty()) continue;
                ItemStack taken = stack.split(holder.get().value().count());
                forge.setChanged();
                return new Fetched(taken, holder.get());
            }
        }
        return null;
    }

    /** The shape for a hot stack that has enough of it, or empty when it will not do. */
    private Optional<RecipeHolder<AnvilRecipe>> ready(ServerLevel level, ItemStack stack) {
        if (stack.isEmpty() || Heat.get(stack, level) < AnvilBlockEntity.workingTemperature(stack)) return Optional.empty();
        return shape.resolve(level, stack).filter(holder -> stack.getCount() >= holder.value().count());
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

    @Override
    public ShapeSelector shapes() {
        return shape;
    }

    /** The piece on the anvil, else the one waiting in the hammer. */
    @Override
    public ItemStack shapePiece() {
        if (level != null && level.getBlockEntity(anvilPos()) instanceof AnvilBlockEntity anvil && !anvil.input().isEmpty()) return anvil.input();
        return items.get(INPUT);
    }

    /** What the shape button shows: the item the working shape makes. */
    private int shapeId() {
        if (!(level instanceof ServerLevel server)) return 0;
        return ShapeSelector.displayId(shape.resolve(server, shapePiece()).orElse(null));
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
        return slot == INPUT;
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
        shape.load(in);
        timer = in.getFloatOr("timer", 0.0f);
        lastHit = in.getLongOr("last_hit", -100L);
        kinetic.load(in);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        ContainerHelper.saveAllItems(out, items);
        shape.save(out);
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
