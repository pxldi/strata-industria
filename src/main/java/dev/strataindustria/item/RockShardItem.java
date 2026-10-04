package dev.strataindustria.item;

import dev.strataindustria.knapping.Knapping;
import dev.strataindustria.knapping.Shaping;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A shard of rock off a boulder. Use strikes it into the shape you picked, sneak and use picks the shape. */
public class RockShardItem extends Item {
    public RockShardItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!Knapping.isKnappable(held)) return super.use(level, player, hand);
        if (player instanceof ServerPlayer serverPlayer) Shaping.use(serverPlayer, hand);
        return InteractionResult.SUCCESS;
    }
}
