package dev.strataindustria.power;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import dev.strataindustria.registry.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A leather belt (spec 7.3): use it on one pulley, then on another on a parallel axis up to 8 blocks
 * away, to join them. The belt is used up; breaking either pulley gives it back.
 */
public class LeatherBeltItem extends Item {
    public LeatherBeltItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack belt = context.getItemInHand();
        if (!(level.getBlockEntity(pos) instanceof PulleyBlockEntity pulley)) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        String key = StrataIndustria.MOD_ID + ".belt.";
        if (pulley.link() != null) {
            message(player, Component.translatable(key + "taken"));
            return InteractionResult.FAIL;
        }
        BlockPos start = belt.get(ModDataComponents.BELT_START.get());
        if (start == null || !(level.getBlockEntity(start) instanceof PulleyBlockEntity first) || first.link() != null) {
            belt.set(ModDataComponents.BELT_START.get(), pos.immutable());
            level.playSound(null, pos, ModSounds.BELT_ATTACH.get(), SoundSource.BLOCKS, 0.7f, 1.2f);
            message(player, Component.translatable(key + "started"));
            return InteractionResult.SUCCESS;
        }
        BlockState a = level.getBlockState(start), b = level.getBlockState(pos);
        String problem = PulleyBlockEntity.problem(a, start, b, pos);
        if (problem != null) {
            if (!problem.equals("same")) message(player, Component.translatable(key + problem));
            return InteractionResult.FAIL;
        }
        first.setLink(pos);
        pulley.setLink(start);
        belt.remove(ModDataComponents.BELT_START.get());
        if (player == null || !player.getAbilities().instabuild) belt.shrink(1);
        level.playSound(null, pos, ModSounds.BELT_ATTACH.get(), SoundSource.BLOCKS, 0.9f, 0.8f);
        message(player, Component.translatable(key + "joined"));
        return InteractionResult.SUCCESS;
    }

    private static void message(Player player, Component text) {
        if (player != null) player.sendOverlayMessage(text);
    }
}
