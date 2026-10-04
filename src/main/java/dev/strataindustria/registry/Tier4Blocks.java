package dev.strataindustria.registry;

import dev.strataindustria.ceramics.RefractoryCrucibleBlock;
import dev.strataindustria.coking.CokeOvenBlock;
import dev.strataindustria.fluid.FluidPipeBlock;
import dev.strataindustria.fluid.PressureGaugeBlock;
import dev.strataindustria.power.IronTransmission;
import dev.strataindustria.smithing.AnvilBlock;
import dev.strataindustria.steam.BoilerBlock;
import dev.strataindustria.steam.CrackedBoilerBlock;
import dev.strataindustria.steam.FireboxBlock;
import dev.strataindustria.steam.MechanicalPumpBlock;
import dev.strataindustria.steam.SteamEngineBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;

/** Tier 4 (steel and steam) blocks, kept apart from the earlier tiers' blocks. */
public final class Tier4Blocks {
    // Spec 21.7: the fire brick sound, a touch deeper for the heavier brick.
    private static final SoundType COKE_OVEN_SOUND = new SoundType(1.0f, 1.0f, SoundType.DEEPSLATE_BRICKS.getBreakSound(),
            SoundType.DEEPSLATE_BRICKS.getStepSound(), SoundType.DEEPSLATE_BRICKS.getPlaceSound(),
            SoundType.DEEPSLATE_BRICKS.getHitSound(), SoundType.DEEPSLATE_BRICKS.getFallSound());
    private static final SoundType COKE_SOUND = new SoundType(1.0f, 1.2f, SoundType.BASALT.getBreakSound(),
            SoundType.BASALT.getStepSound(), SoundType.BASALT.getPlaceSound(),
            SoundType.BASALT.getHitSound(), SoundType.BASALT.getFallSound());

    // Spec 5.1: the coke oven.
    public static final DeferredBlock<Block> COKE_OVEN_BRICKS = ModBlocks.BLOCKS.registerSimpleBlock("coke_oven_bricks", Tier4Blocks::cokeOven);
    public static final DeferredBlock<CokeOvenBlock> COKE_OVEN_DOOR = ModBlocks.BLOCKS.registerBlock("coke_oven_door", CokeOvenBlock::new,
            p -> cokeOven(p).lightLevel(state -> state.getValue(CokeOvenBlock.LIT) ? 10 : 0));
    /** Nine coke, a compact fuel. */
    public static final DeferredBlock<Block> COKE_BLOCK = ModBlocks.BLOCKS.registerSimpleBlock("coke_block", p -> p
            .mapColor(MapColor.COLOR_GRAY)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .strength(4.0f, 6.0f)
            .requiresCorrectToolForDrops()
            .sound(COKE_SOUND));

    // Spec 6.1: the refractory crucible, rated to 1700 degrees; it melts iron.
    public static final DeferredBlock<RefractoryCrucibleBlock> REFRACTORY_CRUCIBLE = ModBlocks.BLOCKS.registerBlock("refractory_crucible",
            RefractoryCrucibleBlock::new, p -> p.mapColor(MapColor.SAND)
                    .strength(1.5f)
                    .sound(SoundType.DECORATED_POT)
                    .noOcclusion()
                    .pushReaction(PushReaction.POPPED));

    // Spec 4.6: creosote-treated wood, which never rots.
    public static final DeferredBlock<Block> TREATED_PLANKS = ModBlocks.BLOCKS.registerSimpleBlock("treated_planks", Tier4Blocks::treated);
    public static final DeferredBlock<SlabBlock> TREATED_SLAB = ModBlocks.BLOCKS.registerBlock("treated_slab", SlabBlock::new, Tier4Blocks::treated);
    public static final DeferredBlock<StairBlock> TREATED_STAIRS = ModBlocks.BLOCKS.registerBlock("treated_stairs",
            p -> new StairBlock(TREATED_PLANKS.get().defaultBlockState(), p), Tier4Blocks::treated);
    public static final DeferredBlock<FenceBlock> TREATED_FENCE = ModBlocks.BLOCKS.registerBlock("treated_fence", FenceBlock::new,
            p -> treated(p).forceSolidOn());

