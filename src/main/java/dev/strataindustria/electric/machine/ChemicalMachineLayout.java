package dev.strataindustria.electric.machine;

/**
 * Slots, tanks and screen positions of the fluid machines (tier 5 spec 10.1, 11.1 and 11.2). Both have two
 * item inputs; the mixer takes three fluids and makes one, the electrolyser takes one and makes three.
 * Tanks are numbered inputs first, then outputs, and each holds 4000 mB.
 */
public enum ChemicalMachineLayout {
    MIXER("mixer", 3, 1, 1, new int[] {36, 54, 72, 118}, 92),
    ELECTROLYSER("electrolyser", 1, 3, 2, new int[] {36, 100, 118, 136}, 66);

    public static final int ITEM_INPUTS = 2;
    public static final int TANK_CAPACITY = 4000;
    public static final int WIDTH = 176, HEIGHT = 176, INVENTORY_Y = 94, STATUS_Y = 74;
    /** A tank's glass: the inside is 16 x 34 and the frame round it 18 x 36. */
    public static final int TANK_Y = 18, TANK_W = 16, TANK_H = 34, SLOT_Y = 54, ARROW_Y = 36;
    public static final int BAR_X = 17, BAR_Y = 18, BAR_W = 8, BAR_H = 52;
    public static final int EJECT_X = 154, EJECT_Y = 56, EJECT_SIZE = 14;
    public static final int MAX_TANKS = 4;

    /** Container data: status, power percent, buffer percent, auto-eject, progress, the fluid of a full tank, then two values per tank. */
    public static final int STATUS = 0, POWER = 1, BUFFER = 2, EJECT = 3, PROGRESS = 4, FULL_FLUID = 5, TANKS = 6,
            DATA_COUNT = TANKS + 2 * MAX_TANKS;

    private final String name;
    private final int fluidIn, fluidOut, itemOut;
    private final int[] tankX;
    private final int arrowX;

    ChemicalMachineLayout(String name, int fluidIn, int fluidOut, int itemOut, int[] tankX, int arrowX) {
        this.name = name;
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

    public int slots() {
        return ITEM_INPUTS + itemOut;
    }

    public int tankX(int tank) {
        return tankX[tank];
    }

    public int arrowX() {
        return arrowX;
    }

    /** Item input {@code i} (0 or 1) sits under the first tanks. */
    public int inputSlotX(int i) {
        return 36 + i * 18;
    }

    /** Item output {@code i} sits under the first output tank. */
    public int outputSlotX(int i) {
        return tankX[fluidIn] + i * 18;
    }
}
