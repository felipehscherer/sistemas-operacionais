package server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

public class SelectorServer extends Server {
    private Selector selector;
    private ReentrantLock lock = new ReentrantLock();
    private static final int BUFFER_SIZE = 256;
    private static final int SELECTOR_TIMEOUT = 500;

    public SelectorServer(int port, int databaseSize) throws IOException {
        super(port, databaseSize);
        this.selector = Selector.open();
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(port));
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    @Override
    public void runServer() {
        log("Servidor SelectorServer iniciado na porta " + port);

        while (running) {
            try {
                int readyChannels = selector.select(SELECTOR_TIMEOUT);

                if (readyChannels == 0) {
                    continue;
                }

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();

                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    iter.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isAcceptable()) {
                        registerClient(selector, key);
                    } else if (key.isReadable()) {
                        handleRead(key);
                    } else if (key.isWritable()) {
                        handleWrite(key);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void stopServer() {
        super.stopServer();
        try {
            if (selector != null) {
                selector.close();
            }
        } catch (IOException e) {
            System.err.println("Erro ao fechar o selector: " + e.getMessage());
        }
    }

    private void registerClient(Selector selector, SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientSocket = serverSocketChannel.accept();
        clientSocket.configureBlocking(false);

        // Attach a new buffer for this client
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        clientSocket.register(selector, SelectionKey.OP_READ, buffer);

        String clientId = String.valueOf(clientSocket.getRemoteAddress().toString().split(":")[1]);
        log(clientId, "Cliente conectado");
    }

    private void handleRead(SelectionKey key) {
        boolean criticalSectionEnabled = ServerConfig.getInstance().isCriticalSectionAccessControlEnabled();
        if (criticalSectionEnabled) {
            lock.lock();
        }

        try {
            SocketChannel clientChannel = (SocketChannel) key.channel();
            ByteBuffer buffer = (ByteBuffer) key.attachment();

            buffer.clear();
            int bytesRead = clientChannel.read(buffer);
            if (bytesRead == -1) {
                clientChannel.close();
                key.cancel();
                log("Cliente desconectado");
                return;
            }

            buffer.flip();
            byte[] clientMessageBytes = new byte[buffer.remaining()];
            buffer.get(clientMessageBytes);
            String clientMessage = new String(clientMessageBytes).trim();
            String clientId = String.valueOf(clientChannel.getRemoteAddress().toString().split(":")[1]);
            log(clientId, "Mensagem recebida: " + clientMessage);

            // Process command and prepare response
            String response = processCommand(clientMessage);
            buffer.clear();
            buffer.put(response.getBytes());
            buffer.flip();

            // Register interest in writing
            key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
        } catch (IOException e) {
            e.printStackTrace();
            try {
                key.channel().close();
                key.cancel();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } finally {
            if (criticalSectionEnabled) {
                lock.unlock();
            }
        }
    }

    private void handleWrite(SelectionKey key) {
        try {
            SocketChannel clientChannel = (SocketChannel) key.channel();
            ByteBuffer buffer = (ByteBuffer) key.attachment();

            while (buffer.hasRemaining()) {
                clientChannel.write(buffer);
            }

            // Once response is sent, switch back to reading mode
            key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            buffer.clear();
        } catch (IOException e) {
            e.printStackTrace();
            try {
                key.channel().close();
                key.cancel();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private String processCommand(String command) {
        String[] parts = command.split(" ");
        String cmd = parts[0];
        if ("READ".equalsIgnoreCase(cmd)) {
            int position = Integer.parseInt(parts[1].trim());
            return "Dados lidos da posição " + position + ": " + database.read(position);
        } else if ("WRITE".equalsIgnoreCase(cmd)) {
            int position = Integer.parseInt(parts[1].trim());
            int value = Integer.parseInt(parts[2].trim());
            database.write(position, value);
            return "Dados escritos na posição " + position;
        } else {
            return "Comando desconhecido.";
        }
    }
}
