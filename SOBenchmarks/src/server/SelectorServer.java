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

    public SelectorServer(int port) throws IOException {
        super(port);
        selector = Selector.open();
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(port));
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    @Override
    public void runServer() {
        log("Servidor SelectorServer iniciado na porta " + port);
        while (true) {
            try {
                selector.select();
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();

                while (iter.hasNext()) {
                    SelectionKey key = iter.next();

                    if (key.isAcceptable()) {
                        register(selector, key);
                    }

                    if (key.isReadable()) {
                        handleClient(key);
                    }

                    iter.remove();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
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
            String clientMessage = new String(clientMessageBytes);
            String clientId = String.valueOf(clientChannel.getRemoteAddress().toString().split(":")[1]);
            log(clientId, "Mensagem recebida: " + clientMessage);
            String response = "Mensagem recebida: " + clientMessage;
            buffer.clear();
            buffer.put(response.getBytes());
            buffer.flip();
            while (buffer.hasRemaining()) {
                clientChannel.write(buffer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (criticalSectionEnabled) {
                lock.unlock();
            }
        }
    }
}