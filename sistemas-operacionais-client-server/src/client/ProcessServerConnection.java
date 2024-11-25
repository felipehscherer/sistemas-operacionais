package client;

import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicInteger;
import static util.Log.log;

class ProcessServerConnection implements ServerConnection {
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 100;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final String host;
    private final LoadBalancerClient loadBalancer;
    private int currentPort;
    private final AtomicInteger pendingOperations;

    public ProcessServerConnection(String host, LoadBalancerClient loadBalancer) throws IOException {
        this.host = host;
        this.loadBalancer = loadBalancer;
        this.pendingOperations = new AtomicInteger(0);
        connectToWorker();
    }

    private void connectToWorker() throws IOException {
        currentPort = loadBalancer.getNextWorkerPort();
        try {
            socket = new Socket(host, currentPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            loadBalancer.reportWorkerFailure(currentPort);
            throw e;
        }
    }

    @Override
    public synchronized String executeOperation(String operation) throws IOException {
        pendingOperations.incrementAndGet();
        int retries = 0;

        while (retries < MAX_RETRIES) {
            try {
                out.println(operation);
                String response = in.readLine();

                if (response != null) {
                    pendingOperations.decrementAndGet();
                    return response;
                }

                // Se a resposta for null, provavelmente o worker falhou
                throw new IOException("Worker não respondeu");
            } catch (IOException e) {
                loadBalancer.reportWorkerFailure(currentPort);

                if (retries < MAX_RETRIES - 1) {
                    // Tenta reconectar com outro worker
                    try {
                        close();
                        Thread.sleep(RETRY_DELAY_MS * (retries + 1));
                        connectToWorker();
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Interrupted during retry", ie);
                    }
                }
                retries++;
            }
        }

        pendingOperations.decrementAndGet();
        throw new IOException("Failed to execute operation after " + MAX_RETRIES + " retries");
    }

    @Override
    public void close() throws IOException {
        // Espera todas as operações pendentes terminarem
        while (pendingOperations.get() > 0) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (in != null) in.close();
        if (out != null) out.close();
        if (socket != null) {
            loadBalancer.releaseWorker(currentPort);
            socket.close();
        }
    }
}

class LoadBalancerClient {
    private static final int BASE_PORT = 12345;
    private static final int NUM_WORKERS = 4;
    private static final int MAX_CONNECTIONS_PER_WORKER = 250; // Ajuste conforme necessário
    private final server.LoadBalancer loadBalancer;

    public LoadBalancerClient() {
        this.loadBalancer = new server.LoadBalancer(BASE_PORT, NUM_WORKERS, MAX_CONNECTIONS_PER_WORKER);
    }

    public int getNextWorkerPort() {
        return loadBalancer.getNextWorkerPort();
    }

    public void reportWorkerFailure(int port) {
        loadBalancer.markWorkerAsFailed(port);
    }

    public void releaseWorker(int port) {
        loadBalancer.releaseWorker(port);
    }
}

class ProcessServerConnectionFactory extends ServerConnectionFactory {
    private final LoadBalancerClient loadBalancer;

    public ProcessServerConnectionFactory() {
        this.loadBalancer = new LoadBalancerClient();
    }

    @Override
    public ServerConnection createConnection() throws IOException {
        return new ProcessServerConnection(SERVER_HOST, loadBalancer);
    }
}