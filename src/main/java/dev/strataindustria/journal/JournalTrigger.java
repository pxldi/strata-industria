package dev.strataindustria.journal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * One trigger for every journal goal that no vanilla trigger covers (spec 11): lighting a fire pit,
 * a finished kiln, a molten crucible and so on. Each goal names its event.
 */
public class JournalTrigger extends SimpleCriterionTrigger<JournalTrigger.TriggerInstance> {
    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, String event) {
        trigger(player, instance -> instance.event().equals(event));
    }

    public record TriggerInstance(Optional<Holder<LootItemCondition>> player, String event)
            implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(i -> i.group(
                LootItemCondition.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                Codec.STRING.fieldOf("event").forGetter(TriggerInstance::event)
        ).apply(i, TriggerInstance::new));

        public static Criterion<TriggerInstance> of(String event) {
            return Journal.TRIGGER.get().createCriterion(new TriggerInstance(Optional.empty(), event));
        }
    }
}
