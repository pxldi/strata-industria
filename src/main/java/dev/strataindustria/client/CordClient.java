package dev.strataindustria.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.cord.BarkStripItem;
import dev.strataindustria.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * First-person view of twisting bark (redesign R3): both hands come up to the middle of the screen, the strips
 * wind round each other and the whole pose tightens, then jerks as the cord snaps tight.
 */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID, value = Dist.CLIENT)
public final class CordClient {
    private CordClient() {}

    @SubscribeEvent
    static void register(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public boolean applyForgeHandTransform(PoseStack pose, PlayerRenderState state, HumanoidArm arm, ItemStack stack,
                    float partialTick, float equipProcess, float swingProcess) {
                LocalPlayer player = Minecraft.getInstance().player;
                if (player == null || !player.isUsingItem() || player.getUseItem() != stack) return false;
                float used = BarkStripItem.TWIST_TICKS - player.getUseItemRemainingTicks() + partialTick;
                float t = Math.min(1.0f, used / BarkStripItem.TWIST_TICKS);
                int side = arm == HumanoidArm.RIGHT ? 1 : -1;
                // From the usual hand position towards the middle, a little up and close.
                float lift = Math.min(1.0f, used / 5.0f);
                pose.translate(side * (0.56f - 0.38f * lift), -0.52f - equipProcess * 0.6f + 0.2f * lift, -0.72f + 0.12f * lift);
                // Twisting: the strips turn about their length, faster as they tighten, with a small side wobble.
                float wind = (float) (t * t * 900.0 + t * 180.0);
                pose.mulPose(new org.joml.Matrix4f().rotationY((float) Math.toRadians(side * wind)));
                pose.mulPose(new org.joml.Matrix4f().rotationZ((float) Math.toRadians(Math.sin(used * 0.9f) * 6.0f * t)));
                // The snap: a quick jerk away from you at the very end.
                if (t > 0.92f) pose.translate(0.0f, 0.0f, -0.09f * (t - 0.92f) / 0.08f);
                return true;
            }
        }, ModItems.BARK.get());
    }
}
