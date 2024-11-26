package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

// Implementação para servidor de seleção
public class SelectorServerConnection implements ServerConnection {
    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;

    public SelectorServerConnection(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    @Override
    public String executeOperation(String operation) throws IOException {
        out.println(operation);
        return in.readLine();
    }

    @Override
    public void close() throws IOException {
        in.close();
        out.close();
        socket.close();
    }
}