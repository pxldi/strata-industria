package dev.strataindustria.event;

import dev.strataindustria.Config;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.knapping.KnappedFrom;
import dev.strataindustria.knapping.Knapping;
import dev.strataindustria.registry.ModDataComponents;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/** Tier 0 rules that live outside items and blocks: no punching logs, fibre and straw, extra sticks and flint. */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class StoneAgeEvents {
    static final float STRAW_CHANCE = 0.5f;
    static final float LEAF_STICK_CHANCE = 0.2f;
    /** Vanilla gravel gives flint 10% of the time; this extra roll on the rest brings it to 15%. */
    static final float EXTRA_FLINT_CHANCE = 0.05f / 0.9f;

    private StoneAgeEvents() {}

    /** Flint has no use of its own, so a right-click with it opens the knapping grid. */
    @SubscribeEvent
    static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        ItemStack held = event.getItemStack();
        if (!Knapping.isFlint(held) || event.getEntity().isSecondaryUseActive()) return;
        if (event.getEntity() instanceof ServerPlayer player) Knapping.tryOpen(player, event.getHand());
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    @SubscribeEvent
    static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!Config.LOGS_NEED_AXE.getAsBoolean()) return;
        Player player = event.getEntity();
        if (event.getState().is(BlockTags.LOGS) && !player.getMainHandItem().is(ModTags.Items.AXES)) {
            event.setNewSpeed(0.0f);
        }
    }

    @SubscribeEvent
    static void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof Player player) || player.isCreative()) return;
        ServerLevel level = event.getLevel();
        BlockState state = event.getState();
        ItemStack tool = event.getTool();
        RandomSource random = level.getRandom();

        if (state.is(ModTags.Blocks.FIBRE_PLANTS) && tool.is(ModTags.Items.KNIVES)) {
            drop(event, new ItemStack(ModItems.PLANT_FIBRE.get()));
            if (random.nextFloat() < STRAW_CHANCE) drop(event, new ItemStack(ModItems.STRAW.get()));
            player.getMainHandItem().hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        } else if (state.is(BlockTags.LEAVES) && Config.LEAVES_DROP_STICKS.getAsBoolean()
                && !tool.is(Items.SHEARS) && !hasSilkTouch(level, tool)) {
            if (random.nextFloat() < LEAF_STICK_CHANCE) drop(event, new ItemStack(Items.STICK));
        } else if (state.is(Blocks.GRAVEL) && Config.GRAVEL_FLINT_CHANCE.getAsBoolean() && !hasSilkTouch(level, tool)) {
            boolean gaveFlint = event.getDrops().stream().anyMatch(e -> e.getItem().is(Items.FLINT));
            if (!gaveFlint && random.nextFloat() < EXTRA_FLINT_CHANCE) {
                event.getDrops().removeIf(e -> e.getItem().is(Items.GRAVEL));
                drop(event, new ItemStack(Items.FLINT));
            }
        }
    }

    private static void drop(BlockDropsEvent event, ItemStack stack) {
        BlockPos pos = event.getPos();
        event.getDrops().add(new ItemEntity(event.getLevel(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack));
    }

    private static boolean hasSilkTouch(ServerLevel level, ItemStack tool) {
        if (tool.isEmpty()) return false;
        Holder<Enchantment> silk = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
        return EnchantmentHelper.getItemEnchantmentLevel(silk, tool) > 0;
    }

    @SubscribeEvent
    static void onTooltip(ItemTooltipEvent event) {
        KnappedFrom source = event.getItemStack().get(ModDataComponents.KNAPPED_FROM.get());
        if (source != null) event.getToolTip().add(1, source.tooltip());
    }
}
