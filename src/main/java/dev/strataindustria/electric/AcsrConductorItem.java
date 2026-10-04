package dev.strataindustria.electric;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.registry.Tier5DataComponents;
import dev.strataindustria.registry.Tier5Sounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * ACSR conductor (spec 8.4): use it on one pole insulator, then on another up to {@code electric.maxSpan}
 * blocks away, to string a span. A span uses one conductor per 4 blocks of length.
 */
public class AcsrConductorItem extends Item {
    public AcsrConductorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockEntity(pos) instanceof PoleInsulatorBlockEntity)) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        String key = StrataIndustria.MOD_ID + ".span.";
        BlockPos start = stack.get(Tier5DataComponents.SPAN_LINK.get());
        if (start == null || !(level.getBlockEntity(start) instanceof PoleInsulatorBlockEntity)) {
            stack.set(Tier5DataComponents.SPAN_LINK.get(), pos.immutable());
            level.playSound(null, pos, Tier5Sounds.POLE_INSULATOR_CONNECT.get(), SoundSource.BLOCKS, 0.5f, 1.3f);
            message(player, Component.translatable(key + "started"));
            return InteractionResult.SUCCESS;
        }
        String problem = OverheadLine.problem(level, start, pos);
        if (problem != null) {
            if (problem.equals("same")) {
                stack.remove(Tier5DataComponents.SPAN_LINK.get());
            } else {
                message(player, Component.translatable(key + problem, dev.strataindustria.Config.ELECTRIC_MAX_SPAN.get()));
            }
            return InteractionResult.FAIL;
        }
        int needed = OverheadLine.conductorsFor(start, pos);
        boolean creative = player != null && player.getAbilities().instabuild;
        if (!creative && stack.getCount() < needed) {
            message(player, Component.translatable(key + "needs", needed));
            return InteractionResult.FAIL;
        }
        OverheadLine.connect(level, start, pos);
        stack.remove(Tier5DataComponents.SPAN_LINK.get());
        if (!creative) stack.shrink(needed);
        level.playSound(null, pos, Tier5Sounds.POLE_INSULATOR_CONNECT.get(), SoundSource.BLOCKS, 0.9f, 1.0f);
        message(player, Component.translatable(key + "joined", needed));
        return InteractionResult.SUCCESS;
    }

    private static void message(Player player, Component text) {
        if (player != null) player.sendOverlayMessage(text);
    }
}
