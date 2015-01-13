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

package org.knowhowlab.comm.testing.oracle;

import org.knowhowlab.comm.testing.common.Linkable;
import org.knowhowlab.comm.testing.common.config.DriverConfig;
import org.knowhowlab.comm.testing.common.config.PortConfig;
import org.knowhowlab.comm.testing.common.config.PortType;

import javax.comm.CommDriver;
import javax.comm.CommPort;
import javax.comm.CommPortIdentifier;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author dpishchukhin
 */
public class MockOracleDriver implements CommDriver {
    private static final Logger LOG = Logger.getLogger(MockOracleDriver.class.getName());

    private DriverConfig config;

    private List<Linkable> serialPorts = new ArrayList<Linkable>();

    public MockOracleDriver() {
    }

    public MockOracleDriver(DriverConfig config) {
        this.config = config;
    }

    @Override
    public void initialize() {
        try {
            serialPorts.clear();
            if (config != null) {
                List<PortConfig> ports = config.getPorts();
                if (ports != null) {
                    for (PortConfig port : ports) {
                        switch (port.getType()) {
                            case SERIAL:
                                Linkable serialPort = new MockOracleSerialPort(port.getName());
                                serialPorts.add(serialPort);
                                if (port.getLinkPort() != null) {
                                    linkPort(serialPorts, serialPort, port.getLinkPort().getName());
                                }
                                break;
                        }
                        CommPortIdentifier.addPortName(port.getName(), port.getType().getCode(), this);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void linkPort(List<? extends Linkable> ports, Linkable port, String linkName) throws IOException {
        if (ports != null) {
            for (Linkable linkable : ports) {
                if (linkable.getName().equals(linkName)) {
                    port.linkTo(linkable);
                }
            }
        }
    }

    @Override
    public CommPort getCommPort(String portName, int portType) {
        if (portType == PortType.SERIAL.getCode()) {
            for (Linkable serialPort : serialPorts) {
                if (serialPort.getName().equals(portName)) {
                    return (CommPort) serialPort;
                }
            }
        }
        return null;
    }

    public void setConfig(DriverConfig config) {
        this.config = config;
    }
}
