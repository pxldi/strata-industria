package dev.strataindustria.smithing;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.heat.Heat;
import dev.strataindustria.material.Metal;
import dev.strataindustria.metal.Alloy;
import dev.strataindustria.metal.MetalContent;
import dev.strataindustria.metal.Melt;
import dev.strataindustria.metal.Quality;
import dev.strataindustria.registry.ModBlockEntities;
import dev.strataindustria.registry.ModDataComponents;
import dev.strataindustria.registry.ModRecipes;
import dev.strataindustria.registry.ModSounds;
import dev.strataindustria.registry.ModTags;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * An anvil (spec 9.1 and 9.2): a workpiece slot and a finished-piece slot. The work itself is stored
 * on the workpiece, so it can go back into the forge and come back half done.
 */
public class AnvilBlockEntity extends BaseContainerBlockEntity {
    public static final int INPUT = 0, OUTPUT = 1, SLOTS = 2;
    public static final int MAX_PLANS = 12;

    public static final int DATA_POSITION = 0, DATA_TARGET = 1, DATA_RECENT = 2, DATA_RULES = 5, DATA_SELECTED = 8,
            DATA_STATUS = 9, DATA_HITS = 10, DATA_WORKING = 11, DATA_COUNT = 12;

    /** What the screen shows under the bar. */
    public enum Status { EMPTY, CHOOSE, READY, TOO_COLD, NO_HAMMER, TOO_WEAK, OUTPUT_FULL, NOT_ENOUGH, NO_PLAN;
        public String key() {
            return StrataIndustria.MOD_ID + ".anvil.status." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    /** Previews of what the workpiece can become, shown as buttons in the screen. Not saved. */
    private final SimpleContainer plans = new SimpleContainer(MAX_PLANS);
    private final List<RecipeHolder<AnvilRecipe>> candidates = new ArrayList<>();
    private ItemStack candidatesFor = ItemStack.EMPTY;

    public AnvilBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ANVIL.get(), pos, state);
    }

    public SimpleContainer plans() {
        return plans;
    }

    private int tier() {
        return getBlockState().getBlock() instanceof AnvilBlock anvil ? anvil.tier() : 2;
    }

    public ItemStack input() {
        return items.get(INPUT);
    }

    /** The metal a workpiece is made of, which sets its working temperature and the anvil it needs. */
    public static Optional<Metal> metalOf(ItemStack stack) {
        return MetalContent.of(stack).flatMap(Alloy::resultOf);
    }

    /** Rebuilds the plan buttons when the workpiece changes. */
    private void refreshPlans() {
        if (!(level instanceof ServerLevel server)) return;
        ItemStack input = input();
        if (ItemStack.isSameItemSameComponents(input, candidatesFor) && input.getCount() == candidatesFor.getCount()) return;
        candidatesFor = input.copy();
        candidates.clear();
        if (!input.isEmpty()) {
            server.recipeAccess().recipeMap().getRecipesFor(ModRecipes.ANVIL.get(), new SingleRecipeInput(input), server)
                    .sorted(Comparator.comparing(h -> h.id().identifier().toString()))
                    .limit(MAX_PLANS)
                    .forEach(candidates::add);
        }
        for (int i = 0; i < MAX_PLANS; i++) {
            plans.setItem(i, i < candidates.size() ? candidates.get(i).value().result().create() : ItemStack.EMPTY);
        }
    }

    private Optional<RecipeHolder<AnvilRecipe>> selected() {
        SmithingProgress progress = input().get(ModDataComponents.SMITHING_PROGRESS.get());
        if (progress == null) return Optional.empty();
        for (RecipeHolder<AnvilRecipe> holder : candidates) if (holder.id().equals(progress.recipe())) return Optional.of(holder);
        return Optional.empty();
    }

    /** Picks plan {@code index} for the workpiece; the position and hits so far carry over. */
    public void choose(int index) {
        refreshPlans();
        if (index < 0 || index >= candidates.size()) return;
        ItemStack input = input();
        var id = candidates.get(index).id();
        SmithingProgress progress = input.get(ModDataComponents.SMITHING_PROGRESS.get());
        input.set(ModDataComponents.SMITHING_PROGRESS.get(), progress == null ? SmithingProgress.start(id) : progress.withRecipe(id));
        if (level != null) level.playSound(null, worldPosition, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3f, 1.4f);
        setChanged();
    }

