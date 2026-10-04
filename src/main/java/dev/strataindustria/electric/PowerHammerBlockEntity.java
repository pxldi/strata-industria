package dev.strataindustria.electric;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.electric.machine.ElectricMachineBlock;
import dev.strataindustria.forge.ForgeLimits;
import dev.strataindustria.heat.Heat;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.power.ElectricConsumer;
import dev.strataindustria.power.ElectricNetwork;
import dev.strataindustria.power.ElectricNetworks;
import dev.strataindustria.power.ElectricStats;
import dev.strataindustria.power.ElectricStatus;
import dev.strataindustria.power.ElectricTier;
import dev.strataindustria.power.StatusLight;
import dev.strataindustria.registry.ModDataComponents;
import dev.strataindustria.registry.Tier5BlockEntities;
import dev.strataindustria.registry.Tier5Sounds;
import dev.strataindustria.smithing.AnvilBlockEntity;
import dev.strataindustria.smithing.AnvilRecipe;
import dev.strataindustria.smithing.Smithing;
import dev.strataindustria.smithing.ShapeMachine;
import dev.strataindustria.smithing.ShapeSelector;
import dev.strataindustria.smithing.SmithingProgress;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
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
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The power hammer (tier 5 spec 10.8): the steam hammer's electric mirror with the tier 5 anvil built in.
 * It works the shape picked with its screen's button on workpieces from its input slot, heats the piece itself by
 * induction (to its working temperature + 100 °C, 16 J/t extra while it heats), and with a second piece it
 * welds, heating both. A blow takes 4 ticks (LV) or 2 (MV) of full power; on low power it waits
 * between blows rather than hitting weakly.
 */
public class PowerHammerBlockEntity extends AnvilBlockEntity implements WorldlyContainer, ElectricConsumer, ShapeMachine {
    /** These two follow the anvil's slots. */
    public static final int QUEUE = AnvilBlockEntity.SLOTS, RESULT = AnvilBlockEntity.SLOTS + 1, HAMMER_SLOTS = AnvilBlockEntity.SLOTS + 2;
    public static final int ANVIL_TIER = 5;
    public static final int BASE_HIT_TICKS = 4;
    /** Extra J/t while the induction coil heats a piece. */
    public static final int HEAT_DRAW = 16;
    /** Degrees above the working temperature the coil heats a smithing piece to. */
    public static final int HEAT_OVER = 100;
    /** Degrees above the welding temperature the coil heats welding pieces to. */
    public static final int WELD_OVER = 50;
    /** The heating constant per second (tier 0 to 2 spec 5.2 rates). */
    public static final float HEAT_RATE = 0.1f;
    public static final int HEAT_BATCH = 5;
    public static final int EJECT_TICKS = 20;
    public static final ElectricStats STATS = new ElectricStats(new ElectricStats.Tiered<>(16, 64), new ElectricStats.Tiered<>(1.0f, 0.5f),
            new ElectricStats.Tiered<>(1, 1), 20);
    private static final float SWING_TICKS = 4.0f;
    /** The anvil face, 7 pixels up. */
    public static final double FACE = 7.0 / 16.0;

    public static final int DATA_STATUS = 0, DATA_HITS = 1, DATA_TOTAL = 2, DATA_PIECE_TEMPERATURE = 3, DATA_NEEDED = 4, DATA_POWER = 5,
            DATA_BUFFER = 6, DATA_EJECT = 7, DATA_SHAPE = 8, DATA_COUNT = 9;

    public enum Status {
        WAITING(StatusLight.OFF), WRONG_PIECE(StatusLight.ERROR),
        OUTPUT_FULL(StatusLight.ERROR), NO_POWER(StatusLight.WAIT), LOW_POWER(StatusLight.WAIT),
        HEATING(StatusLight.RUN), WORKING(StatusLight.RUN), WELDING(StatusLight.RUN), OVERVOLTAGE(StatusLight.ERROR), TOO_FAR(StatusLight.ERROR);

        private final StatusLight light;

        Status(StatusLight light) {
            this.light = light;
        }

        public StatusLight light() {
            return light;
        }

        public String key() {
            return StrataIndustria.MOD_ID + ".power_hammer.status." + name().toLowerCase(Locale.ROOT);
        }

