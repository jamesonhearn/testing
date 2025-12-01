package core.NPC;

import core.AiBehavior;
import core.Direction;
import core.Entity;
import core.animation.AnimationCycle;

import tileengine.TETile;
import tileengine.Tileset;

import java.util.EnumMap;
import java.util.Random;

/**
 * Minimal NPC representation with random-walk behavior and sprite cycling.
 * Instances are updated by {@link NpcManager} and rendered directly by the engine
 * between the base and front tile layers.
 */
public class Npc extends Entity {
    private final Random rng;
    private int moveTick = 0;

    private boolean attacking = false;


    public final Tileset.NpcSpriteSet spriteSet;

    private final EnumMap<Action, EnumMap<Direction, AnimationCycle>> animations = new EnumMap<>(Action.class);
    private AnimationCycle currentAnimation;
    private Action currentAction = Action.WALK;

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
        this.drawX = x;
        this.drawY = y;
        initializeAnimations();
        switchState(State.IDLE);
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



        attacking = false;


        State desiredState = selectState(view);
        boolean stateChanged = desiredState != state;
        if (stateChanged) {
            switchState(desiredState);
        }

        activeBehavior.onTick(this, view);
        Direction move = null;
        if (moveTick >= STEP_INTERVAL) {
            moveTick = 0;
            move = activeBehavior.desiredMove();
        }

        if (move == null) {
            updateAnimation(stateChanged);
            return;
        }
        int nx = x + move.dx;
        int ny = y + move.dy;
        if (view.isWalkable(nx, ny) && !view.isOccupied(nx, ny)) {
            facing = move;
            x = nx;
            y = ny;
            updateAnimation(stateChanged);
            return;
        }
        updateAnimation(stateChanged);
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
        currentAction = Action.WALK;
        currentAnimation = animations.get(currentAction).get(facing);
        resetAnimationPhase();
        activeBehavior.onEnterState(this);

    }


    /**
     * Current animation frame tile based on facing direction.
     */
    public TETile currentTile() {
        return currentAnimation.currentFrame();
    }

    public void markAttacking() {
        attacking = true;
    }

    private void updateAnimation(boolean refreshedState) {
        Action desiredAction = attacking ? Action.ATTACK : Action.WALK;
        EnumMap<Direction, AnimationCycle> byDirection = animations.get(desiredAction);
        AnimationCycle selected = byDirection.get(facing);

        boolean actionChanged = desiredAction != currentAction;
        boolean animationChanged = selected != currentAnimation;

        if (actionChanged || refreshedState) {
            selected.randomizeFrame(rng);
        } else if (animationChanged && currentAnimation != null) {
            int carryIndex = currentAnimation.frameIndex() % selected.frameCount();
            selected.setFrameIndex(carryIndex);
        }

        currentAction = desiredAction;
        currentAnimation = selected;
        currentAnimation.advance();
    }


    private void initializeAnimations() {
        animations.put(Action.WALK, buildDirectionAnimations(
                spriteSet.walkUpFrames(), spriteSet.walkDownFrames(),
                spriteSet.walkLeftFrames(), spriteSet.walkRightFrames()));
        animations.put(Action.ATTACK, buildDirectionAnimations(
                spriteSet.attackUpFrames(), spriteSet.attackDownFrames(),
                spriteSet.attackLeftFrames(), spriteSet.attackRightFrames()));

        currentAnimation = animations.get(currentAction).get(facing);
        resetAnimationPhase();
    }

    private EnumMap<Direction, AnimationCycle> buildDirectionAnimations(TETile[] up, TETile[] down,
                                                                        TETile[] left, TETile[] right) {
        EnumMap<Direction, AnimationCycle> map = new EnumMap<>(Direction.class);
        map.put(Direction.UP, new AnimationCycle(up, ANIM_INTERVAL));
        map.put(Direction.DOWN, new AnimationCycle(down, ANIM_INTERVAL));
        map.put(Direction.LEFT, new AnimationCycle(left, ANIM_INTERVAL));
        map.put(Direction.RIGHT, new AnimationCycle(right, ANIM_INTERVAL));
        return map;
    }

    private void resetAnimationPhase() {
        if (currentAnimation != null) {
            currentAnimation.randomizeFrame(rng);
        }
    }
    private enum State {
        IDLE,
        SEEK,
        ATTACK
    }

    private enum Action {
        WALK,
        ATTACK
    }
}