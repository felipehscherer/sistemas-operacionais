package client;

import java.io.IOException;

public class ThreadServerConnectionFactory extends ServerConnectionFactory {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 12345;

    @Override
    public ServerConnection createConnection() throws IOException {
        return new ThreadServerConnection(SERVER_HOST, SERVER_PORT);
    }
}
