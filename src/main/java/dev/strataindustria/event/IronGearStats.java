package dev.strataindustria.event;

import dev.strataindustria.Config;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.metal.ModToolMaterials;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.block.Block;
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
    private static final Identifier ATTACK_DAMAGE_ID = Identifier.withDefaultNamespace("base_attack_damage");
    private static final Identifier ATTACK_SPEED_ID = Identifier.withDefaultNamespace("base_attack_speed");
    private static final int ARMOUR_MULTIPLIER = 20;
    private static final float TOUGHNESS = 1.0f;

    /** A vanilla iron tool, the blocks it mines, and its base damage and speed before the material bonus. */
    private record IronTool(Item item, TagKey<Block> mineable, float damage, float speed) {}

    /** A vanilla iron armour piece and its armour points. */
    private record IronArmour(Item item, ArmorType type, int defense) {}

    private IronGearStats() {}

    @SubscribeEvent
    static void modify(ModifyDefaultComponentsEvent event) {
        if (!Config.IRON_STAT_OVERRIDE.getAsBoolean()) return;
        var material = ModToolMaterials.WROUGHT_IRON;
        for (IronTool tool : List.of(
                new IronTool(Items.IRON_PICKAXE, BlockTags.MINEABLE_WITH_PICKAXE, 1.0f, -2.8f),
                new IronTool(Items.IRON_AXE, BlockTags.MINEABLE_WITH_AXE, 6.0f, -3.1f),
                new IronTool(Items.IRON_SHOVEL, BlockTags.MINEABLE_WITH_SHOVEL, 1.5f, -3.0f),
                new IronTool(Items.IRON_HOE, BlockTags.MINEABLE_WITH_HOE, -2.0f, -1.0f),
                new IronTool(Items.IRON_SWORD, null, 3.0f, -2.4f))) {
            event.modify(tool.item(), (builder, registries, item) -> {
                builder.set(DataComponents.MAX_DAMAGE, material.durability());
                builder.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.builder()
                        .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(ATTACK_DAMAGE_ID, tool.damage() + material.attackDamageBonus(),
                                AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                        .add(Attributes.ATTACK_SPEED, new AttributeModifier(ATTACK_SPEED_ID, tool.speed(),
                                AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                        .build());
                if (tool.mineable() != null) builder.set(DataComponents.TOOL, tool(registries, tool.mineable(), material.speed()));
            });
        }
        for (IronArmour armour : List.of(
                new IronArmour(Items.IRON_HELMET, ArmorType.HELMET, 2),
                new IronArmour(Items.IRON_CHESTPLATE, ArmorType.CHESTPLATE, 6),
                new IronArmour(Items.IRON_LEGGINGS, ArmorType.LEGGINGS, 5),
                new IronArmour(Items.IRON_BOOTS, ArmorType.BOOTS, 2))) {
            event.modify(armour.item(), (builder, registries, item) -> {
                builder.set(DataComponents.MAX_DAMAGE, armour.type().getDurability(ARMOUR_MULTIPLIER));
                Identifier id = Identifier.withDefaultNamespace("armor." + armour.type().getName());
                EquipmentSlotGroup slot = EquipmentSlotGroup.bySlot(armour.type().getSlot());
                builder.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.builder()
                        .add(Attributes.ARMOR, new AttributeModifier(id, armour.defense(), AttributeModifier.Operation.ADD_VALUE), slot)
                        .add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(id, TOUGHNESS, AttributeModifier.Operation.ADD_VALUE), slot)
                        .build());
            });
        }
    }

    /** Vanilla tool rules for iron, mining at the wrought iron speed. */
    private static Tool tool(HolderLookup.Provider registries, TagKey<Block> mineable, float speed) {
        var blocks = registries.lookupOrThrow(Registries.BLOCK);
        return new Tool(List.of(
                Tool.Rule.deniesDrops(blocks.getOrThrow(BlockTags.INCORRECT_FOR_IRON_TOOL)),
                Tool.Rule.minesAndDrops(blocks.getOrThrow(mineable), speed)), 1.0f, 1, true);
    }
}
