package dev.strataindustria.structure;

import dev.strataindustria.structure.Plan.Kind;
import java.util.HashMap;
import java.util.Map;

/**
 * Every building plan of the world structures (structures spec 6). Plans face south: the open
 * side of a tent or shed is at the bottom of the drawing. A portal's mouth is at the top and the tunnel
 * runs south into the hill. The pieces turn them to face the right way.
 */
public final class Plans {
    private static final Map<String, Plan> PLANS = new HashMap<>();

    /** Plans whose ground layer is not their first layer: how many layers lie below the ground. */
    private static final Map<String, Integer> BASE = new HashMap<>();

    // ---------------------------------------------------------------- 6.1 charcoal burners' clearing

    /**
     * The clearing: a burnt pit still smouldering in the middle, an unburnt mound of eight log piles with its
     * turf stacked beside it, a cordwood wall at the back, stumps around the edge, a sawhorse and chopping
     * block, a woodpile, and the burners' hut with a hay bed. The crate set into the floor under the bed is
     * the cache.
     */
    public static final Plan CLEARING = add(Plan.of("charcoal_burners_clearing", Kind.LEVELLED,
            new String[] {
                    "",
                    "",
                    "     ,,,",
                    "    ,,?,,",
                    "   ,,???,,   ,,,,,,",
                    "   ,?????,   ,,,,,,",
                    "   ,,???,,   ,,,,,,",
                    "    ,,?,,    ,,,,,,",
                    "     ,,,     ,,,,,,",
                    " ,,,,,,,",
                    " ,,,,,,,",
                    " ,,,,,,,",
                    " ,,,,,,,",
                    " ,,,,,,,",
                    " ,,,,,,,",
                    " ,X,,,,,",
                    " ,,,,,,,",
            },
            new String[] {
                    "",
                    "   k========k",
                    "                  z",
                    " z !    x z",
                    "              dddd",
                    "      @      dLLLL 00",
                    "   x     !   dLLLL 00",
                    "z             dddd",
                    "    !",
                    " zDDDDDz",
                    " D    *D z  z",
                    " D     D           z",
                    " D  f  D  k k x",
                    " D     D       o WW",
                    " D~   uD   x   a WW",
                    " D~~   D         z",
                    " zDz zDz   z",
            },
            new String[] {
                    "",
                    "   k========k",
                    "                  &",
                    " &        &",
                    "",
                    "              dddd 00",
                    "              dddd 0",
                    "&",
                    "",
                    " zDDkDDz",
                    " D     D &  &",
                    " D    sD           &",
                    " k    sk  ===",
                    " D     D",
                    " k     D",
                    " D     D         &",
                    " zDz zDz   &",
            },
            new String[] {
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    " zDDDDDz",
                    " D     D",
                    " D    *D",
                    " D    *D",
                    " D     D",
                    " D     D",
                    " D     D",
                    " zDzpzDz",
            },
            new String[] {
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "~~~~~~~~~",
                    "~~~~~~~~~",
                    "~~~~~~~~~",
                    "~~~~~~~~~",
                    "~~~~ ~~~~",
                    "~~~~~~~~~",
                    "~~~~~~~~~",
                    "~~~~~~~~~",
                    "~~~~~~~~~",
                    "~~~~~~~~~",
            },
            new String[] {
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    " ~~~~~~~",
                    " ~~~~~~~",
                    " ~~~~~~~",
                    " ~~~ ~~~",
                    " ~~~~~~~",
                    " ~~~~~~~",
                    " ~~~~~~~",
                    " ~~~~~~~",
            },
            new String[] {
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "   ~~~",
                    "   ~ ~",
                    "   ~~~",
                    "   ~~~",
            }));

    // ---------------------------------------------------------------- 6.2 prospector's camp

