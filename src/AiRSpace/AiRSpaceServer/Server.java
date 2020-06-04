package AiRSpace.AiRSpaceServer;

import AiRSpace.AiRSpaceServer.Exceptions.NoEmptySlot;
import AiRSpace.AiRSpaceServer.Handlers.ClientHandler;
import AiRSpace.AiRSpaceServer.Utility.Setting;

import java.io.IOException;
import java.net.ServerSocket;

public class Server implements Runnable
{
    public static final int STATE_RUNNING = 1;
    public static final int STATE_STOPPING = 2;
    public static final int STATE_STOPPED = 3;

    public static int state = 1;
    private static ServerSocket server_socket = null;

    public Server()
    {
        try {
            // Try to listen on a port
            server_socket = new java.net.ServerSocket(Setting.getPort(), 1, null);
            System.out.println("Server listening on port " + Setting.getPort());
        } catch (IOException ioe) {
            System.out.println("Could not listen on port. Is the server already running?");
        }
    }

    public void run()
    {
        // As long as the server hasn't been instructed to shutdown
        while (!shouldShutdown()) {
            try {
                // Accept new client
                java.net.Socket socket = server_socket.accept();
                // Don't delay receiving message until buffer is full, just receive them as they arrive
                socket.setTcpNoDelay(true);
                try {
                    Client client = ClientHandler.request(socket);
                    client.run();
                } catch (NoEmptySlot ncs) {
                    // If there're no slots available, close the connection.
                    System.out.println("There're no free slots for new connections");
                    socket.close();
                }
            } catch (IOException ioe) {
                if (!shouldShutdown()) {
                    System.out.println("Could not accept connect from server socket");
                }
            }
        }

        // Do everything required to shutdown here...

        // Last of all...
        state = STATE_STOPPED;
    }

    private static boolean shouldShutdown()
    {
        // If the server is mid-shutdown...
        if (state == STATE_STOPPING) {
            return true;
        }
        return false;
    }

    public static void shutdown()
    {
        System.out.println("Shutting down the server");
        // Mark the server is currently shutting down
        state = STATE_STOPPING;
        // Kick all clients
        try {
            // close() interrupts the accept() method which will otherwise wait forever
            server_socket.close();
        } catch (IOException ioe) {
            // This exception is deliberate, do nothing
        }
    }
}
