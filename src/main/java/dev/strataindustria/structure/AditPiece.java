package dev.strataindustria.structure;

import dev.strataindustria.geology.GeologyContext;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.geology.VeinCells;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.survey.Surveyor;
import java.util.ArrayList;
import java.util.List;
import dev.strataindustria.registry.ModItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * A timbered tunnel driven toward a vein (structures spec 3.6). Three wide and three high, with a set of
 * pit props in the walls and a lintel every third block. It runs straight from its portal, sinking one
 * block in three, turns once if the vein lies to the side, and stops just inside the vein's poor rim: the
 * miners took the easy ore at the edge and left the rich core. Where the vein lies too deep for a sloping
 * tunnel, a laddered winze goes straight down to it.
 */
public class AditPiece extends StructurePiece {
    /** Normalised vein distance at which digging stops: just inside the rim. */
    static final double REACH = 0.85;
    static final int PROP_SPACING = 3;
    /** Deepest a winze is sunk before the vein is called out of reach. */
    static final int MAX_WINZE = 24;

    static final int VERTICAL = 1, COLLAPSE = 2, PROPS = 4;
    private static final int STRIDE = 5;

    /** Steps as (x, floor y, z, heading, flags). */
    private final int[] steps;
    private final OreMineral mineral;
    private final PlanPiece.Wood wood;
    private final int surfaceY;
    private final long seed;
    private final boolean cache;
    /** Which way from a shaft's middle its ladder hangs. */
    private final Direction ladderSide;
    /** Loot plan id for the suspicious gravel in a collapse. */
    private final String digPlan;

    AditPiece(int[] steps, OreMineral mineral, PlanPiece.Wood wood, int surfaceY, long seed, boolean cache,
            Direction ladderSide, String digPlan) {
        super(StructureContent.ADIT_PIECE.get(), 0, box(steps));
        this.steps = steps;
        this.mineral = mineral;
        this.wood = wood;
        this.surfaceY = surfaceY;
        this.seed = seed;
        this.cache = cache;
        this.ladderSide = ladderSide;
        this.digPlan = digPlan;
    }

    public AditPiece(CompoundTag tag) {
        super(StructureContent.ADIT_PIECE.get(), tag);
        this.steps = tag.getIntArray("Steps").orElse(new int[0]);
        OreMineral m = Surveyor.mineral(tag.getStringOr("Mineral", ""));
        this.mineral = m == null ? OreMineral.MALACHITE : m;
        this.wood = PlanPiece.Wood.values()[tag.getIntOr("Wood", 0)];
        this.surfaceY = tag.getIntOr("Surface", 64);
        this.seed = tag.getLongOr("Seed", 0L);
        this.cache = tag.getBooleanOr("Cache", false);
        this.ladderSide = Direction.from3DDataValue(tag.getIntOr("Ladder", Direction.NORTH.get3DDataValue()));
        this.digPlan = tag.getStringOr("Dig", "adit/mining");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putIntArray("Steps", steps);
        tag.putString("Mineral", mineral.id());
        tag.putInt("Wood", wood.ordinal());
        tag.putInt("Surface", surfaceY);
        tag.putLong("Seed", seed);
        tag.putBoolean("Cache", cache);
        tag.putInt("Ladder", ladderSide.get3DDataValue());
        tag.putString("Dig", digPlan);
    }

    /** A straight propped tunnel from a portal at the surface, sinking one block in three. For tests. */
    public static AditPiece straight(BlockPos portal, Direction heading, int length) {
        int[] flat = new int[length * STRIDE];
        for (int i = 0; i < length; i++) {
            int at = i * STRIDE;
            flat[at] = portal.getX() + heading.getStepX() * i;
            flat[at + 1] = portal.getY() - i / 3;
            flat[at + 2] = portal.getZ() + heading.getStepZ() * i;
            flat[at + 3] = heading.get3DDataValue();
            flat[at + 4] = PROPS;
        }
        return new AditPiece(flat, OreMineral.MALACHITE, PlanPiece.Wood.OAK, portal.getY(), 7L, false, heading.getClockWise(),
                "adit/mining");
    }

    /** The older, collapsed adit's tunnel, for tests: {@link #straight} with the miner's end and old weathering. */
    public static AditPiece ruined(BlockPos portal, Direction heading, int length) {
        AditPiece p = straight(portal, heading, length);
        return new AditPiece(p.steps, p.mineral, p.wood, p.surfaceY, p.seed, false, p.ladderSide, "adit/collapsed");
    }

