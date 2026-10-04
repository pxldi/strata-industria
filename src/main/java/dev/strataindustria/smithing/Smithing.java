package dev.strataindustria.smithing;

import dev.strataindustria.Config;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.crafting.Recipe;

/** Smithing numbers (spec 9.2): the bar, per-world targets, the fewest hits a recipe needs, and quality. */
public final class Smithing {
    public static final int MAX_POSITION = 150;
    public static final int MIN_TARGET = 40;
    public static final int MAX_TARGET = 110;
    /** Craft quality of a quick-smithed piece: a steady run with a few spare blows, not the best and not the worst. */
    public static final int QUICK_CRAFT = 2;

    private static final Map<String, Integer> MIN_HITS = new ConcurrentHashMap<>();

    private static final Map<String, List<Integer>> SOLUTIONS = new ConcurrentHashMap<>();

    private Smithing() {}

    /** The recipe's target in this world: a hash of the seed and recipe id, or its default target. */
    public static int target(ServerLevel level, ResourceKey<Recipe<?>> id, AnvilRecipe recipe) {
        if (!Config.SMITHING_RANDOM_TARGETS.getAsBoolean()) return recipe.defaultTarget();
        long h = level.getSeed() * 31L + id.identifier().toString().hashCode();
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        return MIN_TARGET + (int) Math.floorMod(h, (long) (MAX_TARGET - MIN_TARGET + 1));
    }

    /** Whether a workpiece at {@code position} with these recent hits is finished. */
    public static boolean done(int position, int target, List<Rule> rules, HitType last, HitType second, HitType third) {
        return position == target && Rule.all(rules, last, second, third);
    }

    /**
     * The fewest hits that reach the target with every rule met, by breadth-first search over the
     * position and the last three hits. Cached per target and rule set.
     */
    public static int minHits(int target, List<Rule> rules) {
        StringBuilder key = new StringBuilder().append(target);
        for (Rule rule : rules) key.append(',').append(rule.encode());
        return MIN_HITS.computeIfAbsent(key.toString(), k -> search(target, rules));
    }

    private static int search(int target, List<Rule> rules) {
        int types = HitType.VALUES.length + 1; // index 0 means "no hit yet"
        int states = (MAX_POSITION + 1) * types * types * types;
        int[] dist = new int[states];
        java.util.Arrays.fill(dist, -1);
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        int start = encode(0, 0, 0, 0, types);
        dist[start] = 0;
        queue.add(start);
        while (!queue.isEmpty()) {
            int state = queue.poll();
            int third = state % types, rest = state / types;
            int second = rest % types;
            rest /= types;
            int last = rest % types;
            int position = rest / types;
            if (done(position, target, rules, type(last), type(second), type(third))) return dist[state];
            for (HitType hit : HitType.VALUES) {
                int next = position + hit.delta();
                if (next < 0 || next > MAX_POSITION) continue;
                int code = encode(next, hit.ordinal() + 1, last, second, types);
                if (dist[code] >= 0) continue;
                dist[code] = dist[state] + 1;
                queue.add(code);
            }
        }
        return 0;
    }

    /** The shortest hit sequence (as {@link HitType} ordinals) that finishes the target with every rule met; cached like {@link #minHits}. */
    public static List<Integer> solve(int target, List<Rule> rules) {
        StringBuilder key = new StringBuilder().append(target);
        for (Rule rule : rules) key.append(',').append(rule.encode());
        return SOLUTIONS.computeIfAbsent(key.toString(), k -> solveSearch(target, rules));
    }

    private static List<Integer> solveSearch(int target, List<Rule> rules) {
        int types = HitType.VALUES.length + 1;
        int states = (MAX_POSITION + 1) * types * types * types;
        int[] from = new int[states];
        int[] via = new int[states];
        java.util.Arrays.fill(from, -2);
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        int start = encode(0, 0, 0, 0, types);
        from[start] = -1;
        queue.add(start);
        while (!queue.isEmpty()) {
            int state = queue.poll();
            int third = state % types, rest = state / types;
            int second = rest % types;
            rest /= types;
            int last = rest % types;
            int position = rest / types;
            if (done(position, target, rules, type(last), type(second), type(third))) {
                java.util.ArrayList<Integer> hits = new java.util.ArrayList<>();
                for (int at = state; from[at] >= 0; at = from[at]) hits.add(0, via[at]);
                return List.copyOf(hits);
            }
            for (HitType hit : HitType.VALUES) {
                int next = position + hit.delta();
                if (next < 0 || next > MAX_POSITION) continue;
                int code = encode(next, hit.ordinal() + 1, last, second, types);
                if (from[code] != -2) continue;
                from[code] = state;
                via[code] = hit.ordinal();
                queue.add(code);
            }
        }
        return List.of();
    }

    private static int encode(int position, int last, int second, int third, int types) {
        return ((position * types + last) * types + second) * types + third;
    }

    private static HitType type(int index) {
        return index == 0 ? null : HitType.VALUES[index - 1];
    }

    /** Spec 9.2: the craft part of quality from the hits used beyond the minimum. */
    public static int craftQuality(int hits, int minimum) {
        int extra = Math.max(0, hits - minimum);
        if (extra == 0) return 10;
        if (extra <= 2) return 6;
        if (extra <= 5) return 2;
        if (extra <= 9) return -2;
        return -6;
    }

    public static float barFraction(int position) {
        return Mth.clamp(position / (float) MAX_POSITION, 0.0f, 1.0f);
    }
}
