package de.sprax2013.hems.analog_chess;

import com.fazecast.jSerialComm.SerialPort;

import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Serial {
    private SerialPort port;
    private ExecutorService thread;
    private byte[] buffer;
    private int bufferIndex;

    public SerialPort getPort() {
        return this.port;
    }

    public void sendLedColor(int ledIndex, Color color) {
        write("LED" + ledIndex + color.getRGB() + ((char) 4));

        // TODO: read response
    }

    public void sendLedShow() {
        write("LED SHOW" + ((char) 4));

        // TODO: read response
    }

    public void requestBoard() {
        write("SYNC" + ((char) 4));

        // TODO: read response
    }

    private void write(String str) {
        byte[] data = str.getBytes(StandardCharsets.US_ASCII);

        this.port.writeBytes(data, data.length);

        // Block until data is sent
        while (true) {
            if (this.port.bytesAwaitingWrite() <= 0) break;
        }

        // TODO: Read response data
    }

    public void start(int baudRate) {
        if (thread != null) {
            throw new IllegalStateException("A serial connection is already running");
        }

        this.port.setBaudRate(baudRate);

        this.port.openPort();

        this.buffer = new byte[8];

        thread = Executors.newSingleThreadExecutor();
        thread.execute(() -> {
            try {
                while (!Thread.interrupted()) {
                    if (this.port.bytesAvailable() > 0) {
                        if (bufferIndex + 1 >= buffer.length) {
                            int newLength = buffer.length * 2;

                            if (newLength > 4096) {
                                throw new RuntimeException("Serial buffer limit of 4096 bytes has been exceeded");
                            }

                            // TODO: Use this debug information to determine default buffer size
                            System.out.println("[Serial] Buffer size changed (" + this.buffer.length + "->" + newLength + ")");

                            buffer = Arrays.copyOf(buffer, newLength);
                        }

                        this.port.readBytes(this.buffer, 1, bufferIndex);
                        byte b = this.buffer[bufferIndex++];

                        if (b == 4) {
                            // TODO: Do something with the data

                            System.out.println("[Serial] Received " + (bufferIndex + 1) + " bytes");

                            Arrays.fill(this.buffer, (byte) 0);
                            this.bufferIndex = 0;

                            break;
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    public void stop() throws InterruptedException {
        if (thread != null) {
            thread.shutdown();

            if (!thread.awaitTermination(3, TimeUnit.SECONDS)) {
                thread.shutdownNow();
            }

            thread = null;
        }

        this.buffer = null;
        this.bufferIndex = 0;
    }

    public void promptPortSelection() throws IOException {
        SerialPort[] ports = SerialPort.getCommPorts();

        printPorts(ports);

        try (BufferedInputStream in = new BufferedInputStream(System.in)) {
            while (true) {
                System.out.print("Port-Nummer, 'r' zum aktualisieren oder '.' zum Überspringen: ");
                String inStr = readLine(in);

                if (inStr.equalsIgnoreCase("r")) {
                    ports = SerialPort.getCommPorts();
                    printPorts(ports);
                } else if (inStr.equalsIgnoreCase(".")) {
                    this.port = null;
                    return;
                } else {
                    try {
                        this.port = ports[Integer.parseInt(inStr)];
                        return;
                    } catch (NumberFormatException | ArrayIndexOutOfBoundsException ex) {
                        System.err.println("Ungültige Port-Nummer");
                    }
                }
            }
        }
    }

    private void printPorts(SerialPort[] ports) {
        for (int i = 0; i < ports.length; ++i) {
            SerialPort port = ports[i];

            System.out.println("[" + i + "] " + port.getSystemPortName() + " (" + port.getPortDescription() + ")");
        }
    }

    private String readLine(BufferedInputStream in) throws IOException {
        StringBuilder result = new StringBuilder();

        int b;
        while ((b = in.read()) != (int) '\n') {
            if (b != -1) {
                result.append((char) b);
            }
        }

        return result.toString();
    }
}
