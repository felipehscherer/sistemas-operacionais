package client;

import server.ServerConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClientThread implements Runnable {
    private Client cliente;
    private int numReads;
    private int numWrites;
    private String operationSequence;

    public ClientThread(String host, int port, int numReads, int numWrites, String operationSequence) {
        this.cliente = new Client(host, port);
        this.numReads = numReads;
        this.numWrites = numWrites;
        this.operationSequence = operationSequence;
    }

    @Override
    public void run() {
        log("Iniciando client thread");
        try {
            cliente.connect();
            performOperations();
            cliente.disconnect();
        } catch (IOException e) {
            System.err.println("Error in client thread: " + e.getMessage());
        }
    }

    private void performOperations() throws IOException {
        int readCount = 0;
        int writeCount = 0;

        for (char operation : operationSequence.toCharArray()) {
            if (operation == 'R' && readCount < numReads) {
                log("READ response: " + cliente.read(readCount));
                readCount++;
            } else if (operation == 'W' && writeCount < numWrites) {
                log("WRITE response: " + cliente.write(writeCount, 1)); // Escreve o valor 1
                writeCount++;
            }
        }
    }

    protected static void log(String message) {
        if (ServerConfig.getInstance().isPrintLogEnabled()) {
            System.out.println(message);
        }
    }

    public static void runMultipleClients(String host, int port, int numClients, int numReads, int numWrites,
                                          String operationSequence) {
        List<Thread> clientThreads = new ArrayList<>();

        for (int i = 0; i < numClients; i++) {
            ClientThread clientThread = new ClientThread(host, port, numReads, numWrites, operationSequence);
            log("Instanciou client thread");
            Thread thread = new Thread(clientThread);
            clientThreads.add(thread);
            thread.start();
        }

        for (Thread thread : clientThreads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                System.err.println("Error while waiting for client threads to finish: " + e.getMessage());
            }
        }

        log("All client threads have finished.");
    }

    public static void main(String[] args) {
        if (args.length < 6) {
            System.out.println("Uso: java client.ClientThread <host> <port> <numClients> <numReads> <numWrites> <operationSequence>");
            System.out.println("Exemplo: ");
            return;
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        int numClients = Integer.parseInt(args[2]);
        int numReads = Integer.parseInt(args[3]);
        int numWrites = Integer.parseInt(args[4]);
        String operationSequence = args[5];
        runMultipleClients(host, port, numClients, numReads, numWrites, operationSequence);
    }
}