        /** Shown in the screen's calm colour. */
        public boolean fine() {
            return this == WAITING || this == HEATING || this == WORKING || this == WELDING;
        }

        public boolean active() {
            return this == HEATING || this == WORKING || this == WELDING;
        }
    }

    private static final int[] UP_SLOTS = {QUEUE}, SIDE_SLOTS = {SECOND, RESULT}, DOWN_SLOTS = {RESULT};

    private Status status = Status.WAITING;
    private final ShapeSelector shape = new ShapeSelector();
    private double buffer;
    private float power;
    private int timer;
    private float heatShare;
    private int heatTicks;
    private int hitsDone;
    private int hitsTotal;
    private int age;
    private boolean autoEject;
    private long nextEject;
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
                case DATA_NEEDED -> neededTemperature();
                case DATA_POWER -> Math.round(power * 100);
                case DATA_BUFFER -> (int) Math.round(buffer / bufferCapacity() * 100);
                case DATA_EJECT -> autoEject ? 1 : 0;
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

    public PowerHammerBlockEntity(BlockPos pos, BlockState state) {
        super(Tier5BlockEntities.POWER_HAMMER.get(), pos, state);
    }

    @Override
    protected int slotCount() {
        return HAMMER_SLOTS;
    }

    @Override
    protected int anvilTier() {
        return ANVIL_TIER;
    }

    @Override
    protected double faceHeight() {
        return FACE;
    }

    public ElectricStats stats() {
        return ElectricStats.of(getBlockState().getBlock(), STATS);
    }

    /** Ticks between blows at this tier: 4 at LV, 2 at MV. */
    public int hitTicks() {
        return Math.max(1, Math.round(BASE_HIT_TICKS * stats().ticks().get(tier())));
    }

    public double bufferCapacity() {
        return (stats().draw().get(tier()) + HEAT_DRAW) * (double) stats().bufferTicks();
    }

    public double buffer() {
        return buffer;
    }

    /** Fills the buffer directly, for game tests. */
    public void setBuffer(double joules) {
        buffer = Math.max(0, Math.min(bufferCapacity(), joules));
    }

    public Status status() {
        return status;
    }

    public boolean autoEject() {
        return autoEject;
    }

    public void toggleAutoEject() {
        autoEject = !autoEject;
        setChanged();
    }

    /** 1 at the moment of a blow, falling to 0 as the ram lifts again, for the renderer. */
    public float swing(long gameTime, float partialTick) {
        float since = gameTime - lastHit + partialTick;
        return since < 0 || since > SWING_TICKS ? 0.0f : 1.0f - since / SWING_TICKS;
    }

    /** The temperature the screen shows a piece has to reach: the weld's when welding, else the metal's working one. */
    private int neededTemperature() {
        ItemStack piece = input().isEmpty() ? getItem(QUEUE) : input();
        ItemStack second = getItem(SECOND);
        if (!second.isEmpty()) return piece.isEmpty() ? 0 : weldingTemperature(piece, second);
        return workingTemperature(piece);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PowerHammerBlockEntity hammer) {
        hammer.tick((ServerLevel) level, pos, state);
    }

    private void tick(ServerLevel level, BlockPos pos, BlockState state) {
        Status before = status;
        status = work(level);
        if (status != before) {
            setChanged();
            transition(level, before);
        }
        if (autoEject && level.getGameTime() >= nextEject) {
            nextEject = level.getGameTime() + EJECT_TICKS;
            eject(level);
        }
        BlockState next = state.setValue(StatusLight.PROPERTY, status.light()).setValue(ElectricMachineBlock.ACTIVE, status.active());
        if (next != state) level.setBlock(pos, next, Block.UPDATE_CLIENTS);
        // The workpiece's heat changes every tick; it is saved once a second rather than synced each tick.
        if (++age % 20 == 0 && !input().isEmpty()) setChanged();
    }

