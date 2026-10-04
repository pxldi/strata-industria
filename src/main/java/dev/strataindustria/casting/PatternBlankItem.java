package dev.strataindustria.casting;

import dev.strataindustria.knapping.Knapping;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** A plank cut to size. Right-click opens the same grid as clay forming; what is left is a carved pattern. */
public class PatternBlankItem extends Item {
    public PatternBlankItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (player instanceof ServerPlayer server) Knapping.tryOpen(server, hand);
        return InteractionResult.SUCCESS;
    }
}