    private static BoundingBox box(int[] steps) {
        BoundingBox box = null;
        for (int i = 0; i < steps.length; i += STRIDE) {
            BoundingBox cell = new BoundingBox(steps[i] - 3, steps[i + 1], steps[i + 2] - 3, steps[i] + 3, steps[i + 1] + 4, steps[i + 2] + 3);
            box = box == null ? cell : box.encapsulate(cell);
        }
        return box == null ? new BoundingBox(0, 0, 0, 0, 0, 0) : box;
    }

    // ---------------------------------------------------------------- planning

    /** Options for {@link #plan}. */
    record Route(int maxLength, boolean collapse, boolean cache, String digPlan) {}

    /**
     * Lays out a tunnel from {@code start} (the floor of its first block) heading toward the vein. Empty if
     * the vein cannot be reached within {@code route.maxLength()}, in which case the site is not used:
     * an adit that leads nowhere would teach the wrong lesson.
     *
     * @param shaftDepth when above 0, the tunnel starts with a shaft this deep from {@code start}
     */
    static Optional<AditPiece> plan(VeinCells.Vein vein, BlockPos start, Direction heading, int shaftDepth, Direction ladderSide,
            Route route, OreMineral mineral, PlanPiece.Wood wood, RandomSource random) {
        List<int[]> out = new ArrayList<>();
        int x = start.getX(), y = start.getY(), z = start.getZ();
        Direction h = heading;
        for (int i = 0; i < shaftDepth; i++) {
            out.add(new int[] {x, y, z, h.get3DDataValue(), VERTICAL});
            y--;
        }
        boolean reached = false, turned = false;
        for (int i = 0; i < route.maxLength() && !reached; i++) {
            boolean props = i > 0 && i % PROP_SPACING == 0;
            out.add(new int[] {x, y, z, h.get3DDataValue(), props ? PROPS : 0});
            if (VeinCells.distance(vein, x, y + 2, z) <= REACH) {
                reached = true;
                break;
            }
            int along = (vein.x() - x) * h.getStepX() + (vein.z() - z) * h.getStepZ();
            if (along <= 0) {
                Direction side = h.getClockWise();
                int lateral = (vein.x() - x) * side.getStepX() + (vein.z() - z) * side.getStepZ();
                if (!turned && lateral != 0) {
                    h = lateral > 0 ? side : side.getOpposite();
                    turned = true;
                } else {
                    // Straight above the vein and still not in it: sink a winze.
                    for (int k = 0; k < MAX_WINZE && !reached; k++) {
                        y--;
                        out.add(new int[] {x, y, z, h.get3DDataValue(), VERTICAL});
                        reached = VeinCells.distance(vein, x, y + 2, z) <= REACH;
                    }
                    break;
                }
            }
            if (y + 2 > vein.y() && i % PROP_SPACING == PROP_SPACING - 1) y--;
            x += h.getStepX();
            z += h.getStepZ();
        }
        if (!reached) return Optional.empty();

        // A stretch of fallen roof, never in the first few blocks.
        int horizontal = (int) out.stream().filter(s -> (s[4] & VERTICAL) == 0).count();
        if (route.collapse() && horizontal >= 10) {
            int from = shaftDepth + 4 + random.nextInt(horizontal - 8);
            int length = 2 + random.nextInt(2);
            for (int i = from; i < Math.min(out.size() - 2, from + length); i++) {
                if ((out.get(i)[4] & VERTICAL) == 0) out.get(i)[4] = COLLAPSE;
            }
        }
        int[] flat = new int[out.size() * STRIDE];
        for (int i = 0; i < out.size(); i++) System.arraycopy(out.get(i), 0, flat, i * STRIDE, STRIDE);
        return Optional.of(new AditPiece(flat, mineral, wood, vein.surfaceY(), random.nextLong(), route.cache(), ladderSide,
                route.digPlan()));
    }

