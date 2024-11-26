package server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import static util.Log.log;

public class SelectorServer extends AbstractServer {
    private static final int DEFAULT_PORT = 12345;
    private Selector selector;

    public SelectorServer() {
        super(DEFAULT_PORT);
    }

    public SelectorServer(int port) {
        super(port);
    }

    public static void main(String[] args) {
        SelectorServer server = new SelectorServer();
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
        // Não utilizado no servidor por seleção
    }

    @Override
    protected void printSummary() {
        int sum = 0;
        log("\n=== SUMÁRIO DA EXECUÇÃO ===");
        log("Conteúdo final do vetor:");
        for (int i = 0; i < database.length; i++) {
            sum += database[i];
        }
        System.out.println("\nSomatório total do vetor: " + sum);
        log("==========================");
    }

    @Override
    protected void startServerSocket() {
        try (ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()) {
            serverSocketChannel.bind(new InetSocketAddress(port));
            serverSocketChannel.configureBlocking(false);
            selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("Servidor iniciado na porta " + port);

            while (true) {
                selector.select();
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectedKeys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    if (key.isAcceptable()) {
                        handleAccept(serverSocketChannel);
                    } else if (key.isReadable()) {
                        handleRead(key);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleAccept(ServerSocketChannel serverSocketChannel) throws IOException {
        SocketChannel clientChannel = serverSocketChannel.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ);
        activeConnections.incrementAndGet();
        updateLastActivityTime();
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(256);
        int bytesRead = clientChannel.read(buffer);

        if (bytesRead == -1) {
            clientChannel.close();
            activeConnections.decrementAndGet();
            updateLastActivityTime();
            return;
        }

        String request = new String(buffer.array()).trim();
        String[] parts = request.split(" ");
        String operation = parts[0];
        int position = Integer.parseInt(parts[1]);

        if (position >= database.length) {
            clientChannel.write(ByteBuffer.wrap("ERROR: Posição inválida\n".getBytes()));
            return;
        }

        if (operation.equals("READ")) {
            handleRead(position, clientChannel);
        } else if (operation.equals("WRITE")) {
            handleWrite(position, clientChannel);
        }
    }

    private void handleRead(int position, SocketChannel clientChannel) throws IOException {
        if (useMutex) {
            locks[position].lock();
            try {
                clientChannel.write(ByteBuffer.wrap((database[position] + "\n").getBytes()));
            } finally {
                locks[position].unlock();
            }
        } else {
            clientChannel.write(ByteBuffer.wrap((database[position] + "\n").getBytes()));
        }
    }

    private void handleWrite(int position, SocketChannel clientChannel) throws IOException {
        if (useMutex) {
            locks[position].lock();
            try {
                database[position]++;
                clientChannel.write(ByteBuffer.wrap("OK\n".getBytes()));
            } finally {
                locks[position].unlock();
            }
        } else {
            database[position]++;
            clientChannel.write(ByteBuffer.wrap("OK\n".getBytes()));
        }
    }
}