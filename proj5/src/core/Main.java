package core;

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
}