    // ---------------------------------------------------------------- building

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
            BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos) {
        GeologyContext geology = StructureGeology.of(level);
        int count = steps.length / STRIDE;
        // Where the tunnel or its shaft comes up near the surface, any tree over it goes whole.
        for (int i = 0; i < count; i++) {
            int y = steps[i * STRIDE + 1];
            if (y + 4 < surfaceY - 2) continue;
            int x = steps[i * STRIDE], z = steps[i * STRIDE + 2];
            TreeClearing.clear(level, chunkBB, x - 3, z - 3, x + 3, z + 3, y, surfaceY + 8, pos -> false);
        }
        // The last horizontal steps are the worked face: dug, but not yet propped.
        int lastHorizontal = -1;
        for (int i = 0; i < count; i++) if ((steps[i * STRIDE + 4] & VERTICAL) == 0) lastHorizontal = i;
        // Vertical steps before the first level one are the entry shaft; later ones are a winze.
        int lastShaftStep = -1;
        while (lastShaftStep + 1 < count && (steps[(lastShaftStep + 1) * STRIDE + 4] & VERTICAL) != 0) lastShaftStep++;
        boolean digPlaced = false;
        for (int i = 0; i < count; i++) {
            int x = steps[i * STRIDE], y = steps[i * STRIDE + 1], z = steps[i * STRIDE + 2];
            Direction h = Direction.from3DDataValue(steps[i * STRIDE + 3]);
            int flags = steps[i * STRIDE + 4];
            if ((flags & VERTICAL) != 0) {
                // A winze below the tunnel hangs its ladder on the wall ahead, away from the tunnel it opens off.
                shaft(level, chunkBB, x, y, z, i > lastShaftStep ? h : ladderSide);
                continue;
            }
            Direction side = h.getClockWise();
            if ((flags & COLLAPSE) != 0) {
                collapse(level, chunkBB, x, y, z, side, !digPlaced, geology);
                digPlaced = true;
                continue;
            }
            carve(level, chunkBB, x, y, z, side);
            if (ruin()) weather(level, chunkBB, x, y, z, side, i, geology);
            if ((flags & PROPS) != 0 && i < lastHorizontal - 1 && !Weathering.chance(new BlockPos(x, y, z), seed, 0.06)) {
                props(level, chunkBB, x, y, z, side);
            }
            if (i == lastHorizontal && i == count - 1) face(level, random, chunkBB, x, y, z, side, geology);
        }
        // A tunnel that ends in a winze is worked at the bottom of it; a face cut beside the last level
        // step would only be dug away again by the winze.
        int last = count - 1;
        if (last >= 0 && last != lastHorizontal) {
            Direction h = Direction.from3DDataValue(steps[last * STRIDE + 3]);
            face(level, random, chunkBB, steps[last * STRIDE], steps[last * STRIDE + 1], steps[last * STRIDE + 2], h.getClockWise(), geology);
        }
    }

    /** The older adit of structures v2 6.4: cracked walls, rubble, roots, and the miner's end. */
    private boolean ruin() {
        return "adit/collapsed".equals(digPlan);
    }

    /** Old rock and old damp: cracked wall blocks, rubble on the floor, roots through the first stretch of roof, webs. */
    private void weather(WorldGenLevel level, BoundingBox chunkBB, int x, int y, int z, Direction side, int index, GeologyContext geology) {
        Rock rock = geology == null ? Rock.GRANITE : geology.sampler().rockAt(x, y + 2, z, surfaceY);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int o = -2; o <= 2; o += 4) {
            for (int dy = 1; dy <= 3; dy++) {
                pos.set(x + side.getStepX() * o, y + dy, z + side.getStepZ() * o);
                if (Terrain.isGround(level.getBlockState(pos)) && Weathering.chance(pos, seed, 0.22)) {
                    set(level, chunkBB, pos, SharedBlocks.CRACKED.get(rock).get().defaultBlockState());
                }
            }
        }
        int o = (int) (Weathering.hash(new BlockPos(x, y, z), seed) % 3) - 1;
        pos.set(x + side.getStepX() * o, y + 1, z + side.getStepZ() * o);
        if (level.getBlockState(pos).isAir() && Terrain.isGround(level.getBlockState(pos.below())) && Weathering.chance(pos, seed, 0.4)) {
            set(level, chunkBB, pos, SharedBlocks.RUBBLE.get(rock).get().defaultBlockState());
        }
        if (index < 8) {
            pos.set(x + side.getStepX() * -o, y + 3, z + side.getStepZ() * -o);
            if (level.getBlockState(pos).isAir() && Terrain.isGround(level.getBlockState(pos.above())) && Weathering.chance(pos, seed, 0.35)) {
                set(level, chunkBB, pos, Blocks.HANGING_ROOTS.defaultBlockState());
            }
        }
        pos.set(x + side.getStepX() * -o, y + 3, z + side.getStepZ() * -o);
        if (level.getBlockState(pos).isAir() && Terrain.isGround(level.getBlockState(pos.above())) && Weathering.chance(pos, seed, 0.06)) {
            set(level, chunkBB, pos, Blocks.COBWEB.defaultBlockState());
        }
    }

    /**
     * The miner's end: a slumped stand in a leather cap and tunic, his pack crate with the burnt-out lamp on
     * it, a bone half buried in rubble, a pick hung in the wall, and a cache behind two cracked blocks.
     */
    private void minersEnd(WorldGenLevel level, RandomSource random, BoundingBox chunkBB, int x, int y, int z, Direction h, Rock rock) {
        Direction side = h.getClockWise();
        BlockPos base = new BlockPos(x, y, z);
        BlockPos crate = base.relative(side).above();
        BlockPos stand = base.relative(side.getOpposite()).above();
        BlockState air = Blocks.CAVE_AIR.defaultBlockState();
        for (BlockPos p : new BlockPos[] {crate, stand, crate.above(), base.above()}) {
            if (chunkBB.isInside(p) && !isFluid(level, p)) set(level, chunkBB, p, air);
        }
        if (chunkBB.isInside(crate)) {
            set(level, chunkBB, crate, SharedBlocks.CRATE.get().defaultBlockState());
            RandomizableContainer.setBlockEntityLootTable(level, random, crate, CampLoot.barrel("adit/cache", mineral));
            set(level, chunkBB, crate.above(), SharedBlocks.MINERS_LAMP.get().defaultBlockState()
                    .setValue(MinersLampBlock.MODE, MinersLampBlock.Mode.OFF));
        }
        BlockPos rubble = base.above();
        if (chunkBB.isInside(rubble)) set(level, chunkBB, rubble, SharedBlocks.RUBBLE.get(rock).get().defaultBlockState());
        BlockPos bone = base.relative(h.getOpposite()).relative(side.getOpposite()).above();
        if (chunkBB.isInside(bone) && Terrain.isGround(level.getBlockState(bone.below()))) {
            set(level, chunkBB, bone, Blocks.BONE_BLOCK.defaultBlockState());
        }
        if (chunkBB.isInside(stand)) {
            ArmorStand dummy = new ArmorStand(level.getLevel(), stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5);
            {
                dummy.setYRot(h.toYRot());
                dummy.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
                dummy.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
                level.addFreshEntity(dummy);
            }
        }
        BlockPos rack = base.relative(h).above(2);
        if (chunkBB.isInside(rack) && Terrain.isGround(level.getBlockState(rack))) {
            set(level, chunkBB, rack, SharedBlocks.TOOL_RACK.get().defaultBlockState().setValue(ToolRackBlock.FACING, h.getOpposite()));
            ToolRackBlockEntity.stock(level.getLevel(), rack, List.of(new ItemStack(ModItems.STONE_PICKAXE.get())));
        }
        // The cache: two cracked blocks stacked in the side wall, a crate behind them.
        Direction wall = side.getOpposite();
        BlockPos lower = base.relative(wall, 2).above(), upper = lower.above(), cache = lower.relative(wall);
        if (chunkBB.isInside(lower) && chunkBB.isInside(upper) && chunkBB.isInside(cache) && Terrain.isGround(level.getBlockState(cache))) {
            BlockState cracked = SharedBlocks.CRACKED.get(rock).get().defaultBlockState();
            set(level, chunkBB, lower, cracked);
            set(level, chunkBB, upper, cracked);
            set(level, chunkBB, cache, SharedBlocks.CRATE.get().defaultBlockState());
            RandomizableContainer.setBlockEntityLootTable(level, random, cache, CampLoot.key(CampLoot.ADIT_HIDDEN, mineral));
        }
    }

    private static boolean isFluid(WorldGenLevel level, BlockPos pos) {
        return !level.getFluidState(pos).isEmpty();
    }

    private void set(WorldGenLevel level, BoundingBox chunkBB, BlockPos pos, BlockState state) {
        if (chunkBB.isInside(pos)) level.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }

    private void carve(WorldGenLevel level, BoundingBox chunkBB, int x, int y, int z, Direction side) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int o = -1; o <= 1; o++) {
            for (int dy = 1; dy <= 3; dy++) {
                pos.set(x + side.getStepX() * o, y + dy, z + side.getStepZ() * o);
                // The shaft's ladder runs down to the tunnel floor; leave it for the climb back out.
                BlockState state = level.getBlockState(pos);
                if (!chunkBB.isInside(pos) || isFluid(level, pos) || state.isAir() || state.is(Blocks.LADDER)) continue;
                level.setBlock(pos, Blocks.CAVE_AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    /** Two props set into the walls and a lintel across the roof, where there is rock to hold them. */
    private void props(WorldGenLevel level, BoundingBox chunkBB, int x, int y, int z, Direction side) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean both = true;
        for (int o = -2; o <= 2; o += 4) {
            for (int dy = 1; dy <= 3; dy++) {
                pos.set(x + side.getStepX() * o, y + dy, z + side.getStepZ() * o);
                if (!Terrain.isGround(level.getBlockState(pos))) {
                    both = false;
                    continue;
                }
                set(level, chunkBB, pos, StructureContent.PIT_PROP.get().defaultBlockState());
            }
        }
        if (!both) return;
        BlockState lintel = wood.stripped().setValue(RotatedPillarBlock.AXIS, side.getAxis());
        for (int o = -2; o <= 2; o++) {
            pos.set(x + side.getStepX() * o, y + 4, z + side.getStepZ() * o);
            if (Terrain.isGround(level.getBlockState(pos))) set(level, chunkBB, pos, lintel);
        }
    }

    /** Fallen roof: gravel and broken rock filling the tunnel, with one find in it. */
    private void collapse(WorldGenLevel level, BoundingBox chunkBB, int x, int y, int z, Direction side, boolean dig,
            GeologyContext geology) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        Rock rock = geology == null ? Rock.GRANITE : geology.sampler().rockAt(x, y + 2, z, surfaceY);
        for (int o = -1; o <= 1; o++) {
            for (int dy = 1; dy <= 3; dy++) {
                pos.set(x + side.getStepX() * o, y + dy, z + side.getStepZ() * o);
                if (!chunkBB.isInside(pos) || isFluid(level, pos)) continue;
                BlockState state = dy < 3 && Weathering.chance(pos, seed, 0.35)
                        ? ModBlocks.COBBLED_ROCK.get(rock).get().defaultBlockState()
                        : Blocks.GRAVEL.defaultBlockState();
                if (dig && o == 0 && dy == 1) state = Blocks.SUSPICIOUS_GRAVEL.defaultBlockState();
                level.setBlock(pos, state, Block.UPDATE_CLIENTS);
                if (state.is(Blocks.SUSPICIOUS_GRAVEL) && level.getBlockEntity(pos) instanceof BrushableBlockEntity brush) {
                    brush.setLootTable(CampLoot.dig(digPlan, mineral), Weathering.hash(pos, seed));
                }
            }
        }
    }

    /** The worked face: a few loose stones of the host rock, and in old adits the miners' cache. */
    private void face(WorldGenLevel level, RandomSource random, BoundingBox chunkBB, int x, int y, int z, Direction side,
            GeologyContext geology) {
        Rock rock = geology == null ? Rock.GRANITE : geology.sampler().rockAt(x, y, z, surfaceY);
        if (ruin()) {
            Direction heading = side.getCounterClockWise();
            minersEnd(level, random, chunkBB, x, y, z, heading, rock);
            return;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int o = -1; o <= 1; o += 2) {
            pos.set(x + side.getStepX() * o, y + 1, z + side.getStepZ() * o);
            if (!chunkBB.isInside(pos) || !level.getBlockState(pos).isAir() || !Terrain.isGround(level.getBlockState(pos.below()))) continue;
            if (cache && o == 1) {
                level.setBlock(pos, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP), Block.UPDATE_CLIENTS);
                RandomizableContainer.setBlockEntityLootTable(level, random, pos, CampLoot.barrel("adit/cache", mineral));
            } else if (Weathering.chance(pos, seed, 0.75)) {
                level.setBlock(pos, ModBlocks.BOULDER.get(rock).get().with(1, false, Direction.Plane.HORIZONTAL.getRandomDirection(random)), Block.UPDATE_CLIENTS);
            }
        }
    }

    /** One block of a laddered shaft, three by three. */
    private void shaft(WorldGenLevel level, BoundingBox chunkBB, int x, int y, int z, Direction ladderSide) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int ox = -1; ox <= 1; ox++) {
            for (int oz = -1; oz <= 1; oz++) {
                for (int dy = 1; dy <= 3; dy++) {
                    pos.set(x + ox, y + dy, z + oz);
                    if (!chunkBB.isInside(pos) || isFluid(level, pos)) continue;
                    boolean ladder = ox == ladderSide.getStepX() && oz == ladderSide.getStepZ();
                    BlockState state = ladder
                            ? Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, ladderSide.getOpposite())
                            : Blocks.CAVE_AIR.defaultBlockState();
                    level.setBlock(pos, state, Block.UPDATE_CLIENTS);
                }
            }
        }
    }
}
