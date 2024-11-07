package org.example;

import java.io.*;
import java.net.*;

public class ServidorProcessos {
    private static final int PORT = 12346;
    private static int[] database;
    private static boolean enableMessages = true;
    private static boolean enableConcurrencyControl = true;

    public static void main(String[] args) {
        // Parâmetros de execução
        int databaseSize = 100; // Tamanho do vetor
        parseArguments(args);

        // Inicializar o vetor com zeros
        database = new int[databaseSize];
        for (int i = 0; i < database.length; i++) {
            database[i] = 0;
        }

        if (enableMessages)
            System.out.println("Servidor iniciado na porta " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                if (enableMessages)
                    System.out.println("Cliente conectado: " + clientSocket.getInetAddress());

                // Criar um novo processo para cada cliente
                ProcessBuilder pb = new ProcessBuilder(
                        "java", "-cp", ".", "ProcessWorker",
                        String.valueOf(clientSocket.getPort()),
                        String.valueOf(enableMessages),
                        String.valueOf(enableConcurrencyControl)
                );

                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("Erro no servidor: " + e.getMessage());
        } finally {
            // Imprimir o somatório das posições do vetor
            int sum = 0;
            for (int value : database) {
                sum += value;
            }
            System.out.println("Somatório das posições do vetor: " + sum);
        }
    }

    // Classe interna para manipular as conexões dos clientes
    static class ClientHandler implements Runnable {
        private Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try (
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(clientSocket.getInputStream())
                    );
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
            ) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (enableMessages)
                        System.out.println("Recebido: " + inputLine);

                    String[] parts = inputLine.split(" ");
                    String command = parts[0];
                    int index = Integer.parseInt(parts[1]);

                    switch (command.toUpperCase()) {
                        case "READ":
                            int value = database[index];
                            out.println("VALOR " + value);
                            break;
                        case "WRITE":
                            if (enableConcurrencyControl) {
                                synchronized (database) {
                                    database[index] += 1;
                                }
                            } else {
                                database[index] += 1;
                            }
                            out.println("OK");
                            break;
                        default:
                            out.println("COMANDO INVÁLIDO");
                    }
                }
            } catch (IOException e) {
                System.err.println("Erro no cliente: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                    if (enableMessages)
                        System.out.println("Cliente desconectado.");
                } catch (IOException e) {
                    System.err.println("Erro ao fechar o socket do cliente: " + e.getMessage());
                }
            }
        }
    }

    // Método para analisar os argumentos de execução
    private static void parseArguments(String[] args) {
        for (String arg : args) {
            if (arg.equals("--disable-messages")) {
                enableMessages = false;
            } else if (arg.equals("--disable-concurrency-control")) {
                enableConcurrencyControl = false;
            }
        }
    }
}
