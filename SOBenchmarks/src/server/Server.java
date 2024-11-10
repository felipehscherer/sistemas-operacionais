package server;

import java.io.IOException;

public abstract class Server {
    protected int port;
    protected boolean running;

    protected void log(String message) {
        if (ServerConfig.getInstance().isPrintLogEnabled()) {
            System.out.println(message);
        }
    }

    public Server(int port) {
        this.port = port;
    }

    public void startServer() {
        this.running = true;
        System.out.println("Server started on port " + this.port);
        this.runServer();
    }

    protected abstract void runServer();

    public void stopServer() {
        this.running = false;
        System.out.println("Server stopped.");
    }
}
