package org.knowhowlab.comm.testing.it.osgi.tests;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.knowhowlab.comm.testing.common.config.DriverConfig;
import org.knowhowlab.comm.testing.common.config.PortConfig;
import org.knowhowlab.comm.testing.common.config.PortType;
import org.knowhowlab.comm.testing.it.osgi.rxtx.ZoomDriver;
import org.knowhowlab.comm.testing.it.osgi.tests.device.ZoomDevice;
import org.knowhowlab.comm.testing.oracle.MockOracleDriver;
import org.knowhowlab.comm.testing.rxtx.MockRxTxDriver;
import org.knowhowlab.osgi.testing.utils.ServiceUtils;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.monitor.MonitorAdmin;
import org.osgi.service.monitor.Monitorable;

import javax.comm.CommPort;
import javax.comm.CommPortIdentifier;
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
}
