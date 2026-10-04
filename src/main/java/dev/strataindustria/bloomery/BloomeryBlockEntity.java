package dev.strataindustria.bloomery;

import dev.strataindustria.Config;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.heat.Heat;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.material.Metal;
import dev.strataindustria.metal.Melt;
import dev.strataindustria.metal.MetalContent;
import dev.strataindustria.registry.ModBlockEntities;
import dev.strataindustria.registry.ModDataComponents;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.ModSounds;
import dev.strataindustria.registry.ModTags;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * The bloomery (spec 5): charge iron-bearing items and charcoal, light it, and after a long burn
 * pull hot raw blooms out with a pickaxe. Everything that matters lives here; the block only routes
 * clicks.
 */
public class BloomeryBlockEntity extends BlockEntity implements MenuProvider {
    public static final int ORE_PER_LEVEL = 8;
    public static final int CHARCOAL_PER_LEVEL = 4;
    public static final float BASE_TEMPERATURE = 1100.0f;
    public static final float PER_CHIMNEY = 50.0f;
    public static final float PER_BELLOWS = 100.0f;
    public static final int MAX_BELLOWS = 2;
    /** Below this a run makes no bloom (spec 5.3). */
    public static final float MIN_TEMPERATURE = 1200.0f;
    /** At and above this the yield is full. */
    public static final float FULL_YIELD_TEMPERATURE = 1300.0f;
    public static final float LOW_YIELD = 0.85f;

    /** The configured bloom thresholds (config {@code bloomery.*}); the constants are their defaults. */
    public static float minTemperature() {
        return dev.strataindustria.Config.BLOOMERY_MIN_TEMPERATURE.getAsInt();
    }

    public static float fullYieldTemperature() {
        return Math.max(minTemperature(), dev.strataindustria.Config.BLOOMERY_FULL_YIELD_TEMPERATURE.getAsInt());
    }

    public static float lowYield() {
        return (float) dev.strataindustria.Config.BLOOMERY_LOW_YIELD.getAsDouble();
    }
    public static final int BLOOM_UNITS = 100;
    public static final int MIN_PARTIAL_UNITS = 10;
    static final float HEAT_PER_TICK = 5.0f / 20;
    static final float COOL_PER_TICK = 4.0f / 20;
    private static final int CHARGE_SLOTS = (ORE_PER_LEVEL + CHARCOAL_PER_LEVEL) * (BloomeryStructure.MAX_CHIMNEY + 1);

    public enum Status { INCOMPLETE, EMPTY, CHARGED, NEEDS_CHARCOAL, HEATING, TOO_COOL, BURNING, READY;
        public String key() {
            return StrataIndustria.MOD_ID + ".bloomery.status." + name().toLowerCase(Locale.ROOT);
        }
    }

    public static final int DATA_STATUS = 0, DATA_TEMPERATURE = 1, DATA_PROGRESS = 2, DATA_ORE = 3, DATA_CHARCOAL = 4,
            DATA_CHIMNEY = 5, DATA_PROBLEM = 6, DATA_DX = 7, DATA_DY = 8, DATA_DZ = 9, DATA_YIELD = 10, DATA_BLOOMS = 11,
            DATA_TARGET = 12, DATA_BELLOWS = 13, DATA_COUNT = 14;

    /** The charge in the order it went in, so the last stack can come back out. */
    private NonNullList<ItemStack> charge = NonNullList.withSize(CHARGE_SLOTS, ItemStack.EMPTY);
    private float temperature = Heat.AMBIENT;
    /** Share of the burn done, 0 to 1. It survives the fire going out, so a relit run resumes (spec 5.3). */
    private float progress;
    /** Set when a lit run settled below 1200 °C, until the next lighting. */
    private boolean tooCool;
    private int fullBlooms;
    private int partialUnits;
    private int slag;
    private BloomeryStructure.Result structure = new BloomeryStructure.Result(0, BloomeryStructure.Problem.NEEDS_BRICK, BlockPos.ZERO);
    private boolean announced;

