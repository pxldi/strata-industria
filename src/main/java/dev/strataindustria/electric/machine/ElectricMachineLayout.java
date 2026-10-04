package dev.strataindustria.electric.machine;

import dev.strataindustria.power.ElectricTier;

/**
 * Slot counts and screen positions of the tier 5 item machines (spec 10.1). Each machine has one lane
 * (an input, its arrow and its outputs) per parallel operation: one at LV, two at MV. The block entity
 * always holds both lanes' slots so a tier change never loses items; only the active lanes are used.
 */
public enum ElectricMachineLayout {
    ELECTRIC_FURNACE("electric_furnace", 1),
    MACERATOR("macerator", 3),
    WIREMILL("wiremill", 1),
    BENDER("bender", 1),
    LATHE("lathe", 1);

    public static final int MAX_LANES = 2;
    public static final int WIDTH = 176, HEIGHT = 176, INVENTORY_Y = 94, STATUS_Y = 74;
    public static final int INPUT_X = 44, ARROW_X = 68, OUTPUT_X = 98;
    /** The power bar's well, the auto-eject button and the tier badge. */
    public static final int BAR_X = 17, BAR_Y = 18, BAR_W = 8, BAR_H = 52;
    public static final int EJECT_X = 154, EJECT_Y = 56, EJECT_SIZE = 14;
    /** The mode button of a machine that has one (the lathe), above the eject button. */
    public static final int MODE_X = 154, MODE_Y = 38;

    private final String name;
    private final int outputs;

    ElectricMachineLayout(String name, int outputs) {
        this.name = name;
        this.outputs = outputs;
    }

    public String id() {
        return name;
    }

    /** Whether the screen has a mode button (menu button 1). */
    public boolean hasMode() {
        return this == LATHE;
    }

    /** Outputs per lane. */
    public int outputs() {
        return outputs;
    }

    public int slots() {
        return MAX_LANES * (1 + outputs);
    }

    public int inputSlot(int lane) {
        return lane;
    }

    public int outputSlot(int lane, int output) {
        return MAX_LANES + lane * outputs + output;
    }

    /** The screen row of {@code lane} when {@code lanes} are shown. */
    public static int laneY(int lanes, int lane) {
        return lanes == 1 ? 35 : 24 + lane * 22;
    }

    public static int outputX(int output) {
        return OUTPUT_X + output * 18;
    }

    /** The background texture: the MV screen shows both lanes. */
    public String texture(ElectricTier tier) {
        return tier == ElectricTier.MV ? name + "_mv" : name;
    }

    /** Container data: one progress value per lane, then status, power percent, buffer percent, auto-eject, mode. */
    public static final int STATUS = MAX_LANES, POWER = MAX_LANES + 1, BUFFER = MAX_LANES + 2, EJECT = MAX_LANES + 3, MODE = MAX_LANES + 4,
            DATA_COUNT = MAX_LANES + 5;
}
