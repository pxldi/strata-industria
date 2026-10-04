package dev.strataindustria.mark;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** A punch with a blank face. Right-click opens the editor to cut a new mark. */
public class MakerPunchItem extends Item {
    public MakerPunchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer server) MakerMarks.openEditor(server, null);
        return InteractionResult.SUCCESS;
    }
}
