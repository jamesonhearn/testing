package core;
import edu.princeton.cs.algs4.StdDraw;
import tileengine.Tileset;
import tileengine.TERenderer;
import tileengine.TETile;
import utils.FileUtils;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;

import java.util.Random;
import java.util.stream.Collectors;

import core.NPC.Npc;
import core.NPC.NpcManager;
import core.animation.AnimationCycle;
import core.items.DroppedItem;
import core.items.Inventory;
import core.items.Item;
import core.items.ItemRegistry;
import core.items.ItemStack;



public class Engine {
    public static final int MENU_POLL_MS = 20;
    public static final int INPUT_POLL_MS = 15;
    public static final double HUD_TEXT_OFFSET_TILES = 1.5;
    public static final int HEALTHBAR_DRAW_WIDTH = 30;
    public static final double HEALTH_THRESHOLD_75 = 0.75;
    public static final double HEALTH_THRESHOLD_50 = 0.50;
    public static final double HEALTH_THRESHOLD_25 = 0.25;
    public static final double INVENTORY_ROW_SPACING = 1.5;
    public static final int HEALTH_POTION_MESSAGE_MS = 2000;
    public static final int PLAYER_HEALTH = 50;
    public static final int INVULNERABILITY_FRAMES = 15;
    public static final int ITEM_DROP_RETRIES = 400;
    public static final int LIGHT_SURGE_MESSAGE_MS = 3000;
    public static final double RNG_20_PERCENT = 0.8;
    public static final double RNG_30_PERCENT = 0.7;
    public static final double RNG_95_PERCENT = 0.05;
    public static final int MS_PER_S = 1000;
    public static final int SEC_PER_MIN = 60;
    public static final int ATTACK_OFFSET = -2;
    public static final int DEFAULT_ALPHA = 170;

    public static final int WORLD_WIDTH = World.WIDTH;
    public static final int WORLD_HEIGHT = World.HEIGHT;

    private final int VIEW_WIDTH = 50; //screenWidth / 24;
    private final int VIEW_HEIGHT = 35; //screenHeight / 24;
    public static final int HUD_HEIGHT = 3;
    public static final String SAVE_FILE = "save.txt";

    private final TERenderer ter = new TERenderer();
    private TETile[][] world;
    private long worldSeed;
    private Avatar avatar;
    private TETile avatarSprite;
    private StringBuilder history;
    private NpcManager npcManager;
    private long npcSeed;
    private CombatService combatService;
    private long sessionStartMs;
    private long accumulatedPlayTimeMs;
    private long finalPlayTimeMs;
    private int enemiesFelled;
    private int totalDamageTaken;
    private int totalDamageGiven;

    // Inventory system stuffs
    private Inventory inventory;
    private List<DroppedItem> droppedItems;
    private boolean inventoryVisible;
    private String hudMessage;
    private boolean tabDown = false;
    private static final int DEFAULT_SLOT_COUNT = 16;

    // Lighting variables

    // Light decay system
    private static final double MAX_LIGHT_RADIUS = 9.0;      // starting radius
    private static final double MIN_LIGHT_RADIUS = 0.0;      // when reached, avatar dies
    private static final long LIGHT_DECAY_INTERVAL_MS = 10_000; // shrink every 30 seconds

    private long lastDecayTime = -1L;
    private double decayingLightRadius = MAX_LIGHT_RADIUS;

    private static final double BASE_LIGHT_RADIUS = MAX_LIGHT_RADIUS;
    private static final double SURGE_LIGHT_RADIUS = 12.0;
    private static final long LIGHT_SURGE_DURATION_MS = 10_000L;
    private static final long LIGHT_FADE_DURATION_MS = 3_000L;
    private static final long END_FADE_DURATION_MS = 3_000L;
    private long lightSurgeStartMs = -1L;


    // AUDIO STUFF
    private final AudioPlayer music = new AudioPlayer();

    private long hudMessageExpireMs = 0;

    // Movement variables
    private boolean wDown, aDown, sDown, dDown;
    private char currentDirection = 0;
    private boolean shiftDown = false;


    private static final String HB_FULL = "assets/ui/healthbar_full.png";
    private static final String HB_75   = "assets/ui/healthbar_75.png";
    private static final String HB_50   = "assets/ui/healthbar_50.png";
    private static final String HB_25   = "assets/ui/healthbar_25.png";
    private static final String HB_ZERO = "assets/ui/healthbar_empty.png";
    private static final double HUD_MARGIN_TILES = 0.5;

    private static final int TICK_MS = 30; // create ticks to create consistent movements


    /** Half-size of the avatar collision box in tile units (smaller than a full tile). */
    private static final double AVATAR_HITBOX_HALF = 0.24;
    /** Half-size of the NPC collision box in tile units (smaller than a full tile). */
    private static final double NPC_HITBOX_HALF = 0.30;
    /** Offset the avatar toward the entry edge when squeezing past an NPC. */
    private static final double HUG_EDGE_OFFSET = 0.5 - AVATAR_HITBOX_HALF - 0.02;


    //Animation variables
    private int ticksSinceLastMove = 0;
    private static final int WALK_REPEAT_TICKS = 2;
    private static final int RUN_REPEAT_TICKS = 1;   // ~2× faster
    private static final int AVATAR_WALK_TICKS = Math.max(1, (int) Math.round(40.0 / TICK_MS));
    private static final int AVATAR_RUN_TICKS = Math.max(1, AVATAR_WALK_TICKS - 1);
    private static final int AVATAR_ATTACK_TICKS = Math.max(1, (int) Math.round(60.0 / TICK_MS));
    private static final int AVATAR_ATTACK_DAMAGE = 1;
    private static final int AVATAR_DEATH_TICKS = Math.max(1, (int) Math.round(80.0 / TICK_MS));

    private final EnumMap<AvatarAction, EnumMap<Direction, AnimationCycle>> avatarAnimations =
            new EnumMap<>(AvatarAction.class);
    private AnimationCycle avatarAnimation;
    private AvatarAction avatarAction = AvatarAction.IDLE;
    private char lastFacing = 's';
    private boolean attackDown = false;
    private boolean attackInProgress = false;
    private boolean attackQueued = false;
    private Direction attackFacing = Direction.DOWN;

    private enum GameState { PLAYING, DYING, DEAD, ENDING, ENDED }
    private GameState gameState = GameState.PLAYING;
    private double endFadeStartRadius = BASE_LIGHT_RADIUS;
    private long endFadeStartMs = -1L;


