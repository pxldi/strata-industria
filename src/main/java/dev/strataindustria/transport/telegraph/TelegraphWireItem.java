package dev.strataindustria.transport.telegraph;

import dev.strataindustria.Config;
import dev.strataindustria.electric.PoleInsulatorBlockEntity;
import dev.strataindustria.registry.Tier5DataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Telegraph wire (outposts spec 9.1): use it on one pole insulator, then on another up to {@code transport.telegraphSpan}
 * blocks away. A span uses one wire for every 8 blocks. It hangs on the same poles as the power spans and never joins them.
 */
public class TelegraphWireItem extends Item {
    public TelegraphWireItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockEntity(pos) instanceof PoleInsulatorBlockEntity)) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos start = stack.get(Tier5DataComponents.SPAN_LINK.get());
        if (start == null || !(level.getBlockEntity(start) instanceof PoleInsulatorBlockEntity)) {
            stack.set(Tier5DataComponents.SPAN_LINK.get(), pos.immutable());
            level.playSound(null, pos, TelegraphRegistry.DROP_HUNG.get(), SoundSource.BLOCKS, 0.6f, 0.9f);
            message(player, Component.translatable(TelegraphLine.key("wire.started")));
            return InteractionResult.SUCCESS_SERVER;
        }
        String problem = TelegraphLine.problem(server, start, pos);
        if (problem != null) {
            if (problem.equals("same")) {
                stack.remove(Tier5DataComponents.SPAN_LINK.get());
            } else {
                message(player, Component.translatable(TelegraphLine.key("wire." + problem), Config.TRANSPORT_TELEGRAPH_SPAN.get()));
            }
            return InteractionResult.FAIL;
        }
        int needed = TelegraphIndex.wireFor(start, pos);
        boolean creative = player != null && player.getAbilities().instabuild;
        if (!creative && stack.getCount() < needed) {
            message(player, Component.translatable(TelegraphLine.key("wire.needs"), needed));
            return InteractionResult.FAIL;
        }
        TelegraphLine.connect(server, start, pos);
        stack.remove(Tier5DataComponents.SPAN_LINK.get());
        if (!creative) stack.shrink(needed);
        message(player, Component.translatable(TelegraphLine.key("wire.joined"), needed));
        return InteractionResult.SUCCESS_SERVER;
    }

    private static void message(Player player, Component text) {
        if (player != null) player.sendOverlayMessage(text);
    }
}
