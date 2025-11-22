package core.NPC;

import core.AiBehavior;
import core.Direction;

public class IdleBehavior implements AiBehavior {
    private Direction desired;

    @Override
    public void onEnterState(Npc owner) {
        desired = null;
        owner.setVelocity(0, 0);
    }

    @Override
    public void onTick(Npc owner, WorldView view) {
        desired = null;
    }

    @Override
    public Direction desiredMove() {
        return desired;
    }
}