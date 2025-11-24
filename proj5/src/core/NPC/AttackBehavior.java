package core.NPC;

import core.AiBehavior;
import core.Direction;

public class AttackBehavior implements AiBehavior {
    private Direction desired;

    @Override
    public void onEnterState(Npc owner) {
        desired = null;
    }

    @Override
    public void onTick(Npc owner, WorldView view) {
        desired = null;
        int dx = view.avatarPosition().x() - owner.x();
        int dy = view.avatarPosition().y() - owner.y();
        if (Math.abs(dx) + Math.abs(dy) < 3) {
            owner.setFacing(directionToward(dx, dy, owner.facing()));
            view.damageAvatar(2, owner);
            owner.markAttacking();
        }
    }

    private Direction directionToward(int dx, int dy, Direction fallback) {
        if (Math.abs(dx) > Math.abs(dy)) {
            return dx > 0 ? Direction.RIGHT : Direction.LEFT;
        }
        if (dy != 0) {
            return dy > 0 ? Direction.UP : Direction.DOWN;
        }
        return fallback;
    }

    @Override
    public Direction desiredMove() {
        return desired;
    }
}