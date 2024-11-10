package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadedServer extends Server {
    private final ExecutorService threadPool;
    private ReentrantLock lock = new ReentrantLock();
    private ServerSocket serverSocket;

    public ThreadedServer(int port, int maxThreads) {
        super(port);
        this.threadPool = Executors.newFixedThreadPool(maxThreads);
        try {
            this.serverSocket = new ServerSocket(this.port);
        } catch (IOException e) {
            System.err.println("Error initializing server socket: " + e.getMessage());
        }
    }

    @Override
    protected void runServer() {
        log("ThreadedServer started on port " + port);
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                log("Client connected from " + clientSocket.getRemoteSocketAddress());
                threadPool.execute(() -> handleClient(clientSocket));
            } catch (IOException e) {
                if (running) {
                    System.err.println("Error accepting client connection: " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void stopServer() {
        super.stopServer();
        threadPool.shutdown();
        try {
            if (this.serverSocket != null && !this.serverSocket.isClosed()) {
                this.serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }
    }

    protected void handleClient(Socket clientSocket) {
        boolean criticalSectionEnabled = ServerConfig.getInstance().isCriticalSectionAccessControlEnabled();

        try {
            if (criticalSectionEnabled) {
                lock.lock();
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            String clientMessage;
            while ((clientMessage = in.readLine()) != null) {
                log("Received message from client: " + clientMessage);
                String response = processCommand(clientMessage);
                out.println(response);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (criticalSectionEnabled) {
                lock.unlock();
            }
            try {
                clientSocket.close();
                log("Client disconnected.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String processCommand(String command) {
        String[] parts = command.split(" ");
        String cmd = parts[0];
        if ("READ".equalsIgnoreCase(cmd)) {
            int position = Integer.parseInt(parts[1]);
            // Implementar a lógica de leitura (por exemplo, ler de um array ou banco de dados)
            return "Data read from position " + position;
        } else if ("WRITE".equalsIgnoreCase(cmd)) {
            int position = Integer.parseInt(parts[1]);
            // Implementar a lógica de escrita
            return "Data written to position " + position;
        } else {
            return "Unknown command.";
        }
    }

}
