package dev.strataindustria.grid;

import dev.strataindustria.journal.Journal;
import dev.strataindustria.power.ElectricConductor;
import dev.strataindustria.power.ElectricDevice;
import dev.strataindustria.power.ElectricNetwork;
import dev.strataindustria.power.ElectricNetworks;
import dev.strataindustria.power.ElectricNode;
import dev.strataindustria.power.ElectricStatus;
import dev.strataindustria.power.Kinetic;
import dev.strataindustria.power.KineticSource;
import dev.strataindustria.power.KineticState;
import dev.strataindustria.power.PortKey;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The mechanic's stethoscope (uniqueness 2.1). Right-click a kinetic or electric block with it and the action bar gives
 * the same line sneak-clicking would, while a sound tells you the rest by ear: a steady beat, a rough one under strain,
 * a click for something dead. {@link GridEvents} hands it the click before the block's own action.
 */
public class StethoscopeItem extends Item {
    /** Ticks before it can be used again, so a held click does not machine-gun the sound. */
    private static final int COOLDOWN = 10;

    /** How a block sounds through the stethoscope. */
    public enum Beat {
        STEADY, STRAINED, SILENT;

        SoundEvent sound() {
            return switch (this) {
                case STEADY -> GridSounds.LISTEN_STEADY.get();
                case STRAINED -> GridSounds.LISTEN_STRAINED.get();
                case SILENT -> GridSounds.LISTEN_SILENT.get();
            };
        }
    }

    /** What a stethoscope hears: the diagnosis line and the beat. */
    public record Reading(Component line, Beat beat) {}

    public StethoscopeItem(Properties properties) {
        super(properties);
    }

    /** Applies the stethoscope to a block; PASS when there is nothing to hear there. Nothing changes on the client. */
    public static InteractionResult listen(Level level, BlockPos pos, Player player, ItemStack stack, BlockHitResult hit) {
        Reading reading = read(level, pos, hit);
        if (reading == null) return InteractionResult.PASS;
        if (level.isClientSide() || player.getCooldowns().isOnCooldown(stack)) return InteractionResult.SUCCESS;
        player.sendOverlayMessage(reading.line());
        level.playSound(null, pos, reading.beat().sound(), SoundSource.PLAYERS, 0.7f, 1.0f);
        player.getCooldowns().addCooldown(stack, COOLDOWN);
        if (player instanceof ServerPlayer server) Journal.award(server, Journal.STETHOSCOPE);
        return InteractionResult.SUCCESS;
    }

    /** The diagnosis of the block at {@code pos}, or null if it is neither electric nor kinetic. */
    public static @Nullable Reading read(Level level, BlockPos pos, BlockHitResult hit) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof ElectricNode node) {
            int port = node.portAt(hit.getDirection());
            return new Reading(ElectricNetworks.line(level, pos, port), electricBeat(level, pos, port, node));
        }
        if (entity instanceof Kinetic kinetic) {
            MutableComponent line = kinetic.kinetic().report().copy();
            if (kinetic instanceof KineticSource source && source.idleReason() != null) line = source.idleReason().copy().append(" · ").append(line);
            KineticState.Status status = kinetic.kinetic().status();
            Beat beat = status == KineticState.Status.RUNNING ? Beat.STEADY : status == KineticState.Status.OVERSTRESSED ? Beat.STRAINED : Beat.SILENT;
            return new Reading(line, beat);
        }
        return null;
    }

    private static Beat electricBeat(Level level, BlockPos pos, int port, ElectricNode node) {
        ElectricNetwork network = ElectricNetworks.networkAt(level, pos, port);
        if (network == null || network.tier() == null) return Beat.SILENT;
        if (network.tooLarge() || network.overvoltageCable() != null) return Beat.STRAINED;
        if (node instanceof ElectricDevice && !(node instanceof ElectricConductor)) {
            ElectricStatus status = network.report(new PortKey(pos, port)).status();
            if (status.fault() || status == ElectricStatus.LOW_POWER) return Beat.STRAINED;
            return status == ElectricStatus.RUNNING ? Beat.STEADY : Beat.SILENT;
        }
        if (network.strained()) return Beat.STRAINED;
        return network.load() >= GridVoices.HUM_MIN ? Beat.STEADY : Beat.SILENT;
    }
}
