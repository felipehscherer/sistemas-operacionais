package client;

import java.io.IOException;

public class SelectorServerConnectionFactory extends ServerConnectionFactory {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 12345;

    @Override
    public ServerConnection createConnection() throws IOException {
        return new SelectorServerConnection(SERVER_HOST, SERVER_PORT);
    }
}