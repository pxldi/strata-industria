package dev.strataindustria.datagen;

import dev.strataindustria.StrataIndustria;
import java.util.function.BiConsumer;

/** Redesign R5 translations: the subtitles of felling a tree. */
final class FellingData {
    private FellingData() {}

    static void lang(BiConsumer<String, String> add) {
        String subtitles = "subtitles." + StrataIndustria.MOD_ID + ".felling.";
        add.accept(subtitles + "notch", "Axe bites into the trunk");
        add.accept(subtitles + "creak", "Tree creaks");
        add.accept(subtitles + "crash", "Tree crashes down");
    }
}
