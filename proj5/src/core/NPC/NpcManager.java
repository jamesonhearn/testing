package core.NPC;

import tileengine.TETile;
import tileengine.Tileset;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class NpcManager {
    private final Random rng;
    private final List<Npc> npcs = new ArrayList<>();

    private static final int DEFAULT_NPC_COUNT = 4;

    public NpcManager(Random rng) {
        this.rng = rng;
    }
    public List<Npc> npcs() {
        return npcs;
    }

    public void spawn(TETile[][] world, int avoidX, int avoidY) {
        npcs.clear();
        int attempts = 0;
        while (npcs.size() < DEFAULT_NPC_COUNT && attempts < 500) {
            attempts += 1;
            int x = rng.nextInt(world.length);
            int y = rng.nextInt(world[0].length);
            if (!world[x][y].equals(Tileset.FLOOR)){
                continue;
            }
            if (x == avoidX && y == avoidY) {
                continue;
            }
            if (isOccupied(x,y)) {
                continue;
            }
            Npc npc = new Npc(x,y, new Random(rng.nextLong()));
            npc.setDrawX(x);
            npc.setDrawY(y);
            npcs.add(npc);
        }

    }


    public void tick(TETile[][] world, int avatarX, int avatarY) {
        Set<Npc.Point> occupied = new HashSet<>();
        for (Npc npc : npcs) {
            occupied.add((new Npc.Point(npc.x(), npc.y())));
        }
        for (Npc npc : npcs) {
            occupied.remove(new Npc.Point(npc.x(), npc.y()));
            npc.tick(world, occupied, avatarX, avatarY);
            occupied.add(new Npc.Point(npc.x(), npc.y()));
        }
    }

    private boolean isOccupied(int x, int y) {
        for (Npc npc : npcs) {
            if (npc.x() == x && npc.y() == y) {
                return true;
            }
        }
        return false;
    }
}
