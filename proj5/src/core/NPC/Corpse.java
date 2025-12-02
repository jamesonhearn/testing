package core.NPC;

import tileengine.TETile;
import core.animation.AnimationCycle;

/**
 * Simple marker to render NPC death remnants separately from active actors.
 */
public class Corpse {
    private final int x;
    private final int y;
    private TETile tile;
    private AnimationCycle animation;

    public Corpse(int x, int y, TETile tile) {
        this.x = x;
        this.y = y;
        this.tile = tile;
        this.animation = null;
    }
    public Corpse(int x, int y, AnimationCycle animation) {
        this.x = x;
        this.y = y;
        this.animation = animation;
        this.tile = animation.currentFrame();
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
    /**
     * Advance the death animation one frame, ends on final frame
     */
    public boolean tick() {
        if (animation == null) {
            return false;
        }
        animation.advance();
        tile = animation.currentFrame();
        if (animation.isComplete()) {
            animation = null;
            return false;
        }
        return true;
    }

    public boolean isAnimating() {
        return animation != null && !animation.isComplete();
    }
}
