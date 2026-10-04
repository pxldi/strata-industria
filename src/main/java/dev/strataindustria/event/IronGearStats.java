package dev.strataindustria.event;

import dev.strataindustria.Config;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.metal.ModToolMaterials;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

/**
 * Vanilla iron tools and armour are the wrought iron ones (tier 3 spec 10.2 and 10.4), so they take the
 * wrought iron stats: 520 durability, 6.5 mining speed and half a point more damage for tools, and
 * toughness 1 with a durability multiplier of 20 for armour. Config {@code vanilla.ironStatOverride}.
 */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class IronGearStats {
    private static final List<Item> TOOLS = List.of(Items.IRON_PICKAXE, Items.IRON_AXE, Items.IRON_SHOVEL, Items.IRON_HOE, Items.IRON_SWORD);
    private static final List<Item> ARMOUR = List.of(Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS);
    /** Vanilla iron: 250 durability, 6.0 speed, 2.0 attack bonus, armour multiplier 15, toughness 0. */
    private static final float VANILLA_SPEED = 6.0f, VANILLA_ATTACK = 2.0f, VANILLA_ARMOUR_MULTIPLIER = 15.0f;
    private static final float ARMOUR_MULTIPLIER = 20.0f, TOUGHNESS = 1.0f;

    private IronGearStats() {}

    @SubscribeEvent
    static void modify(ModifyDefaultComponentsEvent event) {
        if (!Config.IRON_STAT_OVERRIDE.getAsBoolean()) return;
        event.modify(Items.IRON_PICKAXE, (Object) null);
        float attackBonus = ModToolMaterials.WROUGHT_IRON.attackDamageBonus() - VANILLA_ATTACK;
        for (Item item : TOOLS) {
            Tool tool = item.components().get(DataComponents.TOOL);
            ItemAttributeModifiers attributes = item.components().get(DataComponents.ATTRIBUTE_MODIFIERS);
            event.modify(item, builder -> {
                builder.set(DataComponents.MAX_DAMAGE, ModToolMaterials.WROUGHT_IRON.durability());
                if (tool != null && item != Items.IRON_SWORD) builder.set(DataComponents.TOOL, faster(tool));
                if (attributes != null) builder.set(DataComponents.ATTRIBUTE_MODIFIERS, withAttack(attributes, attackBonus));
            });
        }
        for (Item item : ARMOUR) {
            Integer durability = item.components().get(DataComponents.MAX_DAMAGE);
            ItemAttributeModifiers attributes = item.components().get(DataComponents.ATTRIBUTE_MODIFIERS);
            event.modify(item, builder -> {
                if (durability != null) builder.set(DataComponents.MAX_DAMAGE, Math.round(durability * ARMOUR_MULTIPLIER / VANILLA_ARMOUR_MULTIPLIER));
                if (attributes != null) builder.set(DataComponents.ATTRIBUTE_MODIFIERS, withToughness(attributes));
            });
        }
    }

    /** The same rules, mining at the wrought iron speed where vanilla iron mined at 6.0. */
    private static Tool faster(Tool tool) {
        float speed = ModToolMaterials.WROUGHT_IRON.speed();
        List<Tool.Rule> rules = tool.rules().stream()
                .map(rule -> new Tool.Rule(rule.blocks(), rule.speed().map(s -> s == VANILLA_SPEED ? speed : s), rule.correctForDrops()))
                .toList();
        return new Tool(rules, tool.defaultMiningSpeed(), tool.damagePerBlock(), tool.canDestroyBlocksInCreative());
    }

    private static ItemAttributeModifiers withAttack(ItemAttributeModifiers attributes, float bonus) {
        ItemAttributeModifiers.Builder out = ItemAttributeModifiers.builder();
        for (ItemAttributeModifiers.Entry entry : attributes.modifiers()) {
            AttributeModifier modifier = entry.modifier();
            if (entry.attribute().is(Attributes.ATTACK_DAMAGE) && modifier.id().equals(Item.BASE_ATTACK_DAMAGE_ID)) {
                modifier = new AttributeModifier(modifier.id(), modifier.amount() + bonus, modifier.operation());
            }
            out.add(entry.attribute(), modifier, entry.slot());
        }
        return out.build();
    }

    /** Sets the toughness, using the armour modifier's id when vanilla gives the piece none. */
    private static ItemAttributeModifiers withToughness(ItemAttributeModifiers attributes) {
        ItemAttributeModifiers.Builder out = ItemAttributeModifiers.builder();
        ItemAttributeModifiers.Entry armour = null;
        boolean hadToughness = false;
        for (ItemAttributeModifiers.Entry entry : attributes.modifiers()) {
            AttributeModifier modifier = entry.modifier();
            if (entry.attribute().is(Attributes.ARMOR)) armour = entry;
            if (entry.attribute().is(Attributes.ARMOR_TOUGHNESS)) {
                modifier = new AttributeModifier(modifier.id(), TOUGHNESS, modifier.operation());
                hadToughness = true;
            }
            out.add(entry.attribute(), modifier, entry.slot());
        }
        if (!hadToughness && armour != null) {
            out.add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(armour.modifier().id(), TOUGHNESS, AttributeModifier.Operation.ADD_VALUE),
                    armour.slot());
        }
        return out.build();
    }
}
