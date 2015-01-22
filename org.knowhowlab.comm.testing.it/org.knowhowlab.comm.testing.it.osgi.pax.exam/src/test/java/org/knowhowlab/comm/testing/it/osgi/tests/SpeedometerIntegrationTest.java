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

package org.knowhowlab.comm.testing.it.osgi.tests;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.knowhowlab.comm.testing.common.config.DriverConfig;
import org.knowhowlab.comm.testing.common.config.PortConfig;
import org.knowhowlab.comm.testing.common.config.PortType;
import org.knowhowlab.comm.testing.oracle.MockOracleDriver;
import org.knowhowlab.osgi.testing.utils.ServiceUtils;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.monitor.MonitorAdmin;
import org.osgi.service.monitor.Monitorable;

import javax.comm.CommPort;
import javax.comm.CommPortIdentifier;
import javax.comm.NoSuchPortException;
import javax.comm.PortInUseException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.knowhowlab.osgi.testing.assertions.ServiceAssert.assertServiceAvailable;
import static org.knowhowlab.osgi.testing.utils.FilterUtils.*;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.osgi.framework.Constants.SERVICE_PID;

/**
 * @author dpishchukhin
 */
public class SpeedometerIntegrationTest extends AbstractTest {
    private static MockOracleDriver driver;

    @BeforeClass
    public static void initComm() throws IOException {
        driver = new MockOracleDriver(createConfig());
        driver.initialize();
    }

    @After
    public void after() throws IOException {
        driver.reset();
    }

    private static DriverConfig createConfig() throws IOException {
        List<PortConfig> ports = new ArrayList<PortConfig>();
        PortConfig com1 = new PortConfig("COM1", PortType.SERIAL);
        ports.add(com1);
        PortConfig com2 = new PortConfig("COM2", PortType.SERIAL, com1);
        ports.add(com2);
        return new DriverConfig(ports);
    }

    @Configuration
    public static Option[] customTestConfiguration() {
        Option[] options = options(
                mavenBundle().groupId("org.knowhowlab.osgi").artifactId("monitoradmin").version("1.0.2"),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.scr").version("1.8.2"),
                mavenBundle().groupId("org.knowhowlab.comm").artifactId("org.knowhowlab.comm.testing.oracle").version(System.getProperty("project.version")),
                mavenBundle().groupId("org.knowhowlab.comm").artifactId("org.knowhowlab.comm.testing.it.osgi.oracle").version(System.getProperty("project.version"))
        );
        return combine(options, baseConfiguration());
    }

    @Test
    public void testAllServices() throws InvalidSyntaxException, InterruptedException {
        // assert MonitorAdmin service is available in OSGi registry
        assertServiceAvailable(MonitorAdmin.class, 5, SECONDS);
        // assert Monitorable service is available in OSGi registry
        assertServiceAvailable(and(create(Monitorable.class), eq(SERVICE_PID, "speedometer")), 5, SECONDS);
    }

    @Test
    public void testDataTransfer() throws Exception {
        Monitorable speedometer = ServiceUtils.getService(bc, Monitorable.class, eq(SERVICE_PID, "speedometer"));
        // driver is offline
        assertThat(speedometer.getStatusVariable("online").getBoolean(), is(false));

        // wait for connection
        TimeUnit.SECONDS.sleep(2);

        // driver is online
        assertThat(speedometer.getStatusVariable("online").getBoolean(), is(true));

        // connect test device
        CommPortIdentifier com2Id = CommPortIdentifier.getPortIdentifier("COM2");
        CommPort com2 = com2Id.open("TestDevice", 2000);

        // provide speed = 100
        com2.getOutputStream().write("100\n".getBytes(Charset.defaultCharset()));
        assertThat(speedometer.getStatusVariable("speed").getInteger(), is(100));

        // provide speed = 150
        com2.getOutputStream().write("150\n".getBytes(Charset.defaultCharset()));
        assertThat(speedometer.getStatusVariable("speed").getInteger(), is(150));
    }
}
