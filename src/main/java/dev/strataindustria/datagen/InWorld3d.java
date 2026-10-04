package dev.strataindustria.datagen;

import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.renderer.item.SelectItemModel;
import net.minecraft.client.renderer.item.properties.select.DisplayContext;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * Items that show a flat sprite in inventories and a 3D model everywhere else (hand, ground, shelves, frames, anvils),
 * the way vanilla's spyglass and trident do. See STYLE_GUIDE section 11.
 */
public final class InWorld3d {
    private InWorld3d() {}

    /** The sprite comes from the item texture (flat item model); `model3d` is any model id, usually a block model. */
    public static void apply(ItemModelGenerators itemModels, Item item, Identifier model3d) {
        Identifier sprite = itemModels.createFlatItemModel(item, ModelTemplates.FLAT_ITEM);
        itemModels.itemModelOutput.accept(item, ItemModelUtils.select(new DisplayContext(),
                ItemModelUtils.plainModel(model3d),
                ItemModelUtils.when(ItemDisplayContext.GUI, ItemModelUtils.plainModel(sprite))));
    }
}
