package core;
import tileengine.TETile;
import tileengine.TERenderer;

public class Main {
    public static void main(String[] args){
        //long seed = args.length > 0 ? parseSeed(args[0]) : System.currentTimeMillis();
        long seed = 6206686636164178845L;
        TETile[][] demoTiles = generateDemoWorld(seed);

//        long seed = ;
//        long seed = ;
//        long seed = ;
//        long seed = ;

        TERenderer ter = new TERenderer();
        int worldWidth = demoTiles.length;
        int worldHeight = demoTiles[0].length;



        ter.configureView(worldWidth, worldHeight, worldWidth, worldHeight, 0);
        ter.initialize(worldWidth, worldHeight, 0, 0);
        ter.setAvatarPosition(worldWidth / 2, worldHeight / 2);
        ter.updateCamera();
        ter.renderFrame(demoTiles);

    }



//        Engine engine = new Engine();
//        if (args.length > 0) {
//            //Probably going to get rid of this - was for replay but its too hard to implement with AI
//            engine.interactWithInputString(args[0]);
//        } else {
//            engine.interactWithKeyboard();
//        }
//    }



//        Engine engine = new Engine();
//        if (args.length > 0) {
//            //Probably going to get rid of this - was for replay but its too hard to implement with AI
//            engine.interactWithInputString(args[0]);
//        } else {
//            engine.interactWithKeyboard();
//        }
//    }




//        Your seeds are:
//        6578897764558030256
//        1013758890894698811
//        7162790311120124118
//        7003957710856902839
//        6206686636164176845



    private static long parseSeed(String arg) {
        try {
            return Long.parseLong(arg);
        } catch (NumberFormatException e) {
            return System.currentTimeMillis();
        }
    }

    private static TETile[][] generateDemoWorld(long seed) {
        World world = new World(seed);
        TETile[][] generated = world.generate();
        return firstSquare(generated, 50);
    }

    private static TETile[][] firstSquare(TETile[][] tiles, int size) {
        int width = Math.min(size, tiles.length);
        int height = Math.min(size, tiles[0].length);
        TETile[][] cropped = new TETile[width][height];

        for (int x = 0; x < width; x += 1) {
            System.arraycopy(tiles[x], 0, cropped[x], 0, height);
        }
        return cropped;
    }


}
