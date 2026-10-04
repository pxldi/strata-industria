package dev.strataindustria.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.geology.GeologyContext;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.geology.VeinCells;
import dev.strataindustria.geology.VeinType;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * The structure type behind the world structures (structures spec 3). Each start reads the vein cells
 * around its chunk, picks a vein to camp beside, checks the ground, and lays out its pieces in a fixed
 * order: levelled pieces first, so the soft edges they blend into the land never cut into a piece built
 * later, then paths and heaps that follow the ground, then cuttings into hillsides, then the tunnel.
 */
public class CampStructure extends Structure {
    public enum Layout implements StringRepresentable {
        CHARCOAL_BURNERS_CLEARING, PROSPECTOR_CAMP, MINING_CAMP, COLLAPSED_ADIT, RUINED_BLOOMERY, PLACER_WORKINGS;

        public static final Codec<Layout> CODEC = StringRepresentable.fromEnum(Layout::values);

        public String id() {
            return getSerializedName();
        }

        public ResourceKey<Structure> key() {
            return ResourceKey.create(Registries.STRUCTURE, StrataIndustria.id(id()));
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final MapCodec<CampStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            Layout.CODEC.fieldOf("layout").forGetter(s -> s.layout)
    ).apply(i, CampStructure::new));

    /** The four minerals a mining camp works (structures spec 6.3). */
    private static final List<OreMineral> MINING_MINERALS =
            List.of(OreMineral.NATIVE_COPPER, OreMineral.MALACHITE, OreMineral.TENNANTITE, OreMineral.CASSITERITE);
    /** The iron ores an old bloomery was built beside (structures spec 6.5). */
    private static final List<OreMineral> BLOOMERY_MINERALS = List.of(OreMineral.HEMATITE, OreMineral.MAGNETITE);

    private final Layout layout;

    public CampStructure(StructureSettings settings, Layout layout) {
        super(settings);
        this.layout = layout;
    }

    @Override
    public StructureType<?> type() {
        return StructureContent.CAMP.get();
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        Site site = new Site(context);
        return switch (layout) {
            case CHARCOAL_BURNERS_CLEARING -> clearing(site);
            case PROSPECTOR_CAMP -> prospectorCamp(site);
            case MINING_CAMP -> miningCamp(site);
            case COLLAPSED_ADIT -> collapsedAdit(site);
            case RUINED_BLOOMERY -> ruinedBloomery(site);
            case PLACER_WORKINGS -> placerWorkings(site);
        };
    }

    // ---------------------------------------------------------------- 6.1 charcoal burners' clearing

    private static Optional<GenerationStub> clearing(Site site) {
        int cx = site.context.chunkPos().getMiddleBlockX(), cz = site.context.chunkPos().getMiddleBlockZ();
        Plan plan = Plans.CLEARING;
        int minX = cx - plan.width() / 2, minZ = cz - plan.depth() / 2;
        OptionalInt ground = site.level(minX, minZ, minX + plan.width() - 1, minZ + plan.depth() - 1, 2);
        if (ground.isEmpty()) return Optional.empty();
        RandomSource random = site.context.random();
        Rotation rotation = Rotation.getRandom(random);
        PlanPiece.Wood wood = site.wood(cx, ground.getAsInt(), cz);
        long seed = random.nextLong();
        return Optional.of(new GenerationStub(new BlockPos(cx, ground.getAsInt(), cz), builder ->
                builder.addPiece(new PlanPiece(plan, rotation, minX, minZ, ground.getAsInt(), OreMineral.MALACHITE, wood, seed))));
    }

    // ---------------------------------------------------------------- 6.2 prospector's camp

