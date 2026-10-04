package dev.strataindustria.registry;

import dev.strataindustria.bloomery.BloomeryMenu;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.ceramics.SmallVesselMenu;
import dev.strataindustria.fire.FirePitMenu;
import dev.strataindustria.forge.ForgeMenu;
import dev.strataindustria.metal.CrucibleMenu;
import dev.strataindustria.knapping.KnappingMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import dev.strataindustria.smithing.AnvilMenu;
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

    public static final DeferredHolder<MenuType<?>, MenuType<ForgeMenu>> FORGE =
            MENUS.register("forge", () -> IMenuTypeExtension.create((id, inventory, buf) -> new ForgeMenu(id, inventory)));

    public static final DeferredHolder<MenuType<?>, MenuType<AnvilMenu>> ANVIL =
            MENUS.register("anvil", () -> IMenuTypeExtension.create((id, inventory, buf) -> new AnvilMenu(id, inventory)));
    public static final DeferredHolder<MenuType<?>, MenuType<CrucibleMenu>> CRUCIBLE =
            MENUS.register("crucible", () -> IMenuTypeExtension.create((id, inventory, buf) -> new CrucibleMenu(id, inventory)));

    public static final DeferredHolder<MenuType<?>, MenuType<BloomeryMenu>> BLOOMERY =
            MENUS.register("bloomery", () -> IMenuTypeExtension.create(BloomeryMenu::new));

    private ModMenus() {}
}
