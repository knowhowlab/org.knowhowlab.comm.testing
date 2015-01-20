package org.knowhowlab.comm.testing.it.osgi.oracle;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Producer;
import org.osgi.service.wireadmin.Wire;
import org.osgi.service.wireadmin.WireConstants;
import org.osgi.util.measurement.Measurement;
import org.osgi.util.measurement.Unit;

import javax.comm.*;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.charset.Charset;
import java.util.Arrays;
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
public class SpeedometerDriver implements Producer {
    public static final String PRODUCE_PID = "speedometer";

    private static final Logger LOG = Logger.getLogger(SpeedometerDriver.class.getName());

    private static final String PORT_CONFIG_PROP = "port";
    private static final String BAUDRATE_CONFIG_PROP = "baudrate";
    private static final String DATABITS_CONFIG_PROP = "databits";
    private static final String STOPBITS_CONFIG_PROP = "stopbits";
    private static final String PARITY_CONFIG_PROP = "parity";

    private String port;
    private Integer baudrate;
    private Integer databits;
    private Integer stopbits;
    private Integer parity;

    private SerialPort serialPort;
    private SerialPortEventListenerImpl eventListener = new SerialPortEventListenerImpl();

    private Wire[] wires = new Wire[0];
    private volatile Measurement lastValue;

    private ServiceRegistration serviceRegistration;

    protected void activate(ComponentContext context) {
        // register Producer
        serviceRegistration = context.getBundleContext().registerService(Producer.class.getName(), this,
                new Hashtable<String, Object>() {{
                    put(Constants.SERVICE_PID, PRODUCE_PID);
                    put(WireConstants.WIREADMIN_PRODUCER_FLAVORS, new Class[]{
                            Measurement.class
                    });
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
    public Object polled(Wire wire) {
        return lastValue;
    }

    @Override
    public synchronized void consumersConnected(Wire[] wires) {
        if (wires != null) {
            this.wires = Arrays.copyOf(wires, wires.length);
        } else {
            this.wires = new Wire[0];
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
                                            double value = convertToMetersPerSecond(parseValue(commandStr.trim()));
                                            if (!Double.isNaN(value)) {
                                                lastValue = new Measurement(value, Unit.m_s);
                                                // notify wires
                                                notifyWires();
                                            }
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

    private synchronized void notifyWires() {
        if (wires != null) {
            for (Wire wire : wires) {
                wire.update(lastValue);
            }
        }
    }

    private double convertToMetersPerSecond(double kmPerHour) {
        if (Double.isNaN(kmPerHour)) {
            return Double.NaN;
        }

        return new BigDecimal(kmPerHour, MathContext.DECIMAL32)
                .multiply(new BigDecimal(5))
                .divide(new BigDecimal(18, MathContext.DECIMAL32), BigDecimal.ROUND_HALF_UP)
                .intValue();
    }

    private double parseValue(String stringValue) {
        try {
            return Integer.parseInt(stringValue);
        } catch (NumberFormatException e) {
            return Double.NaN;
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
