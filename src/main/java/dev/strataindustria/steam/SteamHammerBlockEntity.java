package dev.strataindustria.steam;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.fluid.FluidPort;
import dev.strataindustria.forge.ForgeLimits;
import dev.strataindustria.heat.Heat;
import dev.strataindustria.heat.HeatConsumer;
import dev.strataindustria.heat.HeatIntake;
import dev.strataindustria.heat.HeatPipeBlock;
import dev.strataindustria.heat.HeatPort;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.registry.ModDataComponents;
import dev.strataindustria.registry.Tier4BlockEntities;
import dev.strataindustria.registry.Tier4Fluids;
import dev.strataindustria.registry.Tier4Sounds;
import dev.strataindustria.smithing.AnvilBlockEntity;
import dev.strataindustria.smithing.AnvilRecipe;
import dev.strataindustria.smithing.Smithing;
import dev.strataindustria.smithing.ShapeMachine;
import dev.strataindustria.smithing.ShapeSelector;
import dev.strataindustria.smithing.SmithingProgress;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The steam hammer (tier 4 spec 10.5): the trip hammer's tier 4 mirror with a tier 5 anvil built in. It
 * works the shape picked with its screen's button on workpieces from its input slot, heating each one itself from a
 * firebox below or heat pipes, so it never hands a cooling piece back. One blow per 5 ticks at 2 bar or
 * more, per 10 ticks at 1 to 2 bar, for 10 mB of steam a tick while it works.
 */
public class SteamHammerBlockEntity extends AnvilBlockEntity implements WorldlyContainer, FluidPort, HeatConsumer, HeatPort, ShapeMachine {
    /** These two follow the anvil's slots. */
    public static final int QUEUE = AnvilBlockEntity.SLOTS, RESULT = AnvilBlockEntity.SLOTS + 1, HAMMER_SLOTS = AnvilBlockEntity.SLOTS + 2;
    public static final int TIER = 5;
    public static final int FAST_TICKS = 5, SLOW_TICKS = 10;
    public static final float FULL_PRESSURE = 2.0f, MIN_PRESSURE = 1.0f;
    public static final int STEAM_USE = 10, BUFFER = 40;
    public static final int MIN_TEMPERATURE = 100, HEAT = 20;
    /** Twice a forge slot's rate: the hammer heats one piece, close up. */
    public static final float HEAT_RATE = 2 * Heat.FORGE_RATE;
    /** Ticks without steam before the pressure it last saw counts as gone. */
    private static final int STEAM_TIMEOUT = 5;
    private static final float SWING_TICKS = 4.0f;
    /** The anvil face, 7 pixels up. */
    public static final double FACE = 7.0 / 16.0;

    public static final int DATA_STATUS = 0, DATA_HITS = 1, DATA_TOTAL = 2, DATA_PIECE_TEMPERATURE = 3, DATA_WORKING = 4, DATA_HEAT = 5,
            DATA_HEAT_TEMPERATURE = 6, DATA_LIMIT = 7, DATA_STEAM = 8, DATA_PRESSURE = 9, DATA_SHAPE = 10, DATA_COUNT = 11;

    public enum Status {
        WAITING, WRONG_PIECE, OUTPUT_FULL, NO_STEAM, TOO_COLD, HEATING, WORKING;

        public String key() {
            return StrataIndustria.MOD_ID + ".steam_hammer.status." + name().toLowerCase(java.util.Locale.ROOT);
        }

        public boolean fine() {
            return this == WAITING || this == HEATING || this == WORKING;
        }
    }

    private static final int[] FACE_SLOTS = {QUEUE, RESULT};

