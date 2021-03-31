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

//            System.out.println(Arrays.toString(serial.requestBoard()));
        }

//        HemsSerial s = new HemsSerial("ttyUSB0", 14400, 8, 1, 0);
//        if (s.open()) {
//            System.out.println("Sending...");
//            s.write("Huso");
//
//            while (true) {
//                if (s.dataAvailable() > 0) {
//                    System.out.println(s.read());
//                }
//            }
//        }

//        new ChessGui(new ChessGame());
    }
}