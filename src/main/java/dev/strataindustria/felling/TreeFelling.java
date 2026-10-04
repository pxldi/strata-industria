package dev.strataindustria.felling;

import com.mojang.math.Transformation;
import dev.strataindustria.Config;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.ModTags;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Felling trees (redesign R5). Chopping the bottom log of a natural tree with an axe cuts a notch that grows with
 * every blow: a pale wedge appears on the cut face and widens, chips fly, the axe thunks deeper, and the trunk
 * starts to creak. The last blow fells the whole tree (see {@link FallingTree}). Nothing is ever lost by chopping:
 * a notch only heals if the tree is left alone for a good while. Sneaking chops a single log, and logs a player
 * placed (no natural leaves on them) always break one at a time.
 */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class TreeFelling {
    /** Blows to fell a tree: three notches, then the fall. */
    public static final int BLOWS = 4;
    /** Ticks a notch stays before the tree heals over. */
    static final long HEAL_TICKS = 6000;
    private static final float[] WIDTH = {0.0f, 0.38f, 0.66f, 0.96f};
    private static final float[] HEIGHT = {0.0f, 0.24f, 0.4f, 0.56f};

    private record Key(ResourceKey<Level> dimension, long anchor) {}

    private static final class Notch {
        int stage;
        long lastBlow;
        UUID wedge;
    }

    private static final Map<Key, Notch> NOTCHES = new HashMap<>();

    private TreeFelling() {}

    @SubscribeEvent
    static void onBreak(BreakBlockEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel)) return;
        if (chop(player, event.getPos())) event.setCanceled(true);
    }

    /** One axe blow at a log. Returns true when it was a felling blow, so the log itself is not broken. */
    public static boolean chop(ServerPlayer player, BlockPos pos) {
        ServerLevel level = (ServerLevel) player.level();
        if (!Config.FELLING.getAsBoolean() || player.isCreative() || player.isShiftKeyDown()) return false;
        ItemStack axe = player.getMainHandItem();
        if (!axe.is(ModTags.Items.AXES)) return false;
        TreeScan.Tree tree = TreeScan.scan(level, pos);
        if (tree == null) return false;

        long now = level.getGameTime();
        Key key = new Key(level.dimension(), tree.anchor().asLong());
        Notch notch = NOTCHES.computeIfAbsent(key, k -> new Notch());
        if (now - notch.lastBlow > HEAL_TICKS) notch.stage = 0;
        notch.lastBlow = now;
        notch.stage++;
        Direction facing = player.getDirection();
        BlockState log = level.getBlockState(pos);
        axe.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);

        if (notch.stage >= BLOWS) {
            NOTCHES.remove(key);
            discard(level, notch.wedge);
            fell(level, player, axe, tree, facing);
            return true;
        }
        bite(level, pos, log, notch, facing.getOpposite());
        return true;
    }

    /** The blow's feedback: the wedge grows, chips fly, the thunk deepens, and from the second notch the tree answers. */
    private static void bite(ServerLevel level, BlockPos pos, BlockState log, Notch notch, Direction face) {
        int stage = notch.stage;
        double fx = pos.getX() + 0.5 + face.getStepX() * 0.52;
        double fz = pos.getZ() + 0.5 + face.getStepZ() * 0.52;
        double fy = pos.getY() + 0.5;
        notch.wedge = wedge(level, notch.wedge, pos, log, face, stage);
        level.playSound(null, pos, FellingSounds.NOTCH.get(), SoundSource.BLOCKS, 0.9f + 0.15f * stage, 1.15f - 0.13f * stage);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, log), fx, fy, fz, 5 + 4 * stage, 0.12, 0.14, 0.12, 0.09);
        if (stage >= 2) {
            level.playSound(null, pos, FellingSounds.CREAK.get(), SoundSource.BLOCKS, 0.7f + 0.5f * (stage - 2), 1.0f - 0.2f * (stage - 2));
            leafFall(level, pos, stage == 2 ? 3 : 8);
        }
    }

    /** A few leaves shake loose from the crown. */
    private static void leafFall(ServerLevel level, BlockPos pos, int count) {
        TreeScan.Tree tree = TreeScan.scan(level, pos);
        if (tree == null || tree.leaves().isEmpty()) return;
        for (int i = 0; i < count; i++) {
            BlockPos leaf = tree.leaves().get(level.getRandom().nextInt(tree.leaves().size()));
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(leaf)), leaf.getX() + 0.5, leaf.getY() + 0.2, leaf.getZ() + 0.5, 3, 0.35, 0.2, 0.35, 0.03);
        }
    }

    private static void fell(ServerLevel level, ServerPlayer player, ItemStack axe, TreeScan.Tree tree, Direction facing) {
        FallingTree falling = new FallingTree(level, tree.start(), facing);
        int logs = tree.logs().size();
        int leaves = tree.leaves().size();
        // Loot is rolled before the blocks go: the axe for the logs, bare hands for the leaves.
        List<List<ItemStack>> logDrops = new ArrayList<>();
        List<BlockState> logStates = new ArrayList<>();
        for (BlockPos pos : tree.logs()) {
            BlockState state = level.getBlockState(pos);
            logStates.add(state);
            logDrops.add(Block.getDrops(state, level, pos, null, player, axe));
        }
        List<List<ItemStack>> leafDrops = new ArrayList<>();
        List<BlockState> leafStates = new ArrayList<>();
        for (BlockPos pos : tree.leaves()) {
            BlockState state = level.getBlockState(pos);
            leafStates.add(state);
            leafDrops.add(Block.getDrops(state, level, pos, null, player, ItemStack.EMPTY));
        }
        for (int i = 0; i < logs; i++) {
            BlockPos pos = tree.logs().get(i);
            falling.add(pos, logStates.get(i), false, logDrops.get(i), i, logs);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        for (int i = 0; i < leaves; i++) {
            BlockPos pos = tree.leaves().get(i);
            falling.add(pos, leafStates.get(i), true, leafDrops.get(i), i, leaves);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        falling.addExtra(new ItemStack(Items.STICK, 1 + logs / 2));
        falling.addExtra(new ItemStack(ModItems.BARK.get(), 1 + logs / 3));
        falling.launch();
    }

    // ---------------------------------------------------------------- the notch you see

    /** The cut: a pale wedge of stripped wood on the struck face, wider and taller with every blow. */
    private static UUID wedge(ServerLevel level, UUID existing, BlockPos pos, BlockState log, Direction face, int stage) {
        Entity found = existing == null ? null : level.getEntity(existing);
        Display.BlockDisplay display = found instanceof Display.BlockDisplay d ? d : null;
        Vec3 origin = new Vec3(pos.getX() + 0.5 + face.getStepX() * 0.5, pos.getY(), pos.getZ() + 0.5 + face.getStepZ() * 0.5);
        if (display == null) {
            display = net.minecraft.world.entity.EntityTypes.BLOCK_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
            if (display == null) return null;
            display.setPos(origin.x, origin.y, origin.z);
            level.addFreshEntity(display);
        }
        float w = WIDTH[stage];
        float h = HEIGHT[stage];
        float depth = 0.05f;
        // Local +z points out of the face; the box grows from a point on the face, so the bite seems to open.
        Quaternionf turn = new Quaternionf().rotationY((float) Math.atan2(face.getStepX(), face.getStepZ()));
        Vector3f shift = turn.transform(new Vector3f(-w / 2, 0.5f - h / 2, -0.015f));
        Transformation cut = new Transformation(shift, turn, new Vector3f(w, h, depth), new Quaternionf());
        TagValueOutput view = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        view.store("transformation", Transformation.EXTENDED_CODEC, cut);
        view.store("block_state", BlockState.CODEC, cutWood(log));
        view.putInt("interpolation_duration", 3);
        view.putInt("start_interpolation", 0);
        view.store("UUID", UUIDUtil.CODEC, display.getUUID());
        view.store("Pos", Vec3.CODEC, origin);
        CompoundTag tag = view.buildResult();
        ListTag tags = new ListTag();
        tags.add(StringTag.valueOf(FallingTree.TAG));
        tag.put("Tags", tags);
        display.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
        return display.getUUID();
    }

    /** What the cut shows: the stripped log of the same wood when there is one, else plain planks. */
    private static BlockState cutWood(BlockState log) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(log.getBlock());
        Identifier stripped = Identifier.fromNamespaceAndPath(id.getNamespace(), "stripped_" + id.getPath());
        return BuiltInRegistries.BLOCK.getOptional(stripped).map(Block::defaultBlockState).orElse(Blocks.OAK_PLANKS.defaultBlockState());
    }

    private static void discard(ServerLevel level, UUID id) {
        if (id == null) return;
        Entity entity = level.getEntity(id);
        if (entity != null) entity.discard();
    }

    /** Old notches heal, and their wedges go with them. */
    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || NOTCHES.isEmpty() || level.getGameTime() % 200 != 0) return;
        long now = level.getGameTime();
        Iterator<Map.Entry<Key, Notch>> it = NOTCHES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Key, Notch> entry = it.next();
            if (!entry.getKey().dimension().equals(level.dimension())) continue;
            BlockPos anchor = BlockPos.of(entry.getKey().anchor());
            boolean gone = !level.getBlockState(anchor).is(net.minecraft.tags.BlockTags.LOGS);
            if (gone || now - entry.getValue().lastBlow > HEAL_TICKS) {
                discard(level, entry.getValue().wedge);
                it.remove();
            }
        }
    }

    @SubscribeEvent
    static void onStopped(ServerStoppedEvent event) {
        NOTCHES.clear();
    }

    /** Test hooks. */
    static int notchStage(ServerLevel level, BlockPos anchor) {
        Notch notch = NOTCHES.get(new Key(level.dimension(), anchor.asLong()));
        return notch == null ? 0 : notch.stage;
    }

    public static void reset() {
        NOTCHES.clear();
    }
}
