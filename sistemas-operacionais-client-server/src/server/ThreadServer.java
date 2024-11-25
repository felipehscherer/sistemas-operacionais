package server;

import java.io.PrintWriter;
import java.util.concurrent.locks.ReentrantLock;

import static util.Log.log;

public class ThreadServer extends AbstractServer {
    private static final int DEFAULT_PORT = 12345;

    public ThreadServer() {
        super(DEFAULT_PORT);
    }

    public ThreadServer(int port) {
        super(port);
    }

    public static void main(String[] args) {
        ThreadServer server = new ThreadServer();
        server.start(args);
    }

    @Override
    protected void initializeDatabase(int size) {
        database = new int[size];
        locks = new ReentrantLock[size];
        for (int i = 0; i < size; i++) {
            locks[i] = new ReentrantLock();
        }
    }

    @Override
    protected void handleClientOperation(String operation, int position, PrintWriter out) {
        if (operation.equals("READ")) {
            handleRead(position, out);
        } else if (operation.equals("WRITE")) {
            handleWrite(position, out);
        }
    }

    private void handleRead(int position, PrintWriter out) {
        if (useMutex) {
            locks[position].lock();
            try {
                out.println(database[position]);
            } finally {
                locks[position].unlock();
            }
        } else {
            out.println(database[position]);
        }
    }

    private void handleWrite(int position, PrintWriter out) {
        if (useMutex) {
            locks[position].lock();
            try {
                database[position]++;
                out.println("OK");
            } finally {
                locks[position].unlock();
            }
        } else {
            database[position]++;
            out.println("OK");
        }
    }

    @Override
    protected void printSummary() {
        int sum = 0;
        log("\n=== SUMÁRIO DA EXECUÇÃO ===");
        log("Conteúdo final do vetor:");
        for (int i = 0; i < database.length; i++) {
            //log("Posição %d: %d%n", i, database[i]);
            sum += database[i];
        }
        System.out.println("\nSomatório total do vetor: " + sum); // deixei pra sempre vermos o resultado
        log("==========================");
    }
}