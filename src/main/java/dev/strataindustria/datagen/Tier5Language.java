package dev.strataindustria.datagen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.electric.machine.ElectricMachineBlockEntity;
import dev.strataindustria.power.ElectricStatus;
import dev.strataindustria.rubber.TreeTapBlockEntity;
import java.util.function.BiConsumer;

/** English names and messages for tier 5 (electric). */
final class Tier5Language {
    private Tier5Language() {}

    static void add(BiConsumer<String, String> lang) {
        String id = StrataIndustria.MOD_ID;
        String item = "item." + id + ".", block = "block." + id + ".";
        lang.accept(item + "magnet", "Magnet");
        lang.accept(item + "copper_rod", "Copper Rod");
        lang.accept(item + "copper_wire", "Copper Wire");
        lang.accept(item + "lead_plate", "Lead Plate");
        lang.accept(item + "raw_rubber", "Raw Rubber");
        lang.accept(item + "compounded_rubber", "Compounded Rubber");
        lang.accept(item + "rubber", "Rubber");
        lang.accept(item + "latex_bucket", "Latex Bucket");
        lang.accept(item + "red_alloy_rod", "Red Alloy Rod");
        lang.accept(item + "red_alloy_wire", "Red Alloy Wire");
        lang.accept(item + "draw_plate", "Draw Plate");
        lang.accept(item + "circuit_board", "Circuit Board");
        lang.accept(item + "basic_circuit", "Basic Circuit");
        lang.accept(item + "electric_motor", "Electric Motor");
        lang.accept(dev.strataindustria.metal.CrucibleStatus.REDSTONE_WAITING.key(), "Redstone needs molten copper");
        lang.accept("fluid_type." + id + ".latex", "Latex");
        lang.accept(block + "tree_tap", "Tree Tap");
        lang.accept(block + "lv_cable", "LV Cable");
        lang.accept(block + "mv_cable", "MV Cable");
        lang.accept(block + "kinetic_dynamo", "Kinetic Dynamo");
        lang.accept(block + "battery_box", "Battery Box");
        lang.accept(block + "lv_machine_hull", "LV Machine Hull");
        lang.accept(block + "electric_furnace", "Electric Furnace");
        lang.accept(block + "macerator", "Macerator");
        lang.accept(block + "steam_turbine", "Steam Turbine");
        lang.accept(block + "combustion_generator", "Combustion Generator");
        String turbine = id + ".steam_turbine.", combustion = id + ".combustion_generator.";
        lang.accept(turbine + "running", "Rotor at %s%%, steam at %s bar, up to %s J/t");
        lang.accept(turbine + "no_steam", "No steam: needs at least %s bar");
        lang.accept(turbine + "spinning_down", "Spinning down: steam below %s bar");
        lang.accept(combustion + "empty", "Out of fuel");
        lang.accept(combustion + "tank", "%s: %s / %s mB");

        // Spec 10.1: machine status lines and screen tooltips.
        String machine = id + ".electric_machine.";
        lang.accept(ElectricMachineBlockEntity.Status.EMPTY.key(), "Idle");
        lang.accept(ElectricMachineBlockEntity.Status.WORKING.key(), "Running");
        lang.accept(ElectricMachineBlockEntity.Status.LOW_POWER.key(), "Low power (%s%%)");
        lang.accept(ElectricMachineBlockEntity.Status.NO_POWER.key(), "No power");
        lang.accept(ElectricMachineBlockEntity.Status.NO_RECIPE.key(), "Can't process this");
        lang.accept(ElectricMachineBlockEntity.Status.OUTPUT_FULL.key(), "Output full");
        lang.accept(ElectricMachineBlockEntity.Status.OVERVOLTAGE.key(), "Overvoltage");
        lang.accept(ElectricMachineBlockEntity.Status.TOO_FAR.key(), "Too far from a source");
        lang.accept(machine + "buffer", "Buffer: %s%%");
        lang.accept(machine + "eject_on", "Auto-eject on: outputs go into the block behind");
        lang.accept(machine + "eject_off", "Auto-eject off");

        // Spec 6.5: diagnostics and status lines.
        String electric = id + ".electric.";
        lang.accept(electric + "network", "%s network: %s / %s J/t, loss up to %s%%, stored %s / %s J");
        lang.accept(electric + "consumer", "%s J/t requested, %s J/t drawn (%s%% loss), %s");
        lang.accept(electric + "source", "Giving %s J/t, %s");
        lang.accept(electric + "storage", "%s / %s J, %s J/t %s");
        lang.accept(electric + "charging", "charging");
        lang.accept(electric + "discharging", "discharging");
        lang.accept(electric + "holding", "holding");
        lang.accept(electric + "no_demand", "spinning, no demand");
        lang.accept(electric + "limited_by", "Limited by %s cable (%s J/t)");
        lang.accept(ElectricStatus.NO_SOURCE.key(), "No power source");
        lang.accept(ElectricStatus.RUNNING.key(), "running");
        lang.accept(ElectricStatus.LOW_POWER.key(), "Low power (%s%%)");
        lang.accept(ElectricStatus.IDLE.key(), "idle");
        lang.accept(ElectricStatus.OVERVOLTAGE.key(), "Overvoltage: %s device on an %s network. Use a transformer");
        lang.accept(ElectricStatus.CABLE_OVERVOLTAGE.key(), "Overvoltage: %s cable at %s %s %s");
        lang.accept(ElectricStatus.TOO_FAR.key(), "Too far from a source (%s%% loss)");
        lang.accept(ElectricStatus.TOO_LARGE.key(), "Network too large");
        lang.accept(id + ".battery_box.charge", "%s battery: %s / %s J");

        // Spec 5.1: the tap's cup and why it is not dripping.
        lang.accept(id + ".tree_tap.cup", "%s: %s / %s mB");
        lang.accept(TreeTapBlockEntity.Status.NO_TREE.key(), "No living tree");
        lang.accept(TreeTapBlockEntity.Status.TAPPED_OUT.key(), "This tree is tapped out");
        lang.accept(TreeTapBlockEntity.Status.WORKING.key(), "Dripping");
        lang.accept(id + ".soaking_barrel.fluid.latex", "Latex");

        String subtitles = "subtitles." + id + ".";
        lang.accept(subtitles + "electric.overvoltage", "Electricity cracks");
        lang.accept(subtitles + "electric.spark", "Cable sparks");
        lang.accept(subtitles + "kinetic_dynamo.run", "Dynamo whirs");
        lang.accept(subtitles + "battery_box.charge", "Battery hums");
        lang.accept(subtitles + "block.tree_tap.place", "Tree tap knocked in");
        lang.accept(subtitles + "block.tree_tap.drip", "Latex drips");
        lang.accept(subtitles + "block.machine.power_on", "Machine powers up");
        lang.accept(subtitles + "block.machine.power_off", "Machine powers down");
        lang.accept(subtitles + "block.machine.low_power", "Machine beeps");
        lang.accept(subtitles + "block.electric_furnace.run", "Electric furnace hums");
        lang.accept(subtitles + "block.macerator.grind", "Macerator grinds");
        lang.accept(subtitles + "block.steam_turbine.run", "Steam turbine whines");
        lang.accept(subtitles + "block.steam_turbine.spin_down", "Steam turbine spins down");
        lang.accept(subtitles + "block.combustion_generator.ignite", "Combustion generator coughs");
        lang.accept(subtitles + "block.combustion_generator.run", "Combustion generator putters");
    }
}
