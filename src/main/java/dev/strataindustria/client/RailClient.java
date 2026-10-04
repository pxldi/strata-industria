package dev.strataindustria.client;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.client.rail.LocomotiveModel;
import dev.strataindustria.client.rail.LocomotiveRenderer;
import dev.strataindustria.client.rail.MineTubModel;
import dev.strataindustria.client.rail.InclineWinchRenderer;
import dev.strataindustria.client.rail.MineTubRenderer;
import dev.strataindustria.client.rail.PonyRenderer;
import dev.strataindustria.client.rail.WagonModel;
import dev.strataindustria.client.rail.WagonRenderer;
import dev.strataindustria.transport.rail.RailwayRegistry;
import dev.strataindustria.client.screen.TubStopScreen;
import dev.strataindustria.transport.rail.RailRegistry;
import dev.strataindustria.transport.rail.StopData;
import com.mojang.blaze3d.platform.InputConstants;
import dev.strataindustria.client.foot.FootClient;
import dev.strataindustria.transport.rail.RailPayloads;
import dev.strataindustria.transport.rail.SteamLocomotiveEntity;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** The client side of the wooden tramway: the tub's model and the stop screen. */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID, value = Dist.CLIENT)
public final class RailClient {
    private RailClient() {}

    public static void openStop(StopData data, String station) {
        Minecraft.getInstance().gui.setScreen(new TubStopScreen(data, station));
    }

    private static final KeyMapping WHISTLE_KEY = new KeyMapping("key." + StrataIndustria.MOD_ID + ".whistle",
            InputConstants.Type.KEYBOARD, InputConstants.KEY_H, FootClient.CATEGORY);

    @SubscribeEvent
    static void keys(RegisterKeyMappingsEvent event) {
        event.register(WHISTLE_KEY);
    }

    /** The whistle key blows the engine's whistle, or rings the tram's bell; the server limits how often. */
    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (WHISTLE_KEY.consumeClick()) {
            if (minecraft.player != null && (minecraft.player.getVehicle() instanceof SteamLocomotiveEntity || minecraft.player.getVehicle() instanceof dev.strataindustria.transport.rail.ElectricTramEntity)) {
                ClientPacketDistributor.sendToServer(new RailPayloads.Whistle());
            }
        }
    }

    @SubscribeEvent
    static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MineTubModel.LAYER, MineTubModel::createBodyLayer);
        event.registerLayerDefinition(LocomotiveModel.LAYER, LocomotiveModel::createBodyLayer);
        event.registerLayerDefinition(dev.strataindustria.client.rail.TramModel.LAYER, dev.strataindustria.client.rail.TramModel::createBodyLayer);
        event.registerLayerDefinition(WagonModel.ORE_LAYER, WagonModel::createOreLayer);
        event.registerLayerDefinition(WagonModel.TANK_LAYER, WagonModel::createTankLayer);
        event.registerLayerDefinition(WagonModel.FLAT_LAYER, WagonModel::createFlatLayer);
    }

    @SubscribeEvent
    static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(RailRegistry.MINE_TUB_ENTITY.get(), MineTubRenderer::new);
        event.registerEntityRenderer(RailRegistry.PONY_ENTITY.get(), PonyRenderer::new);
        event.registerEntityRenderer(RailwayRegistry.STEAM_LOCOMOTIVE_ENTITY.get(), LocomotiveRenderer::new);
        event.registerEntityRenderer(dev.strataindustria.transport.rail.TramRegistry.ELECTRIC_TRAM_ENTITY.get(), dev.strataindustria.client.rail.TramRenderer::new);
        event.registerBlockEntityRenderer(dev.strataindustria.transport.rail.TramRegistry.TROLLEY_BRACKET_ENTITY.get(),
                dev.strataindustria.client.render.TrolleyBracketRenderer::new);
        event.registerEntityRenderer(RailwayRegistry.ORE_WAGON_ENTITY.get(),
                context -> WagonRenderer.of(context, WagonModel.ORE_LAYER, "ore_wagon", 0.47f));
        event.registerEntityRenderer(RailwayRegistry.TANK_WAGON_ENTITY.get(),
                context -> WagonRenderer.of(context, WagonModel.TANK_LAYER, "tank_wagon", 0.41f));
        event.registerEntityRenderer(RailwayRegistry.FLAT_WAGON_ENTITY.get(),
                context -> WagonRenderer.of(context, WagonModel.FLAT_LAYER, "flat_wagon", 0.41f));
        event.registerBlockEntityRenderer(RailRegistry.INCLINE_WINCH_ENTITY.get(), InclineWinchRenderer::new);
    }
}
