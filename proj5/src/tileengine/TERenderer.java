package tileengine;

import core.NPC.Npc;
import edu.princeton.cs.algs4.StdDraw;

import java.awt.*;
import java.lang.reflect.Field;

import tileengine.TETile;
import core.NPC.NpcManager;

import javax.swing.*;



/**
 * Utility class for rendering tiles. You do not need to modify this file. You're welcome
 * to, but be careful. We strongly recommend getting everything else working before
 * messing with this renderer, unless you're trying to do something fancy like
 * allowing scrolling of the screen or tracking the avatar or something similar.
 */
public class TERenderer {
    static final int TILE_SIZE = 24;
    private int width;
    private int height;
    private int xOffset;
    private int yOffset;



    //Trying camera centering
    private int viewWidth;
    private int viewHeight;
    private int hudHeight;
    private int worldWidth;
    private int worldHeight;
    private int viewOriginX;
    private int viewOriginY;

    private double cameraX = -1;
    private double cameraY = -1;
    private static final double CAMERA_SMOOTH = 0.20;

    private static final double SMOOTH_SPEED = 0.10;




    private int avatarX = -1;
    private int avatarY = -1;


    private double lightRadius = 8;   // tunable
    public static final TETile DARK =
            new TETile(' ', new Color(0,0,0), new Color(0,0,0), "darkness", 2);


    private boolean isLit(int x, int y) {
        double dx = x - avatarX;
        double dy = y - avatarY;
        return dx * dx + dy * dy <= lightRadius * lightRadius;
    }

    public void setAvatarPosition(int x, int y) {
        this.avatarX = x;
        this.avatarY = y;
        recenterOnAvatar();
    }

