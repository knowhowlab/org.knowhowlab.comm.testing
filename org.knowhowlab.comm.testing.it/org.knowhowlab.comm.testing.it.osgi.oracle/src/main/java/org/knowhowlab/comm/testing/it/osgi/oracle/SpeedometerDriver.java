package org.knowhowlab.comm.testing.it.osgi.oracle;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.monitor.Monitorable;
import org.osgi.service.monitor.StatusVariable;

import javax.comm.*;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.TooManyListenersException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author dpishchukhin
 */
public class SpeedometerDriver implements Monitorable {
    public static final String PRODUCE_PID = "speedometer";

    private static final Logger LOG = Logger.getLogger(SpeedometerDriver.class.getName());

    private static final String PORT_CONFIG_PROP = "port";
    private static final String BAUDRATE_CONFIG_PROP = "baudrate";
    private static final String DATABITS_CONFIG_PROP = "databits";
    private static final String STOPBITS_CONFIG_PROP = "stopbits";
    private static final String PARITY_CONFIG_PROP = "parity";
    public static final String SPEED_SV = "speed";

    private String port;
    private Integer baudrate;
    private Integer databits;
    private Integer stopbits;
    private Integer parity;

    private SerialPort serialPort;
    private SerialPortEventListenerImpl eventListener = new SerialPortEventListenerImpl();

    private volatile int speed;

    private ServiceRegistration serviceRegistration;

    protected void activate(ComponentContext context) {
        // register Producer
        serviceRegistration = context.getBundleContext().registerService(Monitorable.class.getName(), this,
                new Hashtable<String, Object>() {{
                    put(Constants.SERVICE_PID, PRODUCE_PID);
                }}
        );

        readConfig(context.getProperties());

        Executors.newSingleThreadExecutor().submit(new OpenPortThread());

        LOG.info("Activated");
    }

    protected void deactivate(ComponentContext context) {
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
        }

        if (isPortOpened()) {
            closeConnectionToDevice();
        }
        LOG.info("Deactivated");
    }

    protected void modified(ComponentContext context) {
        if (readConfig(context.getProperties())) {
            Executors.newSingleThreadExecutor().submit(new OpenPortThread());
        }
        LOG.info("Modified");
    }

    private boolean readConfig(Dictionary dictionary) {
        // 1. read new connection settings
        boolean newValues = false;
        Object portProp = dictionary.get(PORT_CONFIG_PROP);
        if (portProp != null && (portProp instanceof String) && !portProp.equals(port)) {
            port = (String) portProp;
            newValues = true;
        }
        Object baudrateProp = dictionary.get(BAUDRATE_CONFIG_PROP);
        if (baudrateProp != null && (baudrateProp instanceof Integer) && !baudrateProp.equals(baudrate)) {
            baudrate = (Integer) baudrateProp;
            newValues = true;
        }
        Object databitsProp = dictionary.get(DATABITS_CONFIG_PROP);
        if (databitsProp != null && (databitsProp instanceof Integer) && !databitsProp.equals(databits)) {
            databits = (Integer) databitsProp;
            newValues = true;
        }
        Object stopbitsProp = dictionary.get(STOPBITS_CONFIG_PROP);
        if (stopbitsProp != null && (stopbitsProp instanceof Integer) && !stopbitsProp.equals(stopbits)) {
            stopbits = (Integer) stopbitsProp;
            newValues = true;
        }
        Object parityProp = dictionary.get(PARITY_CONFIG_PROP);
        if (parityProp != null && (parityProp instanceof Integer) && !parityProp.equals(parity)) {
            parity = (Integer) parityProp;
            newValues = true;
        }

        return newValues;
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
            serialPort = (SerialPort) portIdentifier.open("MIP Navman Location Provider", 2000);
            serialPort.setSerialPortParams(baudrate, databits, stopbits, parity);
            return true;
        } catch (PortInUseException e) {
            LOG.log(Level.WARNING, String.format("%s port is in use", port), e);
        } catch (NoSuchPortException e) {
            LOG.log(Level.WARNING, String.format("%s port is unknown", port), e);
        } catch (UnsupportedCommOperationException e) {
            LOG.log(Level.WARNING, String.format("Unable to set connection parameters to %s port: %d, %d, %d, %d", port, baudrate, databits, stopbits, parity), e);
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

    @Override
    public String[] getStatusVariableNames() {
        return new String[] {SPEED_SV};
    }

    @Override
    public StatusVariable getStatusVariable(String s) throws IllegalArgumentException {
        if (SPEED_SV.equals(s)) {
            return new StatusVariable(SPEED_SV, StatusVariable.CM_SI, speed);
        }
        throw new IllegalArgumentException(String.format("Illegal variable name %s", s));
    }

    @Override
    public boolean notifiesOnChange(String s) throws IllegalArgumentException {
        if (SPEED_SV.equals(s)) {
            return false;
        }
        throw new IllegalArgumentException(String.format("Illegal variable name %s", s));
    }

    @Override
    public boolean resetStatusVariable(String s) throws IllegalArgumentException {
        if (SPEED_SV.equals(s)) {
            speed = -1;
            return true;
        }
        throw new IllegalArgumentException(String.format("Illegal variable name %s", s));
    }

    @Override
    public String getDescription(String s) throws IllegalArgumentException {
        if (SPEED_SV.equals(s)) {
            return "Speedometer value";
        }
        throw new IllegalArgumentException(String.format("Illegal variable name %s", s));
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
                                            speed = parseValue(commandStr.trim());
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
                        LOG.log(Level.WARNING, String.format("Read error from port %s", port), e);
                    } catch (Exception e) {
                        LOG.log(Level.WARNING, String.format("Read error from port %s", port), e);
                    }
                    break;
            }
        }
    }

    private int parseValue(String stringValue) {
        try {
            return Integer.parseInt(stringValue);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private class OpenPortThread implements Runnable {
        @Override
        public void run() {
            while (!reopenPort()) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }
}
