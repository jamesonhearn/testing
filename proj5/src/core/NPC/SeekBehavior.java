package core.NPC;

import core.AiBehavior;
import core.Direction;

import java.util.*;

public class SeekBehavior implements AiBehavior {
    private Direction desired;

    // Static RNG for tie-breaking so SeekBehavior doesn't need NPC RNG
    private static final Random RAND = new Random();

    @Override
    public void onEnterState(Npc owner) {
        desired = null;
    }

    @Override
    public void onTick(Npc owner, WorldView view) {
        desired = null;

        var avatarPos = view.avatarPosition();
        int ax = avatarPos.x();
        int ay = avatarPos.y();

        // Copy all directions
        List<Direction> directions = new ArrayList<>(List.of(Direction.values()));

        // Sort by squared Euclidean distance + tiny random jitter
        directions.sort(Comparator.comparingInt(dir -> {
            int nx = owner.x() + dir.dx;
            int ny = owner.y() + dir.dy;
            return heuristic(nx, ny, ax, ay) + RAND.nextInt(3);
        }));

        // Choose first valid move
        for (Direction dir : directions) {
            int nx = owner.x() + dir.dx;
            int ny = owner.y() + dir.dy;

            if (view.isWalkable(nx, ny) && !view.isOccupied(nx, ny)) {
                desired = dir;
                return;
            }
        }
    }

    @Override
    public Direction desiredMove() {
        return desired;
    }

    private int heuristic(int x1, int y1, int x2, int y2) {
        int dx = x1 - x2;
        int dy = y1 - y2;
        return dx*dx + dy*dy; // squared distance
    }
}