    /** Tier 4 spec 14.4: the steel anvil, anvil tier 5 and the tier 4 exit item. */
    public static final DeferredBlock<AnvilBlock> STEEL_ANVIL = ModBlocks.BLOCKS.registerBlock("steel_anvil", p -> new AnvilBlock(5, false, p),
            p -> p.mapColor(MapColor.METAL)
                    .strength(5.0f, 1200.0f)
                    .sound(SoundType.ANVIL)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .pushReaction(PushReaction.IMMOVEABLE));

    // Spec 11.7: iron transmission, good to 256 RPM.
    public static final DeferredBlock<IronTransmission.Axle> IRON_AXLE = ModBlocks.BLOCKS.registerBlock("iron_axle", IronTransmission.Axle::new,
            p -> iron(p).noOcclusion());
    public static final DeferredBlock<IronTransmission.Gearbox> IRON_GEARBOX = ModBlocks.BLOCKS.registerBlock("iron_gearbox",
            IronTransmission.Gearbox::new, Tier4Blocks::iron);
    public static final DeferredBlock<IronTransmission.StepUpGearbox> IRON_STEP_UP_GEARBOX = ModBlocks.BLOCKS.registerBlock("iron_step_up_gearbox",
            IronTransmission.StepUpGearbox::new, Tier4Blocks::iron);

    // Spec 21.7: fire brick for the firebox; a heavy, deep metal for boilers; copper for copper and bronze
    // pipes; brass fittings ring higher.
    private static final SoundType FIRE_BRICK_SOUND = new SoundType(1.0f, 1.1f, SoundType.DEEPSLATE_BRICKS.getBreakSound(),
            SoundType.DEEPSLATE_BRICKS.getStepSound(), SoundType.DEEPSLATE_BRICKS.getPlaceSound(),
            SoundType.DEEPSLATE_BRICKS.getHitSound(), SoundType.DEEPSLATE_BRICKS.getFallSound());
    public static final SoundType HEAVY_METAL = new SoundType(1.0f, 0.9f, SoundType.NETHERITE_BLOCK.getBreakSound(),
            SoundType.NETHERITE_BLOCK.getStepSound(), SoundType.NETHERITE_BLOCK.getPlaceSound(),
            SoundType.NETHERITE_BLOCK.getHitSound(), SoundType.NETHERITE_BLOCK.getFallSound());
    private static final SoundType BRASS_SOUND = new SoundType(1.0f, 1.2f, SoundType.COPPER.getBreakSound(),
            SoundType.COPPER.getStepSound(), SoundType.COPPER.getPlaceSound(),
            SoundType.COPPER.getHitSound(), SoundType.COPPER.getFallSound());

