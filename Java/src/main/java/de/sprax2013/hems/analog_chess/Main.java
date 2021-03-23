package de.sprax2013.hems.analog_chess;

import java.io.IOException;

public class Main {
    private static Serial serial;

    public static void main(String[] args) throws IOException {
        serial = new Serial();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                serial.stop();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }));

        serial.promptPortSelection();

        if (serial.getPort() != null) {
            serial.start(14400);
        }

        new ChessGui(new ChessGame());
    }
}