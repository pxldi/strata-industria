package dev.strataindustria.registry;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.casting.PatternBlankItem;
import dev.strataindustria.casting.PatternItem;
import dev.strataindustria.ceramics.MoldType;
import dev.strataindustria.metal.CastMoldItem;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/** Pattern casting: carved wooden patterns, sand flasks and the sand molds they press. */
public final class PatternRegistry {
    /** Shape ids in registry order: "ingot", "gear", then every tool part by {@link MoldType#id()}. */
    public static final java.util.List<String> SHAPES;
    public static final Map<String, DeferredItem<PatternItem>> PATTERNS = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<CastMoldItem>> SAND_MOLDS = new LinkedHashMap<>();

    /** A plank cut to size; carving it opens the forming grid. */
    public static final DeferredItem<PatternBlankItem> PATTERN_BLANK = ModItems.ITEMS.registerItem("pattern_blank", PatternBlankItem::new,
            p -> p.stacksTo(16));
    /** A wooden frame packed with damp sand, ready for a pattern. */
    public static final DeferredItem<Item> SAND_FLASK = ModItems.ITEMS.registerSimpleItem("sand_flask", p -> p.stacksTo(16));

    /** A pattern pressed into a flask. */
    public static final DeferredHolder<SoundEvent, SoundEvent> PATTERN_PRESS = sound("pattern.press");
    /** A cut of the knife while carving. */
    public static final DeferredHolder<SoundEvent, SoundEvent> PATTERN_CARVE = sound("pattern.carve");
    /** The carving is done. */
    public static final DeferredHolder<SoundEvent, SoundEvent> PATTERN_FINISH = sound("pattern.finish");

    static {
        java.util.List<String> shapes = new java.util.ArrayList<>(java.util.List.of("ingot", "gear"));
        for (MoldType type : MoldType.values()) shapes.add(type.id());
        SHAPES = java.util.List.copyOf(shapes);
        for (String shape : SHAPES) {
            MoldType type = null;
            for (MoldType t : MoldType.values()) if (t.id().equals(shape)) type = t;
            MoldType moldType = type;
            boolean gear = shape.equals("gear");
            DeferredItem<CastMoldItem> sand = ModItems.ITEMS.registerItem(shape + "_sand_mold",
                    p -> new CastMoldItem(moldType, gear, true, true, p), p -> p.stacksTo(16));
            SAND_MOLDS.put(shape, sand);
            PATTERNS.put(shape, ModItems.ITEMS.registerItem(shape + "_pattern", p -> new PatternItem(sand, p), p -> p.stacksTo(1)));
        }
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return ModSounds.SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(StrataIndustria.id(name)));
    }

    public static void init() {}

    private PatternRegistry() {}
}
