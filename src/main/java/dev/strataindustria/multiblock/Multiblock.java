package dev.strataindustria.multiblock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.StrataIndustria;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;

/**
 * A multiblock shape as data (progression design 5.6, tier 4 spec 16.7): layers of rows of symbols around a
 * controller, what each symbol accepts, and counts of marked blocks the whole needs. Datapack registry
 * {@code strataindustria:multiblock}, so the files live in {@code data/<ns>/strataindustria/multiblock/}.
 *
 * <p>The layers are listed bottom first; each holds rows from the front (the controller's row) to the back,
 * and each row holds symbols from the left to the right as seen standing at the controller and looking into
 * the structure. {@code @} is the controller itself, {@code .} and a space are any block. A layer may repeat
 * between {@code min} and {@code max} times, and only layers above the controller's may repeat. The pattern
 * is what the check, the builder's ledger and the ghost preview all read.
 */
public final class Multiblock {
    public static final ResourceKey<Registry<Multiblock>> REGISTRY = ResourceKey.createRegistryKey(StrataIndustria.id("multiblock"));

    public static final char CONTROLLER = '@';

    /** What a symbol's cell may hold besides plain blocks. */
    public enum Special implements StringRepresentable {
        /** Air or anything without collision or fluid: the chamber of a furnace. */
        OPEN("open"),
        /** Anything that is not a full block or a fluid: the top a draught needs. */
        NO_DRAUGHT("no_draught");

        public static final Codec<Special> CODEC = StringRepresentable.fromEnum(Special::values);
        private final String name;

        Special(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    /**
     * One thing a symbol accepts: a block (with the block state values it must have), a block tag, or a special
     * rule. A block that carries {@code roles} is recorded under each of them in the result.
     */
    public record Entry(Optional<Block> block, Optional<TagKey<Block>> tag, Optional<Special> special, Map<String, String> state, List<String> roles) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.<Entry>create(i -> i.group(
                BuiltInRegistries.BLOCK.byNameCodec().optionalFieldOf("block").forGetter(Entry::block),
                TagKey.codec(Registries.BLOCK).optionalFieldOf("tag").forGetter(Entry::tag),
                Special.CODEC.optionalFieldOf("special").forGetter(Entry::special),
                Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("state", Map.of()).forGetter(Entry::state),
                Codec.STRING.listOf().optionalFieldOf("roles", List.of()).forGetter(Entry::roles)
        ).apply(i, Entry::new)).validate(entry -> {
            int kinds = (entry.block.isPresent() ? 1 : 0) + (entry.tag.isPresent() ? 1 : 0) + (entry.special.isPresent() ? 1 : 0);
            return kinds == 1 ? DataResult.success(entry) : DataResult.error(() -> "an entry needs exactly one of block, tag and special");
        });

        public boolean accepts(Level level, BlockPos pos, BlockState at) {
            if (special.isPresent()) {
                return switch (special.get()) {
                    case OPEN -> at.isAir() || at.getCollisionShape(level, pos).isEmpty() && at.getFluidState().isEmpty();
                    case NO_DRAUGHT -> !(at.isCollisionShapeFullBlock(level, pos) || !at.getFluidState().isEmpty());
                };
            }
            if (block.isPresent() && !at.is(block.get())) return false;
            if (tag.isPresent() && !at.is(tag.get())) return false;
            for (Map.Entry<String, String> required : state.entrySet()) {
                Property<?> property = at.getBlock().getStateDefinition().getProperty(required.getKey());
                if (property == null || !valueName(at, property).equals(required.getValue())) return false;
            }
            return true;
        }

        /** Whether this entry stands for a solid block, which counts a layer as started. */
        boolean solid() {
            return special.isEmpty();
        }

        private static <T extends Comparable<T>> String valueName(BlockState state, Property<T> property) {
            return property.getName(state.getValue(property));
        }
    }

    /** The block the ledger places and the ghost shows for a symbol; {@code facing} is relative to the controller. */
    public record Preview(Block block, Map<String, String> state, Optional<String> facing) {
        public static final Codec<Preview> CODEC = RecordCodecBuilder.create(i -> i.group(
                BuiltInRegistries.BLOCK.byNameCodec().fieldOf("block").forGetter(Preview::block),
                Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("state", Map.of()).forGetter(Preview::state),
                Codec.STRING.optionalFieldOf("facing").forGetter(Preview::facing)
        ).apply(i, Preview::new));

