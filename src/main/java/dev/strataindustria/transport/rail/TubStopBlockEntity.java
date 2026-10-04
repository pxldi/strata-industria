package dev.strataindustria.transport.rail;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A tub stop (outposts spec 6.3): its name, its departure rule and what it is holding. The lead of a consist that
 * reaches the stop is captured and held still; this entity watches the rule each tick and lets it go.
 */
public class TubStopBlockEntity extends BlockEntity {
    /** Speed a released consist is given, in the direction it leaves. */
    public static final double START_PUSH = 0.05;

    private String name = "";
    private StopRule rule = StopRule.WAIT;
    private int seconds = StopData.DEFAULT_SECONDS;
    private boolean reverse = true;

    private @Nullable UUID held;
    private long heldSince;
    private long lastChange;
    private int contentHash;
    private Vec3 arrival = Vec3.ZERO;
    private boolean pulsed;

    public TubStopBlockEntity(BlockPos pos, BlockState state) {
        super(RailRegistry.TUB_STOP_ENTITY.get(), pos, state);
    }

    // ---------------------------------------------------------------- settings

    public StopData data() {
        return new StopData(worldPosition, name, rule, seconds, reverse);
    }

    public void apply(StopData data) {
        name = StopData.cleanName(data.name());
        rule = data.rule();
        seconds = StopData.cleanSeconds(data.seconds());
        reverse = data.reverse();
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public String name() {
        return name;
    }

    public StopRule rule() {
        return rule;
    }

    public boolean reverse() {
        return reverse;
    }

    // ---------------------------------------------------------------- holding

    public boolean isHolding() {
        return held != null;
    }

    public @Nullable UUID heldLead() {
        return held;
    }

    /** The lead of a consist has reached the stop: hold it and note the arrival. */
    public void capture(ServerLevel server, MineTubEntity lead) {
        if (held != null) return;
        Vec3 motion = lead.getDeltaMovement();
        Vec3 flat = new Vec3(motion.x, 0, motion.z);
        arrival = flat.lengthSqr() > 1.0E-4 ? flat.normalize() : Vec3.ZERO;
        held = lead.getUUID();
        heldSince = server.getGameTime();
        lastChange = heldSince;
        contentHash = contentHash(lead.consist());
        pulsed = false;
        lead.holdAt(worldPosition);
        server.playSound(null, worldPosition, RailRegistry.TUB_STOP_BRAKE.get(), SoundSource.BLOCKS, 0.8f, 0.9f + 0.2f * server.getRandom().nextFloat());
        TramwayRoutes.arrived(server, lead, worldPosition);
        server.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
    }

    /** A redstone pulse: let the consist go at once. */
    public void pulse() {
        pulsed = true;
    }

    public void serverTick(ServerLevel server) {
        if (held == null) return;
        Entity entity = server.getEntity(held);
        if (!(entity instanceof MineTubEntity lead) || lead.isRemoved() || !lead.isHeldAt(worldPosition)) {
            held = null;
            server.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            return;
        }
        List<MineTubEntity> consist = lead.consist();
        long now = server.getGameTime();
        int hash = contentHash(consist);
        if (hash != contentHash) {
            contentHash = hash;
            lastChange = now;
        }
        if ((pulsed || ruleMet(server, consist, now)) && lead.canDepart(server)) release(server, lead);
        if (now % 4 == 0) server.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
    }

    private boolean ruleMet(ServerLevel server, List<MineTubEntity> consist, long now) {
        return switch (rule) {
            case WAIT -> now - heldSince >= seconds * 20L;
            case FULL -> consist.stream().allMatch(tub -> tub.cannotTakeMore(server));
            case EMPTY -> consist.stream().allMatch(MineTubEntity::isEmpty);
            case REDSTONE -> false;
            case IDLE -> now - lastChange >= seconds * 20L;
        };
    }

    /** Lets the consist go: on in the direction it came, or back the way it came with "reverse here" on. */
    private void release(ServerLevel server, MineTubEntity lead) {
        Vec3 direction = arrival;
        if (direction.lengthSqr() < 1.0E-4) direction = lead.railAxis(worldPosition);
        else if (reverse) direction = direction.scale(-1);
        held = null;
        pulsed = false;
        lead.release(worldPosition, direction.normalize().scale(START_PUSH));
        server.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
    }

    private static int contentHash(List<MineTubEntity> consist) {
        int hash = 1;
        for (MineTubEntity tub : consist) {
            for (int slot = 0; slot < tub.getContainerSize(); slot++) {
                ItemStack stack = tub.getItem(slot);
                hash = hash * 31 + (stack.isEmpty() ? 0 : BuiltInRegistries.ITEM.getId(stack.getItem()) * 64 + stack.getCount());
            }
        }
        return hash;
    }

    /** 0 to 15 for how full the consist standing here is, as a comparator reads it. */
    public int fill(Level level) {
        if (held == null || !(level instanceof ServerLevel server) || !(server.getEntity(held) instanceof MineTubEntity lead)) return 0;
        double total = 0;
        int slots = 0;
        for (MineTubEntity tub : lead.consist()) {
            for (int slot = 0; slot < tub.getContainerSize(); slot++) {
                ItemStack stack = tub.getItem(slot);
                slots++;
                if (!stack.isEmpty()) total += (float) stack.getCount() / Math.min(tub.getMaxStackSize(), stack.getMaxStackSize());
            }
        }
        if (slots == 0 || total == 0) return 0;
        return (int) Math.floor(total / slots * 14.0) + 1;
    }

    // ---------------------------------------------------------------- saving

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        name = StopData.cleanName(input.getStringOr("name", ""));
        rule = input.read("rule", StopRule.CODEC).orElse(StopRule.WAIT);
        seconds = StopData.cleanSeconds(input.getIntOr("seconds", StopData.DEFAULT_SECONDS));
        reverse = input.getBooleanOr("reverse", true);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("name", name);
        output.store("rule", StopRule.CODEC, rule);
        output.putInt("seconds", seconds);
        output.putBoolean("reverse", reverse);
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
