package dev.strataindustria.structure;

import dev.strataindustria.StrataIndustria;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;

/** The cart's load. The block state shows how many slots are in use, so the cart looks as full as it is. */
public class OreCartBlockEntity extends NineSlotBlockEntity {
    public OreCartBlockEntity(BlockPos pos, BlockState state) {
        super(SharedBlocks.ORE_CART_ENTITY.get(), pos, state);
    }

    /** Fills the cart from a loot table when it is first opened, and shows it heaped until then. */
    public static void stock(ServerLevel level, BlockPos pos, ResourceKey<LootTable> table) {
        if (level.getBlockEntity(pos) instanceof OreCartBlockEntity cart) {
            cart.setLootTable(table);
            cart.setChanged();
            BlockState state = level.getBlockState(pos);
            level.setBlock(pos, state.setValue(OreCartBlock.FILL, 2), 2);
        }
    }

    /** How full the cart looks: 0 for empty, then a third of the slots at a time. */
    public static int fill(int usedSlots) {
        return usedSlots == 0 ? 0 : Math.min(3, (usedSlots + 2) / 3);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level == null || level.isClientSide() || lootTable != null) return;
        int used = 0;
        for (ItemStack stack : getItems()) if (!stack.isEmpty()) used++;
        BlockState state = getBlockState();
        if (state.hasProperty(OreCartBlock.FILL) && state.getValue(OreCartBlock.FILL) != fill(used)) {
            level.setBlock(worldPosition, state.setValue(OreCartBlock.FILL, fill(used)), 3);
        }
    }

    @Override
    protected Component name() {
        return Component.translatable("block." + StrataIndustria.MOD_ID + ".ore_cart");
    }

    @Override
    protected SoundEvent openSound() {
        return SharedBlocks.ORE_CART_RATTLE.get();
    }

    @Override
    protected SoundEvent closeSound() {
        return SharedBlocks.ORE_CART_RATTLE.get();
    }
}