        public BlockState state(Direction controllerFacing) {
            BlockState result = block.defaultBlockState();
            for (Map.Entry<String, String> value : state.entrySet()) {
                Property<?> property = block.getStateDefinition().getProperty(value.getKey());
                if (property != null) result = withValue(result, property, value.getValue());
            }
            if (facing.isPresent()) {
                Property<?> property = block.getStateDefinition().getProperty("facing");
                Direction direction = switch (facing.get()) {
                    case "front" -> controllerFacing;
                    case "back" -> controllerFacing.getOpposite();
                    case "left" -> controllerFacing.getClockWise();
                    case "right" -> controllerFacing.getCounterClockWise();
                    default -> controllerFacing;
                };
                if (property != null) result = withValue(result, property, direction.getSerializedName());
            }
            return result;
        }

        private static <T extends Comparable<T>> BlockState withValue(BlockState state, Property<T> property, String name) {
            Optional<T> value = property.getValue(name);
            return value.isPresent() ? state.setValue(property, value.get()) : state;
        }
    }

    /**
     * What a symbol is: the things it accepts, the problem named when a cell holds none of them and the
     * block to show and place for it, if any (symbols that are only space have none).
     */
    public record Symbol(List<Entry> accepts, String problem, Optional<Preview> preview) {
        public static final Codec<Symbol> CODEC = RecordCodecBuilder.create(i -> i.group(
                Entry.CODEC.listOf().fieldOf("accepts").forGetter(Symbol::accepts),
                Codec.STRING.fieldOf("problem").forGetter(Symbol::problem),
                Preview.CODEC.optionalFieldOf("preview").forGetter(Symbol::preview)
        ).apply(i, Symbol::new));
    }

    /**
     * A layer of rows. It repeats between {@code min} and {@code max} times; once it has {@code min}, a
     * repeat that fails ends the layer quietly unless {@code strict} and the repeat was started (it holds a
     * solid block), in which case the failure is the structure's.
     */
    public record Layer(List<String> rows, int min, int max, boolean strict) {
        public static final Codec<Layer> CODEC = RecordCodecBuilder.<Layer>create(i -> i.group(
                Codec.STRING.listOf().fieldOf("rows").forGetter(Layer::rows),
                Codec.INT.optionalFieldOf("min", 1).forGetter(Layer::min),
                Codec.INT.optionalFieldOf("max", 1).forGetter(Layer::max),
                Codec.BOOL.optionalFieldOf("strict", false).forGetter(Layer::strict)
        ).apply(i, Layer::new)).validate(layer -> layer.min < 0 || layer.max < layer.min || layer.max == 0
                ? DataResult.error(() -> "a layer needs 0 <= min <= max and max > 0") : DataResult.success(layer));
    }

