package dev.strataindustria.electric;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.registry.Tier5BlockEntities;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** The one place the mod exposes the FE capability (spec 8.5): the front face of an energy adapter. */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class EnergyAdapterCapabilities {
    private EnergyAdapterCapabilities() {}

    @SubscribeEvent
    static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, Tier5BlockEntities.ENERGY_ADAPTER.get(),
                (adapter, side) -> side == adapter.front() ? adapter.feHandler() : null);
    }
}
