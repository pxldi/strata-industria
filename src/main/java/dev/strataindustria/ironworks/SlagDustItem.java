package dev.strataindustria.ironworks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;

/** Slag dust (tier 4 spec 4.6): a fertiliser, each pinch working like two bone meal. */
public class SlagDustItem extends Item {
    public SlagDustItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack doses = new ItemStack(Items.BONE_MEAL, 2);
        if (!BoneMealItem.growCrop(doses, level, pos)) return InteractionResult.PASS;
        BoneMealItem.growCrop(doses, level, pos);
        if (!level.isClientSide()) {
            context.getItemInHand().shrink(1);
            level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, pos, 15);
        }
        return InteractionResult.SUCCESS;
    }
}
