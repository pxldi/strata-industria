package dev.strataindustria.bronze;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Remembers how the bell was cast, and keeps it on the item when the bell is taken down. */
public class BellBlockEntity extends BlockEntity {
    private BellTone tone = BellTone.DEFAULT;

    public BellBlockEntity(BlockPos pos, BlockState state) {
        super(BronzeRegistry.BELL_ENTITY.get(), pos, state);
    }

    public BellTone tone() {
        return tone;
    }

    public void setTone(BellTone tone) {
        this.tone = tone;
        setChanged();
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(BronzeRegistry.BELL_TONE.get(), tone);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        tone = components.getOrDefault(BronzeRegistry.BELL_TONE.get(), BellTone.DEFAULT);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tone = input.read("tone", BellTone.CODEC).orElse(BellTone.DEFAULT);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("tone", BellTone.CODEC, tone);
    }
}