    private final HeatIntake intake = new HeatIntake(MIN_TEMPERATURE, HEAT);
    private Status status = Status.WAITING;
    private final ShapeSelector shape = new ShapeSelector();
    private int steam;
    private float pressure;
    private int sinceSteam = STEAM_TIMEOUT;
    private int timer;
    private int hitsDone;
    private int hitsTotal;
    private int age;
    /** Game time of the last blow, for the renderer's ram. */
    private long lastHit = -100;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            ItemStack piece = input();
            return switch (index) {
                case DATA_STATUS -> status.ordinal();
                case DATA_HITS -> hitsDone;
                case DATA_TOTAL -> hitsTotal;
                case DATA_PIECE_TEMPERATURE -> piece.isEmpty() || level == null ? 0 : Math.round(Heat.get(piece, level));
                case DATA_WORKING -> workingTemperature(piece.isEmpty() ? getItem(QUEUE) : piece);
                case DATA_SHAPE -> shapeId();
                case DATA_HEAT -> intake.heat();
                case DATA_HEAT_TEMPERATURE -> Math.round(intake.temperature());
                case DATA_LIMIT -> intake.limit();
                case DATA_STEAM -> steam;
                case DATA_PRESSURE -> Math.round(pressure * 10);
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

    public SteamHammerBlockEntity(BlockPos pos, BlockState state) {
        super(Tier4BlockEntities.STEAM_HAMMER.get(), pos, state);
    }

    @Override
    protected int slotCount() {
        return HAMMER_SLOTS;
    }

    @Override
    protected int anvilTier() {
        return TIER;
    }

    @Override
    protected double faceHeight() {
        return FACE;
    }

    private Direction facing() {
        return getBlockState().getValue(SteamHammerBlock.FACING);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SteamHammerBlockEntity hammer) {
        hammer.tick((ServerLevel) level, pos, state);
    }

    private void tick(ServerLevel level, BlockPos pos, BlockState state) {
        intake.roll();
        if (++sinceSteam > STEAM_TIMEOUT) pressure = 0;
        Status before = status;
        status = work(level);
        boolean active = status == Status.WORKING;
        if (state.getValue(SteamHammerBlock.ACTIVE) != active) level.setBlock(pos, state.setValue(SteamHammerBlock.ACTIVE, active), Block.UPDATE_ALL);
        // The workpiece's heat changes every tick; it is saved once a second rather than synced each tick.
        if (status != before || ++age % 20 == 0 && !input().isEmpty()) setChanged();
    }

    private Status work(ServerLevel level) {
        spillRetired(level);
        if (!moveResult(level)) return Status.OUTPUT_FULL;
        ItemStack piece = input();
        ItemStack queued = getItem(QUEUE);
        Optional<RecipeHolder<AnvilRecipe>> holder = shape.resolve(level, shapePiece());
        if (holder.isEmpty()) return piece.isEmpty() && queued.isEmpty() ? Status.WAITING : Status.WRONG_PIECE;
        AnvilRecipe recipe = holder.get().value();
        ResourceKey<Recipe<?>> key = holder.get().id();
        hitsTotal = blowsFor(recipe, piece.isEmpty() ? queued : piece);

        if (piece.isEmpty()) {
            hitsDone = 0;
            if (queued.isEmpty()) return Status.WAITING;
            if (queued.getCount() < recipe.count()) return Status.WAITING;
            setItem(INPUT, queued.split(recipe.count()));
            if (!select(key)) return Status.WRONG_PIECE;
            timer = 0;
            piece = input();
        }
        SmithingProgress progress = piece.get(ModDataComponents.SMITHING_PROGRESS.get());
        if (progress != null && !progress.recipe().equals(key)) {
            // The shape was changed halfway: the work starts over on the new one.
            piece.remove(ModDataComponents.SMITHING_PROGRESS.get());
            progress = null;
        }
        if (progress == null && !select(key)) return Status.WRONG_PIECE;
        hitsDone = progress == null ? 0 : progress.blows();
        if (hitsDone >= hitsTotal) return Status.WRONG_PIECE;

        // The hammer keeps its own piece hot, towards whatever its heat comes in at.
        long now = level.getGameTime();
        int working = workingTemperature(piece);
        float offered = intake.temperature();
        if (intake.heat() > 0) {
            float current = Heat.get(piece, now);
            if (!(offered <= current && current - offered < 1)) {
                Heat.heatToward(piece, offered, HEAT_RATE * Math.max(0.1f, intake.share()), 1, ForgeLimits.maxFor(piece), now);
            }
        }
        if (intake.heat() <= 0 || offered < working) return Status.TOO_COLD;
        float temperature = Heat.get(piece, now);
        if (temperature < working) return Status.HEATING;

        if (pressure < MIN_PRESSURE || steam < STEAM_USE) return Status.NO_STEAM;
        steam -= STEAM_USE;
        if (++timer < (pressure >= FULL_PRESSURE ? FAST_TICKS : SLOW_TICKS)) return Status.WORKING;
        timer = 0;
                MachineHit result = machineBlow(2);
        if (result == MachineHit.TOO_COLD) return Status.HEATING;
        if (result == MachineHit.REFUSED) return Status.WRONG_PIECE;
        hitsDone++;
        lastHit = now;
        level.playSound(null, worldPosition, Tier4Sounds.STEAM_HAMMER_STRIKE.get(), SoundSource.BLOCKS, 0.8f,
                0.9f + level.getRandom().nextFloat() * 0.15f);
        if (result == MachineHit.STRUCK) {
            // The hammer's own heat makes up what the blow took (spec 10.5).
            ItemStack worked = input();
            Heat.set(worked, Math.max(Heat.get(worked, now), temperature), now);
        } else {
            hitsDone = 0;
            Journal.awardNear(level, worldPosition, Journal.STEAM_HAMMER);
        }
        return Status.WORKING;
    }

    /** Moves a finished piece from the anvil into the result slot. False when there is no room. */
    private boolean moveResult(ServerLevel level) {
        ItemStack done = getItem(OUTPUT);
        if (done.isEmpty()) return true;
        ItemStack held = getItem(RESULT);
        if (held.isEmpty()) {
            setItem(RESULT, done);
        } else {
            // Pieces finished at different heats still stack; the pile keeps the hotter heat.
            if (!sameIgnoringHeat(held, done) || held.getCount() + done.getCount() > held.getMaxStackSize()) return false;
            long now = level.getGameTime();
            float heat = Math.max(Heat.get(held, now), Heat.get(done, now));
            held.grow(done.getCount());
            Heat.set(held, heat, now);
        }
        setItem(OUTPUT, ItemStack.EMPTY);
        return true;
    }

    private static boolean sameIgnoringHeat(ItemStack a, ItemStack b) {
        ItemStack x = a.copyWithCount(1), y = b.copyWithCount(1);
        x.remove(ModDataComponents.TEMPERATURE.get());
        y.remove(ModDataComponents.TEMPERATURE.get());
        return ItemStack.isSameItemSameComponents(x, y);
    }

    public Status status() {
        return status;
    }

    public int steam() {
        return steam;
    }

    public float pressure() {
        return pressure;
    }

    public HeatIntake intake() {
        return intake;
    }

    /** 1 at the moment of a blow, falling to 0 as the ram lifts again, for the renderer. */
    public float swing(long gameTime, float partialTick) {
        float since = gameTime - lastHit + partialTick;
        return since < 0 || since > SWING_TICKS ? 0.0f : 1.0f - since / SWING_TICKS;
    }

    // ------------------------------------------------------------------ steam

    @Override
    public boolean connectsFluid(Direction side) {
        return side == facing().getOpposite();
    }

    @Override
    public int fill(Direction side, Fluid fluid, int amount, float pressure, boolean simulate) {
        if (side != facing().getOpposite() || !fluid.isSame(Tier4Fluids.STEAM.get())) return 0;
        // Whatever it is offered tells it the pressure, even when its buffer is full.
        this.pressure = pressure;
        sinceSteam = 0;
        if (pressure < MIN_PRESSURE) return 0;
        int take = Math.max(0, Math.min(amount, BUFFER - steam));
        if (!simulate) steam += take;
        return take;
    }

    // ------------------------------------------------------------------ heat

    @Override
    public boolean connectsHeat(Direction side) {
        return side == Direction.DOWN || side.getAxis() != Direction.Axis.Y && side.getAxis() != facing().getAxis();
    }

    @Override
    public int heatDemand(float temperature) {
        // It draws while it has a piece to work or one waiting.
        boolean wanted = status != Status.OUTPUT_FULL && status != Status.WRONG_PIECE && (!input().isEmpty() || !getItem(QUEUE).isEmpty());
        return intake.demand(temperature, wanted);
    }

    @Override
    public int offerHeat(float temperature, int heat) {
        return intake.offer(temperature, heat);
    }

    @Override
    public void heatRoute(int pipes, @Nullable HeatPipeBlock limitedBy) {
        intake.route(limitedBy);
    }

    // ------------------------------------------------------------------ container

    @Override
    public ShapeSelector shapes() {
        return shape;
    }

    @Override
    public ItemStack shapePiece() {
        return input().isEmpty() ? getItem(QUEUE) : input();
    }

    /** What the shape button shows: the item the working shape makes. */
    private int shapeId() {
        if (!(level instanceof ServerLevel server)) return 0;
        return ShapeSelector.displayId(shape.resolve(server, shapePiece()).orElse(null));
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == QUEUE;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return FACE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == QUEUE;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == RESULT;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container." + StrataIndustria.MOD_ID + ".steam_hammer");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new SteamHammerMenu(id, inventory, this, data);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        shape.load(in);
        steam = in.getIntOr("steam", 0);
        timer = in.getIntOr("timer", 0);
        lastHit = in.getLongOr("last_hit", -100L);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        shape.save(out);
        out.putInt("steam", steam);
        out.putInt("timer", timer);
        out.putLong("last_hit", lastHit);
    }
}
