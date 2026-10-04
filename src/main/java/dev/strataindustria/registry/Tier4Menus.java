package dev.strataindustria.registry;

import dev.strataindustria.coking.CokeOvenMenu;
import dev.strataindustria.steam.BoilerMenu;
import dev.strataindustria.steam.FireboxMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Tier 4 screens. */
public final class Tier4Menus {
    public static final DeferredHolder<MenuType<?>, MenuType<CokeOvenMenu>> COKE_OVEN =
            ModMenus.MENUS.register("coke_oven", () -> IMenuTypeExtension.create(CokeOvenMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<FireboxMenu>> FIREBOX =
            ModMenus.MENUS.register("firebox", () -> IMenuTypeExtension.create(FireboxMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<BoilerMenu>> BRONZE_BOILER =
            ModMenus.MENUS.register("bronze_boiler", () -> IMenuTypeExtension.create(BoilerMenu::new));

    public static void init() {}

    private Tier4Menus() {}
}
