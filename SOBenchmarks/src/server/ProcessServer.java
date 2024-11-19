package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;
import database.*;

public class ProcessServer extends Server {
    private ReentrantLock lock = new ReentrantLock();
    private ServerSocket serverSocket;

    public ProcessServer(int port, int databaseSize) {
        super(port, databaseSize);
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
            BufferedReader clientIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter clientOut = new PrintWriter(clientSocket.getOutputStream(), true);
            String clientMessage;
            while ((clientMessage = clientIn.readLine()) != null) {
                log(clientId, "Mensagem recebida: " + clientMessage);
                String response = processCommand(clientMessage);
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
                log(clientId, "Cliente desconectado.");
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
            return "Dados lidos da posição " + position + ": " + database.read(position);
        } else if ("WRITE".equalsIgnoreCase(cmd)) {
            int position = Integer.parseInt(parts[1]);
            int value = Integer.parseInt(parts[2]);
            database.write(position, value);
            return "Dados escritos na posição " + position;
        } else {
            return "Comando desconhecido.";
        }
    }
}