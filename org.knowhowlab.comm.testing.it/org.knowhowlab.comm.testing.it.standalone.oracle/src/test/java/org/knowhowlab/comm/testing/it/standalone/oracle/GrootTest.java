package org.knowhowlab.comm.testing.it.standalone.oracle;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.knowhowlab.comm.testing.common.config.DriverConfig;
import org.knowhowlab.comm.testing.common.config.PortConfig;
import org.knowhowlab.comm.testing.common.config.PortType;
import org.knowhowlab.comm.testing.oracle.MockOracleDriver;

import javax.comm.CommPort;
import javax.comm.CommPortIdentifier;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class GrootTest {
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

    private static DriverConfig createConfig() throws IOException {
        List<PortConfig> ports = new ArrayList<PortConfig>();
        PortConfig com1 = new PortConfig("/dev/ttyS0", PortType.SERIAL);
        ports.add(com1);
        PortConfig com2 = new PortConfig("/dev/ttyS1", PortType.SERIAL, com1);
        ports.add(com2);
        return new DriverConfig(ports);
    }

    @Test(expected = RuntimeException.class)
    public void testWrongPort() throws Exception {
        new Groot("COM1");
    }

    @Test
    public void testSimple() throws Exception {
        Groot groot = new Groot("/dev/ttyS0");
        groot.start();

        CommPortIdentifier com2Id = CommPortIdentifier.getPortIdentifier("/dev/ttyS1");
        CommPort com2 = com2Id.open("Test", 2000);

        com2.getOutputStream().write("Hello!\n".getBytes(Charset.defaultCharset()));

        byte[] buff = new byte[32];
        int read = com2.getInputStream().read(buff);

        assertThat(new String(buff, 0, read), is(equalTo("I am Groot!\n")));

        com2.close();

        groot.stop();
    }
}