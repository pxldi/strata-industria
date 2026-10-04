package dev.strataindustria.smithing;

import com.mojang.serialization.Codec;
import dev.strataindustria.StrataIndustria;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The anvil plans one player has finished by hand. Once a plan is in here the anvil's Quick button works for
 * that player on that plan. Kept on the player and copied on death; the menu reports the answer, so nothing
 * needs syncing.
 */
public final class SmithedRecipes {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, StrataIndustria.MOD_ID);

    private static final DeferredHolder<AttachmentType<?>, AttachmentType<SmithedRecipes>> TYPE = ATTACHMENTS.register("smithed_recipes",
            () -> AttachmentType.<SmithedRecipes>builder(() -> new SmithedRecipes())
                    .serialize(Identifier.CODEC.listOf().xmap(SmithedRecipes::new, SmithedRecipes::list).fieldOf("plans"))
                    .copyOnDeath()
                    .build());

    private final Set<Identifier> plans = new HashSet<>();

    public SmithedRecipes() {}

    private SmithedRecipes(java.util.List<Identifier> plans) {
        this.plans.addAll(plans);
    }

    private java.util.List<Identifier> list() {
        return plans.stream().sorted().toList();
    }

    public static SmithedRecipes of(ServerPlayer player) {
        return player.getData(TYPE);
    }

    public boolean has(Identifier plan) {
        return plans.contains(plan);
    }

    public void add(Identifier plan) {
        plans.add(plan);
    }

    public static void register(IEventBus bus) {
        ATTACHMENTS.register(bus);
    }
}
