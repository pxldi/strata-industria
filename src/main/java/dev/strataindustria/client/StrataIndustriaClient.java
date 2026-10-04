package dev.strataindustria.client;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.client.hud.FirestarterHud;
import dev.strataindustria.client.render.PitKilnRenderer;
import dev.strataindustria.client.render.QuernRenderer;
import dev.strataindustria.client.render.AnvilRenderer;
import dev.strataindustria.client.render.RotorRenderer;
import dev.strataindustria.client.screen.MillstoneScreen;
import dev.strataindustria.client.screen.SawMillScreen;
import dev.strataindustria.client.screen.TripHammerScreen;
import dev.strataindustria.client.render.TripHammerRenderer;
import dev.strataindustria.power.AxleBlock;
import dev.strataindustria.power.HandCrankBlock;
import dev.strataindustria.power.WaterWheelBlock;
import net.minecraft.core.Direction;
import dev.strataindustria.client.screen.AnvilScreen;
import dev.strataindustria.client.screen.BloomeryScreen;
import dev.strataindustria.client.screen.CrucibleScreen;
import dev.strataindustria.client.screen.FirePitScreen;
import dev.strataindustria.client.screen.ForgeScreen;
import dev.strataindustria.client.screen.KnappingScreen;
import dev.strataindustria.client.screen.SmallVesselScreen;
import dev.strataindustria.registry.ModBlockEntities;
import dev.strataindustria.registry.ModMenus;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterConditionalItemModelPropertyEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = StrataIndustria.MOD_ID, dist = Dist.CLIENT)
public final class StrataIndustriaClient {
    public StrataIndustriaClient(IEventBus modBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modBus.addListener(StrataIndustriaClient::registerScreens);
        modBus.addListener(StrataIndustriaClient::registerGuiLayers);
        modBus.addListener(StrataIndustriaClient::registerRenderers);
        modBus.addListener(StrataIndustriaClient::registerTints);
        modBus.addListener(StrataIndustriaClient::registerItemProperties);
        modBus.addListener(SurveyClient::registerTints);
        NeoForge.EVENT_BUS.addListener(SurveyClient::onTooltip);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.KNAPPING.get(), KnappingScreen::new);
        event.register(ModMenus.FIRE_PIT.get(), FirePitScreen::new);
        event.register(ModMenus.SMALL_VESSEL.get(), SmallVesselScreen::new);
        event.register(ModMenus.FORGE.get(), ForgeScreen::new);
        event.register(ModMenus.CRUCIBLE.get(), CrucibleScreen::new);
        event.register(ModMenus.ANVIL.get(), AnvilScreen::new);
        event.register(ModMenus.BLOOMERY.get(), BloomeryScreen::new);
        event.register(ModMenus.MILLSTONE.get(), MillstoneScreen::new);
        event.register(ModMenus.SAW_MILL.get(), SawMillScreen::new);
        event.register(ModMenus.TRIP_HAMMER.get(), TripHammerScreen::new);
        event.register(ModMenus.CORE_SAMPLER.get(), dev.strataindustria.client.screen.CoreSamplerScreen::new);
        event.register(ModMenus.SLUICE.get(), dev.strataindustria.client.screen.SluiceScreen::new);
        event.register(ModMenus.SOAKING_BARREL.get(), dev.strataindustria.client.screen.SoakingBarrelScreen::new);
        event.register(dev.strataindustria.registry.Tier4Menus.COKE_OVEN.get(), dev.strataindustria.client.screen.CokeOvenScreen::new);
        event.register(dev.strataindustria.registry.Tier4Menus.FIREBOX.get(), dev.strataindustria.client.screen.FireboxScreen::new);
        event.register(dev.strataindustria.registry.Tier4Menus.BRONZE_BOILER.get(), dev.strataindustria.client.screen.BoilerScreen::new);
        event.register(dev.strataindustria.registry.Tier4Menus.CRUSHER.get(), dev.strataindustria.client.screen.ProcessingScreen::new);
        event.register(dev.strataindustria.registry.Tier4Menus.WASHER.get(), dev.strataindustria.client.screen.ProcessingScreen::new);
        event.register(dev.strataindustria.registry.Tier4Menus.BLAST_FURNACE.get(), dev.strataindustria.client.screen.BlastFurnaceScreen::new);
        event.register(dev.strataindustria.registry.Tier4Menus.CONVERTER.get(), dev.strataindustria.client.screen.ConverterScreen::new);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.PIT_KILN.get(), PitKilnRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.QUERN.get(), QuernRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ANVIL.get(), AnvilRenderer::new);
        // Tier 3 spec 7: rotors spin at their network's speed.
        event.registerBlockEntityRenderer(ModBlockEntities.KINETIC_TRANSMISSION.get(), context -> new RotorRenderer<>(context, "wooden_axle",
                state -> state.hasProperty(AxleBlock.AXIS)
                        ? Direction.fromAxisAndDirection(state.getValue(AxleBlock.AXIS), Direction.AxisDirection.POSITIVE)
                        : null, 0));
        event.registerBlockEntityRenderer(ModBlockEntities.HAND_CRANK.get(), context -> new RotorRenderer<>(context, "hand_crank",
                state -> state.getValue(HandCrankBlock.FACING).getOpposite(), 0));
        event.registerBlockEntityRenderer(ModBlockEntities.WATER_WHEEL.get(), context -> new RotorRenderer<>(context, "water_wheel",
                state -> Direction.fromAxisAndDirection(state.getValue(WaterWheelBlock.AXIS), Direction.AxisDirection.POSITIVE), 1));
        event.registerBlockEntityRenderer(ModBlockEntities.MILLSTONE.get(), context -> new RotorRenderer<>(context, "millstone_runner",
                state -> Direction.UP, 0));
        event.registerBlockEntityRenderer(ModBlockEntities.TRIP_HAMMER.get(), TripHammerRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.WINDMILL_BEARING.get(), dev.strataindustria.client.render.WindmillRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SOAKING_BARREL.get(), dev.strataindustria.client.render.SoakingBarrelRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.PULLEY.get(), dev.strataindustria.client.render.PulleyRenderer::new);
        // Tier 5 spec 7.1: the dynamo's armature turns with its shaft, seen through the front window.
        event.registerBlockEntityRenderer(dev.strataindustria.registry.Tier5BlockEntities.KINETIC_DYNAMO.get(), context -> new RotorRenderer<>(context,
                "kinetic_dynamo_armature", state -> state.getValue(dev.strataindustria.electric.KineticDynamoBlock.FACING), 0));
        // Tier 4 spec 11.7: iron axles turn like the wooden ones.
        // Tier 4 spec 10.5: the steam engine's flywheel turns on the shaft it drives.
        event.registerBlockEntityRenderer(dev.strataindustria.registry.Tier4BlockEntities.STEAM_ENGINE.get(), context -> new RotorRenderer<>(context,
                "steam_engine_flywheel", state -> state.getValue(dev.strataindustria.steam.SteamEngineBlock.FACING), 0));
        event.registerBlockEntityRenderer(dev.strataindustria.registry.Tier4BlockEntities.IRON_TRANSMISSION.get(), context -> new RotorRenderer<>(context,
                "iron_axle", state -> state.hasProperty(AxleBlock.AXIS)
                        ? Direction.fromAxisAndDirection(state.getValue(AxleBlock.AXIS), Direction.AxisDirection.POSITIVE)
                        : null, 0));
        // Tier 4 spec 21.4: the blower's fan turns behind its grille.
        event.registerBlockEntityRenderer(dev.strataindustria.registry.Tier4BlockEntities.BLOWER.get(), context -> new RotorRenderer<>(context,
                "blower_fan", state -> state.getValue(dev.strataindustria.ironworks.BlowerBlock.FACING), 0));
    }

    private static void registerTints(RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(StrataIndustria.id("heat_glow"), HeatGlow.Tint.MAP_CODEC);
    }

    private static void registerItemProperties(RegisterConditionalItemModelPropertyEvent event) {
        event.register(StrataIndustria.id("glowing"), HeatGlow.Glowing.MAP_CODEC);
    }

    private static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, StrataIndustria.id("firestarter"), FirestarterHud::render);
    }
}
