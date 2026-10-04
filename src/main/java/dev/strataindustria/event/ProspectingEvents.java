package dev.strataindustria.event;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.prospecting.CoreSample;
import dev.strataindustria.registry.ModDataComponents;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Core sample tooltip (tier 3 spec 8.5): where it was drilled and the three main finds. */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class ProspectingEvents {
    private ProspectingEvents() {}

    @SubscribeEvent
    static void onTooltip(ItemTooltipEvent event) {
        CoreSample sample = event.getItemStack().get(ModDataComponents.CORE_SAMPLE.get());
        if (sample == null) return;
        List<Component> lines = new ArrayList<>();
        sample.tooltip(lines);
        event.getToolTip().addAll(Math.min(1, event.getToolTip().size()), lines);
    }
}