    /**
     * A one-person tent, a cold fire, a seat, and the line of three pebbles on a log: the rock at the
     * surface, the rock below it, and the hard rock at the bottom.
     */
    public static final Plan PROSPECTOR_CAMP = add(Plan.of("prospector_camp", Kind.LEVELLED,
            new String[] {
                    "         ",
                    " ,,,,,,  ",
                    " ,,,,,,  ",
                    " ,,,,,,  ",
                    "   ,,,   ",
                    "   ,,,   ",
                    "   ,,,   ",
                    "         ",
                    "         ",
            },
            new String[] {
                    "",
                    " PCCCCP  ",
                    " C____C  ",
                    "  __B_   ",
                    "         ",
                    " |  f  | ",
                    " |     | ",
                    " |  x    ",
                    "        r",
            },
            new String[] {
                    "",
                    " PCCCCP  ",
                    " C    C  ",
                    " CCCCCC  ",
                    "",
                    " 1       ",
                    " 2       ",
                    " 3       ",
            },
            new String[] {
                    "",
                    " CCCCCC  ",
                    " CCCCCC  ",
            }));

    /** Two stones and a pebble, set up to point the way to the vein. */
    public static final Plan CAIRN = add(Plan.of("cairn", Kind.TERRAIN,
            new String[] {" "}, new String[] {"#"}, new String[] {"#"}, new String[] {"1"}));

    /** A test pit three deep with a ladder, its spoil heaped on one side. */
    public static final Plan TRIAL_PIT = add(Plan.of("trial_pit", Kind.LEVELLED,
            new String[] {
                    "     ",
                    " RRR ",
                    " RRR ",
                    " RRR ",
            },
            new String[] {
                    "     ",
                    " .H. ",
                    " ... ",
                    " ... ",
            },
            new String[] {
                    "     ",
                    " .H. ",
                    " ... ",
                    " ... ",
            },
            new String[] {
                    "     ",
                    " .H. ",
                    " ... ",
                    " ... ",
                    "     ",
            },
            new String[] {
                    "  gg ",
                    "    g",
                    "    $",
                    "    g",
                    " gg  ",
            }), 3);

    // ---------------------------------------------------------------- 6.3 mining camp

    /** The yard: trampled ground round a fire with log seats, and a stack of log piles. */
    public static final Plan YARD = add(Plan.of("mining_camp/yard", Kind.LEVELLED,
            new String[] {
                    "      ,      ",
                    "    ,,,,,    ",
                    "   ,,,,,,,   ",
                    "  ,,,,,,,,,  ",
                    " ,,,,,,,,,,, ",
                    " ,,,,,,,,,,, ",
                    ",,,,,,,,,,,,,",
                    " ,,,,,,,,,,, ",
                    " ,,,,,,,,,,, ",
                    "  ,,,,,,,,,  ",
                    "   ,,,,,,,   ",
                    "    ,,,,,    ",
                    "      ,      ",
            },
            new String[] {
                    "",
                    "",
                    "",
                    "          r  ",
                    "     ===     ",
                    "    |   |    ",
                    "    | f |    ",
                    "    |   |    ",
                    "   WW    x   ",
                    "   WW        ",
                    "  q          ",
            },
            new String[] {
                    "", "", "", "", "", "", "", "",
                    "   WW        ",
                    "   WW        ",
            }));

    /** A small A-frame tent: one bed and a barrel. */
    public static final Plan TENT_SMALL = add(Plan.of("mining_camp/tent_small", Kind.LEVELLED,
            new String[] {
                    "     ",
                    " ,,, ",
                    " ,,, ",
                    " ,,, ",
                    " ,,, ",
                    "  ,  ",
                    "  ,  ",
            },
            new String[] {
                    "CCPCC",
                    "CE_BC",
                    "Ce__C",
                    "C___C",
                    "C___C",
                    "C   C",
            },
            new String[] {
                    " CPC ",
                    " C C ",
                    " C C ",
                    " C C ",
                    " C C ",
                    " C C ",
            },
            new String[] {
                    "  |  ", "  |  ", "  |  ", "  |  ", "  |  ", "  |  ",
            },
            new String[] {
                    "  _  ", "  _  ", "  _  ", "  _  ", "  _  ", "  _  ",
            }));

