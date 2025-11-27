package tileengine;

import core.NPC.Corpse;
import core.NPC.Npc;
import edu.princeton.cs.algs4.StdDraw;

import java.awt.*;
import java.lang.reflect.Field;

import core.NPC.NpcManager;
import core.items.DroppedItem;

import javax.swing.*;
import java.util.List;

/**
 * Utility class for rendering tiles. You do not need to modify this file. You're welcome
 * to, but be careful. We strongly recommend getting everything else working before
 * messing with this renderer, unless you're trying to do something fancy like
 * allowing scrolling of the screen or tracking the avatar or something similar.
 */
public class TERenderer {
    static final int TILE_SIZE = 32;
    // Keep NPC sprites aligned to a single tile so their visual footprint matches the collision
    // grid even as TILE_SIZE (zoom) changes.
    private static final double NPC_SCALE_TILES = 1.0;
    //Canvas sizes
    private int width;
    private int height;

    //Screen offset values
    private int xOffset;
    private int yOffset;



    //How much of map is visible within user window
    private int viewWidth;
    private int viewHeight;

    //Space for HUD on top of screen
    private int hudHeight;

    //Actual full size of world
    private int worldWidth;
    private int worldHeight;

    //Bottom left tile of the visible region within screen
    private int viewOriginX;
    private int viewOriginY;

    //Smoothing factor for camera transitions (how much change per frame)
    private static final double CAMERA_SMOOTH = 0.20;

    private static final double SMOOTH_SPEED = 0.10;

    // Keep NPC sprites aligned to a single tile so their visual footprint matches the collision
    // grid even as TILE_SIZE (zoom) changes.

    /**
     * Immutable snapshot of the visible window for the current frame. Computing the
     * bounds once lets multiple render passes (tiles, lighting, overlays) reuse the
     * same extents without repeating clamp math and keeps deferred draw queues that
     * preserve depth ordering.
     */
    public static final class RenderContext {
        final int startX;
        final int endX;
        final int startY;
        final int endY;
        final LightBounds litBounds;
        final java.util.List<TileDraw> frontTiles = new java.util.ArrayList<>();

        RenderContext(int startX, int endX, int startY, int endY, LightBounds litBounds) {
            this.startX = startX;
            this.endX = endX;
            this.startY = startY;
            this.endY = endY;
            this.litBounds = litBounds;
        }

        boolean contains(int x, int y) {
            return x >= startX && x < endX && y >= startY && y < endY;
        }

        boolean withinLightWindow(int x, int y) {
            return x >= litBounds.startX && x < litBounds.endX
                    && y >= litBounds.startY && y < litBounds.endY;
        }
    }

    private record TileDraw(int x, int y, TETile tile) { }
    private record LightBounds(int startX, int endX, int startY, int endY) { }

    // Camera fields - used to smoothly transition camera location when avatar
    // nears edges of map
    private double camTileX;
    private double camTileY;

    // Avatar tile coordinate
    private int avatarX = -1;
    private int avatarY = -1;


    //Radius of visible light circle around player
    private double lightRadius = 6;   // tunable
    public static final TETile DARK =
            new TETile(' ', new Color(0,0,0), new Color(0,0,0), "darkness", 2);


    private boolean isLit(int x, int y) {
        double dx = x - avatarX;
        double dy = y - avatarY;
        return dx * dx + dy * dy <= lightRadius * lightRadius;
    }
    public void setLightRadius(double r) {
        this.lightRadius = r;
    }


    // Move avatar instantly - replace avatar x and Y with new position
    // Recenter camera on top of avatar
    public void setAvatarPosition(int x, int y) {
        this.avatarX = x;
        this.avatarY = y;
        if (viewWidth > 0 && viewHeight > 0) {
            camTileX = avatarX - viewWidth / 2.0;
            camTileY = avatarY - viewHeight / 2.0;

            viewOriginX = clamp((int) Math.round(camTileX), 0, worldWidth - viewWidth);
            viewOriginY = clamp((int) Math.round(camTileY), 0, worldHeight - viewHeight);
        }
    }

    // If camera is not on avatar, set goal as avatar location and recenter
    public void updateCamera() {
        if (avatarX < 0 || avatarY < 0) return;
        if (worldWidth == 0 || worldHeight == 0) return;

        double targetX = avatarX - viewWidth / 2.0;
        double targetY = avatarY - viewHeight / 2.0;

        camTileX += (targetX - camTileX) * CAMERA_SMOOTH;
        camTileY += (targetY - camTileY) * CAMERA_SMOOTH;

        viewOriginX = clamp((int)Math.round(camTileX), 0, worldWidth - viewWidth);
        viewOriginY = clamp((int)Math.round(camTileY), 0, worldHeight - viewHeight);
    }
    public int getViewOriginX() {
        return viewOriginX;
    }

