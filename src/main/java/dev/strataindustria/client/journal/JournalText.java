package dev.strataindustria.client.journal;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.journal.JournalState;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

/** The words of the leads notebook, with fallbacks so goals whose notebook text is not written yet still read. */
public final class JournalText {
    static final String UI = "journal." + StrataIndustria.MOD_ID + ".ui.";

    private JournalText() {}

    public static Component ui(String key, Object... args) {
        return Component.translatable(UI + key, args);
    }

    /** The goal's name, as on the old checklist. */
    public static Component title(JournalState.Lead lead) {
        return Component.translatable(lead.key());
    }

    /** The open question; the goal's name if no question is written. */
    public static Component question(JournalState.Lead lead) {
        String key = lead.key() + ".lead";
        return Language.getInstance().has(key) ? Component.translatable(key) : title(lead);
    }

    public static Component hint(JournalState.Lead lead) {
        return Component.translatable(lead.key() + ".hint");
    }

    /** The line written when the lead closes, or null. */
    public static Component closing(JournalState.Lead lead) {
        String key = lead.key() + ".note";
        return Language.getInstance().has(key) ? Component.translatable(key) : null;
    }

    /** "t3/iron_ore" belongs to tier 3. */
    public static int tier(JournalState.Lead lead) {
        String path = lead.path();
        int slash = path.indexOf('/');
        try {
            return Integer.parseInt(path.substring(1, slash));
        } catch (RuntimeException e) {
            return 0;
        }
    }

    public static Component chapter(int tier) {
        String key = UI + "chapter." + tier;
        return Language.getInstance().has(key) ? Component.translatable(key) : ui("chapter", tier);
    }

    public static Component note(JournalState.Note note) {
        Object[] args = new Object[note.args().size()];
        for (int i = 0; i < args.length; i++) args[i] = arg(note.args().get(i));
        return Component.translatable(note.key(), args);
    }

    /** One argument: a lang key, or several joined with {@link Journal#ARG_LIST} read as "a, b and c". */
    static Component arg(String arg) {
        if (!arg.contains(Journal.ARG_LIST)) return Component.translatable(arg);
        List<String> parts = new ArrayList<>(List.of(arg.split(java.util.regex.Pattern.quote(Journal.ARG_LIST))));
        MutableComponent out = Component.empty();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) out.append(i == parts.size() - 1 ? ui("list.and") : ui("list.comma"));
            out.append(Component.translatable(parts.get(i)));
        }
        return out;
    }

    public static ItemStack icon(net.minecraft.resources.Identifier id) {
        return BuiltInRegistries.ITEM.getOptional(id).map(ItemStack::new).orElse(ItemStack.EMPTY);
    }
}
