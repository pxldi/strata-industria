package dev.strataindustria.datagen;

import dev.strataindustria.StrataIndustria;
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
        lang.accept("fluid_type." + id + ".latex", "Latex");
        lang.accept(block + "tree_tap", "Tree Tap");
        lang.accept(block + "lv_cable", "LV Cable");
        lang.accept(block + "mv_cable", "MV Cable");
        lang.accept(block + "kinetic_dynamo", "Kinetic Dynamo");
        lang.accept(block + "battery_box", "Battery Box");
        lang.accept(block + "lv_machine_hull", "LV Machine Hull");

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
    }
}
