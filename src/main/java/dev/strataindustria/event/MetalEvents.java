package dev.strataindustria.event;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.geology.OreGrade;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.metal.CastMoldItem;
import dev.strataindustria.metal.Melt;
import dev.strataindustria.metal.Quality;
import dev.strataindustria.registry.ModDataComponents;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.smithing.SmithingPattern;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Metal tooltips (spec 7 and 8): what a mold holds, and the quality of cast and smithed parts. */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class MetalEvents {
    private MetalEvents() {}

    @SubscribeEvent
    static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        CastMoldItem.contentsLine(stack).ifPresent(line -> event.getToolTip().add(1, line.copy().withStyle(ChatFormatting.GRAY)));
        Quality quality = stack.get(ModDataComponents.QUALITY.get());
        if (quality != null) event.getToolTip().add(1, quality.tooltip());
        Melt bloom = stack.get(ModDataComponents.BLOOM_CONTENTS.get());
        if (bloom != null) {
            event.getToolTip().add(1, Component.translatable(StrataIndustria.MOD_ID + ".bloomery.bloom_units", bloom.total())
                    .withStyle(ChatFormatting.GRAY));
        }
        SmithingPattern pattern = stack.get(ModDataComponents.SMITHING_PATTERN.get());
        if (pattern != null) {
            event.getToolTip().add(1, pattern.tooltip().copy().withStyle(ChatFormatting.GRAY));
        } else if (stack.is(ModItems.SMITHING_PATTERN.get())) {
            event.getToolTip().add(1, Component.translatable(StrataIndustria.MOD_ID + ".pattern.blank").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (isSulfide(stack)) {
            event.getToolTip().add(1, Component.translatable(StrataIndustria.MOD_ID + ".ore.sulfide").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (stack.has(ModDataComponents.SLAG.get())) {
            event.getToolTip().add(1, Component.translatable(StrataIndustria.MOD_ID + ".metal.slag_note").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /** Tier 4 spec 4.4: sulfide ore pieces never melt; they say so instead. */
    private static boolean isSulfide(ItemStack stack) {
        for (OreMineral mineral : OreMineral.withPieces()) {
            if (!mineral.isSulfide()) continue;
            if (stack.is(ModItems.SMALL_ORES.get(mineral).get())) return true;
            for (OreGrade grade : OreGrade.values()) {
                if (stack.is(ModItems.orePiece(mineral, grade)) || stack.is(ModItems.crushedOre(mineral, grade))) return true;
            }
        }
        return false;
    }
}
