package dev.strataindustria.event;

import dev.strataindustria.Config;
import dev.strataindustria.StrataIndustria;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/** Tier 3 changes to vanilla (spec 2): wrought iron gear outlasts bronze, and golems give nuggets. */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class IronAgeEvents {
    /** Spec 10.2: wrought iron tools. */
    public static final int WROUGHT_IRON_TOOL_DURABILITY = 520;
    /** Spec 10.4: durability multiplier 20 on the vanilla per-piece base (11 / 16 / 15 / 13). */
    public static final int WROUGHT_IRON_ARMOUR_MULTIPLIER = 20;

    private IronAgeEvents() {}

    @SubscribeEvent
    static void onModifyComponents(ModifyDefaultComponentsEvent event) {
        for (Item tool : new Item[] {Items.IRON_PICKAXE, Items.IRON_AXE, Items.IRON_SHOVEL, Items.IRON_HOE, Items.IRON_SWORD}) {
            event.modify(tool, (patch, context) -> patch.set(DataComponents.MAX_DAMAGE, WROUGHT_IRON_TOOL_DURABILITY));
        }
        event.modify(Items.IRON_HELMET, (patch, context) -> patch.set(DataComponents.MAX_DAMAGE, 11 * WROUGHT_IRON_ARMOUR_MULTIPLIER));
        event.modify(Items.IRON_CHESTPLATE, (patch, context) -> patch.set(DataComponents.MAX_DAMAGE, 16 * WROUGHT_IRON_ARMOUR_MULTIPLIER));
        event.modify(Items.IRON_LEGGINGS, (patch, context) -> patch.set(DataComponents.MAX_DAMAGE, 15 * WROUGHT_IRON_ARMOUR_MULTIPLIER));
        event.modify(Items.IRON_BOOTS, (patch, context) -> patch.set(DataComponents.MAX_DAMAGE, 13 * WROUGHT_IRON_ARMOUR_MULTIPLIER));
    }

    /** Spec 2: iron golems drop 2 to 4 nuggets, so iron farms do not skip the bloomery. */
    @SubscribeEvent
    static void onLivingDrops(LivingDropsEvent event) {
        if (!BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()).getPath().equals("iron_golem") || !Config.GOLEM_DROPS_NUGGETS.getAsBoolean()) return;
        boolean hadIron = event.getDrops().removeIf(drop -> drop.getItem().is(Items.IRON_INGOT));
        if (!hadIron) return;
        var golem = event.getEntity();
        int nuggets = 2 + golem.getRandom().nextInt(3);
        event.getDrops().add(new ItemEntity(golem.level(), golem.getX(), golem.getY(), golem.getZ(), new ItemStack(Items.IRON_NUGGET, nuggets)));
    }
}
