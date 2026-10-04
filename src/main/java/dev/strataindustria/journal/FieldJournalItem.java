package dev.strataindustria.journal;

import dev.strataindustria.client.JournalClient;
import dev.strataindustria.registry.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Opens the advancements screen on the journal's tab (spec 3.6). */
public class FieldJournalItem extends Item {
    public FieldJournalItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            JournalClient.open();
        } else {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.JOURNAL_OPEN.get(),
                    SoundSource.PLAYERS, 0.8f, 0.9f + level.getRandom().nextFloat() * 0.2f);
        }
        return InteractionResult.SUCCESS;
    }
}
