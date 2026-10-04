package dev.strataindustria.tanning;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.material.Fluid;

/** An amount of one fluid in millibuckets, as recipes name it. */
public record FluidAmount(Fluid fluid, int amount) {
    public static final Codec<FluidAmount> CODEC = RecordCodecBuilder.create(i -> i.group(
            BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid").forGetter(FluidAmount::fluid),
            Codec.intRange(1, SoakingBarrelBlockEntity.CAPACITY).fieldOf("amount").forGetter(FluidAmount::amount)
    ).apply(i, FluidAmount::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, FluidAmount> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.registry(Registries.FLUID), FluidAmount::fluid,
            ByteBufCodecs.VAR_INT, FluidAmount::amount,
            FluidAmount::new);
}
