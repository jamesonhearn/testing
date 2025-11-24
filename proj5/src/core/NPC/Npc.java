package core.NPC;

import core.AiBehavior;
import core.Direction;
import core.Entity;

import tileengine.TETile;
import tileengine.Tileset;

import java.util.EnumMap;
import java.util.Random;
import java.util.Set;

/**
 * Minimal NPC representation with random-walk behavior and sprite cycling.
 * Instances are updated by {@link NpcManager} and rendered directly by the engine
 * between the base and front tile layers.
 */
public class Npc extends Entity{
    private final Random rng;
    private int animFrame = 0;
    private int animTick = 0;
    private int moveTick = 0;

    private boolean attacking = false;


    private final Tileset.NpcSpriteSet spriteSet;

    private final EnumMap<State, AiBehavior> behaviors = new EnumMap<>(State.class);
    private State state = State.IDLE;
    private AiBehavior activeBehavior;

    // Tunables for movement and animation pacing.
    private static final int STEP_INTERVAL = 8;    // ticks between movement attempts
    private static final int ANIM_INTERVAL = 3;    // ticks between animation frames

    private double drawX;
    private double drawY;

    public void setDrawX(double x) { this.drawX = x; }
    public void setDrawY(double y) { this.drawY = y; }

    public void updateSmooth(double speed) {
        drawX += (x - drawX) * speed;
        drawY += (y - drawY) * speed;
    }


    public Npc(int x, int y, Random rng, Tileset.NpcSpriteSet spriteSet, core.HealthComponent health) {
        super(x, y, health);
        this.rng = rng;
        this.spriteSet = spriteSet;
        behaviors.put(State.IDLE, new IdleBehavior());
        behaviors.put(State.SEEK, new SeekBehavior());
        behaviors.put(State.ATTACK, new AttackBehavior());
        switchState(State.IDLE);
        this.drawX = x;
        this.drawY = y;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public double drawX() {
        return drawX;
    }
    public double drawY() {
        return drawY;
    }

    /**
     * Advance one tick of NPC simulation: possibly move and advance animation.
     */
    public void tick(WorldView view) {
        moveTick += 1;
        animTick += 1;



        attacking = false;
        if (moveTick < STEP_INTERVAL) {
            updateAnimationFrame();;
        }
        moveTick = 0;

        State desiredState = selectState(view);
        if (desiredState != state) {
            switchState(desiredState);
        }

        activeBehavior.onTick(this, view);
        Direction move = activeBehavior.desiredMove();
        if (move == null) {
            updateAnimationFrame();
            return;
        }
        int nx = x + move.dx;
        int ny = y + move.dy;
        if (view.isWalkable(nx, ny) && !view.isOccupied(nx, ny)) {
            facing = move;
            x = nx;
            y = ny;
            updateAnimationFrame();
            return;
        }
        updateAnimationFrame();
    }

    private State selectState(WorldView view) {
        int dx = Math.abs(view.avatarPosition().x() - x);
        int dy = Math.abs(view.avatarPosition().y() - y);
        int manhattan = dx + dy;
        if (manhattan <= 2) {
            return State.ATTACK;
        }
        if (manhattan < 15) {
            return State.SEEK;
        }
        return State.IDLE;
    }


    private void switchState(State next) {
        state = next;
        activeBehavior = behaviors.get(next);
        animTick = 0;
        animFrame = 0;
        activeBehavior.onEnterState(this);

    }


    /**
     * Current animation frame tile based on facing direction.
     */
    public TETile currentTile() {
        TETile[] frames = attacking ? attackFramesForFacing() : walkFramesForFacing();
        return frames[animFrame];
    }

    public void markAttacking() {
        attacking = true;
    }

    private void updateAnimationFrame() {
        if (animTick < ANIM_INTERVAL) {
            return;
        }
        animTick = 0;
        int frameCount = attacking ? spriteSet.attackUpFrames().length : spriteSet.walkUpFrames().length;
        animFrame = (animFrame + 1) % frameCount;
    }

    private TETile[] walkFramesForFacing() {
        return switch (facing) {
            case UP -> spriteSet.walkUpFrames();
            case DOWN -> spriteSet.walkDownFrames();
            case LEFT -> spriteSet.walkLeftFrames();
            case RIGHT -> spriteSet.walkRightFrames();
        };
    }
    private TETile[] attackFramesForFacing() {
        return switch (facing) {

            case UP -> spriteSet.attackUpFrames();
            case DOWN -> spriteSet.attackDownFrames();
            case LEFT -> spriteSet.attackLeftFrames();
            case RIGHT -> spriteSet.attackRightFrames();
    };
}
    private enum State {
        IDLE,
        SEEK,
        ATTACK
    }
}