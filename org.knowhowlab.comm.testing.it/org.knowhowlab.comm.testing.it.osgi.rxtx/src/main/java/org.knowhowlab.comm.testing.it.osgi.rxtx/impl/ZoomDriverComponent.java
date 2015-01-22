package org.knowhowlab.comm.testing.it.osgi.rxtx.impl;

import gnu.io.*;
import org.knowhowlab.comm.testing.it.osgi.rxtx.ZoomDriver;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.monitor.Monitorable;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author dpishchukhin
 */
public class ZoomDriverComponent implements ZoomDriver {
    private static final Logger LOG = Logger.getLogger(ZoomDriver.class.getName());

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

    protected void activate(ComponentContext context) {
        readConfig(context.getProperties());

        LOG.info("Activated");
    }

    protected void deactivate(ComponentContext context) {
        if (isPortOpened()) {
            closeConnectionToDevice();
        }
        LOG.info("Deactivated");
    }

    protected void modified(ComponentContext context) {
        if (readConfig(context.getProperties())) {
            reopenPort();
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
            serialPort.close();
            serialPort = null;
        }
        LOG.info(String.format("Port %s closed", port));
    }

    private boolean openConnectionToDevice() {
        if (openPort()) {
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
            serialPort = (SerialPort) portIdentifier.open("ZoomDriver", 2000);
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

    @Override
    public boolean zoomIn() {
        // todo:
        return false;
    }

    @Override
    public boolean zoomOut() {
        // todo:
        return false;
    }

    @Override
    public boolean reset() {
        // todo:
        return false;
    }

    @Override
    public int getZoom() {
        // todo:
        return 0;
    }
}
