package core.NPC;



import core.AiBehavior;
import core.Direction;

/**
 * Defines Wandering for AI
 */

public class WanderBehavior implements AiBehavior {
    @Override
    public void onEnterState(Npc owner) {
        // No-op placeholder behavior.
    }


    @Override
    public void onTick(Npc owner, WorldView view) {
        // No-op placeholder behavior.
    }

    @Override
    public Direction desiredMove() {
        return null;
    }



}
