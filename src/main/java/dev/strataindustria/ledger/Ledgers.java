package dev.strataindustria.ledger;

import com.mojang.serialization.Codec;
import dev.strataindustria.StrataIndustria;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** What each player has entered in their builder's ledger: the multiblocks they have seen whole. Kept on the player and copied on death. */
public final class Ledgers {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, StrataIndustria.MOD_ID);

    private static final DeferredHolder<AttachmentType<?>, AttachmentType<Set<String>>> TYPE = ATTACHMENTS.register("ledger_entries",
            () -> AttachmentType.<Set<String>>builder(() -> Set.of())
                    .serialize(Codec.STRING.listOf().<Set<String>>xmap(Set::copyOf, List::copyOf).fieldOf("plans"))
                    .copyOnDeath()
                    .build());

    private Ledgers() {}

    public static void register(IEventBus bus) {
        ATTACHMENTS.register(bus);
    }

    public static boolean knows(Player player, Plan plan) {
        return player.getData(TYPE).contains(plan.id());
    }

    /** Enters a plan. Returns false when it was in the ledger already. */
    public static boolean learn(Player player, Plan plan) {
        if (knows(player, plan)) return false;
        Set<String> plans = new HashSet<>(player.getData(TYPE));
        plans.add(plan.id());
        player.setData(TYPE, Set.copyOf(plans));
        return true;
    }
}
