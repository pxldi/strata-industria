package dev.strataindustria.prospecting;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.client.OreScannerClient;
import dev.strataindustria.electric.BatteryBoxBlockEntity;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.power.ElectricNetworks;
import dev.strataindustria.registry.Tier5DataComponents;
import dev.strataindustria.registry.Tier5Sounds;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The ore scanner (spec 13.2, prospecting IV). It holds 10 000 J, filled by right-clicking a battery box with it.
 * Hold right-click for two seconds to scan the 3 x 3 chunks around you for 1000 J; the map opens and the result
 * stays on the item. Sneak and right-click reads the last scan again.
 */
public class OreScannerItem extends Item {
    public static final int CAPACITY = 10_000;
    public static final int COST = 1000;
    public static final int SCAN_TICKS = 40;

    public OreScannerItem(Properties properties) {
        super(properties);
    }

    public static int energy(ItemStack stack) {
        return stack.getOrDefault(Tier5DataComponents.ENERGY.get(), 0);
    }

    /** Draws charge from a battery box into the scanner: PASS for anything else. */
    public static InteractionResult charge(Level level, BlockPos pos, Player player, ItemStack stack) {
        if (!(level.getBlockEntity(pos) instanceof BatteryBoxBlockEntity box)) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        int room = CAPACITY - energy(stack);
        int moved = (int) Math.min(room, Math.floor(box.stored()));
        if (moved <= 0) {
            player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + (room <= 0 ? ".ore_scanner.full" : ".ore_scanner.box_empty")));
            return InteractionResult.SUCCESS;
        }
        box.extract(moved);
        stack.set(Tier5DataComponents.ENERGY.get(), energy(stack) + moved);
        level.playSound(null, pos, Tier5Sounds.BATTERY_BOX_CHARGE.get(), SoundSource.PLAYERS, 0.7f, 1.4f);
        player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".ore_scanner.charged", ElectricNetworks.joules(energy(stack)),
                ElectricNetworks.joules(CAPACITY)));
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        OreScan last = stack.get(Tier5DataComponents.ORE_SCAN.get());
        if (player.isShiftKeyDown() && last != null) {
            if (level.isClientSide()) OreScannerClient.openHeld();
            return InteractionResult.SUCCESS;
        }
        if (energy(stack) < COST) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".ore_scanner.flat", ElectricNetworks.joules(COST)));
            }
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        if (!level.isClientSide()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), Tier5Sounds.ORE_SCANNER_SCAN.get(), SoundSource.PLAYERS, 0.7f, 1.0f);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return SCAN_TICKS;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!(level instanceof ServerLevel serverLevel) || !(user instanceof ServerPlayer player) || !scan(serverLevel, player, stack)) return stack;
        level.playSound(null, player.getX(), player.getY(), player.getZ(), Tier5Sounds.ORE_SCANNER_DONE.get(), SoundSource.PLAYERS, 0.8f, 1.0f);
        PacketDistributor.sendToPlayer(player, new ScannerPayloads.Open());
        Journal.award(player, Journal.PROSPECT);
        Journal.award(player, Journal.ORE_SCAN);
        return stack;
    }

    /** Spends the charge and stores the scan of the 3 x 3 chunks around the player; false when there is too little charge. */
    public static boolean scan(ServerLevel level, ServerPlayer player, ItemStack stack) {
        if (energy(stack) < COST) return false;
        stack.set(Tier5DataComponents.ENERGY.get(), energy(stack) - COST);
        stack.set(Tier5DataComponents.ORE_SCAN.get(), OreScan.scan(level, player.blockPosition()));
        return true;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0f * energy(stack) / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x4fae4a;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(StrataIndustria.MOD_ID + ".ore_scanner.charge", ElectricNetworks.joules(energy(stack)),
                ElectricNetworks.joules(CAPACITY)).withStyle(ChatFormatting.GRAY));
    }
}