    private static ItemStack hammer(Player player) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < Inventory.SELECTION_SIZE; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(ModTags.Items.HAMMERS)) return stack;
        }
        ItemStack off = player.getOffhandItem();
        return off.is(ModTags.Items.HAMMERS) ? off : ItemStack.EMPTY;
    }

    public Status status(Player player) {
        refreshPlans();
        ItemStack input = input();
        if (input.isEmpty()) return Status.EMPTY;
        if (candidates.isEmpty()) return Status.NO_PLAN;
        Optional<RecipeHolder<AnvilRecipe>> recipe = selected();
        if (recipe.isEmpty()) return Status.CHOOSE;
        Optional<Metal> metal = metalOf(input);
        if (metal.isPresent() && metal.get().tier() > tier()) return Status.TOO_WEAK;
        if (input.getCount() < recipe.get().value().count()) return Status.NOT_ENOUGH;
        if (!items.get(OUTPUT).isEmpty()) return Status.OUTPUT_FULL;
        if (level != null && metal.isPresent() && Heat.get(input, level) < metal.get().workingTemperature()) return Status.TOO_COLD;
        if (player != null && hammer(player).isEmpty()) return Status.NO_HAMMER;
        return Status.READY;
    }

    /** One hammer blow from the screen. */
    public void hit(ServerPlayer player, HitType type) {
        if (!(level instanceof ServerLevel server)) return;
        Status status = status(player);
        if (status != Status.READY) {
            if (status != Status.EMPTY) player.sendOverlayMessage(Component.translatable(status.key()));
            return;
        }
        ItemStack input = input();
        RecipeHolder<AnvilRecipe> recipe = selected().orElseThrow();
        SmithingProgress progress = input.get(ModDataComponents.SMITHING_PROGRESS.get());
        int next = progress.position() + type.delta();
        if (next < 0 || next > Smithing.MAX_POSITION) {
            player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".anvil.out_of_range"));
            return;
        }
        progress = progress.hit(type);
        input.set(ModDataComponents.SMITHING_PROGRESS.get(), progress);
        hammer(player).hurtAndBreak(1, server, player,
                broken -> server.playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 0.8f, 1.0f));

        float pitch = 0.9f + (type.delta() < 0 ? -type.delta() : type.delta()) * -0.012f + server.getRandom().nextFloat() * 0.1f;
        server.playSound(null, worldPosition, ModSounds.SMITH_HIT.get(), SoundSource.BLOCKS, 0.7f, pitch + 0.3f);
        server.sendParticles(ParticleTypes.SMALL_FLAME, worldPosition.getX() + 0.5, worldPosition.getY() + 1.02, worldPosition.getZ() + 0.5,
                3 + server.getRandom().nextInt(3), 0.12, 0.0, 0.12, 0.04);

        int target = Smithing.target(server, recipe.id(), recipe.value());
        if (Smithing.done(progress.position(), target, recipe.value().rules(), progress.recent(0), progress.recent(1), progress.recent(2))) {
            finish(server, recipe, progress, target);
        }
        setChanged();
    }

    private void finish(ServerLevel server, RecipeHolder<AnvilRecipe> recipe, SmithingProgress progress, int target) {
        ItemStack input = input();
        float temperature = Heat.get(input, server);
        int material = MetalContent.of(input.copyWithCount(1)).map(Melt::quality).orElse(0);
        int craft = Smithing.craftQuality(progress.hits(), Smithing.minHits(target, recipe.value().rules()));
        ItemStack out = recipe.value().assemble(new SingleRecipeInput(input));
        out.set(ModDataComponents.QUALITY.get(), new Quality(material, craft));
        Heat.set(out, temperature, server.getGameTime());
        input.shrink(recipe.value().count());
        if (input.isEmpty()) items.set(INPUT, ItemStack.EMPTY);
        items.set(OUTPUT, out);
        server.playSound(null, worldPosition, ModSounds.SMITH_DONE.get(), SoundSource.BLOCKS, 0.8f, 1.0f);
        server.sendParticles(ParticleTypes.LAVA, worldPosition.getX() + 0.5, worldPosition.getY() + 1.05, worldPosition.getZ() + 0.5,
                4, 0.15, 0.0, 0.15, 0.0);
    }

    /** Menu data, read fresh each sync so it follows the workpiece as it cools. */
    public ContainerData data(Player player) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                ItemStack input = input();
                SmithingProgress progress = input.get(ModDataComponents.SMITHING_PROGRESS.get());
                Optional<RecipeHolder<AnvilRecipe>> recipe = selected();
                if (index == DATA_POSITION) return progress == null ? 0 : progress.position();
                if (index == DATA_TARGET) {
                    return recipe.isPresent() && level instanceof ServerLevel server ? Smithing.target(server, recipe.get().id(), recipe.get().value()) : -1;
                }
                if (index >= DATA_RECENT && index < DATA_RECENT + 3) {
                    HitType hit = progress == null ? null : progress.recent(index - DATA_RECENT);
                    return hit == null ? -1 : hit.ordinal();
                }
                if (index >= DATA_RULES && index < DATA_RULES + 3) {
                    if (recipe.isEmpty()) return -1;
                    List<Rule> rules = recipe.get().value().rules();
                    int i = index - DATA_RULES;
                    return i < rules.size() ? rules.get(i).encode() : -1;
                }
                if (index == DATA_SELECTED) return recipe.map(candidates::indexOf).orElse(-1);
                if (index == DATA_STATUS) return status(player).ordinal();
                if (index == DATA_HITS) return progress == null ? 0 : progress.hits();
                if (index == DATA_WORKING) return metalOf(input).map(Metal::workingTemperature).orElse(0);
                return 0;
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    @Override
    public void setChanged() {
        super.setChanged();
        refreshPlans();
        // The renderer shows the pieces on the anvil face.
        if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == INPUT;
    }

    @Override
    public int getContainerSize() {
        return SLOTS;
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
        return Component.translatable("container." + StrataIndustria.MOD_ID + ".anvil");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new AnvilMenu(id, inventory, this, data(inventory.player));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        candidatesFor = ItemStack.EMPTY;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items, false);
    }
}
