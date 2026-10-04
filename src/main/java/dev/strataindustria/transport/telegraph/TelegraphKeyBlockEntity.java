package dev.strataindustria.transport.telegraph;

import dev.strataindustria.transport.outpost.Charter;
import dev.strataindustria.transport.outpost.Link;
import dev.strataindustria.transport.outpost.LinkKind;
import dev.strataindustria.transport.outpost.OutpostPlan;
import dev.strataindustria.transport.outpost.RouteIndex;
import dev.strataindustria.transport.rail.TramwayRoutes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * A telegraph key: it sends when pressed, and every 30 seconds it reports its charter to the dispatch boards on its
 * line (outposts spec 9.1): how the charter is loaded, how its line stands, and how full the container in front is.
 */
public class TelegraphKeyBlockEntity extends BlockEntity {
    /** Ticks between reports. */
    public static final int REPORT_EVERY = 600;
    /** A charter with no traffic for this long reads "idle": ten minutes. */
    public static final long IDLE_AFTER = 12000;
    private static final int FIRST_REPORT = 40;

    private boolean wasPowered;
    private long lastPress = -100;
    private long nextReport = -1;

    public TelegraphKeyBlockEntity(BlockPos pos, BlockState state) {
        super(TelegraphRegistry.KEY_ENTITY.get(), pos, state);
    }

    public boolean wasPowered() {
        return wasPowered;
    }

    public void setWasPowered(boolean powered) {
        wasPowered = powered;
        setChanged();
    }

    /** Sends a call: the lever goes down, the line hears it, and the player is told how it went. */
    public void press(ServerLevel level, BlockState state, ServerPlayer by) {
        long now = level.getGameTime();
        if (now - lastPress < TelegraphKeyBlock.HOLD) return;
        lastPress = now;
        level.setBlock(worldPosition, state.setValue(TelegraphKeyBlock.PRESSED, true), Block.UPDATE_ALL);
        level.scheduleTick(worldPosition, state.getBlock(), TelegraphKeyBlock.HOLD);
        float pitch = 0.92f + level.getRandom().nextFloat() * 0.16f;
        level.playSound(null, worldPosition, TelegraphRegistry.KEY_DOWN.get(), SoundSource.BLOCKS, 0.8f, pitch);
        Vec3 lever = Vec3.atCenterOf(worldPosition).add(0, 0.1, 0);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, lever.x, lever.y, lever.z, 3, 0.1, 0.05, 0.1, 0.02);
        RouteIndex routes = RouteIndex.get(level);
        int before = routes.links().size();
        TelegraphLine.Call call = TelegraphLine.send(level, worldPosition, by);
        if (routes.links().size() > before) {
            level.playSound(null, worldPosition, TelegraphRegistry.LINE_OPEN.get(), SoundSource.BLOCKS, 0.8f, 1.0f);
        }
        if (by != null) {
            String suffix = !call.attached() ? "key.no_pole" : call.sounders() == 0 ? "key.silent" : "key.sent";
            by.sendOverlayMessage(Component.translatable(TelegraphLine.key(suffix), call.sounders(), TelegraphIndex.DROP_REACH));
        }
        if (nextReport < 0 || nextReport > now + FIRST_REPORT) nextReport = now + FIRST_REPORT;
    }

    public void serverTick(ServerLevel level, BlockState state) {
        long now = level.getGameTime();
        if (nextReport < 0) {
            TelegraphIndex index = TelegraphIndex.get(level);
            index.register(worldPosition, TelegraphIndex.Kind.KEY);
            nextReport = now + FIRST_REPORT + Math.floorMod(worldPosition.hashCode(), 40);
        }
        if (now < nextReport) return;
        nextReport = now + REPORT_EVERY;
        TelegraphIndex index = TelegraphIndex.get(level);
        if (index.dropOf(worldPosition) == null) {
            BlockPos pole = index.attach(level, worldPosition, TelegraphIndex.Kind.KEY);
            if (pole != null) TelegraphLine.dropConnected(level, worldPosition, pole);
        }
        if (index.dropOf(worldPosition) == null) return;
        Charter charter = TramwayRoutes.stationCharter(RouteIndex.get(level), worldPosition);
        if (charter == null) return;
        index.report(report(level, state, charter));
    }

    /** What this key says about {@code charter} right now. */
    TelegraphIndex.Report report(ServerLevel level, BlockState state, Charter charter) {
        RouteIndex routes = RouteIndex.get(level);
        long now = level.getGameTime();
        OutpostPlan.Entry entry = routes.plan(level).entry(charter.id());
        // The best line that is not the telegraph itself: live ones by tier, else a cut one, else the telegraph alone.
        Link best = null;
        boolean telegraph = false;
        boolean anyCut = false;
        for (Link link : routes.linksOf(charter.id())) {
            if (link.kind() == LinkKind.TELEGRAPH) {
                telegraph |= routes.live(link);
                continue;
            }
            if (!routes.live(link)) {
                if (best == null && !link.open()) {
                    best = link;
                    anyCut = true;
                }
                continue;
            }
            if (best == null || anyCut || link.kind().tier() > best.kind().tier()) {
                best = link;
                anyCut = false;
            }
        }
        int line = best != null ? (anyCut ? 1 : 0) : telegraph ? 2 : 3;
        String kind = best == null ? "" : best.kind().langKey();
        long traffic = best == null || anyCut ? -1 : best.lastTraffic();
        int status;
        if (entry == null || entry.state() == OutpostPlan.State.NO_LINE) status = 2;
        else if (entry.state() == OutpostPlan.State.OWNER_AWAY) status = 3;
        else if (entry.state() == OutpostPlan.State.CHUNK_LIMIT) status = 4;
        else status = traffic >= 0 && now - traffic <= IDLE_AFTER ? 0 : 1;
        return new TelegraphIndex.Report(worldPosition.immutable(), charter.id(), charter.name(), status, line, kind, fill(level, state), traffic, now);
    }

    /** How full the container in front of the key is, 0 to 100, or -1 when there is none. */
    private int fill(ServerLevel level, BlockState state) {
        Container container = HopperBlockEntity.getContainerAt(level, worldPosition.relative(state.getValue(TelegraphKeyBlock.FACING)));
        if (container == null || container.getContainerSize() == 0) return -1;
        double filled = 0;
        int size = container.getContainerSize();
        for (int slot = 0; slot < size; slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty()) filled += stack.getCount() / (double) Math.min(stack.getMaxStackSize(), container.getMaxStackSize());
        }
        return (int) Math.min(100, Math.round(100.0 * filled / size));
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        wasPowered = in.getBooleanOr("was_powered", false);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putBoolean("was_powered", wasPowered);
    }
}
