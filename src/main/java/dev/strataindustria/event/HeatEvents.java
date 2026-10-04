package dev.strataindustria.event;

import dev.strataindustria.Config;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.heat.Heat;
import dev.strataindustria.heat.HeatBand;
import dev.strataindustria.registry.ModDamageTypes;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Item heat outside heat sources (spec 5): the tooltip band, and quenching in water. */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class HeatEvents {
    static final int QUENCH_INTERVAL = 5;
    static final int BURN_INTERVAL = 20;
    /** Above this a quench hisses and steams. */
    static final float STEAM_FROM = 100.0f;

    private HeatEvents() {}

    /** The band name in its colour; the number only with advanced tooltips. */
    @SubscribeEvent
    static void onTooltip(ItemTooltipEvent event) {
        if (event.getItemStack().is(ModItems.TONGS.get())) {
            event.getToolTip().add(1, Component.translatable("item." + StrataIndustria.MOD_ID + ".tongs.tooltip")
                    .withStyle(ChatFormatting.GRAY));
        }
        Player player = event.getEntity();
        if (player == null) return;
        float temperature = Heat.get(event.getItemStack(), player.level());
        HeatBand band = HeatBand.of(temperature);
        if (band == HeatBand.NONE) return;
        Component line = band.displayName();
        if (event.getFlags().isAdvanced()) {
            line = line.copy().append(Component.literal(" (" + Math.round(temperature) + " °C)").withStyle(ChatFormatting.DARK_GRAY));
        }
        event.getToolTip().add(1, line);
    }

    /**
     * Spec 5.4: hot metal in either hand burns once a second, unless tongs are in the off hand. The tongs
     * wear a little for every second they do the holding.
     */
    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % BURN_INTERVAL != 0) return;
        if (!Config.HEAT_BURN_PLAYER.get() || player.isCreative() || player.isSpectator()) return;
        ServerLevel level = player.level();
        boolean hot = false;
        for (InteractionHand hand : InteractionHand.values()) {
            if (Heat.get(player.getItemInHand(hand), level) >= Heat.BURN_FROM) hot = true;
        }
        if (!hot) return;
        ItemStack offhand = player.getOffhandItem();
        if (offhand.is(ModItems.TONGS.get())) {
            offhand.hurtAndBreak(1, player, InteractionHand.OFF_HAND);
            return;
        }
        if (player.hasEffect(MobEffects.FIRE_RESISTANCE)) return;
        if (player.hurtServer(level, level.damageSources().source(ModDamageTypes.HOT_ITEM), 1.0f)) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.SEAR.get(), SoundSource.PLAYERS,
                    0.7f, 0.9f + level.getRandom().nextFloat() * 0.2f);
            player.sendOverlayMessage(Component.translatable("heat." + StrataIndustria.MOD_ID + ".too_hot"));
        }
    }

    /** A hot item dropped in water cools fast, with a hiss and a puff of steam. */
    @SubscribeEvent
    static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ItemEntity item) || !(item.level() instanceof ServerLevel level)) return;
        if (item.tickCount % QUENCH_INTERVAL != 0 || !item.isInWater()) return;
        ItemStack stack = item.getItem();
        float before = Heat.get(stack, level);
        if (before < HeatBand.WARMING.from()) return;
        ItemStack cooled = stack.copy();
        Heat.heatToward(cooled, Heat.AMBIENT, Heat.QUENCH_RATE, QUENCH_INTERVAL, Float.MAX_VALUE, level.getGameTime());
        item.setItem(cooled);
        if (before >= STEAM_FROM) {
            level.sendParticles(ParticleTypes.CLOUD, item.getX(), item.getY() + 0.3, item.getZ(), 3, 0.1, 0.1, 0.1, 0.02);
            if (item.tickCount % (QUENCH_INTERVAL * 4) == 0) {
                level.playSound(null, item.getX(), item.getY(), item.getZ(), ModSounds.QUENCH.get(), SoundSource.NEUTRAL,
                        0.6f, 0.9f + level.getRandom().nextFloat() * 0.3f);
            }
        }
    }
}
