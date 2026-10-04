package dev.strataindustria.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.Config;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.conditions.ICondition;

/**
 * Data condition on a boolean switch in the {@code vanilla} config section. Used to drop vanilla
 * recipes: the override file only loads while the switch has {@code value}.
 */
public record ConfigCondition(String key, boolean value) implements ICondition {
    public static final MapCodec<ConfigCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("key").forGetter(ConfigCondition::key),
            Codec.BOOL.optionalFieldOf("value", true).forGetter(ConfigCondition::value)
    ).apply(i, ConfigCondition::new));

    static final Map<String, ModConfigSpec.BooleanValue> SWITCHES = Map.ofEntries(
            Map.entry("vanilla.removeWoodTools", Config.REMOVE_WOOD_TOOLS),
            Map.entry("vanilla.removeStoneTools", Config.REMOVE_STONE_TOOLS),
            Map.entry("vanilla.planksNeedTools", Config.PLANKS_NEED_TOOLS),
            Map.entry("vanilla.removeCampfire", Config.REMOVE_CAMPFIRE),
            Map.entry("vanilla.gateFurnace", Config.GATE_FURNACE),
            Map.entry("vanilla.replaceCopperGear", Config.REPLACE_COPPER_GEAR),
            Map.entry("vanilla.removeFurnaceCharcoal", Config.REMOVE_FURNACE_CHARCOAL),
            Map.entry("vanilla.removeOreSmelting", Config.REMOVE_ORE_SMELTING),
            Map.entry("vanilla.removeBlastFurnace", Config.REMOVE_BLAST_FURNACE),
            Map.entry("vanilla.replaceIronGear", Config.REPLACE_IRON_GEAR),
            Map.entry("vanilla.replaceGoldGear", Config.REPLACE_GOLD_GEAR));

    @Override
    public boolean test(IContext context) {
        ModConfigSpec.BooleanValue sw = SWITCHES.get(key);
        // An unknown key keeps the content loaded rather than silently dropping it.
        return sw == null ? value : sw.getAsBoolean() == value;
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
