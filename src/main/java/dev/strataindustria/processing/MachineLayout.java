package dev.strataindustria.processing;

/**
 * Slot counts and screen positions of the tier 4 ore processing machines (spec 11.2 to 11.4). The block
 * entity, the menu and the screen all read the same layout, so they cannot disagree on slot numbers.
 */
public enum MachineLayout {
    /** Three inputs side by side in a column, each with its own arrow, into a 3x2 output grid. */
    CRUSHER("crusher", 30, new int[] {18, 36, 54}, new int[] {98, 116, 134}, new int[] {27, 45});

    public static final int WIDTH = 176, HEIGHT = 176, INVENTORY_Y = 94, STATUS_Y = 74, ARROW_X = 56;

    private final String name;
    private final int inputX;
    private final int[] inputYs, outputXs, outputYs;

    MachineLayout(String name, int inputX, int[] inputYs, int[] outputXs, int[] outputYs) {
        this.name = name;
        this.inputX = inputX;
        this.inputYs = inputYs;
        this.outputXs = outputXs;
        this.outputYs = outputYs;
    }

    public String id() {
        return name;
    }

    public int inputs() {
        return inputYs.length;
    }

    public int outputs() {
        return outputXs.length * outputYs.length;
    }

    public int slots() {
        return inputs() + outputs();
    }

    public int inputX() {
        return inputX;
    }

    public int inputY(int input) {
        return inputYs[input];
    }

    public int outputX(int output) {
        return outputXs[output % outputXs.length];
    }

    public int outputY(int output) {
        return outputYs[output / outputXs.length];
    }

    /** Container data: one progress value per input, then the status. */
    public int statusIndex() {
        return inputs();
    }

    public int dataCount() {
        return inputs() + 1;
    }
}
