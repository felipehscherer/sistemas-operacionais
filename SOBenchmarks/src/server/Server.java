package server;

public abstract class Server {
    protected int port;
    protected boolean running;

    protected void log(String message) {
        if (ServerConfig.getInstance().isPrintLogEnabled()) {
            System.out.println(message);
        }
    }

    protected void log(String clientId, String message) {
        if (ServerConfig.getInstance().isPrintLogEnabled()) {
            System.out.println("Cliente " + clientId + ": " + message);
        }
    }

    public Server(int port) {
        this.port = port;
    }

    public void startServer() {
        this.running = true;
        System.out.println("Servidor iniciado na porta " + this.port);
        this.runServer();
    }

    protected abstract void runServer();

    public void stopServer() {
        this.running = false;
        System.out.println("Servidor parado.");
    }
}