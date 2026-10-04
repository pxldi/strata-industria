package dev.strataindustria.signs;

import dev.strataindustria.registry.ModBlocks;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;

/** The ground stains of the ore signs (redesign R7), registered into the shared block register. They have no item. */
public final class SignBlocks {
    public static final Map<Stain, DeferredBlock<StainBlock>> STAINS = new EnumMap<>(Stain.class);

    static {
        for (Stain stain : Stain.values()) {
            STAINS.put(stain, ModBlocks.BLOCKS.registerBlock(stain.id(), p -> new StainBlock(stain, p),
                    p -> p.mapColor(switch (stain) {
                        case GOSSAN -> MapColor.COLOR_ORANGE;
                        case MALACHITE_BLOOM -> MapColor.EMERALD;
                        case SULFUR_CRUST -> MapColor.COLOR_YELLOW;
                    }).noCollision().noOcclusion().replaceable().instabreak().sound(SoundType.SAND).pushReaction(PushReaction.POPPED)));
        }
    }

    public static void init() {}

    private SignBlocks() {}
}
