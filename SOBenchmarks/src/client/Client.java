package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import static client.ClientThread.log;

public class Client {
    private String host;
    private int port;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public Client(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void connect() throws IOException {
        log("Iniciando conexão do cliente");
        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        log("Connected to the server at " + host + ":" + port);
    }

    public String sendCommand(String command) throws IOException {
        out.println(command);
        return in.readLine(); // Read the response from the server
    }

    public void disconnect() throws IOException {
        if (socket != null) {
            socket.close();
            log("Disconnected from the server.");
        }
    }

    public String read(int position) throws IOException {
        return sendCommand("READ " + position);
    }

    public String write(int position) throws IOException {
        return sendCommand("WRITE " + position);
    }
}
