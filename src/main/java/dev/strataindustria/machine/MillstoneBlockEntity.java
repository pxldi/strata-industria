package dev.strataindustria.machine;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.power.KineticConsumer;
import dev.strataindustria.power.KineticNetworks;
import dev.strataindustria.power.KineticState;
import dev.strataindustria.quern.QuernBlockEntity;
import dev.strataindustria.quern.QuernRecipe;
import dev.strataindustria.registry.ModBlockEntities;
import dev.strataindustria.registry.ModSounds;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The millstone (spec 8.1): the hand quern on a shaft. It runs every quern recipe, 40 ticks per item
 * at 16 RPM and proportionally faster or slower. Hoppers feed the top and sides and empty the bottom.
 */
public class MillstoneBlockEntity extends BaseContainerBlockEntity implements KineticConsumer, WorldlyContainer {
    public static final int INPUT = 0, OUTPUT = 1;
    public static final int IMPACT = 4, MIN_SPEED = 4;
    public static final float BASE_TICKS = 40.0f;
    public static final int DATA_PROGRESS = 0, DATA_STATUS = 1, DATA_COUNT = 2;

    public enum Status {
        EMPTY, WORKING, NOT_TURNING, TOO_SLOW, NO_RECIPE, OUTPUT_FULL;

        public String key() {
            return StrataIndustria.MOD_ID + ".machine." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private static final int[] TOP_AND_SIDES = {INPUT};
    private static final int[] BOTTOM = {OUTPUT};

    private NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private final KineticState kinetic = new KineticState();
    private float progress;
    private Status status = Status.EMPTY;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> Math.round(progress / BASE_TICKS * 1000);
                case DATA_STATUS -> status.ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public MillstoneBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MILLSTONE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MillstoneBlockEntity mill) {
        Status before = mill.status;
        mill.status = mill.work((ServerLevel) level);
        if (mill.status != before) mill.setChanged();
    }

    private Status work(ServerLevel level) {
        ItemStack input = items.get(INPUT);
        if (input.isEmpty()) {
            progress = 0;
            return Status.EMPTY;
        }
        Optional<RecipeHolder<QuernRecipe>> recipe = QuernBlockEntity.recipeFor(level, input);
        if (recipe.isEmpty()) return Status.NO_RECIPE;
        float rpm = kinetic.rpm();
        if (rpm <= 0) return Status.NOT_TURNING;
        if (rpm < MIN_SPEED) return Status.TOO_SLOW;
        ItemStack result = recipe.get().value().assemble(new SingleRecipeInput(input));
        ItemStack output = items.get(OUTPUT);
        if (!output.isEmpty() && (!ItemStack.isSameItemSameComponents(output, result)
                || output.getCount() + result.getCount() > output.getMaxStackSize())) {
            return Status.OUTPUT_FULL;
        }
        progress += rpm / 16.0f;
        if (level.getGameTime() % 20 == 0) {
            level.playSound(null, worldPosition, ModSounds.MILLSTONE_GRIND.get(), SoundSource.BLOCKS, 0.5f, 0.9f + level.getRandom().nextFloat() * 0.2f);
        }
        if (level.getGameTime() % 5 == 0) {
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, input.getItem()), worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.9, worldPosition.getZ() + 0.5, 2, 0.3, 0.02, 0.3, 0.02);
        }
        if (progress >= BASE_TICKS) {
            progress = 0;
            input.shrink(1);
            if (output.isEmpty()) items.set(OUTPUT, result);
            else output.grow(result.getCount());
            level.playSound(null, worldPosition, ModSounds.QUERN_DONE.get(), SoundSource.BLOCKS, 0.5f, 0.8f + level.getRandom().nextFloat() * 0.2f);
            Journal.awardNear(level, worldPosition, Journal.MILLSTONE);
        }
        setChanged();
        return Status.WORKING;
    }

    public Status status() {
        return status;
    }

    // ------------------------------------------------------------------ kinetics

    @Override
    public boolean connects(Direction side) {
        return side != Direction.UP;
    }

    @Override
    public KineticState kinetic() {
        return kinetic;
    }

    @Override
    public int impact() {
        return IMPACT;
    }

    @Override
    public int minSpeed() {
        return MIN_SPEED;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        KineticNetworks.markDirty(level, worldPosition);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        KineticNetworks.markDirty(level, worldPosition);
    }

    // ------------------------------------------------------------------ container

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? BOTTOM : TOP_AND_SIDES;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == INPUT && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == OUTPUT;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == INPUT && (level == null || QuernBlockEntity.recipeFor(level, stack).isPresent() || level.isClientSide());
    }

    @Override
    public int getContainerSize() {
        return items.size();
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
    protected Component getDefaultName() {
        return Component.translatable("container." + StrataIndustria.MOD_ID + ".millstone");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new MillstoneMenu(id, inventory, worldPosition, this, data);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(in, items);
        progress = in.getFloatOr("progress", 0.0f);
        kinetic.load(in);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        ContainerHelper.saveAllItems(out, items);
        out.putFloat("progress", progress);
        kinetic.save(out);
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
