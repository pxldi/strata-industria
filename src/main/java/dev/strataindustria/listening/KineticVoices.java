package dev.strataindustria.listening;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.power.GearboxBlock;
import dev.strataindustria.power.Kinetic;
import dev.strataindustria.power.PulleyBlockEntity;
import dev.strataindustria.power.StepUpGearboxBlock;
import dev.strataindustria.power.WaterWheelBlockEntity;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Gear and belt voices (uniqueness 2.1). A running kinetic network that is working hard makes noise: belts squeal
 * and a water wheel groans near the limit, gears tick faster as the load rises. The kinetic solver tells this class
 * which parts of a network are loud whenever it recomputes one; a level tick plays the sounds.
 */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class KineticVoices {
    /** Share of the network's capacity at which belts squeal and the wheel groans. */
    public static final float STRAIN = 0.85f;
    /** Share at which gears start to tick. */
    public static final float TICKING = 0.5f;

    public enum Voice { BELT, GEAR, WHEEL }

    private static final Map<ResourceKey<Level>, Map<BlockPos, Entry>> LOUD = new HashMap<>();

    private record Entry(Voice voice, float load) {}

    private KineticVoices() {}

    /** Which voice a kinetic part has, or null if it is quiet. */
    public static Voice voiceOf(Kinetic part) {
        if (part instanceof PulleyBlockEntity pulley) return pulley.link() != null ? Voice.BELT : null;
        if (part instanceof WaterWheelBlockEntity) return Voice.WHEEL;
        if (part instanceof BlockEntity be && (be.getBlockState().getBlock() instanceof GearboxBlock
                || be.getBlockState().getBlock() instanceof StepUpGearboxBlock)) return Voice.GEAR;
        return null;
    }

    /** Whether a voice speaks at this load share. */
    public static boolean speaks(Voice voice, float load) {
        return voice == Voice.GEAR ? load >= TICKING : load >= STRAIN;
    }

    /** Called after every network rebuild: remember which members are loud, forget the ones that no longer are. */
    public static void update(ServerLevel level, Map<BlockPos, ? extends Kinetic> members, boolean running, int load, int capacity) {
        Map<BlockPos, Entry> loud = LOUD.computeIfAbsent(level.dimension(), k -> new HashMap<>());
        float share = capacity <= 0 ? 0.0f : (float) load / capacity;
        for (var member : members.entrySet()) {
            Voice voice = voiceOf(member.getValue());
            if (running && voice != null && speaks(voice, share)) loud.put(member.getKey().immutable(), new Entry(voice, share));
            else loud.remove(member.getKey());
        }
    }

    /** The positions currently loud in a level, for game tests. */
    public static int loudCount(ServerLevel level) {
        Map<BlockPos, Entry> loud = LOUD.get(level.dimension());
        return loud == null ? 0 : loud.size();
    }

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Map<BlockPos, Entry> loud = LOUD.get(level.dimension());
        if (loud == null || loud.isEmpty()) return;
        long time = level.getGameTime();
        loud.entrySet().removeIf(entry -> {
            BlockPos pos = entry.getKey();
            if (!level.isLoaded(pos)) return true;
            if (!(level.getBlockEntity(pos) instanceof Kinetic part) || voiceOf(part) != entry.getValue().voice) return true;
            play(level, pos, entry.getValue(), time);
            return false;
        });
    }

    private static void play(ServerLevel level, BlockPos pos, Entry entry, long time) {
        float over = Math.min(1.0f, Math.max(0.0f, (entry.load - TICKING) / (1.0f - TICKING)));
        switch (entry.voice) {
            case BELT -> {
                if ((time + pos.asLong()) % 50 == 0) sound(level, pos, ListeningSounds.BELT_SQUEAL.get(), 0.5f, 0.9f + entry.load * 0.2f);
            }
            case WHEEL -> {
                if ((time + pos.asLong()) % 70 == 0) sound(level, pos, ListeningSounds.WATER_WHEEL_GROAN.get(), 0.7f, 0.85f + entry.load * 0.15f);
            }
            case GEAR -> {
                // Ticks come quicker as the load rises: every 24 ticks at half load down to every 8 at the limit.
                int interval = Math.round(24 - 16 * over);
                if ((time + pos.asLong()) % interval == 0) sound(level, pos, ListeningSounds.GEAR_TICK.get(), 0.35f, 0.9f + over * 0.4f);
            }
        }
    }

    private static void sound(ServerLevel level, BlockPos pos, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, pos, sound, SoundSource.BLOCKS, volume, pitch);
    }
}
