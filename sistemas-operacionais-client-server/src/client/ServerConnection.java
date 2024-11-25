package client;

import java.io.IOException;

// Interface base para conexões com servidor
public interface ServerConnection {
    String executeOperation(String operation) throws IOException;

    void close() throws IOException;
}
