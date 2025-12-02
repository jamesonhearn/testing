package core.animation;

import tileengine.TETile;

import java.util.Objects;
import java.util.Random;

/**
 * Simple animation helper that advances through a set of frames at a fixed tick interval.
 */
public class AnimationCycle {
    private final TETile[] frames;
    private final int ticksPerFrame;
    private final boolean loop;
    private int frameIndex;
    private int tickCounter;
    private boolean completed;

    public AnimationCycle(TETile[] frames, int ticksPerFrame) {
        this(frames, ticksPerFrame, true);
    }

    public AnimationCycle(TETile[] frames, int ticksPerFrame, boolean loop) {
        if (ticksPerFrame <= 0) {
            throw new IllegalArgumentException("ticksPerFrame must be positive");
        }
        Objects.requireNonNull(frames, "frames cannot be null");
        if (frames.length == 0) {
            throw new IllegalArgumentException("frames must not be empty");
        }
        this.frames = frames;
        this.ticksPerFrame = ticksPerFrame;
        this.loop = loop;
        this.frameIndex = 0;
        this.tickCounter = 0;
        this.completed = !loop && frames.length == 1;
    }

    public TETile currentFrame() {
        return frames[frameIndex];
    }

    public void advance() {
        tickCounter += 1;
        if (tickCounter < ticksPerFrame) {
            return;
        }
        tickCounter = 0;
        if (frameIndex < frames.length - 1) {
            frameIndex += 1;
            if (!loop && frameIndex == frames.length - 1) {
                completed = true;
            }
            return;
        }
        if (loop) {
            frameIndex = 0;
            return;
        }
        completed = true;
    }

    public void randomizeFrame(Random rng) {
        if (rng == null) {
            return;
        }
        frameIndex = rng.nextInt(frames.length);
        tickCounter = rng.nextInt(ticksPerFrame);
        completed = !loop && frameIndex == frames.length - 1;
    }

    public void setFrameIndex(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("frame index must be non-negative");
        }
        frameIndex = index % frames.length;
        tickCounter = 0;
        completed = !loop && frameIndex == frames.length - 1;
    }

    public int frameIndex() {
        return frameIndex;
    }

    public int frameCount() {
        return frames.length;
    }

    public boolean isComplete() {
        return completed;
    }

    public void restart() {
        frameIndex = 0;
        tickCounter = 0;
        completed = !loop && frames.length == 1;
    }
}
