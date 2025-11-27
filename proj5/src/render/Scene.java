package render;

import tileengine.TETile;

import java.awt.Color;
import java.util.ArrayList;
import java.util.*;

/**
 * A renderable snapshot of the world. A {@code Scene} is built by the game logic
 * and handed to the renderer so drawing code does not need to know about domain
 * objects like NPCs or items. Sprites and overlays are organized into ordered
 * layers so the renderer can simply iterate and draw them front-to-back.
 */
public class Scene {
    /** Draw order for sprites and overlays. Layers earlier in the enum are drawn first. */
    public enum Layer {
        BACKGROUND,
        DECOR,
        ITEMS,
        ACTORS_BEHIND,
        PLAYER,
        ACTORS_FRONT,
        FOREGROUND,
        OVERLAY
    }

    /**
     * A single drawable tile at the given world coordinate. {@code tileSize}
     * is measured in tile units to support oversized sprites.
     */
    public record Sprite(TETile tile, double x, double y, double tileSize) {
        public Sprite {
            if (tile == null) {
                throw new IllegalArgumentException("Sprite tile cannot be null");
            }
        }
    }

    /**
     * A colored square overlay drawn on top of sprite layers. Useful for
     * lighting or highlight effects without coupling to the renderer.
     */
    public record Overlay(Color color, double x, double y, double tileSize) {
        public Overlay {
            if (color == null) {
                throw new IllegalArgumentException("Overlay color cannot be null");
            }
        }
    }

    private final Map<Layer, List<Sprite>> spritesByLayer = new EnumMap<>(Layer.class);
    private final Map<Layer, List<Overlay>> overlaysByLayer = new EnumMap<>(Layer.class);

    public Scene() {
        for (Layer layer : Layer.values()) {
            spritesByLayer.put(layer, new ArrayList<>());
            overlaysByLayer.put(layer, new ArrayList<>());
        }
    }

    public void addSprite(Layer layer, TETile tile, double x, double y) {
        addSprite(layer, tile, x, y, 1.0);
    }

    public void addSprite(Layer layer, TETile tile, double x, double y, double tileSize) {
        spritesByLayer.get(layer).add(new Sprite(tile, x, y, tileSize));
    }

    public void addOverlay(Layer layer, Color color, double x, double y, double tileSize) {
        overlaysByLayer.get(layer).add(new Overlay(color, x, y, tileSize));
    }

    public List<Sprite> sprites(Layer layer) {
        return Collections.unmodifiableList(spritesByLayer.get(layer));
    }

    public List<Overlay> overlays(Layer layer) {
        return Collections.unmodifiableList(overlaysByLayer.get(layer));
    }

    public List<Layer> drawOrder() {
        return Collections.unmodifiableList(Arrays.asList(Layer.values()));
    }
}