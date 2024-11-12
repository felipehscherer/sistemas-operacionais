package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;

public class ProcessServer extends Server {
    private ReentrantLock lock = new ReentrantLock();
    private ServerSocket serverSocket;

    public ProcessServer(int port) {
        super(port);
        try {
            this.serverSocket = new ServerSocket(this.port);
        } catch (IOException e) {
            System.err.println("Erro ao inicializar o socket do servidor: " + e.getMessage());
        }
    }

    @Override
    protected void runServer() {
        log("Servidor ProcessServer iniciado na porta " + port);
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                String clientId = String.valueOf(clientSocket.getPort());
                log(clientId, "Cliente conectado");
                handleClient(clientSocket, clientId);
            } catch (IOException e) {
                if (running) {
                    System.err.println("Erro ao aceitar conexão do cliente: " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void stopServer() {
        super.stopServer();
        try {
            if (this.serverSocket != null && !this.serverSocket.isClosed()) {
                this.serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Erro ao fechar o socket do servidor: " + e.getMessage());
        }
    }

    protected void handleClient(Socket clientSocket, String clientId) {
        boolean criticalSectionEnabled = ServerConfig.getInstance().isCriticalSectionAccessControlEnabled();
        try {
            if (criticalSectionEnabled) {
                lock.lock();
            }
            ProcessBuilder processBuilder = new ProcessBuilder("java", "-cp", System.getProperty("java.class.path"), "server.ProcessServer$ClientHandler");
            Process process = processBuilder.start();
            PrintWriter processOut = new PrintWriter(new OutputStreamWriter(process.getOutputStream()), true);
            BufferedReader processIn = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader clientIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter clientOut = new PrintWriter(clientSocket.getOutputStream(), true);
            String clientMessage;
            while ((clientMessage = clientIn.readLine()) != null) {
                log(clientId, "Mensagem recebida: " + clientMessage);
                processOut.println(clientMessage);
                String response = processIn.readLine();
                clientOut.println(response);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (criticalSectionEnabled) {
                lock.unlock();
            }
            try {
                clientSocket.close();
                log(clientId, "Cliente desconectado.\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static class ClientHandler {
        public static void main(String[] args) {
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            PrintWriter out = new PrintWriter(System.out, true);

            String clientMessage;
            try {
                while ((clientMessage = in.readLine()) != null) {
                    String response = processCommand(clientMessage);
                    out.println(response);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private static String processCommand(String command) {
            String[] parts = command.split(" ");
            String cmd = parts[0];
            if ("READ".equalsIgnoreCase(cmd)) {
                int position = Integer.parseInt(parts[1]);
                // Implementar a lógica de leitura (por exemplo, ler de um array ou banco de dados)
                return "Dados lidos da posição " + position;
            } else if ("WRITE".equalsIgnoreCase(cmd)) {
                int position = Integer.parseInt(parts[1]);
                // Implementar a lógica de escrita
                return "Dados escritos na posição " + position;
            } else {
                return "Comando desconhecido.";
            }
        }
    }
}