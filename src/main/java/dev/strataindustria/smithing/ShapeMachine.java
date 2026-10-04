package dev.strataindustria.smithing;

import dev.strataindustria.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** A hammer machine with a shape button on its screen: trip, steam and power hammer. Implemented by block entities. */
public interface ShapeMachine {
    /** Menu button ids for the shape button: left click goes on, right click goes back. */
    int BUTTON_NEXT = 1, BUTTON_PREVIOUS = 2;

    ShapeSelector shapes();

    /** The piece the shape is for: the one on the anvil, else the one waiting to go on. */
    ItemStack shapePiece();

    /** The shape button was pressed: the next (or previous) shape, a note up the scale and a few sparks. */
    default void cycleShape(ServerPlayer player, int step) {
        BlockEntity machine = (BlockEntity) this;
        if (!(machine.getLevel() instanceof ServerLevel level)) return;
        int at = shapes().cycle(level, shapePiece(), step);
        BlockPos pos = machine.getBlockPos();
        level.playSound(null, pos, ModSounds.ANVIL_VOICE_BRONZE.get(), SoundSource.BLOCKS, 0.5f, Smithing.scaleNote(at));
        level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3f, 1.4f);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 4, 0.2, 0.05, 0.2, 0.1);
        machine.setChanged();
    }

    /** Handles a menu button: true when it was one of the shape button's. */
    static boolean press(Object container, ServerPlayer player, int id) {
        if (!(container instanceof ShapeMachine machine) || id != BUTTON_NEXT && id != BUTTON_PREVIOUS) return false;
        machine.cycleShape(player, id == BUTTON_NEXT ? 1 : -1);
        return true;
    }
}
