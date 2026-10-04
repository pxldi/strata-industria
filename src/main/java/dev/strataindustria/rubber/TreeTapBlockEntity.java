package dev.strataindustria.rubber;

import dev.strataindustria.Config;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.fluid.FluidPipes;
import dev.strataindustria.fluid.FluidPort;
import dev.strataindustria.registry.Tier5BlockEntities;
import dev.strataindustria.registry.Tier5Fluids;
import dev.strataindustria.registry.Tier5Particles;
import dev.strataindustria.registry.Tier5Sounds;
import dev.strataindustria.tanning.SoakingBarrelBlock;
import dev.strataindustria.tanning.SoakingBarrelBlockEntity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Spec 5.1: the tap's cup. A tap on a living tree (3 or more tappable logs in a column with leaves round
 * the top) fills at the data map's rate; at most four taps per tree draw, the earliest placed first. The
 * cup holds 1000 mB, gives a bucket when full, and drains into a barrel, pipe or port right below it.
 */
public class TreeTapBlockEntity extends BlockEntity implements FluidPort {
    public static final int CUP = 1000;
    /** Spec 5.1: the tree is checked again this often. */
    public static final int CHECK_TICKS = 600;
    /** Most latex the tap pushes down per push. */
    private static final int PUSH = 100;
    private static final int MIN_COLUMN = 3, MIN_LEAVES = 4, LEAF_REACH = 2, MAX_COLUMN = 32;

    public enum Status {
        NO_TREE, TAPPED_OUT, WORKING;

        public String key() {
            return StrataIndustria.MOD_ID + ".tree_tap.status." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private Fluid fluid = Fluids.EMPTY;
    private int amount;
    private long placedAt = -1;
    private Status status = Status.NO_TREE;
    private int rate = 1;
    private int age;
    private int nextDrip = 40;
    private FluidPipes.Network network = FluidPipes.Network.NONE;

    public TreeTapBlockEntity(BlockPos pos, BlockState state) {
        super(Tier5BlockEntities.TREE_TAP.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TreeTapBlockEntity tap) {
        ServerLevel server = (ServerLevel) level;
        if (tap.placedAt < 0) tap.placed(level);
        if (tap.age++ % CHECK_TICKS == 0) tap.check(server);
        if (tap.status == Status.WORKING && tap.amount < CUP && tap.age % Math.max(1, Config.RUBBER_TAP_TICKS_PER_MB.get()) == 0) {
            tap.amount = Math.min(CUP, tap.amount + tap.rate);
            tap.setChanged();
        }
        if (tap.amount > 0 && tap.age % 10 == 5) tap.pushDown(server);
        if (tap.status == Status.WORKING && tap.amount < CUP && --tap.nextDrip <= 0) {
            tap.drip(server, state);
            tap.nextDrip = 80 + server.getRandom().nextInt(81);
        }
        int fill = fillLevel(tap.amount);
        if (state.getValue(TreeTapBlock.FILL) != fill) level.setBlock(pos, state.setValue(TreeTapBlock.FILL, fill), Block.UPDATE_CLIENTS);
    }

    /** Cup model level: empty, a film, half, full. */
    public static int fillLevel(int mb) {
        if (mb <= 0) return 0;
        if (mb < CUP / 2) return 1;
        return mb < CUP ? 2 : 3;
    }

    /** Remembers when the tap went in, which decides who draws when a tree has too many taps. */
    public void placed(Level level) {
        placedAt = level.getGameTime();
        setChanged();
    }

    /** Spec 5.1: whether the log is part of a living tree, and whether this tap is among the first four on it. */
    public void check(ServerLevel level) {
        BlockState state = getBlockState();
        BlockPos log = TreeTapBlock.logPos(state, worldPosition);
        Tappable tappable = Tappable.of(level.getBlockState(log));
        Status before = status;
        if (tappable == null || !livingTree(level, log)) {
            status = Status.NO_TREE;
        } else {
            rate = tappable.amount();
            if (amount <= 0) fluid = tappable.fluid();
            status = rank(level, log) < Config.RUBBER_MAX_TAPS_PER_TREE.get() ? Status.WORKING : Status.TAPPED_OUT;
        }
        if (status != before) setChanged();
    }

    /** A column of at least 3 tappable logs through {@code log}, with at least 4 leaf blocks within 2 of its top. */
    static boolean livingTree(Level level, BlockPos log) {
        BlockPos bottom = log, top = log;
        while (log.getY() - bottom.getY() < MAX_COLUMN && level.getBlockState(bottom.below()).is(Tappable.TAG)) bottom = bottom.below();
        while (top.getY() - log.getY() < MAX_COLUMN && level.getBlockState(top.above()).is(Tappable.TAG)) top = top.above();
        if (top.getY() - bottom.getY() + 1 < MIN_COLUMN) return false;
        int leaves = 0;
        for (BlockPos p : BlockPos.betweenClosed(top.offset(-LEAF_REACH, -LEAF_REACH, -LEAF_REACH), top.offset(LEAF_REACH, LEAF_REACH, LEAF_REACH))) {
            if (level.getBlockState(p).is(BlockTags.LEAVES) && ++leaves >= MIN_LEAVES) return true;
        }
        return false;
    }

    /** This tap's place among all taps on the same column, earliest placed first. */
    private int rank(Level level, BlockPos log) {
        BlockPos bottom = log, top = log;
        while (log.getY() - bottom.getY() < MAX_COLUMN && level.getBlockState(bottom.below()).is(Tappable.TAG)) bottom = bottom.below();
        while (top.getY() - log.getY() < MAX_COLUMN && level.getBlockState(top.above()).is(Tappable.TAG)) top = top.above();
        List<TreeTapBlockEntity> taps = new ArrayList<>();
        for (BlockPos p = bottom; p.getY() <= top.getY(); p = p.above()) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos at = p.relative(d);
                BlockState there = level.getBlockState(at);
                if (there.getBlock() instanceof TreeTapBlock && there.getValue(TreeTapBlock.FACING) == d
                        && level.getBlockEntity(at) instanceof TreeTapBlockEntity tap) taps.add(tap);
            }
        }
        // A tap that has not ticked yet was placed just now.
        long now = level.getGameTime();
        taps.sort(Comparator.<TreeTapBlockEntity>comparingLong(t -> t.placedAt < 0 ? now : t.placedAt)
                .thenComparingLong(t -> t.worldPosition.asLong()));
        return taps.indexOf(this);
    }

