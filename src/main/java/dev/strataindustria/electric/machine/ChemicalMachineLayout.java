package dev.strataindustria.electric.machine;

/**
 * Slots, tanks and screen positions of the fluid machines (tier 5 spec 10.1, 10.7, 11.1 and 11.2). The mixer
 * and electrolyser have two item inputs; the mixer takes three fluids and makes one, the electrolyser takes one
 * and makes three. The assembler has six item inputs, one fluid tank and one output. Tanks are numbered inputs
 * first, then outputs, and each holds 4000 mB.
 */
public enum ChemicalMachineLayout {
    MIXER("mixer", 2, 3, 1, 1, new int[] {36, 54, 72, 118}, 92),
    ELECTROLYSER("electrolyser", 2, 1, 3, 2, new int[] {36, 100, 118, 136}, 66),
    ASSEMBLER("assembler", 6, 1, 0, 1, new int[] {36}, 116),
    /** Two item inputs and one output in a row, no tanks, a mode button (spec 10.9). */
    EXTRUDER("extruder", 2, 0, 0, 1, new int[0], 86);

    public static final int TANK_CAPACITY = 4000;
    public static final int WIDTH = 176, HEIGHT = 176, INVENTORY_Y = 94, STATUS_Y = 74;
    /** A tank's glass: the inside is 16 x 34 and the frame round it 18 x 36. */
    public static final int TANK_Y = 18, TANK_W = 16, TANK_H = 34, SLOT_Y = 54, ARROW_Y = 36;
    public static final int BAR_X = 17, BAR_Y = 18, BAR_W = 8, BAR_H = 52;
    public static final int EJECT_X = 154, EJECT_Y = 56, EJECT_SIZE = 14;
    public static final int MAX_TANKS = 4;
    /** The mode button of a machine that has one (the extruder), above the eject button. */
    public static final int MODE_X = 154, MODE_Y = 38;

    /** Container data: status, power percent, buffer percent, auto-eject, progress, the fluid of a full tank, then two values per tank, then the mode. */
    public static final int STATUS = 0, POWER = 1, BUFFER = 2, EJECT = 3, PROGRESS = 4, FULL_FLUID = 5, TANKS = 6,
            MODE = TANKS + 2 * MAX_TANKS, DATA_COUNT = MODE + 1;

    private final String name;
    private final int itemIn, fluidIn, fluidOut, itemOut;
    private final int[] tankX;
    private final int arrowX;

    ChemicalMachineLayout(String name, int itemIn, int fluidIn, int fluidOut, int itemOut, int[] tankX, int arrowX) {
        this.name = name;
        this.itemIn = itemIn;
        this.fluidIn = fluidIn;
        this.fluidOut = fluidOut;
        this.itemOut = itemOut;
        this.tankX = tankX;
        this.arrowX = arrowX;
    }

    public String id() {
        return name;
    }

    public int fluidInputs() {
        return fluidIn;
    }

    public int fluidOutputs() {
        return fluidOut;
    }

    public int tanks() {
        return fluidIn + fluidOut;
    }

    public boolean isInputTank(int tank) {
        return tank < fluidIn;
    }

    public int itemOutputs() {
        return itemOut;
    }

    public int itemInputs() {
        return itemIn;
    }

    /** Whether the screen has a mode button (menu button 1). */
    public boolean hasMode() {
        return this == EXTRUDER;
    }

    public int slots() {
        return itemIn + itemOut;
    }

    public int tankX(int tank) {
        return tankX[tank];
    }

    public int arrowX() {
        return arrowX;
    }

    /** Item input {@code i} sits under the first tanks; the assembler's six make a grid of three by two. */
    public int inputSlotX(int i) {
        if (hasMode()) return 44 + i * 18;
        return itemIn > 2 ? 62 + i % 3 * 18 : 36 + i * 18;
    }

    public int inputSlotY(int i) {
        if (hasMode()) return ARROW_Y;
        return itemIn > 2 ? 26 + i / 3 * 18 : SLOT_Y;
    }

    /** Item output {@code i} sits under the first output tank; the assembler's is right of the arrow. */
    public int outputSlotX(int i) {
        if (hasMode()) return 116;
        return itemIn > 2 ? 144 : tankX[fluidIn] + i * 18;
    }

    public int outputSlotY() {
        if (hasMode()) return ARROW_Y;
        return itemIn > 2 ? 36 : SLOT_Y;
    }
}
