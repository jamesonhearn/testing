package tileengine;

import java.awt.Color;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Contains constant tile objects, to avoid having to remake the same tiles in different parts of
 * the code.
 *
 * You are free to (and encouraged to) create and add your own tiles to this file. This file will
 * be turned in with the rest of your code.
 *
 * Ex:
 *      world[x][y] = Tileset.FLOOR;
 *
 * The style checker may crash when you try to style check this file due to use of unicode
 * characters. This is OK.
 */

public class Tileset {

    // Added to avoid having to hard code filepaths to assets
    // Added another method specific for NPCs - probably redundant
    private static String assetPath(String filename) {
        URL url = Tileset.class.getClassLoader().getResource("tiles/" + filename);
        if (url != null) {
            return url.getFile();
        }
        Path local = Paths.get("assets", "tiles", filename).toAbsolutePath();
        return local.toString();
    }


    // Create object for storing full set of directional sprites per avatar
    public record NpcSpriteSet(TETile[] walkUpFrames, TETile[] walkDownFrames,
                               TETile[] walkLeftFrames, TETile[] walkRightFrames,
                               TETile[] attackUpFrames, TETile[] attackDownFrames,
                               TETile[] attackLeftFrames, TETile[] attackRightFrames) { }



    // helper path method
    private static String npcAssetPath(int variant, String filename) {
        Path local = Paths.get("assets", "avatars", "NPC", String.valueOf(variant), filename)
                .toAbsolutePath();
        return local.toString();
    }

    // creates set of directional frames based on action, direction, and NPC id
    private static TETile[] loadNpcDirectionFrames(int variant, String action, String direction, int id) {
        TETile[] frames = new TETile[8];
        for (int i = 0; i < frames.length; i++) {
            frames[i] = new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "npc",
                    npcAssetPath(variant, "avatar_" + action + "_" + direction + "_" + i + ".png"), id);
        }

        return frames;
    }

    public static TETile[] loadAvatarFramesFromSheet(String sheetPath, int row, int id) {
        SpriteSheet sheet = new SpriteSheet(sheetPath, 16, 16); // assumes 16x16 squares in sheet - will need to keep things consistent here, not worth the effort of dynamically checking size
        TETile[] frames = new TETile[8];
        for (int col = 0; col < frames.length; col += 1) {
            frames[col] = sheet.tileAt(col, row, "avatar", id);
        }
        return frames;
    }

    /**
     * Load walking animation frames for a specific NPC variant.
     * The assets are expected under assets/avatars/NPC/{variant}/.
     */
    public static NpcSpriteSet loadNpcSpriteSet(int variant) {
        TETile[] walkRight = loadNpcDirectionFrames(variant, "walk", "right", 16);
        TETile[] walkLeft = loadNpcDirectionFrames(variant, "walk", "left", 15);
        TETile[] walkUp = loadNpcDirectionFrames(variant, "walk", "up", 13);
        TETile[] walkDown = loadNpcDirectionFrames(variant, "walk", "down", 14);

        TETile[] attackRight = loadNpcDirectionFrames(variant, "attack", "right", 26);
        TETile[] attackLeft = loadNpcDirectionFrames(variant, "attack", "left", 25);
        TETile[] attackUp = loadNpcDirectionFrames(variant, "attack", "up", 23);
        TETile[] attackDown = loadNpcDirectionFrames(variant, "attack", "down", 24);
        return new NpcSpriteSet(walkUp, walkDown, walkLeft, walkRight,
                attackUp, attackDown, attackLeft, attackRight);
    }


    public static final TETile NPC_CORPSE = new TETile('†', new Color(115, 80, 65),
            new Color(58, 52, 45), "fallen foe", 98);


    public static final TETile LOOT_BAG = new TETile('X', new Color(221, 176, 61),
            new Color(74, 63, 52), "dropped item", 99);
    // I have like 10 iterations worth of wall tiles in here and a lot of methods reference various versions
    // So while the game only uses 2 PNGs for walls I have a bunch of redundant tiles
    // Should clean those up sometime
    public static final TETile WALL = new TETile('#', new Color(196, 178, 158),
            new Color(58, 52, 45), "cave wall", "assets/tiles/cave_wall_base.png", 1);

    public static final TETile BACK_WALL = new TETile('#', new Color(196, 178, 158),
            new Color(58, 52, 45), "cave wall", "assets/tiles/cave_wall_base.png", 1);
    public static final TETile FRONT_WALL = new TETile('#', new Color(196, 178, 158),
            new Color(58, 52, 45), "cave wall", "assets/tiles/cave_wall_top.png", 1);
    public static final TETile LEFT_WALL = new TETile('#', new Color(196, 178, 158),
            new Color(58, 52, 45), "cave wall", "assets/tiles/cave_wall_top.png", 1);
    public static final TETile WALL_TOP = new TETile(' ', new Color(196, 178, 158),
            new Color(58, 52, 45), "cave wall", "assets/tiles/cave_wall_top.png", 1);
    public static final TETile WALL_SIDE = new TETile(' ', new Color(196, 178, 158),
            new Color(58, 52, 45), "cave wall", "assets/tiles/cave_wall_base.png", 1);

    public static final TETile RIGHT_WALL = new TETile('#', new Color(196, 178, 158),
            new Color(58, 52, 45), "cave wall", "assets/tiles/cave_wall_top.png", 1);
    public static final TETile FRONT_WALL_TOP = new TETile('#', new Color(196, 178, 158),
            new Color(58, 52, 45), "cave wall", "assets/tiles/cave_wall_cap.png", 1);

    public static final TETile FLOOR = new TETile(' ', new Color(214, 193, 170),
            new Color(74, 63, 52), "cave floor", "assets/tiles/cave_floor_6.png", 2);
    public static final TETile ELEVATOR = new TETile(' ', new Color(214, 193, 170),
            new Color(74, 63, 52), "cave floor", "assets/tiles/elevator.png", 2);
    public static final TETile NOTHING = new TETile(' ', new Color(0, 0, 0),
            new Color(0, 0, 0), "nothing", 3);

