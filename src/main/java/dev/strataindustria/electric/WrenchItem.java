package dev.strataindustria.electric;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.electric.machine.ChemicalMachineBlock;
import dev.strataindustria.electric.machine.ChemicalMachineBlockEntity;
import dev.strataindustria.electric.machine.ElectricMachineBlock;
import dev.strataindustria.logistics.ItemPipeBlock;
import dev.strataindustria.power.ElectricNetworks;
import dev.strataindustria.power.ElectricTier;
import dev.strataindustria.registry.Tier5Blocks;
import dev.strataindustria.registry.Tier5Items;
import dev.strataindustria.registry.Tier5Sounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The wrench (spec 13.1). It has no use of its own: {@link ToolEvents} hands it the click before the block's own
 * screen or empty-hand action, so right-click turns a machine, steps an item pipe face or flips a transformer, and
 * sneak and right-click takes an MV machine back to LV and returns the kit. Anything else is left to the block.
 */
public class WrenchItem extends Item {
    public WrenchItem(Properties properties) {
        super(properties);
    }

    /** Applies the wrench to a block; PASS when it has nothing to do there. Nothing changes on the client. */
    public static InteractionResult apply(Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockState state = level.getBlockState(pos);
        if (player.isShiftKeyDown() && isMv(state)) return downgrade(level, pos, state, player);
        if (state.getBlock() instanceof TransformerBlock) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TransformerBlockEntity transformer) {
                TransformerBlock.toggle(level, pos, state, transformer, player);
            }
            return InteractionResult.SUCCESS;
        }
        if (state.getBlock() instanceof ItemPipeBlock pipe) return pipe.wrench(level, pos, state, player, hit);
        if (state.getBlock() instanceof ChemicalMachineBlock<?> && level.getBlockEntity(pos) instanceof ChemicalMachineBlockEntity machine
                && machine.layout().fluidOutputs() > 1) {
            // Spec 11.2: on a machine with several products, the front panel turns it and every other face is a port to set.
            if (hit.getDirection() != state.getValue(ElectricMachineBlock.FACING)) return setPort(level, pos, player, machine, hit.getDirection());
            return turnClockwise(level, pos, state);
        }
        if (state.getBlock() instanceof ElectricMachineBlock<?> || state.getBlock() instanceof GeneratorBlock<?>
                || state.getBlock() instanceof ChemicalMachineBlock<?> || state.getBlock() instanceof BatteryBoxBlock) {
            return turn(level, pos, state, hit);
        }
        return InteractionResult.PASS;
    }

    /** Cycles the output setting of one face: auto, product 1, 2, 3, none. */
    private static InteractionResult setPort(Level level, BlockPos pos, Player player, ChemicalMachineBlockEntity machine, Direction face) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        int mode = machine.cycleFace(face);
        level.playSound(null, pos, Tier5Sounds.WRENCH_TURN.get(), SoundSource.BLOCKS, 0.8f, 1.2f);
        Component what = mode == 0 ? Component.translatable(StrataIndustria.MOD_ID + ".port.auto")
                : mode > machine.layout().fluidOutputs() ? Component.translatable(StrataIndustria.MOD_ID + ".port.none")
                : Component.translatable(StrataIndustria.MOD_ID + ".port.product", mode);
        String name = face.getName();
        player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".port.face", Character.toUpperCase(name.charAt(0)) + name.substring(1), what));
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult turnClockwise(Level level, BlockPos pos, BlockState state) {
        if (!level.isClientSide()) {
            level.setBlock(pos, state.setValue(ElectricMachineBlock.FACING, state.getValue(ElectricMachineBlock.FACING).getClockWise()), Block.UPDATE_ALL);
            ElectricNetworks.markDirty(level, pos);
            level.playSound(null, pos, Tier5Sounds.WRENCH_TURN.get(), SoundSource.BLOCKS, 0.8f, 1.0f);
        }
        return InteractionResult.SUCCESS;
    }

    /** Faces the machine the way the clicked side points; the top and bottom turn it a quarter instead. */
    private static InteractionResult turn(Level level, BlockPos pos, BlockState state, BlockHitResult hit) {
        Direction now = state.getValue(ElectricMachineBlock.FACING);
        Direction side = hit.getDirection();
        Direction next = side.getAxis().isHorizontal() ? side : now.getClockWise();
        if (next == now) return InteractionResult.SUCCESS;
        if (!level.isClientSide()) {
            level.setBlock(pos, state.setValue(ElectricMachineBlock.FACING, next), Block.UPDATE_ALL);
            ElectricNetworks.markDirty(level, pos);
            level.playSound(null, pos, Tier5Sounds.WRENCH_TURN.get(), SoundSource.BLOCKS, 0.8f, 1.0f);
        }
        return InteractionResult.SUCCESS;
    }

    /** Whether the block is a machine that comes in LV and MV and is MV now. */
    public static boolean isMv(BlockState state) {
        return state.hasProperty(ElectricTier.PROPERTY) && state.getValue(ElectricTier.PROPERTY) == ElectricTier.MV
                && Tier5Blocks.upgradable().stream().anyMatch(block -> state.is(block.get()));
    }

    /** Spec 9.5: the casing panels come off again, contents and orientation stay, and the kit is returned. */
    private static InteractionResult downgrade(Level level, BlockPos pos, BlockState state, Player player) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        level.setBlock(pos, state.setValue(ElectricTier.PROPERTY, ElectricTier.LV), Block.UPDATE_ALL);
        // A battery box keeps what fits in the smaller cells.
        if (level.getBlockEntity(pos) instanceof BatteryBoxBlockEntity box) box.setStored(box.stored());
        ElectricNetworks.markDirty(level, pos);
        level.playSound(null, pos, Tier5Sounds.MACHINE_UPGRADE.get(), SoundSource.BLOCKS, 1.0f, 0.75f);
        player.getInventory().placeItemBackInInventory(new ItemStack(Tier5Items.MV_UPGRADE_KIT.get()), net.minecraft.util.Prediction.SERVER_ONLY);
        player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".machine.downgraded", ElectricTier.LV.label()));
        return InteractionResult.SUCCESS;
    }
}
