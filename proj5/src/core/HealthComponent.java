package core;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Minimal health container so entities can track survivability without
 * embedding combat logic directly into NPCs.
 */
public class HealthComponent {
    private int current;
    private final int max;
    private final int armor;
    private final int invulnerabilityFrames;
    private int invulnerabilityRemaining;
    private final List<Consumer<Entity>> deathCallbacks = new ArrayList<>();

    public HealthComponent(int max) {
        this(max, max,0,0);
    }

    public HealthComponent(int current, int max, int armor, int invulnerabilityFrames) {
        this.current = Math.min(current, max);
        this.max = max;
        this.armor = Math.max(0, armor);
        this.invulnerabilityFrames = Math.max(0, invulnerabilityFrames);
    }

    public int current() {
        return current;
    }

    public int max() {
        return max;
    }
    public int armor() {
        return armor;
    }

    public boolean isDepleted() {
        return current <= 0;
    }


    /**
     * Apply damage after armor reduction. Returns true if health was reduced.
     */
    public boolean damage(int amount, Entity owner) {
        if (isInvulnerable() || amount <= 0 || isDepleted()) {
            return false;
        }
        int applied = Math.max(0, amount - armor);
        if (applied <= 0) {
            invulnerabilityRemaining = invulnerabilityFrames;
            return false;
        }
        current = Math.max(0, current - applied);
        invulnerabilityRemaining = invulnerabilityFrames;
        if (current == 0) {
            fireDeath(owner);
        }
        return true;
    }

    public boolean isInvulnerable() {
        return invulnerabilityRemaining > 0;
    }


    public void heal(int amount) {
        if (amount <= 0) {
            return;
        }
        current = Math.min(max, current + amount);
    }

    public void tickInvulnerability() {
        if (invulnerabilityRemaining > 0) {
            invulnerabilityRemaining -= 1;
        }
    }

    public void resetInvulnerability() {
        invulnerabilityRemaining = 0;
    }

    public void restoreFull() {
        current = max;
        resetInvulnerability();
    }

    public void addDeathCallback(Consumer<Entity> callback) {
        if (callback != null) {
            deathCallbacks.add(callback);
        }
    }

    private void fireDeath(Entity owner) {
        for (Consumer<Entity> callback : deathCallbacks) {
            callback.accept(owner);
        }
    }
}