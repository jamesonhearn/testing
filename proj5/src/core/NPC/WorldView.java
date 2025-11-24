package core.NPC;

import core.Entity;
import tileengine.TETile;
import tileengine.Tileset;
import java.util.Set;
import core.Avatar;
import core.CombatService;


public class WorldView {
    private final TETile[][] world;
    private final Set<Entity.Position> occupied;
    private final Entity.Position avatarPosition;
    private final Avatar avatar;
    private final CombatService combatService;

    public WorldView(TETile[][] world, Avatar avatar, Set<Entity.Position> occupied, CombatService combatService) {
        this.world = world;
        this.avatarPosition = avatar.position();
        this.avatar = avatar;
        this.occupied = occupied;
        this.combatService = combatService;
    }

    public boolean isWalkable(int x, int y) {
        if (x < 0 || y < 0 || x >= world.length || y >= world[0].length) {
            return false;
        }
        return world[x][y].equals(Tileset.FLOOR);
    }

    public Avatar avatar() {
        return avatar;
    }

    public void damageAvatar(int amount, Entity source) {
        combatService.queueDamage(avatar, source, amount);
    }

    public boolean isOccupied(int x, int y) {
        return occupied.contains(new Entity.Position(x, y));
    }

    public Entity.Position avatarPosition() {
        return avatarPosition;
    }
}
