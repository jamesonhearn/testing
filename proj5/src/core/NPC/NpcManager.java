package core.NPC;

import core.Entity;


import core.Avatar;
import core.CombatService;
import edu.princeton.cs.algs4.StdDraw;
import tileengine.TETile;
import tileengine.Tileset;
import core.HealthComponent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Central coordinator for NPC creation, updates, and rendering helpers.
 */
public class NpcManager {
    private final Random rng;
    private final List<Npc> npcs = new ArrayList<>();
    private final List<Corpse> corpses = new ArrayList<>();
    /** Quick membership check for existing NPC tiles. */
    private final Set<Entity.Position> npcPositions = new HashSet<>();
    private final CombatService combatService;
    private Consumer<Npc> deathHandler = npc -> {};

    private static final int DEFAULT_NPC_COUNT = 30;

    public NpcManager(Random rng, CombatService combatService) {
        this.rng = rng;
        this.combatService = combatService;
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
        corpses.clear();
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
            if (npcPositions.contains(new Entity.Position(x, y))) {
                continue;
            }
            int variant = selectVariant();
            HealthComponent health = new HealthComponent(3, 3, 0, 8);
            Npc npc = new Npc(x, y, new Random(rng.nextLong()), Tileset.loadNpcSpriteSet(variant), health);
            npc.setDrawX(x);
            npc.setDrawY(y);
            health.addDeathCallback(entity -> handleNpcDeath((Npc) entity));
            combatService.register(npc);
            npcs.add(npc);
            npcPositions.add(new Entity.Position(x, y));
            StdDraw.pause(20);

        }
    }

    /**
     * Advance all NPCs by one tick with simple collision against walls, avatar, and each other.
     */
    public void tick(TETile[][] world, Avatar avatar) {
        Set<Entity.Position> occupied = buildOccupiedSet(avatar.x(), avatar.y());
        WorldView sharedView = new WorldView(world, avatar, occupied, combatService);

        for (Npc npc : npcs) {
            Entity.Position previous = new Entity.Position(npc.x(), npc.y());
            occupied.remove(previous);
            npcPositions.remove(previous);

            npc.tick(sharedView);

            Entity.Position updated = new Entity.Position(npc.x(), npc.y());
            npcPositions.add(updated);
            occupied.add(updated);
        }
    }

    public List<Corpse> corpses() {
        return corpses;
    }


    private Set<Entity.Position> buildOccupiedSet(int avatarX, int avatarY) {
        Set<Entity.Position> occupied = new HashSet<>(npcPositions);
        occupied.add(new Entity.Position(avatarX, avatarY));
        return occupied;
    }



    /**
     * True when any NPC currently sits on the requested tile.
     */
    public boolean isNpcAt(int x, int y)
    {
        return npcPositions.contains(new Entity.Position(x, y));
    }


    public void setDeathHandler(Consumer<Npc> deathHandler) {
        this.deathHandler = deathHandler;
    }

    private int selectVariant() {
        List<Integer> variants = availableVariants();
        if (variants.isEmpty()) {
            return 0;
        }
        return variants.get(rng.nextInt(variants.size()));
    }

    private List<Integer> availableVariants() {
        List<Integer> variants = new ArrayList<>();
        Path npcRoot = Path.of("assets", "avatars", "NPC");
        try (var paths = Files.list(npcRoot)) {
            paths.filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.matches("\\d+"))
                    .map(Integer::valueOf)
                    .sorted()
                    .forEach(variants::add);
        } catch (IOException e) {
            // Fallback to default variant when asset listing fails.
        }
        if (variants.isEmpty()) {
            variants.add(0);
        }
        return variants;
    }

    private void handleNpcDeath(Npc npc) {
        npcPositions.remove(new Entity.Position(npc.x(), npc.y()));
        npcs.remove(npc);
        corpses.add(new Corpse(npc.x(), npc.y(), Tileset.NPC_CORPSE));
        combatService.unregister(npc);
        deathHandler.accept(npc);
    }

}