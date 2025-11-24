package core;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 * Centralized dispatcher for combat events. Entities enqueue damage and the
 * service resolves armor, invulnerability frames, and death callbacks each tick.
 */
public class CombatService {
    public record DamageEvent(Entity target, Entity source, int amount) { }

    private final Queue<DamageEvent> damageEvents = new ArrayDeque<>();
    private final Set<Entity> trackedEntities = new HashSet<>();

    public void register(Entity entity) {
        if (entity != null) {
            trackedEntities.add(entity);
        }
    }

    public void unregister(Entity entity) {
        trackedEntities.remove(entity);
    }

    public void queueDamage(Entity target, Entity source, int amount) {
        if (target == null || target.health() == null) {
            return;
        }
        damageEvents.add(new DamageEvent(target, source, Math.max(0, amount)));
    }

    /**
     * Resolve invulnerability timers and apply any queued damage for this frame.
     */
    public void tick() {
        for (Entity entity : trackedEntities) {
            if (entity.health() != null) {
                entity.health().tickInvulnerability();
            }
        }

        int eventsToProcess = damageEvents.size();
        for (int i = 0; i < eventsToProcess; i += 1) {
            DamageEvent event = damageEvents.poll();
            if (event == null) {
                continue;
            }
            applyDamage(event);
        }
    }

    private void applyDamage(DamageEvent event) {
        HealthComponent health = event.target().health();
        if (health == null) {
            return;
        }
        health.damage(event.amount(), event.target());
    }
}