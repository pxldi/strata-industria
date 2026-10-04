package dev.strataindustria.electric;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.power.ElectricNetworks;
import dev.strataindustria.power.ElectricTier;
import dev.strataindustria.registry.Tier5DataComponents;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/** A battery box as an item; it shows the charge it was picked up with and places at the tier it was picked up at (spec 7.4, 9.5). */
public class BatteryBoxItem extends MachineBlockItem {
    public BatteryBoxItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        ElectricTier tier = tierOf(stack);
        int stored = stack.getOrDefault(Tier5DataComponents.ENERGY.get(), 0);
        tooltip.accept(Component.translatable(StrataIndustria.MOD_ID + ".battery_box.charge", tier.label(), ElectricNetworks.joules(stored),
                ElectricNetworks.joules(BatteryBoxBlockEntity.capacityOf(tier))).withStyle(ChatFormatting.GRAY));
    }
}
