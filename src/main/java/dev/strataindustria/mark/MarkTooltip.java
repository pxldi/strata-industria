package dev.strataindustria.mark;

import net.minecraft.world.inventory.tooltip.TooltipComponent;

/** A part's mark drawn into its tooltip. */
public record MarkTooltip(MakersMark mark) implements TooltipComponent {}