    /** Spec 5.1: straight down into an open barrel, a pipe or a port. */
    private void pushDown(ServerLevel level) {
        BlockPos below = worldPosition.below();
        int moved;
        if (level.getBlockEntity(below) instanceof SoakingBarrelBlockEntity barrel) {
            if (level.getBlockState(below).getValue(SoakingBarrelBlock.SEALED)) return;
            moved = barrel.fill(fluid, Math.min(amount, PUSH), false);
        } else {
            if (age % 20 == 5 || network.isEmpty()) network = FluidPipes.find(level, worldPosition, Direction.DOWN);
            moved = FluidPipes.push(level, network, fluid, Math.min(amount, PUSH), 20.0f, 0.0f).moved();
        }
        if (moved > 0) {
            amount -= moved;
            setChanged();
        }
    }

    private void drip(ServerLevel level, BlockState state) {
        Direction facing = state.getValue(TreeTapBlock.FACING);
        double x = worldPosition.getX() + 0.5 - facing.getStepX() * 3 / 16.0;
        double z = worldPosition.getZ() + 0.5 - facing.getStepZ() * 3 / 16.0;
        double y = worldPosition.getY() + 9 / 16.0;
        level.sendParticles(Tier5Particles.LATEX_DRIP.get(), x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        level.playSound(null, worldPosition, Tier5Sounds.TREE_TAP_DRIP.get(), SoundSource.BLOCKS, 0.35f, 0.9f + level.getRandom().nextFloat() * 0.2f);
    }

    public boolean full() {
        return amount >= CUP;
    }

    public int amount() {
        return amount;
    }

    public Status status() {
        return status;
    }

    /** Fills the cup directly, for game tests. */
    public void setAmount(int mb) {
        amount = Math.max(0, Math.min(CUP, mb));
        if (amount > 0 && fluid == Fluids.EMPTY) fluid = Tier5Fluids.LATEX.get();
        setChanged();
    }

    /** Empties a full cup into a bucket of its fluid, or returns nothing when the fluid has no bucket. */
    public ItemStack takeBucket() {
        if (amount < CUP) return ItemStack.EMPTY;
        Item bucket = fluid.getBucket();
        if (bucket == null || bucket == Items.AIR) return ItemStack.EMPTY;
        amount -= CUP;
        setChanged();
        return new ItemStack(bucket);
    }

    /** "Latex: 420 / 1000 mB", or what stops the tap. */
    public Component statusLine() {
        if (status != Status.WORKING) return Component.translatable(status.key());
        Fluid shown = fluid == Fluids.EMPTY ? Tier5Fluids.LATEX.get() : fluid;
        return Component.translatable(StrataIndustria.MOD_ID + ".tree_tap.cup", shown.getFluidType().getDescription(), amount, CUP);
    }

    @Override
    public boolean connectsFluid(Direction side) {
        return side == Direction.DOWN;
    }

    @Override
    public int fill(Direction side, Fluid fluid, int amount, float pressure, boolean simulate) {
        return 0;
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        amount = in.getIntOr("amount", 0);
        placedAt = in.getLongOr("placed_at", -1L);
        fluid = in.read("fluid", BuiltInRegistries.FLUID.byNameCodec()).orElse(Fluids.EMPTY);
        if (amount > 0 && fluid == Fluids.EMPTY) fluid = Tier5Fluids.LATEX.get();
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("amount", amount);
        out.putLong("placed_at", placedAt);
        if (fluid != Fluids.EMPTY) out.store("fluid", BuiltInRegistries.FLUID.byNameCodec(), fluid);
    }
}
