package core.NPC;

import org.knowm.xchart.internal.chartpart.Axis;
import tileengine.TETile;
import tileengine.Tileset;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Skeleton representation for any NPC with a random walk behavior and sprite cycling for
 * animations similar to Avatar.
 * Instances updated by {@link NpcManager} and are rendered directly by engine between
 * base and front tile layers same as Avatar
 */
public class Npc {
    private final Random rng;
    private int x;
    private int y;
    private Direction facing = Direction.DOWN;
    private int animFrame = 0;
    private int animTick = 0;
    private int moveTick = 0;


    private double drawX;
    private double drawY;

    public double drawX() {
        return drawX;
    }
    public double drawY() {
        return drawY;
    }


    public void setDrawX(double x) { this.drawX = x; }
    public void setDrawY(double y) { this.drawY = y; }

    public void updateSmoothPosition(double speed) {
        drawX += (x - drawX) * speed;
        drawY += (y - drawY) * speed;
    }




    //same tuning for animation cycling
    private static final int STEP_INTERVAL = 10;
    private static final int ANIM_INTERVAL = 5;

    public Npc(int x, int y, Random rng) {
        this.x = x;
        this.y = y;
        this.rng = rng;
    }

    public int x() {
        return x;
    }

    public int y(){
        return y;
    }

    public void tick(TETile[][] world, Set<Point> occupied, int avatarX, int avatarY){
        moveTick +=1;
        animTick +=1;

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


        attempts.remove(preferred);
        attempts.addFirst(preferred);

        for (Direction dir : attempts) {
            int nx = x + dir.dx;
            int ny = y + dir.dy;
            if (!canOccupy(world, occupied, avatarX, avatarY, nx, ny)) {
                continue;
            }
            facing = dir;
            x = nx;
            y = ny;
            return;
        }
    }

    private boolean canOccupy(TETile[][] world, Set<Point> occupied, int avatarX, int avatarY, int nx, int ny) {
        if (nx < 0 || ny < 0 || nx >= world.length || ny >= world[0].length) {
            return false;
        }
        if (!world[nx][ny].equals(Tileset.FLOOR)) {
            return false;
        }
        if (occupied.contains(new Point(nx, ny))) {
            return false;
        }
        return !(nx == avatarX && ny == avatarY);
    }


    public TETile currentTile() {
        return switch (facing) {
            case UP -> Tileset.SPHEREVIS;
            case DOWN -> Tileset.SPHEREVIS;
            case LEFT -> Tileset.SPHEREVIS;
            case RIGHT -> Tileset.SPHEREVIS;
//            case UP -> Tileset.SLIME_UP_FRAMES[animFrame];
//            case DOWN -> Tileset.SLIME_DOWN_FRAMES[animFrame];
//            case LEFT -> Tileset.SLIME_LEFT_FRAMES[animFrame];
//            case RIGHT -> Tileset.SLIME_RIGHT_FRAMES[animFrame];
        };
    }


    public record Point(int x, int y) { }

    public enum Direction {
        UP(0,1), DOWN(0,-1), LEFT(-1, 0), RIGHT(1,0);
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
            Collections.shuffle(dirs,rng);
            return dirs;
        }
    }

}
