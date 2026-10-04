package dev.strataindustria.power;

/**
 * A block that shows how hard its network is working (uniqueness 7.1). The network tells it whenever its load level
 * changes: 0 calm, 1 busy, 2 heavy, 3 at its limit.
 */
public interface LoadListener {
    void gridLoad(int level);
}
