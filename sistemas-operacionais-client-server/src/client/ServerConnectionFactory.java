package client;

import java.io.*;

// Factory abstrata para criar conexões
public abstract class ServerConnectionFactory {
    protected static final String SERVER_HOST = "localhost";

    public static ServerConnectionFactory getFactory(String serverType) {
        switch (serverType) {
            case "THREAD":
                return new ThreadServerConnectionFactory();
            case "PROCESS":
                return new ProcessServerConnectionFactory();
            case "SELECTOR":
                return new SelectorServerConnectionFactory();
            default:
                return null;
        }
    }

    public abstract ServerConnection createConnection() throws IOException;
}

