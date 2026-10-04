package dev.strataindustria.structure;

import dev.strataindustria.StrataIndustria;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Handwritten ledger pages (structures v2 4.2): a {@code survey_notes} sheet carrying a text key instead of
 * a deposit. Each place has a few pages that tell what happened there. Keys read {@code <place>.<n>}, and
 * the text is the translation of {@code item.strataindustria.ledger.<key>}.
 */
public final class Ledgers {
    /** Pages per place, by layout id. A structure rebuild adds its own pages here and in its language lines. */
    public static final Map<String, Integer> PAGES = new LinkedHashMap<>();

    static {
        PAGES.put("charcoal_burners_clearing", 2);
        PAGES.put("prospector_camp", 2);
        PAGES.put("mining_camp", 2);
        PAGES.put("collapsed_adit", 2);
        PAGES.put("ruined_bloomery", 2);
        PAGES.put("placer_workings", 2);
    }

    private Ledgers() {}

    public static String key(String place, int page) {
        return place + "." + page;
    }

    public static ItemStack page(String key) {
        ItemStack stack = new ItemStack(StructureContent.SURVEY_NOTES.get());
        stack.set(StructureContent.LEDGER.get(), key);
        return stack;
    }

    public static Component text(String key) {
        return Component.translatable("item." + StrataIndustria.MOD_ID + ".ledger." + key);
    }

    /** The place a page belongs to, named as in the journal. */
    public static Component place(String key) {
        String place = key.substring(0, key.lastIndexOf('.'));
        return Component.translatable("journal." + StrataIndustria.MOD_ID + ".place." + place);
    }
}
