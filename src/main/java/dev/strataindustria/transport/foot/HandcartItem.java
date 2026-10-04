package dev.strataindustria.transport.foot;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

/** Sets a handcart down where the player points. */
public class HandcartItem extends Item {
    public HandcartItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        Vec3 at = context.getClickLocation();
        HandcartEntity cart = FootRegistry.HANDCART_ENTITY.get().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (cart == null) return InteractionResult.FAIL;
        cart.snapTo(at.x, at.y, at.z, player == null ? 0 : player.getYRot(), 0);
        if (!level.noCollision(cart, cart.getBoundingBox())) return InteractionResult.FAIL;
        level.addFreshEntity(cart);
        if (stack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)) cart.setCustomName(stack.getHoverName());
        level.playSound(null, at.x, at.y, at.z, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.9f, 0.8f);
        if (player == null || !player.hasInfiniteMaterials()) stack.shrink(1);
        return InteractionResult.SUCCESS;
    }
}