    private static Optional<GenerationStub> prospectorCamp(Site site) {
        int cx = site.context.chunkPos().getMiddleBlockX(), cz = site.context.chunkPos().getMiddleBlockZ();
        GeologyContext geology = StructureGeology.of(site.context);
        if (geology == null) return Optional.empty();
        // Only veins of the stone and copper tiers: the camp's pebbles are its loot (structures spec 4.1, L6).
        VeinCells.Vein vein = anchor(geology, cx, cz, 48, 32, true, type -> !mainMineral(type, null).needsBronzeTool());
        if (vein == null) return Optional.empty();
        RandomSource random = site.context.random();

        // The camp stands just outside the vein, where its stained ground and loose stones are in sight.
        double[] away = away(vein, cx, cz, random);
        int dist = vein.radiusH() + 2 + random.nextInt(5) + 4;
        int campX = vein.x() + (int) Math.round(away[0] * dist), campZ = vein.z() + (int) Math.round(away[1] * dist);
        Direction toVein = cardinal(-away[0], -away[1]);
        Plan plan = Plans.PROSPECTOR_CAMP;
        int minX = campX - plan.width() / 2, minZ = campZ - plan.depth() / 2;
        OptionalInt ground = site.level(minX, minZ, minX + plan.width() - 1, minZ + plan.depth() - 1, 3);
        if (ground.isEmpty()) return Optional.empty();

        OreMineral mineral = mainMineral(vein, null);
        PlanPiece.Wood wood = site.wood(campX, ground.getAsInt(), campZ);
        Rotation rotation = facing(toVein);
        Direction side = toVein.getClockWise();
        long campSeed = random.nextLong();
        PlanPiece camp = new PlanPiece(plan, rotation, minX, minZ, ground.getAsInt(), mineral, wood, campSeed);

        // Three cairns lead on from the end of the trench toward the vein.
        List<PlanPiece> cairns = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            int along = plan.depth() / 2 + 3 + 4 * i, across = 2 + i;
            int cairnX = campX + toVein.getStepX() * along + side.getStepX() * across;
            int cairnZ = campZ + toVein.getStepZ() * along + side.getStepZ() * across;
            cairns.add(new PlanPiece(Plans.CAIRN, Rotation.NONE, cairnX, cairnZ, site.surface(cairnX, cairnZ), mineral, wood,
                    random.nextLong()));
        }

