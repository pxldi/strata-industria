package dev.strataindustria.knapping;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.ModSounds;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Which items can be knapped, what opening the grid costs, and how the grid looks and sounds. */
public final class Knapping {
    public static final int ROCK_OPENING_COST = 2;
    public static final int FLINT_OPENING_COST = 1;
    /** Clay forming (spec 4.1) uses the same grid; it takes 5 clay balls. */
    public static final int CLAY_OPENING_COST = 5;

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

    /** A pattern blank: wood carved on the same grid. */
    public static boolean isWood(ItemStack stack) {
        return stack.is(dev.strataindustria.registry.PatternRegistry.PATTERN_BLANK.get());
    }

    public static boolean isFireClay(ItemStack stack) {
        return stack.is(ModItems.FIRE_CLAY_BALL.get());
    }

    /** Anything that opens the grid: loose rocks, flint or clay. */
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

    /** Texture that fills the grid: the rock's own block texture, or the flint nodule texture. */
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

    /** Chips that fly off a strike: grey stone, dark flint, or brown clay crumbs. */
    public static int chipColour(ItemStack stack, float shade) {
        if (isWood(stack)) {
            int r = Math.round(0xa8 * shade), g = Math.round(0x82 * shade), b = Math.round(0x38 * shade);
            return 0xFF000000 | r << 16 | g << 8 | b;
        }
        if (isFireClay(stack)) {
            int r = Math.round(0xc8 * shade), g = Math.round(0xb8 * shade), b = Math.round(0x9a * shade);
            return 0xFF000000 | r << 16 | g << 8 | b;
        }
        if (isClay(stack)) {
            int r = Math.round(0x8e * shade), g = Math.round(0x7a * shade), b = Math.round(0x74 * shade);
            return 0xFF000000 | r << 16 | g << 8 | b;
        }
        boolean flint = isFlint(stack);
        int grey = (int) (shade * (flint ? 150 : 200));
        return 0xFF000000 | grey << 16 | grey << 8 | Math.min(255, grey + (flint ? 18 : 6));
    }

    /** Opens the grid if the player holds enough material. Returns whether it opened. */
    public static boolean tryOpen(ServerPlayer player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!isKnappable(held)) return false;
        if (held.getCount() < openingCost(held)) {
            player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".knapping.need_more",
                    openingCost(held), held.getHoverName()));
            return false;
        }
        ItemStack material = held.copyWithCount(1);
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> new KnappingMenu(id, inventory, material, hand),
                Component.translatable("container." + StrataIndustria.MOD_ID + (isClay(material) ? ".clay_forming" : isWood(material) ? ".carving" : ".knapping"))),
                buf -> {
                    ItemStack.STREAM_CODEC.encode(buf, material);
                    buf.writeBoolean(hand == InteractionHand.MAIN_HAND);
                });
        return true;
    }
}
