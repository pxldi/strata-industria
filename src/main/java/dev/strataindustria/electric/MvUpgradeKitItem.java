package dev.strataindustria.electric;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.power.ElectricNetworks;
import dev.strataindustria.power.ElectricTier;
import dev.strataindustria.registry.Tier5Blocks;
import dev.strataindustria.registry.Tier5Sounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Spec 9.5: sneak and right-click an LV machine to make it MV. It keeps its contents and orientation; only
 * the {@code tier} blockstate changes, which swaps the casing and the limits (everything else reads the tier
 * from the state). Plain right-click still opens the machine's screen.
 */
public class MvUpgradeKitItem extends Item {
    public MvUpgradeKitItem(Properties properties) {
        super(properties);
    }

    /** Whether the block is a machine that comes in LV and MV and is still LV. */
    public static boolean canUpgrade(BlockState state) {
        return state.hasProperty(ElectricTier.PROPERTY) && state.getValue(ElectricTier.PROPERTY) == ElectricTier.LV
                && Tier5Blocks.upgradable().stream().anyMatch(block -> state.is(block.get()));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (player == null || !player.isShiftKeyDown() || !state.hasProperty(ElectricTier.PROPERTY)) return InteractionResult.PASS;
        if (!canUpgrade(state)) {
            if (!level.isClientSide() && Tier5Blocks.upgradable().stream().anyMatch(block -> state.is(block.get()))) {
                player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".machine.already_mv"));
            }
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide()) {
            level.setBlock(pos, state.setValue(ElectricTier.PROPERTY, ElectricTier.MV), Block.UPDATE_ALL);
            ElectricNetworks.markDirty(level, pos);
            level.playSound(null, pos, Tier5Sounds.MACHINE_UPGRADE.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
            if (!player.hasInfiniteMaterials()) context.getItemInHand().shrink(1);
            player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".machine.upgraded_now", ElectricTier.MV.label()));
        }
        return InteractionResult.SUCCESS;
    }
}
