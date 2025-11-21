package core.NPC;

import core.NPC.NpcManager;

import tileengine.TETile;
import tileengine.Tileset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Minimal NPC representation with random-walk behavior and sprite cycling.
 * Instances are updated by {@link NpcManager} and rendered directly by the engine
 * between the base and front tile layers.
 */
public class Npc {
    private final Random rng;
    private int x;
    private int y;
    private Direction facing = Direction.DOWN;
    private int animFrame = 0;
    private int animTick = 0;
    private int moveTick = 0;


    // Tunables for movement and animation pacing.
    private static final int STEP_INTERVAL = 24;    // ticks between movement attempts
    private static final int ANIM_INTERVAL = 12;    // ticks between animation frames

    private double drawX;
    private double drawY;

    public void setDrawX(double x) { this.drawX = x; }
    public void setDrawY(double y) { this.drawY = y; }

    public void updateSmooth(double speed) {
        drawX += (x - drawX) * speed;
        drawY += (y - drawY) * speed;
    }


    public Npc(int x, int y, Random rng) {
        this.x = x;
        this.y = y;
        this.rng = rng;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    /**
     * Advance one tick of NPC simulation: possibly move and advance animation.
     */
    public void tick(TETile[][] world, Set<Point> occupied) {
        moveTick += 1;
        animTick += 1;

        if (animTick >= ANIM_INTERVAL) {
            animFrame = (animFrame + 1) % Tileset.AVATAR_UP_FRAMES.length;
            animTick = 0;
        }

        if (moveTick < STEP_INTERVAL) {
            return;
        }
        moveTick = 0;

        Direction preferred = rng.nextDouble() < 0.8 ? facing : Direction.random(rng);
        List<Direction> attempts = Direction.shuffled(rng);
        // Bias toward keeping current heading when possible.
        attempts.remove(preferred);
        attempts.add(0, preferred);

        for (Direction dir : attempts) {
            int nx = x + dir.dx;
            int ny = y + dir.dy;
            if (!canOccupy(world, occupied, nx, ny)) {
                continue;
            }
            facing = dir;
            x = nx;
            y = ny;
            return;
        }
    }

    private boolean canOccupy(TETile[][] world, Set<Point> occupied, int nx, int ny) {
        if (nx < 0 || ny < 0 || nx >= world.length || ny >= world[0].length) {
            return false;
        }
        if (!world[nx][ny].equals(Tileset.FLOOR)) {
            return false;
        }
        return !occupied.contains(new Point(nx, ny));
    }

    /**
     * Current animation frame tile based on facing direction.
     */
    public TETile currentTile() {
        return switch (facing) {
            case UP -> Tileset.SLIME_UP_FRAMES[animFrame];
            case DOWN -> Tileset.SLIME_DOWN_FRAMES[animFrame];
            case LEFT -> Tileset.SLIME_LEFT_FRAMES[animFrame];
            case RIGHT -> Tileset.SLIME_RIGHT_FRAMES[animFrame];
        };
    }

    public record Point(int x, int y) { }

    public enum Direction {
        UP(0, 1), DOWN(0, -1), LEFT(-1, 0), RIGHT(1, 0);
        final int dx;
        final int dy;
        Direction(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }

        static Direction random(Random rng) {
            Direction[] values = values();
            return values[rng.nextInt(values.length)];
        }

        static List<Direction> shuffled(Random rng) {
            List<Direction> dirs = new ArrayList<>(Arrays.asList(values()));
            Collections.shuffle(dirs, rng);
            return dirs;
        }
    }
}