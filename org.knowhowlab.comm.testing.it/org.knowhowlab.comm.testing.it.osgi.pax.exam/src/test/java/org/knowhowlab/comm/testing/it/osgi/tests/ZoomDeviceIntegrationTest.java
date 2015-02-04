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
import org.knowhowlab.comm.testing.it.osgi.rxtx.ZoomDriver;
import org.knowhowlab.comm.testing.it.osgi.tests.device.ZoomDevice;
import org.knowhowlab.comm.testing.rxtx.MockRxTxDriver;
import org.knowhowlab.osgi.testing.utils.ServiceUtils;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.osgi.framework.InvalidSyntaxException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.knowhowlab.osgi.testing.assertions.ServiceAssert.assertServiceAvailable;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.OptionUtils.combine;

/**
 * @author dpishchukhin
 */
public class ZoomDeviceIntegrationTest extends AbstractTest {
    private static MockRxTxDriver driver;
    private static ZoomDevice zoomDevice;

    @BeforeClass
    public static void initComm() throws IOException {
        driver = new MockRxTxDriver(createConfig());
        driver.initialize();
        zoomDevice = new ZoomDevice("COM2", 0, 20, 2);
        zoomDevice.start();
    }

    @After
    public void after() throws IOException {
        zoomDevice.stop();
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
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.scr").version("1.8.2"),
                mavenBundle().groupId("org.knowhowlab.comm").artifactId("org.knowhowlab.comm.rxtx-patched").version(System.getProperty("project.version")),
                mavenBundle().groupId("org.knowhowlab.comm").artifactId("org.knowhowlab.comm.testing.rxtx").version(System.getProperty("project.version")),
                mavenBundle().groupId("org.knowhowlab.comm").artifactId("org.knowhowlab.comm.testing.it.osgi.rxtx").version(System.getProperty("project.version"))
        );
        return combine(options, baseConfiguration());
    }

    @Test
    public void testAllServices() throws InvalidSyntaxException, InterruptedException {
        // assert ZoomDriver service is available in OSGi registry
        assertServiceAvailable(ZoomDriver.class, 5, SECONDS);
    }

    @Test
    public void testFunctionality() throws InvalidSyntaxException, InterruptedException {
        ZoomDriver zoomDriver = ServiceUtils.getService(bc, ZoomDriver.class, 5, SECONDS);

        // get value
        assertThat(zoomDriver.getZoom(), is(0));

        // zoom in
        assertThat(zoomDriver.zoomIn(), is(true));
        assertThat(zoomDriver.getZoom(), is(2));
        assertThat(zoomDriver.zoomIn(), is(true));
        assertThat(zoomDriver.getZoom(), is(4));

        // zoom out
        assertThat(zoomDriver.zoomOut(), is(true));
        assertThat(zoomDriver.getZoom(), is(2));

        // reset
        assertThat(zoomDriver.reset(), is(true));
        assertThat(zoomDriver.getZoom(), is(0));

        // out of margin
        assertThat(zoomDriver.zoomOut(), is(false));
    }
}
