package AiRSpace.AiRSpaceServer;

import AiRSpace.AiRSpaceServer.Handlers.ClientHandler;
import AiRSpace.AiRSpaceServer.Utility.Setting;

public class Game implements Runnable
{
    private static final int STATE_RUNNING = 1;
    private static final int STATE_STOPPING = 2;
    private static final int STATE_STOPPED = 3;

    private static final int TICK_TIME = 600;

    private static int state = STATE_RUNNING;

    public static void main(String[] args)
    {
        Game game = new Game();
        (new Thread(game)).start();
        Server server = new Server();
        (new Thread(server)).start();
    }

    public Game()
    {
        System.out.println("Starting " + Setting.get("Name"));
    }

    public void run()
    {
        ClientHandler client_handler = new ClientHandler();

        while (!shouldShutdown()) {
            // Get the time at the beginning of each loop
            long time_start = System.currentTimeMillis();

            // Process all the stuff in the game
            client_handler.process();

            // Get the time at the end of each loop
            long time_end = System.currentTimeMillis();

            // Make sure we wait an amount of time before refreshing the player's screens
            if (time_end - time_start < 0) {
                System.out.println("The server cannot keep up with it's load. Expect lag.");
            } else {
                try {
                    long sleepTime = TICK_TIME - (time_end - time_start);
                    System.out.println(sleepTime);
                    Thread.sleep(sleepTime);
                } catch (InterruptedException ie) {
                    // Don't do anything
                }
            }
        }

        // Do everything required to shutdown here...
        System.out.println("Saving bla bla ...");

        // Lastly...
        state = STATE_STOPPED;
    }

    public static boolean shouldShutdown()
    {
        return state == STATE_STOPPING && Server.state == Server.STATE_STOPPED;
    }

    public static void shutdown()
    {
        state = STATE_STOPPING;
        System.out.println(Setting.get("Name") + " is shutting down");
        Server.shutdown();
    }
}
