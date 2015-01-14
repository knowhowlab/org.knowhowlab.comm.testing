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

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.knowhowlab.comm.testing.common.config.DriverConfig;
import org.knowhowlab.comm.testing.common.config.PortConfig;
import org.knowhowlab.comm.testing.common.config.PortType;

import javax.comm.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class MockOracleDriverTest {
    private static MockOracleDriver driver;

    @BeforeClass
    public static void init() throws IOException {
        driver = new MockOracleDriver(createConfig());
        driver.initialize();
    }

    @After
    public void after() throws IOException {
        driver.reset();
    }

    @Test
    public void testInitialize() throws Exception {
        Enumeration portIdentifiers = CommPortIdentifier.getPortIdentifiers();
        assertThat(portIdentifiers, notNullValue());

        CommPortIdentifier com1 = CommPortIdentifier.getPortIdentifier("COM1");
        assertThat(com1, notNullValue());
        assertThat(com1.getName(), is(equalTo("COM1")));
        assertThat(com1.getPortType(), is(equalTo(CommPortIdentifier.PORT_SERIAL)));

        CommPortIdentifier com2 = CommPortIdentifier.getPortIdentifier("COM2");
        assertThat(com2, notNullValue());
        assertThat(com2.getName(), is(equalTo("COM2")));
        assertThat(com2.getPortType(), is(equalTo(CommPortIdentifier.PORT_SERIAL)));
    }

    @Test(expected = NoSuchPortException.class)
    public void testInitialize_UnknownPort() throws Exception {
        CommPortIdentifier com2 = CommPortIdentifier.getPortIdentifier("COM3");
    }

    @Test
    public void testSimpleDataTransfer() throws Exception {
        CommPortIdentifier com1Id = CommPortIdentifier.getPortIdentifier("COM1");
        CommPortIdentifier com2Id = CommPortIdentifier.getPortIdentifier("COM2");

        CommPort com1 = com1Id.open("MyApp", 2000);
        CommPort com2 = com2Id.open("MyApp", 2000);

        com1.getOutputStream().write("Test".getBytes());
        byte[] buff = new byte[32];
        int read = com2.getInputStream().read(buff);

        assertThat("Test", is(equalTo(new String(buff, 0, read))));

        com1.close();
        com2.close();
    }

    @Test
    public void testSimpleDataTransfer_WithNotification() throws Exception {
        CommPortIdentifier com1Id = CommPortIdentifier.getPortIdentifier("COM1");
        CommPortIdentifier com2Id = CommPortIdentifier.getPortIdentifier("COM2");

        final CommPort com1 = com1Id.open("MyApp", 2000);
        final SerialPort com2 = (SerialPort) com2Id.open("MyApp", 2000);
        final StringBuilder data = new StringBuilder();


        com2.notifyOnDataAvailable(true);
        com2.addEventListener(new SerialPortEventListener() {
            @Override
            public void serialEvent(SerialPortEvent serialPortEvent) {
                switch (serialPortEvent.getEventType()) {
                    case SerialPortEvent.DATA_AVAILABLE:
                        try {
                            byte[] buff = new byte[32];
                            int read = com2.getInputStream().read(buff);
                            data.append(new String(buff, 0, read));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                }
            }
        });

        new Thread(){
            @Override
            public void run() {
                try {
                    com1.getOutputStream().write("Test".getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();

        TimeUnit.SECONDS.sleep(1);

        assertThat("Test", is(equalTo(data.toString())));

        com1.close();
        com2.close();
    }

    private static DriverConfig createConfig() throws IOException {
        List<PortConfig> ports = new ArrayList<PortConfig>();
        PortConfig com1 = new PortConfig("COM1", PortType.SERIAL);
        ports.add(com1);
        PortConfig com2 = new PortConfig("COM2", PortType.SERIAL, com1);
        ports.add(com2);
        return new DriverConfig(ports);
    }
}