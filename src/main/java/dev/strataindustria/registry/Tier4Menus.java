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
    public static final DeferredHolder<MenuType<?>, MenuType<dev.strataindustria.ceramics.KilnMenu>> KILN =
            ModMenus.MENUS.register("kiln", () -> IMenuTypeExtension.create(dev.strataindustria.ceramics.KilnMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<dev.strataindustria.roasting.RoasterMenu>> ROASTER =
            ModMenus.MENUS.register("roaster", () -> IMenuTypeExtension.create(dev.strataindustria.roasting.RoasterMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<dev.strataindustria.metal.SmelterMenu>> SMELTER =
            ModMenus.MENUS.register("smelter", () -> IMenuTypeExtension.create(dev.strataindustria.metal.SmelterMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<dev.strataindustria.steam.SteamHammerMenu>> STEAM_HAMMER =
            ModMenus.MENUS.register("steam_hammer", () -> IMenuTypeExtension.create(dev.strataindustria.steam.SteamHammerMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<BoilerMenu>> BRONZE_BOILER =
            ModMenus.MENUS.register("bronze_boiler", () -> IMenuTypeExtension.create((id, inventory, buf) ->
                    new BoilerMenu(Tier4Menus.BRONZE_BOILER.get(), id, inventory, buf)));
    public static final DeferredHolder<MenuType<?>, MenuType<BoilerMenu>> BOILER_CONTROLLER =
            ModMenus.MENUS.register("boiler_controller", () -> IMenuTypeExtension.create((id, inventory, buf) ->
                    new BoilerMenu(Tier4Menus.BOILER_CONTROLLER.get(), id, inventory, buf)));

    public static final DeferredHolder<MenuType<?>, MenuType<dev.strataindustria.processing.ProcessingMenu>> CRUSHER =
            ModMenus.MENUS.register("crusher", () -> IMenuTypeExtension.create((id, inventory, buf) ->
                    new dev.strataindustria.processing.ProcessingMenu(dev.strataindustria.processing.MachineLayout.CRUSHER, id, inventory, buf)));

    public static final DeferredHolder<MenuType<?>, MenuType<dev.strataindustria.processing.ProcessingMenu>> WASHER =
            ModMenus.MENUS.register("washer", () -> IMenuTypeExtension.create((id, inventory, buf) ->
                    new dev.strataindustria.processing.ProcessingMenu(dev.strataindustria.processing.MachineLayout.WASHER, id, inventory, buf)));

    public static final DeferredHolder<MenuType<?>, MenuType<dev.strataindustria.ironworks.BlastFurnaceMenu>> BLAST_FURNACE =
            ModMenus.MENUS.register("blast_furnace", () -> IMenuTypeExtension.create(dev.strataindustria.ironworks.BlastFurnaceMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<dev.strataindustria.ironworks.ConverterMenu>> CONVERTER =
            ModMenus.MENUS.register("converter", () -> IMenuTypeExtension.create(dev.strataindustria.ironworks.ConverterMenu::new));

    public static void init() {}

    private Tier4Menus() {}
}
