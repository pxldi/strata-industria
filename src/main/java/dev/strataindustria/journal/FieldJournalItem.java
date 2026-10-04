package dev.strataindustria.journal;

import dev.strataindustria.client.JournalClient;
import dev.strataindustria.registry.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * The field journal (spec 3.6, journal leads spec): use it to open the leads notebook, or sneak and use it on a
 * block to study that block.
 */
public class FieldJournalItem extends Item {
    /** Ticks between two studies, so a held button does not fill the notebook. */
    static final int STUDY_COOLDOWN = 20;

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

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (player instanceof ServerPlayer server) {
            Study.study(server, context.getClickedPos());
            player.getCooldowns().addCooldown(context.getItemInHand(), STUDY_COOLDOWN);
        }
        return InteractionResult.SUCCESS;
    }
}
