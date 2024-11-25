package server;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import static util.Log.log;

public class LoadBalancer {
    private final ConcurrentHashMap<Integer, WorkerStatus> workerStatus;
    private final Queue<Integer> availableWorkers;
    private final int basePort;
    private final int numWorkers;
    private final AtomicInteger currentConnections;
    private final int maxConnectionsPerWorker;

    public LoadBalancer(int basePort, int numWorkers, int maxConnectionsPerWorker) {
        this.basePort = basePort;
        this.numWorkers = numWorkers;
        this.maxConnectionsPerWorker = maxConnectionsPerWorker;
        this.workerStatus = new ConcurrentHashMap<>();
        this.availableWorkers = new ConcurrentLinkedQueue<>();
        this.currentConnections = new AtomicInteger(0);

        initializeWorkers();
    }

    private void initializeWorkers() {
        for (int i = 0; i < numWorkers; i++) {
            int port = basePort + i + 1;
            workerStatus.put(port, new WorkerStatus());
            availableWorkers.offer(port);
        }
    }

    public synchronized int getNextWorkerPort() {
        // Primeiro, tenta obter um worker disponível
        Integer port = availableWorkers.poll();
        if (port == null) {
            // Se não houver workers disponíveis, encontra o menos carregado
            port = findLeastLoadedWorker();
        }

        WorkerStatus status = workerStatus.get(port);
        status.incrementConnections();

        // Se ainda não atingiu o limite, coloca de volta na fila
        if (status.getCurrentConnections() < maxConnectionsPerWorker) {
            availableWorkers.offer(port);
        }

        return port;
    }

    private int findLeastLoadedWorker() {
        return workerStatus.entrySet().stream()
                .min((e1, e2) -> Integer.compare(
                        e1.getValue().getCurrentConnections(),
                        e2.getValue().getCurrentConnections()))
                .map(e -> e.getKey())
                .orElse(basePort + 1);
    }

    public void markWorkerAsFailed(int port) {
        WorkerStatus status = workerStatus.get(port);
        if (status != null) {
            status.setFailed(true);
            availableWorkers.remove(port);
        }
    }

    public void markWorkerAsRecovered(int port) {
        WorkerStatus status = workerStatus.get(port);
        if (status != null) {
            status.setFailed(false);
            if (status.getCurrentConnections() < maxConnectionsPerWorker) {
                availableWorkers.offer(port);
            }
        }
    }

    public void releaseWorker(int port) {
        WorkerStatus status = workerStatus.get(port);
        if (status != null) {
            status.decrementConnections();
            if (!status.isFailed() && status.getCurrentConnections() < maxConnectionsPerWorker) {
                availableWorkers.offer(port);
            }
        }
    }

    public boolean isWorkerHealthy(int port) {
        WorkerStatus status = workerStatus.get(port);
        return status != null && !status.isFailed();
    }

    private static class WorkerStatus {
        private final AtomicInteger connections;
        private volatile boolean failed;

        public WorkerStatus() {
            this.connections = new AtomicInteger(0);
            this.failed = false;
        }

        public void incrementConnections() {
            connections.incrementAndGet();
        }

        public void decrementConnections() {
            connections.decrementAndGet();
        }

        public int getCurrentConnections() {
            return connections.get();
        }

        public boolean isFailed() {
            return failed;
        }

        public void setFailed(boolean failed) {
            this.failed = failed;
        }
    }
}
