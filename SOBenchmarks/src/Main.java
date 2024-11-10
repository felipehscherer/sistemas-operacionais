import java.io.IOException;
import server.ProcessServer;
import server.SelectorServer;
import server.Server;
import server.ServerConfig;
import server.ThreadedServer;
import server.ServerConfig.SERVER_TYPES;

public class Main {
    public static void main(String[] args) {
        int port = 8080;
        boolean printLogEnabled = false;
        boolean criticalSectionEnabled = false;
        int maxThreads = 10;
        String serverType = "threaded";

        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
                printLogEnabled = Boolean.parseBoolean(args[1]);
                criticalSectionEnabled = Boolean.parseBoolean(args[2]);
                serverType = args.length > 3 ? args[3].toLowerCase() : "threaded";
                maxThreads = args.length > 4 ? Integer.parseInt(args[4]) : 10;
            } catch (Exception e) {
                System.out.println("Erro ao interpretar os parâmetros. Uso: java Main <porta> <printLogEnabled> <criticalSectionEnabled> <serverType> <maxThreads>");
                System.out.println("Exemplo: java Main 12345 true false threaded 10");
                return;
            }
        }

        ServerConfig config = ServerConfig.getInstance();
        config.setCriticalSectionAccessControlEnabled(criticalSectionEnabled);
        config.setPrintLogEnabled(printLogEnabled);

        Server server;

        try {
            if ("selector".equals(serverType)) {
                server = new SelectorServer(port);
                config.setServerType(SERVER_TYPES.SELECTOR);
            } else if ("process".equals(serverType)) {
                server = new ProcessServer(port);
                config.setServerType(SERVER_TYPES.PROCESS);
            } else if ("threaded".equals(serverType)) {
                server = new ThreadedServer(port, maxThreads);
                config.setServerType(SERVER_TYPES.THREADED);
            } else {
                System.out.println("Tipo de servidor desconhecido. Use 'threaded', 'selector' ou 'process'.");
                return;
            }

            server.startServer();
        } catch (IOException e) {
            System.out.println("Erro ao iniciar o servidor: " + e.getMessage());
        }
    }
}
