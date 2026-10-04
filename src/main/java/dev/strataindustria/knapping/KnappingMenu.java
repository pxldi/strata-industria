package dev.strataindustria.knapping;

import dev.strataindustria.journal.Journal;
import dev.strataindustria.registry.ModMenus;
import dev.strataindustria.registry.ModRecipes;
import dev.strataindustria.registry.ModSounds;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * The 5x5 knapping grid. Each menu button id 0-24 strikes one cell out. The opening cost is taken on
 * the first strike, so closing before that costs nothing; closing an unfinished grid loses the
 * material. Button id 25 repeats the player's last pattern from the same kind of material. Taking the
 * result closes the screen.
 */
public class KnappingMenu extends AbstractContainerMenu {
    public static final int GRID_X = 17;
    public static final int GRID_Y = 18;
    public static final int CELL = 16;
    public static final int RESULT_X = 134;
    public static final int RESULT_Y = 50;
    public static final int INVENTORY_Y = 114;
    /** Menu button id of the repeat-last button; the grid cells are 0-24. */
    public static final int REPEAT_BUTTON = 25;
    public static final int REPEAT_X = 137;
    public static final int REPEAT_Y = 80;

    private final ItemStack material;
    private final InteractionHand hand;
    private final int[] cells = new int[GridPattern.CELLS];
    private final DataSlot started = DataSlot.standalone();
    private final DataSlot finished = DataSlot.standalone();
    /** Bit mask of the cells the repeat button leaves in place, or 0 when it cannot be used. */
    private final DataSlot repeat = DataSlot.standalone();
    private final SimpleContainer result = new SimpleContainer(1);
    private RecipeHolder<KnappingRecipe> recipe;

