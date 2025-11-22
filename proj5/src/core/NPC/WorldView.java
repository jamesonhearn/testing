package core.NPC;

import core.Entity;
import tileengine.TETile;
import tileengine.Tileset;
import java.util.Set;


public class WorldView {
    private final TETile[][] world;
    private final Set<Entity.Position> occupied;
    private final Entity.Position avatarPosition;

    public WorldView(TETile[][] world, Entity.Position avatarPosition, Set<Entity.Position> occupied) {
        this.world = world;
        this.avatarPosition = avatarPosition;
        this.occupied = occupied;
    }

    public boolean isWalkable(int x, int y) {
        if (x < 0 || y < 0 || x >= world.length || y >= world[0].length) {
            return false;
        }
        return world[x][y].equals(Tileset.FLOOR);
    }

    public boolean isOccupied(int x, int y) {
        return occupied.contains(new Entity.Position(x, y));
    }

    public Entity.Position avatarPosition() {
        return avatarPosition;
    }
}
