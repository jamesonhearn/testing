package core.NPC;

import core.AiBehavior;
import core.Direction;

import java.util.ArrayList;
import java.util.List;

public class SeekBehavior implements AiBehavior {
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

        Direction primary = Math.abs(dx) >= Math.abs(dy)
                ? horizontal(dx)
                : vertical(dy);
        Direction secondary = primary == horizontal(dx) ? vertical(dy) : horizontal(dx);

        List<Direction> attempts = new ArrayList<>();
        if (primary != null) {
            attempts.add(primary);
        }
        if (secondary != null && secondary != primary) {
            attempts.add(secondary);
        }

        for (Direction dir : attempts) {
            int nx = owner.x() + dir.dx;
            int ny = owner.y() + dir.dy;
            if (view.isWalkable(nx, ny) && !view.isOccupied(nx, ny)) {
                desired = dir;
                return;
            }
        }
    }

    private Direction horizontal(int dx) {
        if (dx == 0) {
            return null;
        }
        return dx > 0 ? Direction.RIGHT : Direction.LEFT;
    }

    private Direction vertical(int dy) {
        if (dy == 0) {
            return null;
        }
        return dy > 0 ? Direction.UP : Direction.DOWN;
    }

    @Override
    public Direction desiredMove() {
        return desired;
    }
}