package dev.strataindustria.registry;

import dev.strataindustria.electric.machine.ElectricMachineLayout;
import dev.strataindustria.electric.machine.ElectricMachineMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Tier 5 screens. */
public final class Tier5Menus {
    public static final DeferredHolder<MenuType<?>, MenuType<ElectricMachineMenu>> ELECTRIC_FURNACE = machine(ElectricMachineLayout.ELECTRIC_FURNACE);
    public static final DeferredHolder<MenuType<?>, MenuType<ElectricMachineMenu>> MACERATOR = machine(ElectricMachineLayout.MACERATOR);
    public static final DeferredHolder<MenuType<?>, MenuType<ElectricMachineMenu>> WIREMILL = machine(ElectricMachineLayout.WIREMILL);
    public static final DeferredHolder<MenuType<?>, MenuType<ElectricMachineMenu>> BENDER = machine(ElectricMachineLayout.BENDER);
    public static final DeferredHolder<MenuType<?>, MenuType<ElectricMachineMenu>> LATHE = machine(ElectricMachineLayout.LATHE);

    private static DeferredHolder<MenuType<?>, MenuType<ElectricMachineMenu>> machine(ElectricMachineLayout layout) {
        return ModMenus.MENUS.register(layout.id(), () -> IMenuTypeExtension.create((id, inventory, buf) ->
                new ElectricMachineMenu(layout, id, inventory, buf)));
    }

    public static void init() {}

    private Tier5Menus() {}
}
