package dev.strataindustria.structure;

import dev.strataindustria.charcoal.LogPileBlockEntity;
import dev.strataindustria.fire.FirePitBlockEntity;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.geology.StrataSampler;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.smithing.AnvilBlock;
import dev.strataindustria.survey.Surveyor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Builds one {@link Plan} into the world (structures spec 3.4 and 3.5): turned to face its camp, levelled
 * into the ground with a soft edge, built from the rock of the place it stands in, and a little weathered.
 */
public class PlanPiece extends StructurePiece {
    /** Width of the ring around a levelled plan that blends its ground into the land around it. */
    static final int MARGIN = 2;
    /** How far above a building trees and terrain are cleared. */
    static final int HEADROOM = 8;

    private final String planId;
    private final Plan plan;
    private final Rotation rotation;
    private final int minX, minZ, groundY;
    private final OreMineral mineral;
    private final Wood wood;
    private final long seed;

    /**
     * @param minX    west edge of the turned footprint
     * @param minZ    north edge of the turned footprint
     * @param groundY height of the plan's ground layer
     */
    public PlanPiece(Plan plan, Rotation rotation, int minX, int minZ, int groundY, OreMineral mineral, Wood wood, long seed) {
        super(StructureContent.PLAN_PIECE.get(), 0, box(plan, rotation, minX, minZ, groundY));
        this.planId = plan.id();
        this.plan = plan;
        this.rotation = rotation;
        this.minX = minX;
        this.minZ = minZ;
        this.groundY = groundY;
        this.mineral = mineral;
        this.wood = wood;
        this.seed = seed;
    }

    public PlanPiece(CompoundTag tag) {
        super(StructureContent.PLAN_PIECE.get(), tag);
        this.planId = tag.getStringOr("Plan", "");
        this.plan = Plans.get(planId);
        this.rotation = Rotation.values()[tag.getIntOr("Rot", 0)];
        this.minX = tag.getIntOr("MinX", 0);
        this.minZ = tag.getIntOr("MinZ", 0);
        this.groundY = tag.getIntOr("Ground", 64);
        OreMineral m = Surveyor.mineral(tag.getStringOr("Mineral", ""));
        this.mineral = m == null ? OreMineral.MALACHITE : m;
        this.wood = Wood.values()[tag.getIntOr("Wood", 0)];
        this.seed = tag.getLongOr("Seed", 0L);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putString("Plan", planId);
        tag.putInt("Rot", rotation.ordinal());
        tag.putInt("MinX", minX);
        tag.putInt("MinZ", minZ);
        tag.putInt("Ground", groundY);
        tag.putString("Mineral", mineral.id());
        tag.putInt("Wood", wood.ordinal());
        tag.putLong("Seed", seed);
    }

    /** Footprint size after turning: x span, then z span. */
    static int[] size(Plan plan, Rotation rotation) {
        boolean quarter = rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90;
        return quarter ? new int[] {plan.depth(), plan.width()} : new int[] {plan.width(), plan.depth()};
    }

    private static BoundingBox box(Plan plan, Rotation rotation, int minX, int minZ, int groundY) {
        int[] size = size(plan, rotation);
        int margin = plan.kind() == Plan.Kind.LEVELLED ? MARGIN : 0;
        int base = Plans.base(plan);
        return new BoundingBox(minX - margin, groundY - base - 12, minZ - margin,
                minX + size[0] - 1 + margin, groundY + plan.height() - base + HEADROOM + Terrain.TREE_TOP, minZ + size[1] - 1 + margin);
    }

    // ---------------------------------------------------------------- coordinates

    private int worldX(int px, int pz) {
        return minX + switch (rotation) {
            case NONE -> px;
            case CLOCKWISE_90 -> plan.depth() - 1 - pz;
            case CLOCKWISE_180 -> plan.width() - 1 - px;
            case COUNTERCLOCKWISE_90 -> pz;
        };
    }

    private int worldZ(int px, int pz) {
        return minZ + switch (rotation) {
            case NONE -> pz;
            case CLOCKWISE_90 -> px;
            case CLOCKWISE_180 -> plan.depth() - 1 - pz;
            case COUNTERCLOCKWISE_90 -> plan.width() - 1 - px;
        };
    }

