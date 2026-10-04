package dev.strataindustria.datagen;

import dev.strataindustria.multiblock.Multiblock;
import dev.strataindustria.multiblock.Multiblock.Entry;
import dev.strataindustria.multiblock.Multiblock.Layer;
import dev.strataindustria.multiblock.Multiblock.Preview;
import dev.strataindustria.multiblock.Multiblock.Requirement;
import dev.strataindustria.multiblock.Multiblock.Special;
import dev.strataindustria.multiblock.Multiblock.Symbol;
import dev.strataindustria.multiblock.Multiblocks;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModTags;
import dev.strataindustria.registry.Tier4Blocks;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.block.Block;

/**
 * The shapes of the bloomery, coke oven, blast furnace, converter and steel boiler (tier 3 spec 5, tier 4
 * spec 5.1, 10.3, 12.1, 12.2), written to {@code data/strataindustria/strataindustria/multiblock/}. Rows run
 * from the controller's row to the back, symbols from the left to the right as seen from the controller.
 */
final class MultiblockData {
    private MultiblockData() {}

    static void multiblocks(BootstrapContext<Multiblock> ctx) {
        ctx.register(Multiblocks.BLOOMERY, bloomery());
        ctx.register(Multiblocks.COKE_OVEN, cokeOven());
        ctx.register(Multiblocks.BLAST_FURNACE, blastFurnace());
        ctx.register(Multiblocks.CONVERTER, converter());
        ctx.register(Multiblocks.STEEL_BOILER, steelBoiler());
    }

    /** Fire bricks round a one-block chamber and under it, one to three chimney levels, a free top. */
    private static Multiblock bloomery() {
        Block bricks = ModBlocks.FIRE_BRICKS.get();
        Map<Character, Symbol> symbols = new LinkedHashMap<>();
        symbols.put('B', symbol("needs_brick", bricks, null, tag()));
        symbols.put('_', symbol("needs_air", null, null, special(Special.OPEN)));
        symbols.put('D', symbol("needs_air", null, null, special(Special.NO_DRAUGHT)));
        return new Multiblock(ModBlocks.BLOOMERY.get(), symbols, List.of(
                layer("...", ".B.", "..."),
                layer(".@.", "B_B", ".B."),
                new Layer(List.of(".B.", "B_B", ".B."), 1, 3, false),
                layer("...", ".D.", "...")), List.of());
    }

    /** A 3 x 3 x 3 cube of coke oven bricks round a chamber, the door in the middle of the front face. */
    private static Multiblock cokeOven() {
        Block bricks = Tier4Blocks.COKE_OVEN_BRICKS.get();
        Map<Character, Symbol> symbols = new LinkedHashMap<>();
        symbols.put('b', symbol("needs_brick", bricks, null, block(bricks)));
        symbols.put('_', symbol("needs_air", null, null, special(Special.OPEN)));
        return new Multiblock(Tier4Blocks.COKE_OVEN_DOOR.get(), symbols, List.of(
                layer("bbb", "bbb", "bbb"),
                layer("b@b", "b_b", "bbb"),
                layer("bbb", "bbb", "bbb")), List.of());
    }

    /**
     * A hearth of refractory casing with the controller, a tap hatch and tuyeres on its edges, a casing bosh,
     * two refractory layers round the open shaft and a throat with the charging hatch. Heat inlets may stand
     * in for casing in the hearth and the bosh.
     */
    private static Multiblock blastFurnace() {
        Block casing = Tier4Blocks.REFRACTORY_CASING.get();
        Block tuyere = Tier4Blocks.TUYERE.get(), tap = Tier4Blocks.TAP_HATCH.get(), hatch = Tier4Blocks.CHARGING_HATCH.get();
        Block inlet = Tier4Blocks.HEAT_INLET.get();
        Entry[] edge = {block(tuyere, "tuyere"), block(tap, "tap"), block(casing), block(inlet, "inlet")};
        Map<Character, Symbol> symbols = new LinkedHashMap<>();
        symbols.put('h', symbol("needs_casing", casing, null, block(casing), block(inlet, "inlet")));
        symbols.put('t', symbol("needs_casing", tuyere, "left", edge));
        symbols.put('p', symbol("needs_casing", tap, "back", edge));
        symbols.put('e', symbol("needs_casing", casing, null, edge));
        symbols.put('r', symbol("needs_refractory", ModBlocks.FIRE_BRICKS.get(), null, tag()));
        symbols.put('_', symbol("needs_air", null, null, special(Special.OPEN)));
        symbols.put('H', symbol("needs_charging_hatch", hatch, null, block(hatch, "hatch")));
        return new Multiblock(Tier4Blocks.BLAST_FURNACE_CONTROLLER.get(), symbols, List.of(
                layer("h@h", "the", "hph"),
                layer("hhh", "h_h", "hhh"),
                layer("rrr", "r_r", "rrr"),
                layer("rrr", "r_r", "rrr"),
                layer("rrr", "rHr", "rrr")), List.of(
                new Requirement("tap", 1, Optional.of(1), "needs_tap_hatch", Optional.of(0)),
                new Requirement("tuyere", 1, Optional.empty(), "needs_tuyere", Optional.of(0))));
    }

