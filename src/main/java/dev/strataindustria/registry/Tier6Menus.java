package dev.strataindustria.registry;

import dev.strataindustria.oil.OilStillMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Tier 6 screens. */
public final class Tier6Menus {
    public static final DeferredHolder<MenuType<?>, MenuType<OilStillMenu>> OIL_STILL =
            ModMenus.MENUS.register("oil_still", () -> IMenuTypeExtension.create(OilStillMenu::new));

    public static void init() {}

    private Tier6Menus() {}
}
