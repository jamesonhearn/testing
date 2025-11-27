package core;
import edu.princeton.cs.algs4.StdDraw;
import tileengine.Tileset;
import tileengine.TERenderer;
import tileengine.TETile;
import utils.FileUtils;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import java.util.Random;
import java.util.stream.Collectors;

import core.NPC.Npc;
import core.NPC.NpcManager;
import core.items.DroppedItem;
import core.items.Inventory;
import core.items.Item;
import core.items.ItemRegistry;
import core.items.ItemStack;



public class Engine {
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int screenWidth = (int) screenSize.getWidth();
    int screenHeight = (int) screenSize.getHeight();

    public static final int WORLD_WIDTH = World.WIDTH;
    public static final int WORLD_HEIGHT = World.HEIGHT;

    private final int VIEW_WIDTH = 50; //screenWidth / 24;
    private final int VIEW_HEIGHT = 35;//screenHeight / 24;
    public static final int HUD_HEIGHT = 3;
    public static final String SAVE_FILE = "save.txt";

    private final TERenderer ter = new TERenderer();
    private TETile[][] world;
    private Avatar avatar;
    private TETile avatarSprite;
    private StringBuilder history;
    private NpcManager npcManager;
    private CombatService combatService;

    // Inventory system stuffs
    private Inventory inventory;
    private List<DroppedItem> droppedItems;
    private boolean inventoryVisible;
    private String hudMessage;
    private boolean tabDown = false;



    // Lighting variables
    private static final double BASE_LIGHT_RADIUS = 20.0;
    private static final double SURGE_LIGHT_RADIUS = 30.0;
    private static final long LIGHT_SURGE_DURATION_MS = 10_000L;
    private static final long LIGHT_FADE_DURATION_MS = 3_000L;
    private long lightSurgeStartMs = -1L;


    // AUDIO STUFF
    private final AudioPlayer music = new AudioPlayer();


    // Movement variables
    private boolean wDown, aDown, sDown, dDown;
    private char currentDirection = 0;
    private boolean shiftDown = false;


    private static final String HEALTHBAR_IMAGE_PATH = "assets/ui/healthbar_early_concept.png";
    private static final double HEALTHBAR_WIDTH_TILES = 8.0;
    private static final double HEALTHBAR_HEIGHT_TILES = 2.0;
    private static final double HUD_MARGIN_TILES = 0.5;


    /** Half-size of the avatar collision box in tile units (smaller than a full tile). */
    private static final double AVATAR_HITBOX_HALF = 0.24;
    /** Half-size of the NPC collision box in tile units (smaller than a full tile). */
    private static final double NPC_HITBOX_HALF = 0.30;
    /** Offset the avatar toward the entry edge when squeezing past an NPC. */
    private static final double HUG_EDGE_OFFSET = 0.5 - AVATAR_HITBOX_HALF - 0.02;


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
    private double avatarOffsetX = 0.0;
    private double avatarOffsetY = 0.0;
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
                music.stop();
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
        combatService = new CombatService();

        //Reset inventory
        inventory = new Inventory(16);
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
        ter.setLightRadius(BASE_LIGHT_RADIUS);
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
        music.playLoop("assets/audio/spookycave.wav"); // uncomment when you want to check music
        while (true) {
            renderWithHud();


            while (StdDraw.hasNextKeyTyped()) {
                char raw = StdDraw.nextKeyTyped();
                char c = Character.toLowerCase(raw);
                if (processCommand(c, true, true)) {
                    return;
                }
            }
            updateInventoryToggle();

            handleMovementRealtime(true);
            if (npcManager != null && avatar != null) {
                npcManager.tick(world, avatar);
            }
            combatService.tick();
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
        StdDraw.picture(screenWidth / 2, screenHeight / 2, "assets/ui/healthbar_early_concept.png", 20,10);
        StdDraw.show();
    }