    public int getViewOriginY() {
        return viewOriginY;
    }

    public void configureView(int worldWidth, int worldHeight, int viewWidth, int viewHeight, int hudHeight) {
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.viewWidth = viewWidth;
        this.viewHeight = viewHeight;
        this.hudHeight = hudHeight;
    }



    /**
     * Compute the viewable bounds for the current camera position. Subsequent render
     * steps should pass this context around instead of recalculating ranges.
     */
    public RenderContext buildContext(TETile[][] world) {
        int startX = Math.max(0, viewOriginX);
        int endX = Math.min(world.length, viewOriginX + viewWidth);

        int startY = Math.max(0, viewOriginY);
        int endY = Math.min(world[0].length, viewOriginY + viewHeight);

        LightBounds litBounds = litBounds(startX, endX, startY, endY);
        return new RenderContext(startX, endX, startY, endY, litBounds);
    }



    private int clamp(int value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    //Is some tiles coords inside of the region between origin and width/height
    private boolean inView(int x, int y) {
        return x >= viewOriginX && x < viewOriginX + viewWidth
                && y >= viewOriginY && y < viewOriginY + viewHeight;
    }


    // Maps world tile coords to on screen tile coords
    public double toScreenX(double worldX) {
        return (worldX - viewOriginX) + xOffset;
    }

    public double toScreenY(double worldY) {
        return (worldY - viewOriginY) + yOffset;
    }


    // Apparently StdDraw doesnt directly expose this, but you can pull it via
    // the declared field in the Class - here we are just explicitly defining a
    // custom frame that sits centered relative to user window
    public static void centerStdDraw() {
        try {
            Field f = StdDraw.class.getDeclaredField("frame");
            f.setAccessible(true);
            JFrame frame = (JFrame) f.get(null);

            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            int x = (screen.width - frame.getWidth()) / 2;
            int y = (screen.height - frame.getHeight()) / 2;
            frame.setLocation(x, y);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Same functionality as the other initialization method. The only difference is that the xOff
     * and yOff parameters will change where the renderFrame method starts drawing. For example,
     * if you select w = 60, h = 30, xOff = 3, yOff = 4 and then call renderFrame with a
     * TETile[50][25] array, the renderer will leave 3 tiles blank on the left, 7 tiles blank
     * on the right, 4 tiles blank on the bottom, and 1 tile blank on the top.
     * @param w width of the window in tiles
     * @param h height of the window in tiles.
     */
    public void initialize(int w, int h, int xOff, int yOff) {
        this.width = w;
        this.height = h;
        this.xOffset = xOff;
        this.yOffset = yOff;

        if (viewWidth == 0) {
            viewWidth = w;
        }
        if (viewHeight == 0) {
            viewHeight = h - hudHeight;
        }


        StdDraw.setCanvasSize(width * TILE_SIZE, height * TILE_SIZE);
        centerStdDraw();
        resetFont();
        StdDraw.setXscale(xOffset, xOffset + width);
        StdDraw.setYscale(yOffset, yOffset + height);
        StdDraw.clear(new Color(0, 0, 0));

        StdDraw.enableDoubleBuffering();
        StdDraw.show();
    }

    /**
     * Initializes StdDraw parameters and launches the StdDraw window. w and h are the
     * width and height of the world in number of tiles. If the TETile[][] array that you
     * pass to renderFrame is smaller than this, then extra blank space will be left
     * on the right and top edges of the frame. For example, if you select w = 60 and
     * h = 30, this method will create a 60 tile wide by 30 tile tall window. If
     * you then subsequently call renderFrame with a TETile[50][25] array, it will
     * leave 10 tiles blank on the right side and 5 tiles blank on the top side. If
     * you want to leave extra space on the left or bottom instead, use the other
     * initializatiom method.
     * @param w width of the window in tiles
     * @param h height of the window in tiles.
     */
    public void initialize(int w, int h) {
        initialize(w, h, 0, 0);
    }

    /**
     * Takes in a 2d array of TETile objects and renders the 2d array to the screen, starting from
     * xOffset and yOffset.
     *
     * If the array is an NxM array, then the element displayed at positions would be as follows,
     * given in units of tiles.
     *
     *              positions   xOffset |xOffset+1|xOffset+2| .... |xOffset+world.length
     *
     * startY+world[0].length   [0][M-1] | [1][M-1] | [2][M-1] | .... | [N-1][M-1]
     *                    ...    ......  |  ......  |  ......  | .... | ......
     *               startY+2    [0][2]  |  [1][2]  |  [2][2]  | .... | [N-1][2]
     *               startY+1    [0][1]  |  [1][1]  |  [2][1]  | .... | [N-1][1]
     *                 startY    [0][0]  |  [1][0]  |  [2][0]  | .... | [N-1][0]
     *
     * By varying xOffset, yOffset, and the size of the screen when initialized, you can leave
     * empty space in different places to leave room for other information, such as a GUI.
     * This method assumes that the xScale and yScale have been set such that the max x
     * value is the width of the screen in tiles, and the max y value is the height of
     * the screen in tiles.
     * @param world the 2D TETile[][] array to render
     */
    // No longer used - split rendering into 2 phases to create depth
    public void renderFrame(TETile[][] world) {
        StdDraw.clear(new Color(0, 0, 0));
        drawTiles(world);
        StdDraw.show();
    }

    // Bresenhams line algo for LOS lighting
    private boolean isOccluded(int x2, int y2, TETile[][] world){
        int x1 = avatarX;
        int y1 = avatarY;


        // get directional distances
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);

        // are we going Left/Right Up/Down
        int sx = x1 < x2? 1 : -1;
        int sy = y1 < y2? 1 : -1;
        // course correction
        int err = dx - dy;

        while (true) {
            if (x1 == x2 && y1 == y2) return false; //base case
            if (world[x1][y1] == Tileset.WALL_TOP) return true; // hit  wall - occlude beyond
            int e2 = 2 * err;

            // check if veering right/left more than up/down
            if (e2 > -dy) {
                err -= dy;
                x1 += sx;
            }

            if (e2 < dx) {
                err += dx;
                y1 += sy;
            }

        }
    }

    public void applyFullLightingPass(TETile[][] world, RenderContext context) {
        for (int x = context.startX; x < context.endX; x++) {
            for (int y = context.startY; y < context.endY; y++) {
                applyLightingMask(world, x, y);
            }
        }
    }


    // Apply lighting mask to restrict player visibility to circular ring around player avatar
    private void applyLightingMask(TETile[][] world, int x, int y) {

        double dx = x - avatarX;
        double dy = y - avatarY;
        double dist = Math.sqrt(dx*dx + dy*dy); // basic trig to get tile distance

        double radius = lightRadius; // define based on distance wanting to see
        // fully dark
        if (dist >= radius + 1.0) {
            StdDraw.setPenColor(0, 0, 0);
            StdDraw.filledSquare(toScreenX(x) + 0.5, toScreenY(y) + 0.5, 0.5);
            return;
        }

        if (isOccluded(x, y, world)) {
            StdDraw.setPenColor(0, 0, 0); // Occlude beyond wall
            StdDraw.filledSquare(toScreenX(x) + 0.5, toScreenY(y) + 0.5, 0.5);
            return;
        }

        // fully lit
        if (dist <= radius) {
            return;
        }

        // partial fade (0 to 1)
        double fade = dist - radius;   // 0.0 → 1.0
        fade = Math.min(1.0, Math.max(0.0, fade));

        int brightness = (int) (50 * (1.0 - fade)); // 0 (black) to 50 (dim)

        StdDraw.setPenColor(brightness, brightness, brightness);
        StdDraw.filledSquare(toScreenX(x) + 0.5, toScreenY(y) + 0.5, 0.5);
    }



    /**
     * Draws all world tiles without clearing the canvas or showing the tiles.
     * @param world the 2D TETile[][] array to render
     */
    // Not used
    public void drawTiles(TETile[][] world) {
        RenderContext context = buildContext(world);
        drawBaseTiles(world, context);
        drawFrontTiles(context);
    }

    public void drawNpcsBack(TETile[][] world, NpcManager npcManager, RenderContext context) {
        if (npcManager == null) {
            return;
        }
        for (Npc npc : npcManager.npcs()) {
            if (!context.withinLightWindow(npc.x(), npc.y())) {
                continue;
            }

            if (npc.y() > avatarY) {
                npc.updateSmooth(SMOOTH_SPEED);
                npc.currentTile().drawScaled(toScreenX(npc.drawX()), toScreenY(npc.drawY()), 2.0);
                redrawCoverWalls(world, npc.x(), npc.y());
            }
        }
    }
    public void drawNpcsFront(TETile[][] world, NpcManager npcManager, RenderContext context) {
        if (npcManager == null) {
            return;
        }
        for (Npc npc : npcManager.npcs()) {
            if (!context.withinLightWindow(npc.x(), npc.y())) {
                continue;
            }
            if (npc.y() <= avatarY) {
                npc.updateSmooth(SMOOTH_SPEED);
                npc.currentTile().drawScaled(toScreenX(npc.drawX()), toScreenY(npc.drawY()), 2.0);
            }
        }
    }

    public void drawDroppedItems(List<DroppedItem> drops, RenderContext context) {
        if (drops == null) {
            return;
        }
        for (DroppedItem drop : drops) {
            if (!context.withinLightWindow(drop.x(), drop.y())) {
                continue;
            }
            Tileset.LOOT_BAG.drawSized(toScreenX(drop.x()), toScreenY(drop.y()), 1.0);
        }
    }

    public void drawCorpses(List<Corpse> corpses, RenderContext context) {
        if (corpses == null) {
            return;
        }
        for (Corpse corpse : corpses) {
            if (!context.withinLightWindow(corpse.x(), corpse.y())) {
                continue;
            }
            corpse.tile().drawSized(toScreenX(corpse.x()), toScreenY(corpse.y()), 1.0);
        }
    }



    // If behind avatar and standable, render (
    public void drawBaseTiles(TETile[][] world, RenderContext context) {
        LightBounds bounds = context.litBounds;
        for (int x = bounds.startX; x < bounds.endX; x++) {
            for (int y = bounds.startY; y < bounds.endY; y++) {
                TETile tile = world[x][y];

                if (tile == null) {
                    throw new IllegalArgumentException("Tile at " + x + "," + y + " is null.");
                }
                // draw non-wall tiles now
                // draw walls behind the avatar now
                if (isFloor(tile)){
                    tile.drawSized(toScreenX(x), toScreenY(y), 1.0);
                } else if (isTopWall(tile) && y > avatarY) {
                    tile.drawSized(toScreenX(x), toScreenY(y), 1.0);
                }
                else {
                    context.frontTiles.add(new TileDraw(x, y, tile));
                }
            }
        }
    }

    // If in front of avatar and not a floor/standable tile, render (makes sure floors dont render above player)
    public void drawFrontTiles(RenderContext context) {
        for (TileDraw draw : context.frontTiles) {
            draw.tile().drawSized(toScreenX(draw.x()), toScreenY(draw.y()), 1.0);
        }
    }


    private LightBounds litBounds(int viewStartX, int viewEndX, int viewStartY, int viewEndY) {
        // pad the light radius by one tile to keep gradient and occlusion ring intact
        int radius = Math.max(1, (int) Math.ceil(lightRadius + 1.0));

        int startX = Math.max(viewStartX, avatarX - radius);
        int endX = Math.min(viewEndX, avatarX + radius + 1);

        int startY = Math.max(viewStartY, avatarY - radius);
        int endY = Math.min(viewEndY, avatarY + radius + 1);

        return new LightBounds(startX, endX, startY, endY);
    }

    // Minimal redraw to occlude tall NPC sprites when they overlap walls above them.
    private void redrawCoverWalls(TETile[][] world, int npcX, int npcY) {
        int numXTiles = world.length;
        int numYTiles = world[0].length;

        for (int dx = -1; dx <= 1; dx++) {
            int x = npcX + dx;

            for (int dy = -1; dy <= 1; dy++) {
                int y = npcY + dy;

                if (x < 0 || x >= numXTiles || y < 0 || y >= numYTiles) {
                    continue;
                }

                TETile tile = world[x][y];
                if (tile == null) {
                    throw new IllegalArgumentException("Tile at " + x + "," + y + " is null.");
                }

                if (!inView(x,y)) {
                    continue;
                }

                if (isTopWall(tile) && y > avatarY) {
                    tile.drawSized(toScreenX(x), toScreenY(y), 1.0);
                }
            }
        }
    }
    private boolean isFloor(TETile t) {
        return t == Tileset.FLOOR || t == Tileset.ELEVATOR;
    }

    private boolean isSideWall(TETile t) {
        return t == Tileset.LEFT_WALL
                || t == Tileset.RIGHT_WALL
                || t == Tileset.WALL_SIDE;
    }

    private boolean isTopWall(TETile t) {
        return t == Tileset.WALL_TOP
                || t == Tileset.FRONT_WALL_TOP
                || t == Tileset.BACK_WALL;
    }

    private boolean isWall(TETile t) {
        return isSideWall(t) || isTopWall(t);
    }
    /**
     * Resets the font to default settings. You should call this method before drawing any tiles
     * if you changed the pen settings.
     */
    public void resetFont() {
        Font font = new Font("Monaco", Font.BOLD, TILE_SIZE - 2);
        StdDraw.setFont(font);
    }
}