package core;
import tileengine.TETile;
import tileengine.TERenderer;

public class Main {
    public static void main(String[] args){

        Engine engine = new Engine();
        if (args.length > 0) {
            //Probably going to get rid of this - was for replay but its too hard to implement with AI
            engine.interactWithInputString(args[0]);
        } else {
            engine.interactWithKeyboard();
        }
    }





//        Create seed based on current system time:
//        long seed = args.length > 0 ? parseSeed(args[0]) : System.currentTimeMillis();
//        //long seed = 6206686636164176845L; //-- swap out for repeated generation
//        //long seed = 6206686636164176845L; //-- swap out for repeated generation
//        World world = new World(seed);
//        TETile[][] tiles = world.generate();
//        // build your own world!
//
//
//        TERenderer ter = new TERenderer();
//        ter.initialize(World.WIDTH, World.HEIGHT);
//        ter.renderFrame(tiles);
//    }
//
//    private static long parseSeed(String arg) {
//        try {
//            return Long.parseLong(arg);
//        } catch (NumberFormatException e) {
//            return System.currentTimeMillis();
//        }
//    }
}
