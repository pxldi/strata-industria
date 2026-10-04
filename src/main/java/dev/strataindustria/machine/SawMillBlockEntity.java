package dev.strataindustria.machine;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.ceramics.MoldType;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.material.Metal;
import dev.strataindustria.metal.ModToolMaterials;
import dev.strataindustria.power.KineticConsumer;
import dev.strataindustria.power.KineticNetworks;
import dev.strataindustria.power.KineticState;
import dev.strataindustria.registry.ModBlockEntities;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.ModRecipes;
import dev.strataindustria.registry.ModSounds;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The saw mill (spec 8.2): a metal saw blade on a shaft. Each cut wears the blade by one; a blade
 * lasts as long as a saw of its metal. Logs give six planks and a strip of bark.
 */
public class SawMillBlockEntity extends BaseContainerBlockEntity implements KineticConsumer, WorldlyContainer {
    public static final int INPUT = 0, BLADE = 1, OUTPUT = 2, EXTRA = 3;
    public static final int IMPACT = 4, MIN_SPEED = 8;
    public static final int DATA_PROGRESS = 0, DATA_STATUS = 1, DATA_COUNT = 2;

    public enum Status {
        EMPTY, WORKING, NOT_TURNING, TOO_SLOW, NO_RECIPE, OUTPUT_FULL, NO_BLADE;

        public String key() {
            return StrataIndustria.MOD_ID + ".machine." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private static final int[] TOP_AND_SIDES = {INPUT};
    private static final int[] BOTTOM = {OUTPUT, EXTRA};

    private NonNullList<ItemStack> items = NonNullList.withSize(4, ItemStack.EMPTY);
    private final KineticState kinetic = new KineticState();
    private float progress;
    private int ticks = SawingRecipe.DEFAULT_TICKS;
    private Status status = Status.EMPTY;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> Math.round(progress / Math.max(1, ticks) * 1000);
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

    public SawMillBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SAW_MILL.get(), pos, state);
    }

    /** How many cuts a saw blade lasts: the durability of a saw of its metal, or 0 if it is no blade. */
    public static int bladeLife(ItemStack stack) {
        for (Metal metal : Metal.values()) {
            if (!metal.isToolMetal() || !metal.toolTypes().contains(MoldType.SAW_BLADE)) continue;
            if (stack.is(ModItems.head(metal, MoldType.SAW_BLADE))) return ModToolMaterials.of(metal).durability();
        }
        return 0;
    }

    public static Optional<RecipeHolder<SawingRecipe>> recipeFor(Level level, ItemStack stack) {
        if (stack.isEmpty() || !(level instanceof ServerLevel server)) return Optional.empty();
        return server.recipeAccess().getRecipeFor(ModRecipes.SAWING.get(), new SingleRecipeInput(stack), level);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SawMillBlockEntity mill) {
        Status before = mill.status;
        mill.status = mill.work((ServerLevel) level);
        boolean active = mill.status == Status.WORKING;
        if (state.getValue(SawMillBlock.ACTIVE) != active) level.setBlock(pos, state.setValue(SawMillBlock.ACTIVE, active), Block.UPDATE_CLIENTS);
        if (mill.status != before) mill.setChanged();
    }

    private Status work(ServerLevel level) {
        ItemStack input = items.get(INPUT);
        if (input.isEmpty()) {
            progress = 0;
            return Status.EMPTY;
        }
        Optional<RecipeHolder<SawingRecipe>> holder = recipeFor(level, input);
        if (holder.isEmpty()) return Status.NO_RECIPE;
        ItemStack blade = items.get(BLADE);
        if (bladeLife(blade) <= 0) return Status.NO_BLADE;
        float rpm = kinetic.rpm();
        if (rpm <= 0) return Status.NOT_TURNING;
        if (rpm < MIN_SPEED) return Status.TOO_SLOW;
        SawingRecipe recipe = holder.get().value();
        ItemStack result = recipe.assemble(new SingleRecipeInput(input));
        ItemStack extra = recipe.extraResult();
        if (!fits(items.get(OUTPUT), result) || !fits(items.get(EXTRA), extra)) return Status.OUTPUT_FULL;
        ticks = recipe.ticks();
        progress += rpm / 16.0f;
        if (level.getGameTime() % 16 == 0) {
            level.playSound(null, worldPosition, ModSounds.SAW_MILL_SAW.get(), SoundSource.BLOCKS, 0.5f, 0.9f + level.getRandom().nextFloat() * 0.2f);
        }
        if (level.getGameTime() % 4 == 0) {
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, input.getItem()), worldPosition.getX() + 0.5,
                    worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, 2, 0.1, 0.05, 0.3, 0.04);
        }
        if (progress < ticks) {
            setChanged();
            return Status.WORKING;
        }
        progress = 0;
        input.shrink(1);
        put(OUTPUT, result);
        put(EXTRA, extra);
        wear(level, blade);
        Journal.awardNear(level, worldPosition, Journal.SAW_MILL);
        setChanged();
        return Status.WORKING;
    }

    /** One cut's worth of wear. Blades carry their wear as item damage, set up when first used. */
    private void wear(ServerLevel level, ItemStack blade) {
        if (!blade.has(DataComponents.MAX_DAMAGE)) {
            // A worn blade cannot stack, so it carries its own stack size of one.
            blade.set(DataComponents.MAX_STACK_SIZE, 1);
            blade.set(DataComponents.MAX_DAMAGE, bladeLife(blade));
            blade.set(DataComponents.DAMAGE, 0);
        }
        int damage = blade.getDamageValue() + 1;
        if (damage >= blade.getMaxDamage()) {
            items.set(BLADE, ItemStack.EMPTY);
            level.playSound(null, worldPosition, ModSounds.SAW_MILL_BLADE_BREAK.get(), SoundSource.BLOCKS, 0.8f, 1.0f);
        } else {
            blade.setDamageValue(damage);
        }
    }

    private static boolean fits(ItemStack slot, ItemStack add) {
        return add.isEmpty() || slot.isEmpty()
                || ItemStack.isSameItemSameComponents(slot, add) && slot.getCount() + add.getCount() <= slot.getMaxStackSize();
    }

    private void put(int slot, ItemStack add) {
        if (add.isEmpty()) return;
        ItemStack current = items.get(slot);
        if (current.isEmpty()) items.set(slot, add.copy());
        else current.grow(add.getCount());
    }

    public Status status() {
        return status;
    }

    // ------------------------------------------------------------------ kinetics

    /** Shaft input from the back, the sides and below; the front shows the blade and the top takes logs. */
    @Override
    public boolean connects(Direction side) {
        return side != Direction.UP && side != getBlockState().getValue(SawMillBlock.FACING);
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
        return slot == OUTPUT || slot == EXTRA;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            // Only what the saw can cut, so a hopper cannot jam it with anything else.
            case INPUT -> level == null || level.isClientSide() || recipeFor(level, stack).isPresent();
            case BLADE -> bladeLife(stack) > 0;
            default -> false;
        };
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
        return Component.translatable("container." + StrataIndustria.MOD_ID + ".saw_mill");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new SawMillMenu(id, inventory, worldPosition, this, data);
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