    private static final long NPC_SEED_SALT = 0x9e3779b97f4a7c15L;


    // Added smoothing to animations
    private double drawX = 0, drawY = 0;
    private double avatarOffsetX = 0.0;
    private double avatarOffsetY = 0.0;

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
            boolean loaded = loadGame();
            if (!loaded || world == null) {
                music.stop();
                promptSeedAndStart();
            }
            gameLoop();
        } else {
            promptSeedAndStart();
            gameLoop();
        }
    }


    public void interactWithInputString(String input) {
        reset();
        applyCommands(input.toLowerCase(Locale.ROOT), true, false);
        worldWithAvatar();
    }

    private void reset() {
        world = null;
        worldSeed = 0L;
        avatar = null;
        history = new StringBuilder();
        npcManager = null;
        npcSeed = 0L;
        combatService = new CombatService();
        combatService.setDamageListener(this::recordDamageStats);
        avatarAnimations.clear();
        avatarAnimation = null;
        avatarAction = AvatarAction.IDLE;
        currentDirection = 0;
        attackInProgress = false;
        attackQueued = false;
        attackDown = false;
        gameState = GameState.PLAYING;
        sessionStartMs = 0L;
        accumulatedPlayTimeMs = 0L;
        finalPlayTimeMs = 0L;
        enemiesFelled = 0;
        totalDamageTaken = 0;
        totalDamageGiven = 0;
        endFadeStartRadius = BASE_LIGHT_RADIUS;
        endFadeStartMs = -1L;
        avatarOffsetX = 0.0;
        avatarOffsetY = 0.0;
        drawX = 0.0;
        drawY = 0.0;
        //Reset inventory
        inventory = new Inventory(DEFAULT_SLOT_COUNT);
        droppedItems = new ArrayList<>();
        inventoryVisible = false;
        hudMessage = "";
        resetLighting();

    }

    private void resetLighting() {
        lightSurgeStartMs = -1L;
        ter.setLightRadius(BASE_LIGHT_RADIUS);
    }

    private void triggerLightSurge() {
        lightSurgeStartMs = System.currentTimeMillis();
        ter.setLightRadius(SURGE_LIGHT_RADIUS);
    }

    private void updateLightingRadius() {
        if (lightSurgeStartMs < 0) {
            return;
        }

        long now = System.currentTimeMillis();
        long elapsed = now - lightSurgeStartMs;

        if (elapsed <= LIGHT_SURGE_DURATION_MS) {
            ter.setLightRadius(SURGE_LIGHT_RADIUS);
            return;
        }

        if (elapsed <= LIGHT_SURGE_DURATION_MS + LIGHT_FADE_DURATION_MS) {
            double fadeProgress = (double) (elapsed - LIGHT_SURGE_DURATION_MS) / LIGHT_FADE_DURATION_MS;
            double radius = SURGE_LIGHT_RADIUS - (SURGE_LIGHT_RADIUS - BASE_LIGHT_RADIUS) * fadeProgress;
            ter.setLightRadius(radius);
            return;
        }

        lightSurgeStartMs = -1L;

        // Snap rendering radius back to base
        ter.setLightRadius(BASE_LIGHT_RADIUS);

        //  Correct the decaying state
        decayingLightRadius = BASE_LIGHT_RADIUS;
        lastDecayTime = System.currentTimeMillis();
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
        music.playThenCallback("assets/audio/cavegame.wav", () -> music.playLoop("assets/audio/main_menu.wav"));
        while (true) {

            if (StdDraw.hasNextKeyTyped()) {
                char c = Character.toLowerCase(StdDraw.nextKeyTyped());
                if (c == 'n' || c == 'l' || c == 'q') {
                    return c;
                }
            }
            StdDraw.pause(MENU_POLL_MS);
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
                StdDraw.pause(INPUT_POLL_MS);
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
        music.playLoop("assets/audio/spookycave.wav"); // uncomment when you want to check music
        while (true) {

            switch (gameState) {
                case DYING, DEAD -> {
                    runDeathSequence();
                    continue;
                }
                case ENDING, ENDED -> {
                    runEndSequence();
                    continue;
                }
                default -> { } // fall through
            }
            updateHudMessage();
            renderWithHud();


            while (StdDraw.hasNextKeyTyped()) {
                char raw = StdDraw.nextKeyTyped();
                char c = Character.toLowerCase(raw);
                if (processCommand(c, true, true)) {
                    return;
                }
            }
            updateInventoryToggle();

            boolean avatarMoved = handleMovementRealtime(true);
            if (npcManager != null && avatar != null) {
                npcManager.tick(world, avatar);
            }
            combatService.tick();
            checkForEndgame();
            updateLightDecay();
            StdDraw.pause(TICK_MS);
            tickAvatarAnimation(avatarMoved);
        }
    }


    //primary method for overlaying world
    private void renderWithHud() {
        StdDraw.clear(Color.BLACK);
        if (lightSurgeStartMs >= 0) {
            updateLightingRadius();
        }
        ter.setAvatarPosition(avatar.x, avatar.y);
        ter.updateCamera();
        TERenderer.RenderContext context = ter.buildContext(world);
        ter.drawBaseTiles(world, context);
        ter.drawCorpses(npcManager == null ? null : npcManager.corpses(), context);
        ter.drawDroppedItems(droppedItems, context);
        ter.drawNpcsBack(world, npcManager, context);
        drawAvatar();
        ter.drawNpcsFront(world, npcManager, context);
        ter.drawFrontTiles(context);
        ter.applyFullLightingPass(world, context);
        drawHud();
        drawInventoryOverlay();
        if (gameState == GameState.DEAD) {
            drawDeathOverlay();
        }
        if (gameState == GameState.ENDED) {
            drawEndOverlay();
        }
        StdDraw.show();
    }

    //Draw hud (just a bar at the top that displays tile under mouse
    private void drawHud() {
        StdDraw.setPenColor(Color.WHITE);
        double hudY = VIEW_HEIGHT + HUD_TEXT_OFFSET_TILES;


        double barWidth = HEALTHBAR_DRAW_WIDTH;
        double barHeight = HEALTHBAR_DRAW_WIDTH / 3.0;

        double hbX = HUD_MARGIN_TILES + barWidth / 2.0;
        double hbY = VIEW_HEIGHT + HUD_HEIGHT - (barHeight / 2.0) - HUD_MARGIN_TILES * 2;
        String healthBarImage;
        double pct = 0.0;

        if (avatar != null) {
            pct = (double) avatar.health().current() / avatar.health().max();
        }

        if (pct >= HEALTH_THRESHOLD_75) {
            healthBarImage = HB_FULL;
        } else if (pct >= HEALTH_THRESHOLD_50) {
            healthBarImage = HB_75;
        } else if (pct >= HEALTH_THRESHOLD_25) {
            healthBarImage = HB_50;
        } else if (pct > 0.0) {
            healthBarImage = HB_25;
        } else {
            healthBarImage = HB_ZERO;
        }

        StdDraw.picture(hbX, hbY, healthBarImage, barWidth, barHeight);

        // === existing HUD text stuff ===
        StdDraw.textLeft(1, hudY, tileUnderMouse());
        if (!hudMessage.isEmpty()) {
            StdDraw.textRight(VIEW_WIDTH - 1, hudY, hudMessage);
        }
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

    private void drawDeathOverlay() {
        StdDraw.setPenColor(new Color(0, 0, 0, DEFAULT_ALPHA));
        StdDraw.filledRectangle(VIEW_WIDTH / 2.0, VIEW_HEIGHT / 2.0, VIEW_WIDTH / 2.0, VIEW_HEIGHT / 2.0);
        StdDraw.setPenColor(Color.WHITE);
        StdDraw.text(VIEW_WIDTH / 2.0, VIEW_HEIGHT / 2.0 + 2, "Your light has been extinguished");
        StdDraw.text(VIEW_WIDTH / 2.0, VIEW_HEIGHT / 2.0, "N: New Game");
        StdDraw.text(VIEW_WIDTH / 2.0, VIEW_HEIGHT / 2.0 - 1, "L: Restore Save");
        StdDraw.text(VIEW_WIDTH / 2.0, VIEW_HEIGHT / 2.0 - 2, "Q: Quit");
    }

    private void drawEndOverlay() {
        StdDraw.setPenColor(new Color(0, 0, 0, DEFAULT_ALPHA));
        StdDraw.filledRectangle(VIEW_WIDTH / 2.0, VIEW_HEIGHT / 2.0, VIEW_WIDTH / 2.0, VIEW_HEIGHT / 2.0);
        StdDraw.setPenColor(Color.WHITE);
        double centerX = VIEW_WIDTH / 2.0;
        double centerY = VIEW_HEIGHT / 2.0 + 3;
        StdDraw.text(centerX, centerY, "The lightkeeper has escaped, the fire burns on");
        StdDraw.text(centerX, centerY - 2, "Escape time: " + formatDuration(finalPlayTimeMs));
        StdDraw.text(centerX, centerY - 3, "Enemies felled: " + enemiesFelled);
        StdDraw.text(centerX, centerY - 4, "Damage taken: " + totalDamageTaken);
        StdDraw.text(centerX, centerY - 5, "Damage given: " + totalDamageGiven);
        StdDraw.text(centerX, centerY - 7, "N: Play Again");
        StdDraw.text(centerX, centerY - 8, "Q: Quit");
    }

    private void drawInventoryOverlay() {
        if (!inventoryVisible) {
            return;
        }
        StdDraw.setPenColor(new Color(0, 0, 0, DEFAULT_ALPHA));
        StdDraw.filledRectangle(VIEW_WIDTH / 2.0, VIEW_HEIGHT / 2.0, VIEW_WIDTH / 2.0, VIEW_HEIGHT / 2.0);
        StdDraw.setPenColor(Color.WHITE);
        StdDraw.text(VIEW_WIDTH / 2.0, VIEW_HEIGHT - 2, "Inventory (press I to close)");

        double startY = VIEW_HEIGHT - 4;
        int index = 0;
        for (ItemStack stack : inventory.nonEmptySlots()) {
            double y = startY - index * INVENTORY_ROW_SPACING;
            if (y < HUD_HEIGHT) {
                break;
            }
            StdDraw.textLeft(2, y, stack.toString());
            index += 1;
        }
        if (index == 0) {
            StdDraw.textLeft(2, startY, "(empty)");
        }
    }


    private boolean inventoryHasItem(Item item) {
        if (inventory == null || item == null) {
            return false;
        }
        for (ItemStack stack : inventory.nonEmptySlots()) {
            if (stack.item().equals(item) && stack.quantity() > 0) {
                return true;
            }
        }
        return false;
    }
    private boolean useHealthPotion() {
        if (gameState != GameState.PLAYING || avatar == null || inventory == null) {
            return false;
        }
        if (!inventory.remove(ItemRegistry.SMALL_POTION, 1)) {
            return false;
        }
        avatar.health().restoreFull();
        setHudMessage("Used Small Potion", HEALTH_POTION_MESSAGE_MS);
        return true;
    }

    private void updateInventoryToggle() {
        boolean tab = StdDraw.isKeyPressed(KeyEvent.VK_V);

        // Edge-trigger: only toggle when Tab goes from up -> down
        if (tab && !tabDown) {
            inventoryVisible = !inventoryVisible;
        }

        tabDown = tab;
    }


    // applyCommands for loading saves
    private void applyCommands(String input, boolean recordHistory, boolean allowQuit) {
        boolean awaitingQuit = false;
        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);
            if (awaitingQuit) {
                if (c == 'q') {
                    saveGameState();
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
                    i = end;
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
                        //moveAvatar(c);
                        boolean moved = moveAvatar(c);
                        if (moved) {
                            pickupAtAvatar();
                        }
                    }
                    break;
                case 'e':
                    pickupAtAvatar();
                    break;
                case 'r':
                    if (recordHistory) {
                        history.append(c);
                    }
                    useHealthPotion();
                    break;
                case ':':
                    awaitingQuit = true;
                    break;
                default:
                    break;
            }
            i += 1;
        }
    }
    private boolean handleMovementRealtime(boolean record) {
        shiftDown = StdDraw.isKeyPressed(KeyEvent.VK_SHIFT);
        boolean w = StdDraw.isKeyPressed(KeyEvent.VK_W);
        boolean a = StdDraw.isKeyPressed(KeyEvent.VK_A);
        boolean s = StdDraw.isKeyPressed(KeyEvent.VK_S);
        boolean d = StdDraw.isKeyPressed(KeyEvent.VK_D);
        boolean attack = StdDraw.isKeyPressed(KeyEvent.VK_SPACE);

        boolean anyDown = w || a || s || d;

        if (attack && !attackDown) {
            Direction facing = directionFromChar(
                    (currentDirection != 0) ? currentDirection : lastFacing);
            startAttack(facing);
        }

        updateDirectionOnPress(w, a, s, d);
        updateDirectionOnRelease(w, a, s, d);

        boolean movedThisTick = processMovement(record, anyDown, w, a, s, d);

        wDown = w;
        aDown = a;
        sDown = s;
        dDown = d;
        attackDown = attack;

        return movedThisTick;
    }

    private void updateDirectionOnPress(boolean w, boolean a, boolean s, boolean d) {
        if (w && !wDown) {
            currentDirection = 'w';
        }
        if (a && !aDown) {
            currentDirection = 'a';
        }
        if (s && !sDown) {
            currentDirection = 's';
        }
        if (d && !dDown) {
            currentDirection = 'd';
        }
    }
    private void updateDirectionOnRelease(boolean w, boolean a, boolean s, boolean d) {
        if (!w && currentDirection == 'w') {
            currentDirection = fallbackDirection(w, a, s, d);
        }
        if (!a && currentDirection == 'a') {
            currentDirection = fallbackDirection(w, a, s, d);
        }
        if (!s && currentDirection == 's') {
            currentDirection = fallbackDirection(w, a, s, d);
        }
        if (!d && currentDirection == 'd') {
            currentDirection = fallbackDirection(w, a, s, d);
        }
    }

    private boolean processMovement(boolean record,
                                    boolean anyDown,
                                    boolean w, boolean a, boolean s, boolean d) {

        if (!anyDown) {
            currentDirection = 0;
            ticksSinceLastMove = 0;
            return false;
        }

        if (currentDirection == 0 || world == null) {
            return false;
        }

        boolean freshPress = (w && !wDown)
                || (a && !aDown)
                || (s && !sDown)
                || (d && !dDown);

        if (freshPress) {
            ticksSinceLastMove = 0;
            return tryMove(record);
        }

        ticksSinceLastMove += 1;
        int speedTicks = shiftDown ? RUN_REPEAT_TICKS : WALK_REPEAT_TICKS;

        if (ticksSinceLastMove >= speedTicks) {
            ticksSinceLastMove = 0;
            return tryMove(record);
        }

        return false;
    }
    private boolean tryMove(boolean record) {
        boolean moved = moveAvatar(currentDirection);

        if (record) {
            history.append(currentDirection);
        }

        if (moved) {
            music.playRandomEffect();
            pickupAtAvatar();
        }

        return moved;
    }

    // Allow for return to prior direction on multi key movements
    private char fallbackDirection(boolean w, boolean a, boolean s, boolean d) {
        if (w) {
            return 'w';
        }
        if (a) {
            return 'a';
        }
        if (s) {
            return 's';
        }
        if (d) {
            return 'd';
        }
        return 0;
    }

    // Checks for System commands (save/quit)
    private boolean processCommand(char command, boolean record, boolean allowQuit) {
        if (command == ':') {
            while (true) {
                if (StdDraw.hasNextKeyTyped()) {
                    char next = Character.toLowerCase(StdDraw.nextKeyTyped());
                    if (next == 'q') {
                        saveGameState();
                        if (allowQuit) {
                            System.exit(0);
                        }
                        return true;
                    } else {
                        return false;
                    }
                }
                StdDraw.pause(INPUT_POLL_MS);
            }
        }
        if (command == 'e') {
            pickupAtAvatar();
            return false;
        }
        if (command == 'r') {
            useHealthPotion();
            return false;
        }
        if (command == 'w' || command == 's' || command == 'a' || command == 'd') {
            return false;
        }
        applyCommands(String.valueOf(command), record, allowQuit);
        return false;
    }


    // Generator func via seed - drop player
    private void startNewWorld(long seed) {
        worldSeed = seed;
        sessionStartMs = System.currentTimeMillis();
        accumulatedPlayTimeMs = 0L;
        finalPlayTimeMs = 0L;
        World generator = new World(seed);
        world = generator.generate();
        resetLighting();
        decayingLightRadius = MAX_LIGHT_RADIUS;
        lastDecayTime = System.currentTimeMillis();
        ter.setLightRadius(decayingLightRadius);
        placeAvatar();
        npcSeed = seed ^ NPC_SEED_SALT; // golden ratio hash, allows nice NPC RNG relative to world RNG
        npcManager = new NpcManager(new Random(npcSeed), combatService);
        npcManager.setDeathHandler(this::handleNpcDeath);
        npcManager.spawn(world, avatar.x, avatar.y);
        // give initial items and random spawn ground loot
        seedInitialInventory();
        seedDroppedItems(new Random(seed));
    }

    // Find first coordiate that is valid placement for player on spawn - just seeks from bottom right currently
    // Eventually include ladder/elevator placement
    private void placeAvatar() {
        for (int x = 0; x < WORLD_WIDTH; x += 1) {
            for (int y = 0; y < WORLD_HEIGHT; y += 1) {
                if (world[x][y].equals(Tileset.FLOOR)) {
                    HealthComponent avatarHealth = new HealthComponent(PLAYER_HEALTH, PLAYER_HEALTH,
                            1, INVULNERABILITY_FRAMES);
                    avatarHealth.addDeathCallback(this::handleAvatarDeath);
                    avatar = new Avatar(x, y, 3, avatarHealth);
                    avatar.setSpawnPoint(new Entity.Position(x, y));
                    combatService.register(avatar);
                    initializeAvatarAnimations(Direction.DOWN);
                    // Snap the smoothed draw coordinates to the spawn tile so the avatar
                    // doesn't glide in from (0,0) on the first frame.
                    avatarOffsetX = 0.0;
                    avatarOffsetY = 0.0;
                    drawX = avatar.x;
                    drawY = avatar.y;
                    return;
                }
            }
        }
    }



    // Depending on direction, update avatar position and rotate sprite animation frame
    // validate that canEnter (is FLOOR)
    // validate that canEnter (is FLOOR)
    private boolean moveAvatar(char direction) {
        MovementPlan plan = planMove(direction);
        boolean moved = false;
        if (plan != null) {
            avatar.setPosition(plan.target().x(), plan.target().y());
            avatarOffsetX = plan.offsetX();
            avatarOffsetY = plan.offsetY();
            moved = true;
        }
        return moved;
    }
    private void startAttack(Direction facing) {
        if (avatar == null || attackInProgress) {
            return;
        }
        attackInProgress = true;
        attackQueued = true;
        attackFacing = facing;
        AnimationCycle attackCycle = avatarAnimations.get(AvatarAction.ATTACK).get(facing);
        attackCycle.restart();
        avatarAnimation = attackCycle;
        avatarAction = AvatarAction.ATTACK;
        avatarSprite = attackCycle.currentFrame();

        applyAttackDamage(facing);
    }

    private void applyAttackDamage(Direction facing) {
        if (npcManager == null) {
            return;
        }

        int ax = avatar.x;
        int ay = avatar.y;

        int[][] offsets = attackOffsetsFor(facing);
        if (offsets == null) {
            return;
        }

        for (int[] o : offsets) {
            int tx = ax + o[0];
            int ty = ay + o[1];
            npcManager.damageAtTile(tx, ty, avatar, AVATAR_ATTACK_DAMAGE);
        }

        npcManager.damageAtTile(ax, ay, avatar, AVATAR_ATTACK_DAMAGE);
    }
    private int[][] attackOffsetsFor(Direction facing) {
        switch (facing) {
            case UP:
                return new int[][]{
                        {-1, 1}, {0, 1}, {1, 1},
                        {-1, 2}, {0, 2}, {1, 2},
                        {-1, 0}, {1, 0}
                };

            case DOWN:
                return new int[][]{
                        {-1, -1}, {0, -1}, {1, -1},
                        {-1, ATTACK_OFFSET}, {0, ATTACK_OFFSET}, {1, ATTACK_OFFSET},
                        {-1, 0}, {1, 0}
                };

            case LEFT:
                return new int[][]{
                        {-1, -1}, {-1, 0}, {-1, 1},
                        {ATTACK_OFFSET, -1}, {ATTACK_OFFSET, 0}, {ATTACK_OFFSET, 1},
                        {0, -1}, {0, 1}
                };

            case RIGHT:
                return new int[][]{
                        {1, -1}, {1, 0}, {1, 1},
                        {2, -1}, {2, 0}, {2, 1},
                        {0, -1}, {0, 1}
                };

            default:
                return null;
        }
    }



    private record MovementPlan(Entity.Position target, double offsetX, double offsetY) { }


    private MovementPlan planMove(char direction) {
        Entity.Position target = avatar.position();
        lastFacing = direction;
        double offsetX = 0.0;
        double offsetY = 0.0;

        switch (direction) {
            case 'w':
                target = new Entity.Position(avatar.x, avatar.y + 1);
                break;
            case 'a':
                target = new Entity.Position(avatar.x - 1, avatar.y);
                break;
            case 's':
                target = new Entity.Position(avatar.x, avatar.y - 1);
                break;
            case 'd':
                target = new Entity.Position(avatar.x + 1, avatar.y);
                break;
            default:
                return null;
        }
        if (!isWalkableFloor(target)) {
            return null;
        }

        Npc blocking = npcManager == null ? null : npcManager.npcAtTile(target.x(), target.y());
        if (blocking != null) {
            // Hug the edge of the tile closest to the movement direction to avoid the NPC's body.
            offsetX = switch (direction) {
                case 'a' -> HUG_EDGE_OFFSET;
                case 'd' -> -HUG_EDGE_OFFSET;
                default -> 0.0;
            };
            offsetY = switch (direction) {
                case 's' -> -HUG_EDGE_OFFSET;
                case 'w' -> HUG_EDGE_OFFSET;
                default -> 0.0;
            };

            if (overlapsNpc(target, offsetX, offsetY, blocking)) {
                return null;
            }
        }

        // When slipping past an NPC, keep the avatar anchored toward the edge; otherwise center the hitbox.
        if (blocking == null) {
            offsetX = 0.0;
            offsetY = 0.0;
        }

        return new MovementPlan(target, offsetX, offsetY);
    }


    private void initializeAvatarAnimations(Direction facing) {
        avatarAnimations.clear();
        avatarAnimations.put(AvatarAction.IDLE, buildAvatarAnimations(
                singleFrame(Tileset.AVATAR_UP_FRAMES[0]),
                singleFrame(Tileset.AVATAR_DOWN_FRAMES[0]),
                singleFrame(Tileset.AVATAR_LEFT_FRAMES[0]),
                singleFrame(Tileset.AVATAR_RIGHT_FRAMES[0]),
                1,
                false));
        avatarAnimations.put(AvatarAction.WALK, buildAvatarAnimations(
                Tileset.AVATAR_UP_FRAMES, Tileset.AVATAR_DOWN_FRAMES,
                Tileset.AVATAR_LEFT_FRAMES, Tileset.AVATAR_RIGHT_FRAMES,
                AVATAR_WALK_TICKS,
                true));
        avatarAnimations.put(AvatarAction.RUN, buildAvatarAnimations(
                Tileset.AVATAR_UP_FRAMES, Tileset.AVATAR_DOWN_FRAMES,
                Tileset.AVATAR_LEFT_FRAMES, Tileset.AVATAR_RIGHT_FRAMES,
                AVATAR_RUN_TICKS,
                true));
        avatarAnimations.put(AvatarAction.ATTACK, buildAvatarAnimations(
                Tileset.AVATAR_ATTACK_UP_FRAMES, Tileset.AVATAR_ATTACK_DOWN_FRAMES,
                Tileset.AVATAR_ATTACK_LEFT_FRAMES, Tileset.AVATAR_ATTACK_RIGHT_FRAMES,
                AVATAR_ATTACK_TICKS,
                false));
        avatarAnimations.put(AvatarAction.DEATH, buildAvatarAnimations(
                Tileset.AVATAR_DEATH_UP_FRAMES, Tileset.AVATAR_DEATH_DOWN_FRAMES,
                Tileset.AVATAR_DEATH_LEFT_FRAMES, Tileset.AVATAR_DEATH_RIGHT_FRAMES,
                AVATAR_DEATH_TICKS,
                false));

        avatarAction = AvatarAction.IDLE;
        avatarAnimation = avatarAnimations.get(avatarAction).get(facing);
        avatarSprite = avatarAnimation.currentFrame();
        lastFacing = directionToChar(facing);
    }

    private EnumMap<Direction, AnimationCycle> buildAvatarAnimations(TETile[] up, TETile[] down,
                                                                     TETile[] left, TETile[] right,
                                                                     int ticksPerFrame,
                                                                     boolean loop) {
        EnumMap<Direction, AnimationCycle> map = new EnumMap<>(Direction.class);
        map.put(Direction.UP, new AnimationCycle(up, ticksPerFrame, loop));
        map.put(Direction.DOWN, new AnimationCycle(down, ticksPerFrame, loop));
        map.put(Direction.LEFT, new AnimationCycle(left, ticksPerFrame, loop));
        map.put(Direction.RIGHT, new AnimationCycle(right, ticksPerFrame, loop));
        return map;
    }

    private TETile[] singleFrame(TETile tile) {
        return new TETile[]{tile};
    }

    private Direction directionFromChar(char facing) {
        return switch (facing) {
            case 'w' -> Direction.UP;
            case 'a' -> Direction.LEFT;
            case 'd' -> Direction.RIGHT;
            default -> Direction.DOWN;
        };
    }

    private char directionToChar(Direction direction) {
        return switch (direction) {
            case UP -> 'w';
            case LEFT -> 'a';
            case RIGHT -> 'd';
            case DOWN -> 's';
        };
    }


    //starting inventory
    private void seedInitialInventory() {
        if (inventory == null) {
            inventory = new Inventory(DEFAULT_SLOT_COUNT);
        }
        inventory.add(ItemRegistry.SMALL_POTION, 2);
        inventory.add(ItemRegistry.TORCH, 1);
    }


    // Randmly place items around the map
    private void seedDroppedItems(Random random) {
        if (world == null || avatar == null) {
            return;
        }
        Item[] candidates = new Item[]{ItemRegistry.LIGHT_SHARD};
        int placed = 0;
        int attempts = 0;
        while (placed < 5 && attempts < ITEM_DROP_RETRIES) {
            int x = random.nextInt(WORLD_WIDTH);
            int y = random.nextInt(WORLD_HEIGHT);
            attempts += 1;
            if (!world[x][y].equals(Tileset.FLOOR) || (x == avatar.x && y == avatar.y)) {
                continue;
            }
            Item choice = candidates[placed % candidates.length];
            int qty = 1 + random.nextInt(Math.max(1, choice.getMaxStackSize() / 2));
            droppedItems.add(new DroppedItem(choice, qty, x, y));
            placed += 1;
        }
    }



    // Pickup item in front of avatar if room in inventory
    private void pickupAtAvatar() {
        if (avatar == null || droppedItems == null || inventory == null) {
            return;
        }
        List<DroppedItem> remaining = new ArrayList<>();
        boolean pickedSomething = false;
        for (DroppedItem drop : droppedItems) {
            if (drop.x() == avatar.x && drop.y() == avatar.y) {
                if (drop.item() == ItemRegistry.LIGHT_SHARD) {
                    triggerLightSurge();
                    pickedSomething = true;
                    setHudMessage("A burst of light surrounds you", LIGHT_SURGE_MESSAGE_MS);
                    decayingLightRadius = MAX_LIGHT_RADIUS;
                    lastDecayTime = System.currentTimeMillis();

                    // Also update renderer radius immediately
                    ter.setLightRadius(decayingLightRadius);
                    continue;
                }
                int leftover = inventory.add(drop.item(), drop.quantity());
                pickedSomething = true;
                if (leftover > 0) {
                    drop.setQuantity(leftover);
                    remaining.add(drop);
                    hudMessage = "Inventory full - left " + leftover + " " + drop.item().name();
                } else {
                    hudMessage = "Picked up " + drop.item().name();
                }
            } else {
                remaining.add(drop);
            }
        }
        if (!pickedSomething) {
            hudMessage = "";
        }
        droppedItems = remaining;
    }


    private void setHudMessage(String msg, long durationMs) {
        hudMessage = msg;
        hudMessageExpireMs = System.currentTimeMillis() + durationMs;
    }

    private void updateHudMessage() {
        if (!hudMessage.isEmpty() && System.currentTimeMillis() > hudMessageExpireMs) {
            hudMessage = "";
        }
    }

    // True iff valid world position and is FLOOR tile
    private boolean isWalkableFloor(Entity.Position pos) {
        if (pos.x() < 0 || pos.x() >= WORLD_WIDTH || pos.y() < 0 || pos.y() >= WORLD_HEIGHT) {
            return false;
        }
        return world[pos.x()][pos.y()].equals(Tileset.FLOOR);
    }


    private boolean overlapsNpc(Entity.Position target, double offsetX, double offsetY, Npc npc) {
        double avatarCenterX = target.x() + 0.5 + offsetX;
        double avatarCenterY = target.y() + 0.5 + offsetY;
        double npcCenterX = npc.x() + 0.5;
        double npcCenterY = npc.y() + 0.5;

        double dx = avatarCenterX - npcCenterX;
        double dy = avatarCenterY - npcCenterY;
        double minDistance = AVATAR_HITBOX_HALF + NPC_HITBOX_HALF;
        return Math.hypot(dx, dy) < minDistance;
    }

    private void handleAvatarDeath(Entity entity) {
        if (!(entity instanceof Avatar)) {
            return;
        }
        if (gameState != GameState.PLAYING) {
            return;
        }
        beginDeathSequence();
    }

    private void updateLightDecay() {
        if (lastDecayTime < 0) {
            return;
        }

        long now = System.currentTimeMillis();
        long elapsed = now - lastDecayTime;

        if (elapsed >= LIGHT_DECAY_INTERVAL_MS) {
            lastDecayTime = now;

            // shrink radius
            decayingLightRadius -= 1.0;
            if (decayingLightRadius < MIN_LIGHT_RADIUS) {
                decayingLightRadius = MIN_LIGHT_RADIUS;
            }

            ter.setLightRadius(decayingLightRadius);

            // Check death condition
            if (decayingLightRadius <= 1.0) {
                decayingLightRadius = 1.0;
                ter.setLightRadius(decayingLightRadius);
                hudMessage = "Your light has been extinguished";
                handleAvatarDeath(avatar);
            }
        }
    }

    private long currentPlayTimeMs() {
        long base = accumulatedPlayTimeMs;
        if (gameState == GameState.ENDING || gameState == GameState.ENDED) {
            return Math.max(base, finalPlayTimeMs);
        }
        if (sessionStartMs > 0) {
            base += System.currentTimeMillis() - sessionStartMs;
        }
        return base;
    }

    private void recordDamageStats(Entity target, Entity source, int attempted, int applied) {
        if (applied <= 0) {
            return;
        }
        if (target instanceof Avatar) {
            totalDamageTaken += applied;
        }
        if (source instanceof Avatar) {
            totalDamageGiven += applied;
        }
    }

    private void checkForEndgame() {
        if (gameState != GameState.PLAYING || world == null || avatar == null) {
            return;
        }
        if (!inventoryHasItem(ItemRegistry.KEY)) {
            return;
        }
        if (world[avatar.x][avatar.y] == Tileset.ELEVATOR) {
            beginEndSequence();
        }
    }

    private void handleNpcDeath(Npc npc) {
        Random rng = new Random();
        if (npc == null) {
            return;
        }
        enemiesFelled += 1;
        double r = rng.nextDouble();
        if (r > RNG_20_PERCENT) {
            droppedItems.add(new DroppedItem(ItemRegistry.LIGHT_SHARD, 1, npc.x(), npc.y()));
        }
        if (r <= RNG_20_PERCENT && RNG_30_PERCENT <= r) {
            droppedItems.add(new DroppedItem(ItemRegistry.SMALL_POTION, 1, npc.x(), npc.y()));
        }
        if (r < RNG_95_PERCENT && !inventoryHasItem(ItemRegistry.KEY)) {
            droppedItems.add(new DroppedItem(ItemRegistry.KEY, 1, npc.x(), npc.y()));
        }

    }

    private void beginDeathSequence() {
        gameState = GameState.DYING;
        currentDirection = 0;
        ticksSinceLastMove = 0;
        attackInProgress = false;
        attackQueued = false;
        attackDown = false;
        clampLightToDeathRadius();
        AvatarAction desired = AvatarAction.DEATH;
        Direction facing = directionFromChar(lastFacing);
        EnumMap<Direction, AnimationCycle> map = avatarAnimations.getOrDefault(desired,
                avatarAnimations.get(AvatarAction.IDLE));
        if (map != null) {
            AnimationCycle cycle = map.get(facing);
            if (cycle != null) {
                cycle.restart();
                avatarAnimation = cycle;
                avatarAction = desired;
                avatarSprite = avatarAnimation.currentFrame();
            }
        }
    }

    private void clampLightToDeathRadius() {
        if (decayingLightRadius > 1.0) {
            decayingLightRadius = 1.0;
            ter.setLightRadius(decayingLightRadius);
        }
    }
    private void beginEndSequence() {
        if (gameState != GameState.PLAYING) {
            return;
        }
        gameState = GameState.ENDING;
        endFadeStartRadius = Math.max(0.0, ter.getLightRadius());
        endFadeStartMs = System.currentTimeMillis();
        finalPlayTimeMs = currentPlayTimeMs();
    }

    private void runEndSequence() {
        updateHudMessage();
        if (gameState == GameState.ENDING) {
            long elapsed = System.currentTimeMillis() - endFadeStartMs;
            double progress = Math.min(1.0, (double) elapsed / END_FADE_DURATION_MS);
            double radius = Math.max(0.0, endFadeStartRadius * (1.0 - progress));
            decayingLightRadius = radius;
            ter.setLightRadius(radius);
            if (progress >= 1.0) {
                finalPlayTimeMs = accumulatedPlayTimeMs + (System.currentTimeMillis() - sessionStartMs);
                decayingLightRadius = 0.0;
                ter.setLightRadius(decayingLightRadius);
                gameState = GameState.ENDED;
            }
        } else {
            handleEndMenuInput();
        }
        renderWithHud();
        StdDraw.pause(TICK_MS);
        tickAvatarAnimation(false);
    }


    private void runDeathSequence() {
        updateHudMessage();
        renderWithHud();
        if (gameState == GameState.DYING && avatarAnimation == null) {
            gameState = GameState.DEAD;
        }
        if (gameState == GameState.DYING && avatarAnimation != null && avatarAnimation.isComplete()) {
            gameState = GameState.DEAD;
        }
        if (gameState == GameState.DEAD) {
            handleDeathMenuInput();
        }
        StdDraw.pause(TICK_MS);
        tickAvatarAnimation(false);
    }

    private void handleDeathMenuInput() {
        while (StdDraw.hasNextKeyTyped()) {
            char c = Character.toLowerCase(StdDraw.nextKeyTyped());
            if (c == 'q') {
                System.exit(0);
            }
            if (c == 'n') {
                startNewGameFromDeath();
                return;
            }
            if (c == 'l') {
                restoreSaveFromDeath();
                return;
            }
        }
    }

    private void handleEndMenuInput() {
        while (StdDraw.hasNextKeyTyped()) {
            char c = Character.toLowerCase(StdDraw.nextKeyTyped());
            if (c == 'q') {
                System.exit(0);
            }
            if (c == 'n') {
                startNewGameFromEnd();
                return;
            }
        }
    }

    private void startNewGameFromDeath() {
        reset();
        promptSeedAndStart();
        gameState = GameState.PLAYING;
    }
    private void startNewGameFromEnd() {
        reset();
        promptSeedAndStart();
        gameState = GameState.PLAYING;
    }

    private void restoreSaveFromDeath() {
        reset();
        boolean loaded = loadGame();
        if (!loaded || world == null) {
            promptSeedAndStart();
        }
        gameState = GameState.PLAYING;
    }


    private void tickAvatarAnimation(boolean movedThisTick) {
        if (avatarAnimation == null) {
            return;
        }

        if (attackInProgress && avatarAnimation.isComplete()) {
            attackInProgress = false;
            attackQueued = false;
        }

        Direction facing = attackInProgress
                ? attackFacing
                : directionFromChar((currentDirection != 0) ? currentDirection : lastFacing);
        AvatarAction desiredAction = (gameState == GameState.PLAYING)
                ? (attackInProgress
                ? AvatarAction.ATTACK
                : (currentDirection == 0)
                ? AvatarAction.IDLE
                : (shiftDown ? AvatarAction.RUN : AvatarAction.WALK))
                : AvatarAction.DEATH;

        EnumMap<Direction, AnimationCycle> byDirection = avatarAnimations.get(desiredAction);
        AnimationCycle selected = byDirection.get(facing);

        boolean actionChanged = desiredAction != avatarAction;
        boolean animationChanged = selected != avatarAnimation;

        if (actionChanged) {
            if (desiredAction == AvatarAction.IDLE) {
                selected.setFrameIndex(0);
            } else if (desiredAction == AvatarAction.ATTACK) {
                selected.restart();
                attackQueued = false;
            } else if (avatarAnimation != null) {
                int carryIndex = avatarAnimation.frameIndex() % selected.frameCount();
                selected.setFrameIndex(carryIndex);
            }
        } else if (animationChanged && avatarAnimation != null) {
            int carryIndex = avatarAnimation.frameIndex() % selected.frameCount();
            selected.setFrameIndex(carryIndex);
        }
        avatarAction = desiredAction;
        avatarAnimation = selected;

        boolean shouldAdvance = avatarAction != AvatarAction.IDLE || movedThisTick || avatarAnimation.frameCount() > 1;
        if (shouldAdvance) {
            avatarAnimation.advance();
        }

        avatarSprite = avatarAnimation.currentFrame();
        lastFacing = directionToChar(facing);
    }



    //Load game via save file if exists, restores state directly from snapshot
    private boolean loadGame() {
        if (!FileUtils.fileExists(SAVE_FILE)) {
            return false;
        }
        try (ObjectInputStream input = new ObjectInputStream(new FileInputStream(SAVE_FILE))) {
            Object raw = input.readObject();
            if (!(raw instanceof SaveState saved)) {
                return false;
            }
            restoreFromState(saved);
            return true;
        } catch (IOException | ClassNotFoundException e) {
            return false;
        }
    }

    private void saveGameState() {
        if (world == null || avatar == null || npcManager == null || inventory == null) {
            return;
        }
        SaveState.SaveSnapshot snap = new SaveState.SaveSnapshot(
                worldSeed,
                npcSeed,
                avatar,
                decayingLightRadius,
                lastDecayTime,
                lightSurgeStartMs,
                currentPlayTimeMs(),
                enemiesFelled,
                totalDamageTaken,
                totalDamageGiven,
                inventory,
                droppedItems,
                npcManager.npcs(),
                npcManager.corpses()
        );

        SaveState state = SaveState.capture(snap);
        try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(SAVE_FILE))) {
            output.writeObject(state);
        } catch (IOException e) {
            // Best-effort; ignore failures to keep gameplay responsive
        }
    }

    private void restoreFromState(SaveState state) {
        reset();
        worldSeed = state.worldSeed();
        npcSeed = state.npcSeed();
        accumulatedPlayTimeMs = state.playTimeMs();
        finalPlayTimeMs = 0L;
        sessionStartMs = System.currentTimeMillis();
        enemiesFelled = state.enemiesFelled();
        totalDamageTaken = state.damageTaken();
        totalDamageGiven = state.damageGiven();
        World generator = new World(worldSeed);
        world = generator.generate();
        decayingLightRadius = state.decayingLightRadius();
        lastDecayTime = state.lastDecayTime();
        lightSurgeStartMs = state.lightSurgeStartMs();
        ter.setLightRadius(decayingLightRadius);

        avatar = buildAvatar(state.avatar());
        initializeAvatarAnimations(directionFromChar(lastFacing));
        drawX = avatar.x;
        drawY = avatar.y;

        inventory = rebuildInventory(state.inventory());
        droppedItems = rebuildDroppedItems(state.droppedItems());

        npcManager = new NpcManager(new Random(npcSeed), combatService);
        npcManager.setDeathHandler(this::handleNpcDeath);
        npcManager.restoreState(rebuildNpcs(state.npcs()), rebuildCorpses(state.corpses()));
    }

    private Avatar buildAvatar(SaveState.AvatarState avatarState) {
        SaveState.HealthState healthState = avatarState.health();
        HealthComponent health = new HealthComponent(healthState.current(), healthState.max(),
                healthState.armor(), healthState.invulnerabilityFrames());
        health.setInvulnerabilityRemaining(healthState.invulnerabilityRemaining());
        health.addDeathCallback(this::handleAvatarDeath);
        Avatar built = new Avatar(avatarState.x(), avatarState.y(), avatarState.lives(), health);
        built.setSpawnPoint(new Entity.Position(avatarState.spawnX(), avatarState.spawnY()));
        combatService.register(built);
        lastFacing = directionToChar(Direction.DOWN);
        return built;
    }

    private Inventory rebuildInventory(SaveState.InventoryState inventoryState) {
        Inventory rebuilt = new Inventory(inventoryState.slots());
        for (SaveState.ItemStackState stack : inventoryState.stacks()) {
            Item item = ItemRegistry.byId(stack.itemId());
            if (item != null) {
                rebuilt.add(item, stack.quantity());
            }
        }
        return rebuilt;
    }

    private List<DroppedItem> rebuildDroppedItems(List<SaveState.DroppedItemState> states) {
        List<DroppedItem> drops = new ArrayList<>();
        for (SaveState.DroppedItemState drop : states) {
            Item item = ItemRegistry.byId(drop.itemId());
            if (item != null) {
                drops.add(new DroppedItem(item, drop.quantity(), drop.x(), drop.y()));
            }
        }
        return drops;
    }

    private List<Npc> rebuildNpcs(List<SaveState.NpcState> states) {
        List<Npc> npcs = new ArrayList<>();
        for (SaveState.NpcState state : states) {
            SaveState.HealthState healthState = state.health();
            HealthComponent health = new HealthComponent(healthState.current(), healthState.max(),
                    healthState.armor(), healthState.invulnerabilityFrames());
            health.setInvulnerabilityRemaining(healthState.invulnerabilityRemaining());
            Npc npc = new Npc(state.x(), state.y(), new Random(state.rngSeed()), state.rngSeed(), state.variant(),
                    Tileset.loadNpcSpriteSet(state.variant()), health);
            npc.setDrawX(state.drawX());
            npc.setDrawY(state.drawY());
            npcs.add(npc);
        }
        return npcs;
    }

    private List<core.NPC.Corpse> rebuildCorpses(List<SaveState.CorpseState> states) {
        List<core.NPC.Corpse> corpses = new ArrayList<>();
        for (SaveState.CorpseState state : states) {
            corpses.add(new core.NPC.Corpse(state.x(), state.y(), Tileset.NPC_CORPSE));
        }
        return corpses;
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
            drawX = avatar.x + avatarOffsetX;
            drawY = avatar.y + avatarOffsetY;
            double avatarScale = 2;   // adjust this number as desired (0.3–0.6 looks good)
            double screenX = ter.toScreenX(drawX);
            double screenY = ter.toScreenY(drawY);
            avatarSprite.drawScaled(screenX, screenY, avatarScale);
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

    private String formatDuration(long millis) {
        long totalSeconds = Math.max(0, millis) / MS_PER_S;
        long minutes = totalSeconds / SEC_PER_MIN;
        long seconds = totalSeconds % SEC_PER_MIN;
        return String.format("%d:%02d", minutes, seconds);
    }

    private enum AvatarAction {
        IDLE,
        WALK,
        RUN,
        ATTACK,
        DEATH
    }
}
