/*
 * Copyright (c) 2010-2015 Dmytro Pishchukhin (http://knowhowlab.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.knowhowlab.comm.testing.it.osgi.rxtx.impl;

import gnu.io.*;
import org.knowhowlab.comm.testing.it.osgi.rxtx.ZoomDriver;
import org.osgi.service.component.ComponentContext;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Dictionary;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author dpishchukhin
 */
public class ZoomDriverComponent implements ZoomDriver {
    private static final Logger LOG = Logger.getLogger(ZoomDriver.class.getName());

    private static final String GET_COMMAND = "GET";
    private static final String ZOOMIN_COMMAND = "ZOOMIN";
    private static final String ZOOMOUT_COMMAND = "ZOOMOUT";
    private static final String RESET_COMMAND = "RESET";
    private static final String OK_ANSWER = "OK";
    private static final String ERROR_ANSWER = "ERROR";

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

        openConnectionToDevice();

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
        if (!isPortOpened()) {
            openPort();
        }
        return OK_ANSWER.equals(sendCommand(ZOOMIN_COMMAND));
    }

    @Override
    public boolean zoomOut() {
        if (!isPortOpened()) {
            openPort();
        }
        return OK_ANSWER.equals(sendCommand(ZOOMOUT_COMMAND));
    }

    @Override
    public boolean reset() {
        if (!isPortOpened()) {
            openPort();
        }
        return OK_ANSWER.equals(sendCommand(RESET_COMMAND));
    }

    @Override
    public int getZoom() {
        reopenPort();
        return Integer.parseInt(sendCommand(GET_COMMAND));
    }

    private String sendCommand(String command) {
        try {
            LOG.info("Command: " + command);
            serialPort.getOutputStream().write((command + "\n").getBytes(Charset.defaultCharset()));
            byte[] buff = new byte[16];
            int read = serialPort.getInputStream().read(buff);
            String response = new String(buff, 0, read - 1);
            LOG.info("Response: " + response);
            return response; // w/o \n
        } catch (IOException e) {
            LOG.log(Level.WARNING, "IOException", e);
            throw new RuntimeException("IOException", e);
        }
    }
}
