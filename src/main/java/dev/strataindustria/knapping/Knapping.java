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

    public static boolean isKnappable(ItemStack stack) {
        return isFlint(stack) || rockOf(stack).isPresent();
    }

    public static int openingCost(ItemStack stack) {
        return isFlint(stack) ? FLINT_OPENING_COST : ROCK_OPENING_COST;
    }

    public static Optional<KnappedFrom> sourceOf(ItemStack stack) {
        if (isFlint(stack)) return Optional.of(new KnappedFrom(KnappedFrom.FLINT));
        return rockOf(stack).map(KnappedFrom::of);
    }

    /** Texture that fills the grid: the rock's own block texture, or the flint nodule texture. */
    public static Identifier gridTexture(ItemStack stack) {
        return rockOf(stack)
                .map(rock -> StrataIndustria.id("textures/block/" + rock.id() + ".png"))
                .orElse(StrataIndustria.id("textures/gui/knapping/flint.png"));
    }

    public static SoundEvent strikeSound(ItemStack stack) {
        return isFlint(stack) ? ModSounds.KNAP_FLINT.get() : ModSounds.KNAP_ROCK.get();
    }

    /** Opens the grid if the player holds enough material. Returns whether it opened. */
    public static boolean tryOpen(ServerPlayer player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!isKnappable(held) || held.getCount() < openingCost(held)) return false;
        ItemStack material = held.copyWithCount(1);
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> new KnappingMenu(id, inventory, material, hand),
                Component.translatable("container." + StrataIndustria.MOD_ID + ".knapping")),
                buf -> {
                    ItemStack.STREAM_CODEC.encode(buf, material);
                    buf.writeBoolean(hand == InteractionHand.MAIN_HAND);
                });
        return true;
    }
}
