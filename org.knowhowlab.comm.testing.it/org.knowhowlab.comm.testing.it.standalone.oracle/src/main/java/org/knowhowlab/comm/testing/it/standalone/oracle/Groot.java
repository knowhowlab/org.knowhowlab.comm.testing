package org.knowhowlab.comm.testing.it.standalone.oracle;

import javax.comm.*;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.TooManyListenersException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author dpishchukhin
 */
public class Groot {
    private static final Logger LOG = Logger.getLogger(Groot.class.getName());

    private final SerialPort serialPort;

    private final int baudrate = 9600;
    private final int databits = 8;
    private final int stopbits = 1;
    private final int parity = 0;

    public Groot(String serialPortName) {
        try {
            CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(serialPortName);
            if (portIdentifier.getPortType() != CommPortIdentifier.PORT_SERIAL) {
                throw new RuntimeException("Port is not a serial port: " + serialPortName);
            }
            serialPort = (SerialPort) portIdentifier.open("Groot", 2000);
            serialPort.setSerialPortParams(baudrate, databits, stopbits, parity);
        } catch (NoSuchPortException e) {
            throw new RuntimeException("Unknown port: " + serialPortName, e);
        } catch (PortInUseException e) {
            throw new RuntimeException("Port in use: " + serialPortName, e);
        } catch (UnsupportedCommOperationException e) {
            throw new RuntimeException("Unsupported operation: " + serialPortName, e);
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java org.knowhowlab.comm.testing.it.standalone.oracle.Groot <SerialPort>");
        } else {
            final Groot groot = new Groot(args[0]);
            groot.start();
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    groot.stop();
                }
            });
        }
    }

    public void start() {
        try {
            serialPort.addEventListener(new DataListener());
            serialPort.notifyOnDataAvailable(true);
        } catch (TooManyListenersException e) {
            throw new RuntimeException("Listener is already added", e);
        }
    }

    public void stop() {
        serialPort.notifyOnDataAvailable(false);
        serialPort.removeEventListener();
    }

    private class DataListener implements SerialPortEventListener {
        private final StringBuilder buff = new StringBuilder();

        @Override
        public void serialEvent(SerialPortEvent serialPortEvent) {
            if (SerialPortEvent.DATA_AVAILABLE == serialPortEvent.getEventType()) {
                try {
                    int read;
                    byte[] byteBuff = new byte[128];
                    InputStream inputStream = new BufferedInputStream(serialPort.getInputStream());
                    while (inputStream.available() > 0) {
                        if ((read = inputStream.read(byteBuff)) == -1) {
                            break;
                        }
                        char[] chars = new String(byteBuff, 0, read, Charset.defaultCharset()).toCharArray();
                        for (char aChar : chars) {
                            switch (aChar) {
                                case '\n':
                                    String commandStr = buff.toString().trim();
                                    if (!commandStr.isEmpty()) {
                                        LOG.info(String.format("Received: %s", commandStr));
                                        serialPort.getOutputStream().write("I am Groot!\n".getBytes(Charset.defaultCharset()));
                                        buff.delete(0, buff.length());
                                    }
                                    break;
                                default:
                                    buff.append(aChar);
                                    break;
                            }
                        }
                    }
                } catch (IOException e) {
                    LOG.log(Level.WARNING, String.format("Read error from port %s", serialPort), e);
                } catch (Exception e) {
                    LOG.log(Level.WARNING, String.format("Read error from port %s", serialPort), e);
                }
            }
        }
    }
}
