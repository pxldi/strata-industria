package dev.strataindustria.transport.foot;

import dev.strataindustria.StrataIndustria;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * What the player carries or pulls, kept as attribute modifiers so the client sees it too: a full pack frame stops
 * sprinting, and pulling a handcart slows the walk, stops sprinting and takes away the jump (spec 4.2 and 4.3).
 */
public final class Burden {
    /** Marker on a player whose pack frame is more than half full. Changes no speed by itself. */
    public static final Identifier BURDENED = StrataIndustria.id("burdened");
    /** Marker and slowdown on a player pulling a handcart. */
    public static final Identifier HAULING = StrataIndustria.id("hauling");
    private static final Identifier NO_JUMP = StrataIndustria.id("hauling_no_jump");

    /** Walking speed while pulling a handcart. */
    public static final double HAUL_SPEED = -0.15;

    private Burden() {}

    public static boolean isBurdened(Player player) {
        return has(player, Attributes.MOVEMENT_SPEED, BURDENED);
    }

    public static boolean isHauling(Player player) {
        return has(player, Attributes.MOVEMENT_SPEED, HAULING);
    }

    /** Sprinting is off for anyone burdened or hauling. */
    public static boolean noSprint(Player player) {
        return isBurdened(player) || isHauling(player);
    }

    public static void setBurdened(Player player, boolean on) {
        set(player, Attributes.MOVEMENT_SPEED, BURDENED, 0.0, on);
    }

    public static void setHauling(Player player, boolean on) {
        set(player, Attributes.MOVEMENT_SPEED, HAULING, HAUL_SPEED, on);
        set(player, Attributes.JUMP_STRENGTH, NO_JUMP, -1.0, on);
    }

    private static boolean has(Player player, Holder<Attribute> attribute, Identifier id) {
        AttributeInstance instance = player.getAttribute(attribute);
        return instance != null && instance.hasModifier(id);
    }

    private static void set(Player player, Holder<Attribute> attribute, Identifier id, double amount, boolean on) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        if (!on) {
            instance.removeModifier(id);
        } else if (!instance.hasModifier(id)) {
            instance.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }
}