    public KnappingMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, ItemStack.STREAM_CODEC.decode(buf), buf.readBoolean() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
    }

    public KnappingMenu(int id, Inventory inventory, ItemStack material, InteractionHand hand) {
        super(ModMenus.KNAPPING.get(), id);
        this.material = material;
        this.hand = hand;
        java.util.Arrays.fill(cells, 1);
        for (int i = 0; i < cells.length; i++) addDataSlot(DataSlot.shared(cells, i));
        addDataSlot(started);
        addDataSlot(finished);
        addDataSlot(repeat);
        if (inventory.player instanceof ServerPlayer player) repeat.set(repeatMask(player));

        addSlot(new Slot(result, 0, RESULT_X, RESULT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player taker, ItemStack stack) {
                finish(taker);
                super.onTake(taker, stack);
            }
        });
        addStandardInventorySlots(inventory, 8, INVENTORY_Y);
    }

    public ItemStack material() {
        return material;
    }

    public boolean isKept(int cell) {
        return cells[cell] != 0;
    }

    public boolean hasStarted() {
        return started.get() != 0;
    }

    public boolean isFinished() {
        return finished.get() != 0;
    }

    /** The pattern the repeat button would cut, or 0 when this player has not knapped one from this material yet. */
    public int repeatMask() {
        return repeat.get();
    }

    private int repeatMask(ServerPlayer player) {
        if (Knapping.isClay(material) || Knapping.isWood(material)) return 0;
        int mask = KnappedPatterns.of(player).last(Knapping.isFlint(material));
        if (mask == 0 || !(player.level() instanceof ServerLevel level)) return 0;
        Optional<RecipeHolder<KnappingRecipe>> found = level.recipeAccess().getRecipeFor(ModRecipes.KNAPPING.get(), new KnappingInput(material, mask), level);
        if (found.isEmpty()) return 0;
        return player.hasInfiniteMaterials() || player.getItemInHand(hand).getCount() >= found.get().value().consume() ? mask : 0;
    }

    public int keptMask() {
        int mask = 0;
        for (int i = 0; i < cells.length; i++) if (cells[i] != 0) mask |= 1 << i;
        return mask;
    }

    @Override
    public boolean clickMenuButton(Player clicker, int id) {
        if (id == REPEAT_BUTTON) return repeatLast(clicker);
        if (id < 0 || id >= GridPattern.CELLS || cells[id] == 0 || isFinished()) return false;
        if (!start(clicker)) return false;
        cells[id] = 0;
        clicker.level().playSound(null, clicker.getX(), clicker.getY(), clicker.getZ(), Knapping.strikeSound(material),
                SoundSource.PLAYERS, 0.7f, 0.85f + clicker.getRandom().nextFloat() * 0.3f);
        updateResult(clicker);
        return true;
    }

    /** Takes the opening cost on the first strike. Returns false, closing the grid, if the material is gone. */
    private boolean start(Player clicker) {
        if (hasStarted()) return true;
        ItemStack held = clicker.getItemInHand(hand);
        int cost = Knapping.openingCost(material);
        if (!ItemStack.isSameItem(held, material) || held.getCount() < cost) {
            clicker.closeContainer();
            return false;
        }
        if (!clicker.hasInfiniteMaterials()) held.shrink(cost);
        started.set(1);
        return true;
    }

    /** The repeat button: cuts the player's last pattern from this material in one go, for the usual cost. */
    private boolean repeatLast(Player clicker) {
        int mask = repeat.get();
        if (mask == 0 || hasStarted() || isFinished() || !(clicker instanceof ServerPlayer)) return false;
        if (!start(clicker)) return false;
        for (int i = 0; i < cells.length; i++) cells[i] = (mask >> i & 1) != 0 ? 1 : 0;
        clicker.level().playSound(null, clicker.getX(), clicker.getY(), clicker.getZ(), ModSounds.KNAP_REPEAT.get(),
                SoundSource.PLAYERS, 0.8f, 1.0f);
        updateResult(clicker);
        return true;
    }

    private void updateResult(Player clicker) {
        if (!(clicker.level() instanceof ServerLevel level)) return;
        KnappingInput input = new KnappingInput(material, keptMask());
        Optional<RecipeHolder<KnappingRecipe>> found = level.recipeAccess().getRecipeFor(ModRecipes.KNAPPING.get(), input, level);
        recipe = found.filter(r -> extraCost(r.value()) <= clicker.getItemInHand(hand).getCount()
                || clicker.hasInfiniteMaterials()).orElse(null);
        result.setItem(0, recipe == null ? ItemStack.EMPTY : recipe.value().assemble(input));
        broadcastChanges();
    }

    private int extraCost(KnappingRecipe r) {
        return Math.max(0, r.consume() - Knapping.openingCost(material));
    }

    private void finish(Player taker) {
        if (taker.level().isClientSide() || isFinished()) return;
        if (recipe != null && !taker.hasInfiniteMaterials()) {
            int extra = extraCost(recipe.value());
            if (extra > 0) taker.getItemInHand(hand).shrink(extra);
        }
        finished.set(1);
        if (taker instanceof ServerPlayer player && recipe != null && !Knapping.isClay(material) && !Knapping.isWood(material)) {
            KnappedPatterns.of(player).set(Knapping.isFlint(material), keptMask());
        }
        if (taker instanceof ServerPlayer player && !Knapping.isWood(material)) Journal.award(player, Knapping.isClay(material) ? Journal.CLAY_FORMING : Journal.KNAP);
        taker.level().playSound(null, taker.getX(), taker.getY(), taker.getZ(), Knapping.finishSound(material),
                SoundSource.PLAYERS, 0.8f, 1.0f);
        broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player taker, int index) {
        if (index != 0) return ItemStack.EMPTY;
        Slot slot = slots.get(0);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (!moveItemStackTo(stack, 1, slots.size(), true)) return ItemStack.EMPTY;
        slot.setByPlayer(ItemStack.EMPTY);
        slot.onTake(taker, copy);
        return ItemStack.EMPTY;
    }

    @Override
    public void removed(Player closer) {
        super.removed(closer);
        // The result only exists while the grid is open; it is never handed back on close.
        result.removeItemNoUpdate(0);
    }

    @Override
    public boolean stillValid(Player viewer) {
        return viewer.isAlive();
    }
}
