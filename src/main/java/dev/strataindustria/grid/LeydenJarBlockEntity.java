package dev.strataindustria.grid;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.power.ElectricNetworks;
import dev.strataindustria.power.ElectricStorage;
import dev.strataindustria.power.ElectricTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A Leyden jar's charge. It holds 25 000 J and gives 32 J/t to its network like an LV battery, but the network never
 * charges it: only a lightning mast does ({@link LightningHarvest}), which is why a storm is worth waiting for.
 */
public class LeydenJarBlockEntity extends BlockEntity implements ElectricStorage {
    public static final int CAPACITY = 25_000;

    private double stored;

    public LeydenJarBlockEntity(BlockPos pos, BlockState state) {
        super(GridBlocks.LEYDEN_JAR_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LeydenJarBlockEntity jar) {
        if (level.getGameTime() % 10 != 0) return;
        BlockState next = state.setValue(LeydenJarBlock.CHARGE, jar.segments());
        if (next != state) level.setBlock(pos, next, Block.UPDATE_ALL);
    }

    /** Lit segments: none when empty, all four only when full. */
    public int segments() {
        if (stored <= 0) return 0;
        return Math.min(LeydenJarBlock.SEGMENTS, (int) Math.ceil(stored / CAPACITY * LeydenJarBlock.SEGMENTS - 1e-9));
    }

    /** Puts charge in directly, from a lightning strike. Returns how much it took. */
    public double charge(double joules) {
        double taken = Math.max(0, Math.min(joules, CAPACITY - stored));
        stored += taken;
        if (taken > 0) setChanged();
        return taken;
    }

    public void setStored(double joules) {
        stored = Math.max(0, Math.min(CAPACITY, joules));
        setChanged();
    }

    public Component chargeLine() {
        return Component.translatable(StrataIndustria.MOD_ID + ".leyden_jar.charge", ElectricNetworks.joules(stored), ElectricNetworks.joules(CAPACITY));
    }

    @Override
    public ElectricTier tier() {
        return ElectricTier.LV;
    }

    @Override
    public boolean connectsElectric(Direction side) {
        return true;
    }

    @Override
    public double stored() {
        return stored;
    }

    @Override
    public double capacity() {
        return CAPACITY;
    }

    @Override
    public double maxOutput() {
        return Math.min(tier().maxPower(), stored);
    }

    @Override
    public void extract(double amount) {
        stored = Math.max(0, stored - amount);
        if (amount > 0) setChanged();
    }

    /** The network never charges a jar, so it asks for nothing. */
    @Override
    public double request() {
        return 0;
    }

    @Override
    public void receive(double amount) {}

    @Override
    public void onLoad() {
        super.onLoad();
        ElectricNetworks.markDirty(level, worldPosition);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        ElectricNetworks.markDirty(level, worldPosition);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        stored = in.getDoubleOr("stored", 0.0);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putDouble("stored", stored);
    }
}