//    public static final TETile AVATAR_UP = new TETile(' ', new Color(240, 234, 214),
//            new Color(46, 38, 33), "you", assetPath("avatar_up.png"), 0);
//    public static final TETile AVATAR_DOWN = new TETile(' ', new Color(240, 234, 214),
//            new Color(46, 38, 33), "you", assetPath("avatar_down.png"), 13);
//    public static final TETile AVATAR_LEFT = new TETile(' ', new Color(240, 234, 214),
//            new Color(46, 38, 33), "you", assetPath("avatar_left.png"), 14);
//    public static final TETile AVATAR_RIGHT = new TETile(' ', new Color(240, 234, 214),
//            new Color(46, 38, 33), "you", assetPath("avatar_right.png"), 15);
//    public static final TETile AVATAR = AVATAR_DOWN;


    public static final TETile[] AVATAR_UP_FRAMES = {
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "you", assetPath("avatar_walk_up_0.png"), 13),
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "you", assetPath("avatar_walk_up_1.png"), 13),
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "you", assetPath("avatar_walk_up_2.png"), 13),
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "you", assetPath("avatar_walk_up_3.png"), 13),
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "you", assetPath("avatar_walk_up_4.png"), 13),
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "you", assetPath("avatar_walk_up_5.png"), 13),
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "you", assetPath("avatar_walk_up_6.png"), 13),
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "you", assetPath("avatar_walk_up_7.png"), 13),
    };
    public static final TETile[] AVATAR_DOWN_FRAMES = {
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "you", assetPath("avatar_walk_down_0.png"), 14),
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "you", assetPath("avatar_walk_down_1.png"), 14),
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "you", assetPath("avatar_walk_down_2.png"), 14),
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "you", assetPath("avatar_walk_down_3.png"), 14),
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "you", assetPath("avatar_walk_down_4.png"), 14),
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "you", assetPath("avatar_walk_down_5.png"), 14),
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "you", assetPath("avatar_walk_down_6.png"), 14),
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "you", assetPath("avatar_walk_down_7.png"), 14),
    };
    public static final TETile[] AVATAR_LEFT_FRAMES = {
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "you", assetPath("avatar_walk_left_0.png"), 15),
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "you", assetPath("avatar_walk_left_1.png"), 15),
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "you", assetPath("avatar_walk_left_2.png"), 15),
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "you", assetPath("avatar_walk_left_3.png"), 15),
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "you", assetPath("avatar_walk_left_4.png"), 15),
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "you", assetPath("avatar_walk_left_5.png"), 15),
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "you", assetPath("avatar_walk_left_6.png"), 15),
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "you", assetPath("avatar_walk_left_7.png"), 15),
    };
    public static final TETile[] AVATAR_RIGHT_FRAMES = {
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "you", assetPath("avatar_walk_right_0.png"), 16),
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "you", assetPath("avatar_walk_right_1.png"), 16),
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "you", assetPath("avatar_walk_right_2.png"), 16),
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "you", assetPath("avatar_walk_right_3.png"), 16),
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "you", assetPath("avatar_walk_right_4.png"), 16),
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "you", assetPath("avatar_walk_right_5.png"), 16),
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "you", assetPath("avatar_walk_right_6.png"), 16),
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "you", assetPath("avatar_walk_right_7.png"), 16),
    };



    public static final TETile HEALTH = new TETile(' ', new Color(240, 234, 214), new Color(46, 38, 33), "healthbar","assets/ui/healthbar_early_concept.png", 100);

    public static final TETile SPHEREVIS = new TETile(' ', new Color(240, 234, 214),
            new Color(46, 38, 33), "spherevis", "assets/avatars/spherevis/spherevis.png", 16);

    public static final TETile[] SLIME_RIGHT_FRAMES = {
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "slime", "assets/avatars/slime/slime_walk_0.png", 16),
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "slime", "assets/avatars/slime/slime_walk_1.png", 16),
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "slime", "assets/avatars/slime/slime_walk_2.png", 16),
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "slime", "assets/avatars/slime/slime_walk_3.png", 16),
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "slime", "assets/avatars/slime/slime_walk_4.png", 16),
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "slime", "assets/avatars/slime/slime_walk_5.png", 16),
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "slime", "assets/avatars/slime/slime_walk_6.png", 16),
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "slime", "assets/avatars/slime/slime_walk_7.png", 16),
    };

    public static final TETile[] SLIME_UP_FRAMES = SLIME_RIGHT_FRAMES;
    public static final TETile[] SLIME_DOWN_FRAMES = SLIME_RIGHT_FRAMES;

    public static final TETile[] SLIME_LEFT_FRAMES = {
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "slime", "assets/avatars/slime/slime_walk_0_flipped.png", 15),
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "slime", "assets/avatars/slime/slime_walk_1_flipped.png", 15),
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "slime", "assets/avatars/slime/slime_walk_2_flipped.png", 15),
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "slime", "assets/avatars/slime/slime_walk_3_flipped.png", 15),
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "slime", "assets/avatars/slime/slime_walk_4_flipped.png", 15),
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "slime", "assets/avatars/slime/slime_walk_5_flipped.png", 15),
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "slime", "assets/avatars/slime/slime_walk_6_flipped.png", 15),
            new TETile(' ', new Color(240, 234, 214),
                    new Color(46, 38, 33), "slime", "assets/avatars/slime/slime_walk_7_flipped.png", 15),
    };




}


