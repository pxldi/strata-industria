package dev.strataindustria.structure;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.geology.VeinType;
import dev.strataindustria.survey.SurveyText;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * A cut and polished sample of a mineral (structures v2 4.1). It is a collectible: not smeltable, not an
 * ore, in no tag, so it unlocks and completes nothing. The tooltip says where the mineral forms.
 */
public class MineralSpecimenItem extends Item {
    public MineralSpecimenItem(Properties properties) {
        super(properties);
    }

    public static ItemStack of(OreMineral mineral) {
        ItemStack stack = new ItemStack(StructureContent.MINERAL_SPECIMEN.get());
        stack.set(StructureContent.MINERAL.get(), mineral.id());
        return stack;
    }

    /** Mineral id, or null for a blank chip. */
    public static String mineral(ItemStack stack) {
        return stack.get(StructureContent.MINERAL.get());
    }

    @Override
    public Component getName(ItemStack stack) {
        String mineral = mineral(stack);
        if (mineral == null) return super.getName(stack);
        return Component.translatable("item." + StrataIndustria.MOD_ID + ".mineral_specimen.of", SurveyText.mineralName(mineral));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
            TooltipFlag flag) {
        String id = mineral(stack);
        OreMineral mineral = id == null ? null : dev.strataindustria.survey.Surveyor.mineral(id);
        HolderLookup.Provider registries = context.registries();
        if (mineral == null || registries == null) return;
        Set<String> hosts = new LinkedHashSet<>();
        int low = Integer.MAX_VALUE, high = Integer.MIN_VALUE;
        var veins = registries.lookup(VeinType.REGISTRY);
        if (veins.isEmpty()) return;
        for (var holder : veins.get().listElements().toList()) {
            VeinType type = holder.value();
            if (type.minerals().stream().noneMatch(m -> m.mineral() == mineral)) continue;
            for (Rock rock : type.hosts()) hosts.add(rock.id());
            low = Math.min(low, type.minY());
            high = Math.max(high, type.maxY());
        }
        if (hosts.isEmpty()) return;
        String prefix = "item." + StrataIndustria.MOD_ID + ".mineral_specimen.";
        Component rocks = Component.empty();
        boolean first = true;
        for (String rock : hosts) {
            if (!first) rocks = rocks.copy().append(", ");
            rocks = rocks.copy().append(Component.translatable(StrataIndustria.MOD_ID + ".knapped_from.material." + rock));
            first = false;
        }
        tooltip.accept(Component.translatable(prefix + "forms_in", rocks).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable(prefix + "depth", low, high).withStyle(ChatFormatting.GRAY));
    }
}
