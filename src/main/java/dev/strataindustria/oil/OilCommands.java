package dev.strataindustria.oil;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.strataindustria.StrataIndustria;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Debug tools for oil (spec 19.1.7): {@code /strataindustria oil info} for the reservoir under the caller, and
 * {@code /strataindustria oil map <radius>} for every reservoir within a radius of chunks.
 */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class OilCommands {
    private static final String KEY = "commands." + StrataIndustria.MOD_ID + ".oil.";
    private static final int MAX_RADIUS = 128;

    private OilCommands() {}

    @SubscribeEvent
    static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(StrataIndustria.MOD_ID)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("oil")
                        .then(Commands.literal("info").executes(OilCommands::info))
                        .then(Commands.literal("map")
                                .executes(c -> map(c, 32))
                                .then(Commands.argument("radius", IntegerArgumentType.integer(1, MAX_RADIUS))
                                        .executes(c -> map(c, IntegerArgumentType.getInteger(c, "radius")))))));
    }

    private static int info(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        Optional<OilReservoir> found = OilReservoirs.at(level, new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4));
        if (found.isEmpty()) {
            source.sendFailure(Component.translatable(KEY + "none"));
            return 0;
        }
        OilReservoir r = found.get();
        OilReservoirData data = OilReservoirData.get(level);
        source.sendSuccess(() -> describe(r, data), false);
        source.sendSuccess(() -> Component.translatable(KEY + "details", r.chunks().size(), r.topY(), pos.getY() - r.topY(),
                r.capacity(), data.remaining(r), Math.round(data.fraction(r) * 100)), false);
        for (OilReservoir.Seep seep : r.seeps()) {
            source.sendSuccess(() -> Component.translatable(KEY + "seep", seep.x(), seep.z(), seep.size()), false);
        }
        return 1;
    }

    private static int map(CommandContext<CommandSourceStack> context, int radius) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        int cx = pos.getX() >> 4, cz = pos.getZ() >> 4;
        int cells = Math.floorDiv(radius, OilReservoir.CELL_CHUNKS) + 1;
        int cellX = Math.floorDiv(cx, OilReservoir.CELL_CHUNKS), cellZ = Math.floorDiv(cz, OilReservoir.CELL_CHUNKS);
        List<OilReservoir> found = new ArrayList<>();
        for (int x = cellX - cells; x <= cellX + cells; x++) {
            for (int z = cellZ - cells; z <= cellZ + cells; z++) {
                OilReservoirs.inCell(level, x, z).filter(r -> Math.abs(r.centreX() - cx) <= radius && Math.abs(r.centreZ() - cz) <= radius)
                        .ifPresent(found::add);
            }
        }
        found.sort(Comparator.comparingDouble(r -> distanceSq(r, pos)));
        OilReservoirData data = OilReservoirData.get(level);
        source.sendSuccess(() -> Component.translatable(KEY + "map", found.size(), radius), false);
        for (OilReservoir r : found) {
            source.sendSuccess(() -> describe(r, data), false);
        }
        return found.size();
    }

    private static Component describe(OilReservoir r, OilReservoirData data) {
        int seeps = r.seeps().size();
        return Component.translatable(KEY + "reservoir", Component.translatable(KEY + "size." + r.sizeClass().id()), r.centreBlockX(),
                r.centreBlockZ(), r.topY(), Math.round(data.fraction(r) * 100), seeps);
    }

    private static double distanceSq(OilReservoir r, BlockPos pos) {
        double dx = r.centreBlockX() - pos.getX(), dz = r.centreBlockZ() - pos.getZ();
        return dx * dx + dz * dz;
    }
}
