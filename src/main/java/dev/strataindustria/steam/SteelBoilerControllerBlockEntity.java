package dev.strataindustria.steam;

import dev.strataindustria.fluid.FluidPipeBlock;
import dev.strataindustria.fluid.FluidPipes;
import dev.strataindustria.heat.HeatInletBlockEntity;
import dev.strataindustria.heat.InletHost;
import dev.strataindustria.registry.Tier4BlockEntities;
import dev.strataindustria.registry.Tier4Blocks;
import dev.strataindustria.registry.Tier4Fluids;
import dev.strataindustria.registry.Tier4Menus;
import dev.strataindustria.registry.Tier4Sounds;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The steel boiler (tier 4 spec 10.3): the bronze boiler's workings, grown with its shell. Each shell layer
 * holds 8000 mB of water and 8000 mB of steam and takes up to 90 HU a tick, rated 10 bar. Heat comes from
 * the fireboxes under the shell and through heat inlets in the fire layer; water comes in at the water
 * ports and steam leaves through the steam ports. The safety valve sits on top.
 */
public class SteelBoilerControllerBlockEntity extends BoilerBlockEntity implements InletHost {
    public static final int WATER_PER_LAYER = 8000, STEAM_PER_LAYER = 8000, HEAT_PER_LAYER = 90;
    public static final float RATED = 10.0f;
    private static final int CHECK_INTERVAL = 20;

    private SteelBoilerStructure.Result structure = SteelBoilerStructure.INCOMPLETE;
    /** Shell layers as last built; kept while the structure is broken so nothing is lost. */
    private int layers = SteelBoilerStructure.MIN_LAYERS;
    private Set<BlockPos> parts = Set.of();
    private List<FluidPipes.Network> outlets = List.of();
    private int checkAge = CHECK_INTERVAL;
    private boolean formed;

    public SteelBoilerControllerBlockEntity(BlockPos pos, BlockState state) {
        super(Tier4BlockEntities.BOILER_CONTROLLER.get(), pos, state);
    }

    public SteelBoilerStructure.Result structure() {
        return structure;
    }

    public int layers() {
        return layers;
    }

    private Direction facing() {
        return getBlockState().getValue(BoilerBlock.FACING);
    }

    @Override
    public int waterCapacity() {
        return WATER_PER_LAYER * layers;
    }

    @Override
    public int steamCapacity() {
        return STEAM_PER_LAYER * layers;
    }

    @Override
    public float ratedPressure() {
        return RATED;
    }

    @Override
    public int maxHeat() {
        return HEAT_PER_LAYER * layers;
    }

    /** How full the water is, for the sight glass. */
    public float waterShare() {
        return Math.min(1.0f, water / (float) waterCapacity());
    }

    @Override
    protected boolean ready() {
        return structure.complete();
    }

    /** The controller's own faces take no pipes: water comes in at the ports. */
    @Override
    protected boolean takesWater(Direction side) {
        return false;
    }

    @Override
    public boolean connectsFluid(Direction side) {
        return false;
    }

    /** Heat comes through the fire layer, not pipes on the controller. */
    @Override
    public boolean connectsHeat(Direction side) {
        return false;
    }

    public boolean usesPart(BlockPos pos) {
        return structure.complete() && parts.contains(pos);
    }

    @Override
    public boolean usesInlet(BlockPos inlet) {
        return structure.complete() && structure.inlets().contains(inlet);
    }

    @Override
    protected FluidPipes.Push pushSteam(ServerLevel level, BlockPos pos, int amount) {
        int moved = 0;
        boolean refused = false;
        for (FluidPipes.Network network : outlets) {
            if (moved >= amount) break;
            FluidPipes.Push push = FluidPipes.push(level, network, Tier4Fluids.STEAM.get(), amount - moved, steamTemperature(), pressure());
            moved += push.moved();
            if (push.refused()) {
                refused = true;
                if (age % 20 == 0 && network.firstPipe() != null) FluidPipeBlock.refuseSound(level, network.firstPipe());
            }
        }
        return new FluidPipes.Push(moved, refused);
    }

