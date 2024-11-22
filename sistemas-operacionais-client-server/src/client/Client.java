package client;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.Random;

import static util.Log.log;

public class Client {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 12345;
    private static CountDownLatch latch;
    private static final Random random = new Random();
    private static final Semaphore connectionSemaphore = new Semaphore(50); // Limita conexões simultâneas
    private static final int MAX_RETRIES = 5;
    private static final int INITIAL_BACKOFF_MS = 100;

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Uso: java client/Client <num_clientes> <num_reads> <num_writes>");
            return;
        }

        int numClients = Integer.parseInt(args[0]);
        int numReads = Integer.parseInt(args[1]);
        int numWrites = Integer.parseInt(args[2]);

        System.out.println("Iniciando " + numClients + " clientes");
        System.out.println("Cada cliente realizará " + numReads + " leituras e " + numWrites + " escritas");
        System.out.println("Total de operações de escrita esperadas: " + (numClients * numWrites));

        latch = new CountDownLatch(numClients);
        // Usando um thread pool fixo maior para comportar mais clientes
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(numClients, 100));

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numClients; i++) {
            final int clientId = i;
            executor.execute(() -> {
                try {
                    // Adiciona um pequeno delay aleatório para evitar sobrecarga inicial
                    Thread.sleep(random.nextInt(100));
                    new ClientWorker(clientId, numReads, numWrites).run();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }

        try {
            latch.await();
            long endTime = System.currentTimeMillis();
            System.out.println("Todas as operações completadas em " + (endTime - startTime) + "ms");
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

        public ClientWorker(int clientId, int numReads, int numWrites) {
            this.clientId = clientId;
            this.numReads = numReads;
            this.numWrites = numWrites;
        }

        private Socket connectWithRetry() throws IOException, InterruptedException {
            int retries = 0;
            int backoffMs = INITIAL_BACKOFF_MS;

            while (true) {
                try {
                    connectionSemaphore.acquire();
                    return new Socket(SERVER_HOST, SERVER_PORT);
                } catch (IOException e) {
                    connectionSemaphore.release();
                    if (retries >= MAX_RETRIES) {
                        throw e;
                    }
                    Thread.sleep(backoffMs + random.nextInt(50));
                    backoffMs *= 2; // Backoff exponencial
                    retries++;
                }
            }
        }

        @Override
        public void run() {
            try {
                Socket socket = connectWithRetry();
                try (
                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
                ) {
                    // Executa operações intercaladas (R,W,R,W,...)
                    int maxOps = Math.max(numReads, numWrites);
                    for (int i = 0; i < maxOps; i++) {
                        if (i < numReads) {
                            int position = random.nextInt(1000); // Ajustado para vetor maior
                            out.println("READ " + position);
                            String response = in.readLine();
                            log("Cliente " + clientId + " - READ na posição " + position + ": " + response);
                        }

                        if (i < numWrites) {
                            int position = random.nextInt(1000); // Ajustado para vetor maior
                            out.println("WRITE " + position);
                            String response = in.readLine();
                            log("Cliente " + clientId + " - WRITE na posição " + position + ": " + response);
                        }
                    }
                } finally {
                    connectionSemaphore.release();
                    socket.close();
                }
            } catch (Exception e) {
                System.err.println("Erro no cliente " + clientId + ": " + e.getMessage());
            } finally {
                latch.countDown();
            }
        }
    }
}