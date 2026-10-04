package dev.strataindustria.registry;

import dev.strataindustria.electric.BatteryBoxBlock;
import dev.strataindustria.electric.CableBlock;
import dev.strataindustria.electric.KineticDynamoBlock;
import dev.strataindustria.power.ElectricTier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;

/** Tier 5 (electric) blocks, kept apart from the earlier tiers' blocks. */
public final class Tier5Blocks {
    /** Spec 23.6: cables thud softly, like wool a little higher. */
    public static final SoundType CABLE_SOUND = new SoundType(1.0f, 1.2f, SoundType.WOOL.getBreakSound(), SoundType.WOOL.getStepSound(),
            SoundType.WOOL.getPlaceSound(), SoundType.WOOL.getHitSound(), SoundType.WOOL.getFallSound());
    /** Spec 23.6: electric machines ring like metal, with the heavier netherite knock when hit. */
    public static final SoundType MACHINE_SOUND = new SoundType(1.0f, 1.0f, SoundType.METAL.getBreakSound(), SoundType.METAL.getStepSound(),
            SoundType.METAL.getPlaceSound(), SoundType.NETHERITE_BLOCK.getHitSound(), SoundType.METAL.getFallSound());

    // Spec 8.1: rubber-insulated copper cables.
    public static final DeferredBlock<CableBlock> LV_CABLE = ModBlocks.BLOCKS.registerBlock("lv_cable", p -> new CableBlock(ElectricTier.LV, p),
            Tier5Blocks::cable);
    public static final DeferredBlock<CableBlock> MV_CABLE = ModBlocks.BLOCKS.registerBlock("mv_cable", p -> new CableBlock(ElectricTier.MV, p),
            Tier5Blocks::cable);

    // Spec 7.1 and 7.4: the first generator and the network's storage.
    public static final DeferredBlock<KineticDynamoBlock> KINETIC_DYNAMO = ModBlocks.BLOCKS.registerBlock("kinetic_dynamo", KineticDynamoBlock::new,
            p -> p.mapColor(MapColor.METAL)
                    .strength(3.5f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion());
    public static final DeferredBlock<BatteryBoxBlock> BATTERY_BOX = ModBlocks.BLOCKS.registerBlock("battery_box", BatteryBoxBlock::new,
            Tier5Blocks::machine);

    /** Spec 9.5: the shell every electric machine is built around; also a decorative block. */
    public static final DeferredBlock<Block> LV_MACHINE_HULL = ModBlocks.BLOCKS.registerSimpleBlock("lv_machine_hull", Tier5Blocks::machine);

    private static Block.Properties cable(Block.Properties p) {
        return p.mapColor(MapColor.COLOR_BLACK)
                .strength(0.4f)
                .sound(CABLE_SOUND)
                .noOcclusion()
                .pushReaction(PushReaction.DESTROY);
    }

    /** Riveted steel casings: mined with a pickaxe. */
    private static Block.Properties machine(Block.Properties p) {
        return p.mapColor(MapColor.METAL)
                .strength(4.0f, 6.0f)
                .requiresCorrectToolForDrops()
                .sound(MACHINE_SOUND);
    }

    public static void init() {}

    private Tier5Blocks() {}
}
