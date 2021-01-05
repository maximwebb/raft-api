package maximwebb.app;

import maximwebb.app.ui.Console;

public class App {
    public static void main(String[] args) {
        if (args.length >= 1) {
            Console console = new Console(args[0]);
        } else {
            Console console = new Console("User");
        }
    }
}
