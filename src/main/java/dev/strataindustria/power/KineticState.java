package dev.strataindustria.power;

import dev.strataindustria.StrataIndustria;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * What a kinetic block knows about its network: its own RPM after gear ratios, the network's load
 * and capacity, and why it stopped. Rebuilt from the world on load, so only the client copy is synced.
 */
public final class KineticState {
    public enum Status {
        /** No turning source on the network. */
        IDLE,
        RUNNING,
        OVERSTRESSED,
        /** Part of the network is in an unloaded chunk. */
        INCOMPLETE,
        /** The network is larger than {@code kinetics.maxNetworkSize}. */
        TOO_LARGE;

        public String key() {
            return StrataIndustria.MOD_ID + ".kinetic." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private float rpm;
    private Status status = Status.IDLE;
    private int load;
    private int capacity;
    private @Nullable BlockPos limiter;

    public float rpm() {
        return rpm;
    }

    public Status status() {
        return status;
    }

    public int load() {
        return load;
    }

    public int capacity() {
        return capacity;
    }

    public boolean turning() {
        return rpm > 0;
    }

    /** Sets the new state and reports whether anything a client draws or reads changed. */
    boolean set(float rpm, Status status, int load, int capacity, @Nullable BlockPos limiter) {
        boolean changed = this.rpm != rpm || this.status != status || this.load != load || this.capacity != capacity
                || !java.util.Objects.equals(this.limiter, limiter);
        this.rpm = rpm;
        this.status = status;
        this.load = load;
        this.capacity = capacity;
        this.limiter = limiter;
        return changed;
    }

    /** "16 RPM, 192 / 256 SU", or why the network stands still (spec 7.1 diagnostics). */
    public Component line() {
        return switch (status) {
            case RUNNING -> Component.translatable(StrataIndustria.MOD_ID + ".kinetic.running", Math.round(rpm), load, capacity);
            case OVERSTRESSED -> Component.translatable(Status.OVERSTRESSED.key(), load, capacity).withStyle(ChatFormatting.RED);
            default -> Component.translatable(status.key());
        };
    }

    /** The diagnostics line plus, when several sources disagree, which one sets the speed. */
    public Component report() {
        MutableComponent line = line().copy();
        if (status == Status.RUNNING && limiter != null) {
            line.append(Component.literal(" · ")).append(Component.translatable(StrataIndustria.MOD_ID + ".kinetic.limited_by",
                    limiter.getX(), limiter.getY(), limiter.getZ()));
        }
        return line;
    }

    public void save(ValueOutput out) {
        out.putFloat("rpm", rpm);
        out.putInt("status", status.ordinal());
        out.putInt("load", load);
        out.putInt("capacity", capacity);
    }

    public void load(ValueInput in) {
        rpm = in.getFloatOr("rpm", 0.0f);
        int s = in.getIntOr("status", 0);
        status = s >= 0 && s < Status.values().length ? Status.values()[s] : Status.IDLE;
        load = in.getIntOr("load", 0);
        capacity = in.getIntOr("capacity", 0);
    }
}
