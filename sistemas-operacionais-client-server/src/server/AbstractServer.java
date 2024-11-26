package server;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.concurrent.atomic.AtomicInteger;

import static util.Log.log;

public abstract class AbstractServer {
    protected final int port;
    protected int[] database;
    protected boolean useMutex;
    public static boolean log;
    protected ReentrantLock[] locks;
    protected ExecutorService threadPool;
    protected AtomicInteger activeConnections;
    protected long lastActivityTime;
    protected final Object timeLock;

    public static boolean isLog() {
        return log;
    }

    protected AbstractServer(int port) {
        this.port = port;
        this.activeConnections = new AtomicInteger(0);
        this.timeLock = new Object();
    }

    protected abstract void initializeDatabase(int size);
    protected abstract void handleClientOperation(String operation, int position, PrintWriter out);
    protected abstract void printSummary();

    protected void start(String[] args) {
        if (!validateArgs(args)) {
            return;
        }

        int size = Integer.parseInt(args[0]);
        useMutex = Boolean.parseBoolean(args[1]);
        log = Boolean.parseBoolean(args[2]);

        initializeDatabase(size);
        initializeThreadPool();
        startMonitorThread();

        System.out.println("Servidor iniciado na porta " + port);
        System.out.println("Usando mutex: " + useMutex);
        System.out.println("Usando log: " + log);

        startServerSocket();
    }

    protected boolean validateArgs(String[] args) {
        if (args.length != 3) {
            System.out.println("Uso: java server/ThreadServer <tamanho_vetor> <usar_mutex>(true/false) <log>(true/false)");
            return false;
        }
        return true;
    }

    protected void initializeThreadPool() {
        //threadPool = Executors.newCachedThreadPool();
        threadPool = Executors.newFixedThreadPool(1000); // Para testar altas conexões
        updateLastActivityTime();
    }

    protected void startMonitorThread() {
        Thread monitorThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(2000);
                    synchronized (timeLock) {
                        if (System.currentTimeMillis() - lastActivityTime > 3000 &&
                                activeConnections.get() == 0) {
                            printSummary();
                            Thread.sleep(1000);
                            //log("\nServidor aguardando novas conexões...");
                        }
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    protected void startServerSocket() {
        //Claudinho disse que esse integer.maxvalues aí pode causar problemas
        try (ServerSocket serverSocket = new ServerSocket(port, 100000)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                activeConnections.incrementAndGet();
                updateLastActivityTime();
                threadPool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void updateLastActivityTime() {
        synchronized (timeLock) {
            lastActivityTime = System.currentTimeMillis();
        }
    }

    protected class ClientHandler implements Runnable {
        private final Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
            ) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    updateLastActivityTime();
                    String[] parts = inputLine.split(" ");
                    String operation = parts[0];
                    int position = Integer.parseInt(parts[1]);

                    if (position >= database.length) {
                        out.println("ERROR: Posição inválida");
                        continue;
                    }

                    handleClientOperation(operation, position, out);
                }
            } catch (IOException e) {
                System.out.println("Erro na conexão com cliente: " + e.getMessage());
            } finally {
                activeConnections.decrementAndGet();
                updateLastActivityTime();
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}