package de.sprax2013.hems.analog_chess;

import com.fazecast.jSerialComm.SerialPort;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Serial {
    private static final boolean DEBUG = true;

    private SerialPort port;
    private ExecutorService thread;
    private byte[] buffer;
    private int bufferIndex;

    private SerialState state;

    public SerialPort getPort() {
        return this.port;
    }

//    public byte[] sendLedColor(int ledIndex, Color color) {
//        return write("LED" + ledIndex + color.getRGB() + ((char) 4));
//
//        // TODO: read response
//    }
//
//    public byte[] sendLedShow() {
//        return write("LED SHOW" + ((char) 4));
//
//        // TODO: read response
//    }

    public byte[] requestBoard() {
        return write(new byte[] {'S', 'Y', 'N', 'C', 4});
    }

    public void sendReset() {
        if (DEBUG) System.out.println("Sending RST-Command...");

        write(new byte[] {'R', 'S', 'T', 4});

        if (DEBUG) System.out.println("Finished sending RST-Command");
    }

    private byte[] write(byte[] data) {
        // Wait until we are ready to write
        while (true) {
            if (this.state == SerialState.IDLE) break;
        }

        this.state = SerialState.BUSY_WRITE;

        if (DEBUG) System.out.println("[Serial] Sending: " + Arrays.toString(data));
        this.port.writeBytes(data, data.length);

        // Block until data is sent
        while (true) {
            if (this.port.bytesAwaitingWrite() <= 0) break;
        }

        this.state = SerialState.WAIT_RESPONSE;

        // Wait for the reading thread to finish reading
        while (true) {
            //noinspection ConstantConditions
            if (this.state == SerialState.RESPONSE_READY) break;
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

        // Reset state
        this.state = SerialState.IDLE;

        return result;
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

        this.buffer = new byte[10];
        this.state = SerialState.IDLE;

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

                    if (b == 4) {
                        this.state = SerialState.RESPONSE_READY;
                    }
                }
            }

            System.out.println("Terminating read-thread");
        });

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
