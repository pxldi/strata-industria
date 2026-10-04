package dev.strataindustria.felling;

import com.mojang.math.Transformation;
import dev.strataindustria.StrataIndustria;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A felled tree tipping over (redesign R5). The blocks are already gone from the world; their images are vanilla
 * block displays that are never saved, tilted about the foot of the trunk in a few interpolated steps: a slow lean
 * and creak, then gathering speed, then the crash, with the drops hopping out where each log lands.
 */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class FallingTree {
    static final String TAG = StrataIndustria.MOD_ID + ".falling_tree";
    /** Tick, angle in degrees and interpolation length of each step of the fall. */
    private static final int[] AT = {1, 15, 22, 28};
    private static final float[] DEGREES = {5.0f, 18.0f, 50.0f, 90.0f};
    private static final int[] LENGTH = {14, 7, 6, 5};
    private static final int CRASH_AT = 33;
    private static final int GONE_AT = 46;
    static final int MAX_LOG_PIECES = 120;
    static final int MAX_LEAF_PIECES = 170;

    private static final List<FallingTree> ACTIVE = new ArrayList<>();

    private record Piece(UUID id, BlockPos pos, Vector3f rel, BlockState state, boolean leaf) {}

    private final ServerLevel level;
    private final Vec3 pivot;
    private final Direction dir;
    private final long start;
    private final List<Piece> pieces = new ArrayList<>();
    private final List<ItemStack> logDrops = new ArrayList<>();
    private final List<Vector3f> logLandings = new ArrayList<>();
    private final List<ItemStack> otherDrops = new ArrayList<>();
    private final Vector3f axis;
    private int step;
    private boolean crashed;

    FallingTree(ServerLevel level, BlockPos foot, Direction dir) {
        this.level = level;
        this.dir = dir;
        this.pivot = new Vec3(foot.getX() + 0.5 + dir.getStepX() * 0.5, foot.getY(), foot.getZ() + 0.5 + dir.getStepZ() * 0.5);
        this.start = level.getGameTime();
        // Up crossed with the direction of the fall: turning about it tips the crown over the ground ahead.
        this.axis = new Vector3f(dir.getStepZ(), 0.0f, -dir.getStepX());
    }

    /** Adds the image of one block that was just removed; drops are what its loot table gave. */
    void add(BlockPos pos, BlockState state, boolean leaf, List<ItemStack> drops, int index, int total) {
        Vector3f rel = new Vector3f((float) (pos.getX() - pivot.x), (float) (pos.getY() - pivot.y), (float) (pos.getZ() - pivot.z));
        if (leaf) otherDrops.addAll(drops);
        else {
            logDrops.addAll(drops);
            for (int i = 0; i < drops.size(); i++) logLandings.add(landing(rel));
        }
        int cap = leaf ? MAX_LEAF_PIECES : MAX_LOG_PIECES;
        // Beyond the cap the images thin out evenly; the blocks and their drops are all accounted for.
        if (total > cap && (long) index * cap / total == (long) (index + 1) * cap / total) return;
        Display.BlockDisplay display = net.minecraft.world.entity.EntityTypes.BLOCK_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
        if (display == null) return;
        display.setPos(pos.getX(), pos.getY(), pos.getZ());
        level.addFreshEntity(display);
        load(display, pos, state, rel, 0.0f, 0);
        pieces.add(new Piece(display.getUUID(), pos.immutable(), rel, state, leaf));
    }

    /** Where the centre of a block that stood at {@code rel} from the foot ends up once the tree lies flat. */
    private Vector3f landing(Vector3f rel) {
        Vector3f centre = new Vector3f(rel).add(0.5f, 0.5f, 0.5f);
        new Quaternionf().rotationAxis((float) Math.PI / 2, axis.x, axis.y, axis.z).transform(centre);
        return centre.add((float) pivot.x, (float) pivot.y, (float) pivot.z);
    }

    void launch() {
        ACTIVE.add(this);
    }

    private void load(Display.BlockDisplay display, BlockPos pos, BlockState state, Vector3f rel, float angle, int duration) {
        Quaternionf turn = new Quaternionf().rotationAxis(angle, axis.x, axis.y, axis.z);
        Vector3f shift = turn.transform(new Vector3f(rel)).sub(rel);
        Transformation tilt = new Transformation(shift, turn, new Vector3f(1.0f, 1.0f, 1.0f), new Quaternionf());
        TagValueOutput view = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        view.store("transformation", Transformation.EXTENDED_CODEC, tilt);
        view.store("block_state", BlockState.CODEC, state);
        view.putInt("interpolation_duration", duration);
        view.putInt("start_interpolation", 0);
        view.store("UUID", UUIDUtil.CODEC, display.getUUID());
        view.store("Pos", Vec3.CODEC, new Vec3(pos.getX(), pos.getY(), pos.getZ()));
        CompoundTag tag = view.buildResult();
        ListTag tags = new ListTag();
        tags.add(StringTag.valueOf(TAG));
        tag.put("Tags", tags);
        display.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
    }

    private void tiltTo(float degrees, int duration) {
        for (Piece piece : pieces) {
            if (level.getEntity(piece.id) instanceof Display.BlockDisplay display) {
                load(display, piece.pos, piece.state, piece.rel, (float) Math.toRadians(degrees), duration);
            }
        }
    }

    private void tick(long now) {
        int t = (int) (now - start);
        while (step < AT.length && t >= AT[step]) {
            tiltTo(DEGREES[step], LENGTH[step]);
            Vec3 crown = crown();
            if (step == 0) sound(FellingSounds.CREAK.get(), crown, 1.6f, 0.9f);
            if (step == 1) {
                sound(FellingSounds.CREAK.get(), crown, 2.0f, 0.75f);
                leaves(crown, 14);
            }
            step++;
        }
        if (!crashed && t >= CRASH_AT) crash();
        if (t >= GONE_AT) clear();
    }

    private Vec3 crown() {
        double top = 0;
        for (Piece piece : pieces) top = Math.max(top, piece.rel.y);
        return pivot.add(0.0, top * 0.8, 0.0);
    }

    private void sound(net.minecraft.sounds.SoundEvent event, Vec3 at, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, event, SoundSource.BLOCKS, volume, pitch);
    }

    private void leaves(Vec3 at, int count) {
        for (Piece piece : pieces) {
            if (!piece.leaf || count <= 0) continue;
            if (level.getRandom().nextInt(6) != 0) continue;
            Vector3f p = piece.rel;
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, piece.state), pivot.x + p.x + 0.5, pivot.y + p.y + 0.5, pivot.z + p.z + 0.5, 4, 0.4, 0.4, 0.4, 0.05);
            count--;
        }
    }

    private void crash() {
        crashed = true;
        double length = 0;
        for (Piece piece : pieces) length = Math.max(length, piece.rel.y + 1.0);
        Vec3 middle = pivot.add(dir.getStepX() * length * 0.5, 0.3, dir.getStepZ() * length * 0.5);
        sound(FellingSounds.CRASH.get(), middle, 3.0f, 0.95f);
        int burst = 0;
        for (Piece piece : pieces) {
            Vector3f at = landing(piece.rel);
            if (piece.leaf) {
                if (burst++ % 2 == 0) level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, piece.state), at.x, at.y + 0.4, at.z, 5, 0.5, 0.4, 0.5, 0.12);
            } else if (burst % 3 == 0) {
                level.sendParticles(ParticleTypes.POOF, at.x, groundY(at.x, at.z, 0.2), at.z, 3, 0.4, 0.05, 0.4, 0.02);
            }
        }
        for (int i = 0; i < logDrops.size(); i++) {
            Vector3f at = logLandings.get(i);
            spawn(logDrops.get(i), at.x, at.z);
        }
        Vec3 crown = pivot.add(dir.getStepX() * length * 0.7, 0.0, dir.getStepZ() * length * 0.7);
        for (ItemStack drop : otherDrops) {
            spawn(drop, crown.x + (level.getRandom().nextDouble() - 0.5) * 3.0, crown.z + (level.getRandom().nextDouble() - 0.5) * 3.0);
        }
    }

    /** Extra pieces that are part of the fall: sticks and bark where the trunk lay. */
    void addExtra(ItemStack stack) {
        otherDrops.add(stack);
    }

    private void spawn(ItemStack stack, double x, double z) {
        if (stack.isEmpty()) return;
        ItemEntity item = new ItemEntity(level, x, groundY(x, z, 0.35), z, stack.copy(), (level.getRandom().nextDouble() - 0.5) * 0.12, 0.18, (level.getRandom().nextDouble() - 0.5) * 0.12);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
    }

    private double groundY(double x, double z, double lift) {
        BlockPos at = BlockPos.containing(x, pivot.y, z);
        if (!level.hasChunkAt(at)) return pivot.y + lift;
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ()) + lift;
    }

    private void clear() {
        for (Piece piece : pieces) {
            Entity entity = level.getEntity(piece.id);
            if (entity != null) entity.discard();
        }
        pieces.clear();
        ACTIVE.remove(this);
    }

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || ACTIVE.isEmpty()) return;
        long now = level.getGameTime();
        for (FallingTree tree : List.copyOf(ACTIVE)) {
            if (tree.level == level) tree.tick(now);
        }
    }

    /** A stop mid-fall must not lose the logs: everything lands at once. */
    @SubscribeEvent
    static void onStopping(ServerStoppingEvent event) {
        for (FallingTree tree : List.copyOf(ACTIVE)) {
            if (!tree.crashed) tree.crash();
            tree.clear();
        }
        ACTIVE.clear();
    }

    /** Pieces left in a saved chunk by a crash are not wanted. */
    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() && event.getEntity().entityTags().contains(TAG)) event.setCanceled(true);
    }

    /** Test hook: runs the whole fall at once. */
    static void finishAll(ServerLevel level) {
        for (FallingTree tree : List.copyOf(ACTIVE)) {
            if (tree.level != level) continue;
            if (!tree.crashed) tree.crash();
            tree.clear();
        }
    }

    static int active() {
        return ACTIVE.size();
    }
}
