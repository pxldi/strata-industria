package dev.strataindustria.registry;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.ceramics.SmallVesselMenu;
import dev.strataindustria.fire.FirePitMenu;
import dev.strataindustria.knapping.KnappingMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, StrataIndustria.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<KnappingMenu>> KNAPPING =
            MENUS.register("knapping", () -> IMenuTypeExtension.create(KnappingMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<FirePitMenu>> FIRE_PIT =
            MENUS.register("fire_pit", () -> IMenuTypeExtension.create((id, inventory, buf) -> new FirePitMenu(id, inventory)));

    public static final DeferredHolder<MenuType<?>, MenuType<SmallVesselMenu>> SMALL_VESSEL =
            MENUS.register("small_vessel", () -> IMenuTypeExtension.create(SmallVesselMenu::new));

    private ModMenus() {}
}
