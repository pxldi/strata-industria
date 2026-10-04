package dev.strataindustria.prospecting;

import dev.strataindustria.client.CoreSampleClient;
import dev.strataindustria.registry.ModDataComponents;
import dev.strataindustria.registry.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** A drilled core (tier 3 spec 8.5). Use it to read the column it came from. */
public class CoreSampleItem extends Item {
    public CoreSampleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        CoreSample sample = player.getItemInHand(hand).get(ModDataComponents.CORE_SAMPLE.get());
        if (sample == null) return InteractionResult.PASS;
        if (level.isClientSide()) {
            CoreSampleClient.open(sample);
        } else {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.CORE_SAMPLE_OPEN.get(),
                    SoundSource.PLAYERS, 0.6f, 0.9f + level.getRandom().nextFloat() * 0.2f);
        }
        return InteractionResult.SUCCESS;
    }
}
