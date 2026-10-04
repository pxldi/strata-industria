package dev.strataindustria.transport.rail;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.registry.Tier5DataComponents;
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
 * Trolley wire (outposts spec 9.2): use it on one bracket, then on another up to 16 blocks away, to string a span.
 * A span takes one wire for every two blocks of length.
 */
public class TrolleyWireItem extends Item {
    public TrolleyWireItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockEntity(pos) instanceof TrolleyBracketBlockEntity)) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        String key = StrataIndustria.MOD_ID + ".trolley.";
        BlockPos start = stack.get(Tier5DataComponents.SPAN_LINK.get());
        if (start == null || !(level.getBlockEntity(start) instanceof TrolleyBracketBlockEntity)) {
            stack.set(Tier5DataComponents.SPAN_LINK.get(), pos.immutable());
            level.playSound(null, pos, TramRegistry.WIRE_STRUNG.get(), SoundSource.BLOCKS, 0.5f, 1.4f);
            message(player, Component.translatable(key + "started"));
            return InteractionResult.SUCCESS;
        }
        String problem = TrolleyWires.problem(level, start, pos);
        if (problem != null) {
            if (problem.equals("same")) {
                stack.remove(Tier5DataComponents.SPAN_LINK.get());
            } else {
                message(player, Component.translatable(key + problem, (int) TrolleyWires.MAX_SPAN));
            }
            return InteractionResult.FAIL;
        }
        int needed = TrolleyWires.wireFor(start, pos);
        boolean creative = player != null && player.getAbilities().instabuild;
        if (!creative && stack.getCount() < needed) {
            message(player, Component.translatable(key + "needs", needed));
            return InteractionResult.FAIL;
        }
        TrolleyWires.connect(level, start, pos);
        stack.remove(Tier5DataComponents.SPAN_LINK.get());
        if (!creative) stack.shrink(needed);
        message(player, Component.translatable(key + "joined", needed));
        return InteractionResult.SUCCESS;
    }

    private static void message(Player player, Component text) {
        if (player != null) player.sendOverlayMessage(text);
    }
}
