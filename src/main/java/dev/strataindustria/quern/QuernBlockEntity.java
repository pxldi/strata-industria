package dev.strataindustria.quern;

import dev.strataindustria.registry.ModBlockEntities;
import dev.strataindustria.registry.ModRecipes;
import dev.strataindustria.registry.ModSounds;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The hand quern (spec 10.1): a stack waits on the top stone, and each turn of the handle grinds a
 * little of the next item. Ground items are pushed out over the rim.
 */
public class QuernBlockEntity extends BlockEntity {
    /** Holding right-click repeats the use every 4 ticks, so one turn counts as 4 ticks of grinding. */
    public static final int TICKS_PER_TURN = 4;

    private ItemStack input = ItemStack.EMPTY;
    private int progress;

    public QuernBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.QUERN.get(), pos, state);
    }

    public ItemStack input() {
        return input;
    }

    public static Optional<RecipeHolder<QuernRecipe>> recipeFor(Level level, ItemStack stack) {
        if (stack.isEmpty() || !(level instanceof ServerLevel server)) return Optional.empty();
        return server.recipeAccess().getRecipeFor(ModRecipes.QUERN.get(), new SingleRecipeInput(stack), level);
    }

    /** Puts as much of {@code held} on the stone as fits. Returns whether anything moved. */
    public boolean insert(ItemStack held) {
        if (!input.isEmpty() && !ItemStack.isSameItemSameComponents(input, held)) return false;
        int room = held.getMaxStackSize() - input.getCount();
        if (room <= 0) return false;
        int moved = Math.min(room, held.getCount());
        if (input.isEmpty()) input = held.split(moved);
        else {
            input.grow(moved);
            held.shrink(moved);
        }
        progress = 0;
        changed();
        return true;
    }

    public ItemStack takeAll() {
        ItemStack taken = input;
        input = ItemStack.EMPTY;
        progress = 0;
        changed();
        return taken;
    }

    /** One turn of the handle. Returns false when there is nothing to grind. */
    public boolean turn(ServerLevel level, BlockPos pos) {
        Optional<RecipeHolder<QuernRecipe>> recipe = recipeFor(level, input);
        if (recipe.isEmpty()) return false;
        progress += TICKS_PER_TURN;
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, input.getItem()), pos.getX() + 0.5, pos.getY() + 0.62, pos.getZ() + 0.5,
                2, 0.25, 0.02, 0.25, 0.03);
        if (progress >= recipe.get().value().ticks()) {
            progress = 0;
            ItemStack out = recipe.get().value().assemble(new SingleRecipeInput(input));
            input.shrink(1);
            if (input.isEmpty()) input = ItemStack.EMPTY;
            ItemEntity drop = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, out);
            drop.setDeltaMovement((level.getRandom().nextDouble() - 0.5) * 0.15, 0.12, (level.getRandom().nextDouble() - 0.5) * 0.15);
            drop.setDefaultPickUpDelay();
            level.addFreshEntity(drop);
            level.playSound(null, pos, ModSounds.QUERN_DONE.get(), SoundSource.BLOCKS, 0.6f, 0.9f + level.getRandom().nextFloat() * 0.2f);
            changed();
        } else {
            setChanged();
        }
        return true;
    }

    /** Breaking the quern spills the waiting stack. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null && !input.isEmpty()) Block.popResource(level, pos, input);
        input = ItemStack.EMPTY;
    }

    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        input = in.read("input", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        progress = in.getIntOr("progress", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (!input.isEmpty()) out.store("input", ItemStack.OPTIONAL_CODEC, input);
        out.putInt("progress", progress);
    }

    // The client draws the waiting stack on the stone.
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