    // If camera is not on avatar, set goal as avatar location and recenter
    public void updateCamera() {
        if (avatarX < 0 || avatarY < 0) {
            return;
        }

        if (cameraX < 0 || cameraY < 0) {
            cameraX = avatarX;
            cameraY = avatarY;
        } else {
            cameraX += (avatarX - cameraX);
            cameraY += (avatarY - cameraY);
        }

        recenterOnAvatar();
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



    public void setLightRadius(double r) {
        this.lightRadius = r;
    }



    private void recenterOnAvatar() {
        if (viewWidth == 0 || viewHeight == 0 || worldWidth == 0 || worldHeight == 0) {
            return;
        }

        int halfViewWidth = viewWidth / 2;
        int halfViewHeight = viewHeight / 2;

        viewOriginX = clamp(avatarX - halfViewWidth, 0, Math.max(0, worldWidth - viewWidth));
        viewOriginY = clamp(avatarY - halfViewHeight, 0, Math.max(0, worldHeight - viewHeight));
    }

    private int clamp(int value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    private boolean inView(int x, int y) {
        return x >= viewOriginX && x < viewOriginX + viewWidth
                && y >= viewOriginY && y < viewOriginY + viewHeight;
    }


    public double toScreenX(double worldX) {
        double halfViewWidth = viewWidth / 2.0;
        return worldX - cameraX + halfViewWidth + xOffset;

        //double sx = worldX - cameraX + halfViewWidth + xOffset;
        //return Math.floor(sx*1.0001);
    }

    public double toScreenY(double worldY) {
        double halfViewHeight = viewHeight / 2.0;
        return worldY - cameraY + halfViewHeight + yOffset;
        //double sy = worldY - cameraY + halfViewHeight + yOffset;
        //return Math.floor(sy*1.0001);
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
        StdDraw.setXscale(0, width);
        StdDraw.setYscale(0, height);

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
    // trying to use bresenhams line algo for LOS
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

    public void applyFullLightingPass(TETile[][] world) {
        int startX = Math.max(0, viewOriginX);
        int endX = Math.min(world.length, viewOriginX + viewWidth);
        int startY = Math.max(0, viewOriginY);
        int endY = Math.min(world[0].length, viewOriginY + viewHeight);

        for (int x = startX; x < endX; x++) {
            for (int y = startY; y < endY; y++) {
                applyLightingMask(world, x, y);
            }
        }
    }


    // Apply lighting mask to restrict player visibility to circular ring around player avatar
    private void applyLightingMask(TETile[][] world, int x, int y) {
        if (isOccluded(x,y, world)) {
            StdDraw.setPenColor(0,0,0); // Occlude beyond wall
            StdDraw.filledSquare(toScreenX(x) + 0.5, toScreenY(y) + 0.5, 0.5);
            return;
        }

        double dx = x - avatarX;
        double dy = y - avatarY;
        double dist = Math.sqrt(dx*dx + dy*dy); // basic trig to get tile distance

        double radius = lightRadius; // define based on distance wanting to see

        // fully lit
        if (dist <= radius) {
            return;
        }

        // fully dark
        if (dist >= radius + 1.0) {
            StdDraw.setPenColor(0, 0, 0);
            StdDraw.filledSquare(toScreenX(x) + 0.5, toScreenY(y) + 0.5, 0.5);
            return;
        }

        // partial fade (0 to 1)
        double fade = dist - radius;   // 0.0 → 1.0
        fade = Math.min(1.0, Math.max(0.0, fade));

        int brightness = (int)(50 * (1.0 - fade)); // 0 (black) to 50 (dim)

        StdDraw.setPenColor(brightness, brightness, brightness);
        StdDraw.filledSquare(toScreenX(x) + 0.5, toScreenY(y) + 0.5, 0.5);
    }



    /**
     * Draws all world tiles without clearing the canvas or showing the tiles.
     * @param world the 2D TETile[][] array to render
     */
    // Not used
    public void drawTiles(TETile[][] world) {
        drawBaseTiles(world);
        drawFrontTiles(world);
    }

    public void drawNpcsBack(TETile[][] world, NpcManager npcManager) {
        if (npcManager == null) {
            return;
        }
        for (Npc npc : npcManager.npcs()) {
            if (!inView(npc.x(), npc.y())) {
                continue;
            }
            if (npc.y() > avatarY) {
                npc.updateSmooth(SMOOTH_SPEED);
                npc.currentTile().drawScaled(toScreenX(npc.x()), toScreenY(npc.y()), 4.0);
                redrawCoverWalls(world, npc.x(), npc.y());
            }
        }
    }
    public void drawNpcsFront(TETile[][] world, NpcManager npcManager) {
        if (npcManager == null) {
            return;
        }
        for (Npc npc : npcManager.npcs()) {
            if (npc.y() <= avatarY) {
                npc.updateSmooth(SMOOTH_SPEED);
                npc.currentTile().drawScaled(toScreenX(npc.x()), toScreenY(npc.y()), 4.0);
            }
        }
    }



    // If behind avatar and standable, render (
    public void drawBaseTiles(TETile[][] world) {
        int numXTiles = world.length;
        int numYTiles = world[0].length;

        int startX = Math.max(0, viewOriginX);
        int endX = Math.min(world.length, viewOriginX + viewWidth);

        int startY = Math.max(0, viewOriginY);
        int endY = Math.min(world[0].length, viewOriginY + viewHeight);

        for (int x = startX; x < endX; x++) {
            for (int y = startY; y < endY; y++) {
                TETile tile = world[x][y];

                if (tile == null) {
                    throw new IllegalArgumentException("Tile at " + x + "," + y + " is null.");
                }

                boolean wall = isWall(tile);

                // draw non-wall tiles now
                // draw walls behind the avatar now
                if (!wall || y > avatarY) {
                    tile.drawSized(toScreenX(x), toScreenY(y), 1.0);
//                    if (isLit(x, y)) {
//                        tile.drawSized(x + xOffset, y + yOffset, 1.0);
//                    } else {
//                        DARK.drawSized(x + xOffset, y + yOffset, 1.0);
//                    }
                }
            }
        }
    }

    // If in front of avatar and not a floor/standable tile, render (makes sure floors dont render above player)
    public void drawFrontTiles(TETile[][] world) {
        int numXTiles = world.length;
        int numYTiles = world[0].length;

        int startX = Math.max(0, viewOriginX);
        int endX = Math.min(world.length, viewOriginX + viewWidth);

        int startY = Math.max(0, viewOriginY);
        int endY = Math.min(world[0].length, viewOriginY + viewHeight);

        for (int x = startX; x < endX; x++) {
            for (int y = startY; y < endY; y++) {
                TETile tile = world[x][y];

                if (tile == null) {
                    throw new IllegalArgumentException("Tile at " + x + "," + y + " is null.");
                }

                if (isWall(tile) && y <= avatarY) {
                    tile.drawSized(toScreenX(x), toScreenY(y), 1.0);
//                    if (isLit(x, y)) {
//                        tile.drawSized(x + xOffset, y + yOffset, 1.0);
//                    } else {
//                        DARK.drawSized(x + xOffset, y + yOffset, 1.0);
//                    }
                }
            }
        }
    }

    // Blocks movement on non-standable tiles
    private boolean isWall(TETile tile) {

        if (tile == null) return true;
        String d = tile.description();
        if (d.equals("you") || d.equals("slime") || d.equals("spherevis")){
            return false;
        }


        return tile != Tileset.FLOOR
                && tile != Tileset.ELEVATOR;
//        tile == Tileset.FRONT_WALL ||
//                tile == Tileset.BACK_WALL ||
//                tile == Tileset.LEFT_WALL ||
//                tile == Tileset.RIGHT_WALL ||
//                tile == Tileset.WALL_TOP ||
//                tile == Tileset.ELEVATOR ||
//                tile == Tileset.WALL_SIDE ||
//                tile == Tileset.FRONT_WALL_TOP;
    }
    private boolean isFrontLayer(TETile tile) {
        return tile == Tileset.FRONT_WALL
                || tile == Tileset.FRONT_WALL_TOP
                || tile == Tileset.LEFT_WALL
                || tile == Tileset.RIGHT_WALL;
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

    private boolean isTopWall(TETile tile) {
        return tile == Tileset.WALL_TOP || tile == Tileset.FRONT_WALL_TOP;
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