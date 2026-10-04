package dev.strataindustria.event;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.heat.Heat;
import dev.strataindustria.heat.HeatBand;
import dev.strataindustria.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Item heat outside heat sources (spec 5): the tooltip band, and quenching in water. */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class HeatEvents {
    static final int QUENCH_INTERVAL = 5;
    /** Above this a quench hisses and steams. */
    static final float STEAM_FROM = 100.0f;

    private HeatEvents() {}

    /** The band name in its colour; the number only with advanced tooltips. */
    @SubscribeEvent
    static void onTooltip(ItemTooltipEvent event) {
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