    public BloomeryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BLOOMERY.get(), pos, state);
    }

    // ------------------------------------------------------------------ what goes in

    /** Any item that holds wrought iron, except a bloom (spec 5.2). */
    public static boolean isOre(ItemStack stack) {
        if (stack.isEmpty() || stack.is(ModItems.RAW_BLOOM.get())) return false;
        return MetalContent.of(stack).map(melt -> melt.units().getOrDefault(Metal.WROUGHT_IRON, 0) > 0).orElse(false);
    }

    public static boolean isFuel(ItemStack stack) {
        return stack.is(ModTags.Items.BLOOMERY_FUEL);
    }

    private Direction facing() {
        return getBlockState().getValue(BloomeryBlock.FACING);
    }

    public BloomeryStructure.Result checkStructure() {
        if (level != null) structure = BloomeryStructure.check(level, worldPosition, facing());
        return structure;
    }

    public int oreCount() {
        int n = 0;
        for (ItemStack stack : charge) if (!stack.isEmpty() && !isFuel(stack)) n += stack.getCount();
        return n;
    }

    public int charcoalCount() {
        int n = 0;
        for (ItemStack stack : charge) if (isFuel(stack)) n += stack.getCount();
        return n;
    }

    /** One charcoal per two ore items, rounded up. */
    public int charcoalNeeded() {
        return Math.max(0, (oreCount() + 1) / 2 - charcoalCount());
    }

    private boolean lit() {
        return getBlockState().getValue(BloomeryBlock.LIT);
    }

    public boolean hasBlooms() {
        return fullBlooms > 0 || partialUnits > 0;
    }

    /** Puts as much of {@code stack} in as fits. Returns how many went in, or -1 with a message. */
    public int insert(ItemStack stack, Player player) {
        boolean fuel = isFuel(stack);
        if (!fuel && !isOre(stack)) {
            if (player != null) {
                String why = stack.is(ModItems.LIGNITE.get()) ? "lignite" : "not_iron";
                player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".bloomery." + why));
            }
            return -1;
        }
        if (lit() || hasBlooms()) {
            if (player != null) player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".bloomery.busy"));
            return -1;
        }
        BloomeryStructure.Result result = checkStructure();
        if (!result.complete()) {
            if (player != null) player.sendOverlayMessage(Component.translatable(Status.INCOMPLETE.key()));
            return -1;
        }
        int room = fuel ? CHARCOAL_PER_LEVEL * result.levels() - charcoalCount() : ORE_PER_LEVEL * result.levels() - oreCount();
        int amount = Math.min(room, stack.getCount());
        if (amount <= 0) {
            if (player != null) player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".bloomery.full"));
            return -1;
        }
        // Top up the last stack, then start new ones, never past the item's own stack size.
        int last = lastFilled();
        int left = amount;
        if (last >= 0 && ItemStack.isSameItemSameComponents(charge.get(last), stack)) {
            int add = Math.min(left, charge.get(last).getMaxStackSize() - charge.get(last).getCount());
            charge.get(last).grow(Math.max(0, add));
            left -= Math.max(0, add);
        }
        for (int slot = last + 1; left > 0 && slot < charge.size(); slot++) {
            int add = Math.min(left, stack.getMaxStackSize());
            charge.set(slot, stack.copyWithCount(add));
            left -= add;
        }
        amount -= left;
        if (amount <= 0) return -1;
        tooCool = false;
        progress = 0;
        setChanged();
        if (level != null) {
            level.playSound(null, worldPosition, ModSounds.BLOOMERY_CHARGE.get(), SoundSource.BLOCKS, 0.8f,
                    0.9f + level.getRandom().nextFloat() * 0.2f);
        }
        return amount;
    }

    private int lastFilled() {
        for (int i = charge.size() - 1; i >= 0; i--) if (!charge.get(i).isEmpty()) return i;
        return -1;
    }

    /** Sneak and empty hand before lighting: the stack that went in last comes back. */
    public ItemStack takeLast() {
        if (lit()) return ItemStack.EMPTY;
        int last = lastFilled();
        if (last < 0) return ItemStack.EMPTY;
        ItemStack out = charge.get(last);
        charge.set(last, ItemStack.EMPTY);
        progress = 0;
        setChanged();
        return out;
    }

    // ------------------------------------------------------------------ running

    /** Bellows that blow into this bloomery while turned by a mechanism (spec 8.3). */
    public int poweredBellows() {
        if (level == null) return 0;
        int count = 0;
        BlockPos chamber = BloomeryStructure.chamber(worldPosition, facing());
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos wall = chamber.relative(side);
            if (wall.equals(worldPosition)) continue;
            BlockPos outside = wall.relative(side);
            if (level.getBlockEntity(outside) instanceof BloomeryAir air && air.blowsInto(wall) && ++count >= MAX_BELLOWS) break;
        }
        return count;
    }

    public float targetTemperature() {
        return BASE_TEMPERATURE + PER_CHIMNEY * structure.chimney() + PER_BELLOWS * poweredBellows();
    }

    public int burnTicks() {
        return (int) Math.round(Config.BLOOMERY_BURN_TICKS.get() * Math.pow(0.75, poweredBellows()));
    }

    public Status status() {
        if (hasBlooms()) return Status.READY;
        if (lit()) {
            if (temperature >= minTemperature()) return Status.BURNING;
            return targetTemperature() < minTemperature() && temperature >= targetTemperature() - 1 ? Status.TOO_COOL : Status.HEATING;
        }
        if (!structure.complete()) return Status.INCOMPLETE;
        if (tooCool) return Status.TOO_COOL;
        if (oreCount() == 0) return Status.EMPTY;
        if (charcoalNeeded() > 0) return Status.NEEDS_CHARCOAL;
        return Status.CHARGED;
    }

    public boolean canLight() {
        return !lit() && !hasBlooms() && checkStructure().complete() && oreCount() > 0 && charcoalNeeded() == 0;
    }

    /** Why lighting is refused, or null if it would light. */
    public Component refusal() {
        if (lit()) return null;
        if (hasBlooms()) return Component.translatable(Status.READY.key());
        if (!checkStructure().complete()) return Component.translatable(Status.INCOMPLETE.key());
        if (oreCount() == 0) return Component.translatable(Status.EMPTY.key());
        int more = charcoalNeeded();
        if (more > 0) return Component.translatable(Status.NEEDS_CHARCOAL.key(), more);
        return null;
    }

    void onLit() {
        tooCool = false;
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BloomeryBlockEntity bloomery) {
        boolean lit = state.getValue(BloomeryBlock.LIT);
        long time = level.getGameTime();
        if (time % 20 == 0) {
            bloomery.checkStructure();
            if (bloomery.structure.complete() && !bloomery.announced) {
                bloomery.announced = true;
                Journal.awardNear(level, pos, Journal.BLOOMERY_BUILT);
            }
            if (lit && !bloomery.structure.complete()) {
                BloomeryBlock.setLit(level, pos, state, false);
                lit = false;
            }
        }
        float before = bloomery.temperature;
        if (lit) {
            float target = bloomery.targetTemperature();
            if (bloomery.temperature < target) bloomery.temperature = Math.min(target, bloomery.temperature + HEAT_PER_TICK);
            else bloomery.temperature = Math.max(target, bloomery.temperature - COOL_PER_TICK);
            if (bloomery.temperature >= minTemperature()) {
                bloomery.progress += 1.0f / Math.max(1, bloomery.burnTicks());
                if (bloomery.progress >= 1) bloomery.finish((ServerLevel) level);
            } else if (target < minTemperature() && bloomery.temperature >= target - 1) {
                // Too cool to ever make a bloom: the fire dies down and the charge stays (spec 5.3).
                bloomery.tooCool = true;
                BloomeryBlock.setLit(level, pos, state, false);
            }
            if (time % 4 == 0 && level instanceof ServerLevel server) bloomery.smoke(server);
        } else {
            bloomery.temperature = Math.max(Heat.AMBIENT, bloomery.temperature - COOL_PER_TICK);
            if (time % 10 == 0) bloomery.collectDropped(level);
        }
        if (bloomery.temperature != before || lit) setChanged(level, pos, state);
    }

    /** Smoke out of the chimney top while burning. */
    private void smoke(ServerLevel server) {
        BlockPos top = BloomeryStructure.chamber(worldPosition, facing()).above(structure.chimney() + 1);
        server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, top.getX() + 0.5, top.getY() + 0.1, top.getZ() + 0.5,
                1, 0.15, 0.0, 0.15, 0.01);
        if (server.getRandom().nextInt(3) == 0) {
            server.sendParticles(ParticleTypes.LAVA, top.getX() + 0.5, top.getY(), top.getZ() + 0.5, 1, 0.1, 0.0, 0.1, 0.0);
        }
    }

    /** Items dropped down the chimney land in the charge (spec 5.2). */
    private void collectDropped(Level level) {
        if (hasBlooms() || !structure.complete()) return;
        BlockPos chamber = BloomeryStructure.chamber(worldPosition, facing());
        AABB shaft = new AABB(chamber).expandTowards(0, structure.chimney() + 1, 0);
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, shaft);
        for (ItemEntity entity : items) {
            ItemStack stack = entity.getItem();
            int moved = insert(stack, null);
            if (moved <= 0) continue;
            stack.shrink(moved);
            if (stack.isEmpty()) entity.discard();
            else entity.setItem(stack);
        }
    }

    /** The charge becomes blooms and slag (spec 5.4). */
    private void finish(ServerLevel level) {
        float yield = temperature >= fullYieldTemperature() ? 1.0f : lowYield();
        int ore = oreCount();
        Melt iron = Melt.EMPTY;
        for (ItemStack stack : charge) {
            if (stack.isEmpty() || isFuel(stack)) continue;
            Melt melt = MetalContent.of(stack.copyWithCount(1)).orElse(Melt.EMPTY);
            int units = melt.units().getOrDefault(Metal.WROUGHT_IRON, 0);
            iron = iron.plus(Melt.of(Metal.WROUGHT_IRON, units * stack.getCount()));
        }
        int total = (int) Math.floor(iron.total() * yield);
        fullBlooms = total / BLOOM_UNITS;
        int rest = total % BLOOM_UNITS;
        partialUnits = rest >= MIN_PARTIAL_UNITS ? rest : 0;
        slag = (ore + 3) / 4;
        for (int i = 0; i < charge.size(); i++) charge.set(i, ItemStack.EMPTY);
        progress = 0;
        BloomeryBlock.setLit(level, worldPosition, getBlockState(), false);
        level.playSound(null, worldPosition, ModSounds.BLOOMERY_DONE.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
        setChanged();
    }

    /** One bloom out per pickaxe click, hot from the hearth; the slag comes with the last one. */
    public ItemStack extract() {
        if (!hasBlooms() || level == null) return ItemStack.EMPTY;
        int units;
        if (fullBlooms > 0) {
            units = BLOOM_UNITS;
            fullBlooms--;
        } else {
            units = partialUnits;
            partialUnits = 0;
        }
        ItemStack bloom = new ItemStack(ModItems.RAW_BLOOM.get());
        bloom.set(ModDataComponents.BLOOM_CONTENTS.get(), Melt.of(Metal.WROUGHT_IRON, units));
        Heat.set(bloom, Math.max(temperature, minTemperature()), level.getGameTime());
        if (!hasBlooms() && slag > 0) {
            Block.popResource(level, worldPosition.relative(facing()), new ItemStack(ModItems.BLOOMERY_SLAG.get(), slag));
            slag = 0;
        }
        setChanged();
        return bloom;
    }

    /** What the screen shows as the expected number of blooms for the current charge and heat. */
    public int expectedBlooms() {
        if (hasBlooms()) return fullBlooms + (partialUnits > 0 ? 1 : 0);
        float hottest = targetTemperature();
        if (hottest < minTemperature()) return 0;
        float yield = hottest >= fullYieldTemperature() ? 1.0f : lowYield();
        int units = 0;
        for (ItemStack stack : charge) {
            if (stack.isEmpty() || isFuel(stack)) continue;
            units += MetalContent.of(stack.copyWithCount(1)).map(m -> m.units().getOrDefault(Metal.WROUGHT_IRON, 0)).orElse(0)
                    * stack.getCount();
        }
        int total = (int) Math.floor(units * yield);
        return total / BLOOM_UNITS + (total % BLOOM_UNITS >= MIN_PARTIAL_UNITS ? 1 : 0);
    }

    public int expectedYield() {
        float hottest = lit() || hasBlooms() ? Math.max(temperature, targetTemperature()) : targetTemperature();
        if (hottest < minTemperature()) return 0;
        return hottest >= fullYieldTemperature() ? 100 : Math.round(lowYield() * 100);
    }

    public float temperature() {
        return temperature;
    }

    // ------------------------------------------------------------------ screen

    public ContainerData data() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                BlockPos at = structure.at().subtract(worldPosition);
                return switch (index) {
                    case DATA_STATUS -> status().ordinal();
                    case DATA_TEMPERATURE -> Math.round(temperature);
                    case DATA_PROGRESS -> Math.round(progress * 1000);
                    case DATA_ORE -> oreCount();
                    case DATA_CHARCOAL -> charcoalCount();
                    case DATA_CHIMNEY -> structure.chimney();
                    case DATA_PROBLEM -> structure.problem().ordinal();
                    case DATA_DX -> at.getX();
                    case DATA_DY -> at.getY();
                    case DATA_DZ -> at.getZ();
                    case DATA_YIELD -> expectedYield();
                    case DATA_BLOOMS -> expectedBlooms();
                    case DATA_TARGET -> Math.round(targetTemperature());
                    case DATA_BELLOWS -> poweredBellows();
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
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container." + StrataIndustria.MOD_ID + ".bloomery");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        checkStructure();
        return new BloomeryMenu(id, inventory, worldPosition, data());
    }

    /** Breaking the controller spills the charge and any finished blooms; a run in progress is lost. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level == null) return;
        for (ItemStack stack : charge) if (!stack.isEmpty()) Block.popResource(level, pos, stack.copy());
        while (hasBlooms()) Block.popResource(level, pos, extract());
        charge = NonNullList.withSize(CHARGE_SLOTS, ItemStack.EMPTY);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        charge = NonNullList.withSize(CHARGE_SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, charge);
        temperature = input.getFloatOr("temperature", Heat.AMBIENT);
        progress = input.getFloatOr("progress", 0);
        tooCool = input.getIntOr("too_cool", 0) != 0;
        fullBlooms = input.getIntOr("full_blooms", 0);
        partialUnits = input.getIntOr("partial_units", 0);
        slag = input.getIntOr("slag", 0);
        announced = input.getIntOr("announced", 0) != 0;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, charge);
        output.putFloat("temperature", temperature);
        output.putFloat("progress", progress);
        output.putInt("too_cool", tooCool ? 1 : 0);
        output.putInt("full_blooms", fullBlooms);
        output.putInt("partial_units", partialUnits);
        output.putInt("slag", slag);
        output.putInt("announced", announced ? 1 : 0);
    }
}
