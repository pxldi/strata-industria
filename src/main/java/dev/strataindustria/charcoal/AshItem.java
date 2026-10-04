package dev.strataindustria.charcoal;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;

/** Wood ash: works like bone meal on crops and saplings (spec 4.4), and is kept for lye later. */
public class AshItem extends Item {
    public AshItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof CropBlock) && !state.is(BlockTags.SAPLINGS)) return InteractionResult.PASS;
        if (!BoneMealItem.growCrop(context.getItemInHand(), level, pos)) return InteractionResult.PASS;
        if (!level.isClientSide()) level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, pos, 15);
        return InteractionResult.SUCCESS;
    }
}
