package server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

public class SelectorServer extends Server{
    private Selector selector;
    private ReentrantLock lock = new ReentrantLock(); // Controle de sessão crítica

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
        log("SelectorServer started on port " + port);
        while (true) {
            try {
                // Seleciona os canais prontos para operações de I/O
                selector.select();
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();

                // Itera sobre as chaves selecionadas
                while (iter.hasNext()) {
                    SelectionKey key = iter.next();

                    if (key.isAcceptable()) {
                        register(selector, key);
                    }

                    if (key.isReadable()) {
                        handleClient(key); // Lida com o cliente quando há dados disponíveis para leitura
                    }

                    iter.remove(); // Remove a chave processada para evitar repetições
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
        log("Client connected: " + clientSocket.getRemoteAddress());
    }

    protected void handleClient(SelectionKey key) {
        boolean criticalSectionEnabled = ServerConfig.getInstance().isCriticalSectionAccessControlEnabled();

        if (criticalSectionEnabled) {
            lock.lock(); // Tenta obter o lock se a sessão crítica estiver habilitada
        }

        try {
            SocketChannel clientChannel = (SocketChannel) key.channel();
            ByteBuffer buffer = ByteBuffer.allocate(256);

            // Lê dados do cliente
            int bytesRead = clientChannel.read(buffer);

            if (bytesRead == -1) {
                clientChannel.close(); // Fecha o canal se o cliente terminou a conexão
                System.out.println("Client disconnected");
                return;
            }

            // Prepara o buffer para leitura de dados recebidos
            buffer.flip();
            byte[] clientMessageBytes = new byte[buffer.remaining()];
            buffer.get(clientMessageBytes);
            String clientMessage = new String(clientMessageBytes);

            log("Received message from client: " + clientMessage);

            // Processa a mensagem (exemplo: apenas devolve a mesma mensagem)
            String response = "Message received: " + clientMessage;
            buffer.clear();
            buffer.put(response.getBytes());
            buffer.flip();

            // Envia a resposta de volta para o cliente
            while (buffer.hasRemaining()) {
                clientChannel.write(buffer);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (criticalSectionEnabled) {
                lock.unlock(); // Libera o lock se a sessão crítica estiver habilitada
            }
        }
    }

    public static void main(String[] args) throws IOException {
        // Aqui a aplicação recebe a configuração global de ServerConfig e roda o SelectorServer
        ServerConfig config = ServerConfig.getInstance();
        SelectorServer selectorServer = new SelectorServer(8080);
        selectorServer.startServer();
    }
}
