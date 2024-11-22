package util;

import server.AbstractServer;

public class Log {

    public static void log(String message) {
        if (AbstractServer.isLog()) {
            System.out.println(message);
        }
    }

    public static void log(String message, int x, int y) { // usando esse pra alguns logs com parametros
        if (AbstractServer.isLog()) {
            System.out.println(message);
        }
    }

}
