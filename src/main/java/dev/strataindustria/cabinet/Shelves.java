package dev.strataindustria.cabinet;

import com.mojang.serialization.Codec;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.block.OreBlock;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.geology.RockLookup;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The shelves one player has filled in a specimen cabinet (uniqueness 9.5). Kept on the player and copied on
 * death. A full rock shelf lets their prospector's pick reach further into that kind of rock, and the full
 * mineral shelf lets it name a fourth deposit.
 */
public final class Shelves {
    /** Extra blocks of reach a prospector's pick gets in a rock whose shelf is full. */
    public static final int REACH_BONUS = 4;

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, StrataIndustria.MOD_ID);

    private static final DeferredHolder<AttachmentType<?>, AttachmentType<List<String>>> TYPE = ATTACHMENTS.register("full_shelves",
            () -> AttachmentType.<List<String>>builder(() -> List.of())
                    .serialize(Codec.STRING.listOf().fieldOf("shelves"))
                    .copyOnDeath()
                    .build());

    private Shelves() {}

    public static List<String> of(ServerPlayer player) {
        return player.getData(TYPE);
    }

    public static boolean has(ServerPlayer player, String shelf) {
        return of(player).contains(shelf);
    }

    /** Marks a shelf full for the player. Returns whether it was new. */
    public static boolean add(ServerPlayer player, String shelf) {
        List<String> shelves = of(player);
        if (shelves.contains(shelf)) return false;
        List<String> next = new ArrayList<>(shelves);
        next.add(shelf);
        player.setData(TYPE, List.copyOf(next));
        return true;
    }

    /** Whether the player has the full shelf for the rock that {@code state} is, host rock or ore alike. */
    public static boolean knowsRock(ServerPlayer player, BlockState state) {
        Rock rock = state.getBlock() instanceof OreBlock ore ? ore.rock() : RockLookup.rawRock(state);
        return rock != null && has(player, rock.category().getSerializedName());
    }

    public static void register(IEventBus bus) {
        ATTACHMENTS.register(bus);
    }
}
