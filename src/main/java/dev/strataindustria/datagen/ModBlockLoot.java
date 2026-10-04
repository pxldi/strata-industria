package dev.strataindustria.datagen;

import dev.strataindustria.charcoal.CharcoalPileBlock;
import dev.strataindustria.geology.OreGrade;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModDataComponents;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.Tier4Blocks;
import dev.strataindustria.registry.Tier5Blocks;
import dev.strataindustria.registry.Tier5DataComponents;
import dev.strataindustria.power.ElectricTier;
import java.util.Set;
import net.minecraft.world.level.storage.loot.entries.UniformContainerBase;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.advancements.predicates.StatePropertiesPredicate;
import net.minecraft.core.Holder;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.MatchBlock;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Block drop tables. */
final class ModBlockLoot extends BlockLootSubProvider {
    ModBlockLoot(LootTableSubProvider.Context context) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), context);
    }

    @Override
    protected void generate() {
        Holder<Enchantment> fortune = enchantments.getOrThrow(Enchantments.FORTUNE);

        for (Rock rock : Rock.values()) {
            Block raw = ModBlocks.RAW_ROCK.get(rock).get();
            var loose = ModItems.LOOSE_ROCK.get(rock).get();
            // Raw rock breaks into one or two loose rocks; silk touch keeps the block.
            add(raw, LootTable.lootTable()
                    .withPool(LootPool.lootPool()
                            .setRolls(ContextIntProviders.exactly(1))
                            .add(LootItem.lootTableItem(raw).when(hasSilkTouch())
                                    .otherwise(applyExplosionDecay(raw, LootItem.lootTableItem(loose)))))
                    .withPool(LootPool.lootPool()
                            .setRolls(ContextIntProviders.exactly(1))
                            .when(doesNotHaveSilkTouch())
                            .add(applyExplosionDecay(raw, LootItem.lootTableItem(loose)))
                            .when(LootItemRandomChanceCondition.randomChance(0.5f))));
            dropSelf(ModBlocks.COBBLED_ROCK.get(rock).get());
            dropSelf(ModBlocks.LOOSE_ROCK.get(rock).get());

            for (OreMineral mineral : OreMineral.inRockValues()) {
                oreDrops(ModBlocks.ORES.get(rock).get(mineral).get(), mineral, fortune);
            }
        }

        for (OreMineral mineral : OreMineral.values()) {
            // Tier 4 spec 4.4: coal and sulfur indicators drop one of their item.
            if (mineral.hasPieces()) dropSelf(ModBlocks.SMALL_ORES.get(mineral).get());
            else dropOther(ModBlocks.SMALL_ORES.get(mineral).get(), plainDrop(mineral));
        }
        dropOther(ModBlocks.LOOSE_STICK.get(), Items.STICK);
        dev.strataindustria.flora.FloraBlocks.PLANTS.values().forEach(plant -> dropSelf(plant.get()));
        dropOther(ModBlocks.LOOSE_FLINT.get(), Items.FLINT);
        dropSelf(ModBlocks.FIRE_PIT.get());
        // The pit kiln drops what it holds itself and has no loot table.
        add(ModBlocks.LARGE_VESSEL.get(), createShulkerBoxDrop(ModBlocks.LARGE_VESSEL.get()));
        crucible();
        charcoalPile();
        dropSelf(ModBlocks.FORGE.get());
        dropSelf(ModBlocks.QUERN.get());
        dropSelf(dev.strataindustria.registry.PrologueRegistry.BRICK_KILN.get());
        dropSelf(dev.strataindustria.registry.PrologueRegistry.CASTING_TABLE.get());
        // Spec 9.1: a stone anvil cannot be picked up and breaks back into two loose rocks.
        for (var entry : ModBlocks.STONE_ANVILS.entrySet()) {
            Block anvil = entry.getValue().get();
            add(anvil, LootTable.lootTable().withPool(applyExplosionCondition(anvil, LootPool.lootPool()
                    .setRolls(ContextIntProviders.exactly(1))
                    .add(LootItem.lootTableItem(ModItems.LOOSE_ROCK.get(entry.getKey()).get())
                            .apply(SetItemCountFunction.setCount(ContextIntProviders.exactly(2)))))));
        }
        dropSelf(ModBlocks.BRONZE_ANVIL.get());
        dropSelf(ModBlocks.WROUGHT_IRON_ANVIL.get());

        // Tier 3 spec 3 and 4.
        oreDrops(ModBlocks.BOG_IRON.get(), OreMineral.LIMONITE, fortune);
        add(ModBlocks.FIRE_CLAY.get(), block -> createSingleItemTableWithSilkTouch(block, ModItems.FIRE_CLAY_BALL.get(),
                ContextIntProviders.exactly(4)));
        add(ModBlocks.LIGNITE_SEAM.get(), block -> createSilkTouchDispatchTable(block, applyExplosionDecay(block,
                LootItem.lootTableItem(ModItems.LIGNITE.get())
                        .apply(SetItemCountFunction.setCount(ContextIntProviders.exactly(2)))
                        .apply(ApplyBonusCount.addUniformBonusCount(fortune, 1)))));
        add(ModBlocks.BAUXITE_BED.get(), block -> createSilkTouchDispatchTable(block, applyExplosionDecay(block,
                LootItem.lootTableItem(ModItems.BAUXITE.get())
                        .apply(SetItemCountFunction.setCount(ContextIntProviders.exactly(3)))
                        .apply(ApplyBonusCount.addUniformBonusCount(fortune, 1)))));
        dropSelf(ModBlocks.FIRE_BRICKS.get());
        dropSelf(ModBlocks.BLOOMERY.get());
        for (var block : java.util.List.of(ModBlocks.WOODEN_AXLE, ModBlocks.WOODEN_GEARBOX, ModBlocks.HAND_CRANK, ModBlocks.WATER_WHEEL,
                ModBlocks.MILLSTONE, ModBlocks.BELLOWS, ModBlocks.SAW_MILL, ModBlocks.TRIP_HAMMER, ModBlocks.CORE_SAMPLER, ModBlocks.SLUICE,
                ModBlocks.STEP_UP_GEARBOX, ModBlocks.PULLEY, ModBlocks.WINDMILL_BEARING, ModBlocks.WINDMILL_SAIL,
                ModBlocks.SOAKING_BARREL)) {
            dropSelf(block.get());
        }
        add(ModBlocks.FIRE_BRICK_SLAB.get(), this::createSlabItemTable);
        dropSelf(ModBlocks.FIRE_BRICK_STAIRS.get());
        dropSelf(ModBlocks.FIRE_BRICK_WALL.get());
        dropSelf(ModBlocks.PLACER_GRAVEL.get());
        dropSelf(ModBlocks.PLACER_SAND.get());
        // Tier 4 spec 5.
        for (var block : java.util.List.of(Tier4Blocks.COKE_OVEN_BRICKS, Tier4Blocks.COKE_OVEN_DOOR, Tier4Blocks.COKE_BLOCK,
                Tier4Blocks.TREATED_PLANKS, Tier4Blocks.TREATED_STAIRS, Tier4Blocks.TREATED_FENCE, Tier4Blocks.STEEL_ANVIL,
                Tier4Blocks.IRON_AXLE, Tier4Blocks.IRON_GEARBOX, Tier4Blocks.IRON_STEP_UP_GEARBOX, Tier4Blocks.FIREBOX,
                Tier4Blocks.BRONZE_BOILER, Tier4Blocks.CRACKED_BRONZE_BOILER, Tier4Blocks.COPPER_FLUID_PIPE, Tier4Blocks.BRONZE_FLUID_PIPE,
                Tier4Blocks.STEEL_FLUID_PIPE, Tier4Blocks.PRESSURE_GAUGE, dev.strataindustria.listening.ListeningBlocks.STEAM_WHISTLE, Tier4Blocks.MECHANICAL_PUMP, Tier4Blocks.STEAM_ENGINE,
                Tier4Blocks.CRUSHER, Tier4Blocks.WASHER, Tier4Blocks.REFRACTORY_CASING, Tier4Blocks.BLAST_FURNACE_CONTROLLER,
                Tier4Blocks.TUYERE, Tier4Blocks.CHARGING_HATCH, Tier4Blocks.TAP_HATCH, Tier4Blocks.BLOWER,
                Tier4Blocks.CONVERTER_CONTROLLER, Tier4Blocks.COPPER_HEAT_PIPE, Tier4Blocks.REFRACTORY_HEAT_DUCT, Tier4Blocks.HEAT_INLET,
                Tier4Blocks.INSULATED_COPPER_HEAT_PIPE, Tier4Blocks.INSULATED_REFRACTORY_HEAT_DUCT, Tier4Blocks.KILN,
                Tier4Blocks.ROASTER, Tier4Blocks.STEAM_HAMMER, Tier4Blocks.VALVE, Tier4Blocks.FLUID_TANK,
                Tier4Blocks.BLOWING_ENGINE, Tier4Blocks.STEEL_BOILER_SHELL, Tier4Blocks.BOILER_FLUID_PORT, Tier4Blocks.BOILER_CONTROLLER,
                Tier4Blocks.CRACKED_BOILER_CONTROLLER, Tier4Blocks.CHUTE, Tier4Blocks.INSERTER, Tier4Blocks.CONVEYOR_BELT, Tier4Blocks.BELT_DIVERTER)) {
            dropSelf(block.get());
        }
        add(Tier4Blocks.TREATED_SLAB.get(), this::createSlabItemTable);
        dropSelf(dev.strataindustria.ledger.LedgerRegistry.BUILDERS_CRATE.get());
        FootData.loot(this::add, blocks);
        dropSelf(dev.strataindustria.registry.TransportBlocks.OUTPOST_CHARTER.get());
        dropSelf(dev.strataindustria.bronze.BronzeRegistry.FUME_HOOD.get());
        // A specimen cabinet keeps its collection.
        Block cabinet = dev.strataindustria.cabinet.CabinetRegistry.SPECIMEN_CABINET.get();
        add(cabinet, LootTable.lootTable().withPool(applyExplosionCondition(cabinet, LootPool.lootPool()
                .setRolls(ContextIntProviders.exactly(1))
                .add(LootItem.lootTableItem(cabinet)
                        .apply(CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY)
                                .include(dev.strataindustria.cabinet.CabinetRegistry.HELD.get()))))));
        // A bell keeps the tone it was cast with.
        Block bell = dev.strataindustria.bronze.BronzeRegistry.BELL.get();
        add(bell, LootTable.lootTable().withPool(applyExplosionCondition(bell, LootPool.lootPool()
                .setRolls(ContextIntProviders.exactly(1))
                .add(LootItem.lootTableItem(bell)
                        .apply(CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY)
                                .include(dev.strataindustria.bronze.BronzeRegistry.BELL_TONE.get()))))));
        tier5();
    }

    /** The crucible keeps its pieces, its melt and its heat when picked up, like a shulker box. */
    private void crucible() {
        crucible(ModBlocks.CRUCIBLE.get());
        crucible(Tier4Blocks.REFRACTORY_CRUCIBLE.get());
        // The smelter keeps its load and melt like a crucible does.
        crucible(Tier4Blocks.SMELTER.get());
    }

    private void crucible(Block block) {
        add(block, LootTable.lootTable().withPool(applyExplosionCondition(block, LootPool.lootPool()
                .setRolls(ContextIntProviders.exactly(1))
                .add(LootItem.lootTableItem(block)
                        .apply(CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY)
                                .include(DataComponents.CUSTOM_NAME)
                                .include(DataComponents.CONTAINER)
                                .include(ModDataComponents.CRUCIBLE_MELT.get())
                                .include(ModDataComponents.TEMPERATURE.get()))))));
    }

    /** Spec 4.4: the pile's state says how much charcoal and ash the burn left. The log pile drops its own logs. */
    private void charcoalPile() {
        Block block = ModBlocks.CHARCOAL_PILE.get();
        LootTable.Builder table = LootTable.lootTable();
        for (int n = 1; n <= CharcoalPileBlock.MAX_CHARCOAL; n++) {
            table.withPool(LootPool.lootPool()
                    .setRolls(ContextIntProviders.exactly(1))
                    .when(MatchBlock.blockMatches(blocks, block,
                            StatePropertiesPredicate.Builder.properties().hasProperty(CharcoalPileBlock.CHARCOAL, n)))
                    .add(LootItem.lootTableItem(Items.CHARCOAL).apply(SetItemCountFunction.setCount(ContextIntProviders.exactly(n)))));
        }
        for (int n = 1; n <= CharcoalPileBlock.MAX_ASH; n++) {
            table.withPool(LootPool.lootPool()
                    .setRolls(ContextIntProviders.exactly(1))
                    .when(MatchBlock.blockMatches(blocks, block,
                            StatePropertiesPredicate.Builder.properties().hasProperty(CharcoalPileBlock.ASH, n)))
                    .add(LootItem.lootTableItem(ModItems.ASH.get()).apply(SetItemCountFunction.setCount(ContextIntProviders.exactly(n)))));
        }
        add(block, table);
    }

    private static net.minecraft.world.level.ItemLike plainDrop(OreMineral mineral) {
        return switch (mineral) {
            case BITUMINOUS_COAL -> Items.COAL;
            case CINNABAR -> Items.REDSTONE;
            case LAZURITE -> Items.LAPIS_LAZULI;
            default -> dev.strataindustria.registry.Tier4Items.SULFUR.get();
        };
    }

    /** Items per ore block by grade: coal and sulfur 1, 2, 3; cinnabar and lazurite 2, 4, 6 (tier 5 spec 18). */
    private static int plainCount(OreMineral mineral, OreGrade grade) {
        int base = grade.ordinal() + 1;
        return mineral == OreMineral.CINNABAR || mineral == OreMineral.LAZURITE ? base * 2 : base;
    }

    /**
     * One pool per grade: the ore piece of that grade, with fortune adding up to one extra. Coal and
     * sulfur drop 1, 2 or 3 of their item by grade instead (tier 4 spec 4.4); silk touch keeps those blocks.
     */
    private void oreDrops(Block block, OreMineral mineral, Holder<Enchantment> fortune) {
        LootTable.Builder table = LootTable.lootTable();
        if (!mineral.hasPieces()) {
            for (OreGrade grade : OreGrade.values()) {
                table.withPool(LootPool.lootPool()
                        .setRolls(ContextIntProviders.exactly(1))
                        .when(MatchBlock.blockMatches(blocks, block,
                                StatePropertiesPredicate.Builder.properties().hasProperty(OreGrade.PROPERTY, grade)))
                        .add(LootItem.lootTableItem(block).when(hasSilkTouch())
                                .otherwise(applyExplosionDecay(block, LootItem.lootTableItem(plainDrop(mineral))
                                        .apply(SetItemCountFunction.setCount(ContextIntProviders.exactly(plainCount(mineral, grade))))
                                        .apply(ApplyBonusCount.addUniformBonusCount(fortune, 1))))));
            }
            add(block, table);
            return;
        }
        for (OreGrade grade : OreGrade.values()) {
            table.withPool(LootPool.lootPool()
                    .setRolls(ContextIntProviders.exactly(1))
                    .when(MatchBlock.blockMatches(blocks, block,
                            StatePropertiesPredicate.Builder.properties().hasProperty(OreGrade.PROPERTY, grade)))
                    .add(applyExplosionDecay(block, LootItem.lootTableItem(ModItems.orePiece(mineral, grade))
                            .apply(ApplyBonusCount.addUniformBonusCount(fortune, 1)))));
        }
        add(block, table);
    }

    /** Tier 5: cables, the dynamo and the hull drop themselves; a battery box keeps its charge (spec 7.4). */
    private void tier5() {
        for (var block : java.util.List.of(Tier5Blocks.LV_CABLE, Tier5Blocks.MV_CABLE, Tier5Blocks.KINETIC_DYNAMO, Tier5Blocks.LV_MACHINE_HULL, Tier5Blocks.TREE_TAP, Tier5Blocks.TRANSFORMER,
                Tier5Blocks.TREATED_LOG, Tier5Blocks.UTILITY_POLE, Tier5Blocks.POLE_INSULATOR, Tier5Blocks.LIQUID_FUEL_BURNER, Tier5Blocks.ELECTRIC_PUMP, Tier5Blocks.EXTRUDER, dev.strataindustria.grid.GridBlocks.ELECTRIC_LAMP, dev.strataindustria.grid.GridBlocks.LEYDEN_JAR)) {
            dropSelf(block.get());
        }
        for (var block : java.util.List.of(dev.strataindustria.logistics.Tier5Logistics.ITEM_PIPE, dev.strataindustria.logistics.Tier5Logistics.PIPE_EXTRACTOR,
                dev.strataindustria.logistics.Tier5Logistics.FAST_PIPE_EXTRACTOR, dev.strataindustria.logistics.Tier5Logistics.STORAGE_CONTROLLER,
                dev.strataindustria.logistics.Tier5Logistics.FLUID_FILTER)) {
            dropSelf(block.get());
        }
        // Spec 9.5: an upgraded machine drops with machine_tier = mv so it places back as MV.
        for (var holder : Tier5Blocks.upgradable()) {
            Block block = holder.get();
            if (block == Tier5Blocks.BATTERY_BOX.get()) continue;
            add(block, LootTable.lootTable().withPool(applyExplosionCondition(block, LootPool.lootPool()
                    .setRolls(ContextIntProviders.exactly(1))
                    .add(upgradedDrop(block, LootItem.lootTableItem(block))))));
        }
        Block box = Tier5Blocks.BATTERY_BOX.get();
        add(box, LootTable.lootTable().withPool(applyExplosionCondition(box, LootPool.lootPool()
                .setRolls(ContextIntProviders.exactly(1))
                .add(upgradedDrop(box, LootItem.lootTableItem(box)
                        .apply(CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY)
                                .include(DataComponents.CUSTOM_NAME)
                                .include(Tier5DataComponents.ENERGY.get())))))));
    }

    private UniformContainerBase.Builder<?> upgradedDrop(Block block, UniformContainerBase.Builder<?> drop) {
        return drop.apply(SetComponentsFunction.setComponent(Tier5DataComponents.MACHINE_TIER.get(), ElectricTier.MV)
                .when(MatchBlock.blockMatches(blocks, block, StatePropertiesPredicate.Builder.properties()
                        .hasProperty(ElectricTier.PROPERTY, ElectricTier.MV))));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(DeferredHolder::value).map(Block.class::cast)::iterator;
    }
}
