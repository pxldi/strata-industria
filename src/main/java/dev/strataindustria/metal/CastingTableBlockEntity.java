package dev.strataindustria.metal;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.heat.Heat;
import dev.strataindustria.registry.ModDataComponents;
import dev.strataindustria.registry.ModSounds;
import dev.strataindustria.registry.PrologueRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The casting table: four fired molds laid out on a stone top. A crucible (or smelter) beside it pours into
 * every empty mold in turn, and one click on the table knocks out every casting that has set.
 *
 * <p>Hook for pattern casting: the table only asks {@link #accepts(ItemStack)} what may be laid on it and
 * {@link #nextEmptyMold()} what to pour into, so a sand flask with a pressed mold can join later without
 * changing the crucible.
 */
public class CastingTableBlockEntity extends BlockEntity {
    public static final int SLOTS = 4;

    private final NonNullList<ItemStack> molds = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    public CastingTableBlockEntity(BlockPos pos, BlockState state) {
        super(PrologueRegistry.CASTING_TABLE_BE.get(), pos, state);
    }

    /** What may be laid on the table. */
    public static boolean accepts(ItemStack stack) {
        return stack.getItem() instanceof CastMoldItem;
    }

    public NonNullList<ItemStack> molds() {
        return molds;
    }

    private static boolean isEmptyMold(ItemStack stack) {
        return accepts(stack) && !stack.has(ModDataComponents.CAST_CONTENTS.get());
    }

    /** The first mold with nothing in it, the stack itself so a pour can fill it in place; empty when there is none. */
    public ItemStack nextEmptyMold() {
        for (ItemStack stack : molds) if (isEmptyMold(stack)) return stack;
        return ItemStack.EMPTY;
    }

    public boolean hasEmptyMold() {
        return !nextEmptyMold().isEmpty();
    }

    /** Lays one mold on the first free spot; false when the table is full. */
    public boolean place(ItemStack one) {
        for (int i = 0; i < SLOTS; i++) {
            if (!molds.get(i).isEmpty()) continue;
            molds.set(i, one.copyWithCount(1));
            changed();
            return true;
        }
        return false;
    }

    /** Takes the last mold off the table, empty ones before filled ones. */
    public ItemStack takeBack() {
        for (int pass = 0; pass < 2; pass++) {
            for (int i = SLOTS - 1; i >= 0; i--) {
                ItemStack stack = molds.get(i);
                if (stack.isEmpty() || (pass == 0) != isEmptyMold(stack)) continue;
                molds.set(i, ItemStack.EMPTY);
                changed();
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /** How many molds hold metal that has not yet set. */
    public int moltenMolds() {
        int n = 0;
        for (ItemStack stack : molds) {
            Melt contents = stack.get(ModDataComponents.CAST_CONTENTS.get());
            if (contents != null && Heat.get(stack, level) >= CrucibleBlockEntity.mixMeltingPoint(contents)) n++;
        }
        return n;
    }

    /** Knocks out every casting that has set, hands them to the player, and tells them how it went. */
    public int knockOut(ServerLevel server, Player player) {
        int out = 0;
        for (int i = 0; i < SLOTS; i++) {
            ItemStack mold = molds.get(i);
            Melt contents = mold.get(ModDataComponents.CAST_CONTENTS.get());
            if (contents == null) continue;
            float heat = Heat.get(mold, server);
            if (heat >= CrucibleBlockEntity.mixMeltingPoint(contents)) continue;
            ItemStack cast = CastMoldItem.castOf(mold, heat, server.getGameTime());
            dev.strataindustria.mark.MakerMarks.stampCasting(mold, cast, player);
            if (CastMoldItem.breaks(mold, server.getRandom())) {
                molds.set(i, ItemStack.EMPTY);
                server.playSound(null, worldPosition, ModSounds.MOLD_BREAK.get(), SoundSource.BLOCKS, 0.9f, 1.0f);
                player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".mold.broke"));
            } else {
                molds.set(i, CastMoldItem.emptied(mold));
            }
            if (!player.addItem(cast)) Block.popResource(server, worldPosition.above(), cast);
            out++;
        }
        if (out > 0) {
            server.playSound(null, worldPosition, PrologueRegistry.TABLE_KNOCK.get(), SoundSource.BLOCKS, 0.9f, 0.9f + server.getRandom().nextFloat() * 0.2f);
            changed();
        }
        return out;
    }

    /** Syncs the molds to clients, which draw them on the table. */
    public void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null) return;
        for (ItemStack mold : molds) if (!mold.isEmpty()) Block.popResource(level, pos, mold);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        for (int i = 0; i < SLOTS; i++) molds.set(i, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(in, molds);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        ContainerHelper.saveAllItems(out, molds);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
