package core;

import core.NPC.Corpse;
import core.NPC.Npc;
import core.items.DroppedItem;
import core.items.Inventory;

import java.io.Serializable;
import java.util.List;

/**
 * serialized snapshot of the full game state so loads can restore the exact
 * world layout, player status, and NPC positions without replaying input
 */
public record SaveState(
        long worldSeed,
        long npcSeed,
        AvatarState avatar,
        double decayingLightRadius,
        long lastDecayTime,
        long lightSurgeStartMs,
        long playTimeMs,
        int enemiesFelled,
        int damageTaken,
        int damageGiven,
        InventoryState inventory,
        List<DroppedItemState> droppedItems,
        List<NpcState> npcs,
        List<CorpseState> corpses
) implements Serializable {

    public record AvatarState(int x, int y, int lives, int spawnX, int spawnY, HealthState health)
            implements Serializable {
    }

    public record HealthState(int current, int max, int armor, int invulnerabilityFrames, int invulnerabilityRemaining)
            implements Serializable {
    }

    public record InventoryState(int slots, List<ItemStackState> stacks) implements Serializable {
    }

    public record ItemStackState(String itemId, int quantity) implements Serializable {
    }

    public record DroppedItemState(String itemId, int quantity, int x, int y) implements Serializable {
    }

    public record NpcState(int x, int y, double drawX, double drawY, int variant, long rngSeed, HealthState health)
            implements Serializable {
    }

    public record CorpseState(int x, int y) implements Serializable {
    }

    public record SaveSnapshot(long worldSeed,
                               long npcSeed,
                               Avatar avatar,
                               double decayingLightRadius,
                               long lastDecayTime,
                               long lightSurgeStartMs,
                               long playTimeMs,
                               int enemiesFelled,
                               int damageTaken,
                               int damageGiven,
                               Inventory inventory,
                               List<DroppedItem> droppedItems,
                               List<Npc> npcs,
                               List<Corpse> corpses
    ) { }

    public static SaveState capture(SaveSnapshot snap) {

        Avatar avatar = snap.avatar();

        AvatarState avatarState = new AvatarState(
                avatar.x(),
                avatar.y(),
                avatar.lives(),
                avatar.spawnPoint().x(),
                avatar.spawnPoint().y(),
                new HealthState(
                        avatar.health().current(),
                        avatar.health().max(),
                        avatar.health().armor(),
                        avatar.health().invulnerabilityFrames(),
                        avatar.health().invulnerabilityRemaining())
        );

        // Inventory
        Inventory inventory = snap.inventory();
        List<ItemStackState> stacks = inventory.slots().stream().
                filter(stack -> stack != null && stack.quantity() > 0).
                map(stack -> new ItemStackState(stack.item().id(), stack.quantity())).
                toList();

        InventoryState inventoryState = new InventoryState(inventory.slots().size(), stacks);

        // Dropped items
        List<DroppedItemState> dropStates = snap.droppedItems().stream().
                map(drop -> new DroppedItemState(drop.item().id(), drop.quantity(), drop.x(), drop.y())).
                toList();

        // NPCs
        List<NpcState> npcStates = snap.npcs().stream().
                map(npc -> new NpcState(
                        npc.x(),
                        npc.y(),
                        npc.drawX(),
                        npc.drawY(),
                        npc.variant(),
                        npc.rngSeed(),
                        new HealthState(
                                npc.health().current(),
                                npc.health().max(),
                                npc.health().armor(),
                                npc.health().invulnerabilityFrames(),
                                npc.health().invulnerabilityRemaining()))).
                toList();

        // Corpses
        List<CorpseState> corpseStates = snap.corpses().stream().
                map(corpse -> new CorpseState(corpse.x(), corpse.y())).
                toList();

        return new SaveState(
                snap.worldSeed(),
                snap.npcSeed(),
                avatarState,
                snap.decayingLightRadius(),
                snap.lastDecayTime(),
                snap.lightSurgeStartMs(),
                snap.playTimeMs(),
                snap.enemiesFelled(),
                snap.damageTaken(),
                snap.damageGiven(),
                inventoryState,
                dropStates,
                npcStates,
                corpseStates
        );
    }
}
