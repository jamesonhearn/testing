package core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public enum Direction {
    UP(0, 1), DOWN(0, -1), LEFT(-1, 0), RIGHT(1, 0);

    private final int dx;
    private final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public int getDx() {
        return this.dx;
    }
    public int getDy() {
        return this.dy;
    }
    public static Direction random(Random rng) {
        Direction[] values = values();
        return values[rng.nextInt(values.length)];
    }

    public static List<Direction> shuffled(Random rng) {
        List<Direction> dirs = new ArrayList<>(Arrays.asList(values()));
        Collections.shuffle(dirs, rng);
        return dirs;
    }
}