    // ---------------------------------------------------------------- building

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
            BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos) {
        LocalRock rock = LocalRock.at(level, (boundingBox.minX() + boundingBox.maxX()) / 2,
                (boundingBox.minZ() + boundingBox.maxZ()) / 2, groundY);
        if (plan.kind() == Plan.Kind.LEVELLED) level(level, chunkBB);
        int base = Plans.base(plan);
        // Fences are joined to their neighbours once the chunk's blocks are in.
        List<BlockPos> fences = new ArrayList<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int pz = 0; pz < plan.depth(); pz++) {
            for (int px = 0; px < plan.width(); px++) {
                int x = worldX(px, pz), z = worldZ(px, pz);
                if (!chunkBB.isInside(x, chunkBB.minY(), z)) continue;
                int ground = plan.kind() == Plan.Kind.TERRAIN ? Terrain.surface(level, x, z, groundY) : groundY;
                for (int layer = 0; layer < plan.height(); layer++) {
                    char c = plan.at(px, layer, pz);
                    if (c == ' ') continue;
                    pos.set(x, ground + layer - base, z);
                    if (!chunkBB.isInside(pos)) continue;
                    place(level, random, pos, c, rock, fences);
                }
                if (plan.kind() == Plan.Kind.EMBEDDED && plan.at(px, base, pz) != ' ') {
                    Terrain.foundation(level, pos.set(x, ground - 1, z), chunkBB, Blocks.DIRT.defaultBlockState());
                }
            }
        }
        for (BlockPos fence : fences) {
            level.setBlock(fence, Block.updateFromNeighbourShapes(level.getBlockState(fence), level, fence), Block.UPDATE_CLIENTS);
        }
    }

    /** Flattens the footprint to the ground height and blends a ring around it toward the land. */
    private void level(WorldGenLevel level, BoundingBox chunkBB) {
        int[] size = size(plan, rotation);
        int top = groundY + plan.height() - Plans.base(plan) + HEADROOM;
        for (int x = minX - MARGIN; x < minX + size[0] + MARGIN; x++) {
            for (int z = minZ - MARGIN; z < minZ + size[1] + MARGIN; z++) {
                if (!chunkBB.isInside(x, chunkBB.minY(), z)) continue;
                int ring = Math.max(Math.max(minX - x, x - (minX + size[0] - 1)), Math.max(minZ - z, z - (minZ + size[1] - 1)));
                int natural = Terrain.surface(level, x, z, groundY);
                int target = ring <= 0 ? groundY
                        : (int) Math.round(groundY + (natural - groundY) * ring / (double) (MARGIN + 1));
                Terrain.setSurface(level, chunkBB, x, z, natural, target, ring <= 0 ? top : Math.max(target, natural) + 12);
            }
        }
    }

    private void place(WorldGenLevel level, RandomSource random, BlockPos pos, char c, LocalRock rock, List<BlockPos> fences) {
        if (Weathering.removes(plan, c, pos, seed)) return;
        BlockState state = switch (c) {
            case '.' -> level.getFluidState(pos).isSource() ? null : Blocks.AIR.defaultBlockState();
            case ',' -> Blocks.COARSE_DIRT.defaultBlockState();
            case 'd' -> Blocks.DIRT.defaultBlockState();
            case 'g' -> Blocks.GRAVEL.defaultBlockState();
            case '$' -> Blocks.SUSPICIOUS_GRAVEL.defaultBlockState();
            case '#', 'm' -> rock.cobbled();
            case 'R' -> rock.raw(pos.getX(), pos.getY(), pos.getZ());
            case 'r', '1' -> rock.loose(rock.top());
            case '2' -> rock.loose(rock.middle());
            case '3' -> rock.loose(rock.bottom());
            case 'C' -> StructureContent.FIBRE_CANVAS.get().defaultBlockState();
            case '_' -> StructureContent.FIBRE_CANVAS_CARPET.get().defaultBlockState();
            case 'P' -> StructureContent.PIT_PROP.get().defaultBlockState();
            case '=' -> wood.stripped().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
            case '|' -> wood.stripped().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
            case 'o' -> wood.log();
            case 'L', 'W', 'l' -> ModBlocks.LOG_PILE.get().defaultBlockState();
            case 'f' -> ModBlocks.FIRE_PIT.get().defaultBlockState();
            case 'F' -> ModBlocks.FORGE.get().defaultBlockState();
            case 'c' -> ModBlocks.CRUCIBLE.get().defaultBlockState();
            case 'A' -> rock.anvil().setValue(AnvilBlock.FACING, Direction.SOUTH);
            case 'B', 'b' -> Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP);
            case 'K', 'Y' -> StructureContent.CRACKED_FIRE_BRICKS.get().defaultBlockState();
            case 'S', 'Z' -> StructureContent.SLAG_HEAP.get().defaultBlockState();
            case 'G' -> ModBlocks.PLACER_GRAVEL.get().defaultBlockState();
            case 'E' -> bed(BedPart.HEAD);
            case 'e' -> bed(BedPart.FOOT);
            case 'k' -> wood.fence();
            case 's' -> wood.slab().setValue(SlabBlock.TYPE, SlabType.TOP);
            case 'H' -> Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH);
            case 'x' -> ModBlocks.LOOSE_STICK.get().defaultBlockState();
            case 'q' -> ModBlocks.LOOSE_FLINT.get().defaultBlockState();
            case 'n' -> ModBlocks.SMALL_ORES.get(mineral).get().defaultBlockState();
            default -> null;
        };
        if (state == null) return;
        level.setBlock(pos, state.rotate(rotation), Block.UPDATE_CLIENTS);
        if (c == 'k') fences.add(pos.immutable());
        fill(level, random, pos, c);
        if (c == 'm' && Weathering.chance(pos.above(), seed, 0.5) && level.getBlockState(pos.above()).isAir()) {
            level.setBlock(pos.above(), Blocks.MOSS_CARPET.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private static BlockState bed(BedPart part) {
        BlockState bed = BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace("brown_bed")).defaultBlockState();
        if (!(bed.getBlock() instanceof BedBlock)) return null;
        return bed.setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, part);
    }

    /** Loot, logs and fuel for the blocks that hold things. */
    private void fill(WorldGenLevel level, RandomSource random, BlockPos pos, char c) {
        switch (c) {
            case 'B', 'b' -> {
                var table = CampLoot.barrel(planId, mineral);
                if (table != null) RandomizableContainer.setBlockEntityLootTable(level, random, pos, table);
            }
            case '$' -> {
                if (level.getBlockEntity(pos) instanceof BrushableBlockEntity dig) {
                    dig.setLootTable(CampLoot.dig(planId, mineral), Weathering.hash(pos, seed));
                }
            }
            case 'L', 'W', 'l' -> {
                if (level.getBlockEntity(pos) instanceof LogPileBlockEntity pile) {
                    int logs = c == 'l' ? 2 : c == 'L' ? 4 : 6;
                    for (int i = 0; i < logs; i++) pile.add(new ItemStack(wood.logItem()));
                }
            }
            case 'f' -> {
                if (level.getBlockEntity(pos) instanceof FirePitBlockEntity pit) {
                    Supplier<ItemStack> fuel = switch (planId) {
                        case "charcoal_burners_clearing" -> () -> new ItemStack(Items.STICK, 2);
                        case "mining_camp/yard" -> () -> new ItemStack(wood.logItem(), 2);
                        default -> () -> ItemStack.EMPTY;
                    };
                    pit.setItem(FirePitBlockEntity.FUEL_SLOT, fuel.get());
                }
            }
            default -> {
            }
        }
    }

    /** The rock of the province a structure stands in (structures spec 3.5). */
    record LocalRock(Rock top, Rock middle, Rock bottom, int surfaceY, StrataSampler sampler) {
        static LocalRock at(WorldGenLevel level, int x, int z, int surfaceY) {
            var geology = StructureGeology.of(level);
            if (geology == null) return new LocalRock(Rock.GRANITE, Rock.GRANITE, Rock.GRANITE, surfaceY, null);
            var column = geology.sampler().column(x, z, surfaceY);
            var province = column.province();
            return new LocalRock(province.top(), province.middle(), province.bottom(), surfaceY, geology.sampler());
        }

        BlockState cobbled() {
            return ModBlocks.COBBLED_ROCK.get(top).get().defaultBlockState();
        }

        BlockState raw(int x, int y, int z) {
            Rock rock = sampler == null ? top : sampler.rockAt(x, y, z, surfaceY);
            return ModBlocks.RAW_ROCK.get(rock).get().defaultBlockState();
        }

        BlockState loose(Rock rock) {
            return ModBlocks.LOOSE_ROCK.get(rock).get().defaultBlockState();
        }

        /** Every province's bottom rock is an intrusive igneous rock, which always has an anvil. */
        BlockState anvil() {
            var anvil = ModBlocks.STONE_ANVILS.get(bottom);
            if (anvil == null) anvil = ModBlocks.STONE_ANVILS.get(Rock.GRANITE);
            return anvil.get().defaultBlockState();
        }
    }

    /** The timber a camp was built from, chosen by its biome. */
    public enum Wood {
        OAK(Blocks.OAK_LOG, Blocks.STRIPPED_OAK_LOG, Blocks.OAK_FENCE, Blocks.OAK_SLAB, Items.OAK_LOG),
        SPRUCE(Blocks.SPRUCE_LOG, Blocks.STRIPPED_SPRUCE_LOG, Blocks.SPRUCE_FENCE, Blocks.SPRUCE_SLAB, Items.SPRUCE_LOG),
        DARK_OAK(Blocks.DARK_OAK_LOG, Blocks.STRIPPED_DARK_OAK_LOG, Blocks.DARK_OAK_FENCE, Blocks.DARK_OAK_SLAB, Items.DARK_OAK_LOG);

        private final Block log, stripped, fence, slab;
        private final Item logItem;

        Wood(Block log, Block stripped, Block fence, Block slab, Item logItem) {
            this.log = log;
            this.stripped = stripped;
            this.fence = fence;
            this.slab = slab;
            this.logItem = logItem;
        }

        BlockState log() {
            return log.defaultBlockState();
        }

        BlockState stripped() {
            return stripped.defaultBlockState();
        }

        BlockState fence() {
            return fence.defaultBlockState();
        }

        BlockState slab() {
            return slab.defaultBlockState();
        }

        Item logItem() {
            return logItem;
        }
    }
}
