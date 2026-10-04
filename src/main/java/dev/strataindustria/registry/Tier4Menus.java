package dev.strataindustria.registry;

import dev.strataindustria.coking.CokeOvenMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Tier 4 screens. */
public final class Tier4Menus {
    public static final DeferredHolder<MenuType<?>, MenuType<CokeOvenMenu>> COKE_OVEN =
            ModMenus.MENUS.register("coke_oven", () -> IMenuTypeExtension.create(CokeOvenMenu::new));

    public static void init() {}

    private Tier4Menus() {}
}
