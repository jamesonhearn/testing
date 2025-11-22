package core;

/**
 * Minimal health container so entities can track survivability without
 * embedding combat logic directly into NPCs.
 */
public class HealthComponent {
    private int current;
    private final int max;

    public HealthComponent(int max) {
        this(max, max);
    }

    public HealthComponent(int current, int max) {
        this.current = Math.min(current, max);
        this.max = max;
    }

    public int current() {
        return current;
    }

    public int max() {
        return max;
    }

    public boolean isDepleted() {
        return current <= 0;
    }

    public void damage(int amount) {
        current = Math.max(0, current - Math.max(0, amount));
    }

    public void heal(int amount) {
        current = Math.min(max, current + Math.max(0, amount));
    }
}