    /** The top centre of the shell, where the safety valve lets go. */
    @Override
    protected BlockPos valve() {
        return SteelBoilerStructure.centre(worldPosition, facing()).above(layers - 1);
    }

    @Override
    protected BlockState cracked(BlockState state) {
        return Tier4Blocks.CRACKED_BOILER_CONTROLLER.get().defaultBlockState().setValue(BoilerBlock.FACING, state.getValue(BoilerBlock.FACING));
    }

    @Override
    protected void afterTick(ServerLevel level, BlockPos pos) {
        if (++checkAge >= CHECK_INTERVAL) {
            checkAge = 0;
            check(level, pos);
        }
        BlockState state = getBlockState();
        if (state.getBlock() instanceof SteelBoilerControllerBlock) {
            SteelBoilerControllerBlock.Light light = SteelBoilerControllerBlock.Light.of(status);
            boolean venting = status == Status.VENTING;
            int glass = water <= 0 ? 0 : Math.max(1, (int) Math.ceil(waterShare() * 5));
            BlockState next = state.setValue(SteelBoilerControllerBlock.LIGHT, light).setValue(SteelBoilerControllerBlock.VENTING, venting)
                    .setValue(SteelBoilerControllerBlock.GLASS, glass);
            if (next != state) level.setBlock(pos, next, Block.UPDATE_CLIENTS);
        }
    }

    /** Looks the structure over, claims its parts and finds the pipes past the steam ports. */
    public SteelBoilerStructure.Result check(ServerLevel level, BlockPos pos) {
        boolean was = structure.complete();
        structure = SteelBoilerStructure.check(level, pos, facing());
        if (!structure.complete()) {
            parts = Set.of();
            outlets = List.of();
            formed = false;
            return structure;
        }
        if (structure.layers() != layers) {
            layers = structure.layers();
            water = Math.min(water, waterCapacity());
            steam = Math.min(steam, steamCapacity());
        }
        parts = new HashSet<>(structure.parts());
        for (BlockPos part : structure.parts()) {
            if (level.getBlockEntity(part) instanceof BoilerPartBlockEntity link) link.claim(pos);
        }
        for (BlockPos inlet : structure.inlets()) {
            if (level.getBlockEntity(inlet) instanceof HeatInletBlockEntity link) link.claim(pos);
        }
        List<FluidPipes.Network> found = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        for (BlockPos port : structure.steamPorts()) {
            for (Direction side : Direction.values()) {
                BlockPos next = port.relative(side);
                if (structure.contains(next, pos, facing())) continue;
                FluidPipes.Network network = FluidPipes.find(level, port, side);
                if (network.isEmpty()) continue;
                // Two ports on the same pipe run share one network; a machine right against a port is its own.
                List<BlockPos> marks = network.pipes().isEmpty() ? List.of(next) : network.pipes();
                if (seen.contains(marks.getFirst())) continue;
                seen.addAll(marks);
                found.add(network);
            }
        }
        outlets = List.copyOf(found);
        // The form cue is for building it, not for loading a world with it built.
        boolean firstLook = !formed && age <= 1;
        if (!was && !formed) {
            formed = true;
            if (!firstLook) level.playSound(null, pos, Tier4Sounds.MULTIBLOCK_FORM.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        }
        return structure;
    }

    @Override
    protected int problem() {
        return structure.problem().ordinal();
    }

    @Override
    protected int problemWhere() {
        return structure.complete() ? 0 : SteelBoilerStructure.where(worldPosition, facing(), structure.at());
    }

    @Override
    protected String containerName() {
        return "boiler_controller";
    }

    @Override
    protected AbstractContainerMenu menu(int id, Inventory inventory) {
        return new BoilerMenu(Tier4Menus.BOILER_CONTROLLER.get(), id, inventory, worldPosition, data);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        layers = Math.clamp(in.getIntOr("layers", SteelBoilerStructure.MIN_LAYERS), SteelBoilerStructure.MIN_LAYERS, SteelBoilerStructure.MAX_LAYERS);
        super.loadAdditional(in);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("layers", layers);
    }
}
