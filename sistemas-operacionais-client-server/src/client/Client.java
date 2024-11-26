package client;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.Random;
import static util.Log.log;
import static server.ThreadServer.DATABASE_SIZE_THREAD;
import static server.SelectorServer.DATABASE_SIZE_SELECTOR;
import static server.ProcessServer.DATABASE_SIZE_PROCESS;

public class Client {
    private static CountDownLatch latch;
    private static final Random random = new Random();
    private static final Semaphore connectionSemaphore = new Semaphore(5000);
    private static final int MAX_RETRIES = 15;
    private static final int INITIAL_BACKOFF_MS = 100;
    private static String serverTypeGlobal;

    public static void main(String[] args) {
        if (args.length != 4) {
            System.out.println("Uso: java client/Client <tipo_servidor> <num_clientes> <num_reads> <num_writes>");
            System.out.println("tipo_servidor: THREAD, PROCESS");
            return;
        }

        String serverType = args[0].toUpperCase();
        serverTypeGlobal = serverType;
        int numClients = Integer.parseInt(args[1]);
        int numReads = Integer.parseInt(args[2]);
        int numWrites = Integer.parseInt(args[3]);

        ServerConnectionFactory connectionFactory = ServerConnectionFactory.getFactory(serverType);
        if (connectionFactory == null) {
            System.out.println("Tipo de servidor inválido: " + serverType);
            return;
        }

        System.out.println("Iniciando " + numClients + " clientes");
        System.out.println("Cada cliente realizará " + numReads + " leituras e " + numWrites + " escritas");
        System.out.println("Total de operações de escrita esperadas: " + (numClients * numWrites));

        latch = new CountDownLatch(numClients);
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(numClients, 100));

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numClients; i++) {
            final int clientId = i;
            executor.execute(() -> {
                try {
                    Thread.sleep(random.nextInt(100));
                    new ClientWorker(clientId, numReads, numWrites, connectionFactory).run();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }

        try {
            latch.await();
            long endTime = System.currentTimeMillis();
            System.out.println("Todas as operações completadas em " + (endTime - startTime)/1000 + "s");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        executor.shutdown();
        try {
            executor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    static class ClientWorker implements Runnable {
        private final int clientId;
        private final int numReads;
        private final int numWrites;
        private final ServerConnectionFactory connectionFactory;

        public ClientWorker(int clientId, int numReads, int numWrites, ServerConnectionFactory connectionFactory) {
            this.clientId = clientId;
            this.numReads = numReads;
            this.numWrites = numWrites;
            this.connectionFactory = connectionFactory;
        }

        private ServerConnection connectWithRetry() throws IOException, InterruptedException {
            int retries = 0;
            int backoffMs = INITIAL_BACKOFF_MS;

            while (true) {
                try {
                    connectionSemaphore.acquire();
                    return connectionFactory.createConnection();
                } catch (IOException e) {
                    connectionSemaphore.release();
                    if (retries >= MAX_RETRIES) {
                        throw e;
                    }
                    Thread.sleep(backoffMs + random.nextInt(50));
                    backoffMs *= 2;
                    retries++;
                }
            }
        }

        @Override
        public void run() {
            try {
                ServerConnection connection = connectWithRetry();
                try {
                    // Executa operações intercaladas (R,W,R,W,...)
                    int maxOps = Math.max(numReads, numWrites);
                    for (int i = 0; i < maxOps; i++) {
                        if (i < numReads) {
                            int position = random.nextInt(getDatabaseSize() + 1);
                            String response = connection.executeOperation("READ " + position);
                            log("Cliente " + clientId + " - READ na posição " + position + ": " + response);
                        }

                        if (i < numWrites) {
                            int position = random.nextInt(getDatabaseSize() + 1);
                            String response = connection.executeOperation("WRITE " + position);
                            log("Cliente " + clientId + " - WRITE na posição " + position + ": " + response);
                        }
                    }
                } finally {
                    connectionSemaphore.release();
                    connection.close();
                }
            } catch (Exception e) {
                System.err.println("Erro no cliente " + clientId + ": " + e.getMessage());
            } finally {
                latch.countDown();
            }
        }
    }

    private static int getDatabaseSize() {
        if(serverTypeGlobal.equals("THREAD"))
            return DATABASE_SIZE_THREAD;
        if(serverTypeGlobal.equals("SELECTOR"))
            return DATABASE_SIZE_SELECTOR;
        if(serverTypeGlobal.equals("PROCESS"))
            return DATABASE_SIZE_PROCESS;
        return 0;
    }
}