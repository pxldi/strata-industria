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
     * The prospector's camp: a ridge tent with a cot, a counter with the three rock samples (set out in the
     * wrong order, to be put right), a cold fire, a washing pool, and a trench cut ten blocks toward the vein
     * with stakes along it. The cache crate lies under a block of rubble at the bottom end.
     */
    public static final Plan PROSPECTOR_CAMP = add(Plan.of("prospector_camp", Kind.LEVELLED,
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
                    "",
                    "",
                    "",
                    "",
                    "",
                    "       RRRR",
                    "       RRRR",
                    "       RRRR",
                    "       RRRR",
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
                    "",
                    "",
                    "       RRRR",
                    "       RRRR",
                    "       RRRR",
                    "       R..R",
                    "       R..R",
                    "       R..R",
                    "       R.XR",
                    "       RRRR",
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
                    "       RRRR",
                    "       RRRR",
                    "       R..R",
                    "       R..R",
                    "       R..R",
                    "       R..R",
                    "       R..R",
                    "       R..R",
                    "       R.UR",
                    "       RRRR",
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
                    "       RRRR",
                    "       R..R",
                    "       R..R",
                    "       R..R",
                    "       R..R",
                    "       R..R",
                    "       R..R",
                    "       R..R",
                    "       R..R",
                    "       R..R",
                    "       RRRR",
            },
            new String[] {
                    ",,,,,,,",
                    ",,,,,,,",
                    ",,,,,,,",
                    ",,,,,,,",
                    ",,,,,,,",
                    ",,,,,,,",
                    ",,,,,,,",
                    ",,,,,,,",
                    ",,,,,,,",
                    ",,,,,,,",
                    ",,,,,,,,..,",
                    ",,,,,,,,..,,",
                    ",,,,,,,,..,,",
                    ",,,,,,,,..,,",
                    "----,,,,..,,",
                    "-))-  ,,..,,",
                    "-))-  ,,..,,",
                    "-))-  ,,..,,",
                    "----  ,,..,,",
                    "       ,..,",
                    "        RR",
            },
            new String[] {
                    "",
                    "CCCCCCC",
                    "CE_9_BC",
                    "Ce___.C",
                    "C.___.C",
                    "C.___.C",
                    "C.___.C",
                    "CCz.zCC",
                    "",
                    " ppp",
                    "     k",
                    " |    g    d",
                    " | f dd    gd",
                    "     kg    d",
                    "      d    g",
                    "     dg    $d",
                    "   * kd    g",
                    "      g    d",
                    "     dd    gd",
                    "    nk",
                    "            n",
                    "      n   n",
            },
            new String[] {
                    "",
                    ".7CCC8.",
                    ".7...8.",
                    ".7...8.",
                    ".7...8.",
                    ".7...8.",
                    ".7...8.",
                    ".7z.z8.",
                    "",
                    " 312",
                    "     +",
                    "",
                    "           d",
                    "     +d",
                    "",
                    "           g",
                    "     +",
                    "",
                    "",
                    "     +",
            },
            new String[] {
                    "",
                    "..7C8..",
                    "..7.8..",
                    "..7.8..",
                    "..7(8..",
                    "..7.8..",
                    "..7.8..",
                    "..7C8..",
                    "   (",
            },
            new String[] {
                    "   |",
                    "   |",
                    "   |",
                    "   |",
                    "   |",
                    "   |",
                    "   |",
                    "   |",
                    "   |",
            }), 4);

    /** Two stones and a pebble, set up to point the way to the vein. */
    public static final Plan CAIRN = add(Plan.of("cairn", Kind.TERRAIN,
            new String[] {" "}, new String[] {"#"}, new String[] {"#"}, new String[] {"1"}));

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
     * The smiths' works, burnt down: a roofless workshop with charred posts, fallen beams over a floor crate, a
     * cracked hearth, a quench trough, a roasting pad, a half-collapsed charcoal store, a grave, and the bloomery
     * stack with three bricks missing (the backtick cells). The stack's footprint matches a real bloomery, so a
     * player holding a controller sees the preview line up with the ruin. The three bricks are in the workshop crate.
     */
    public static final Plan BLOOMERY_WORKS = add(bloomeryWorks());

    private static Plan bloomeryWorks() {
        int w = 19, d = 15, h = 4;
        char[][][] g = new char[h][d][w];
        for (char[][] layer : g) for (char[] row : layer) java.util.Arrays.fill(row, ' ');

        // The workshop: x 1-7, z 1-9, door gap in the south wall.
        for (int x = 1; x <= 7; x++) for (int z = 1; z <= 9; z++) g[0][z][x] = ',';
        for (int x = 1; x <= 7; x++) {
            for (int z = 1; z <= 9; z++) {
                boolean edge = x == 1 || x == 7 || z == 1 || z == 9;
                if (edge) {
                    int k = (x * 7 + z * 13) % 5;
                    g[1][z][x] = k == 4 ? ' ' : k == 0 ? 'm' : '#';
                    if ((x + z) % 3 == 0 && k != 4) g[2][z][x] = '#';
                } else if ((x * 3 + z * 5) % 4 == 0) {
                    g[1][z][x] = ':';
                }
            }
        }
        g[1][9][4] = ' ';
        g[2][9][4] = ' ';
        g[1][9][3] = ' ';
        int[][] posts = {{1, 1, 3}, {7, 1, 2}, {1, 9, 3}, {7, 9, 1}};
        for (int[] post : posts) {
            for (int y = 1; y <= post[2]; y++) g[y][post[1]][post[0]] = '[';
        }
        // The hearth in the north-east corner.
        g[1][2][5] = 'K';
        g[1][2][6] = 'K';
        g[1][3][6] = 'K';
        g[2][2][6] = 'K';
        g[1][3][5] = 'S';
        g[1][2][3] = 'a';
        g[1][5][3] = 'A';
        g[1][7][2] = 'u';
        // The cache crate in the floor, and the beams that came down on it.
        g[0][6][5] = 'X';
        g[1][6][4] = '{';
        g[1][6][5] = '{';
        g[1][6][6] = '{';
        g[1][7][5] = '}';
        g[2][6][5] = '}';
        g[2][5][5] = '}';
        g[1][8][2] = '{';
        g[1][8][3] = '{';
        g[1][4][6] = '}';
        g[1][5][6] = '}';

        // The stack, stamped at (8, 4) with three bricks left out.
        String[][] stack = {
                {"", "", "    ,,,    ", "   ,,,,,   ", "   ,,,,,   ", "   ,,K,,   ", "    ,,,    "},
                {"", "  S     Z  ", "", "    KKK    ", "    KSK    ", "    # #    ", "", "  K     S  ", "      #    ", "   Z       "},
                {"", "", "", "     K     ", "    K.K    "},
                {"", "", "", "     K     "}};
        for (int y = 0; y < stack.length; y++) {
            for (int z = 0; z < stack[y].length; z++) {
                for (int x = 0; x < stack[y][z].length(); x++) {
                    char c = stack[y][z].charAt(x);
                    if (c != ' ') g[y][z + 4][x + 8] = c;
                }
            }
        }
        g[1][7][14] = '`';
        g[1][8][12] = '`';
        g[2][8][14] = '`';

        // The quench trough, and a bellows that gave up.
        g[1][8][9] = '#';
        g[1][9][9] = 'y';
        g[1][10][9] = '#';
        g[1][6][8] = 'v';
        g[1][6][9] = 'T';
        g[1][5][8] = 'x';

        // The roasting pad: four by four, rust-coloured, with slag lying about.
        for (int x = 1; x <= 4; x++) for (int z = 11; z <= 14; z++) g[0][z][x] = (x + z) % 3 == 0 ? '<' : '>';
        g[1][12][2] = 'S';
        g[1][13][3] = 'Z';
        g[1][11][1] = 'Z';
        g[1][14][4] = 'S';

        // The charcoal store, half fallen in.
        for (int x = 10; x <= 16; x++) for (int z = 0; z <= 3; z++) g[0][z][x] = ',';
        int[][] fence = {{10, 0}, {16, 0}, {10, 3}};
        for (int[] f : fence) {
            g[1][f[1]][f[0]] = 'k';
            g[2][f[1]][f[0]] = 'k';
        }
        g[1][3][16] = 'k';
        for (int x = 10; x <= 13; x++) for (int z = 0; z <= 3; z++) g[3][z][x] = 's';
        g[1][2][15] = 's';
        g[1][1][16] = 's';
        for (int[] pile : new int[][] {{11, 0}, {12, 0}, {13, 0}, {11, 1}, {12, 1}}) g[1][pile[1]][pile[0]] = 'l';

        // A grave at the edge of the clearing.
        for (int x = 16; x <= 18; x++) for (int z = 11; z <= 14; z++) g[0][z][x] = ',';
        g[1][12][17] = '#';
        g[2][12][17] = '#';
        g[1][13][17] = '/';

        String[][] layers = new String[h][d];
        for (int y = 0; y < h; y++) for (int z = 0; z < d; z++) layers[y][z] = new String(g[y][z]).stripTrailing();
        return Plan.of("ruined_bloomery/works", Kind.LEVELLED, layers);
    }

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
     * The panners' bank, water to the south: a stilt hut over a backwater cut into the bank, a porch with the
     * scale and a tin, a line of sluice boxes feeding the trough that runs into the river (column 5 of the
     * south edge), a rocker box, a drying rack and the gravel they never got through. The crate lies in the
     * bed under the hut. Three layers lie below ground.
     */
    public static final Plan PLACER_BANK = add(placerWorkings(), 3);

    private static Plan placerWorkings() {
        int w = 17, d = 15, base = 3, h = 11;
        char[][][] g = new char[h][d][w];
        for (char[][] layer : g) for (char[] row : layer) java.util.Arrays.fill(row, ' ');

        // The backwater: two deep, a gravel bed, the cache sunk in it under the hut.
        for (int x = 9; x <= 15; x++) {
            for (int z = 3; z <= 8; z++) {
                put(g, base, -2, x, z, 'g');
                put(g, base, -1, x, z, ')');
                put(g, base, 0, x, z, ')');
            }
        }
        put(g, base, -2, 12, 5, 'X');

        // Stilts, then the deck: a porch in the west two columns, the hut in the east five.
        for (int[] s : new int[][] {{9, 3}, {9, 7}, {11, 3}, {11, 7}, {15, 3}, {15, 7}}) {
            for (int y = -2; y <= 2; y++) put(g, base, y, s[0], s[1], 'o');
        }
        for (int x = 9; x <= 15; x++) for (int z = 3; z <= 7; z++) put(g, base, 3, x, z, 'p');
        for (int y = 1; y <= 3; y++) put(g, base, y, 8, 3, '5');

        // The hut: 5 x 5 outside, a door in the west wall, windows north, south and east.
        for (int x = 11; x <= 15; x++) {
            for (int z = 3; z <= 7; z++) {
                if (x > 11 && x < 15 && z > 3 && z < 7) continue;
                boolean corner = (x == 11 || x == 15) && (z == 3 || z == 7);
                for (int y = 4; y <= 5; y++) put(g, base, y, x, z, corner ? 'z' : 'p');
            }
        }
        put(g, base, 4, 11, 5, '.');
        put(g, base, 5, 11, 5, '.');
        for (int[] win : new int[][] {{13, 3}, {13, 7}, {15, 5}}) put(g, base, 5, win[0], win[1], 'w');
        put(g, base, 4, 12, 4, 'B');
        put(g, base, 4, 14, 4, 'E');
        put(g, base, 4, 14, 5, 'e');
        put(g, base, 5, 13, 5, '(');
        for (int x = 11; x <= 15; x++) {
            put(g, base, 6, x, 3, '^');
            for (int z = 4; z <= 6; z++) put(g, base, 6, x, z, 'p');
            put(g, base, 6, x, 7, 'v');
            put(g, base, 7, x, 4, '^');
            put(g, base, 7, x, 5, 'p');
            put(g, base, 7, x, 6, 'v');
        }

        // The porch: a rail, the rack with the rod, the scale on a table and the tin.
        for (int z = 4; z <= 7; z++) put(g, base, 4, 9, z, 'k');
        put(g, base, 4, 10, 3, 'k');
        put(g, base, 4, 10, 4, 'a');
        put(g, base, 4, 10, 6, 's');
        put(g, base, 5, 10, 6, '6');
        put(g, base, 4, 10, 7, 'j');

        // Sluice boxes on the bank, fed from a cauldron, running down to the trough at the south edge.
        put(g, base, 1, 5, 9, 'y');
        for (int z = 10; z <= 12; z++) put(g, base, 1, 5, z, '4');
        // The rocker box: a cradle of boards with a handle.
        put(g, base, 1, 2, 10, '^');
        put(g, base, 1, 2, 11, 'p');
        put(g, base, 1, 2, 12, 'v');
        put(g, base, 2, 2, 11, 'k');
        // A drying rack with a sheet over the rail.
        for (int y = 1; y <= 2; y++) {
            put(g, base, y, 8, 12, 'k');
            put(g, base, y, 11, 12, 'k');
        }
        put(g, base, 2, 9, 12, 'k');
        put(g, base, 2, 10, 12, 'k');
        put(g, base, 3, 9, 12, '_');
        put(g, base, 3, 10, 12, '_');
        // Gravel bars, half in the water.
        for (int[] bar : new int[][] {{9, 8}, {10, 8}, {14, 8}, {15, 8}}) put(g, base, 0, bar[0], bar[1], 'G');
        for (int[] heap : new int[][] {{12, 10}, {13, 10}, {12, 11}, {15, 9}, {16, 8}}) put(g, base, 1, heap[0], heap[1], 'G');
        put(g, base, 2, 12, 10, 'G');
        // Trampled ground from the ladder to the boxes.
        for (int z = 3; z <= 9; z++) put(g, base, 0, 8, z, ',');
        for (int x = 5; x <= 8; x++) put(g, base, 0, x, 9, ',');
        for (int z = 10; z <= 13; z++) put(g, base, 0, 4, z, ',');

        String[][] layers = new String[h][d];
        for (int y = 0; y < h; y++) for (int z = 0; z < d; z++) layers[y][z] = new String(g[y][z]).stripTrailing();
        return Plan.of("placer_workings/bank", Kind.LEVELLED, layers);
    }

    /** Sets the block at height {@code y} above the ground layer; {@code base} layers lie below it. */
    private static void put(char[][][] g, int base, int y, int x, int z, char c) {
        g[base + y][z][x] = c;
    }

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
