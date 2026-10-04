package dev.strataindustria.casting;

import dev.strataindustria.knapping.Shaping;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** A plank cut to size. Right-click strikes it into the shape picked (sneak + right-click picks), the same as clay forming; the result is a carved pattern. */
public class PatternBlankItem extends Item {
    public PatternBlankItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer server) Shaping.use(server, hand);
        return InteractionResult.SUCCESS;
    }
}
