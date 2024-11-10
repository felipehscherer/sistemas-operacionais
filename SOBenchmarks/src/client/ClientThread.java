package client;

import server.ServerConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClientThread implements Runnable {
    private Client cliente;
    private int numReads;
    private int numWrites;
    private String operationSequence; // Ex: "RW", "WR", "RWRW"

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
                log("WRITE response: " + cliente.write(writeCount));
                writeCount++;
            }
        }
    }

    protected static void log(String message) {
        if (ServerConfig.getInstance().isPrintLogEnabled()) {
            System.out.println(message);
        }
    }

    public static void runMultipleClients(String host, int port, int numClients, int numReads, int numWrites, String operationSequence) {
        List<Thread> clientThreads = new ArrayList<>();

        for (int i = 0; i < numClients; i++) {
            ClientThread clientThread = new ClientThread(host, port, numReads, numWrites, operationSequence);
            log("Instanciou client thread");
            Thread thread = new Thread(clientThread);
            clientThreads.add(thread);
            thread.start();
        }

        // Aguarda todas as threads finalizarem
        for (Thread thread : clientThreads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                System.err.println("Error while waiting for client threads to finish: " + e.getMessage());
            }
        }

        log("All client threads have finished.");
    }
}
