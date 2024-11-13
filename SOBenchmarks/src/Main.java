import java.io.IOException;
import server.ProcessServer;
import server.SelectorServer;
import server.Server;
import server.ServerConfig;
import server.ThreadedServer;
import server.ServerConfig.SERVER_TYPES;

public class Main {
    public static void main(String[] args) {
        int port = 12345;
        boolean printLogEnabled = true;
        boolean criticalSectionEnabled = false;
        int maxThreads = 10;
        String serverType = "process";
        int databaseSize = 100;

        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
                printLogEnabled = Boolean.parseBoolean(args[1]);
                criticalSectionEnabled = Boolean.parseBoolean(args[2]);
                serverType = args.length > 3 ? args[3].toLowerCase() : "process";
                maxThreads = args.length > 4 ? Integer.parseInt(args[4]) : 10;
                databaseSize = args.length > 5 ? Integer.parseInt(args[5]) : 100;
            } catch (Exception e) {
                System.out.println(
                        "Erro ao interpretar os parâmetros. Uso: java Main <porta> <printLogEnabled> <criticalSectionEnabled> <serverType> <maxThreads> <databaseSize>");
                System.out.println("Exemplo: java Main 12345 true false process 10 100");
                return;
            }
        }

        ServerConfig config = ServerConfig.getInstance();
        config.setCriticalSectionAccessControlEnabled(criticalSectionEnabled);
        config.setPrintLogEnabled(printLogEnabled);
        Server server;

        try {
            if ("selector".equals(serverType)) {
                server = new SelectorServer(port, databaseSize);
                config.setServerType(ServerConfig.SERVER_TYPES.SELECTOR);
            } else if ("process".equals(serverType)) {
                server = new ProcessServer(port, databaseSize);
                config.setServerType(ServerConfig.SERVER_TYPES.PROCESS);
            } else if ("threaded".equals(serverType)) {
                server = new ThreadedServer(port, maxThreads, databaseSize);
                config.setServerType(ServerConfig.SERVER_TYPES.THREADED);
            } else {
                System.out.println("Tipo de servidor desconhecido. Use 'threaded', 'selector' ou 'process'.");
                return;
            }

            // Adiciona um ShutdownHook para parar o servidor e imprimir a soma do banco de
            // dados
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                server.stopServer();
            }));

            server.startServer();
        } catch (IOException e) {
            System.out.println("Erro ao iniciar o servidor: " + e.getMessage());
        }
    }
}