package tileengine;

import core.NPC.Npc;
import edu.princeton.cs.algs4.StdDraw;

import java.awt.Color;
import java.awt.Font;
import tileengine.TETile;
import core.NPC.NpcManager;


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


    private static final double SMOOTH_SPEED = 0.40;


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
    }

    public void setLightRadius(double r) {
        this.lightRadius = r;
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
        StdDraw.setCanvasSize(width * TILE_SIZE, height * TILE_SIZE);
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
        int numXTiles = world.length;
        int numYTiles = world[0].length;

        for (int x = 0; x < numXTiles; x++) {
            for (int y = 0; y < numYTiles; y++) {
                applyLightingMask(world, x, y);
            }
        }
    }


    // Apply lighting mask to restrict player visibility to circular ring around player avatar
    private void applyLightingMask(TETile[][] world, int x, int y) {
        if (isOccluded(x,y, world)) {
            StdDraw.setPenColor(0,0,0); // Occlude beyond wall
            StdDraw.filledSquare(x + xOffset + 0.5, y+yOffset+0.5, 0.5);
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
            StdDraw.filledSquare(x + xOffset + 0.5, y + yOffset + 0.5, 0.5);
            return;
        }

        // partial fade (0 to 1)
        double fade = dist - radius;   // 0.0 → 1.0
        fade = Math.min(1.0, Math.max(0.0, fade));

        int brightness = (int)(50 * (1.0 - fade)); // 0 (black) to 50 (dim)

        StdDraw.setPenColor(brightness, brightness, brightness);
        StdDraw.filledSquare(x + xOffset + 0.5, y + yOffset + 0.5, 0.5);
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
            if (npc.y() > avatarY) {
                npc.updateSmooth(SMOOTH_SPEED);
                npc.currentTile().drawScaled(npc.x(), npc.y(), 2.0);
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
                npc.currentTile().drawScaled(npc.x(), npc.y(), 2.0);
            }
        }
    }



    // If behind avatar and standable, render (
    public void drawBaseTiles(TETile[][] world) {
        int numXTiles = world.length;
        int numYTiles = world[0].length;

        for (int x = 0; x < numXTiles; x++) {
            for (int y = 0; y < numYTiles; y++) {
                TETile tile = world[x][y];

                if (tile == null) {
                    throw new IllegalArgumentException("Tile at " + x + "," + y + " is null.");
                }

                boolean wall = isWall(tile);

                // draw non-wall tiles now
                // draw walls behind the avatar now
                if (!wall || y > avatarY) {
                    tile.drawSized(x + xOffset, y + yOffset, 1.0);
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

        for (int x = 0; x < numXTiles; x++) {
            for (int y = 0; y < numYTiles; y++) {
                TETile tile = world[x][y];

                if (tile == null) {
                    throw new IllegalArgumentException("Tile at " + x + "," + y + " is null.");
                }

                if (isWall(tile) && y <= avatarY) {
                    tile.drawSized(x + xOffset, y + yOffset, 1.0);
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

    /**
     * Resets the font to default settings. You should call this method before drawing any tiles
     * if you changed the pen settings.
     */
    public void resetFont() {
        Font font = new Font("Monaco", Font.BOLD, TILE_SIZE - 2);
        StdDraw.setFont(font);
    }
}
