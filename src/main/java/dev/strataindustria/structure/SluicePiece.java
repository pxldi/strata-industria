package dev.strataindustria.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.material.Fluids;

/**
 * The panners' wooden sluice (structures spec 6.6): a trough of spruce slabs running from the bank down into
 * the river, boarded with open trapdoors, with a riffle rail every second block and water let in at its head.
 * It is scenery, a crude version of the sluice block the player builds later.
 */
public class SluicePiece extends StructurePiece {
    static final int LENGTH = 8;
    /** The bed drops one block every this many rows, so the water runs down it. */
    static final int DROP = 3;

    private final int headX, headY, headZ;
    private final Direction direction;

    /**
     * @param head      the first bed block of the trough, on the bank
     * @param direction the way the trough runs, toward the water
     */
    public SluicePiece(BlockPos head, Direction direction) {
        super(StructureContent.SLUICE_PIECE.get(), 0, box(head, direction));
        this.headX = head.getX();
        this.headY = head.getY();
        this.headZ = head.getZ();
        this.direction = direction;
    }

    public SluicePiece(CompoundTag tag) {
        super(StructureContent.SLUICE_PIECE.get(), tag);
        this.headX = tag.getIntOr("HeadX", 0);
        this.headY = tag.getIntOr("HeadY", 64);
        this.headZ = tag.getIntOr("HeadZ", 0);
        this.direction = Direction.from2DDataValue(tag.getIntOr("Dir", 0));
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("HeadX", headX);
        tag.putInt("HeadY", headY);
        tag.putInt("HeadZ", headZ);
        tag.putInt("Dir", direction.get2DDataValue());
    }

    private static BoundingBox box(BlockPos head, Direction direction) {
        BlockPos back = head.relative(direction, -1);
        BlockPos end = head.relative(direction, LENGTH - 1);
        Direction side = direction.getClockWise();
        BlockPos a = back.relative(side), b = end.relative(side, -1);
        return new BoundingBox(Math.min(a.getX(), b.getX()), head.getY() - Terrain.MAX_FILL - 3, Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()), head.getY() + 3, Math.max(a.getZ(), b.getZ()));
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
            BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos) {
        BlockPos head = new BlockPos(headX, headY, headZ);
        Direction left = direction.getClockWise(), right = direction.getCounterClockWise();
        BoundingBox span = boundingBox;
        TreeClearing.clear(level, chunkBB, span.minX() - PlanPiece.TREE_PAD, span.minZ() - PlanPiece.TREE_PAD,
                span.maxX() + PlanPiece.TREE_PAD, span.maxZ() + PlanPiece.TREE_PAD, headY - 2, headY + 6, pos -> false);

        // A board across the head keeps the water from running back onto the bank.
        BlockPos back = head.relative(direction, -1).above();
        set(level, chunkBB, back, trapdoor(direction.getOpposite()));

        for (int row = 0; row < LENGTH; row++) {
            BlockPos bed = head.relative(direction, row).below(row / DROP);
            // The riffles: rails across the bed that catch the heavy grains.
            BlockState floor = row % 2 == 1
                    ? Blocks.SPRUCE_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, direction)
                    : Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
            set(level, chunkBB, bed, floor);
            for (int up = 0; up <= 1; up++) {
                // Each side board's panel sits on the inner edge of its block, against the water.
                set(level, chunkBB, bed.relative(left).above(up), trapdoor(left));
                set(level, chunkBB, bed.relative(right).above(up), trapdoor(right));
            }
            BlockPos channel = bed.above();
            if (row == 0) {
                if (chunkBB.isInside(channel)) {
                    level.setBlock(channel, Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
                    level.scheduleTick(channel, Fluids.WATER, 1);
                }
            } else if (level.getFluidState(channel).isEmpty()) {
                set(level, chunkBB, channel, Blocks.AIR.defaultBlockState());
            }
            for (int up = 2; up <= 3; up++) {
                for (Direction d : new Direction[] {left, right, direction}) {
                    BlockPos above = (d == direction ? bed : bed.relative(d)).above(up);
                    if (chunkBB.isInside(above) && Terrain.isGrowth(level.getBlockState(above))) {
                        level.setBlock(above, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
            // Legs under the outer edge, down to the bank or the river bed.
            if (row % 3 == 1) {
                leg(level, chunkBB, bed.relative(left).below());
                leg(level, chunkBB, bed.relative(right).below());
            }
        }
    }

    private static BlockState trapdoor(Direction facing) {
        return Blocks.SPRUCE_TRAPDOOR.defaultBlockState()
                .setValue(TrapDoorBlock.FACING, facing)
                .setValue(TrapDoorBlock.HALF, Half.BOTTOM)
                .setValue(TrapDoorBlock.OPEN, true);
    }

    /** Places a block, keeping it waterlogged where it stands in the river. */
    private static void set(WorldGenLevel level, BoundingBox chunkBB, BlockPos pos, BlockState state) {
        if (!chunkBB.isInside(pos)) return;
        if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
            state = state.setValue(BlockStateProperties.WATERLOGGED, level.getFluidState(pos).isSourceOfType(Fluids.WATER));
        }
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }

    private static void leg(WorldGenLevel level, BoundingBox chunkBB, BlockPos start) {
        BlockPos.MutableBlockPos pos = start.mutable();
        for (int i = 0; i < Terrain.MAX_FILL; i++) {
            if (!chunkBB.isInside(pos) || Terrain.isGround(level.getBlockState(pos))) return;
            set(level, chunkBB, pos, Blocks.SPRUCE_FENCE.defaultBlockState());
            pos.move(0, -1, 0);
        }
    }
}
