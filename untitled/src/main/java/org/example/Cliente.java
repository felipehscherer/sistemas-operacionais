package org.example;

import java.io.*;
import java.net.*;

public class Cliente {
    private static final String HOST = "localhost";
    private static final int PORT = 12345;
    private static boolean enableMessages = true;

    public static void main(String[] args) {
        // Parâmetros de execução
        int numClients = 5;
        int numReads = 10;
        int numWrites = 10;
        parseArguments(args);

        for (int i = 0; i < numClients; i++) {
            new Thread(new ClientEmulator(i, numReads, numWrites)).start();
        }
    }

    static class ClientEmulator implements Runnable {
        private int clientId;
        private int numReads;
        private int numWrites;

        public ClientEmulator(int clientId, int numReads, int numWrites) {
            this.clientId = clientId;
            this.numReads = numReads;
            this.numWrites = numWrites;
        }

        public void run() {
            try (
                    Socket socket = new Socket(HOST, PORT);
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(socket.getInputStream())
                    );
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) {
                // Exemplo de sequência intercalada de operações
                int operations = Math.max(numReads, numWrites);
                for (int i = 0; i < operations; i++) {
                    if (i < numReads) {
                        // Operação de READ
                        String command = "READ " + i; // Acessa o índice 'i' do vetor
                        out.println(command);
                        String response = in.readLine();
                        if (enableMessages)
                            System.out.println("Cliente " + clientId + " recebeu: " + response);
                    }
                    if (i < numWrites) {
                        // Operação de WRITE
                        String command = "WRITE " + i; // Incrementa o índice 'i' do vetor
                        out.println(command);
                        String response = in.readLine();
                        if (enableMessages)
                            System.out.println("Cliente " + clientId + " recebeu: " + response);
                    }
                }
            } catch (IOException e) {
                System.err.println("Erro no cliente " + clientId + ": " + e.getMessage());
            }
        }
    }

    // Método para analisar os argumentos de execução
    private static void parseArguments(String[] args) {
        for (String arg : args) {
            if (arg.equals("--disable-messages")) {
                enableMessages = false;
            }
            // Você pode adicionar mais parâmetros conforme necessário
        }
    }
}
