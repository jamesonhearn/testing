package tileengine;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;


public class SpriteSheet {
    private final BufferedImage sheet;
    private final int frameWidth;
    private final int frameHeight;
    private final Map<String, String> cachedFramePaths = new HashMap<>();

    /**
     * @param sheetPath Path to the sprite sheet image in assets directory
     * @param frameWidth Width of a single frame in pixels
     * @param frameHeight Height of a single frame in pixels
     */
    public SpriteSheet(String sheetPath, int frameWidth, int frameHeight){
        try {
            this.sheet = ImageIO.read(Path.of(sheetPath).toFile());
        } catch (IOException e){
            throw new IllegalArgumentException("Unable to load sprite sheet: " + sheetPath, e);
        }
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
    }

    private String cacheKey(int col, int row) {
        return col + ":" + row;
    }
    private String framePath(int col, int row) {
        String key = cacheKey(col, row);
        if (cachedFramePaths.containsKey(key)) {
            return cachedFramePaths.get(key);
        }

        BufferedImage frame = sheet.getSubimage(col * frameWidth, row * frameHeight, frameWidth, frameHeight);
        try {
            Path temp = Files.createTempFile("spritesheet_frame_", ".png");
            ImageIO.write(frame, "png", temp.toFile());
            temp.toFile().deleteOnExit();
            cachedFramePaths.put(key, temp.toString());
            return temp.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to slice sprite sheet frame " + key, e);
        }
    }


    /**
     * Creates a TETile that renders the specific cell from given sprite sheet
     *
     * @param col column index in the sprite sheet grid (zero-index)
     * @param row row index in sprite sheet grid
     * @param description human-friendly description for tile
     * @return tile - backed by cropped frame from provided sprite sheet
     */
    public TETile tileAt(int col, int row, String description, int id) {
        String path = framePath(col, row);
        return new TETile(' ', new Color(240,234,214), new Color(46,38,33), description, path, id);
    }



}