    /** A large A-frame tent: two beds, a barrel and a rug at the door. */
    public static final Plan TENT_LARGE = add(Plan.of("mining_camp/tent_large", Kind.LEVELLED,
            new String[] {
                    "       ",
                    " ,,,,, ",
                    " ,,,,, ",
                    " ,,,,, ",
                    " ,,,,, ",
                    " ,,,,, ",
                    " ,,,,, ",
                    "  ,,,  ",
            },
            new String[] {
                    "CCCPCCC",
                    "CE___EC",
                    "Ce___eC",
                    "C_____C",
                    "CB____C",
                    "C_____C",
                    "C  P  C",
                    "  ___  ",
            },
            new String[] {
                    " CCPCC ",
                    " C   C ",
                    " C   C ",
                    " C   C ",
                    " C   C ",
                    " C   C ",
                    " C P C ",
            },
            new String[] {
                    "  CPC  ",
                    "  C C  ",
                    "  C C  ",
                    "  C C  ",
                    "  C C  ",
                    "  C C  ",
                    "  CPC  ",
            },
            new String[] {
                    "   |   ", "   |   ", "   |   ", "   |   ", "   |   ", "   |   ", "   |   ",
            },
            new String[] {
                    "   _   ", "   _   ", "   _   ", "   _   ", "   _   ", "   _   ", "   _   ",
            }));

    /**
     * The forge shed: a forge with a crucible on it, a stone anvil of the bottom rock, and the smiths'
     * barrel, under a canvas roof on pit props.
     */
    public static final Plan FORGE_SHED = add(Plan.of("mining_camp/forge_shed", Kind.LEVELLED,
            new String[] {
                    " ,,,,, ",
                    ",,,,,,,",
                    ",,,,,,,",
                    ",,,,,,,",
                    " ,,,,, ",
                    "   ,   ",
            },
            new String[] {
                    "PCCCCCP",
                    "#F#Ay u#",
                    "#     #",
                    "P     P",
            },
            new String[] {
                    "PCCCCCP",
                    "Cc  a C",
                    "C     C",
                    "P     P",
            },
            new String[] {
                    "PCCCCCP",
                    "C     C",
                    "C     C",
                    "P     P",
            },
            new String[] {
                    "=======",
                    "CCCCCCC",
                    "CCCCCCC",
                    "=======",
            }));

    /** The sorting floor: a counter where the ore was picked over, the ore barrel and the waste. */
    public static final Plan ORE_SORTING = add(Plan.of("mining_camp/ore_sorting", Kind.LEVELLED,
            new String[] {
                    "   ,   ",
                    " ,,,,, ",
                    " ,,,,, ",
                    ",,,,,,,",
                    " ,,,,, ",
                    " ,,,,, ",
                    "   ,   ",
            },
            new String[] {
                    "",
                    "",
                    "  ksk  ",
                    "     N ",
                    "  gg U ",
            },
            new String[] {
                    "",
                    "",
                    "   n   ",
            }));

    /** The heap of waste rock from the adit, with two finds for a brush. */
    public static final Plan SPOIL_HEAP = add(Plan.of("mining_camp/spoil_heap", Kind.TERRAIN,
            new String[] {
                    "       ",
            },
            new String[] {
                    "  ggg  ",
                    " gg#gg ",
                    "gg#gg#g",
                    "g#gggg ",
                    "ggg#gg ",
                    " ggggg ",
                    "  g g  ",
            },
            new String[] {
                    "",
                    " r ggg ",
                    "  gg$g ",
                    " g#gg  ",
                    " gg$gr ",
                    "  ggg  ",
            },
            new String[] {
                    "",
                    "",
                    "   g   ",
                    "  gg#  ",
            }));

    /** A timbered adit mouth cut into a hillside; the tunnel continues south from the last row. */
    public static final Plan ADIT_PORTAL = add(Plan.of("mining_camp/adit_portal", Kind.EMBEDDED,
            new String[] {
                    "  ,,,  ",
                    "  RRR  ",
                    "  RRR  ",
                    "  RRR  ",
            },
            portalWalls(), portalWalls(), portalWalls(),
            new String[] {
                    "#=====#",
                    "#RRRRR#",
                    " RRRRR ",
                    " ===== ",
            }));

