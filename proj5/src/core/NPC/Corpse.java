package core.NPC;

import tileengine.TETile;

/**
 * Simple marker to render NPC death remnants separately from active actors.
 */
public class Corpse {
    private final int x;
    private final int y;
    private final TETile tile;

    public Corpse(int x, int y, TETile tile) {
        this.x = x;
        this.y = y;
        this.tile = tile;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public TETile tile() {
        return tile;
    }
}