    /** Spec 23.6: a relay clack when power comes and goes, two beeps when it runs short. */
    private void transition(ServerLevel level, Status before) {
        SoundEvent sound = null;
        if (status.active() && !before.active()) sound = Tier5Sounds.MACHINE_POWER_ON.get();
        else if (before.active() && (status == Status.NO_POWER || status.light() == StatusLight.ERROR)) sound = Tier5Sounds.MACHINE_POWER_OFF.get();
        if (sound != null) level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, 0.4f, 1.0f);
        if (status == Status.LOW_POWER && before != Status.LOW_POWER) {
            level.playSound(null, worldPosition, Tier5Sounds.MACHINE_LOW_POWER.get(), SoundSource.PLAYERS, 0.5f, 1.0f);
        }
    }

    private Status work(ServerLevel level) {
        ElectricNetwork.Report report = ElectricNetworks.report(level, worldPosition);
        if (report.status() == ElectricStatus.OVERVOLTAGE || report.status() == ElectricStatus.CABLE_OVERVOLTAGE) return Status.OVERVOLTAGE;
        if (report.status() == ElectricStatus.TOO_FAR) return Status.TOO_FAR;
        power = 0;
        spillRetired(level);
        if (!moveResult(level)) return Status.OUTPUT_FULL;
        return getItem(SECOND).isEmpty() ? smith(level) : weld(level);
    }

    /** Takes up to {@code joules} from the buffer; the share of it that was there, 0 to 1. */
    private float draw(double joules) {
        double take = Math.min(buffer, joules);
        buffer -= take;
        return (float) (take / joules);
    }

    /** Takes a blow's worth of power only if all of it is there. */
    private boolean drawBlow() {
        double need = stats().draw().get(tier());
        power = (float) Math.min(1.0, buffer / need);
        if (buffer + 1.0e-6 < need) return false;
        buffer -= need;
        power = 1.0f;
        return true;
    }

    /**
     * Heats the pieces towards {@code target} at the coil's rate scaled by the power it got. The heat is applied every
     * {@link #HEAT_BATCH} ticks: a single tick's step on a cold piece is under the 5 °C below which the heat is forgotten.
     */
    private void induct(float target, float share, long now, ItemStack... stacks) {
        heatShare += share;
        if (++heatTicks < HEAT_BATCH) return;
        float scale = heatShare / heatTicks;
        for (ItemStack stack : stacks) {
            float max = ForgeLimits.maxFor(stack);
            Heat.heatToward(stack, Math.min(target, max), HEAT_RATE * scale, heatTicks, max, now);
        }
        heatShare = 0;
        heatTicks = 0;
    }

    // ------------------------------------------------------------------ smithing

    private Status smith(ServerLevel level) {
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

        // Induction: the coil heats the piece itself, so no forge or heat pipe is involved.
        long now = level.getGameTime();
        int working = workingTemperature(piece);
        float share = 1.0f;
        boolean heating = false;
        if (working > 0) {
            float target = working + HEAT_OVER;
            if (Heat.get(piece, now) < Math.min(target, ForgeLimits.maxFor(piece)) - 1) {
                heating = true;
                share = draw(HEAT_DRAW);
                if (share > 0.001f) {
                    induct(target, share, now, piece);
                    induction(level, now);
                }
                power = share;
            }
        }
        float temperature = Heat.get(piece, now);
        if (temperature < working) return share <= 0.001f ? Status.NO_POWER : Status.HEATING;

        if (!drawBlow()) return heating && power <= 0.001f ? Status.NO_POWER : Status.LOW_POWER;
        if (++timer < hitTicks()) return Status.WORKING;
        timer = 0;
                MachineHit result = machineBlow(4);
        if (result == MachineHit.TOO_COLD) return Status.HEATING;
        if (result == MachineHit.REFUSED) return Status.WRONG_PIECE;
        hitsDone++;
        struck(level, now);
        if (result == MachineHit.STRUCK) {
            // The coil's heat makes up what the blow took (spec 10.8: hit cooling is offset while it heats).
            ItemStack worked = input();
            Heat.set(worked, Math.max(Heat.get(worked, now), temperature), now);
        } else {
            hitsDone = 0;
            Journal.awardNear(level, worldPosition, Journal.POWER_HAMMER);
        }
        return Status.WORKING;
    }

    // ------------------------------------------------------------------ welding (T3 spec 9.4, automated)

    private Status weld(ServerLevel level) {
        ItemStack second = getItem(SECOND);
        ItemStack piece = input();
        if (piece.isEmpty()) {
            ItemStack queued = getItem(QUEUE);
            if (queued.isEmpty()) return Status.WAITING;
            if (weldResult(queued.copyWithCount(1), second).isEmpty()) return Status.WRONG_PIECE;
            setItem(INPUT, queued.split(1));
            timer = 0;
            piece = input();
        }
        if (weldResult(piece, second).isEmpty()) return Status.WRONG_PIECE;
        WeldStatus weld = weldStatus(null);
        if (weld == WeldStatus.TOO_WEAK) return Status.WRONG_PIECE;
        hitsDone = 0;
        hitsTotal = 1;

        long now = level.getGameTime();
        int needed = weldingTemperature(piece, second);
        float target = needed + WELD_OVER;
        float share = 1.0f;
        if (Heat.get(piece, now) < Math.min(target, ForgeLimits.maxFor(piece)) - 1 || Heat.get(second, now) < Math.min(target, ForgeLimits.maxFor(second)) - 1) {
            share = draw(HEAT_DRAW);
            if (share > 0.001f) {
                induct(target, share, now, piece, second);
                induction(level, now);
            }
            power = share;
        }
        if (Heat.get(piece, now) < needed || Heat.get(second, now) < needed) return share <= 0.001f ? Status.NO_POWER : Status.HEATING;

        if (!drawBlow()) return Status.LOW_POWER;
        if (++timer < hitTicks()) return Status.WELDING;
        timer = 0;
        if (!machineWeld()) return Status.WRONG_PIECE;
        Journal.awardNear(level, worldPosition, Journal.POWER_HAMMER);
        struck(level, now);
        return Status.WELDING;
    }

    private void struck(ServerLevel level, long now) {
        lastHit = now;
        level.playSound(null, worldPosition, Tier5Sounds.POWER_HAMMER_STRIKE.get(), SoundSource.BLOCKS, 0.8f, 0.9f + level.getRandom().nextFloat() * 0.15f);
    }

    /** A rising whine every two seconds while the coil heats. */
    private void induction(ServerLevel level, long now) {
        if (now % 40 == 0) level.playSound(null, worldPosition, Tier5Sounds.POWER_HAMMER_INDUCTION.get(), SoundSource.BLOCKS, 0.5f, 1.0f);
    }

    // ------------------------------------------------------------------ items out

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

    /** Pushes the finished pieces into the container behind the hammer. */
    private void eject(ServerLevel level) {
        Direction back = getBlockState().getValue(ElectricMachineBlock.FACING).getOpposite();
        if (!(level.getBlockEntity(worldPosition.relative(back)) instanceof Container target)) return;
        ItemStack stack = getItem(RESULT);
        if (stack.isEmpty()) return;
        ItemStack left = HopperBlockEntity.addItem(null, target, stack.copy(), back.getOpposite());
        if (left.getCount() != stack.getCount()) {
            setItem(RESULT, left);
            target.setChanged();
            setChanged();
        }
    }

    // ------------------------------------------------------------------ electricity

    @Override
    public ElectricTier tier() {
        return getBlockState().getValue(ElectricMachineBlock.TIER);
    }

    @Override
    public boolean connectsElectric(Direction side) {
        return true;
    }

    @Override
    public double request() {
        return Math.max(0, Math.min(bufferCapacity() - buffer, stats().draw().get(tier()) + HEAT_DRAW));
    }

    @Override
    public void receive(double amount) {
        buffer = Math.min(bufferCapacity(), buffer + amount);
    }

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
        return slot == QUEUE || slot == SECOND;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.UP ? UP_SLOTS : side == Direction.DOWN ? DOWN_SLOTS : SIDE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return (slot == QUEUE || slot == SECOND) && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == RESULT;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container." + StrataIndustria.MOD_ID + ".power_hammer");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new PowerHammerMenu(id, inventory, this, data);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        shape.load(in);
        buffer = in.getDoubleOr("buffer", 0.0);
        timer = in.getIntOr("timer", 0);
        autoEject = in.getBooleanOr("auto_eject", false);
        lastHit = in.getLongOr("last_hit", -100L);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        shape.save(out);
        out.putDouble("buffer", buffer);
        out.putInt("timer", timer);
        out.putBoolean("auto_eject", autoEject);
        out.putLong("last_hit", lastHit);
    }
}
