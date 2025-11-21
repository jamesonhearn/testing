package core.NPC;

import tileengine.TETile;
import tileengine.Tileset;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Central coordinator for NPC creation, updates, and rendering helpers.
 */
public class NpcManager {
    private final Random rng;
    private final List<Npc> npcs = new ArrayList<>();
    /** Quick membership check for existing NPC tiles. */
    private final Set<Npc.Point> npcPositions = new HashSet<>();

    private static final int DEFAULT_NPC_COUNT = 4;

    public NpcManager(Random rng) {
        this.rng = rng;
    }

    public List<Npc> npcs() {
        return npcs;
    }

    /**
     * Spawn a handful of NPCs on random floor tiles, avoiding the avatar's starting tile.
     */
    public void spawn(TETile[][] world, int avoidX, int avoidY) {
        npcs.clear();
        npcPositions.clear();
        int attempts = 0;
        while (npcs.size() < DEFAULT_NPC_COUNT && attempts < 500) {
            attempts += 1;
            int x = rng.nextInt(world.length);
            int y = rng.nextInt(world[0].length);
            if (!world[x][y].equals(Tileset.FLOOR)) {
                continue;
            }
            if (x == avoidX && y == avoidY) {
                continue;
            }
            if (npcPositions.contains(new Npc.Point(x, y))) {
                continue;
            }
            Npc npc = new Npc(x, y, new Random(rng.nextLong()));
            npc.setDrawX(x);
            npc.setDrawY(y);
            npcs.add(npc);
            npcPositions.add(new Npc.Point(x, y));
        }
    }

    /**
     * Advance all NPCs by one tick with simple collision against walls, avatar, and each other.
     */
    public void tick(TETile[][] world, int avatarX, int avatarY) {
        Set<Npc.Point> occupied = buildOccupiedSet(avatarX, avatarY);

        for (Npc npc : npcs) {
            Npc.Point previous = new Npc.Point(npc.x(), npc.y());
            occupied.remove(previous);
            npcPositions.remove(previous);

            npc.tick(world, occupied);

            Npc.Point updated = new Npc.Point(npc.x(), npc.y());
            npcPositions.add(updated);
            occupied.add(updated);
        }
    }

    private Set<Npc.Point> buildOccupiedSet(int avatarX, int avatarY) {
        Set<Npc.Point> occupied = new HashSet<>(npcPositions);
        occupied.add(new Npc.Point(avatarX, avatarY));
        return occupied;
    }

    /**
     * True when any NPC currently sits on the requested tile.
     */
    public boolean isNpcAt(int x, int y) {
        return npcPositions.contains(new Npc.Point(x, y));
    }
}