    /**
     * The headframe over a shaft, for camps on flat ground: four stripped posts nine high with fence braces,
     * a pulley wheel of trapdoors on an axle, a windlass at its foot and a chain down the shaft. The shaft
     * goes down below the middle.
     */
    public static final Plan SHAFT_HEAD = add(Plan.of("mining_camp/shaft_head", Kind.LEVELLED,
            new String[] {
                    " ### ",
                    "#.H.#",
                    "#...#",
                    "#...#",
                    " ### ",
            },
            new String[] {
                    "",
                    "z   z",
                    "Q  h ",
                    "z   z",
            },
            new String[] {
                    "",
                    "z   z",
                    "   h ",
                    "z   z",
            },
            new String[] {
                    "",
                    "zkkkz",
                    "k  h ",
                    "zkkkz",
            },
            new String[] {
                    "",
                    "z   z",
                    "   h ",
                    "z   z",
            },
            new String[] {
                    "",
                    "z   z",
                    "   h ",
                    "z   z",
            },
            new String[] {
                    "",
                    "zkkkz",
                    "k  h ",
                    "zkkkz",
            },
            new String[] {
                    "",
                    "z   z",
                    "   h ",
                    "z   z",
            },
            new String[] {
                    "",
                    "z t z",
                    "=====",
                    "z t z",
            },
            new String[] {
                    "",
                    "|   |",
                    "| t |",
                    "|   |",
            }));

    /** The cabin and, built onto its east end, the foreman's office whose door is walled up with crates. */
    public static final Plan BUNKHOUSE = add(Plan.of("mining_camp/bunkhouse", Kind.LEVELLED,
            new String[] {
                    "#########",
                    "#ppppppp#####",
                    "#ppppppp#ppp#",
                    "#ppppppp#pXp#",
                    "#ppppppp#ppp#",
                    "#ppppppp#####",
                    "####p####",
            },
            new String[] {
                    "zpppppppz",
                    "pEE...EEzpppz",
                    "pee...eep.V%p",
                    "pu.sss..j.T.p",
                    "pu......p...p",
                    "pO......zpppz",
                    "zppp.pppz",
            },
            new String[] {
                    "zppwppwpz",
                    "p       zpppz",
                    "w       p   p",
                    "p   i   j   w",
                    "w       p   p",
                    "p#      zpppz",
                    "zpwp pwpz",
            },
            new String[] {
                    "zpppppppz",
                    "p       zpppz",
                    "p       p   p",
                    "p       p I p",
                    "p       p   p",
                    "p#      zpppz",
                    "zpppppppz",
            },
            new String[] {
                    "vvvvvvvvv",
                    "p       ppppp",
                    "p       ppppp",
                    "p       ppppp",
                    "p       ppppp",
                    "p#      ppppp",
                    "^^^^^^^^^",
            },
            new String[] {
                    "",
                    "vvvvvvvvv",
                    "p       p",
                    "p       p",
                    "p       p",
                    "^#^^^^^^^",
            },
            new String[] {
                    "",
                    "",
                    "vvvvvvvvv",
                    "p       p",
                    "^^^^^^^^^",
                    " #",
            },
            new String[] {
                    "",
                    "",
                    "",
                    "ppppppppp",
                    "",
                    " #",
            }));

    /** The rail from the adit across the sorting floor: vanilla rail, some of it gone, rubble on the rest. */
    public static final Plan RAIL = add(Plan.of("mining_camp/rail", Kind.TERRAIN,
            new String[] {
                    ",,,,,",
            },
            new String[] {
                    "JJJUJ",
            }));

    /** Wooden track of an older working: sleepers and plank rails, rotted gaps, a tub left at the end. */
    public static final Plan TRAMWAY = add(Plan.of("mining_camp/tramway", Kind.TERRAIN,
            new String[] {
                    ",,,,,,,,,,,,,,",
                    ",,,,,,,,,,,,,,",
                    ",,,,,,,,,,,,,,",
            },
            new String[] {
                    "sss|sss|sss|ss",
                    "   |   |   |  N",
                    "sss|sss|sss|ss",
            }));

    // ---------------------------------------------------------------- 6.4 collapsed adit

    /** An older adit mouth, fallen in: a prop gone, the lintel on the rubble, moss on the stones. */
    public static final Plan COLLAPSED_PORTAL = add(Plan.of("collapsed_adit/portal", Kind.EMBEDDED,
            new String[] {
                    "  ,,,  ",
                    "  RRR  ",
                    "  RRR  ",
                    "  RRR  ",
            },
            new String[] {
                    "mgg$gPm",
                    "mRg#gRm",
                    " R...R ",
                    " P...P ",
            },
            new String[] {
                    "mgggrPm",
                    "mRgggRm",
                    " R...R ",
                    " P...P ",
            },
            new String[] {
                    "m g..Pm",
                    "mR.g.Rm",
                    " R...R ",
                    " P...P ",
            },
            new String[] {
                    "m ====m",
                    "mRRRRRm",
                    " RRRRR ",
                    " ===== ",
            }));

