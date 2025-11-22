package core;

import core.NPC.Npc;
import core.NPC.WorldView;

public interface AiBehavior {
    void onEnterState(Npc owner);
    void onTick(Npc owner, WorldView view);
    Direction desiredMove();
}
