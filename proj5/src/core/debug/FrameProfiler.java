package core.debug;

import utils.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Collects per-frame timing samples and writes them to a CSV file when requested.
 */
public class FrameProfiler {
    private static final int MAX_SAMPLES = 20_000;
    private static final DateTimeFormatter FILENAME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final List<FrameSample> samples = new ArrayList<>();
    private boolean active;

    public boolean isActive() {
        return active;
    }

    public void start() {
        samples.clear();
        active = true;
    }

    public void stop() {
        active = false;
    }

    public void record(FrameSample sample) {
        if (!active) {
            return;
        }
        if (samples.size() >= MAX_SAMPLES) {
            return; // prevent unbounded growth during long sessions
        }
        samples.add(sample);
    }

    /**
     * Writes the collected samples to a CSV file inside the provided directory.
     * Returns the path of the file written or null if no samples were captured.
     */
    public Path writeCsv(Path outputDir) {
        if (samples.isEmpty()) {
            return null;
        }

        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create profiling directory", e);
        }

        Path output = outputDir.resolve("profile-" + LocalDateTime.now().format(FILENAME_FORMAT) + ".csv");
        StringBuilder sb = new StringBuilder();
        sb.append("frame,frame_ms,render_ms,input_ms,movement_ms,npc_ms,combat_ms,animation_ms,npcs,corpses,drops,avatar_moved\n");

        double totalFrameMs = 0.0;
        double totalRenderMs = 0.0;
        double totalInputMs = 0.0;
        double totalMovementMs = 0.0;
        double totalNpcMs = 0.0;
        double totalCombatMs = 0.0;
        double totalAnimMs = 0.0;

        int i = 1;
        for (FrameSample sample : samples) {
            double frameMs = nanosToMs(sample.frameNanos());
            double renderMs = nanosToMs(sample.renderNanos());
            double inputMs = nanosToMs(sample.inputNanos());
            double movementMs = nanosToMs(sample.movementNanos());
            double npcMs = nanosToMs(sample.npcNanos());
            double combatMs = nanosToMs(sample.combatNanos());
            double animationMs = nanosToMs(sample.animationNanos());

            totalFrameMs += frameMs;
            totalRenderMs += renderMs;
            totalInputMs += inputMs;
            totalMovementMs += movementMs;
            totalNpcMs += npcMs;
            totalCombatMs += combatMs;
            totalAnimMs += animationMs;

            sb.append(i).append(',')
                    .append(format(frameMs)).append(',')
                    .append(format(renderMs)).append(',')
                    .append(format(inputMs)).append(',')
                    .append(format(movementMs)).append(',')
                    .append(format(npcMs)).append(',')
                    .append(format(combatMs)).append(',')
                    .append(format(animationMs)).append(',')
                    .append(sample.npcCount()).append(',')
                    .append(sample.corpseCount()).append(',')
                    .append(sample.dropCount()).append(',')
                    .append(sample.avatarMoved()).append('\n');
            i += 1;
        }

        int count = samples.size();
        sb.append("# averages(ms): frame=").append(format(totalFrameMs / count))
                .append(" render=").append(format(totalRenderMs / count))
                .append(" input=").append(format(totalInputMs / count))
                .append(" move=").append(format(totalMovementMs / count))
                .append(" npc=").append(format(totalNpcMs / count))
                .append(" combat=").append(format(totalCombatMs / count))
                .append(" anim=").append(format(totalAnimMs / count)).append('\n');

        FileUtils.writeFile(output.toString(), sb.toString());
        return output;
    }

    private static String format(double value) {
        return String.format(Locale.US, "%.3f", value);
    }

    private static double nanosToMs(long nanos) {
        return nanos / 1_000_000.0;
    }
}