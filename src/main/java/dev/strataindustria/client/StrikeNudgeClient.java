package dev.strataindustria.client;

import dev.strataindustria.StrataIndustria;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** A short downward kick of the view that settles back in a few ticks. */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID, value = Dist.CLIENT)
public final class StrikeNudgeClient {
    private static final float KICK_DEGREES = 1.4f;
    private static float recover;

    private StrikeNudgeClient() {}

    public static void nudge(float strength) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        float kick = KICK_DEGREES * strength;
        player.setXRot(player.getXRot() + kick);
        recover += kick;
    }

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || Math.abs(recover) < 0.01f) {
            recover = 0.0f;
            return;
        }
        float step = recover * 0.4f;
        player.setXRot(player.getXRot() - step);
        recover -= step;
    }
}
