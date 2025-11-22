package core;
import edu.princeton.cs.algs4.StdDraw;
import tileengine.Tileset;
import tileengine.TERenderer;
import tileengine.TETile;
import utils.FileUtils;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Locale;

import java.util.Random;

import core.NPC.Npc;
import core.NPC.NpcManager;



public class Engine {
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int screenWidth = (int) screenSize.getWidth();
    int screenHeight = (int) screenSize.getHeight();

    public static final int WORLD_WIDTH = World.WIDTH;
    public static final int WORLD_HEIGHT = World.HEIGHT;

    private final int VIEW_WIDTH = screenWidth / 24;
    private final int VIEW_HEIGHT = screenHeight / 24;
    public static final int HUD_HEIGHT = 3;
    public static final String SAVE_FILE = "save.txt";

    private final TERenderer ter = new TERenderer();
    private TETile[][] world;
    private Position avatar;
    private TETile avatarSprite;
    private StringBuilder history;
    private NpcManager npcManager;



    // AUDIO STUFF
    private final AudioPlayer music = new AudioPlayer();


    // Movement variables
    private boolean wDown, aDown, sDown, dDown;
    private char currentDirection = 0;
    private boolean shiftDown = false;



    //Animation variables
    private int ticksSinceLastMove = 0;
    private int animFrame = 0; // Assign anim frame to cycle through character pngs
    private int MAX_FRAMES = 8;
    private static final int WALK_REPEAT_TICKS = 2;
    private static final int RUN_REPEAT_TICKS = 1;   // ~2× faster

    private static final int AVATAR_ANIM_MS = 40;    // frame change cadence while moving
    private long lastAnimUpdateMs = 0L;
    private char lastFacing = 's';


    private static final long NPC_SEED_SALT = 0x9e3779b97f4a7c15L;


    // Added smoothing to animations
    private double drawX =0, drawY = 0;
    private static final double SMOOTH_SPEED = 0.40;

    public Engine() {
        music.loadEffects(
                "assets/audio/step1.wav",
                "assets/audio/step2.wav",
                "assets/audio/step3.wav",
                "assets/audio/step4.wav",
                "assets/audio/step5.wav",
                "assets/audio/step6.wav",
                "assets/audio/step7.wav",
                "assets/audio/step8.wav",
                "assets/audio/step9.wav",
                "assets/audio/step10.wav",
                "assets/audio/step11.wav",
                "assets/audio/step12.wav",
                "assets/audio/step13.wav"
        );
        reset();
        ter.configureView(WORLD_WIDTH, WORLD_HEIGHT, VIEW_WIDTH, VIEW_HEIGHT, HUD_HEIGHT);
    }

