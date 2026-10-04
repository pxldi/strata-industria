package dev.strataindustria.casting;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.registry.PatternRegistry;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * A carved wooden pattern. Right-click presses it into a sand flask from the inventory and leaves a sand mold of
 * the same shape; the pattern is not used up.
 */
public class PatternItem extends Item {
    private final Supplier<? extends Item> sandMold;

    public PatternItem(Supplier<? extends Item> sandMold, Properties properties) {
        super(properties);
        this.sandMold = sandMold;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack flask = findFlask(player, hand);
        if (flask.isEmpty()) {
            if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".pattern.need_flask"));
            return InteractionResult.FAIL;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        flask.shrink(1);
        ItemStack mold = new ItemStack(sandMold.get());
        if (!player.addItem(mold)) Block.popResource(level, player.blockPosition(), mold);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), PatternRegistry.PATTERN_PRESS.get(), SoundSource.PLAYERS,
                0.8f, 0.9f + level.getRandom().nextFloat() * 0.2f);
        if (player instanceof ServerPlayer server) Journal.award(server, Journal.PATTERN_PRESSED);
        return InteractionResult.SUCCESS;
    }

    /** The other hand first, then the rest of the inventory. */
    static ItemStack findFlask(Player player, InteractionHand hand) {
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (other.is(PatternRegistry.SAND_FLASK.get())) return other;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(PatternRegistry.SAND_FLASK.get())) return stack;
        }
        return ItemStack.EMPTY;
    }
}
