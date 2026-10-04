package dev.strataindustria.processing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.power.ElectricTier;
import dev.strataindustria.tanning.FluidAmount;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * What a mixer or electrolyser recipe takes and makes (tier 5 spec 11.1 and 11.2): item inputs with counts,
 * fluid inputs, item and fluid results, the LV base ticks and the lowest tier that runs it. Item results
 * come out at {@code resultTemperature} °C when it is above zero (the electrolyser's aluminium, 700 °C).
 */
public record ChemicalIo(List<ItemInput> items, List<FluidAmount> fluids, List<ItemStackTemplate> itemResults,
                         List<FluidAmount> fluidResults, int ticks, ElectricTier minTier, int resultTemperature) {
    /** An item ingredient and how many of it one operation takes from a single slot. */
    public record ItemInput(Ingredient ingredient, int count) {
        public static final Codec<ItemInput> CODEC = RecordCodecBuilder.create(i -> i.group(
                Ingredient.CODEC.fieldOf("ingredient").forGetter(ItemInput::ingredient),
                Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(ItemInput::count)
        ).apply(i, ItemInput::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, ItemInput> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, ItemInput::ingredient,
                ByteBufCodecs.VAR_INT, ItemInput::count,
                ItemInput::new);
    }

    private static final Codec<ElectricTier> TIER_CODEC = StringRepresentable.fromEnum(ElectricTier::values);
    private static final StreamCodec<io.netty.buffer.ByteBuf, ElectricTier> TIER_STREAM =
            ByteBufCodecs.VAR_INT.map(i -> ElectricTier.values()[i], ElectricTier::ordinal);

    public static final MapCodec<ChemicalIo> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ItemInput.CODEC.listOf(0, 6).optionalFieldOf("items", List.of()).forGetter(ChemicalIo::items),
            FluidAmount.CODEC.listOf(0, 3).optionalFieldOf("fluids", List.of()).forGetter(ChemicalIo::fluids),
            ItemStackTemplate.CODEC.listOf(0, 2).optionalFieldOf("item_results", List.of()).forGetter(ChemicalIo::itemResults),
            FluidAmount.CODEC.listOf(0, 3).optionalFieldOf("fluid_results", List.of()).forGetter(ChemicalIo::fluidResults),
            Codec.intRange(1, 72000).fieldOf("ticks").forGetter(ChemicalIo::ticks),
            TIER_CODEC.optionalFieldOf("min_tier", ElectricTier.LV).forGetter(ChemicalIo::minTier),
            Codec.intRange(0, 2000).optionalFieldOf("result_temperature", 0).forGetter(ChemicalIo::resultTemperature)
    ).apply(i, ChemicalIo::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChemicalIo> STREAM_CODEC = StreamCodec.composite(
            ItemInput.STREAM_CODEC.apply(ByteBufCodecs.list()), ChemicalIo::items,
            FluidAmount.STREAM_CODEC.apply(ByteBufCodecs.list()), ChemicalIo::fluids,
            ItemStackTemplate.STREAM_CODEC.apply(ByteBufCodecs.list()), ChemicalIo::itemResults,
            FluidAmount.STREAM_CODEC.apply(ByteBufCodecs.list()), ChemicalIo::fluidResults,
            ByteBufCodecs.VAR_INT, ChemicalIo::ticks,
            TIER_STREAM, ChemicalIo::minTier,
            ByteBufCodecs.VAR_INT, ChemicalIo::resultTemperature,
            ChemicalIo::new);
}
