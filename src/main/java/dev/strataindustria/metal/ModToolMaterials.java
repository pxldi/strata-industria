package dev.strataindustria.metal;

import dev.strataindustria.material.Metal;
import dev.strataindustria.registry.ModTags;
import net.minecraft.world.item.ToolMaterial;

/** Spec 8.2. Copper is the vanilla copper material; the bronzes sit around vanilla iron. */
public final class ModToolMaterials {
    public static final ToolMaterial BRONZE = new ToolMaterial(ModTags.Blocks.INCORRECT_FOR_BRONZE_TOOL, 340, 6.0f, 2.0f, 14,
            ModTags.Items.ingots(Metal.BRONZE));
    public static final ToolMaterial ARSENICAL_BRONZE = new ToolMaterial(ModTags.Blocks.INCORRECT_FOR_BRONZE_TOOL, 300, 5.5f, 2.0f, 10,
            ModTags.Items.ingots(Metal.ARSENICAL_BRONZE));
    public static final ToolMaterial BISMUTH_BRONZE = new ToolMaterial(ModTags.Blocks.INCORRECT_FOR_BRONZE_TOOL, 280, 6.0f, 2.0f, 18,
            ModTags.Items.ingots(Metal.BISMUTH_BRONZE));

    private ModToolMaterials() {}

    public static ToolMaterial of(Metal metal) {
        return switch (metal) {
            case COPPER -> ToolMaterial.COPPER;
            case BRONZE -> BRONZE;
            case ARSENICAL_BRONZE -> ARSENICAL_BRONZE;
            case BISMUTH_BRONZE -> BISMUTH_BRONZE;
            default -> throw new IllegalArgumentException(metal + " makes no tools");
        };
    }
}
