package dev.strataindustria.cabinet;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** The specimens in a cabinet: at most one of each rock and mineral. */
public class SpecimenCabinetBlockEntity extends BlockEntity {
    private static final Codec<java.util.List<String>> LIST = Codec.STRING.listOf();

    private final Set<String> held = new LinkedHashSet<>();

    public SpecimenCabinetBlockEntity(BlockPos pos, BlockState state) {
        super(CabinetRegistry.CABINET_ENTITY.get(), pos, state);
    }

    public Set<String> held() {
        return java.util.Collections.unmodifiableSet(held);
    }

    public boolean holds(String specimen) {
        return held.contains(specimen);
    }

    /** Sets a specimen on its shelf. Returns false if one of that kind is already there. */
    public boolean add(String specimen) {
        if (!held.add(specimen)) return false;
        setChanged();
        return true;
    }

    /** How full the cabinet looks from the front, 0 to {@link SpecimenCabinetBlock#MAX_FILL}. */
    public int fill() {
        int total = Specimens.total();
        if (held.isEmpty()) return 0;
        if (held.size() >= total) return SpecimenCabinetBlock.MAX_FILL;
        return Math.max(1, Math.min(SpecimenCabinetBlock.MAX_FILL - 1, held.size() * SpecimenCabinetBlock.MAX_FILL / total));
    }

    /** The cabinet keeps its collection on the item when it is picked up. */
    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!held.isEmpty()) components.set(CabinetRegistry.HELD.get(), java.util.List.copyOf(held));
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        held.clear();
        held.addAll(components.getOrDefault(CabinetRegistry.HELD.get(), java.util.List.of()));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        held.clear();
        held.addAll(input.read("held", LIST).orElse(java.util.List.of()));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("held", LIST, new ArrayList<>(held));
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
