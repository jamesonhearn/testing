package core;
import edu.princeton.cs.algs4.StdDraw;
import tileengine.Tileset;
import tileengine.TERenderer;
import tileengine.TETile;
import utils.FileUtils;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.Locale;

public class Engine {
    public static final int WIDTH = World.WIDTH;
    public static final int HEIGHT = World.HEIGHT;
    public static final int HUD_HEIGHT = 3;
    public static final String SAVE_FILE = "save.txt";

    private final TERenderer ter = new TERenderer();
    private TETile[][] world;
    private Position avatar;
    private TETile avatarSprite;
    private StringBuilder history;


    // Movement variables
    private boolean wDown, aDown, sDown, dDown;
    private char currentDirection = 0;
    private boolean shiftDown = false;



    //Animation variables
    private int ticksSinceLastMove = 0;
    private int animFrame = 0; // Assign anim frame to cycle through character pngs
    private int MAX_FRAMES = 8;
    private static final int WALK_REPEAT_TICKS = 5;
    private static final int RUN_REPEAT_TICKS = 3;   // ~2× faster

    private static final int WALK_ANIM_TICKS = 2;
    private static final int RUN_ANIM_TICKS = 1;     // faster animation

    private int animTick = 0;

    // Added smoothing to animations
    private double drawX =0, drawY = 0;
    private static final double SMOOTH_SPEED = 0.40;

    public Engine() {
        reset();
    }

    public void interactWithKeyboard() {
        ter.initialize(WIDTH, HEIGHT + HUD_HEIGHT);
        showMainMenu();
        char selection = waitForMenuSelection();
        if (selection == 'q') {
            System.exit(0);
        } else if (selection == 'l') {
            loadGame();
            if (world == null) {
                promptSeedAndStart();
            }
            gameLoop();
        } else {
            promptSeedAndStart();
            gameLoop();
        }
    }


    public TETile[][] interactWithInputString(String input) {
        reset();
        applyCommands(input.toLowerCase(Locale.ROOT), true, false);
        return worldWithAvatar();
    }

    private void reset() {
        world = null;
        avatar = null;
        history = new StringBuilder();
    }
    private void showMainMenu() {
        StdDraw.clear(Color.BLACK);
        StdDraw.setPenColor(Color.WHITE);
        StdDraw.text(WIDTH/2,HEIGHT/2 + 3, "BYOW");
        StdDraw.text(WIDTH/2,HEIGHT/2 + 1, "N - New World");
        StdDraw.text(WIDTH/2,HEIGHT/2, "L - Load");
        StdDraw.text(WIDTH/2,HEIGHT/2  - 1, "Q - Quit");
        StdDraw.show();
    }

    private char waitForMenuSelection() {
        while (true) {
            if (StdDraw.hasNextKeyTyped()) {
                char c = Character.toLowerCase(StdDraw.nextKeyTyped());
                if (c == 'n' || c == 'l' || c == 'q') {
                    return c;
                }
            }
            StdDraw.pause(20);
        }
    }


    private void promptSeedAndStart() {
        StringBuilder seedBuilder = new StringBuilder();
        while (true) {
            StdDraw.clear(Color.BLACK);
            StdDraw.setPenColor(Color.WHITE);
            StdDraw.text(WIDTH / 2, HEIGHT / 2 + 2, "Enter Seed, then press S");
            StdDraw.text(WIDTH / 2, HEIGHT / 2, seedBuilder.toString());
            StdDraw.show();


            if (!StdDraw.hasNextKeyTyped()) {
                StdDraw.pause(15);
                continue;
            }
            char c = StdDraw.nextKeyTyped();
            if (c == 'S' || c == 's') {
                history.append('n').append(seedBuilder).append('s');
                startNewWorld(parseSeed(seedBuilder.toString()));
                return;
            }
            if (Character.isDigit(c)) {
                seedBuilder.append(c);
            }
        }
    }

    private void gameLoop() {
        final int TICK_MS = 15; // create ticks to create consistent movements - hard coded ms delays caused bad inputs. 15ms per frame, but move N ticks

        while (true) {
            renderWithHud();

            while (StdDraw.hasNextKeyTyped()) {
                char c = Character.toLowerCase(StdDraw.nextKeyTyped());
                if (processCommand(c, true, true)) {
                    return;
                }
            }

            handleMovementRealtime(true);
            StdDraw.pause(TICK_MS);
            if (currentDirection != 0) {
                animTick++;

                int animSpeed = shiftDown ? RUN_ANIM_TICKS : WALK_ANIM_TICKS;

                if (animTick >= animSpeed) {
                    animFrame = (animFrame + 1) % MAX_FRAMES;
                    animTick = 0;
                }
            } else {
                // Optional: reset to idle frame when not moving
                //animFrame = 0; - seems to have broken press based moves, no anim change
                animTick = 0;
            }

//            if (!StdDraw.hasNextKeyTyped()) {
//                StdDraw.pause(15);
//                continue;
//            }
//            char c = StdDraw.nextKeyTyped();
//            if (processCommand(Character.toLowerCase(c), true, true)) {
//                return; //save and exit
//            }
        }
    }


