package dev.strataindustria.fire;

import dev.strataindustria.registry.ModItems;
import java.util.Optional;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Fire pit fuels (tier 0-2 spec 3.5): how long one item burns and how hot it can get the pit. */
public record FirePitFuel(int burnTicks, float maxTemperature) {
    public static final FirePitFuel STRAW = new FirePitFuel(100, 300);
    public static final FirePitFuel STICK = new FirePitFuel(300, 400);
    public static final FirePitFuel LOG = new FirePitFuel(1200, 550);
    public static final FirePitFuel CHARCOAL = new FirePitFuel(1800, 650);

    public static Optional<FirePitFuel> of(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        if (stack.is(ModItems.STRAW.get())) return Optional.of(STRAW);
        if (stack.is(Items.STICK)) return Optional.of(STICK);
        if (stack.is(ItemTags.LOGS_THAT_BURN)) return Optional.of(LOG);
        if (stack.is(Items.CHARCOAL)) return Optional.of(CHARCOAL);
        return Optional.empty();
    }

    public static boolean isFuel(ItemStack stack) {
        return of(stack).isPresent();
    }
}
