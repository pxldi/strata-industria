package dev.strataindustria.electric;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.power.ElectricNetworks;
import dev.strataindustria.power.ElectricStorage;
import dev.strataindustria.power.ElectricTier;
import dev.strataindustria.registry.Tier5BlockEntities;
import dev.strataindustria.registry.Tier5DataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Spec 7.4: LV holds 100 000 J and moves 32 J/t each way; MV holds 400 000 J at 128 J/t. */
public class BatteryBoxBlockEntity extends BlockEntity implements ElectricStorage {
    private double stored;
    private double charged;

    public BatteryBoxBlockEntity(BlockPos pos, BlockState state) {
        super(Tier5BlockEntities.BATTERY_BOX.get(), pos, state);
    }

    public static int capacityOf(ElectricTier tier) {
        return tier == ElectricTier.MV ? 400_000 : 100_000;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BatteryBoxBlockEntity box) {
        BlockState next = state.setValue(BatteryBoxBlock.CHARGE, box.segments()).setValue(BatteryBoxBlock.CHARGING, box.charged > 0);
        box.charged = 0;
        if (next != state) level.setBlock(pos, next, Block.UPDATE_CLIENTS);
    }

    /** Lit meter segments: none when empty, all five only when full, so a nearly full box still shows room. */
    public int segments() {
        if (stored <= 0) return 0;
        return Math.min(BatteryBoxBlock.SEGMENTS, (int) Math.ceil(stored / capacity() * BatteryBoxBlock.SEGMENTS - 1e-9));
    }

    /** "41 000 / 100 000 J". */
    public Component chargeLine() {
        return Component.translatable(StrataIndustria.MOD_ID + ".battery_box.charge", tier().label(), ElectricNetworks.joules(stored),
                ElectricNetworks.joules(capacity()));
    }

    /** Sets the charge directly, for game tests. */
    public void setStored(double joules) {
        stored = Math.max(0, Math.min(capacity(), joules));
        setChanged();
    }

    @Override
    public ElectricTier tier() {
        return getBlockState().getValue(BatteryBoxBlock.TIER);
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
        return capacityOf(tier());
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

    @Override
    public double request() {
        return Math.min(tier().maxPower(), capacity() - stored);
    }

    @Override
    public void receive(double amount) {
        stored = Math.min(capacity(), stored + amount);
        charged += amount;
        if (amount > 0) setChanged();
    }

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

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (stored >= 1) components.set(Tier5DataComponents.ENERGY.get(), (int) Math.floor(stored));
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        stored = components.getOrDefault(Tier5DataComponents.ENERGY.get(), 0);
    }
}