    private void renderWithHud() {
        StdDraw.clear(Color.BLACK);
        ter.setAvatarPosition(avatar.x, avatar.y);
        ter.drawBaseTiles(world);
        drawAvatar();
        ter.drawFrontTiles(world);
        drawHud();
        StdDraw.show();
    }

    private void drawHud() {
        StdDraw.setPenColor(Color.WHITE);
        double hudY = HEIGHT + 1.5;
        StdDraw.textLeft(1, hudY, tileUnderMouse());
    }

    private String tileUnderMouse() {
        int x = (int) StdDraw.mouseX();
        int y = (int) StdDraw.mouseY();
        if (x >= 0 && x < WIDTH && y >=0 && y<  HEIGHT && world != null) {
            if (avatar != null && avatar.x == x && avatar.y == y) {
                return avatarSprite.description();
            }
            return world[x][y].description();
        }
        return "";

    }

    // applyCommands for loading saves
    private void applyCommands(String input, boolean recordHistory, boolean allowQuit) {
        boolean awaitingQuit = false;
        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);
            if (awaitingQuit) {
                if (c == 'q') {
                    saveHistory();
                    if (allowQuit) {
                        System.exit(0);
                    }
                    return;
                }
                awaitingQuit = false;
            }


            switch (c) {
                case 'n':
                    int start = i + 1;
                    int end = start;
                    while (end < input.length() && Character.isDigit(input.charAt(end))) {
                        end += 1;
                    }
                    if (end >= input.length() || input.charAt(end) != 's') {
                        return; // seed not of correct form
                    }
                    String seedStr = input.substring(start, end);
                    if (recordHistory) {
                        history.append('n').append(seedStr).append('s');
                    }
                    startNewWorld(parseSeed(seedStr));
                    i=end;
                    break;
                case 'l':
                    loadGame();
                    break;
                case 'w':
                case 'a':
                case 's':
                case 'd':
                    if (world != null) {
                        if (recordHistory) {
                            history.append(c);
                        }
                        moveAvatar(c);
                    }
                    break;
                case ':':
                    awaitingQuit = true;
                    break;
                default:
                    break;
            }
            i+=1;
        }
    }


    // This one is a bit of a mess
    private void handleMovementRealtime(boolean record) {
        // check if shift down and assign T/F for each directional val
        shiftDown = StdDraw.isKeyPressed(KeyEvent.VK_SHIFT);
        boolean w = StdDraw.isKeyPressed(KeyEvent.VK_W);
        boolean a = StdDraw.isKeyPressed(KeyEvent.VK_A);
        boolean s = StdDraw.isKeyPressed(KeyEvent.VK_S);
        boolean d = StdDraw.isKeyPressed(KeyEvent.VK_D);

        // Check if any key pressed, used to reset direction
        boolean anyDown = w || a || s || d;

        // check if press or press and hold (Down vars jut recheck keyEvent)
        boolean wJust = w && !wDown;
        boolean aJust = a && !aDown;
        boolean sJust = s && !sDown;
        boolean dJust = d && !dDown;

        // Detect single key presses and move immediately
        if (wJust) currentDirection = 'w';
        if (aJust) currentDirection = 'a';
        if (sJust) currentDirection = 's';
        if (dJust) currentDirection = 'd';

        // Update current direction when keys are released - use bools to find fallback direction
        if (!w && currentDirection == 'w') currentDirection = fallbackDirection(w,a,s,d);
        if (!a && currentDirection == 'a') currentDirection = fallbackDirection(w,a,s,d);
        if (!s && currentDirection == 's') currentDirection = fallbackDirection(w,a,s,d);
        if (!d && currentDirection == 'd') currentDirection = fallbackDirection(w,a,s,d);

        // Clear direction if no keys are pressed
        if (!anyDown) {
            currentDirection = 0;
            ticksSinceLastMove = 0;
        } else if (currentDirection != 0 && world != null) {
            if (wJust || aJust || sJust || dJust) {
                // New key press: move immediately

                moveAvatar(currentDirection);
                if (record) history.append(currentDirection);
                ticksSinceLastMove = 0;
            } else {
                // Key held: move based on walk/run speed
                ticksSinceLastMove++;
                int speedTicks = shiftDown ? RUN_REPEAT_TICKS : WALK_REPEAT_TICKS;

                if (ticksSinceLastMove >= speedTicks) {
                    moveAvatar(currentDirection);
                    if (record) history.append(currentDirection);
                    ticksSinceLastMove = 0;
                }
            }
        }

        // Update previous key states
        wDown = w;
        aDown = a;
        sDown = s;
        dDown = d;
    }

    // Allow for return to prior direction on multi key movements
    private char fallbackDirection(boolean w, boolean a, boolean s, boolean d) {
        if (w) return 'w';
        if (a) return 'a';
        if (s) return 's';
        if (d) return 'd';
        return 0;
    }

    // Checks for System commands (save/quit)
    private boolean processCommand(char command, boolean record, boolean allowQuit) {
        if (command == ':') {
            while (true) {
                if (StdDraw.hasNextKeyTyped()) {
                    char next = Character.toLowerCase(StdDraw.nextKeyTyped());
                    if (next == 'q') {
                        saveHistory();
                        if (allowQuit) {
                            System.exit(0);
                        }
                        return true;
                    } else {
                        return false;
                    }
                }
                StdDraw.pause(15);
            }
        }
        if (command == 'w' || command == 's' || command == 'a' || command == 'd'){
            return false;
        }
        applyCommands(String.valueOf(command), record, allowQuit);
        return false;
    }


    // Generator func via seed - drop player
    private void startNewWorld(long seed) {
        World generator = new World(seed);
        world = generator.generate();
        placeAvatar();
    }

    // Find first coordiate that is valid placement for player on spawn - just seeks from bottom right currently
    // Eventually include ladder/elevator placement
    private void placeAvatar() {
        for (int x = 0; x < WIDTH; x+=1) {
            for (int y =0; y < HEIGHT; y+=1) {
                if (world[x][y].equals(Tileset.FLOOR)) {
                    avatar = new Position(x,y);
                    avatarSprite = Tileset.AVATAR_DOWN_FRAMES[0];
                    // Snap the smoothed draw coordinates to the spawn tile so the avatar
                    // doesn't glide in from (0,0) on the first frame.
                    drawX = avatar.x;
                    drawY = avatar.y;
                    return;
                }
            }
        }
    }



    // Depending on direction, update avatar position and rotate sprite animation frame
    // validate that canEnter (is FLOOR)
    private void moveAvatar(char direction) {
        Position target = avatar;
        switch (direction) {
            case 'w':
                target = new Position(avatar.x, avatar.y + 1);
                avatarSprite = Tileset.AVATAR_UP_FRAMES[animFrame];
                break;
            case 'a':
                target = new Position(avatar.x -1, avatar.y);
                avatarSprite = Tileset.AVATAR_LEFT_FRAMES[animFrame];
                break;
            case 's':
                target = new Position(avatar.x, avatar.y - 1);
                avatarSprite = Tileset.AVATAR_DOWN_FRAMES[animFrame];
                break;
            case 'd':
                target = new Position(avatar.x + 1, avatar.y);
                avatarSprite = Tileset.AVATAR_RIGHT_FRAMES[animFrame];
                break;
            default:
                break;
        }
        if (canEnter(target)) {
            avatar = target;
        }
    }

    // True iff valid world position and is FLOOR tile
    private boolean canEnter(Position pos) {
        if (pos.x < 0 || pos.x >= WIDTH || pos.y < 0 || pos.y >= HEIGHT) {
            return false;
        }
        return world[pos.x][pos.y].equals(Tileset.FLOOR);
    }

    //Load game via save file if exists, restores state via applyCommands
    private void loadGame() {
        if (!FileUtils.fileExists(SAVE_FILE)) {
            return;
        }
        String saved = FileUtils.readFile(SAVE_FILE).toLowerCase(Locale.ROOT);
        history = new StringBuilder(saved);
        applyCommands(saved, false, false);
    }

    // Basic save func
    private void saveHistory() {
        FileUtils.writeFile(SAVE_FILE, history.toString());
    }


    // seed parser for Menu
    private long parseSeed(String seedDigits) {
        try {
            return Long.parseLong(seedDigits);
        } catch (NumberFormatException e) {
            return 0L; // returns 0 in Long form
        }
    }

    //Avatar now uses smoothing - placement happens instantly but movement is based on frames
    private void drawAvatar() {
        if (avatar != null && avatarSprite != null) {
            // When movement stops, snap to the target tile to avoid post-input sliding.
            drawX += (avatar.x - drawX) * SMOOTH_SPEED;
            drawY += (avatar.y - drawY) * SMOOTH_SPEED;
            double avatarScale = 2;   // adjust this number as desired (0.3–0.6 looks good)
            avatarSprite.drawScaled(drawX, drawY, avatarScale);
        }
    }


    private TETile[][] worldWithAvatar() {
        if (world == null || avatar == null || avatarSprite == null) {
            return world;
        }
        TETile[][] copy = TETile.copyOf(world);
        copy[avatar.x][avatar.y] = avatarSprite;
        return copy;
    }


    private record Position(int x, int y) {
    }


}
