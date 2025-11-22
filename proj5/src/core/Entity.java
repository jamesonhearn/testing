package core;

/**
 * Class for Entity objects - will use to abstract NPC/Enemy objects and set defined characteristics
 * Will essentially need to port over eveyhing that Avatar has, plus additional details like RNG, movement
 * countdowns (when to go from idle to moving to seeking) and positioning details
 */
public class Entity {
    protected int x;
    protected int y;
    protected Direction facing = Direction.DOWN;
    protected double velocityX = 0.0;
    protected double velocityY = 0.0;
    protected HealthComponent health;

    public Entity(int x, int y) {
        this(x,y, new HealthComponent(1));
    }

    public Entity(int x, int y, HealthComponent health) {
        this.x = x;
        this.y = y;
        this.health = health;
    }

    public int x() {
        return x;
    }
    public int y(){
        return y;
    }

    public Position position() {
        return new Position(x,y);
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Direction facing() {
        return facing;
    }

    public void setFacing(Direction facing) {
        this.facing = facing;
    }

    public double velocityX() {
        return velocityX;
    }
    public double velocityY() {
        return velocityY;
    }
    public void setVelocity(double velocityX, double velocityY) {
        this.velocityX = velocityX;
        this.velocityY = velocityY;
    }

    public HealthComponent health() {
        return health;
    }
    public record Position(int x, int y){}
}
