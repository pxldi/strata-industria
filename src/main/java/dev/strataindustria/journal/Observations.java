package dev.strataindustria.journal;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.block.OreBlock;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.ModTags;
import dev.strataindustria.structure.StructureContent;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Observations (journal leads spec): first-person notes written the first time the player comes across
 * something, each of which may open a lead before its place in the tree. Text lives under
 * {@code journal.strataindustria.observe.<id>}.
 */
public final class Observations {
    static final String KEY = "journal." + StrataIndustria.MOD_ID + ".observe.";

    /** Something picked up for the first time. {@code reveals} is a lead path or null. */
    record Find(String id, Predicate<ItemStack> matches, Supplier<Item> icon, String reveals) {}

    static final List<Find> FINDS = new ArrayList<>();

    static {
        find("flint", stack -> stack.is(Items.FLINT), () -> Items.FLINT, null);
        find("copper_nugget", smallOre(OreMineral.NATIVE_COPPER, OreMineral.MALACHITE), smallOreIcon(OreMineral.NATIVE_COPPER), "t1/crucible");
        find("tin_nugget", smallOre(OreMineral.CASSITERITE, OreMineral.BISMUTHINITE, OreMineral.TENNANTITE),
                smallOreIcon(OreMineral.CASSITERITE), "t2/bronze");
        find("iron_nugget", smallOre(OreMineral.HEMATITE, OreMineral.MAGNETITE, OreMineral.LIMONITE),
                smallOreIcon(OreMineral.HEMATITE), "t3/bloomery");
        find("gold_nugget", smallOre(OreMineral.NATIVE_GOLD), smallOreIcon(OreMineral.NATIVE_GOLD), null);
        find("clay", stack -> stack.is(Items.CLAY_BALL), () -> Items.CLAY_BALL, "t1/clay_forming");
        find("raw_hide", stack -> stack.is(ModItems.RAW_HIDE.get()), ModItems.RAW_HIDE::get, "t3/leather");
        find("coal", stack -> stack.is(Items.COAL), () -> Items.COAL, "t4/coke_oven");
        find("survey_notes", stack -> stack.is(StructureContent.SURVEY_NOTES.get()), StructureContent.SURVEY_NOTES::get, null);
    }

    /** Kinetic network overloaded for the first time. */
    public static final String OVERSTRESS = "overstress";

    /** A pony left standing at a stop with no hay. */
    public static final String HUNGRY_PONY = "hungry_pony";
    public static final String BOILER_DRY = "boiler_dry";

    private Observations() {}

    private static void find(String id, Predicate<ItemStack> matches, Supplier<Item> icon, String reveals) {
        FINDS.add(new Find(id, matches, icon, reveals));
    }

    private static Predicate<ItemStack> smallOre(OreMineral... minerals) {
        Set<OreMineral> set = EnumSet.noneOf(OreMineral.class);
        set.addAll(List.of(minerals));
        return stack -> {
            for (OreMineral mineral : set) {
                var item = ModItems.SMALL_ORES.get(mineral);
                if (item != null && stack.is(item.get())) return true;
            }
            return false;
        };
    }

    private static Supplier<Item> smallOreIcon(OreMineral mineral) {
        return () -> ModItems.SMALL_ORES.containsKey(mineral) ? ModItems.SMALL_ORES.get(mineral).get() : Items.FLINT;
    }

    /** Checks the inventory for first finds. Cheap: only finds not yet made are tested. */
    public static void scan(ServerPlayer player) {
        JournalState state = JournalContent.state(player);
        Inventory inventory = player.getInventory();
        for (Find find : FINDS) {
            if (state.seen(find.id())) continue;
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                if (find.matches().test(inventory.getItem(i))) {
                    Leads.observe(player, find.id(), KEY + find.id(), List.of(), id(find.icon().get()), find.reveals());
                    break;
                }
            }
        }
    }

    /**
     * Swinging at ore that is too hard for the pick in hand: the note says what it needs and opens the lead
     * for that pick.
     */
    public static void tooHard(ServerPlayer player, BlockState state) {
        if (!(state.getBlock() instanceof OreBlock ore) || player.hasCorrectToolForDrops(state)) return;
        OreMineral mineral = ore.mineral();
        String tool;
        String reveals;
        if (state.is(ModTags.Blocks.NEEDS_STEEL_TOOL)) {
            tool = "steel";
            reveals = "t4/steel";
        } else if (mineral.needsWroughtIronTool()) {
            tool = "wrought_iron";
            reveals = "t3/iron_pickaxe";
        } else if (mineral.needsBronzeTool()) {
            tool = "bronze";
            reveals = "t2/bronze_tools";
        } else if (mineral.needsCopperTool()) {
            tool = "copper";
            reveals = "t2/copper_pickaxe";
        } else {
            return;
        }
        Leads.observe(player, "too_hard/" + mineral.id(), KEY + "too_hard",
                List.of(state.getBlock().getDescriptionId(), KEY + "too_hard." + tool), id(state.getBlock().asItem()), reveals);
    }

    /** The first overloaded kinetic network anyone nearby sees. */
    public static void overstress(Level level, BlockPos pos) {
        Leads.observeNear(level, pos, OVERSTRESS, KEY + OVERSTRESS, List.of(), id(ModItems.WOODEN_AXLE.get()), null);
    }

    /** The first hungry pony anyone nearby sees. */
    public static void hungryPony(Level level, BlockPos pos) {
        Leads.observeNear(level, pos, HUNGRY_PONY, KEY + HUNGRY_PONY, List.of(), id(dev.strataindustria.transport.rail.RailRegistry.HARNESS.get()), null);
    }

    /** The first locomotive that runs out of water anyone nearby sees. */
    public static void boilerDry(Level level, BlockPos pos) {
        Leads.observeNear(level, pos, BOILER_DRY, KEY + BOILER_DRY, List.of(), id(dev.strataindustria.transport.rail.RailwayRegistry.STEAM_LOCOMOTIVE.get()), null);
    }

    static Identifier id(Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }
}
