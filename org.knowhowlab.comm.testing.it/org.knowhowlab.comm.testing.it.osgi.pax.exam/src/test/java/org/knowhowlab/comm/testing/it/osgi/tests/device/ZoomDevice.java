package org.knowhowlab.comm.testing.it.osgi.tests.device;

import gnu.io.*;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.TooManyListenersException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author dpishchukhin
 */
public class ZoomDevice {
    private static final Logger LOG = Logger.getLogger(ZoomDevice.class.getName());
    private static final String GET_COMMAND = "GET";
    private static final String ZOOMIN_COMMAND = "ZOOMIN";
    private static final String ZOOMOUT_COMMAND = "ZOOMOUT";
    private static final String RESET_COMMAND = "RESET";
    private static final String OK_ANSWER = "OK";
    private static final String ERROR_ANSWER = "ERROR";

    private final String port;
    private final int minValue;
    private final int maxValue;
    private final int step;

    private int value;

    private SerialPort serialPort;
    private SerialPortEventListenerImpl eventListener = new SerialPortEventListenerImpl();

    public ZoomDevice(String port, int minValue, int maxValue, int step) {
        this.port = port;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.step = step;

        value = minValue;
    }

    public void start() {
        Executors.newSingleThreadExecutor().submit(new OpenPortThread());

        LOG.info("Activated");
    }

    public void stop() {
        if (isPortOpened()) {
            closeConnectionToDevice();
        }
        LOG.info("Deactivated");
    }

    private boolean isPortOpened() {
        return serialPort != null;
    }

    private boolean reopenPort() {
        if (isPortOpened()) {
            // 2. try to close opened port
            closeConnectionToDevice();
        }
        // 3. try to open port
        return openConnectionToDevice();
    }

    private void closeConnectionToDevice() {
        if (serialPort != null) {
            serialPort.notifyOnDataAvailable(false);
            serialPort.removeEventListener();
            serialPort.close();
            serialPort = null;
        }
        LOG.info(String.format("Port %s closed", port));
    }

    private boolean openConnectionToDevice() {
        if (openPort() && addSerialDataListener()) {
            LOG.info(String.format("Port %s opened", port));
            return true;
        } else {
            LOG.warning(String.format("Unable to set connection to port %s", port));
            closeConnectionToDevice();
            return false;
        }
    }

    private boolean openPort() {
        try {
            CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(port);
            if (portIdentifier.getPortType() != CommPortIdentifier.PORT_SERIAL) {
                LOG.warning(String.format("%s port is not SERIAL port", port));
                return false;
            }
            serialPort = (SerialPort) portIdentifier.open("ZoomDevice", 2000);
            return true;
        } catch (PortInUseException e) {
            LOG.log(Level.WARNING, String.format("%s port is in use", port), e);
        } catch (NoSuchPortException e) {
            LOG.log(Level.WARNING, String.format("%s port is unknown", port), e);
        }
        return false;
    }

    private boolean addSerialDataListener() {
        try {
            serialPort.addEventListener(eventListener);
            serialPort.notifyOnDataAvailable(true);
            return true;
        } catch (TooManyListenersException e) {
            LOG.log(Level.WARNING, "Unable to set port data listener", e);
            return false;
        }
    }

    private class SerialPortEventListenerImpl implements SerialPortEventListener {
        private final StringBuilder buff = new StringBuilder();

        @Override
        public void serialEvent(SerialPortEvent serialPortEvent) {
            switch (serialPortEvent.getEventType()) {
                case SerialPortEvent.DATA_AVAILABLE:
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
                                            handleCommand(commandStr);
                                        }
                                        buff.delete(0, buff.length());
                                        break;
                                    default:
                                        buff.append(aChar);
                                        break;
                                }
                            }
                        }
                    } catch (IOException e) {
                        LOG.log(Level.WARNING, String.format("Read/Write error from/to port %s", port), e);
                    } catch (Exception e) {
                        LOG.log(Level.WARNING, String.format("Read/Write error from/to port %s", port), e);
                    }
                    break;
            }
        }
    }

    private void handleCommand(String command) throws IOException {
        if (GET_COMMAND.equals(command)) {
            sendAnswer(value);
        } else if (ZOOMIN_COMMAND.equals(command)) {
            if (value + step > maxValue) {
                sendAnswer(ERROR_ANSWER);
            } else {
                value = value + step;
                sendAnswer(OK_ANSWER);
            }
        } else if (ZOOMOUT_COMMAND.equals(command)) {
            if (value - step > minValue) {
                sendAnswer(ERROR_ANSWER);
            } else {
                value = value - step;
                sendAnswer(OK_ANSWER);
            }
        } else if (RESET_COMMAND.equals(command)) {
            value = minValue;
            sendAnswer(OK_ANSWER);
        } else {
            sendAnswer(ERROR_ANSWER);
        }
    }

    private void sendAnswer(Object value) throws IOException {
        serialPort.getOutputStream().write((value.toString() + "\n").getBytes(Charset.defaultCharset()));
    }

    private class OpenPortThread implements Runnable {
        @Override
        public void run() {
            while (!reopenPort()) {
                try {
                    TimeUnit.MILLISECONDS.sleep(250);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }
}
