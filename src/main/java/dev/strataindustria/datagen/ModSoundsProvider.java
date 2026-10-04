package dev.strataindustria.datagen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.registry.ModSounds;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.common.data.SoundDefinitionsProvider;

/** sounds.json. Our sounds are built from vanilla stone samples, pitched to suit each action. */
final class ModSoundsProvider extends SoundDefinitionsProvider {
    ModSoundsProvider(PackOutput output) {
        super(output, StrataIndustria.MOD_ID);
    }

    @Override
    public void registerSounds() {
        // A dull chip off a rock.
        add(ModSounds.KNAP_ROCK, stone("knapping.rock", 1.15f, 0.9f));
        // Flint rings sharper and higher.
        add(ModSounds.KNAP_FLINT, stone("knapping.flint", 1.7f, 0.8f));
        // The last flake falls and the head comes free: a deeper knock.
        add(ModSounds.KNAP_FINISH, stone("knapping.finish", 0.75f, 1.0f));

        // Fire: wood friction for the drill, then vanilla fire samples.
        SoundDefinition drill = definition().subtitle(subtitle("firestarter.drill"));
        for (int i = 1; i <= 4; i++) drill.with(sound("minecraft:step/wood" + i).pitch(1.6f).volume(0.5f));
        add(ModSounds.FIRESTARTER_DRILL, drill);
        add(ModSounds.FIRE_PIT_IGNITE, definition().subtitle(subtitle("fire_pit.ignite"))
                .with(sound("minecraft:fire/ignite").pitch(0.9f))
                .with(sound("minecraft:fire/fire").pitch(1.2f).volume(0.6f)));
        add(ModSounds.FIRE_PIT_EXTINGUISH, definition().subtitle(subtitle("fire_pit.extinguish"))
                .with(sound("minecraft:random/fizz")));
        add(ModSounds.FIRE_PIT_TORCH, definition().subtitle(subtitle("fire_pit.torch"))
                .with(sound("minecraft:fire/ignite").pitch(1.3f).volume(0.7f)));

        // Clay: wet squelches for shaping, then the kiln's thatch, logs and the cooling crackle.
        SoundDefinition shape = definition().subtitle(subtitle("clay.shape"));
        for (int i = 1; i <= 5; i++) shape.with(sound("minecraft:mob/slime/small" + i).pitch(0.8f).volume(0.35f));
        add(ModSounds.CLAY_SHAPE, shape);
        SoundDefinition finish = definition().subtitle(subtitle("clay.finish"));
        for (int i = 1; i <= 3; i++) finish.with(sound("minecraft:mob/slime/big" + i).pitch(1.1f).volume(0.5f));
        add(ModSounds.CLAY_FINISH, finish);
        SoundDefinition straw = definition().subtitle(subtitle("pit_kiln.straw"));
        for (int i = 1; i <= 6; i++) straw.with(sound("minecraft:step/grass" + i).pitch(0.9f));
        add(ModSounds.KILN_STRAW, straw);
        SoundDefinition log = definition().subtitle(subtitle("pit_kiln.log"));
        for (int i = 1; i <= 4; i++) log.with(sound("minecraft:dig/wood" + i).pitch(0.8f));
        add(ModSounds.KILN_LOG, log);
        add(ModSounds.KILN_FIRED, definition().subtitle(subtitle("pit_kiln.fired"))
                .with(sound("minecraft:random/fizz").pitch(0.6f).volume(0.7f)));
    }

    private static String subtitle(String name) {
        return "subtitles." + StrataIndustria.MOD_ID + "." + name;
    }

    private static SoundDefinition stone(String subtitle, float pitch, float volume) {
        SoundDefinition definition = definition().subtitle("subtitles." + StrataIndustria.MOD_ID + "." + subtitle);
        for (int i = 1; i <= 4; i++) {
            definition.with(sound("minecraft:dig/stone" + i).pitch(pitch).volume(volume));
        }
        return definition;
    }
}
