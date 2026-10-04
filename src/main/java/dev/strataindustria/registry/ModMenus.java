package dev.strataindustria.registry;

import dev.strataindustria.bloomery.BloomeryMenu;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.ceramics.SmallVesselMenu;
import dev.strataindustria.fire.FirePitMenu;
import dev.strataindustria.forge.ForgeMenu;
import dev.strataindustria.metal.CrucibleMenu;
import dev.strataindustria.machine.MillstoneMenu;
import dev.strataindustria.machine.SawMillMenu;
import dev.strataindustria.machine.TripHammerMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, StrataIndustria.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<FirePitMenu>> FIRE_PIT =
            MENUS.register("fire_pit", () -> IMenuTypeExtension.create((id, inventory, buf) -> new FirePitMenu(id, inventory)));

    public static final DeferredHolder<MenuType<?>, MenuType<SmallVesselMenu>> SMALL_VESSEL =
            MENUS.register("small_vessel", () -> IMenuTypeExtension.create(SmallVesselMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ForgeMenu>> FORGE =
            MENUS.register("forge", () -> IMenuTypeExtension.create((id, inventory, buf) -> new ForgeMenu(id, inventory)));

    public static final DeferredHolder<MenuType<?>, MenuType<CrucibleMenu>> CRUCIBLE =
            MENUS.register("crucible", () -> IMenuTypeExtension.create((id, inventory, buf) -> new CrucibleMenu(id, inventory)));

    public static final DeferredHolder<MenuType<?>, MenuType<BloomeryMenu>> BLOOMERY =
            MENUS.register("bloomery", () -> IMenuTypeExtension.create(BloomeryMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MillstoneMenu>> MILLSTONE =
            MENUS.register("millstone", () -> IMenuTypeExtension.create(MillstoneMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<SawMillMenu>> SAW_MILL =
            MENUS.register("saw_mill", () -> IMenuTypeExtension.create(SawMillMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<TripHammerMenu>> TRIP_HAMMER =
            MENUS.register("trip_hammer", () -> IMenuTypeExtension.create(TripHammerMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<dev.strataindustria.machine.CoreSamplerMenu>> CORE_SAMPLER =
            MENUS.register("core_sampler", () -> IMenuTypeExtension.create(dev.strataindustria.machine.CoreSamplerMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<dev.strataindustria.tanning.SoakingBarrelMenu>> SOAKING_BARREL =
            MENUS.register("soaking_barrel", () -> IMenuTypeExtension.create(dev.strataindustria.tanning.SoakingBarrelMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<dev.strataindustria.washing.SluiceMenu>> SLUICE =
            MENUS.register("sluice", () -> IMenuTypeExtension.create(dev.strataindustria.washing.SluiceMenu::new));

    private ModMenus() {}
}
