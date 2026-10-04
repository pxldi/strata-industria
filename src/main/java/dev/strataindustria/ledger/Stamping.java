package dev.strataindustria.ledger;

import dev.strataindustria.StrataIndustria;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * A multiblock being stamped: the ledger sets one block every few ticks, taking each from a builder's crate
 * within reach of the controller and then from the player's own pockets. It stops when the blocks run out.
 */
public final class Stamping {
    static final int REACH = 8;
    static final int INTERVAL = 4;
    private static final List<Stamping> JOBS = new ArrayList<>();

    private final ServerLevel level;
    private final UUID player;
    private final BlockPos controller;
    private final ArrayDeque<Plan.Part> queue = new ArrayDeque<>();
    private final List<BlockPos> crates = new ArrayList<>();
    private int placed, inTheWay;

    private Stamping(ServerLevel level, UUID player, BlockPos controller) {
        this.level = level;
        this.player = player;
        this.controller = controller;
    }

    /** Starts stamping, or says why not. Returns the job so tests can drive it. */
    public static @Nullable Stamping start(ServerPlayer player, Plan plan, BlockPos controller, net.minecraft.core.Direction facing) {
        ServerLevel level = player.level();
        for (Stamping job : JOBS) {
            if (job.level == level && job.controller.equals(controller)) {
                say(player, "busy");
                return null;
            }
        }
        Stamping job = new Stamping(level, player.getUUID(), controller);
        for (Plan.Part part : plan.parts(level, controller, facing)) {
            BlockState there = level.getBlockState(part.pos());
            if (there.is(part.state().getBlock())) continue;
            if (!there.isAir() && !there.canBeReplaced()) job.inTheWay++;
            else job.queue.add(part);
        }
        if (job.queue.isEmpty()) {
            say(player, job.inTheWay > 0 ? "in_the_way" : "nothing_to_do");
            return null;
        }
        for (BlockPos pos : BlockPos.betweenClosed(controller.offset(-REACH, -REACH, -REACH), controller.offset(REACH, REACH, REACH))) {
            if (level.getBlockEntity(pos) instanceof BuilderCrateBlockEntity) job.crates.add(pos.immutable());
        }
        JOBS.add(job);
        return job;
    }

    static void tick(ServerLevel level) {
        if (JOBS.isEmpty() || level.getGameTime() % INTERVAL != 0) return;
        JOBS.removeIf(job -> job.level == level && job.step());
    }

    static void clear() {
        JOBS.clear();
    }

    /** Places one block. Returns true when the job is over. */
    boolean step() {
        Plan.Part part = queue.poll();
        if (part == null) return true;
        BlockState there = level.getBlockState(part.pos());
        if (there.is(part.state().getBlock())) return queue.isEmpty() && finish();
        if (!there.isAir() && !there.canBeReplaced()) {
            inTheWay++;
            return queue.isEmpty() && finish();
        }
        Item item = part.state().getBlock().asItem();
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(player);
        if (!take(item, owner)) {
            int missing = 1;
            for (Plan.Part rest : queue) if (rest.state().getBlock().asItem() == item) missing++;
            level.playSound(null, controller, LedgerRegistry.SHORT.get(), SoundSource.BLOCKS, 0.7f, 1.0f);
            if (owner != null) {
                owner.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".ledger.short", missing, item.getName(new ItemStack(item))));
            }
            return true;
        }
        level.setBlock(part.pos(), part.state(), 3);
        placed++;
        float pitch = 0.9f + level.getRandom().nextFloat() * 0.2f;
        level.playSound(null, part.pos(), LedgerRegistry.STAMP.get(), SoundSource.BLOCKS, 0.5f, pitch);
        level.playSound(null, part.pos(), part.state().getSoundType().getPlaceSound(), SoundSource.BLOCKS, 0.6f, pitch);
        return queue.isEmpty() && finish();
    }

    private boolean finish() {
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(player);
        if (owner != null) {
            owner.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + (inTheWay > 0 ? ".ledger.done_in_the_way" : ".ledger.done"),
                    placed, inTheWay));
        }
        return true;
    }

    /** One of {@code item} out of a crate in reach, or out of the player's inventory. */
    private boolean take(Item item, @Nullable Player owner) {
        if (owner != null && owner.hasInfiniteMaterials()) return true;
        for (BlockPos pos : crates) {
            if (!(level.getBlockEntity(pos) instanceof BuilderCrateBlockEntity crate)) continue;
            for (int slot = 0; slot < crate.getContainerSize(); slot++) {
                ItemStack stack = crate.getItem(slot);
                if (stack.is(item)) {
                    stack.shrink(1);
                    crate.setChanged();
                    return true;
                }
            }
        }
        if (owner == null) return false;
        for (int slot = 0; slot < owner.getInventory().getContainerSize(); slot++) {
            ItemStack stack = owner.getInventory().getItem(slot);
            if (stack.is(item)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    public int placed() {
        return placed;
    }

    /** Whether a job is stamping around this controller. */
    public static boolean active(ServerLevel level, BlockPos controller) {
        return JOBS.stream().anyMatch(job -> job.level == level && job.controller.equals(controller));
    }

    /** Runs the job to its end at once, for tests. Returns the blocks it placed. */
    public int runToEnd() {
        while (!step()) {
            // one block per step
        }
        JOBS.remove(this);
        return placed;
    }

    static void say(ServerPlayer player, String key) {
        player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".ledger." + key));
    }
}
