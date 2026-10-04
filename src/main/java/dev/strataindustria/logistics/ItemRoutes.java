package dev.strataindustria.logistics;

import dev.strataindustria.Config;
import dev.strataindustria.automation.InserterBlockEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Where an item goes (tier 5 spec 12.1): the nearest inventory on the pipe network that takes it, by the
 * shortest pipe path. Inventories at the same distance take turns.
 */
public final class ItemRoutes {
    private ItemRoutes() {}

    /**
     * A way through the network.
     *
     * @param path        the direction to leave each pipe by, from the first pipe to the last; the last one points into the inventory
     * @param destination the pipe the item leaves the network from
     */
    public record Route(List<Direction> path, BlockPos destination) {
        public Direction exit() {
            return path.get(path.size() - 1);
        }
    }

    private record Step(BlockPos from, Direction by) {}

    private record Candidate(BlockPos pipe, Direction side) {}

    /**
     * The route for {@code stack} from {@code start}, or null when no inventory takes it. {@code noReturn} is the
     * face of the first pipe that the item came from, which it does not go back into; {@code turn} picks among
     * inventories at the same distance.
     */
    public static @Nullable Route find(ServerLevel level, BlockPos start, ItemStack stack, @Nullable Direction noReturn, int turn) {
        int budget = Config.LOGISTICS_MAX_PIPE_NETWORK.get();
        Map<BlockPos, Step> parent = new HashMap<>();
        Set<BlockPos> seen = new HashSet<>();
        List<BlockPos> layer = new ArrayList<>();
        layer.add(start);
        seen.add(start);
        while (!layer.isEmpty()) {
            List<Candidate> found = new ArrayList<>();
            for (BlockPos pos : layer) {
                BlockState state = level.getBlockState(pos);
                if (!(level.getBlockEntity(pos) instanceof ItemPipeBlockEntity pipe)) continue;
                for (Direction side : Direction.values()) {
                    if (ItemPipeBlock.face(state, side) != ItemPipeBlock.Face.PORT) continue;
                    if (noReturn == side && pos.equals(start)) continue;
                    if (!pipe.allows(side, stack)) continue;
                    if (accepts(level, pos.relative(side), side.getOpposite(), stack)) found.add(new Candidate(pos, side));
                }
            }
            if (!found.isEmpty()) {
                Candidate pick = found.get(Math.floorMod(turn, found.size()));
                List<Direction> path = new ArrayList<>();
                path.add(pick.side());
                BlockPos at = pick.pipe();
                while (!at.equals(start)) {
                    Step step = parent.get(at);
                    path.add(0, step.by());
                    at = step.from();
                }
                return new Route(List.copyOf(path), pick.pipe());
            }
            List<BlockPos> next = new ArrayList<>();
            for (BlockPos pos : layer) {
                BlockState state = level.getBlockState(pos);
                if (!(state.getBlock() instanceof ItemPipeBlock)) continue;
                for (Direction side : Direction.values()) {
                    if (ItemPipeBlock.face(state, side) != ItemPipeBlock.Face.PIPE) continue;
                    BlockPos far = pos.relative(side);
                    if (!(level.getBlockEntity(far) instanceof ItemPipeBlockEntity) || !seen.add(far)) continue;
                    if (seen.size() > budget) continue;
                    parent.put(far, new Step(pos, side));
                    next.add(far);
                }
            }
            layer = next;
        }
        return null;
    }

    /** Whether the inventory at {@code pos} would take some of {@code stack} through {@code face}. */
    static boolean accepts(ServerLevel level, BlockPos pos, Direction face, ItemStack stack) {
        Container container = HopperBlockEntity.getContainerAt(level, pos);
        return container != null && InserterBlockEntity.fits(container, stack, face);
    }
}
