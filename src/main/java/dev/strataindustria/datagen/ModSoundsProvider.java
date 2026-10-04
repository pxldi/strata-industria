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
        add(ModSounds.QUENCH, definition().subtitle(subtitle("heat.quench"))
                .with(sound("minecraft:random/fizz").pitch(1.4f).volume(0.6f))
                .with(sound("minecraft:random/fizz").pitch(1.7f).volume(0.5f)));
        add(ModSounds.FORGE_IGNITE, definition().subtitle(subtitle("forge.ignite"))
                .with(sound("minecraft:fire/ignite").pitch(0.7f))
                .with(sound("minecraft:fire/fire").pitch(0.8f).volume(0.8f)));
        // Metal: lava pops for a piece slumping into the melt, a poured bucket of lava, and fired clay
        // knocking or cracking.
        add(ModSounds.CRUCIBLE_MELT, definition().subtitle(subtitle("crucible.melt"))
                .with(sound("minecraft:liquid/lavapop").pitch(0.8f).volume(0.7f))
                .with(sound("minecraft:liquid/lavapop").pitch(1.1f).volume(0.6f)));
        SoundDefinition pour = definition().subtitle(subtitle("crucible.pour"));
        for (int i = 1; i <= 3; i++) pour.with(sound("minecraft:item/bucket/empty_lava" + i).pitch(1.2f).volume(0.8f));
        add(ModSounds.CRUCIBLE_POUR, pour);
        add(ModSounds.MOLD_KNOCK, stone("mold.knock", 1.4f, 0.8f));
        SoundDefinition crack = definition().subtitle(subtitle("mold.break"));
        for (int i = 1; i <= 3; i++) crack.with(sound("minecraft:random/glass" + i).pitch(0.6f).volume(0.7f));
        add(ModSounds.MOLD_BREAK, crack);
        // Quern: gravelly stone-on-stone for the grind, a soft scrape for loading and a crunch when done.
        SoundDefinition grind = definition().subtitle(subtitle("quern.grind"));
        for (int i = 1; i <= 4; i++) grind.with(sound("minecraft:step/gravel" + i).pitch(0.6f).volume(0.7f));
        for (int i = 1; i <= 4; i++) grind.with(sound("minecraft:dig/stone" + i).pitch(0.5f).volume(0.35f));
        add(ModSounds.QUERN_GRIND, grind);
        SoundDefinition load = definition().subtitle(subtitle("quern.load"));
        for (int i = 1; i <= 4; i++) load.with(sound("minecraft:step/gravel" + i).pitch(1.3f).volume(0.5f));
        add(ModSounds.QUERN_LOAD, load);
        SoundDefinition done = definition().subtitle(subtitle("quern.done"));
        for (int i = 1; i <= 4; i++) done.with(sound("minecraft:dig/gravel" + i).pitch(1.2f).volume(0.6f));
        add(ModSounds.QUERN_DONE, done);
        // Smithing: a bright ring for each blow, a heavier ring to finish, stone chips for dressing an anvil.
        add(ModSounds.SMITH_HIT, definition().subtitle(subtitle("anvil.hit"))
                .with(sound("minecraft:random/anvil_use").pitch(1.3f).volume(0.5f)));
        add(ModSounds.SMITH_DONE, definition().subtitle(subtitle("anvil.done"))
                .with(sound("minecraft:random/anvil_land").pitch(1.4f).volume(0.5f)));
        SoundDefinition dress = definition().subtitle(subtitle("anvil.dress"));
        for (int i = 1; i <= 4; i++) dress.with(sound("minecraft:dig/stone" + i).pitch(0.7f));
        add(ModSounds.ANVIL_DRESS, dress);
        add(ModSounds.PROSPECT, stone("prospect", 1.9f, 0.6f));
        // The journal: pages flipping open.
        SoundDefinition pages = definition().subtitle(subtitle("journal.open"));
        for (int i = 1; i <= 3; i++) pages.with(sound("minecraft:item/book/open_flip" + i).volume(0.8f));
        add(ModSounds.JOURNAL_OPEN, pages);
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
