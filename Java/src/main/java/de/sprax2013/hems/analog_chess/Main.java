package de.sprax2013.hems.analog_chess;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

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

//            System.out.println(bytesToHex(serial.requestBoard()));
            System.out.println(Arrays.toString(serial.requestBoard()));
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

    private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);

    private static String bytesToHex(byte[] bytes) {
        byte[] hexChars = new byte[bytes.length * 2];

        for (int j = 0; j < bytes.length; ++j) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }

        return new String(hexChars, StandardCharsets.UTF_8);
    }
}