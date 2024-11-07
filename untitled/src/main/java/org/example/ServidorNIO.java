package org.example;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

public class ServidorNIO {
    private static final int PORT = 12345;
    private static int[] database;
    private static boolean enableMessages = true;
    private static boolean enableConcurrencyControl = true;

    public static void main(String[] args) {
        // Parâmetros de execução
        int databaseSize = 100; // Tamanho do vetor
        parseArguments(args);

        // Inicializar o vetor com zeros
        database = new int[databaseSize];
        Arrays.fill(database, 0);

        if (enableMessages)
            System.out.println("Servidor NIO iniciado na porta " + PORT);

        try {
            Selector selector = Selector.open();
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
            serverChannel.bind(new InetSocketAddress(PORT));
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            while (true) {
                selector.select(); // Bloqueia até que um evento ocorra
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();

                while (iter.hasNext()) {
                    SelectionKey key = iter.next();

                    if (key.isAcceptable()) {
                        register(selector, serverChannel);
                    }

                    if (key.isReadable()) {
                        answerWithEcho(key);
                    }
                    iter.remove();
                }
            }
        } catch (IOException e) {
            System.err.println("Erro no servidor NIO: " + e.getMessage());
        } finally {
            // Imprimir o somatório das posições do vetor
            int sum = Arrays.stream(database).sum();
            System.out.println("Somatório das posições do vetor: " + sum);
        }
    }

    private static void register(Selector selector, ServerSocketChannel serverSocket)
            throws IOException {
        SocketChannel client = serverSocket.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(256));
        if (enableMessages)
            System.out.println("Cliente conectado: " + client.getRemoteAddress());
    }

    private static void answerWithEcho(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(256);
        int bytesRead = client.read(buffer);

        if (bytesRead == -1) {
            client.close();
            if (enableMessages)
                System.out.println("Cliente desconectado.");
            return;
        }

        String input = new String(buffer.array()).trim();
        if (enableMessages)
            System.out.println("Recebido: " + input);

        String[] parts = input.split(" ");
        String command = parts[0];
        int index = Integer.parseInt(parts[1]);
        String response = "";

        switch (command.toUpperCase()) {
            case "READ":
                int value = database[index];
                response = "VALOR " + value;
                break;
            case "WRITE":
                if (enableConcurrencyControl) {
                    synchronized (database) {
                        database[index] += 1;
                    }
                } else {
                    database[index] += 1;
                }
                response = "OK";
                break;
            default:
                response = "COMANDO INVÁLIDO";
        }

        buffer.clear();
        buffer.put(response.getBytes());
        buffer.flip();
        client.write(buffer);
    }

    // Método para analisar os argumentos de execução
    private static void parseArguments(String[] args) {
        for (String arg : args) {
            if (arg.equals("--disable-messages")) {
                enableMessages = false;
            } else if (arg.equals("--disable-concurrency-control")) {
                enableConcurrencyControl = false;
            }
        }
    }
}
