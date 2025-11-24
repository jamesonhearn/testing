package core;


/**
 * Player-controlled entity with limited lives and a tracked spawn point.
 */
public class Avatar extends Entity {
    private int lives;
    private Position spawnPoint;

    public Avatar(int x, int y, int lives, HealthComponent health) {
        super(x, y, health);
        this.lives = Math.max(0, lives);
        this.spawnPoint = new Position(x, y);
    }

    public int lives() {
        return lives;
    }

    public void setSpawnPoint(Position spawnPoint) {
        this.spawnPoint = spawnPoint;
    }

    public Position spawnPoint() {
        return spawnPoint;
    }

    public void loseLife() {
        if (lives > 0) {
            lives -= 1;
        }
    }

    public void respawn() {
        if (spawnPoint != null) {
            setPosition(spawnPoint.x(), spawnPoint.y());
        }
        if (health != null) {
            health.restoreFull();
        }
    }
}
