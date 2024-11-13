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

    public SelectorServer(int port, int databaseSize) throws IOException {
        super(port, databaseSize);
        selector = Selector.open();
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
                selector.select();
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();

                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    iter.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isAcceptable()) {
                        register(selector, key);
                    }

                    if (key.isReadable()) {
                        handleClient(key);
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

    private void register(Selector selector, SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientSocket = serverSocketChannel.accept();
        clientSocket.configureBlocking(false);
        clientSocket.register(selector, SelectionKey.OP_READ);
        String clientId = String.valueOf(clientSocket.getRemoteAddress().toString().split(":")[1]);
        log(clientId, "Cliente conectado");
    }

    protected void handleClient(SelectionKey key) {
        boolean criticalSectionEnabled = ServerConfig.getInstance().isCriticalSectionAccessControlEnabled();
        if (criticalSectionEnabled) {
            lock.lock();
        }
        try {
            SocketChannel clientChannel = (SocketChannel) key.channel();
            ByteBuffer buffer = ByteBuffer.allocate(256);
            int bytesRead = clientChannel.read(buffer);
            if (bytesRead == -1) {
                clientChannel.close();
                log("Cliente desconectado");
                return;
            }
            buffer.flip();
            byte[] clientMessageBytes = new byte[buffer.remaining()];
            buffer.get(clientMessageBytes);
            String clientMessage = new String(clientMessageBytes).trim(); // Use trim() to remove whitespace
            String clientId = String.valueOf(clientChannel.getRemoteAddress().toString().split(":")[1]);
            log(clientId, "Mensagem recebida: " + clientMessage);
            String response = processCommand(clientMessage);
            buffer.clear();
            buffer.put(response.getBytes());
            buffer.flip();
            while (buffer.hasRemaining()) {
                clientChannel.write(buffer);
            }
        } catch (IOException e) {
            e.printStackTrace();
            try {
                key.channel().close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } finally {
            if (criticalSectionEnabled) {
                lock.unlock();
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