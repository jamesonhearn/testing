package core.NPC;

import core.Entity;


import core.Avatar;
import core.CombatService;
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

import core.animation.AnimationCycle;

/**
 *  coordinator for NPC creation, updates, and rendering
 */
public class NpcManager {
    private final Random rng;
    private final List<Npc> npcs = new ArrayList<>();
    private final List<Corpse> corpses = new ArrayList<>();
    /** Corpses that still have death animation to play */
    private final List<Corpse> animatingCorpses = new ArrayList<>();
    /**  membership check for existing NPC tiles. */
    private final Set<Entity.Position> npcPositions = new HashSet<>();
    private final CombatService combatService;
    private Consumer<Npc> deathHandler = npc -> { };
    private final int MAX_ATTEMPTS = 500;
    /**lookup of NPCs by tile for hitbox collision */
    private final java.util.Map<Entity.Position, List<Npc>> npcByTile = new java.util.HashMap<>();

    private static final int DEFAULT_NPC_COUNT = 60;

    public NpcManager(Random rng, CombatService combatService) {
        this.rng = rng;
        this.combatService = combatService;
    }

    public List<Npc> npcs() {
        return npcs;
    }

    /**
     * Spawn handful of NPCs on random floors, avoiding the avatars starting tile
     */
    public void spawn(TETile[][] world, int avoidX, int avoidY) {
        npcs.clear();
        npcByTile.clear();
        corpses.clear();
        animatingCorpses.clear();
        int attempts = 0;
        while (npcs.size() < DEFAULT_NPC_COUNT && attempts < MAX_ATTEMPTS) {
            attempts += 1;
            int x = rng.nextInt(world.length);
            int y = rng.nextInt(world[0].length);
            if (!world[x][y].equals(Tileset.FLOOR)) {
                continue;
            }
            if (x == avoidX && y == avoidY) {
                continue;
            }
            int variant = selectVariant();
            long npcSeed = rng.nextLong();
            HealthComponent health = new HealthComponent(3, 3, 0, 8);
            Npc npc = new Npc(x, y, new Random(npcSeed), npcSeed, variant,
                    Tileset.loadNpcSpriteSet(variant), health);
            npc.setDrawX(x);
            npc.setDrawY(y);
            health.addDeathCallback(entity -> handleNpcDeath((Npc) entity));
            combatService.register(npc);
            npcs.add(npc);
            addNpcPosition(new Entity.Position(x, y), npc);
        }
    }

    /**
     * advance all NPCs by one tick with collision check against walls, avatar, other npcs.
     */
    public void tick(TETile[][] world, Avatar avatar) {
        Entity.Position avatarPos = new Entity.Position(avatar.x(), avatar.y());
        Set<Entity.Position> occupied = buildOccupiedSet(avatarPos);
        WorldView sharedView = new WorldView(world, avatar, occupied, combatService);

        for (Npc npc : npcs) {
            Entity.Position previous = new Entity.Position(npc.x(), npc.y());
            removeNpcPosition(previous, npc);

            if (previous.equals(avatarPos)) {
                occupied.add(avatarPos);
            }

            npc.tick(sharedView);

            Entity.Position updated = new Entity.Position(npc.x(), npc.y());
            addNpcPosition(updated, npc);
        }
        tickCorpses();
    }

    public List<Corpse> corpses() {
        return corpses;
    }

    public void damageAtTile(int x, int y, Entity source, int amount) {
        if (amount <= 0) {
            return;
        }
        for (Npc npc : npcsAtTile(x, y)) {
            combatService.queueDamage(npc, source, amount);
        }
    }

    private Set<Entity.Position> buildOccupiedSet(Entity.Position avatarPos) {
        Set<Entity.Position> occupied = new HashSet<>();

        // Avatar tile ALWAYS blocked
        occupied.add(avatarPos);

        for (Npc npc : npcs) {

            int dx = Math.abs(npc.x() - avatarPos.x());
            int dy = Math.abs(npc.y() - avatarPos.y());
            int dist = dx + dy;

            Entity.Position pos = new Entity.Position(npc.x(), npc.y());

            // Far from player = normal collision (1 NPC per tile)
            if (dist > 4) {
                occupied.add(pos);
            } else if (dist > 2) {
                if (npcCountAt(pos) >= 3) {
                    occupied.add(pos);
                }
            } else {
                continue;
            }
        }

        return occupied;
    }
    private int npcCountAt(Entity.Position pos) {
        List<Npc> list = npcByTile.get(pos);
        return list == null ? 0 : list.size();
    }


    public boolean isNpcAt(int x, int y) {
        return npcByTile.containsKey(new Entity.Position(x, y));
    }



    public Npc npcAtTile(int x, int y) {
        List<Npc> occupants = npcByTile.get(new Entity.Position(x, y));
        if (occupants == null || occupants.isEmpty()) {
            return null;
        }
        return occupants.get(0);
    }


    public List<Npc> npcsAtTile(int x, int y) {
        List<Npc> occupants = npcByTile.get(new Entity.Position(x, y));
        return occupants == null ? List.of() : List.copyOf(occupants);
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
            paths.filter(Files::isDirectory).
                    map(path -> path.getFileName().toString()).
                    filter(name -> name.matches("\\d+")).
                    map(Integer::valueOf).
                    sorted().
                    forEach(variants::add);
        } catch (IOException e) {
            // Fallback to default variant when asset listing fails.
        }
        if (variants.isEmpty()) {
            variants.add(0);
        }
        return variants;
    }

    private void handleNpcDeath(Npc npc) {
        removeNpcPosition(new Entity.Position(npc.x(), npc.y()), npc);
        npcs.remove(npc);
        AnimationCycle death = new AnimationCycle(new TETile[]{npc.currentTile(), Tileset.NPC_CORPSE}, 2, false);
        Corpse corpse = new Corpse(npc.x(), npc.y(), death);
        corpses.add(corpse);
        if (corpse.isAnimating()) {
            animatingCorpses.add(corpse);
        }
        combatService.unregister(npc);
        deathHandler.accept(npc);
    }



    private void tickCorpses() {
        animatingCorpses.removeIf(corpse -> !corpse.tick());
    }

    public void restoreState(List<Npc> restoredNpcs, List<Corpse> restoredCorpses) {
        npcs.clear();
        npcByTile.clear();
        corpses.clear();
        animatingCorpses.clear();

        for (Npc npc : restoredNpcs) {
            npcs.add(npc);
            addNpcPosition(new Entity.Position(npc.x(), npc.y()), npc);
            npc.health().addDeathCallback(entity -> handleNpcDeath((Npc) entity));
            combatService.register(npc);
        }

        if (restoredCorpses != null) {
            corpses.addAll(restoredCorpses);
            for (Corpse corpse : restoredCorpses) {
                if (corpse.isAnimating()) {
                    animatingCorpses.add(corpse);
                }
            }
        }
    }

    private void addNpcPosition(Entity.Position position, Npc npc) {
        npcByTile.computeIfAbsent(position, p -> new ArrayList<>()).add(npc);
    }

    private void removeNpcPosition(Entity.Position position, Npc npc) {
        List<Npc> occupants = npcByTile.get(position);
        if (occupants == null) {
            return;
        }
        occupants.remove(npc);
        if (occupants.isEmpty()) {
            npcByTile.remove(position);
        }
    }
}
