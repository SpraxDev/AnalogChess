package de.sprax2013.hems.analog_chess;

import com.fazecast.jSerialComm.SerialPort;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// FIXME: Was a stupid idea. Implementation is okay-ish...
public class Serial {
    private static final boolean DEBUG = false;

    private SerialPort port;
    private ExecutorService thread;
    private byte[] buffer;
    private int bufferIndex;

    public SerialPort getPort() {
        return this.port;
    }

    private byte[] write(byte[] data) {
        if (DEBUG) System.out.println("[Serial] Sending: " + Arrays.toString(data));
        this.port.writeBytes(data, data.length);

        // Block until data is sent
        while (true) {
            if (this.port.bytesAwaitingWrite() <= 0) break;
        }

        if (DEBUG) {
            System.out.println("[Serial] Received " + bufferIndex + " bytes (" + Arrays.toString(Arrays.copyOf(this.buffer, bufferIndex)) + ")");
        }

        if (this.buffer[0] != 6) {
            throw new RuntimeException("Connected serial sent unexpected first byte: " + this.buffer[0]);
        }

        // Copy result buffer to return later
        byte[] result = bufferIndex == 0 ? new byte[0] : Arrays.copyOfRange(this.buffer, 1, bufferIndex - 1);

        // Reset buffer
        Arrays.fill(this.buffer, (byte) 0);
        this.bufferIndex = 0;

        return result;
    }

    public void onInput(byte[] data) {
        try {
            String dataStr = new String(data, StandardCharsets.US_ASCII);

            if (dataStr.startsWith("State:")) {
                boolean[] board = new boolean[64];
                int boardIndex = 0;

                for (int i = 6; i < data.length; ++i) {
                    char c = dataStr.charAt(i);

                    if (Character.isWhitespace(c)) continue;

                    board[boardIndex++] = c == '1';
                }

                if (Main.gui != null) {
                    Main.gui.setHighlightedFields(board);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void start(int baudRate) {
        if (thread != null) {
            throw new IllegalStateException("A serial connection is already running");
        }

        if (!this.port.openPort()) {
            throw new RuntimeException("Could not open serial port");
        }

        if (!this.port.setBaudRate(baudRate)) {
            throw new RuntimeException("Could not set BaudRate");
        }

        this.port.setBaudRate(baudRate);
        this.port.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
        this.port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);

        this.buffer = new byte[128];

        thread = Executors.newSingleThreadExecutor();
        thread.execute(() -> {
            while (!thread.isShutdown()) {
                if (this.port.bytesAvailable() > 0) {
                    if (bufferIndex + 1 > buffer.length) {
                        int newLength = buffer.length * 2;

                        if (newLength > 4096) {
                            throw new RuntimeException("Serial buffer limit of 4096 bytes has been exceeded");
                        }

                        // Use this information to determine default buffer size
                        System.out.println("[Serial] Buffer size changed (" + this.buffer.length + "->" + newLength + ")");

                        buffer = Arrays.copyOf(buffer, newLength);
                    }

                    this.port.readBytes(this.buffer, 1, bufferIndex);
                    byte b = this.buffer[bufferIndex++];

                    if (DEBUG) System.out.println("Received byte '" + b + "'");

                    if (b == 10) {
                        if (DEBUG) {
                            System.out.print("Finished transmission '");
                            for (int i = 0; i < bufferIndex; ++i) {
                                System.out.print((char) buffer[i]);
                            }
                            System.out.println("'");
                        }

                        if (bufferIndex > 0) {
                            onInput(Arrays.copyOfRange(this.buffer, 0, bufferIndex - 1));
                        }

                        Arrays.fill(this.buffer, (byte) 0);
                        this.bufferIndex = 0;
                    }
                }
            }

            System.out.println("Terminating read-thread");
        });

        if (DEBUG) {
            System.out.println("[Serial] Ready.");
        }

//        sendReset();
    }

    public void stop() throws InterruptedException {
        if (thread != null) {
            thread.shutdown();

            if (!thread.awaitTermination(3, TimeUnit.SECONDS)) {
                System.err.println("Forcefully shutting down serial read thread");
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
                        System.err.println("\nUngültige Port-Nummer");
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
