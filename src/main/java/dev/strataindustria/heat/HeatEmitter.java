package dev.strataindustria.heat;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * What a heat source other than the firebox shares: the consumers it reaches (the block above, heat inlets
 * on its other faces, then everything its pipes reach) and the glow of the pipes while it is hot. The
 * electric heater and the liquid-fuel burner (tier 5 spec 7.5 and 7.6) are firebox replacements and offer
 * heat the same way.
 */
public final class HeatEmitter {
    private HeatNetwork.Routes routes = HeatNetwork.Routes.NONE;
    private int routesVersion = -1;
    private int pipeFaces = -1;
    private boolean pipesHot;

    public boolean pipesHot() {
        return pipesHot;
    }

    public void setPipesHot(boolean hot) {
        pipesHot = hot;
    }

    /** The block above, heat inlets on the other faces, then whatever the pipes reach. */
    public List<HeatNetwork.Route> targets(Level level, BlockPos pos) {
        List<HeatNetwork.Route> targets = new ArrayList<>();
        BlockPos above = pos.above();
        if (level.getBlockEntity(above) instanceof HeatConsumer) targets.add(HeatNetwork.Route.direct(above));
        for (Direction side : Direction.values()) {
            if (side == Direction.UP) continue;
            BlockPos next = pos.relative(side);
            if (level.getBlockEntity(next) instanceof HeatInletBlockEntity) targets.add(HeatNetwork.Route.direct(next));
        }
        for (HeatNetwork.Route route : routes.routes()) {
            boolean touching = false;
            for (HeatNetwork.Route direct : targets) touching |= direct.pos().equals(route.pos());
            if (!touching) targets.add(route);
        }
        return targets;
    }

    /** Whether any consumer it reaches would take heat from a source at {@code temperature} °C. */
    public boolean wants(Level level, List<HeatNetwork.Route> targets, float temperature) {
        for (HeatNetwork.Route route : targets) {
            if (level.getBlockEntity(route.pos()) instanceof HeatConsumer consumer && consumer.heatDemand(route.temperature(temperature)) > 0) return true;
        }
        return false;
    }

    /** Looks for the consumers on its pipes again when a pipe joined or left anything since the last look. */
    public void refresh(Level level, BlockPos pos) {
        int faces = 0;
        for (Direction side : Direction.values()) {
            if (level.getBlockState(pos.relative(side)).getBlock() instanceof HeatPipeBlock) faces |= 1 << side.ordinal();
        }
        int version = HeatNetwork.version();
        if (faces == pipeFaces && version == routesVersion) return;
        pipeFaces = faces;
        routesVersion = version;
        HeatNetwork.Routes found = faces == 0 ? HeatNetwork.Routes.NONE : HeatNetwork.find(level, pos);
        if (pipesHot) {
            // Pipes cut off from the source cool; newly joined ones light up.
            List<BlockPos> cut = new ArrayList<>(routes.pipes());
            cut.removeAll(found.pipes());
            HeatNetwork.glow(level, cut, false);
            HeatNetwork.glow(level, found.pipes(), true);
        }
        routes = found;
    }

    /** Lights or darkens the pipes it reaches when it passes {@link HeatPipeBlock#GLOWS_FROM}. */
    public void glow(Level level, boolean hot) {
        if (hot == pipesHot) return;
        pipesHot = hot;
        HeatNetwork.glow(level, routes.pipes(), hot);
    }

    /** The pipes it was heating cool when it goes. */
    public void release(Level level) {
        if (pipesHot) HeatNetwork.glow(level, routes.pipes(), false);
    }
}