    // Spec 8.1 and 10.2: the firebox and the bronze boiler that sits on it.
    public static final DeferredBlock<FireboxBlock> FIREBOX = ModBlocks.BLOCKS.registerBlock("firebox", FireboxBlock::new,
            p -> p.mapColor(MapColor.SAND)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .strength(2.5f, 8.0f)
                    .requiresCorrectToolForDrops()
                    .sound(FIRE_BRICK_SOUND)
                    .lightLevel(state -> state.getValue(FireboxBlock.LIT) ? 13 : 0));
    public static final DeferredBlock<BoilerBlock> BRONZE_BOILER = ModBlocks.BLOCKS.registerBlock("bronze_boiler", BoilerBlock::new,
            Tier4Blocks::boiler);
    public static final DeferredBlock<CrackedBoilerBlock> CRACKED_BRONZE_BOILER = ModBlocks.BLOCKS.registerBlock("cracked_bronze_boiler",
            CrackedBoilerBlock::new, Tier4Blocks::boiler);
    // Spec 13.3: the chute.
    public static final DeferredBlock<dev.strataindustria.automation.ChuteBlock> CHUTE = ModBlocks.BLOCKS.registerBlock("chute",
            dev.strataindustria.automation.ChuteBlock::new, p -> p.mapColor(MapColor.METAL)
                    .strength(3.0f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .sound(HEAVY_METAL));
    // Spec 13.4: the inserter.
    public static final DeferredBlock<dev.strataindustria.automation.InserterBlock> INSERTER = ModBlocks.BLOCKS.registerBlock("inserter",
            dev.strataindustria.automation.InserterBlock::new, p -> p.mapColor(MapColor.METAL)
                    .strength(3.0f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .sound(HEAVY_METAL));
    // Spec 10.3: the steel boiler multiblock.
    public static final DeferredBlock<dev.strataindustria.steam.SteelBoilerShellBlock> STEEL_BOILER_SHELL = ModBlocks.BLOCKS.registerBlock(
            "steel_boiler_shell", dev.strataindustria.steam.SteelBoilerShellBlock::new, Tier4Blocks::steelBoiler);
    public static final DeferredBlock<dev.strataindustria.steam.BoilerFluidPortBlock> BOILER_FLUID_PORT = ModBlocks.BLOCKS.registerBlock(
            "boiler_fluid_port", dev.strataindustria.steam.BoilerFluidPortBlock::new, Tier4Blocks::steelBoiler);
    public static final DeferredBlock<dev.strataindustria.steam.SteelBoilerControllerBlock> BOILER_CONTROLLER = ModBlocks.BLOCKS.registerBlock(
            "boiler_controller", dev.strataindustria.steam.SteelBoilerControllerBlock::new,
            p -> steelBoiler(p).lightLevel(state -> state.getValue(dev.strataindustria.steam.SteelBoilerControllerBlock.LIGHT)
                    == dev.strataindustria.steam.SteelBoilerControllerBlock.Light.OFF ? 0 : 4));
    public static final DeferredBlock<CrackedBoilerBlock> CRACKED_BOILER_CONTROLLER = ModBlocks.BLOCKS.registerBlock("cracked_boiler_controller",
            CrackedBoilerBlock::new, Tier4Blocks::steelBoiler);

    // Spec 9.2 and 9.3: fluid pipes, rated by the hottest fluid and the flow they take, and the gauge.
    public static final DeferredBlock<FluidPipeBlock> COPPER_FLUID_PIPE = ModBlocks.BLOCKS.registerBlock("copper_fluid_pipe",
            p -> new FluidPipeBlock(160, 100, p), p -> pipe(p, MapColor.COLOR_ORANGE, SoundType.COPPER));
    public static final DeferredBlock<FluidPipeBlock> BRONZE_FLUID_PIPE = ModBlocks.BLOCKS.registerBlock("bronze_fluid_pipe",
            p -> new FluidPipeBlock(220, 200, p), p -> pipe(p, MapColor.COLOR_BROWN, SoundType.COPPER));
    public static final DeferredBlock<FluidPipeBlock> STEEL_FLUID_PIPE = ModBlocks.BLOCKS.registerBlock("steel_fluid_pipe",
            p -> new FluidPipeBlock(400, 400, p), p -> pipe(p, MapColor.METAL, HEAVY_METAL));
    public static final DeferredBlock<PressureGaugeBlock> PRESSURE_GAUGE = ModBlocks.BLOCKS.registerBlock("pressure_gauge",
            p -> new PressureGaugeBlock(220, 200, p), p -> pipe(p, MapColor.GOLD, BRASS_SOUND));
    public static final DeferredBlock<dev.strataindustria.fluid.ValveBlock> VALVE = ModBlocks.BLOCKS.registerBlock("valve",
            p -> new dev.strataindustria.fluid.ValveBlock(220, 200, p), p -> pipe(p, MapColor.GOLD, BRASS_SOUND));
    public static final DeferredBlock<dev.strataindustria.fluid.FluidTankBlock> FLUID_TANK = ModBlocks.BLOCKS.registerBlock("fluid_tank",
            dev.strataindustria.fluid.FluidTankBlock::new, p -> p.mapColor(MapColor.COLOR_ORANGE)
                    .strength(3.0f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .sound(SoundType.COPPER));

    // Spec 9.3 and 10.5: the mechanical pump, and the steam engine that turns a shaft.
    public static final DeferredBlock<MechanicalPumpBlock> MECHANICAL_PUMP = ModBlocks.BLOCKS.registerBlock("mechanical_pump",
            MechanicalPumpBlock::new, p -> p.mapColor(MapColor.COLOR_BROWN)
                    .strength(3.0f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.COPPER));
    public static final DeferredBlock<SteamEngineBlock> STEAM_ENGINE = ModBlocks.BLOCKS.registerBlock("steam_engine", SteamEngineBlock::new,
            p -> p.mapColor(MapColor.METAL)
                    .strength(4.0f, 8.0f)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .sound(HEAVY_METAL));
    public static final DeferredBlock<dev.strataindustria.steam.SteamHammerBlock> STEAM_HAMMER = ModBlocks.BLOCKS.registerBlock("steam_hammer",
            dev.strataindustria.steam.SteamHammerBlock::new, p -> p.mapColor(MapColor.METAL)
                    .strength(5.0f, 1200.0f)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .sound(HEAVY_METAL));

    // Spec 11.2: ore processing on a shaft.
    public static final DeferredBlock<dev.strataindustria.processing.ProcessingBlock<dev.strataindustria.processing.CrusherBlockEntity>> CRUSHER =
            ModBlocks.BLOCKS.registerBlock("crusher", p -> new dev.strataindustria.processing.ProcessingBlock<dev.strataindustria.processing.CrusherBlockEntity>(
                    () -> Tier4BlockEntities.CRUSHER.get(), dev.strataindustria.processing.CrusherBlockEntity::new, p),
                    p -> p.mapColor(MapColor.METAL)
                            .strength(4.0f, 8.0f)
                            .requiresCorrectToolForDrops()
                            .sound(HEAVY_METAL));

    public static final DeferredBlock<dev.strataindustria.processing.ProcessingBlock<dev.strataindustria.processing.WasherBlockEntity>> WASHER =
            ModBlocks.BLOCKS.registerBlock("washer", p -> new dev.strataindustria.processing.ProcessingBlock<dev.strataindustria.processing.WasherBlockEntity>(
                    () -> Tier4BlockEntities.WASHER.get(), dev.strataindustria.processing.WasherBlockEntity::new, p),
                    p -> p.mapColor(MapColor.WOOD)
                            .strength(2.5f, 4.0f)
                            .sound(SoundType.WOOD));

    // Spec 12.1: the blast furnace's casing, controller and wall parts, and the blower that feeds it air.
    public static final DeferredBlock<Block> REFRACTORY_CASING = ModBlocks.BLOCKS.registerSimpleBlock("refractory_casing", Tier4Blocks::refractory);
    public static final DeferredBlock<dev.strataindustria.ironworks.BlastFurnaceBlock> BLAST_FURNACE_CONTROLLER =
            ModBlocks.BLOCKS.registerBlock("blast_furnace_controller", dev.strataindustria.ironworks.BlastFurnaceBlock::new,
                    p -> refractory(p).lightLevel(state -> state.getValue(dev.strataindustria.ironworks.BlastFurnaceBlock.LIT) ? 10 : 0));
    public static final DeferredBlock<dev.strataindustria.ironworks.FurnacePartBlock> TUYERE = ModBlocks.BLOCKS.registerBlock("tuyere",
            dev.strataindustria.ironworks.FurnacePartBlock::new, Tier4Blocks::refractory);
    public static final DeferredBlock<dev.strataindustria.ironworks.ChargingHatchBlock> CHARGING_HATCH = ModBlocks.BLOCKS.registerBlock(
            "charging_hatch", dev.strataindustria.ironworks.ChargingHatchBlock::new, Tier4Blocks::refractory);
    public static final DeferredBlock<dev.strataindustria.ironworks.TapHatchBlock> TAP_HATCH = ModBlocks.BLOCKS.registerBlock("tap_hatch",
            dev.strataindustria.ironworks.TapHatchBlock::new,
            p -> refractory(p).lightLevel(state -> state.getValue(dev.strataindustria.ironworks.TapHatchBlock.HOT) ? 9 : 0));
    // Spec 12.2: the converter's controller; the rest of it is blast furnace parts.
    public static final DeferredBlock<dev.strataindustria.ironworks.ConverterBlock> CONVERTER_CONTROLLER =
            ModBlocks.BLOCKS.registerBlock("converter_controller", dev.strataindustria.ironworks.ConverterBlock::new,
                    p -> refractory(p).lightLevel(state -> state.getValue(dev.strataindustria.ironworks.ConverterBlock.LIT) ? 12 : 0));
    public static final DeferredBlock<dev.strataindustria.ironworks.BlowerBlock> BLOWER = ModBlocks.BLOCKS.registerBlock("blower",
            dev.strataindustria.ironworks.BlowerBlock::new, p -> p.mapColor(MapColor.METAL)
                    .strength(3.5f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .sound(HEAVY_METAL));
    // Spec 10.5: a steam engine on a blowing cylinder, worth two blowers.
    public static final DeferredBlock<dev.strataindustria.ironworks.BlowingEngineBlock> BLOWING_ENGINE = ModBlocks.BLOCKS.registerBlock(
            "blowing_engine", dev.strataindustria.ironworks.BlowingEngineBlock::new, p -> p.mapColor(MapColor.METAL)
                    .strength(4.0f, 8.0f)
                    .requiresCorrectToolForDrops()
                    .sound(HEAVY_METAL));

    // Spec 8.2 and 8.4: heat pipes, which glow while they carry heat, and the inlet that takes it into a multiblock.
    public static final DeferredBlock<dev.strataindustria.heat.HeatPipeBlock> COPPER_HEAT_PIPE = ModBlocks.BLOCKS.registerBlock("copper_heat_pipe",
            p -> new dev.strataindustria.heat.HeatPipeBlock(1000, 10.0f, 0.01f, p),
            p -> pipe(p, MapColor.COLOR_ORANGE, SoundType.COPPER).lightLevel(Tier4Blocks::heatGlow));
    public static final DeferredBlock<dev.strataindustria.heat.HeatPipeBlock> REFRACTORY_HEAT_DUCT = ModBlocks.BLOCKS.registerBlock(
            "refractory_heat_duct", p -> new dev.strataindustria.heat.HeatPipeBlock(1800, 5.0f, 0.005f, p),
            p -> pipe(p, MapColor.COLOR_ORANGE, FIRE_BRICK_SOUND).lightLevel(Tier4Blocks::heatGlow));
    public static final DeferredBlock<dev.strataindustria.heat.HeatPipeBlock> INSULATED_COPPER_HEAT_PIPE = ModBlocks.BLOCKS.registerBlock(
            "insulated_copper_heat_pipe", p -> new dev.strataindustria.heat.HeatPipeBlock(1000, 5.0f, 0.005f, p),
            p -> pipe(p, MapColor.COLOR_LIGHT_GRAY, SoundType.WOOL));
    public static final DeferredBlock<dev.strataindustria.heat.HeatPipeBlock> INSULATED_REFRACTORY_HEAT_DUCT = ModBlocks.BLOCKS.registerBlock(
            "insulated_refractory_heat_duct", p -> new dev.strataindustria.heat.HeatPipeBlock(1800, 2.5f, 0.0025f, p),
            p -> pipe(p, MapColor.COLOR_LIGHT_GRAY, SoundType.WOOL));
    public static final DeferredBlock<dev.strataindustria.heat.HeatInletBlock> HEAT_INLET = ModBlocks.BLOCKS.registerBlock("heat_inlet",
            dev.strataindustria.heat.HeatInletBlock::new, Tier4Blocks::refractory);

    // Spec 8.6: the kiln.
    public static final DeferredBlock<dev.strataindustria.ceramics.KilnBlock> KILN = ModBlocks.BLOCKS.registerBlock("kiln",
            dev.strataindustria.ceramics.KilnBlock::new,
            p -> refractory(p).lightLevel(state -> state.getValue(dev.strataindustria.ceramics.KilnBlock.LIT) ? 10 : 0));

    // Spec 8.5: the roaster.
    public static final DeferredBlock<dev.strataindustria.roasting.RoasterBlock> ROASTER = ModBlocks.BLOCKS.registerBlock("roaster",
            dev.strataindustria.roasting.RoasterBlock::new,
            p -> refractory(p).lightLevel(state -> state.getValue(dev.strataindustria.roasting.RoasterBlock.LIT) ? 8 : 0));

    // Spec 8.7: the smelter.
    public static final DeferredBlock<dev.strataindustria.metal.SmelterBlock> SMELTER = ModBlocks.BLOCKS.registerBlock("smelter",
            dev.strataindustria.metal.SmelterBlock::new,
            p -> refractory(p).lightLevel(state -> state.getValue(dev.strataindustria.metal.SmelterBlock.LIT) ? 12 : 0));

    private static int heatGlow(BlockState state) {
        return state.getValue(dev.strataindustria.heat.HeatPipeBlock.HOT) ? 6 : 0;
    }

    /** Fire brick held in iron: as tough as the bricks, with their sound. */
    private static Block.Properties refractory(Block.Properties p) {
        return p.mapColor(MapColor.COLOR_ORANGE)
                .instrument(NoteBlockInstrument.BASEDRUM)
                .strength(3.0f, 8.0f)
                .requiresCorrectToolForDrops()
                .sound(FIRE_BRICK_SOUND);
    }

    private static Block.Properties boiler(Block.Properties p) {
        return p.mapColor(MapColor.COLOR_BROWN)
                .strength(4.0f, 8.0f)
                .requiresCorrectToolForDrops()
                .sound(HEAVY_METAL);
    }

    private static Block.Properties steelBoiler(Block.Properties p) {
        return p.mapColor(MapColor.METAL)
                .strength(5.0f, 10.0f)
                .requiresCorrectToolForDrops()
                .sound(HEAVY_METAL);
    }

    private static Block.Properties pipe(Block.Properties p, MapColor colour, SoundType sound) {
        return p.mapColor(colour)
                .strength(2.0f, 6.0f)
                .requiresCorrectToolForDrops()
                .noOcclusion()
                .sound(sound);
    }

    /** Wrought iron machine parts: mined with a pickaxe, they ring like iron. */
    private static Block.Properties iron(Block.Properties p) {
        return p.mapColor(MapColor.METAL)
                .strength(3.5f, 6.0f)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL);
    }

    private static Block.Properties cokeOven(Block.Properties p) {
        return p.mapColor(MapColor.TERRACOTTA_BROWN)
                .instrument(NoteBlockInstrument.BASEDRUM)
                .strength(2.5f, 8.0f)
                .requiresCorrectToolForDrops()
                .sound(COKE_OVEN_SOUND);
    }

    /** Treated wood is tougher than plain planks and does not catch fire from lava. */
    private static Block.Properties treated(Block.Properties p) {
        return p.mapColor(MapColor.COLOR_BROWN)
                .instrument(NoteBlockInstrument.BASS)
                .strength(2.5f, 4.0f)
                .sound(SoundType.WOOD);
    }

    public static void init() {}

    private Tier4Blocks() {}
}
