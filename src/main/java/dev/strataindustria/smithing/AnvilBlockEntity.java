package dev.strataindustria.smithing;

import dev.strataindustria.Config;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.heat.Heat;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.material.Metal;
import dev.strataindustria.mark.MakerMarks;
import dev.strataindustria.metal.Alloy;
import dev.strataindustria.metal.Melt;
import dev.strataindustria.metal.MetalContent;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * An anvil you work in the world (redesign L2): put hot metal down, pick a shape with sneak + hammer, strike.
 * The work (blows done, how many were bright) rides on the piece, so it can go back into the forge and come
 * back half done. Hammer machines build on this class and strike through {@link #machineBlow}.
 */
public class AnvilBlockEntity extends BaseContainerBlockEntity {
    public static final int INPUT = 0, OUTPUT = 1, SECOND = 2, FLUX = 3, PATTERN = 4, SLOTS = 5;
    /** The progress key of the weld shape, which belongs to no recipe. */
    public static final ResourceKey<Recipe<?>> WELD_KEY = ResourceKey.create(Registries.RECIPE, StrataIndustria.id("anvil/weld"));

    /** What the anvil says about the piece on it. */
    public enum Status { EMPTY, READY, TOO_COLD, NO_HAMMER, TOO_WEAK, OUTPUT_FULL, NOT_ENOUGH, NO_PLAN;
        public String key() {
            return StrataIndustria.MOD_ID + ".anvil." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** Why a weld cannot happen, or READY (tier 3 spec 9.4). NONE when there is nothing to weld. */
    public enum WeldStatus { NONE, READY, NO_RECIPE, TOO_WEAK, OUTPUT_FULL, TOO_COLD, NO_FLUX, NO_HAMMER;
        public String key() {
            return StrataIndustria.MOD_ID + ".anvil.weld." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** A shape the piece on the anvil can be struck into. */
    public record Shape(ResourceKey<Recipe<?>> key, ItemStack result, int blows, int count, boolean weld) {}

    /** What a blow from a machine did (tier 3 spec 8.4). */
    public enum MachineHit { STRUCK, DONE, TOO_COLD, REFUSED }

    private NonNullList<ItemStack> items = NonNullList.withSize(slotCount(), ItemStack.EMPTY);
    private @Nullable Identifier lastShape;
    // The rhythm of the person at the anvil. Not saved.
    private long lastBlowTick = Long.MIN_VALUE, lastClickTick = Long.MIN_VALUE;
    private int groove;
    private boolean glintPending;
    // What the renderer needs: the shape's result, the work done, and when the last blow and the finish happened.
    private ItemStack viewResult = ItemStack.EMPTY;
    private int viewDone, viewTotal;
    private long struckAt = Long.MIN_VALUE, finishedAt = Long.MIN_VALUE;

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

    /** How high the working face sits in the block, where the sparks fly from. */
    protected double faceHeight() {
        return getBlockState().getBlock() instanceof AnvilBlock block && block.isStone() ? 15.0 / 16.0 : 1.0;
    }

    protected int anvilTier() {
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

    private static ItemStack hammer(Player player) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < Inventory.SELECTION_SIZE; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(ModTags.Items.HAMMERS)) return stack;
        }
        ItemStack off = player.getOffhandItem();
        return off.is(ModTags.Items.HAMMERS) ? off : ItemStack.EMPTY;
    }

    // ------------------------------------------------------------------ shapes

    /** Everything the pieces on the anvil can become, sorted so the order is the same every time. */
    public List<Shape> shapes(ServerLevel server) {
        List<Shape> shapes = new ArrayList<>();
        ItemStack input = input();
        if (input.isEmpty()) return shapes;
        if (!items.get(SECOND).isEmpty()) {
            weldResult().ifPresent(result -> shapes.add(new Shape(WELD_KEY, result, Smithing.WELD_BLOWS, 1, true)));
            return shapes;
        }
        server.recipeAccess().recipeMap().getRecipesFor(ModRecipes.ANVIL.get(), new SingleRecipeInput(input), server)
                .sorted(Comparator.comparing(h -> h.id().identifier().toString()))
                .forEach(h -> shapes.add(new Shape(h.id(), h.value().assemble(new SingleRecipeInput(input)), h.value().blows(), h.value().count(), false)));
        return shapes;
    }

    /** The shape the piece is set to: the one on its progress, else the last one used on this anvil, else the first. */
    public Optional<Shape> current(ServerLevel server) {
        List<Shape> shapes = shapes(server);
        if (shapes.isEmpty()) return Optional.empty();
        SmithingProgress progress = input().get(ModDataComponents.SMITHING_PROGRESS.get());
        if (progress != null) for (Shape shape : shapes) if (shape.key().equals(progress.recipe())) return Optional.of(shape);
        if (lastShape != null) for (Shape shape : shapes) if (shape.key().identifier().equals(lastShape)) return Optional.of(shape);
        return Optional.of(shapes.get(0));
    }

    /** Sets the piece to a shape, keeping its progress when the shape is unchanged. */
    private void setShape(Shape shape) {
        ItemStack input = input();
        if (input.isEmpty()) return;
        SmithingProgress progress = input.get(ModDataComponents.SMITHING_PROGRESS.get());
        input.set(ModDataComponents.SMITHING_PROGRESS.get(), progress == null ? SmithingProgress.start(shape.key()) : progress.withRecipe(shape.key()));
        if (!shape.weld()) lastShape = shape.key().identifier();
    }

    /** Puts the workpiece on recipe {@code id}, as if its shape had been picked. Returns whether that shape exists for it. */
    public boolean select(ResourceKey<Recipe<?>> id) {
        if (!(level instanceof ServerLevel server)) return false;
        for (Shape shape : shapes(server)) {
            if (!shape.key().equals(id)) continue;
            setShape(shape);
            setChanged();
            return true;
        }
        return false;
    }

    /** Sneak + hammer: the next shape, and a line saying what it is. */
    public void cycleShape(ServerPlayer player) {
        if (!(level instanceof ServerLevel server)) return;
        List<Shape> shapes = shapes(server);
        if (shapes.isEmpty()) {
            if (!input().isEmpty()) player.sendOverlayMessage(Component.translatable(Status.NO_PLAN.key()));
            return;
        }
        Shape now = current(server).orElse(shapes.get(0));
        int next = 0;
        for (int i = 0; i < shapes.size(); i++) if (shapes.get(i).key().equals(now.key())) next = (i + 1) % shapes.size();
        Shape shape = shapes.get(next);
        input().remove(ModDataComponents.SMITHING_PROGRESS.get());
        setShape(shape);
        server.playSound(null, worldPosition, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3f, 1.4f);
        player.sendOverlayMessage(shapeLine(shape));
        setChanged();
    }

    /** "Pickaxe head · 5 blows". */
    public Component shapeLine(Shape shape) {
        int blows = Smithing.totalBlows(shape.blows(), metalOf(input()).orElse(null));
        return Component.translatable(StrataIndustria.MOD_ID + (blows == 1 ? ".anvil.shape_one" : ".anvil.shape"), shape.result().getHoverName(), blows);
    }

    /** Blows the current shape takes on the current piece. */
    public int totalBlows(Shape shape) {
        Metal metal = metalOf(input()).orElse(null);
        if (shape.weld()) metal = higherTier(metalOf(input()).orElse(null), metalOf(items.get(SECOND)).orElse(null));
        return Smithing.totalBlows(shape.blows(), metal);
    }

    /** Blows a recipe takes on this piece, for machines that work a shape from a card. */
    public static int blowsFor(AnvilRecipe recipe, ItemStack piece) {
        return Smithing.totalBlows(recipe.blows(), metalOf(piece).orElse(null));
    }

    private static @Nullable Metal higherTier(@Nullable Metal a, @Nullable Metal b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.tier() >= b.tier() ? a : b;
    }

    // ------------------------------------------------------------------ putting pieces down and taking them back

    /** Whether this item can lie on the anvil to be worked: a metal piece or a raw bloom. */
    public static boolean workable(ItemStack stack) {
        return !stack.isEmpty() && (stack.has(ModDataComponents.BLOOM_CONTENTS.get()) || metalOf(stack).isPresent());
    }

    /**
     * Sets one of the held item on the anvil: as the workpiece, on top of its stack, or as the second piece of a weld.
     * Returns whether anything was placed.
     */
    public boolean place(ServerPlayer player, ItemStack held) {
        if (!(level instanceof ServerLevel server) || !workable(held)) return false;
        ItemStack input = input();
        ItemStack piece = held.copyWithCount(1);
        boolean placed = false;
        if (input.isEmpty()) {
            items.set(INPUT, piece);
            placed = true;
        } else if (ItemStack.isSameItemSameComponents(input, piece) && input.getCount() < 16) {
            // Another of the same: it stacks, for shapes that take two. A second piece of a weld goes beside it instead.
            if (weldResult(input, piece).isPresent() && items.get(SECOND).isEmpty() && input.getCount() == 1) {
                items.set(SECOND, piece);
            } else {
                input.grow(1);
            }
            placed = true;
        } else if (items.get(SECOND).isEmpty() && weldResult(input, piece).isPresent()) {
            items.set(SECOND, piece);
            placed = true;
        }
        if (!placed) return false;
        if (!player.getAbilities().instabuild) held.shrink(1);
        server.playSound(null, worldPosition, ModSounds.ANVIL_SET.get(), SoundSource.BLOCKS, 0.7f, 0.9f + server.getRandom().nextFloat() * 0.2f);
        Optional<Shape> shape = current(server);
        shape.ifPresent(this::setShape);
        setChanged();
        shape.ifPresent(s -> player.sendOverlayMessage(shapeLine(s)));
        return true;
    }

    /** Empty hand: the finished piece first, else the second piece, else the workpiece. Returns whether anything came back. */
    public boolean take(ServerPlayer player) {
        int slot = !items.get(OUTPUT).isEmpty() ? OUTPUT : !items.get(SECOND).isEmpty() ? SECOND : !input().isEmpty() ? INPUT : -1;
        if (slot < 0 || !(level instanceof ServerLevel server)) return false;
        ItemStack stack = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        if (slot == INPUT && !items.get(SECOND).isEmpty()) {
            // The welded-to piece becomes the workpiece again.
            items.set(INPUT, items.get(SECOND));
            items.set(SECOND, ItemStack.EMPTY);
        }
        server.playSound(null, worldPosition, ModSounds.ANVIL_SET.get(), SoundSource.BLOCKS, 0.6f, 1.2f);
        if (!player.getInventory().add(stack)) net.minecraft.world.level.block.Block.popResource(server, worldPosition.above(), stack);
        current(server).ifPresent(this::setShape);
        setChanged();
        return true;
    }

    // ------------------------------------------------------------------ striking

    public Status status(@Nullable Player player) {
        ItemStack input = input();
        if (input.isEmpty()) return Status.EMPTY;
        if (!(level instanceof ServerLevel server)) return Status.READY;
        Optional<Shape> shape = current(server);
        if (shape.isEmpty()) return Status.NO_PLAN;
        Optional<Metal> metal = metalOf(input);
        Shape s = shape.get();
        if (s.weld()) {
            WeldStatus weld = weldStatus(player, false);
            return switch (weld) {
                case TOO_WEAK -> Status.TOO_WEAK;
                case OUTPUT_FULL -> Status.OUTPUT_FULL;
                case TOO_COLD -> Status.TOO_COLD;
                case NO_HAMMER -> Status.NO_HAMMER;
                case READY -> Status.READY;
                default -> Status.NO_PLAN;
            };
        }
        if (metal.isPresent() && metal.get().tier() > anvilTier()) return Status.TOO_WEAK;
        if (input.getCount() < s.count()) return Status.NOT_ENOUGH;
        if (!items.get(OUTPUT).isEmpty()) return Status.OUTPUT_FULL;
        if (level != null && Heat.get(input, level) < workingTemperature(input)) return Status.TOO_COLD;
        if (player != null && hammer(player).isEmpty()) return Status.NO_HAMMER;
        return Status.READY;
    }

    /**
     * A hammer click on the anvil. Clicks that are too close are ignored; one on the glint is a true blow that
     * counts twice; a held button repeats at a steady pace and never lands on the glint.
     */
    public void strikeBy(ServerPlayer player) {
        if (level instanceof ServerLevel server) strikeBy(player, server.getGameTime());
    }

    /** {@link #strikeBy(ServerPlayer)} on an explicit clock, so tests can space their blows. */
    public void strikeBy(ServerPlayer player, long now) {
        if (!(level instanceof ServerLevel server)) return;
        boolean held = lastClickTick != Long.MIN_VALUE && now - lastClickTick <= Smithing.HOLD_REPEAT_TICKS;
        lastClickTick = now;
        long since = lastBlowTick == Long.MIN_VALUE ? Long.MAX_VALUE : now - lastBlowTick;
        if (since < (held ? Smithing.HOLD_GAP_TICKS : Smithing.MIN_GAP_TICKS)) return;
        Status status = status(player);
        if (status != Status.READY) {
            if (status == Status.EMPTY) return;
            if (status == Status.TOO_COLD) coldThud(server, now);
            Component line = status == Status.NOT_ENOUGH
                    ? Component.translatable(status.key(), current(server).map(Shape::count).orElse(1))
                    : Component.translatable(status.key());
            player.sendOverlayMessage(line);
            return;
        }
        boolean inRhythm = since <= Smithing.RHYTHM_TIMEOUT_TICKS;
        boolean trueBlow = !held && inRhythm && Math.abs(since - Smithing.BEAT_TICKS) <= Config.SMITHING_BEAT_WINDOW.get();
        groove = trueBlow ? groove + 1 : 0;
        hammer(player).hurtAndBreak(1, server, player,
                broken -> server.playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 0.8f, 1.0f));
        blow(server, player, trueBlow ? 2 : 1, -1, now);
    }

    /** One blow from a machine: no hammer worn, no heat lost, and the craft part set by the machine. */
    public MachineHit machineBlow(int craft) {
        if (!(level instanceof ServerLevel server)) return MachineHit.REFUSED;
        Status status = status(null);
        if (status == Status.TOO_COLD) return MachineHit.TOO_COLD;
        if (status != Status.READY) return MachineHit.REFUSED;
        return blow(server, null, 1, craft, server.getGameTime()) ? MachineHit.DONE : MachineHit.STRUCK;
    }

    private void coldThud(ServerLevel server, long now) {
        lastBlowTick = now;
        glintPending = false;
        groove = 0;
        server.playSound(null, worldPosition, ModSounds.ANVIL_COLD.get(), SoundSource.BLOCKS, 0.8f, 0.9f + server.getRandom().nextFloat() * 0.2f);
        server.sendParticles(ParticleTypes.SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + faceHeight() + 0.02, worldPosition.getZ() + 0.5,
                2, 0.08, 0.0, 0.08, 0.005);
    }

    /** Lands {@code weight} blows on the current shape. Returns whether that finished the piece. */
    private boolean blow(ServerLevel server, @Nullable ServerPlayer player, int weight, int machineCraft, long tick) {
        Shape shape = current(server).orElseThrow();
        ItemStack input = input();
        long now = server.getGameTime();
        SmithingProgress before = input.get(ModDataComponents.SMITHING_PROGRESS.get());
        if (before == null || !before.recipe().equals(shape.key())) before = SmithingProgress.start(shape.key());
        int total = totalBlows(shape);
        boolean bright = Smithing.isBright(Heat.get(input, now), workingTemperature(input));
        SmithingProgress progress = before.strike(weight, bright);
        input.set(ModDataComponents.SMITHING_PROGRESS.get(), progress);
        // Iron and steel lose heat to every blow, so careless work means a trip back to the forge. A machine keeps its piece hot.
        if (machineCraft < 0 && metalOf(input).map(m -> m.tier() >= 3).orElse(false)) {
            Heat.set(input, Heat.get(input, now) - Config.SMITHING_HIT_COOLING.get(), now);
        }
        boolean done = progress.blows() >= total;
        feedback(server, input, before.blows(), total, weight, bright, done);
        lastBlowTick = tick;
        struckAt = now;
        glintPending = !done;
        if (player != null && !(player instanceof net.neoforged.neoforge.common.util.FakePlayer) && Config.SMITHING_SCREEN_NUDGE.get() && (weight > 1 || done)) {
            StrikeNudge.send(player, done ? 1.0f : 0.6f);
        }
        if (done) {
            finish(server, shape, progress, total, player, machineCraft);
            lastBlowTick = Long.MIN_VALUE;
            groove = 0;
        }
        setChanged();
        return done;
    }

    /** Sound, sparks and chips for a blow: the hammer, the metal's note on the scale, and a shower scaled to the heat. */
    private void feedback(ServerLevel server, ItemStack input, int doneBefore, int total, int weight, boolean bright, boolean finish) {
        var random = server.getRandom();
        float pitch = Smithing.notePitch(doneBefore, total, finish, bright);
        float vary = 1.0f + (random.nextFloat() - 0.5f) * 0.02f;
        SoundSource source = SoundSource.BLOCKS;
        if (input.has(ModDataComponents.BLOOM_CONTENTS.get())) {
            server.playSound(null, worldPosition, ModSounds.RAW_BLOOM_HIT.get(), source, 0.8f, (0.85f + pitch * 0.15f) * vary);
        } else {
            server.playSound(null, worldPosition, hitSound(input), source, 0.6f, (0.9f + pitch * 0.2f) * vary);
            server.playSound(null, worldPosition, voice(input), source, bright ? 0.9f : 0.7f, pitch * vary);
        }
        if (weight > 1) server.playSound(null, worldPosition, ModSounds.ANVIL_TRUE_BLOW.get(), source, 0.8f, 0.95f + random.nextFloat() * 0.1f);
        if (groove >= 3) server.playSound(null, worldPosition, voice(input), source, 0.35f, pitch * 2.0f > 2.0f ? 2.0f : pitch * 2.0f);
        double x = worldPosition.getX() + 0.5, y = worldPosition.getY() + faceHeight() + 0.03, z = worldPosition.getZ() + 0.5;
        float scale = Config.SMITHING_SPARKS.get() / 100.0f;
        int sparks = Math.round((bright ? 8 : 3) * (weight > 1 ? 2 : 1) * (groove >= 3 ? 1.5f : 1.0f) * scale);
        if (sparks > 0) {
            server.sendParticles(bright ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.SMALL_FLAME, x, y, z, sparks, 0.14, 0.0, 0.14, bright ? 0.25 : 0.05);
            if (bright) server.sendParticles(ParticleTypes.LAVA, x, y, z, Math.max(1, Math.round(scale * weight)), 0.1, 0.0, 0.1, 0.0);
        }
        int flakes = Math.round(2 * scale);
        if (flakes > 0) server.sendParticles(ParticleTypes.ASH, x, y + 0.05, z, flakes, 0.18, 0.05, 0.18, 0.01);
    }

    /** The hammer sound: bronze rings; wrought iron rings lower; a raw bloom only thuds (tier 3 spec 20.6). */
    private static SoundEvent hitSound(ItemStack input) {
        if (metalOf(input).filter(m -> m == Metal.WROUGHT_IRON).isPresent()) return ModSounds.WROUGHT_IRON_HIT.get();
        return ModSounds.SMITH_HIT.get();
    }

    /** Copper soft and round, bronze bell-like, iron dry, steel bright and long. */
    private static SoundEvent voice(ItemStack input) {
        Metal metal = metalOf(input).orElse(null);
        if (metal == null) return ModSounds.ANVIL_VOICE_BRONZE.get();
        if (metal.tier() >= 4) return ModSounds.ANVIL_VOICE_STEEL.get();
        if (metal.tier() == 3) return ModSounds.ANVIL_VOICE_IRON.get();
        if (metal == Metal.COPPER) return ModSounds.ANVIL_VOICE_COPPER.get();
        return ModSounds.ANVIL_VOICE_BRONZE.get();
    }

    private void finish(ServerLevel server, Shape shape, SmithingProgress progress, int total, @Nullable ServerPlayer player, int machineCraft) {
        ItemStack input = input();
        long now = server.getGameTime();
        boolean bloom = input.has(ModDataComponents.BLOOM_CONTENTS.get());
        if (bloom) {
            for (ServerPlayer near : server.getEntitiesOfClass(ServerPlayer.class, new net.minecraft.world.phys.AABB(worldPosition).inflate(8))) {
                Journal.award(near, Journal.BLOOM_REFINED);
            }
        }
        double x = worldPosition.getX() + 0.5, y = worldPosition.getY() + faceHeight() + 0.05, z = worldPosition.getZ() + 0.5;
        boolean allBright = progress.bright() >= progress.blows();
        if (shape.weld()) {
            doWeld(server, player, false);
        } else {
            int craft = machineCraft >= 0 ? machineCraft : Smithing.craftQuality(progress.bright(), progress.blows());
            float temperature = Heat.get(input, now);
            int material = MetalContent.of(input.copyWithCount(1)).map(Melt::quality).orElse(0);
            ItemStack out = shape.result().copy();
            out.set(ModDataComponents.QUALITY.get(), new Quality(material, craft));
            Heat.set(out, temperature, now);
            input.shrink(shape.count());
            if (input.isEmpty()) items.set(INPUT, ItemStack.EMPTY);
            else input.remove(ModDataComponents.SMITHING_PROGRESS.get());
            items.set(OUTPUT, out);
            if (player != null) {
                MakerMarks.stampOrAsk(out, player, worldPosition);
                if (out.has(dev.strataindustria.mark.MarkRegistry.STAMP.get())) stampFeedback(server);
            }
        }
        finishedAt = now;
        glintPending = false;
        server.playSound(null, worldPosition, ModSounds.SMITH_DONE.get(), SoundSource.BLOCKS, 0.9f, 1.0f);
        server.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.25f, 1.7f);
        float scale = Config.SMITHING_SPARKS.get() / 100.0f;
        server.sendParticles(ParticleTypes.LAVA, x, y, z, Math.round(4 * scale), 0.15, 0.0, 0.15, 0.0);
        server.sendParticles(ParticleTypes.CLOUD, x, y, z, 3, 0.1, 0.02, 0.1, 0.01);
        if (allBright && machineCraft < 0) {
            server.sendParticles(ParticleTypes.WAX_ON, x, y + 0.1, z, Math.max(1, Math.round(10 * scale)), 0.2, 0.15, 0.2, 0.0);
            if (player != null) Journal.award(player, Journal.BRIGHT_STRIKE);
        }
    }

    /** The glint at the top of the hammer's rebound, a soft tick to strike on. */
    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, AnvilBlockEntity anvil) {
        if (!anvil.glintPending || !(level instanceof ServerLevel server)) return;
        long now = server.getGameTime();
        if (now - anvil.lastBlowTick < Smithing.BEAT_TICKS) return;
        anvil.glintPending = false;
        server.playSound(null, pos, ModSounds.ANVIL_GLINT.get(), SoundSource.BLOCKS, 0.6f, 1.0f);
        server.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + anvil.faceHeight() + 0.35, pos.getZ() + 0.5, 1, 0.0, 0.0, 0.0, 0.0);
    }

    /** What a renderer needs. */
    public ItemStack viewResult() {
        return viewResult;
    }

    public int viewDone() {
        return viewDone;
    }

    public int viewTotal() {
        return viewTotal;
    }

    public long struckAt() {
        return struckAt;
    }

    public long finishedAt() {
        return finishedAt;
    }

    private void refreshView() {
        if (!(level instanceof ServerLevel server)) return;
        Optional<Shape> shape = input().isEmpty() ? Optional.empty() : current(server);
        if (shape.isEmpty()) {
            viewResult = ItemStack.EMPTY;
            viewDone = 0;
            viewTotal = 0;
        } else {
            SmithingProgress progress = input().get(ModDataComponents.SMITHING_PROGRESS.get());
            viewResult = shape.get().result().copyWithCount(1);
            viewDone = progress != null && progress.recipe().equals(shape.get().key()) ? progress.blows() : 0;
            viewTotal = totalBlows(shape.get());
        }
    }

    /** The mark going into a finished piece: a tick of sound, a puff and a few bright flecks, a beat after the finish. */
    private void stampFeedback(ServerLevel server) {
        double x = worldPosition.getX() + 0.5, y = worldPosition.getY() + faceHeight() + 0.15, z = worldPosition.getZ() + 0.5;
        server.playSound(null, worldPosition, ModSounds.ANVIL_STAMP.get(), SoundSource.BLOCKS, 0.8f, 1.0f);
        server.sendParticles(ParticleTypes.CRIT, x, y, z, 6, 0.1, 0.05, 0.1, 0.2);
        server.sendParticles(ParticleTypes.WAX_ON, x, y, z, 3, 0.1, 0.05, 0.1, 0.0);
    }

    /** Stamps the piece waiting in the output, for a player who has just cut their first mark. */
    public void stampOutput(Player player) {
        ItemStack out = items.get(OUTPUT);
        if (out.has(ModDataComponents.QUALITY.get()) && !out.has(dev.strataindustria.mark.MarkRegistry.STAMP.get())) {
            dev.strataindustria.mark.MakerMarks.stamp(out, player);
            if (level instanceof ServerLevel server) stampFeedback(server);
            setChanged();
        }
    }

    // ------------------------------------------------------------------ shape cards for hammer machines

    public static boolean isBlankPattern(ItemStack stack) {
        return stack.is(ModItems.SMITHING_PATTERN.get()) && !stack.has(ModDataComponents.SMITHING_PATTERN.get());
    }

    /** Sneak + a blank pattern on the anvil: the card takes the shape that is picked. */
    public boolean record(ServerPlayer player, ItemStack pattern) {
        if (!(level instanceof ServerLevel server) || !isBlankPattern(pattern)) return false;
        Optional<Shape> shape = current(server);
        if (shape.isEmpty() || shape.get().weld()) return false;
        ItemStack card = pattern.copyWithCount(1);
        card.set(ModDataComponents.SMITHING_PATTERN.get(),
                new SmithingPattern(shape.get().key(), BuiltInRegistries.ITEM.getKey(shape.get().result().getItem())));
        if (!player.getAbilities().instabuild) pattern.shrink(1);
        if (!player.getInventory().add(card)) net.minecraft.world.level.block.Block.popResource(server, worldPosition.above(), card);
        server.playSound(null, worldPosition, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.8f, 1.1f);
        player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".anvil.recorded", shape.get().result().getHoverName()));
        Journal.award(player, Journal.PATTERN_RECORDED);
        return true;
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
        return weldResult(input(), items.get(SECOND));
    }

    /** What welding {@code a} to {@code b} would make, if anything. */
    protected Optional<ItemStack> weldResult(ItemStack a, ItemStack b) {
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

    public WeldStatus weldStatus(@org.jspecify.annotations.Nullable Player player) {
        return weldStatus(player, true);
    }

    /** Welding needs flux in a machine's flux slot; striking by hand does not. */
    public WeldStatus weldStatus(@org.jspecify.annotations.Nullable Player player, boolean needFlux) {
        ItemStack a = input(), b = items.get(SECOND);
        if (a.isEmpty() || b.isEmpty()) return WeldStatus.NONE;
        if (weldResult().isEmpty()) return WeldStatus.NO_RECIPE;
        int metalTier = Math.max(metalOf(a).map(Metal::tier).orElse(0), metalOf(b).map(Metal::tier).orElse(0));
        if (metalTier > anvilTier()) return WeldStatus.TOO_WEAK;
        if (!items.get(OUTPUT).isEmpty()) return WeldStatus.OUTPUT_FULL;
        int needed = weldingTemperature(a, b);
        if (level != null && (Heat.get(a, level) < needed || Heat.get(b, level) < needed)) return WeldStatus.TOO_COLD;
        if (needFlux && !isFlux(items.get(FLUX))) return WeldStatus.NO_FLUX;
        if (player != null && hammer(player).isEmpty()) return WeldStatus.NO_HAMMER;
        return WeldStatus.READY;
    }

    /** One weld by a machine: the same rules, no hammer worn. False when the pieces are not ready. */
    public boolean machineWeld() {
        if (!(level instanceof ServerLevel server) || weldStatus(null) != WeldStatus.READY) return false;
        doWeld(server, null, true);
        return true;
    }

    private void doWeld(ServerLevel server, @org.jspecify.annotations.Nullable ServerPlayer player, boolean useFlux) {
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
        if (useFlux) items.get(FLUX).shrink(1);
        if (a.isEmpty()) items.set(INPUT, ItemStack.EMPTY);
        if (b.isEmpty()) items.set(SECOND, ItemStack.EMPTY);
        items.set(OUTPUT, out);
        // Two pieces becoming one: a flare of sparks thrown out sideways, a hiss as the scale pops off, steam after.
        double x = worldPosition.getX() + 0.5, y = worldPosition.getY() + faceHeight() + 0.05, z = worldPosition.getZ() + 0.5;
        float scale = Config.SMITHING_SPARKS.get() / 100.0f;
        server.playSound(null, worldPosition, ModSounds.ANVIL_WELD.get(), SoundSource.BLOCKS, 0.9f, 0.95f + server.getRandom().nextFloat() * 0.1f);
        server.playSound(null, worldPosition, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.3f, 1.5f);
        server.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, Math.round(18 * scale), 0.2, 0.05, 0.2, 0.2);
        server.sendParticles(ParticleTypes.LAVA, x, y, z, Math.max(1, Math.round(5 * scale)), 0.15, 0.0, 0.15, 0.0);
        server.sendParticles(ParticleTypes.CLOUD, x, y + 0.05, z, 4, 0.1, 0.02, 0.1, 0.01);
        finishedAt = now;
        setChanged();
    }

    private static int craftOf(ItemStack stack) {
        Quality quality = stack.get(ModDataComponents.QUALITY.get());
        return quality == null ? 0 : quality.craft();
    }


    @Override
    public void setChanged() {
        super.setChanged();
        refreshView();
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

    /** The anvil has no screen; hammer machines built on it have their own. */
    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return null;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(slotCount(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        lastShape = input.read("last_shape", Identifier.CODEC).orElse(null);
        viewResult = input.read("view_result", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        viewDone = input.getIntOr("view_done", 0);
        viewTotal = input.getIntOr("view_total", 0);
        struckAt = input.getLongOr("struck_at", Long.MIN_VALUE);
        finishedAt = input.getLongOr("finished_at", Long.MIN_VALUE);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items, false);
        if (lastShape != null) output.store("last_shape", Identifier.CODEC, lastShape);
        output.store("view_result", ItemStack.OPTIONAL_CODEC, viewResult);
        output.putInt("view_done", viewDone);
        output.putInt("view_total", viewTotal);
        output.putLong("struck_at", struckAt);
        output.putLong("finished_at", finishedAt);
    }
}
