package dev.strataindustria.electric;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.power.ElectricTier;
import dev.strataindustria.registry.Tier5DataComponents;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** An electric machine as an item: an upgraded one keeps its {@code machine_tier} and places back as MV (spec 9.5). */
public class MachineBlockItem extends BlockItem {
    public MachineBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    /** The tier a stack of a machine places at: LV unless it was picked up upgraded. */
    public static ElectricTier tierOf(ItemStack stack) {
        return stack.getOrDefault(Tier5DataComponents.MACHINE_TIER.get(), ElectricTier.LV);
    }

    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        if (state == null || !state.hasProperty(ElectricTier.PROPERTY)) return state;
        return state.setValue(ElectricTier.PROPERTY, tierOf(context.getItemInHand()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        if (tierOf(stack) == ElectricTier.MV) {
            tooltip.accept(Component.translatable(StrataIndustria.MOD_ID + ".machine.upgraded", ElectricTier.MV.label()).withStyle(ChatFormatting.GRAY));
        }
    }
}
