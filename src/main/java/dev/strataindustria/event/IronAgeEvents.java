package dev.strataindustria.event;

import dev.strataindustria.Config;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/** Tier 3 changes to vanilla (spec 2): iron golems give nuggets, not ingots, and animals give raw hides. */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class IronAgeEvents {
    private IronAgeEvents() {}

    /** Spec 2: leather drops become raw hides, the same count; chests and fishing still give leather. */
    @SubscribeEvent
    static void hidesInsteadOfLeather(LivingDropsEvent event) {
        if (!Config.HIDES_INSTEAD_OF_LEATHER.getAsBoolean()) return;
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            if (stack.is(Items.LEATHER)) drop.setItem(new ItemStack(ModItems.RAW_HIDE.get(), stack.getCount()));
        }
    }

    /** Spec 12.1: scraping a limed hide with a knife makes itself heard. */
    @SubscribeEvent
    static void onCrafted(net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent event) {
        if (!event.getCrafting().is(ModItems.SCRAPED_HIDE.get())) return;
        var player = event.getEntity();
        if (player.level().isClientSide()) return;
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), dev.strataindustria.registry.ModSounds.HIDE_SCRAPE.get(),
                net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 0.9f + player.getRandom().nextFloat() * 0.2f);
    }

    /** Spec 2: stripping a log with an axe peels off one piece of bark, for tannin. */
    @SubscribeEvent
    static void barkFromStripping(net.neoforged.neoforge.event.level.BlockEvent.BlockToolModificationEvent event) {
        if (event.isSimulated() || event.getItemAbility() != net.neoforged.neoforge.common.ItemAbilities.AXE_STRIP) return;
        if (!Config.STRIPPING_DROPS_BARK.getAsBoolean() || event.getFinalState() == null) return;
        if (!event.getState().is(net.minecraft.tags.BlockTags.LOGS)) return;
        if (!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)) return;
        var context = event.getContext();
        net.minecraft.core.Direction face = context != null ? context.getClickedFace() : net.minecraft.core.Direction.UP;
        net.minecraft.world.level.block.Block.popResourceFromFace(level, event.getPos(), face, new ItemStack(ModItems.BARK.get()));
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
