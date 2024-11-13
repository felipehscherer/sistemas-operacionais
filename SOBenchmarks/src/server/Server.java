package server;

import database.InMemoryDatabase;

public abstract class Server {
    protected int port;
    protected boolean running;
    protected InMemoryDatabase database;

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

    public Server(int port, int databaseSize) {
        this.port = port;
        boolean criticalSectionEnabled = ServerConfig.getInstance().isCriticalSectionAccessControlEnabled();
        this.database = new InMemoryDatabase(databaseSize, criticalSectionEnabled);
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
        printDatabaseSum();
    }

    public void printDatabaseSum() {
        int sum = database.sum();
        System.out.println("Somatório de todas as posições do vetor: " + sum);
    }
}