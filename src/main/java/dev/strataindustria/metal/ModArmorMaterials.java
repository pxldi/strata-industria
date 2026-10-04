package dev.strataindustria.metal;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.material.Metal;
import dev.strataindustria.registry.ModTags;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

/** Spec 8.4. Copper is the vanilla copper armour; the bronzes share 2 / 6 / 5 / 2 armour points. */
public final class ModArmorMaterials {
    public static final ArmorMaterial BRONZE = bronze(Metal.BRONZE, 16, 12, 0.5f);
    public static final ArmorMaterial ARSENICAL_BRONZE = bronze(Metal.ARSENICAL_BRONZE, 14, 9, 0.0f);
    public static final ArmorMaterial BISMUTH_BRONZE = bronze(Metal.BISMUTH_BRONZE, 13, 18, 0.0f);

    private ModArmorMaterials() {}

    public static ResourceKey<EquipmentAsset> asset(Metal metal) {
        return ResourceKey.create(EquipmentAssets.ROOT_ID, StrataIndustria.id(metal.id()));
    }

    private static ArmorMaterial bronze(Metal metal, int durability, int enchantability, float toughness) {
        Map<ArmorType, Integer> defense = new EnumMap<>(ArmorType.class);
        defense.put(ArmorType.HELMET, 2);
        defense.put(ArmorType.CHESTPLATE, 6);
        defense.put(ArmorType.LEGGINGS, 5);
        defense.put(ArmorType.BOOTS, 2);
        defense.put(ArmorType.BODY, 6);
        return new ArmorMaterial(durability, defense, enchantability, SoundEvents.ARMOR_EQUIP_IRON, toughness, 0.0f,
                ModTags.Items.ingots(metal), asset(metal));
    }

    public static ArmorMaterial of(Metal metal) {
        return switch (metal) {
            case COPPER -> ArmorMaterials.COPPER;
            case BRONZE -> BRONZE;
            case ARSENICAL_BRONZE -> ARSENICAL_BRONZE;
            case BISMUTH_BRONZE -> BISMUTH_BRONZE;
            case WROUGHT_IRON -> ArmorMaterials.IRON;
            case GOLD -> ArmorMaterials.GOLD;
            default -> throw new IllegalArgumentException(metal + " makes no armour");
        };
    }
}