    /**
     * At least {@code min} blocks of a role (and at most {@code max}) must be in the structure. Checked once
     * the given layer is matched, or at the end when {@code layer} is not given.
     */
    public record Requirement(String role, int min, Optional<Integer> max, String problem, Optional<Integer> layer) {
        public static final Codec<Requirement> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("role").forGetter(Requirement::role),
                Codec.INT.optionalFieldOf("min", 1).forGetter(Requirement::min),
                Codec.INT.optionalFieldOf("max").forGetter(Requirement::max),
                Codec.STRING.fieldOf("problem").forGetter(Requirement::problem),
                Codec.INT.optionalFieldOf("layer").forGetter(Requirement::layer)
        ).apply(i, Requirement::new));
    }

    private static final Codec<Character> SYMBOL_NAME = Codec.STRING.comapFlatMap(
            s -> s.length() == 1 ? DataResult.success(s.charAt(0)) : DataResult.error(() -> "a symbol is one character: " + s), String::valueOf);

    public static final Codec<Multiblock> CODEC = RecordCodecBuilder.<Multiblock>create(i -> i.group(
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("controller").forGetter(Multiblock::controller),
            Codec.unboundedMap(SYMBOL_NAME, Symbol.CODEC).fieldOf("symbols").forGetter(Multiblock::symbols),
            Layer.CODEC.listOf().fieldOf("layers").forGetter(Multiblock::layers),
            Requirement.CODEC.listOf().optionalFieldOf("requirements", List.of()).forGetter(Multiblock::requirements)
    ).apply(i, Multiblock::new)).validate(Multiblock::validate);

    private final Block controller;
    private final Map<Character, Symbol> symbols;
    private final List<Layer> layers;
    private final List<Requirement> requirements;
    /** Layer, row and column of the controller in the pattern. */
    private final int controllerLayer, controllerRow, controllerColumn;

    public Multiblock(Block controller, Map<Character, Symbol> symbols, List<Layer> layers, List<Requirement> requirements) {
        this.controller = controller;
        this.symbols = Map.copyOf(symbols);
        this.layers = List.copyOf(layers);
        this.requirements = List.copyOf(requirements);
        int layer = -1, row = -1, column = -1;
        for (int l = 0; l < layers.size(); l++) {
            List<String> rows = layers.get(l).rows();
            for (int r = 0; r < rows.size(); r++) {
                int c = rows.get(r).indexOf(CONTROLLER);
                if (c >= 0 && layer < 0) {
                    layer = l;
                    row = r;
                    column = c;
                }
            }
        }
        this.controllerLayer = layer;
        this.controllerRow = row;
        this.controllerColumn = column;
    }

    private DataResult<Multiblock> validate() {
        int markers = 0;
        for (Layer layer : layers) for (String row : layer.rows()) markers += (int) row.chars().filter(c -> c == CONTROLLER).count();
        if (markers != 1) {
            int found = markers;
            return DataResult.error(() -> "a multiblock has exactly one controller marker, found " + found);
        }
        for (int l = 0; l <= controllerLayer; l++) {
            if (layers.get(l).min() != 1 || layers.get(l).max() != 1) return DataResult.error(() -> "layers up to the controller's cannot repeat");
        }
        for (Layer layer : layers) {
            for (String row : layer.rows()) {
                for (char c : row.toCharArray()) {
                    if (c != CONTROLLER && c != '.' && c != ' ' && !symbols.containsKey(c)) return DataResult.error(() -> "unknown symbol " + c);
                }
            }
        }
        return DataResult.success(this);
    }

    public Block controller() {
        return controller;
    }

    public Map<Character, Symbol> symbols() {
        return symbols;
    }

    public List<Layer> layers() {
        return layers;
    }

    public List<Requirement> requirements() {
        return requirements;
    }

    // ---- placing the pattern in the world

    /** A pattern cell in the world. */
    public record Cell(BlockPos pos, char symbol, int layer) {}

    private BlockPos at(BlockPos controllerPos, Direction facing, int row, int column, int y) {
        Direction back = facing.getOpposite();
        Direction right = back.getClockWise();
        return controllerPos.relative(back, row - controllerRow).relative(right, column - controllerColumn).above(y);
    }

    private boolean skipped(char c) {
        return c == '.' || c == ' ' || c == CONTROLLER;
    }

    /** Height of a layer's first repeat above the controller, given that the layers below it have no repeats. */
    private int firstY() {
        return -controllerLayer;
    }

    // ---- checking

    /**
     * The outcome of matching: what is wrong first (or nothing), where the blocks of each role are and how
     * many times each layer repeated.
     *
     * @param problem the {@code problem} id of the first failure, or null when the structure is whole
     * @param at where it failed
     * @param symbol the symbol that failed, or a space when a requirement failed
     * @param role the role a failed requirement is short of
     */
    public record Match(@Nullable String problem, BlockPos at, char symbol, @Nullable String role, Map<String, List<BlockPos>> roles, int[] repeats) {
        public boolean complete() {
            return problem == null;
        }

        public List<BlockPos> role(String name) {
            return roles.getOrDefault(name, List.of());
        }

        public @Nullable BlockPos first(String name) {
            List<BlockPos> list = role(name);
            return list.isEmpty() ? null : list.getFirst();
        }
    }

    private Match fail(String problem, BlockPos at, char symbol, @Nullable String role, Map<String, List<BlockPos>> roles, int[] repeats) {
        return new Match(problem, at, symbol, role, roles, repeats);
    }

    /** Matches the world around a controller against the pattern. The controller block itself is not checked. */
    public Match check(Level level, BlockPos controllerPos, Direction facing) {
        Map<String, List<BlockPos>> roles = new HashMap<>();
        int[] repeats = new int[layers.size()];
        int y = firstY();
        for (int l = 0; l < layers.size(); l++) {
            Layer layer = layers.get(l);
            int count = 0;
            for (int k = 0; k < layer.max(); k++) {
                Map<String, List<BlockPos>> found = new HashMap<>();
                Attempt attempt = layer(level, controllerPos, facing, layer, y, roles, found);
                if (attempt.failure == null) {
                    found.forEach((role, list) -> roles.computeIfAbsent(role, r -> new ArrayList<>()).addAll(list));
                    count++;
                    y++;
                    continue;
                }
                if (count >= layer.min()) {
                    if (layer.strict() && attempt.started) {
                        repeats[l] = count;
                        return fail(attempt.failure.problem, attempt.failure.at, attempt.failure.symbol, null, roles, repeats);
                    }
                    break;
                }
                repeats[l] = count;
                return fail(attempt.failure.problem, attempt.failure.at, attempt.failure.symbol, null, roles, repeats);
            }
            repeats[l] = count;
            Match unmet = requirements(level, controllerPos, facing, roles, repeats, l);
            if (unmet != null) return unmet;
        }
        Match unmet = requirements(level, controllerPos, facing, roles, repeats, -1);
        if (unmet != null) return unmet;
        return new Match(null, BlockPos.ZERO, ' ', null, roles, repeats);
    }

    private record Failure(String problem, BlockPos at, char symbol) {}

    private static final class Attempt {
        @Nullable Failure failure;
        boolean started;
    }

    private Attempt layer(Level level, BlockPos controllerPos, Direction facing, Layer layer, int y, Map<String, List<BlockPos>> have,
            Map<String, List<BlockPos>> found) {
        Attempt attempt = new Attempt();
        for (int r = 0; r < layer.rows().size(); r++) {
            String row = layer.rows().get(r);
            for (int c = 0; c < row.length(); c++) {
                char ch = row.charAt(c);
                if (skipped(ch)) continue;
                Symbol symbol = symbols.get(ch);
                BlockPos pos = at(controllerPos, facing, r, c, y);
                BlockState state = level.getBlockState(pos);
                Entry hit = accepted(level, pos, state, symbol, have, found);
                if (hit == null) {
                    if (attempt.failure == null) attempt.failure = new Failure(symbol.problem(), pos, ch);
                    continue;
                }
                if (hit.solid()) attempt.started = true;
                for (String role : hit.roles()) found.computeIfAbsent(role, x -> new ArrayList<>()).add(pos.immutable());
            }
        }
        return attempt;
    }

    /** The entry of a symbol that takes this block, skipping role entries whose role is full. */
    private @Nullable Entry accepted(Level level, BlockPos pos, BlockState state, Symbol symbol, Map<String, List<BlockPos>> have,
            @Nullable Map<String, List<BlockPos>> found) {
        for (Entry entry : symbol.accepts()) {
            if (!entry.accepts(level, pos, state)) continue;
            boolean full = false;
            for (String role : entry.roles()) {
                Optional<Integer> max = maxOf(role);
                if (max.isEmpty()) continue;
                int n = have.getOrDefault(role, List.of()).size() + (found == null ? 0 : found.getOrDefault(role, List.of()).size());
                if (n >= max.get()) full = true;
            }
            if (!full) return entry;
        }
        return null;
    }

    private Optional<Integer> maxOf(String role) {
        for (Requirement requirement : requirements) if (requirement.role().equals(role) && requirement.max().isPresent()) return requirement.max();
        return Optional.empty();
    }

    private @Nullable Match requirements(Level level, BlockPos controllerPos, Direction facing, Map<String, List<BlockPos>> roles, int[] repeats, int layer) {
        for (Requirement requirement : requirements) {
            if (requirement.layer().orElse(-1) != layer) continue;
            if (roles.getOrDefault(requirement.role(), List.of()).size() >= requirement.min()) continue;
            BlockPos hint = hint(level, controllerPos, facing, requirement.role(), layer < 0 ? layers.size() - 1 : layer, repeats);
            return fail(requirement.problem(), hint, ' ', requirement.role(), roles, repeats);
        }
        return null;
    }

    /**
     * Where a block of a role could go: the first cell that may hold it and holds a plain block now, else the
     * first cell that may hold it, else the controller.
     */
    private BlockPos hint(Level level, BlockPos controllerPos, Direction facing, String role, int lastLayer, int[] repeats) {
        BlockPos first = null;
        for (Cell cell : cells(controllerPos, facing, repeats, lastLayer)) {
            Symbol symbol = symbols.get(cell.symbol());
            boolean may = symbol.accepts().stream().anyMatch(e -> e.roles().contains(role));
            if (!may) continue;
            if (first == null) first = cell.pos();
            BlockState state = level.getBlockState(cell.pos());
            boolean plain = symbol.accepts().stream().anyMatch(e -> e.roles().isEmpty() && e.accepts(level, cell.pos(), state));
            if (plain) return cell.pos();
        }
        return first != null ? first : controllerPos;
    }

    // ---- the cells

    /** Every non-blank cell in layers up to {@code lastLayer}, each layer repeated as often as {@code repeats} (at least its min) says. */
    public List<Cell> cells(BlockPos controllerPos, Direction facing, int[] repeats, int lastLayer) {
        List<Cell> cells = new ArrayList<>();
        int y = firstY();
        for (int l = 0; l <= lastLayer && l < layers.size(); l++) {
            Layer layer = layers.get(l);
            int count = Math.max(layer.min(), repeats == null ? 0 : repeats[l]);
            for (int k = 0; k < count; k++) {
                for (int r = 0; r < layer.rows().size(); r++) {
                    String row = layer.rows().get(r);
                    for (int c = 0; c < row.length(); c++) {
                        char ch = row.charAt(c);
                        if (!skipped(ch)) cells.add(new Cell(at(controllerPos, facing, r, c, y), ch, l));
                    }
                }
                y++;
            }
        }
        return cells;
    }

    /** A block of the standard build and where it goes. */
    public record Part(BlockPos pos, BlockState state) {}

    /**
     * The blocks of the standard build around a controller, lowest layer first: every layer as often as its
     * minimum, each symbol as its preview says. The controller itself is not in it.
     */
    public List<Part> parts(BlockPos controllerPos, Direction facing) {
        List<Part> parts = new ArrayList<>();
        int[] mins = new int[layers.size()];
        for (int l = 0; l < mins.length; l++) mins[l] = layers.get(l).min();
        for (Cell cell : cells(controllerPos, facing, mins, layers.size() - 1)) {
            Optional<Preview> preview = symbols.get(cell.symbol()).preview();
            if (preview.isPresent()) parts.add(new Part(cell.pos(), preview.get().state(facing)));
        }
        return parts;
    }

    // ---- the ghost

    /** What a cell lacks. */
    public enum Fault { MISSING, WRONG }

    /** A cell that does not hold what the pattern wants, with the block to show there (null when the cell must just be clear). */
    public record Mismatch(BlockPos pos, @Nullable BlockState expected, Fault fault) {}

    /**
     * Every cell around a controller that is missing or wrong, for the ghost preview. Layers repeat as often
     * as they do now, at least their minimum; a role the structure is short of is asked of the first cell
     * whose preview carries it.
     */
    public List<Mismatch> mismatches(Level level, BlockPos controllerPos, Direction facing) {
        Match match = check(level, controllerPos, facing);
        List<Mismatch> result = new ArrayList<>();
        Map<String, List<BlockPos>> none = Map.of();
        for (Cell cell : cells(controllerPos, facing, match.repeats(), layers.size() - 1)) {
            Symbol symbol = symbols.get(cell.symbol());
            BlockState state = level.getBlockState(cell.pos());
            if (accepted(level, cell.pos(), state, symbol, none, null) != null) continue;
            result.add(new Mismatch(cell.pos(), symbol.preview().map(p -> p.state(facing)).orElse(null), fault(state)));
        }
        if (match.role() != null) {
            for (Cell cell : cells(controllerPos, facing, match.repeats(), layers.size() - 1)) {
                Symbol symbol = symbols.get(cell.symbol());
                if (symbol.preview().isEmpty() || result.stream().anyMatch(m -> m.pos().equals(cell.pos()))) continue;
                BlockState wanted = symbol.preview().get().state(facing);
                boolean carries = symbol.accepts().stream().anyMatch(e -> e.roles().contains(match.role()) && e.accepts(level, cell.pos(), wanted));
                if (!carries) continue;
                BlockState state = level.getBlockState(cell.pos());
                boolean holds = symbol.accepts().stream().anyMatch(e -> e.roles().contains(match.role()) && e.accepts(level, cell.pos(), state));
                if (holds) continue;
                result.add(new Mismatch(cell.pos(), wanted, fault(state)));
                break;
            }
        }
        return result;
    }

    private static Fault fault(BlockState state) {
        return state.isAir() || state.canBeReplaced() ? Fault.MISSING : Fault.WRONG;
    }

    /**
     * The first thing a builder should fix, for the "Structure incomplete" message: the block to place (null
     * for clearing space) and where. Null when the structure is whole.
     */
    public @Nullable Mismatch firstMismatch(Level level, BlockPos controllerPos, Direction facing) {
        Match match = check(level, controllerPos, facing);
        if (match.complete()) return null;
        List<Mismatch> all = mismatches(level, controllerPos, facing);
        for (Mismatch mismatch : all) if (mismatch.pos().equals(match.at())) return mismatch;
        return all.isEmpty() ? new Mismatch(match.at(), null, Fault.WRONG) : all.getFirst();
    }
}