    // ---------------------------------------------------------------- 6.5 ruined bloomery

    /**
     * The stump of a bloomery: the base ring around its chamber, broken where the door stood, and two
     * courses of the chimney above it, with fallen bricks and slag around. The footprint matches a real
     * bloomery, so a player holding a controller sees the preview line up with the ruin.
     */
    public static final Plan BLOOMERY_STUMP = add(Plan.of("ruined_bloomery/stump", Kind.LEVELLED,
            new String[] {
                    "",
                    "",
                    "    ,,,    ",
                    "   ,,,,,   ",
                    "   ,,,,,   ",
                    "   ,,K,,   ",
                    "    ,,,    ",
                    "",
                    "     b     ",
            },
            new String[] {
                    "",
                    "  S     Z  ",
                    "",
                    "    KKK    ",
                    "    KSK    ",
                    "    # #    ",
                    "",
                    "  K     S  ",
                    "      #    ",
                    "   Z       ",
                    "",
            },
            new String[] {
                    "",
                    "",
                    "",
                    "     K     ",
                    "    Y.K    ",
            },
            new String[] {
                    "",
                    "",
                    "",
                    "     K     ",
            }));

    /** Where the bloomery's charcoal was burned: a scorched ring, and a row of piles nobody came back for. */
    public static final Plan CHARCOAL_SCAR = add(Plan.of("ruined_bloomery/charcoal_scar", Kind.TERRAIN,
            new String[] {
                    " ,,,,, ",
                    ",,ddd,,",
                    ",d,,,d,",
                    ",d,,,d,",
                    ",d,,,d,",
                    ",,ddd,,",
                    " ,,,,, ",
            },
            new String[] {
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    " llll  ",
            }));

    // ---------------------------------------------------------------- 6.6 placer workings

    /**
     * The panners' bank, water to the south: a canvas lean-to with their barrel, and the heap of river
     * gravel they never got through. The sluice runs from column 5 of the south edge into the river.
     */
    public static final Plan PLACER_BANK = add(Plan.of("placer_workings/bank", Kind.LEVELLED,
            new String[] {
                    "",
                    " ,,,,,      ",
                    " ,,,,, ,,,, ",
                    "  ,,,,,,,,, ",
                    "   ,,,,,,,, ",
                    "    ,,,,    ",
            },
            new String[] {
                    "",
                    " PCCCP      ",
                    " C_B_C  GG  ",
                    "  ___  GGGG ",
                    "",
            },
            new String[] {
                    "",
                    " PCCCP      ",
                    " C   C      ",
                    " CCCCC  GG  ",
            },
            new String[] {
                    "",
                    " CCCCC      ",
                    " CCCCC      ",
            }));

    private static String[] portalWalls() {
        return new String[] {
                "#P...P#",
                "#R...R#",
                " R...R ",
                " P...P ",
        };
    }

    private Plans() {}

    private static Plan add(Plan plan) {
        PLANS.put(plan.id(), plan);
        return plan;
    }

    private static Plan add(Plan plan, int base) {
        BASE.put(plan.id(), base);
        return add(plan);
    }

    /** A three-wide trampled path {@code length} blocks long, running south. */
    public static Plan path(int length) {
        String[] rows = new String[length];
        java.util.Arrays.fill(rows, ",,,");
        return Plan.of("path/" + length, Kind.TERRAIN, rows);
    }

    /** Every drawn plan, for tests that go through all of them. */
    public static java.util.Collection<Plan> all() {
        return java.util.Collections.unmodifiableCollection(PLANS.values());
    }

    public static Plan get(String id) {
        if (id.startsWith("path/")) return path(Integer.parseInt(id.substring(5)));
        Plan plan = PLANS.get(id);
        if (plan == null) throw new IllegalArgumentException("Unknown plan " + id);
        return plan;
    }

    /** How many layers of the plan lie below the ground. */
    public static int base(Plan plan) {
        return BASE.getOrDefault(plan.id(), 0);
    }
}
