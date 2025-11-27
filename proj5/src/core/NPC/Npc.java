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
    private final int animPhaseOffset;

    private boolean attacking = false;


    public final Tileset.NpcSpriteSet spriteSet;

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

    /**
     * Nudge the render position toward the logical tile while snapping when close
     * to avoid visible drift (e.g., appearing a tile above the actual blocker).
     */
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
        this.animPhaseOffset = rng.nextInt(ANIM_INTERVAL);
        this.animFrame = rng.nextInt(spriteSet.walkUpFrames().length);
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


        State desiredState = selectState(view);
        if (desiredState != state) {
            switchState(desiredState);
        }

        activeBehavior.onTick(this, view);
        Direction move = null;
        if (moveTick >= STEP_INTERVAL) {
            moveTick = 0;
            move = activeBehavior.desiredMove();
        }

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


    // Less than 2 - attack
    // Less than 15 more than 2 - Seek
    // More than 15 - Idle
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
        animTick = rng.nextInt(ANIM_INTERVAL);
        animFrame = rng.nextInt(frameCountForState(next));
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
        if ((animTick + animPhaseOffset) % ANIM_INTERVAL != 0) {
            return;
        }
        animTick = 0;
        int frameCount = attacking ? spriteSet.attackUpFrames().length : spriteSet.walkUpFrames().length;
        animFrame = (animFrame + 1) % frameCount;
    }

    private int frameCountForState(State next) {
        return next == State.ATTACK ? spriteSet.attackUpFrames().length : spriteSet.walkUpFrames().length;
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