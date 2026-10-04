package dev.strataindustria.knapping;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.ModSounds;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Which items can be shaped by hand, what a shape costs at least, and how striking them looks and sounds. */
public final class Knapping {
    public static final int ROCK_OPENING_COST = 2;
    public static final int FLINT_OPENING_COST = 1;
    /** Clay forming (spec 4.1) takes 5 clay balls. */
    public static final int CLAY_OPENING_COST = 5;

    public static final String KIND_FLINT = "flint";
    public static final String KIND_ROCK = "rock";
    public static final String KIND_CLAY = "clay";
    public static final String KIND_FIRE_CLAY = "fire_clay";
    public static final String KIND_WOOD = "wood";

    private Knapping() {}

    public static Optional<Rock> rockOf(ItemStack stack) {
        for (Rock rock : Rock.values()) {
            if (stack.is(ModItems.LOOSE_ROCK.get(rock).get())) return Optional.of(rock);
        }
        return Optional.empty();
    }

    public static boolean isFlint(ItemStack stack) {
        return stack.is(Items.FLINT);
    }

    public static boolean isClay(ItemStack stack) {
        return stack.is(Items.CLAY_BALL) || isFireClay(stack);
    }

    /** What kind of material this is, which keeps its own picked shape and blows. */
    public static String kind(ItemStack stack) {
        if (isFlint(stack)) return KIND_FLINT;
        if (isFireClay(stack)) return KIND_FIRE_CLAY;
        if (isClay(stack)) return KIND_CLAY;
        if (isWood(stack)) return KIND_WOOD;
        return KIND_ROCK;
    }

    public static boolean isClayKind(String kind) {
        return kind.equals(KIND_CLAY) || kind.equals(KIND_FIRE_CLAY);
    }

    /** A pattern blank: wood carved with the same strikes. */
    public static boolean isWood(ItemStack stack) {
        return stack.is(dev.strataindustria.registry.PatternRegistry.PATTERN_BLANK.get());
    }

    public static boolean isFireClay(ItemStack stack) {
        return stack.is(ModItems.FIRE_CLAY_BALL.get());
    }

    /** Anything shaped by striking: loose rocks, flint, clay or a pattern blank. */
    public static boolean isKnappable(ItemStack stack) {
        return isFlint(stack) || isClay(stack) || isWood(stack) || rockOf(stack).isPresent();
    }

    public static int openingCost(ItemStack stack) {
        if (isClay(stack)) return CLAY_OPENING_COST;
        if (isWood(stack)) return 1;
        return isFlint(stack) ? FLINT_OPENING_COST : ROCK_OPENING_COST;
    }

    /** How the material breaks when struck, or empty for clay and wood, which do not flake. */
    public static Optional<Grain> grainOf(ItemStack stack) {
        if (isFlint(stack)) return Optional.of(Grain.CLEAN);
        return rockOf(stack).map(Rock::grain);
    }

    public static Optional<KnappedFrom> sourceOf(ItemStack stack) {
        if (isFlint(stack)) return Optional.of(new KnappedFrom(KnappedFrom.FLINT));
        return rockOf(stack).map(KnappedFrom::of);
    }

    /** Texture of the material, for the JEI page: the rock's own block texture, or the flint nodule texture. */
    public static Identifier gridTexture(ItemStack stack) {
        if (isFireClay(stack)) return StrataIndustria.id("textures/block/fire_clay.png");
        if (isClay(stack)) return StrataIndustria.id("textures/gui/knapping/clay.png");
        if (isWood(stack)) return StrataIndustria.id("textures/gui/knapping/wood.png");
        return rockOf(stack)
                .map(rock -> StrataIndustria.id("textures/block/" + rock.id() + ".png"))
                .orElse(StrataIndustria.id("textures/gui/knapping/flint.png"));
    }

    public static SoundEvent strikeSound(ItemStack stack) {
        if (isWood(stack)) return dev.strataindustria.registry.PatternRegistry.PATTERN_CARVE.get();
        if (isClay(stack)) return ModSounds.CLAY_SHAPE.get();
        return isFlint(stack) ? ModSounds.KNAP_FLINT.get() : ModSounds.KNAP_ROCK.get();
    }

    public static SoundEvent finishSound(ItemStack stack) {
        if (isWood(stack)) return dev.strataindustria.registry.PatternRegistry.PATTERN_FINISH.get();
        return isClay(stack) ? ModSounds.CLAY_FINISH.get() : ModSounds.KNAP_FINISH.get();
    }
}