    //Draw hud (just a bar at the top that displays tile under mouse
    private void drawHud() {
        StdDraw.setPenColor(Color.WHITE);
        double hudY = VIEW_HEIGHT + 1.5;


        double barWidth = 30;   // or whatever large size you want
        double barHeight = 30 / 3.0;  // keep aspect ratio if needed

        double leftMargin = HUD_MARGIN_TILES;
        double hbX = leftMargin + barWidth / 2.0;
        double hbY = VIEW_HEIGHT + HUD_HEIGHT - (barHeight / 2.0) - HUD_MARGIN_TILES * 2;

        StdDraw.picture(hbX, hbY, HEALTHBAR_IMAGE_PATH, barWidth, barHeight);
        StdDraw.textLeft(1, hudY, tileUnderMouse());
        //StdDraw.textLeft(15, hudY, "Inventory: " + inventorySummary());
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



    // Inventory rendering
    private String inventorySummary() {
        if (inventory == null) {
            return "Empty";
        }
        List<ItemStack> stacks = inventory.nonEmptySlots();
        if (stacks.isEmpty()) {
            return "Empty";
        }
        return stacks.stream()
                .limit(3)
                .map(s -> s.item().name() + " x" + s.quantity())
                .collect(Collectors.joining(", "));
    }

    private void drawInventoryOverlay() {
        if (!inventoryVisible) {
            return;
        }
        StdDraw.setPenColor(new Color(0, 0, 0, 200));
        StdDraw.filledRectangle(VIEW_WIDTH / 2.0, VIEW_HEIGHT / 2.0, VIEW_WIDTH / 2.0, VIEW_HEIGHT / 2.0);
        StdDraw.setPenColor(Color.WHITE);
        StdDraw.text(VIEW_WIDTH / 2.0, VIEW_HEIGHT - 2, "Inventory (press I to close)");

        double startY = VIEW_HEIGHT - 4;
        int index = 0;
        for (ItemStack stack : inventory.nonEmptySlots()) {
            double y = startY - index * 1.5;
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
                        //moveAvatar(c);
                    }
                    break;
                case 'e':
                    pickupAtAvatar();
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
        boolean tab = StdDraw.isKeyPressed(KeyEvent.VK_TAB);
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
                    pickupAtAvatar();
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
                        pickupAtAvatar();
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
        if (command == 'e') {
            pickupAtAvatar();
            return false;
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
        resetLighting();
        placeAvatar();
        npcManager = new NpcManager(new Random(seed ^ NPC_SEED_SALT), combatService); // golden ratio hash, allows nice NPC RNG relative to world RNG
        npcManager.setDeathHandler(this::handleNpcDeath);
        npcManager.spawn(world, avatar.x, avatar.y);
        // give initial items and random spawn ground loot
        seedInitialInventory();
        seedDroppedItems(new Random(seed));
    }

    // Find first coordiate that is valid placement for player on spawn - just seeks from bottom right currently
    // Eventually include ladder/elevator placement
    private void placeAvatar() {
        for (int x = 0; x < WORLD_WIDTH; x+=1) {
            for (int y =0; y < WORLD_HEIGHT; y+=1) {
                if (world[x][y].equals(Tileset.FLOOR)) {
                    HealthComponent avatarHealth = new HealthComponent(10, 10, 1, 15);
                    avatarHealth.addDeathCallback(this::handleAvatarDeath);
                    avatar = new Avatar(x, y, 3, avatarHealth);
                    avatar.setSpawnPoint(new Entity.Position(x, y));
                    combatService.register(avatar);
                    avatarSprite = Tileset.AVATAR_DOWN_FRAMES[0];
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
        refreshAvatarSprite();
        return moved;
    }

    private record MovementPlan(Entity.Position target, double offsetX, double offsetY) {}


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


    //starting inventory
    private void seedInitialInventory() {
        if (inventory == null) {
            inventory = new Inventory(16);
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
        while (placed < 6 && attempts < 400) {
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
                    hudMessage = "A burst of light surrounds you";
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
        if (!(entity instanceof Avatar fallen)) {
            return;
        }
        for (core.items.ItemStack stack : inventory.dumpAll()) {
            droppedItems.add(new DroppedItem(stack.item(), stack.quantity(), fallen.x, fallen.y));
        }
        fallen.loseLife();
        hudMessage = "You died! Lives left: " + fallen.lives();
        fallen.respawn();
        avatarOffsetX = 0.0;
        avatarOffsetY = 0.0;
        drawX = fallen.x;
        drawY = fallen.y;
    }

    private void handleNpcDeath(Npc npc) {
        if (npc == null) {
            return;
        }
        droppedItems.add(new DroppedItem(ItemRegistry.GEMSTONE, 1, npc.x(), npc.y()));
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
            animFrame = 0;
        }
        refreshAvatarSprite();
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
            drawX = avatar.x + avatarOffsetX;
            drawY = avatar.y + avatarOffsetY;
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




}