    public void interactWithKeyboard() {
        ter.initialize(VIEW_WIDTH, VIEW_HEIGHT + HUD_HEIGHT);
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
        npcManager = null;
    }
    private void showMainMenu() {
        StdDraw.clear(Color.BLACK);
        StdDraw.setPenColor(Color.WHITE);
        StdDraw.text(VIEW_WIDTH / 2.0, VIEW_HEIGHT / 2.0 + 3, "BYOW");
        StdDraw.text(VIEW_WIDTH / 2.0, VIEW_HEIGHT / 2.0 + 1, "N - New World");
        StdDraw.text(VIEW_WIDTH / 2.0, VIEW_HEIGHT / 2.0, "L - Load");
        StdDraw.text(VIEW_WIDTH / 2.0, VIEW_HEIGHT / 2.0  - 1, "Q - Quit");
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
            StdDraw.text(VIEW_WIDTH / 2.0, VIEW_HEIGHT / 2.0 + 2, "Enter Seed, then press S");
            StdDraw.text(VIEW_WIDTH / 2.0, VIEW_HEIGHT / 2.0, seedBuilder.toString());
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
        music.playLoop("assets/audio/load_in_or_safe_zone.wav"); // uncomment when you want to check music
        while (true) {
            renderWithHud();


            while (StdDraw.hasNextKeyTyped()) {
                char c = Character.toLowerCase(StdDraw.nextKeyTyped());
                if (processCommand(c, true, true)) {
                    return;
                }
            }

            handleMovementRealtime(true);
            if (npcManager != null && avatar != null) {
                npcManager.tick(world, avatar.x, avatar.y);
            }
            StdDraw.pause(TICK_MS);
            tickAvatarAnimation();


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
        ter.updateCamera();
        ter.drawBaseTiles(world);
        ter.drawNpcsBack(world, npcManager);
        drawAvatar();
        ter.drawNpcsFront(world, npcManager);
        ter.drawFrontTiles(world);
        ter.applyFullLightingPass(world);
        drawHud();
        StdDraw.show();
    }

    private void drawHud() {
        StdDraw.setPenColor(Color.WHITE);
        double hudY = VIEW_HEIGHT + 1.5;
        StdDraw.textLeft(1, hudY, tileUnderMouse());
    }

    private String tileUnderMouse() {
        int screenX = (int) StdDraw.mouseX();
        int screenY = (int) StdDraw.mouseY();

        if (screenX < 0 || screenX >= VIEW_WIDTH || screenY < 0 || screenY >= VIEW_HEIGHT) {
            return "";
        }

        int worldX = screenX + ter.getViewOriginX();
        int worldY = screenY + ter.getViewOriginY();

        if (world == null || worldX < 0 || worldX >= WORLD_WIDTH || worldY < 0 || worldY >= WORLD_HEIGHT) {
            return "";
        }

        if (npcManager != null) {
            for (Npc npc : npcManager.npcs()) {
                if (npc.x() == worldX && npc.y() == worldY) {
                    return npc.currentTile().description();
                }
            }
            if (avatar != null && avatar.x == worldX && avatar.y == worldY) {
                return avatarSprite.description();
            }
            return world[worldX][worldY].description();
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
                        music.stop();
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


                boolean moved = moveAvatar(currentDirection);
                if (record) history.append(currentDirection);
                if (moved) {
                    music.playRandomEffect();
                }
                ticksSinceLastMove = 0;
            } else {
                // Key held: move based on walk/run speed
                ticksSinceLastMove++;
                int speedTicks = shiftDown ? RUN_REPEAT_TICKS : WALK_REPEAT_TICKS;

                if (ticksSinceLastMove >= speedTicks) {
                    boolean moved = moveAvatar(currentDirection);
                    if (record) history.append(currentDirection);
                    if (moved) {
                        music.playRandomEffect();
                    }
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
        npcManager = new NpcManager(new Random(seed ^ NPC_SEED_SALT)); // golden ratio hash, allows nice NPC RNG relative to world RNG
        npcManager.spawn(world, avatar.x, avatar.y);
    }

    // Find first coordiate that is valid placement for player on spawn - just seeks from bottom right currently
    // Eventually include ladder/elevator placement
    private void placeAvatar() {
        for (int x = 0; x < WORLD_WIDTH; x+=1) {
            for (int y =0; y < WORLD_HEIGHT; y+=1) {
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
    private boolean moveAvatar(char direction) {
        Position target = avatar;
        lastFacing = direction;
        switch (direction) {
            case 'w':
                target = new Position(avatar.x, avatar.y + 1);
                break;
            case 'a':
                target = new Position(avatar.x -1, avatar.y);
                break;
            case 's':
                target = new Position(avatar.x, avatar.y - 1);
                break;
            case 'd':
                target = new Position(avatar.x + 1, avatar.y);
                break;
            default:
                break;
        }
        boolean moved = false;
        if (canEnter(target)) {
            avatar = target;
            moved = true;
        }
        refreshAvatarSprite();
        return moved;
    }

    // True iff valid world position and is FLOOR tile
    private boolean canEnter(Position pos) {
        if (pos.x < 0 || pos.x >= WORLD_WIDTH || pos.y < 0 || pos.y >= WORLD_HEIGHT) {
            return false;
        }
        if (npcManager != null && npcManager.isNpcAt(pos.x, pos.y)) {
            return false;
        }
        return world[pos.x][pos.y].equals(Tileset.FLOOR);
    }

    private void tickAvatarAnimation() {
        long now = System.currentTimeMillis();
        if (currentDirection != 0) {
            if (now - lastAnimUpdateMs >= AVATAR_ANIM_MS) {
                animFrame = (animFrame + 1) % MAX_FRAMES;
                lastAnimUpdateMs = now;
                refreshAvatarSprite();
            }
        } else {
            lastAnimUpdateMs = now;
        }
    }

    private void refreshAvatarSprite() {
        char facing = (currentDirection != 0) ? currentDirection : lastFacing;
        switch (facing) {
            case 'w' -> avatarSprite = Tileset.AVATAR_UP_FRAMES[animFrame];
            case 'a' -> avatarSprite = Tileset.AVATAR_LEFT_FRAMES[animFrame];
            case 'd' -> avatarSprite = Tileset.AVATAR_RIGHT_FRAMES[animFrame];
            default -> avatarSprite = Tileset.AVATAR_DOWN_FRAMES[animFrame];
        }
        lastFacing = facing;
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
            drawX = avatar.x;
            drawY = avatar.y;
//            drawX += (avatar.x - drawX) * SMOOTH_SPEED;
//            drawY += (avatar.y - drawY) * SMOOTH_SPEED;
            double avatarScale = 2;   // adjust this number as desired (0.3–0.6 looks good)
            double screenX = ter.toScreenX(drawX);
            double screenY = ter.toScreenY(drawY);
            avatarSprite.drawScaled(screenX, screenY, avatarScale);        }
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