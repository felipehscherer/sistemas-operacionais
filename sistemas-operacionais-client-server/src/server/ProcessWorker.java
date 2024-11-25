package server;

import java.io.*;
import java.net.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.StandardOpenOption;
import static util.Log.log;

public class ProcessWorker {
    private final int port;
    private final int size;
    private final String sharedMemoryFile;
    private final boolean useMutex;
    private final boolean useLog;
    private MappedByteBuffer sharedMemory;
    private FileChannel fileChannel;

    public ProcessWorker(int port, int size, String sharedMemoryFile, boolean useMutex, boolean useLog) {
        this.port = port;
        this.size = size;
        this.sharedMemoryFile = sharedMemoryFile;
        this.useMutex = useMutex;
        this.useLog = useLog;
    }

    public void start() {
        try {
            initializeSharedMemory();
            startServer();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void initializeSharedMemory() throws IOException {
        fileChannel = FileChannel.open(
                new File(sharedMemoryFile).toPath(),
                StandardOpenOption.READ,
                StandardOpenOption.WRITE
        );

        sharedMemory = fileChannel.map(
                FileChannel.MapMode.READ_WRITE,
                0,
                size * 4
        );
    }

    private void startServer() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Worker process started on port " + port);

            while (true) {
                try (Socket clientSocket = serverSocket.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                     PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        String[] parts = inputLine.split(" ");
                        String operation = parts[0];
                        int position = Integer.parseInt(parts[1]);

                        if (position >= size) {
                            out.println("ERROR: Invalid position");
                            continue;
                        }

                        handleOperation(operation, position, out);
                    }
                } catch (IOException e) {
                    if (useLog) {
                        System.err.println("Error handling client connection: " + e.getMessage());
                    }
                }
            }
        }
    }

    private void handleOperation(String operation, int position, PrintWriter out) throws IOException {
        FileLock lock = null;
        try {
            // Adquirir lock se necessário
            if (useMutex) {
                lock = fileChannel.lock(position * 4, 4, false);
            }

            if (operation.equals("READ")) {
                int value = sharedMemory.getInt(position * 4);
                out.println(value);
                if (useLog) {
                    log("READ at position " + position + ": " + value);
                }
            } else if (operation.equals("WRITE")) {
                int currentValue = sharedMemory.getInt(position * 4);
                sharedMemory.putInt(position * 4, currentValue + 1);
                out.println("OK");
                if (useLog) {
                    log("WRITE at position " + position);
                }
            }
        } finally {
            if (lock != null) {
                lock.release();
            }
        }
    }

    public static void main(String[] args) {
        if (args.length != 5) {
            System.out.println("Usage: java ProcessWorker <port> <size> <shared_memory_file> <use_mutex> <use_log>");
            return;
        }

        int port = Integer.parseInt(args[0]);
        int size = Integer.parseInt(args[1]);
        String sharedMemoryFile = args[2];
        boolean useMutex = Boolean.parseBoolean(args[3]);
        boolean useLog = Boolean.parseBoolean(args[4]);

        ProcessWorker worker = new ProcessWorker(port, size, sharedMemoryFile, useMutex, useLog);
        worker.start();
    }
}
