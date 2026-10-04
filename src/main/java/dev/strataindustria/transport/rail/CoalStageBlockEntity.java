package dev.strataindustria.transport.rail;

import dev.strataindustria.steam.FireboxBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/** Nine slots of fuel and the work of topping up a standing locomotive from them (outposts spec 7.4). */
public class CoalStageBlockEntity extends RandomizableContainerBlockEntity {
    public static final int SLOTS = 9;
    /** Blocks sideways from the stage an engine may stand, and blocks up and down from it. */
    public static final double REACH = 2.5;
    public static final double HEIGHT = 3.0;
    /** Ticks between one lump and the next, and between looks for an engine when idle. */
    private static final int LUMP_EVERY = 2;
    private static final int LOOK_EVERY = 10;

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private int loadingFor;

    public CoalStageBlockEntity(BlockPos pos, BlockState state) {
        super(RailwayRegistry.COAL_STAGE_ENTITY.get(), pos, state);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.strataindustria.coal_stage");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new DispenserMenu(containerId, inventory, this);
    }

    /** Only fuel goes in through a hopper or a chute. */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return FireboxBlockEntity.fuelFor(stack) != null;
    }

    /** A standing engine in reach with room in its fuel slot. */
    public @Nullable SteamLocomotiveEntity hungry(ServerLevel server) {
        AABB box = new AABB(worldPosition).inflate(REACH, HEIGHT, REACH);
        for (SteamLocomotiveEntity loco : server.getEntitiesOfClass(SteamLocomotiveEntity.class, box, SteamLocomotiveEntity::holding)) {
            ItemStack slot = loco.fuel();
            if (slot.isEmpty() || slot.getCount() < slot.getMaxStackSize()) return loco;
        }
        return null;
    }

    public void serverTick(ServerLevel server, BlockPos pos, BlockState state) {
        if (loadingFor > 0) loadingFor--;
        long clock = server.getGameTime() + pos.asLong();
        if (loadingFor == 0 && clock % LOOK_EVERY != 0) return;
        if (loadingFor > 0 && clock % LUMP_EVERY != 0) return;
        SteamLocomotiveEntity loco = hungry(server);
        boolean moved = false;
        if (loco != null) {
            for (int slot = 0; slot < SLOTS && !moved; slot++) {
                ItemStack stack = items.get(slot);
                if (stack.isEmpty() || FireboxBlockEntity.fuelFor(stack) == null) continue;
                ItemStack shown = stack.copyWithCount(1);
                if (loco.takeFuel(shown) > 0) {
                    stack.shrink(1);
                    if (stack.isEmpty()) items.set(slot, ItemStack.EMPTY);
                    setChanged();
                    moved = true;
                    lump(server, pos, loco, shown);
                }
            }
        }
        if (moved) {
            loadingFor = 6;
        } else if (state.getValue(CoalStageBlock.LOADING) && loadingFor == 0) {
            server.setBlock(pos, state.setValue(CoalStageBlock.LOADING, false), Block.UPDATE_ALL);
            server.playSound(null, pos, net.minecraft.sounds.SoundEvents.WOODEN_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.6f, 0.8f);
        }
        if (moved && !state.getValue(CoalStageBlock.LOADING)) {
            server.setBlock(pos, state.setValue(CoalStageBlock.LOADING, true), Block.UPDATE_ALL);
            server.playSound(null, pos, net.minecraft.sounds.SoundEvents.WOODEN_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.7f, 0.9f);
        }
    }

    /** One lump down the chute: a rattle that climbs in pitch as the fuel slot fills, and the lump flying across. */
    private void lump(ServerLevel server, BlockPos pos, SteamLocomotiveEntity loco, ItemStack shown) {
        int full = loco.fuel().getMaxStackSize();
        float fill = Math.min(1f, loco.fuel().getCount() / (float) full);
        server.playSound(null, pos, RailwayRegistry.COAL_LOAD.get(), SoundSource.BLOCKS, 0.7f, 0.8f + 0.7f * fill);
        double x = pos.getX() + 0.5, y = pos.getY() + 0.9, z = pos.getZ() + 0.5;
        for (int i = 0; i < 3; i++) {
            double t = (i + 1) / 4.0;
            server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, shown.getItem()), x + (loco.getX() - x) * t, y + 0.4 * Math.sin(t * Math.PI),
                    z + (loco.getZ() - z) * t, 1, 0.02, 0.02, 0.02, 0.0);
        }
        if (loco.fuel().getCount() >= full) {
            server.playSound(null, pos, RailwayRegistry.BUFFER_CLANG.get(), SoundSource.BLOCKS, 0.4f, 1.8f);
        }
    }
}
