package dev.strataindustria.listening;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.block.OreBlock;
import dev.strataindustria.geology.OreGrade;
import dev.strataindustria.metal.Quality;
import dev.strataindustria.registry.ModDataComponents;
import dev.strataindustria.registry.ModTags;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jspecify.annotations.Nullable;

/**
 * The tap test (uniqueness 2.1): right-click ore with a hammer, or a casting held in the other hand, and
 * listen. A clean ring means rich ore or a sound casting; a dull thud means poor ore or a flawed one.
 */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class TapTest {
    /** Ticks before the same hammer can be tapped again. */
    private static final int COOLDOWN = 8;

    private TapTest() {}

    public static SoundEvent forGrade(OreGrade grade) {
        return switch (grade) {
            case RICH -> ListeningSounds.TAP_RING.get();
            case NORMAL -> ListeningSounds.TAP_KNOCK.get();
            case POOR -> ListeningSounds.TAP_THUD.get();
        };
    }

    /** Crude and rough castings carry flaws and sound it; fine ones ring. */
    public static SoundEvent forQuality(Quality quality) {
        return switch (quality.grade()) {
            case "crude", "rough" -> ListeningSounds.TAP_THUD.get();
            case "standard" -> ListeningSounds.TAP_KNOCK.get();
            default -> ListeningSounds.TAP_RING.get();
        };
    }

    /** What tapping this block or held item sounds like, or null if it tells nothing. */
    static @Nullable SoundEvent heard(BlockState state) {
        return state.getBlock() instanceof OreBlock ? forGrade(state.getValue(OreGrade.PROPERTY)) : null;
    }

    @SubscribeEvent
    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack held = event.getItemStack();
        if (event.getHand() != InteractionHand.MAIN_HAND || player.isSecondaryUseActive() || !held.is(ModTags.Items.HAMMERS)) return;
        SoundEvent sound = heard(event.getLevel().getBlockState(event.getPos()));
        if (sound == null || player.getCooldowns().isOnCooldown(held)) return;
        player.getCooldowns().addCooldown(held, COOLDOWN);
        tap(event.getLevel(), player, sound);
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    @SubscribeEvent
    static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack held = event.getItemStack();
        if (event.getHand() != InteractionHand.MAIN_HAND || player.isSecondaryUseActive() || !held.is(ModTags.Items.HAMMERS)) return;
        Quality quality = player.getOffhandItem().get(ModDataComponents.QUALITY.get());
        if (quality == null || player.getCooldowns().isOnCooldown(held)) return;
        player.getCooldowns().addCooldown(held, COOLDOWN);
        tap(event.getLevel(), player, forQuality(quality));
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static void tap(Level level, Player player, SoundEvent sound) {
        if (level.isClientSide()) return;
        level.playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 0.9f, 0.95f + level.getRandom().nextFloat() * 0.1f);
    }
}
