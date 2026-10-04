package dev.strataindustria.cabinet;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.survey.SurveyText;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A glass-fronted cabinet that holds one specimen of every rock and mineral (uniqueness 9.5). Set a loose rock, a
 * cut specimen or a raw ore piece on its shelf; an empty hand reads the cabinet back. Filling a shelf sets the
 * player's prospector's pick up for that rock, and its front fills in as the collection grows.
 */
public class SpecimenCabinetBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final int MAX_FILL = 4;
    public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, MAX_FILL);
    /** Page written the first time any shelf is full. */
    public static final String JOURNAL = "place/cabinet";
    private static final String KEY = StrataIndustria.MOD_ID + ".cabinet.";
    private static final VoxelShape SHAPE = Block.box(1, 0, 2, 15, 16, 14);

    public SpecimenCabinetBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FILL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FILL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        String specimen = Specimens.idOf(stack);
        if (specimen == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(level.getBlockEntity(pos) instanceof SpecimenCabinetBlockEntity cabinet)) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!cabinet.add(specimen)) {
            player.sendOverlayMessage(Component.translatable(KEY + "have", name(specimen)));
            return InteractionResult.CONSUME;
        }
        if (!player.hasInfiniteMaterials()) stack.shrink(1);
        level.setBlock(pos, state.setValue(FILL, cabinet.fill()), Block.UPDATE_ALL);
        level.playSound(null, pos, CabinetRegistry.SET.get(), SoundSource.BLOCKS, 0.8f, 0.95f + level.getRandom().nextFloat() * 0.1f);
        String shelf = Specimens.shelfOf(specimen);
        int held = (int) Specimens.required(shelf).stream().filter(cabinet::holds).count();
        player.sendOverlayMessage(Component.translatable(KEY + "set", name(specimen), held, Specimens.required(shelf).size()));
        if (Specimens.complete(shelf, cabinet.held()) && player instanceof ServerPlayer serverPlayer && Shelves.add(serverPlayer, shelf)) {
            level.playSound(null, pos, CabinetRegistry.SHELF.get(), SoundSource.BLOCKS, 0.9f, 1.0f);
            player.sendOverlayMessage(Component.translatable(KEY + "full", Component.translatable(KEY + "shelf." + shelf)));
            Journal.award(serverPlayer, JOURNAL);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof SpecimenCabinetBlockEntity cabinet)) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            int full = (int) Specimens.shelves().stream().filter(s -> Specimens.complete(s, cabinet.held())).count();
            player.sendOverlayMessage(Component.translatable(KEY + "status", cabinet.held().size(), Specimens.total(), full,
                    Specimens.shelves().size()));
        }
        return InteractionResult.SUCCESS;
    }

    private static Component name(String specimen) {
        String id = specimen.substring(specimen.indexOf(':') + 1);
        return specimen.startsWith("mineral:") ? SurveyText.mineralName(id)
                : Component.translatable(StrataIndustria.MOD_ID + ".knapped_from.material." + id);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpecimenCabinetBlockEntity(pos, state);
    }
}
