package core.debug;

/**
 * Captures timing data for a single game loop iteration.
 */
public record FrameSample(
        long frameNanos,
        long renderNanos,
        long inputNanos,
        long movementNanos,
        long npcNanos,
        long combatNanos,
        long animationNanos,
        int npcCount,
        int corpseCount,
        int dropCount,
        boolean avatarMoved) {
}