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
    }

    private static SoundDefinition stone(String subtitle, float pitch, float volume) {
        SoundDefinition definition = definition().subtitle("subtitles." + StrataIndustria.MOD_ID + "." + subtitle);
        for (int i = 1; i <= 4; i++) {
            definition.with(sound("minecraft:dig/stone" + i).pitch(pitch).volume(volume));
        }
        return definition;
    }
}
