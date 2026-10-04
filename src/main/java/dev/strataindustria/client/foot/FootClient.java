package dev.strataindustria.client.foot;

import com.mojang.blaze3d.platform.InputConstants;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.transport.foot.Burden;
import dev.strataindustria.transport.foot.FootRegistry;
import dev.strataindustria.transport.foot.PackEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Input;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** The client side of tier 2 on foot: the handcart's model, the pack key, and the no-sprint, no-jump rules. */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID, value = Dist.CLIENT)
public final class FootClient {
    private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(StrataIndustria.id("transport"));
    private static final KeyMapping PACK_KEY = new KeyMapping("key." + StrataIndustria.MOD_ID + ".pack",
            InputConstants.Type.KEYBOARD, InputConstants.KEY_B, CATEGORY);

    private FootClient() {}

    @SubscribeEvent
    static void keys(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(PACK_KEY);
    }

    @SubscribeEvent
    static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(HandcartModel.LAYER, HandcartModel::createBodyLayer);
    }

    @SubscribeEvent
    static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(FootRegistry.HANDCART_ENTITY.get(), HandcartRenderer::new);
    }

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (PACK_KEY.consumeClick()) {
            if (minecraft.player != null) ClientPacketDistributor.sendToServer(new PackEvents.Open());
        }
    }

    /** A full pack or a handcart in hand takes sprinting away; a handcart takes the jump too. */
    @SubscribeEvent
    static void onInput(MovementInputUpdateEvent event) {
        Input keys = event.getInput().keyPresses;
        boolean noSprint = Burden.noSprint(event.getEntity());
        boolean noJump = Burden.isHauling(event.getEntity());
        if (!noSprint && !noJump) return;
        event.getInput().keyPresses = new Input(keys.forward(), keys.backward(), keys.left(), keys.right(),
                !noJump && keys.jump(), keys.shift(), !noSprint && keys.sprint());
    }
}
