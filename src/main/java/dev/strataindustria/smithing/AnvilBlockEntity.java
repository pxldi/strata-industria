package dev.strataindustria.smithing;

import dev.strataindustria.Config;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.heat.Heat;
import dev.strataindustria.material.Metal;
import dev.strataindustria.metal.Alloy;
import dev.strataindustria.metal.MetalContent;
import dev.strataindustria.metal.Melt;
import dev.strataindustria.metal.Quality;
import dev.strataindustria.registry.ModBlockEntities;
import dev.strataindustria.registry.ModDataComponents;
import dev.strataindustria.registry.ModItems;
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
    public static final int INPUT = 0, OUTPUT = 1, SECOND = 2, FLUX = 3, PATTERN = 4, SLOTS = 5;
    public static final int MAX_PLANS = 12;

    public static final int DATA_POSITION = 0, DATA_TARGET = 1, DATA_RECENT = 2, DATA_RULES = 5, DATA_SELECTED = 8,
            DATA_STATUS = 9, DATA_HITS = 10, DATA_WORKING = 11, DATA_WELD = 12, DATA_WELD_TEMP = 13, DATA_QUICK = 14, DATA_COUNT = 15;

    /** What the screen shows under the bar. */
    public enum Status { EMPTY, CHOOSE, READY, TOO_COLD, NO_HAMMER, TOO_WEAK, OUTPUT_FULL, NOT_ENOUGH, NO_PLAN;
        public String key() {
            return StrataIndustria.MOD_ID + ".anvil.status." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** Why the Weld button is greyed out, or READY (tier 3 spec 9.4). NONE when there is nothing to weld. */
    public enum WeldStatus { NONE, READY, NO_RECIPE, TOO_WEAK, OUTPUT_FULL, TOO_COLD, NO_FLUX, NO_HAMMER;
        public String key() {
            return StrataIndustria.MOD_ID + ".anvil.weld." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private NonNullList<ItemStack> items = NonNullList.withSize(slotCount(), ItemStack.EMPTY);
    /** Previews of what the workpiece can become, shown as buttons in the screen. Not saved. */
    private final SimpleContainer plans = new SimpleContainer(MAX_PLANS);
    private final List<RecipeHolder<AnvilRecipe>> candidates = new ArrayList<>();
    private ItemStack candidatesFor = ItemStack.EMPTY;

    public AnvilBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.ANVIL.get(), pos, state);
    }

    /** For machines with an anvil built in, such as the steam hammer. */
    protected AnvilBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** How many slots it has; machines built around an anvil add their own after {@link #SLOTS}. Must be a constant. */
    protected int slotCount() {
        return SLOTS;
    }

    public SimpleContainer plans() {
        return plans;
    }

    /** How high the working face sits in the block, where the sparks fly from. */
    protected double faceHeight() {
        return 1.0;
    }

    protected int tier() {
        return getBlockState().getBlock() instanceof AnvilBlock anvil ? anvil.tier() : 2;
    }

    public ItemStack input() {
        return items.get(INPUT);
    }

    /** The metal a workpiece is made of, which sets its working temperature and the anvil it needs. */
    public static Optional<Metal> metalOf(ItemStack stack) {
        return MetalContent.of(stack).flatMap(Alloy::resultOf);
    }

    /** Tier 3 spec 5.4: a raw bloom needs 1000 °C, hotter than the wrought iron it becomes. */
    public static final int RAW_BLOOM_WORKING_TEMPERATURE = 1000;

    public static int workingTemperature(ItemStack stack) {
        if (stack.has(ModDataComponents.BLOOM_CONTENTS.get())) return RAW_BLOOM_WORKING_TEMPERATURE;
        return metalOf(stack).map(Metal::workingTemperature).orElse(0);
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

    public Status status(@org.jspecify.annotations.Nullable Player player) {
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
        if (level != null && metal.isPresent() && Heat.get(input, level) < workingTemperature(input)) return Status.TOO_COLD;
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
        if (!inRange(type)) {
            player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".anvil.out_of_range"));
            return;
        }
        hammer(player).hurtAndBreak(1, server, player,
                broken -> server.playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 0.8f, 1.0f));
        strike(server, type, player);
    }

    /** Whether this player has finished the selected plan by hand before, which unlocks the Quick button. */
    public boolean knowsPlan(@org.jspecify.annotations.Nullable Player player) {
        if (!(player instanceof ServerPlayer server)) return false;
        return selected().map(h -> SmithedRecipes.of(server).has(h.id().identifier())).orElse(false);
    }

    /**
     * The Quick button: finishes a plan the player has already smithed by hand, at a normal craft quality.
     * It costs what the shortest run would cost: hammer wear, and for iron the heat of every blow.
     */
    public void quick(ServerPlayer player) {
        if (!(level instanceof ServerLevel server)) return;
        Status status = status(player);
        if (status != Status.READY) {
            if (status != Status.EMPTY) player.sendOverlayMessage(Component.translatable(status.key()));
            return;
        }
        if (!knowsPlan(player)) {
            player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".anvil.quick.unknown"));
            return;
        }
        RecipeHolder<AnvilRecipe> recipe = selected().orElseThrow();
        int target = Smithing.target(server, recipe.id(), recipe.value());
        int blows = Math.max(1, Smithing.minHits(target, recipe.value().rules()));
        hammer(player).hurtAndBreak(blows, server, player,
                broken -> server.playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 0.8f, 1.0f));
        ItemStack input = input();
        if (metalOf(input).map(m -> m.tier() >= 3).orElse(false)) {
            Heat.set(input, Heat.get(input, server) - Config.SMITHING_HIT_COOLING.get() * blows, server.getGameTime());
        }
        server.playSound(null, worldPosition, ModSounds.SMITH_QUICK.get(), SoundSource.BLOCKS, 0.8f, 1.0f);
        SmithingProgress progress = input.get(ModDataComponents.SMITHING_PROGRESS.get());
        finish(server, recipe, progress, target, player, true);
        dev.strataindustria.journal.Journal.award(player, dev.strataindustria.journal.Journal.QUICK_SMITH);
        setChanged();
    }

    /** What a blow from a machine did (tier 3 spec 8.4). */
    public enum MachineHit { STRUCK, DONE, TOO_COLD, REFUSED }

    /**
     * One blow from a trip hammer: the same rules as a hand blow, but no hammer is needed or worn.
     */
    public MachineHit machineHit(HitType type) {
        if (!(level instanceof ServerLevel server)) return MachineHit.REFUSED;
        Status status = status(null);
        if (status == Status.TOO_COLD) return MachineHit.TOO_COLD;
        if (status != Status.READY || !inRange(type)) return MachineHit.REFUSED;
        return strike(server, type, null) ? MachineHit.DONE : MachineHit.STRUCK;
    }

    /** Puts the workpiece on recipe {@code id}, as if its plan had been picked. Returns whether that plan exists for it. */
    public boolean select(net.minecraft.resources.ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> id) {
        refreshPlans();
        for (RecipeHolder<AnvilRecipe> holder : candidates) {
            if (!holder.id().equals(id)) continue;
            ItemStack input = input();
            SmithingProgress progress = input.get(ModDataComponents.SMITHING_PROGRESS.get());
            if (progress == null || !progress.recipe().equals(id)) {
                input.set(ModDataComponents.SMITHING_PROGRESS.get(), progress == null ? SmithingProgress.start(id) : progress.withRecipe(id));
                setChanged();
            }
            return true;
        }
        return false;
    }

    private boolean inRange(HitType type) {
        SmithingProgress progress = input().get(ModDataComponents.SMITHING_PROGRESS.get());
        if (progress == null) return false;
        int next = progress.position() + type.delta();
        return next >= 0 && next <= Smithing.MAX_POSITION;
    }

    /** Lands one blow on the selected plan. Returns whether it finished the piece. */
    private boolean strike(ServerLevel server, HitType type, @org.jspecify.annotations.Nullable ServerPlayer player) {
        ItemStack input = input();
        RecipeHolder<AnvilRecipe> recipe = selected().orElseThrow();
        SmithingProgress progress = input.get(ModDataComponents.SMITHING_PROGRESS.get()).hit(type);
        input.set(ModDataComponents.SMITHING_PROGRESS.get(), progress);
        // Tier 3 spec 9.2: iron loses heat to every blow, so careless work means a trip back to the forge.
        if (metalOf(input).map(m -> m.tier() >= 3).orElse(false)) {
            Heat.set(input, Heat.get(input, server) - Config.SMITHING_HIT_COOLING.get(), server.getGameTime());
        }

        float pitch = 0.9f + (type.delta() < 0 ? -type.delta() : type.delta()) * -0.012f + server.getRandom().nextFloat() * 0.1f;
        server.playSound(null, worldPosition, hitSound(input), SoundSource.BLOCKS, 0.7f, pitch + 0.3f);
        server.sendParticles(ParticleTypes.SMALL_FLAME, worldPosition.getX() + 0.5, worldPosition.getY() + faceHeight() + 0.02, worldPosition.getZ() + 0.5,
                3 + server.getRandom().nextInt(3), 0.12, 0.0, 0.12, 0.04);

        int target = Smithing.target(server, recipe.id(), recipe.value());
        boolean done = Smithing.done(progress.position(), target, recipe.value().rules(), progress.recent(0), progress.recent(1), progress.recent(2));
        if (done) finish(server, recipe, progress, target, player, false);
        setChanged();
        return done;
    }

    /** Bronze rings; wrought iron rings lower; a raw bloom only thuds (tier 3 spec 20.6). */
    private static net.minecraft.sounds.SoundEvent hitSound(ItemStack input) {
        if (input.has(ModDataComponents.BLOOM_CONTENTS.get())) return ModSounds.RAW_BLOOM_HIT.get();
        if (metalOf(input).filter(m -> m == Metal.WROUGHT_IRON).isPresent()) return ModSounds.WROUGHT_IRON_HIT.get();
        return ModSounds.SMITH_HIT.get();
    }

    private void finish(ServerLevel server, RecipeHolder<AnvilRecipe> recipe, SmithingProgress progress, int target,
            @org.jspecify.annotations.Nullable ServerPlayer player, boolean quick) {
        ItemStack input = input();
        if (input.has(ModDataComponents.BLOOM_CONTENTS.get())) {
            for (ServerPlayer near : server.getEntitiesOfClass(ServerPlayer.class, new net.minecraft.world.phys.AABB(worldPosition).inflate(8))) {
                dev.strataindustria.journal.Journal.award(near, dev.strataindustria.journal.Journal.BLOOM_REFINED);
            }
        }
        float temperature = Heat.get(input, server);
        int material = MetalContent.of(input.copyWithCount(1)).map(Melt::quality).orElse(0);
        int craft = quick ? Smithing.QUICK_CRAFT : Smithing.craftQuality(progress.hits(), Smithing.minHits(target, recipe.value().rules()));
        ItemStack out = recipe.value().assemble(new SingleRecipeInput(input));
        out.set(ModDataComponents.QUALITY.get(), new Quality(material, craft));
        Heat.set(out, temperature, server.getGameTime());
        input.shrink(recipe.value().count());
        if (input.isEmpty()) items.set(INPUT, ItemStack.EMPTY);
        items.set(OUTPUT, out);
        if (player != null) SmithedRecipes.of(player).add(recipe.id().identifier());
        if (!quick) recordPattern(server, recipe, progress, target, craft, out);
        server.playSound(null, worldPosition, ModSounds.SMITH_DONE.get(), SoundSource.BLOCKS, 0.8f, 1.0f);
        server.sendParticles(ParticleTypes.LAVA, worldPosition.getX() + 0.5, worldPosition.getY() + faceHeight() + 0.05, worldPosition.getZ() + 0.5,
                4, 0.15, 0.0, 0.15, 0.0);
    }

    // ------------------------------------------------------------------ smithing patterns (tier 3 spec 9.5)

    public static boolean isBlankPattern(ItemStack stack) {
        return stack.is(ModItems.SMITHING_PATTERN.get()) && !stack.has(ModDataComponents.SMITHING_PATTERN.get());
    }

    /** A blank pattern in the pattern slot takes down the hits that just finished a piece. */
    private void recordPattern(ServerLevel server, RecipeHolder<AnvilRecipe> recipe, SmithingProgress progress, int target, int craft,
            ItemStack out) {
        ItemStack pattern = items.get(PATTERN);
        // A run longer than the remembered history cannot be replayed, so it leaves the pattern blank.
        if (!isBlankPattern(pattern) || progress.history().isEmpty() || progress.history().size() != progress.hits()) return;
        pattern.set(ModDataComponents.SMITHING_PATTERN.get(), new SmithingPattern(recipe.id(),
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(out.getItem()), target, progress.history(), craft));
        server.playSound(null, worldPosition, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.8f, 1.1f);
        for (ServerPlayer player : server.getEntitiesOfClass(ServerPlayer.class, new net.minecraft.world.phys.AABB(worldPosition).inflate(8))) {
            dev.strataindustria.journal.Journal.award(player, dev.strataindustria.journal.Journal.PATTERN_RECORDED);
        }
    }

    // ------------------------------------------------------------------ welding (tier 3 spec 9.4)

    public static boolean isFlux(ItemStack stack) {
        return stack.is(ModItems.FLUX.get());
    }

    /** Two partial blooms with at most a full bloom between them press into one. */
    private static boolean bloomMerge(ItemStack a, ItemStack b) {
        Melt x = a.get(ModDataComponents.BLOOM_CONTENTS.get()), y = b.get(ModDataComponents.BLOOM_CONTENTS.get());
        return x != null && y != null && x.total() + y.total() <= dev.strataindustria.bloomery.BloomeryBlockEntity.BLOOM_UNITS;
    }

    private Optional<ItemStack> weldResult() {
        ItemStack a = input(), b = items.get(SECOND);
        if (a.isEmpty() || b.isEmpty() || !(level instanceof ServerLevel server)) return Optional.empty();
        if (bloomMerge(a, b)) {
            ItemStack bloom = new ItemStack(ModItems.RAW_BLOOM.get());
            bloom.set(ModDataComponents.BLOOM_CONTENTS.get(),
                    a.get(ModDataComponents.BLOOM_CONTENTS.get()).plus(b.get(ModDataComponents.BLOOM_CONTENTS.get())));
            return Optional.of(bloom);
        }
        WeldingInput weld = new WeldingInput(a.copyWithCount(1), b.copyWithCount(1));
        return server.recipeAccess().recipeMap().getRecipesFor(ModRecipes.WELDING.get(), weld, server)
                .findFirst().map(h -> h.value().assemble(weld));
    }

    /** The higher of the two pieces' welding temperatures. */
    public static int weldingTemperature(ItemStack a, ItemStack b) {
        return Math.max(metalOf(a).map(Metal::weldingTemperature).orElse(0), metalOf(b).map(Metal::weldingTemperature).orElse(0));
    }

    public WeldStatus weldStatus(Player player) {
        ItemStack a = input(), b = items.get(SECOND);
        if (a.isEmpty() || b.isEmpty()) return WeldStatus.NONE;
        if (weldResult().isEmpty()) return WeldStatus.NO_RECIPE;
        int metalTier = Math.max(metalOf(a).map(Metal::tier).orElse(0), metalOf(b).map(Metal::tier).orElse(0));
        if (metalTier > tier()) return WeldStatus.TOO_WEAK;
        if (!items.get(OUTPUT).isEmpty()) return WeldStatus.OUTPUT_FULL;
        int needed = weldingTemperature(a, b);
        if (level != null && (Heat.get(a, level) < needed || Heat.get(b, level) < needed)) return WeldStatus.TOO_COLD;
        if (!isFlux(items.get(FLUX))) return WeldStatus.NO_FLUX;
        if (player != null && hammer(player).isEmpty()) return WeldStatus.NO_HAMMER;
        return WeldStatus.READY;
    }

    /** The Weld button: joins the two pieces, using one flux and one hammer blow. */
    public void weld(ServerPlayer player) {
        if (!(level instanceof ServerLevel server)) return;
        WeldStatus status = weldStatus(player);
        if (status != WeldStatus.READY) {
            if (status != WeldStatus.NONE) {
                player.sendOverlayMessage(Component.translatable(status.key(), weldingTemperature(input(), items.get(SECOND))));
            }
            return;
        }
        ItemStack a = input(), b = items.get(SECOND);
        ItemStack out = weldResult().orElseThrow();
        long now = server.getGameTime();
        // Spec 4.5: material by units, the better craft part of the two, never worse for welding.
        Melt ma = MetalContent.of(a.copyWithCount(1)).orElse(Melt.EMPTY), mb = MetalContent.of(b.copyWithCount(1)).orElse(Melt.EMPTY);
        int material = ma.plus(mb).quality();
        int craft = Math.max(craftOf(a), craftOf(b));
        if (!out.has(ModDataComponents.BLOOM_CONTENTS.get())) out.set(ModDataComponents.QUALITY.get(), new Quality(material, craft));
        Heat.set(out, Math.max(Heat.get(a, now), Heat.get(b, now)), now);
        a.shrink(1);
        b.shrink(1);
        items.get(FLUX).shrink(1);
        if (a.isEmpty()) items.set(INPUT, ItemStack.EMPTY);
        if (b.isEmpty()) items.set(SECOND, ItemStack.EMPTY);
        items.set(OUTPUT, out);
        hammer(player).hurtAndBreak(1, server, player,
                broken -> server.playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 0.8f, 1.0f));
        server.playSound(null, worldPosition, ModSounds.ANVIL_WELD.get(), SoundSource.BLOCKS, 0.9f, 0.95f + server.getRandom().nextFloat() * 0.1f);
        server.sendParticles(ParticleTypes.ELECTRIC_SPARK, worldPosition.getX() + 0.5, worldPosition.getY() + 1.05, worldPosition.getZ() + 0.5,
                10, 0.15, 0.05, 0.15, 0.15);
        server.sendParticles(ParticleTypes.SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1.05, worldPosition.getZ() + 0.5,
                4, 0.1, 0.0, 0.1, 0.01);
        setChanged();
    }

    private static int craftOf(ItemStack stack) {
        Quality quality = stack.get(ModDataComponents.QUALITY.get());
        return quality == null ? 0 : quality.craft();
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
                if (index == DATA_WORKING) return workingTemperature(input);
                if (index == DATA_WELD) return weldStatus(player).ordinal();
                if (index == DATA_WELD_TEMP) return weldingTemperature(input, items.get(SECOND));
                if (index == DATA_QUICK) return knowsPlan(player) ? 1 : 0;
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
        return switch (slot) {
            case INPUT, SECOND -> true;
            case FLUX -> isFlux(stack);
            case PATTERN -> stack.is(ModItems.SMITHING_PATTERN.get());
            default -> false;
        };
    }

    @Override
    public int getContainerSize() {
        return slotCount();
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
        items = NonNullList.withSize(slotCount(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        candidatesFor = ItemStack.EMPTY;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items, false);
    }
}