    /**
     * A 3 x 3 x 3 vessel: a casing bottom with the controller and tuyeres on its edges, a casing middle round the
     * open vessel with the tap hatch on one edge, and a refractory top with the charging hatch.
     */
    private static Multiblock converter() {
        Block casing = Tier4Blocks.REFRACTORY_CASING.get();
        Block tuyere = Tier4Blocks.TUYERE.get(), tap = Tier4Blocks.TAP_HATCH.get(), hatch = Tier4Blocks.CHARGING_HATCH.get();
        Block inlet = Tier4Blocks.HEAT_INLET.get();
        Entry[] bottomEdge = {block(tuyere, "tuyere"), block(casing), block(inlet, "inlet")};
        Entry[] middleEdge = {block(tap, "tap"), block(casing)};
        Map<Character, Symbol> symbols = new LinkedHashMap<>();
        symbols.put('h', symbol("needs_casing", casing, null, block(casing), block(inlet, "inlet")));
        symbols.put('t', symbol("needs_casing", tuyere, "left", bottomEdge));
        symbols.put('e', symbol("needs_casing", casing, null, bottomEdge));
        symbols.put('k', symbol("needs_casing", casing, null, block(casing)));
        symbols.put('m', symbol("needs_casing", casing, null, middleEdge));
        symbols.put('p', symbol("needs_casing", tap, "back", middleEdge));
        symbols.put('_', symbol("needs_air", null, null, special(Special.OPEN)));
        symbols.put('r', symbol("needs_refractory", ModBlocks.FIRE_BRICKS.get(), null, tag()));
        symbols.put('H', symbol("needs_charging_hatch", hatch, null, block(hatch, "hatch")));
        return new Multiblock(Tier4Blocks.CONVERTER_CONTROLLER.get(), symbols, List.of(
                layer("h@h", "the", "heh"),
                layer("kmk", "m_m", "kpk"),
                layer("rrr", "rHr", "rrr")), List.of(
                new Requirement("tuyere", 1, Optional.empty(), "needs_tuyere", Optional.of(0)),
                new Requirement("tap", 1, Optional.of(1), "needs_tap_hatch", Optional.of(1))));
    }

    /**
     * A 3 x 3 fire layer of fireboxes and heat inlets, then two to four 3 x 3 layers of shell, the controller in
     * the front of the lowest. At least one water port and one steam port are somewhere in the shell.
     */
    private static Multiblock steelBoiler() {
        Block firebox = Tier4Blocks.FIREBOX.get(), shell = Tier4Blocks.STEEL_BOILER_SHELL.get();
        Block port = Tier4Blocks.BOILER_FLUID_PORT.get(), inlet = Tier4Blocks.HEAT_INLET.get();
        Entry[] shellOrPort = {
                block(shell, "part"),
                new Entry(Optional.of(port), Optional.empty(), Optional.empty(), Map.of("mode", "steam"), List.of("part", "steam_port")),
                new Entry(Optional.of(port), Optional.empty(), Optional.empty(), Map.of("mode", "water"), List.of("part", "water_port"))};
        Map<Character, Symbol> symbols = new LinkedHashMap<>();
        symbols.put('F', symbol("needs_fire", firebox, null, block(firebox), block(inlet, "inlet")));
        symbols.put('S', symbol("needs_shell", shell, null, shellOrPort));
        symbols.put('w', new Symbol(List.of(shellOrPort), "needs_shell",
                Optional.of(new Preview(port, Map.of("mode", "water"), Optional.empty()))));
        symbols.put('s', new Symbol(List.of(shellOrPort), "needs_shell",
                Optional.of(new Preview(port, Map.of("mode", "steam"), Optional.empty()))));
        return new Multiblock(Tier4Blocks.BOILER_CONTROLLER.get(), symbols, List.of(
                layer("FFF", "FFF", "FFF"),
                layer("S@S", "SSS", "SSS"),
                layer("SSS", "sSS", "SwS"),
                new Layer(List.of("SSS", "SSS", "SSS"), 0, 2, true)), List.of(
                new Requirement("water_port", 1, Optional.empty(), "needs_water_port", Optional.empty()),
                new Requirement("steam_port", 1, Optional.empty(), "needs_steam_port", Optional.empty())));
    }

    private static Layer layer(String... rows) {
        return new Layer(List.of(rows), 1, 1, false);
    }

    private static Entry block(Block block, String... roles) {
        return new Entry(Optional.of(block), Optional.empty(), Optional.empty(), Map.of(), List.of(roles));
    }

    private static Entry tag() {
        return new Entry(Optional.empty(), Optional.of(ModTags.Blocks.REFRACTORY), Optional.empty(), Map.of(), List.of());
    }

    private static Entry special(Special special) {
        return new Entry(Optional.empty(), Optional.empty(), Optional.of(special), Map.of(), List.of());
    }

    private static Symbol symbol(String problem, Block preview, String facing, Entry... accepts) {
        return new Symbol(List.of(accepts), problem, Optional.ofNullable(preview).map(b -> new Preview(b, Map.of(), Optional.ofNullable(facing))));
    }
}
