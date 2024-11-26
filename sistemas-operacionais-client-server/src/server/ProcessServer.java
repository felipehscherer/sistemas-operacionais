package server;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import static util.Log.log;

public class ProcessServer extends AbstractServer {
    private static final int DEFAULT_PORT = 12345;
    private static final int NUM_WORKER_PROCESSES = 6;
    private static final int MAX_CONNECTIONS_PER_WORKER = 500;
    private static final String SHARED_MEMORY_FILE = "shared_memory.dat";

    private MappedByteBuffer sharedMemory;
    private FileChannel fileChannel;
    private List<Process> workerProcesses;
    private LoadBalancer loadBalancer;

    public ProcessServer() {
        super(DEFAULT_PORT);
        this.workerProcesses = new ArrayList<>();
        this.loadBalancer = new LoadBalancer(DEFAULT_PORT, NUM_WORKER_PROCESSES, MAX_CONNECTIONS_PER_WORKER);
    }

    public static void main(String[] args) {
        ProcessServer server = new ProcessServer();
        server.start(args);
    }

    @Override
    protected void initializeDatabase(int size) {
        try {
            // Criar ou abrir arquivo para memória compartilhada
            fileChannel = FileChannel.open(
                    new File(SHARED_MEMORY_FILE).toPath(),
                    StandardOpenOption.READ,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.CREATE
            );

            // Mapear o arquivo para memória
            sharedMemory = fileChannel.map(
                    FileChannel.MapMode.READ_WRITE,
                    0,
                    size * 4
            );

            // Inicializar o array com zeros
            for (int i = 0; i < size; i++) {
                sharedMemory.putInt(i * 4, 0);
            }

            // Iniciar processos workers
            startWorkerProcesses(size);

            // Iniciar monitoramento de saúde dos workers
            startHealthCheck();

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void startWorkerProcesses(int size) throws IOException {
        for (int i = 0; i < NUM_WORKER_PROCESSES; i++) {
            int workerPort = DEFAULT_PORT + i + 1;
            ProcessBuilder pb = new ProcessBuilder(
                    "java",
                    "server.ProcessWorker",
                    String.valueOf(workerPort),
                    String.valueOf(size),
                    SHARED_MEMORY_FILE,
                    String.valueOf(useMutex),
                    String.valueOf(log)
            );

            pb.inheritIO();
            Process process = pb.start();
            workerProcesses.add(process);

            // Aguardar um curto período para garantir que o worker iniciou
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void startHealthCheck() {
        Thread healthCheckThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                checkWorkerHealth();
                try {
                    Thread.sleep(5000); // Verificar a cada 5 segundos
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        healthCheckThread.setDaemon(true);
        healthCheckThread.start();
    }

    private void checkWorkerHealth() {
        for (int i = 0; i < NUM_WORKER_PROCESSES; i++) {
            int workerPort = DEFAULT_PORT + i + 1;
            Process workerProcess = workerProcesses.get(i);

            if (!workerProcess.isAlive()) {
                log("Worker on port " + workerPort + " is down. Attempting restart...");
                loadBalancer.markWorkerAsFailed(workerPort);

                try {
                    // Reiniciar o worker
                    ProcessBuilder pb = new ProcessBuilder(
                            "java",
                            "server.ProcessWorker",
                            String.valueOf(workerPort),
                            String.valueOf(sharedMemory.capacity() / 4),
                            SHARED_MEMORY_FILE,
                            String.valueOf(useMutex),
                            String.valueOf(log)
                    );
                    pb.inheritIO();
                    workerProcess = pb.start();
                    workerProcesses.set(i, workerProcess);

                    // Aguardar um pouco para o worker inicializar
                    Thread.sleep(1000);

                    // Marcar como recuperado se o processo estiver rodando
                    if (workerProcess.isAlive()) {
                        loadBalancer.markWorkerAsRecovered(workerPort);
                        log("Worker on port " + workerPort + " successfully restarted");
                    }
                } catch (IOException | InterruptedException e) {
                    log("Failed to restart worker on port " + workerPort + ": " + e.getMessage());
                }
            }
        }
    }

    @Override
    protected void handleClientOperation(String operation, int position, PrintWriter out) {
        // O servidor principal não lida diretamente com as operações
        out.println("ERROR: Main process doesn't handle operations directly");
    }

    @Override
    protected void printSummary() {
        int sum = 0;
        for (int i = 0; i < sharedMemory.capacity() / 4; i++) {
            int value = sharedMemory.getInt(i * 4);
            sum += value;
        }
        System.out.println("\nSomatório total do vetor: " + sum);
        log("==========================");
    }

    @Override
    protected void start(String[] args) {
        if (!validateArgs(args)) {
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(this::cleanup));
        super.start(args);
    }

    private void cleanup() {
        for (Process process : workerProcesses) {
            process.destroy();
        }

        try {
            if (fileChannel != null) {
                fileChannel.close();
            }
            new File(SHARED_MEMORY_FILE).delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}