        return Optional.of(new GenerationStub(new BlockPos(campX, ground.getAsInt(), campZ), builder -> {
            builder.addPiece(camp);
            cairns.forEach(builder::addPiece);
        }));
    }

    // ---------------------------------------------------------------- 6.3 mining camp

    /*
     * Camp-local layout, +x toward the vein, in blocks from the yard's north-west corner:
     *
     *                 north tent (z -13..-6)
     *                     path
     *  forge shed  path   YARD    path  ore sorting  path  portal or shaft head (x 30..)
     *  (x -11..-6)        (0..12)       (x 18..24)
     *                     path          spoil heap (z 13..19)
     *                 south tent (z 18..)
     */
    private static final int YARD_SIZE = 13, PORTAL_X = 30;

    private static Optional<GenerationStub> miningCamp(Site site) {
        int cx = site.context.chunkPos().getMiddleBlockX(), cz = site.context.chunkPos().getMiddleBlockZ();
        GeologyContext geology = StructureGeology.of(site.context);
        if (geology == null) return Optional.empty();
        VeinCells.Vein vein = anchor(geology, cx, cz, 40, 24, true, type -> MINING_MINERALS.stream().anyMatch(m -> holds(type, m)));
        if (vein == null) return Optional.empty();
        RandomSource random = site.context.random();

        double[] away = away(vein, cx, cz, random);
        int dist = Mth.clamp(vein.radiusH() + 28, 30, 50);
        int yardX = vein.x() + (int) Math.round(away[0] * dist), yardZ = vein.z() + (int) Math.round(away[1] * dist);
        Direction toVein = cardinal(-away[0], -away[1]);
        Frame frame = Frame.centred(yardX, yardZ, campRotation(toVein), YARD_SIZE / 2, YARD_SIZE / 2);

        OreMineral mineral = mainMineral(vein, MINING_MINERALS);
        OptionalInt yardGround = site.level(frame, 0, 0, YARD_SIZE - 1, YARD_SIZE - 1, 4);
        if (yardGround.isEmpty()) return Optional.empty();
        PlanPiece.Wood wood = site.wood(yardX, yardGround.getAsInt(), yardZ);
        Camp camp = new Camp(site, frame, mineral, wood, random);

        // Levelled pieces first.
        camp.levelled(Plans.YARD, Rotation.NONE, 0, 0, yardGround);
        boolean shed = camp.levelled(Plans.FORGE_SHED, Rotation.COUNTERCLOCKWISE_90, -11, 3, 5);
        boolean sorting = camp.levelled(Plans.ORE_SORTING, Rotation.NONE, 18, 3, 5);
        // The bunkhouse with the foreman's office stands north of the yard, its door toward the fire.
        boolean north = camp.levelled(Plans.BUNKHOUSE, Rotation.NONE, 1, -5 - Plans.BUNKHOUSE.depth(), 5);
        boolean south = random.nextBoolean() && camp.levelled(Plans.TENT_SMALL, Rotation.CLOCKWISE_180, 4, 18, 5);

        // Find a hillside for an adit; on flat ground, sink a shaft instead.
        int portalAt = -1, portalGround = 0;
        int[] line = new int[16];
        for (int i = 0; i < line.length; i++) line[i] = site.surface(frame.x(PORTAL_X + i, 6), frame.z(PORTAL_X + i, 6));
        for (int m = 0; m <= 5 && portalAt < 0; m++) {
            if (site.wet(frame.x(PORTAL_X + m, 6), frame.z(PORTAL_X + m, 6))) continue;
            for (int k = 1; k <= 10 && m + k < line.length; k++) {
                if (line[m + k] - line[m] >= 4) {
                    portalAt = PORTAL_X + m;
                    portalGround = line[m];
                    break;
                }
            }
        }
        boolean shaft = portalAt < 0;
        Direction heading = frame.rotation().rotate(Direction.EAST);
        BlockPos start;
        int shaftDepth = 0;
        // The shaft head's ladder hangs on the north side of its hole.
        Direction ladder = frame.rotation().rotate(Direction.NORTH);
        if (shaft) {
            if (!camp.levelled(Plans.SHAFT_HEAD, Rotation.NONE, PORTAL_X, 4, 3)) return Optional.empty();
            int ground = camp.lastGround;
            int top = ground - 3;
            shaftDepth = Mth.clamp(top - (vein.y() - 2), 3, 24);
            start = new BlockPos(frame.x(PORTAL_X + 2, 6), top, frame.z(PORTAL_X + 2, 6));
        } else {
            start = new BlockPos(frame.x(portalAt + 4, 6), portalGround, frame.z(portalAt + 4, 6));
        }

        // Then the pieces that follow the ground.
        camp.terrain(Plans.path(5), Rotation.CLOCKWISE_90, 13, 5);
        // The rail from the adit across the yard edge to the sorting floor, and the older wooden track beside it.
        camp.terrain(Plans.RAIL, Rotation.NONE, 25, 6);
        camp.terrain(Plans.TRAMWAY, Rotation.NONE, 14, 10);
        if (shed) camp.terrain(Plans.path(5), Rotation.CLOCKWISE_90, -5, 5);
        if (north) camp.terrain(Plans.path(5), Rotation.NONE, 5, -5);
        if (south) camp.terrain(Plans.path(5), Rotation.NONE, 5, 13);
        if (sorting) camp.terrain(Plans.SPOIL_HEAP, Rotation.NONE, 18, 13);
        else camp.terrain(Plans.SPOIL_HEAP, Rotation.NONE, 18, 3);

        // The cutting into the hill, then the tunnel itself.
        if (!shaft) camp.embedded(Plans.ADIT_PORTAL, Rotation.COUNTERCLOCKWISE_90, portalAt, 3, portalGround);
        AditPiece.Route route = new AditPiece.Route(shaft ? 24 : 36, random.nextInt(4) == 0, false, "adit/mining");
        Optional<AditPiece> adit = AditPiece.plan(vein, start, heading, shaftDepth, ladder, route, mineral, wood, random);
        if (adit.isEmpty()) return Optional.empty();
        camp.pieces.add(adit.get());

        int y = yardGround.getAsInt();
        return Optional.of(new GenerationStub(new BlockPos(yardX, y, yardZ), builder -> camp.pieces.forEach(builder::addPiece)));
    }

    /** Collects the pieces of a mining camp, placed in camp-local coordinates. */
    private static final class Camp {
        final Site site;
        final Frame frame;
        final OreMineral mineral;
        final PlanPiece.Wood wood;
        final RandomSource random;
        final List<StructurePiece> pieces = new ArrayList<>();
        int lastGround;

        Camp(Site site, Frame frame, OreMineral mineral, PlanPiece.Wood wood, RandomSource random) {
            this.site = site;
            this.frame = frame;
            this.mineral = mineral;
            this.wood = wood;
            this.random = random;
        }

        /** World footprint of a plan turned by {@code rotation} with its local north-west corner at (lx, lz). */
        int[] footprint(Plan plan, Rotation rotation, int lx, int lz) {
            int[] size = PlanPiece.size(plan, rotation);
            return frame.rect(lx, lz, lx + size[0] - 1, lz + size[1] - 1);
        }

        boolean levelled(Plan plan, Rotation rotation, int lx, int lz, int maxUneven) {
            int[] r = footprint(plan, rotation, lx, lz);
            return levelled(plan, rotation, lx, lz, site.level(r[0], r[1], r[2], r[3], maxUneven));
        }

        boolean levelled(Plan plan, Rotation rotation, int lx, int lz, OptionalInt ground) {
            if (ground.isEmpty()) return false;
            int[] r = footprint(plan, rotation, lx, lz);
            lastGround = ground.getAsInt();
            pieces.add(new PlanPiece(plan, frame.rotation().getRotated(rotation), r[0], r[1], lastGround, mineral, wood,
                    random.nextLong()));
            return true;
        }

        void terrain(Plan plan, Rotation rotation, int lx, int lz) {
            int[] r = footprint(plan, rotation, lx, lz);
            int ground = site.surface((r[0] + r[2]) / 2, (r[1] + r[3]) / 2);
            pieces.add(new PlanPiece(plan, frame.rotation().getRotated(rotation), r[0], r[1], ground, mineral, wood, random.nextLong()));
        }

        void embedded(Plan plan, Rotation rotation, int lx, int lz, int ground) {
            int[] r = footprint(plan, rotation, lx, lz);
            pieces.add(new PlanPiece(plan, frame.rotation().getRotated(rotation), r[0], r[1], ground, mineral, wood, random.nextLong()));
        }
    }

    // ---------------------------------------------------------------- 6.4 collapsed adit

    private static Optional<GenerationStub> collapsedAdit(Site site) {
        int cx = site.context.chunkPos().getMiddleBlockX(), cz = site.context.chunkPos().getMiddleBlockZ();
        GeologyContext geology = StructureGeology.of(site.context);
        if (geology == null) return Optional.empty();
        VeinCells.Vein vein = anchor(geology, cx, cz, 40, 32, true, type -> true);
        if (vein == null) return Optional.empty();
        RandomSource random = site.context.random();
        Direction toVein = cardinal(vein.x() - cx, vein.z() - cz);
        Direction side = toVein.getClockWise();

        // Walk toward the vein looking for the foot of a slope that rises at least 5 within 8 blocks.
        int[] line = new int[21];
        for (int i = 0; i < line.length; i++) line[i] = site.surface(cx + toVein.getStepX() * i, cz + toVein.getStepZ() * i);
        int mouth = -1;
        for (int m = 0; m <= 12 && mouth < 0; m++) {
            for (int k = 1; k <= 8; k++) {
                if (line[m + k] - line[m] >= 5) {
                    mouth = m;
                    break;
                }
            }
        }
        if (mouth < 0) return Optional.empty();
        int mx = cx + toVein.getStepX() * mouth, mz = cz + toVein.getStepZ() * mouth;
        int ground = line[mouth];
        if (site.wet(mx, mz)) return Optional.empty();
        for (int o = -3; o <= 3; o += 6) {
            int h = site.surface(mx + side.getStepX() * o, mz + side.getStepZ() * o);
            if (h < ground - 1) return Optional.empty();
        }

        Plan plan = Plans.COLLAPSED_PORTAL;
        Rotation rotation = facing(toVein);
        Frame frame = Frame.centred(mx, mz, rotation, plan.width() / 2, 0);
        int[] r = frame.rect(0, 0, plan.width() - 1, plan.depth() - 1);
        OreMineral mineral = mainMineral(vein, null);
        PlanPiece.Wood wood = site.wood(mx, ground, mz);
        PlanPiece portal = new PlanPiece(plan, rotation, r[0], r[1], ground, mineral, wood, random.nextLong());

        BlockPos start = new BlockPos(frame.x(plan.width() / 2, plan.depth()), ground, frame.z(plan.width() / 2, plan.depth()));
        AditPiece.Route route = new AditPiece.Route(20, false, random.nextBoolean(), "adit/collapsed");
        Optional<AditPiece> adit = AditPiece.plan(vein, start, toVein, 0, Direction.NORTH, route, mineral, wood, random);
        if (adit.isEmpty()) return Optional.empty();
        AditPiece tunnel = adit.get();
        return Optional.of(new GenerationStub(new BlockPos(mx, ground, mz), builder -> {
            builder.addPiece(portal);
            builder.addPiece(tunnel);
        }));
    }

    // ---------------------------------------------------------------- 6.5 ruined bloomery

    private static Optional<GenerationStub> ruinedBloomery(Site site) {
        int cx = site.context.chunkPos().getMiddleBlockX(), cz = site.context.chunkPos().getMiddleBlockZ();
        GeologyContext geology = StructureGeology.of(site.context);
        if (geology == null) return Optional.empty();
        // The old smiths knew where the iron was even where nothing shows at the surface.
        VeinCells.Vein vein = anchor(geology, cx, cz, 64, 48, false, type -> BLOOMERY_MINERALS.stream().anyMatch(m -> holds(type, m)));
        if (vein == null) return Optional.empty();
        RandomSource random = site.context.random();

        double[] away = away(vein, cx, cz, random);
        int dist = 24 + random.nextInt(33);
        int x = vein.x() + (int) Math.round(away[0] * dist), z = vein.z() + (int) Math.round(away[1] * dist);
        Direction toVein = cardinal(-away[0], -away[1]);
        Plan plan = Plans.BLOOMERY_WORKS;
        Rotation rotation = facing(toVein);
        int minX = x - plan.width() / 2, minZ = z - plan.depth() / 2;
        OptionalInt ground = site.level(minX, minZ, minX + plan.width() - 1, minZ + plan.depth() - 1, 3);
        if (ground.isEmpty()) return Optional.empty();

        OreMineral mineral = mainMineral(vein, BLOOMERY_MINERALS);
        PlanPiece.Wood wood = site.wood(x, ground.getAsInt(), z);
        PlanPiece works = new PlanPiece(plan, rotation, minX, minZ, ground.getAsInt(), mineral, wood, random.nextLong());

        // The charcoal was burned off to one side of the stack, most of the time.
        PlanPiece scar = null;
        if (random.nextInt(10) < 7) {
            Direction side = random.nextBoolean() ? toVein.getClockWise() : toVein.getCounterClockWise();
            Plan scarPlan = Plans.CHARCOAL_SCAR;
            int along = Math.max(plan.width(), plan.depth()) / 2 + 3 + scarPlan.width() / 2;
            int sx = x + side.getStepX() * along - scarPlan.width() / 2, sz = z + side.getStepZ() * along - scarPlan.depth() / 2;
            if (!site.wet(sx + scarPlan.width() / 2, sz + scarPlan.depth() / 2)) {
                scar = new PlanPiece(scarPlan, Rotation.NONE, sx, sz, site.surface(sx + scarPlan.width() / 2, sz + scarPlan.depth() / 2),
                        mineral, wood, random.nextLong());
            }
        }

        PlanPiece charcoalScar = scar;
        return Optional.of(new GenerationStub(new BlockPos(x, ground.getAsInt(), z), builder -> {
            builder.addPiece(works);
            if (charcoalScar != null) builder.addPiece(charcoalScar);
        }));
    }

    // ---------------------------------------------------------------- 6.6 placer workings

    private static Optional<GenerationStub> placerWorkings(Site site) {
        int sea = site.context.chunkGenerator().getSeaLevel();
        int x0 = site.context.chunkPos().getMinBlockX(), z0 = site.context.chunkPos().getMinBlockZ();
        RandomSource random = site.context.random();
        int offset = random.nextInt(2);
        // Look for low land with river water close by, on a coarse grid over the chunk.
        for (int i = 0; i < 64; i++) {
            int x = x0 + (i % 8) * 2 + offset, z = z0 + (i / 8) * 2 + offset;
            if (site.wet(x, z) || Math.abs(site.surface(x, z) - (sea - 1)) > 2) continue;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                int k = waterAt(site, x, z, d);
                if (k < 1) continue;
                // The bank's south edge is the last dry block before the water.
                int ex = x + d.getStepX() * (k - 1), ez = z + d.getStepZ() * (k - 1);
                Optional<GenerationStub> bank = placerBank(site, ex, ez, d, random);
                if (bank.isPresent()) return bank;
            }
        }
        return Optional.empty();
    }

    /** Distance to the first river water within 6 blocks along {@code d}, or -1 if other water or none. */
    private static int waterAt(Site site, int x, int z, Direction d) {
        for (int k = 1; k <= 6; k++) {
            int wx = x + d.getStepX() * k, wz = z + d.getStepZ() * k;
            if (!site.wet(wx, wz)) continue;
            return site.river(wx, wz) ? k : -1;
        }
        return -1;
    }

    private static Optional<GenerationStub> placerBank(Site site, int edgeX, int edgeZ, Direction toWater, RandomSource random) {
        Plan plan = Plans.PLACER_BANK;
        Rotation rotation = facing(toWater);
        Frame frame = Frame.centred(edgeX, edgeZ, rotation, 5, plan.depth() - 1);
        int[] r = frame.rect(0, 0, plan.width() - 1, plan.depth() - 1);
        OptionalInt ground = site.level(r[0], r[1], r[2], r[3], 2);
        if (ground.isEmpty()) return Optional.empty();
        int y = ground.getAsInt();
        PlanPiece.Wood wood = site.wood(edgeX, y, edgeZ);
        PlanPiece bank = new PlanPiece(plan, rotation, r[0], r[1], y, OreMineral.NATIVE_GOLD, wood, random.nextLong());
        SluicePiece sluice = new SluicePiece(new BlockPos(edgeX, y, edgeZ), toWater);
        int cx = frame.x(plan.width() / 2, plan.depth() / 2), cz = frame.z(plan.width() / 2, plan.depth() / 2);
        return Optional.of(new GenerationStub(new BlockPos(cx, y, cz), builder -> {
            builder.addPiece(bank);
            builder.addPiece(sluice);
        }));
    }

    // ---------------------------------------------------------------- shared

    /**
     * The nearest vein whose top lies at most {@code maxDepth} below the surface; only veins that show at the
     * surface when {@code indicators} is set.
     */
    private static VeinCells.Vein anchor(GeologyContext geology, int x, int z, int radius, int maxDepth, boolean indicators,
            Predicate<VeinType> filter) {
        VeinCells.Vein best = null;
        long bestDist = Long.MAX_VALUE;
        for (VeinCells.Vein vein : geology.veins().around(x, z)) {
            if ((indicators && !vein.type().indicators()) || !filter.test(vein.type())) continue;
            if (vein.surfaceY() - (vein.y() + vein.radiusV()) > maxDepth) continue;
            long dx = vein.x() - x, dz = vein.z() - z, dist = dx * dx + dz * dz;
            if (dist > (long) radius * radius || dist >= bestDist) continue;
            best = vein;
            bestDist = dist;
        }
        return best;
    }

    private static boolean holds(VeinType type, OreMineral mineral) {
        for (VeinType.MineralWeight m : type.minerals()) if (m.mineral() == mineral) return true;
        return false;
    }

    /** The vein's most common mineral, among {@code allowed} when given. */
    private static OreMineral mainMineral(VeinCells.Vein vein, List<OreMineral> allowed) {
        return mainMineral(vein.type(), allowed);
    }

    private static OreMineral mainMineral(VeinType type, List<OreMineral> allowed) {
        VeinType.MineralWeight best = null;
        for (VeinType.MineralWeight m : type.minerals()) {
            if (allowed != null && !allowed.contains(m.mineral())) continue;
            if (best == null || m.weight() > best.weight()) best = m;
        }
        return best == null ? type.minerals().getFirst().mineral() : best.mineral();
    }

    /** Unit vector from the vein's centre toward a point, random if the point is right above it. */
    private static double[] away(VeinCells.Vein vein, int x, int z, RandomSource random) {
        double dx = x - vein.x(), dz = z - vein.z();
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 1) {
            double angle = random.nextDouble() * Math.PI * 2;
            return new double[] {Math.cos(angle), Math.sin(angle)};
        }
        return new double[] {dx / length, dz / length};
    }

    private static Direction cardinal(double dx, double dz) {
        if (Math.abs(dx) >= Math.abs(dz)) return dx >= 0 ? Direction.EAST : Direction.WEST;
        return dz >= 0 ? Direction.SOUTH : Direction.NORTH;
    }

    /** Turns a plan, drawn with its open side to the south, so that side faces {@code direction}. */
    static Rotation facing(Direction direction) {
        return switch (direction) {
            case WEST -> Rotation.CLOCKWISE_90;
            case NORTH -> Rotation.CLOCKWISE_180;
            case EAST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    /** Turns a camp so its local +x points {@code direction}. */
    static Rotation campRotation(Direction direction) {
        return switch (direction) {
            case SOUTH -> Rotation.CLOCKWISE_90;
            case WEST -> Rotation.CLOCKWISE_180;
            case NORTH -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    /** Camp-local coordinates turned about an origin into world coordinates. */
    record Frame(int originX, int originZ, Rotation rotation) {
        /** A frame whose local point (lx, lz) lands on the world point (x, z). */
        static Frame centred(int x, int z, Rotation rotation, int lx, int lz) {
            Frame zero = new Frame(0, 0, rotation);
            return new Frame(x - zero.x(lx, lz), z - zero.z(lx, lz), rotation);
        }

        int x(int lx, int lz) {
            return originX + switch (rotation) {
                case NONE -> lx;
                case CLOCKWISE_90 -> -lz;
                case CLOCKWISE_180 -> -lx;
                case COUNTERCLOCKWISE_90 -> lz;
            };
        }

        int z(int lx, int lz) {
            return originZ + switch (rotation) {
                case NONE -> lz;
                case CLOCKWISE_90 -> lx;
                case CLOCKWISE_180 -> -lz;
                case COUNTERCLOCKWISE_90 -> -lx;
            };
        }

        /** World rectangle (minX, minZ, maxX, maxZ) of a local rectangle. */
        int[] rect(int lx0, int lz0, int lx1, int lz1) {
            int ax = x(lx0, lz0), az = z(lx0, lz0), bx = x(lx1, lz1), bz = z(lx1, lz1);
            return new int[] {Math.min(ax, bx), Math.min(az, bz), Math.max(ax, bx), Math.max(az, bz)};
        }
    }

    /** Ground heights and biomes read from the chunk generator, before any chunk exists. */
    private record Site(GenerationContext context) {
        int surface(int x, int z) {
            return context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(),
                    context.randomState());
        }

        boolean wet(int x, int z) {
            return context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(),
                    context.randomState()) < surface(x, z);
        }

        /** Height to level a footprint to, or empty if it is too uneven or under water anywhere sampled. */
        OptionalInt level(int minX, int minZ, int maxX, int maxZ, int maxUneven) {
            int[][] samples = {{minX, minZ}, {maxX, minZ}, {minX, maxZ}, {maxX, maxZ}, {(minX + maxX) / 2, (minZ + maxZ) / 2}};
            int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE, sum = 0;
            for (int[] s : samples) {
                if (wet(s[0], s[1])) return OptionalInt.empty();
                int h = surface(s[0], s[1]);
                lo = Math.min(lo, h);
                hi = Math.max(hi, h);
                sum += h;
            }
            if (hi - lo > maxUneven) return OptionalInt.empty();
            return OptionalInt.of(Math.round(sum / (float) samples.length));
        }

        OptionalInt level(Frame frame, int lx0, int lz0, int lx1, int lz1, int maxUneven) {
            int[] r = frame.rect(lx0, lz0, lx1, lz1);
            return level(r[0], r[1], r[2], r[3], maxUneven);
        }

        boolean river(int x, int z) {
            int y = context.chunkGenerator().getSeaLevel() - 1;
            return context.biomeResolver().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z))
                    .is(BiomeTags.IS_RIVER);
        }

        /** Spruce in cold forests, dark oak in dark ones, oak elsewhere (structures spec 3.6). */
        PlanPiece.Wood wood(int x, int y, int z) {
            Holder<Biome> biome = context.biomeResolver().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z));
            if (biome.is(Biomes.DARK_FOREST) || biome.is(Biomes.PALE_GARDEN)) return PlanPiece.Wood.DARK_OAK;
            if (biome.is(BiomeTags.IS_TAIGA) || biome.is(BiomeTags.SPAWNS_SNOW_FOXES)) {
                return PlanPiece.Wood.SPRUCE;
            }
            return PlanPiece.Wood.OAK;
        }
    }
}
