package client;

import java.io.*;

// Factory abstrata para criar conexões
public abstract class ServerConnectionFactory {
    protected static final String SERVER_HOST = "localhost";

    public static ServerConnectionFactory getFactory(String serverType) {
        return switch (serverType) {
            case "THREAD" -> new ThreadServerConnectionFactory();
            case "PROCESS" -> new ProcessServerConnectionFactory();
            case "SELECTOR" -> new SelectorServerConnectionFactory();
            default -> null;
        };
    }

    public abstract ServerConnection createConnection() throws IOException;
}

