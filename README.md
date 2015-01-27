# Introduction

Communication API (Oracle javax.comm and RxTx gnu.io) Utilities and Mocks
for Unit and Integration testing (Java standalone and OSGi applications)

[![Build Status](https://travis-ci.org/knowhowlab/org.knowhowlab.comm.testing.svg?branch=master)](https://travis-ci.org/knowhowlab/org.knowhowlab.comm.testing)

### Blog with more samples and tutorials

[http://blog.knowhowlab.org](http://blog.knowhowlab.org)

### Mailing List

[http://groups.google.com/group/knowhowlab-comm-testing](http://groups.google.com/group/knowhowlab-comm-testing)

## Maven artifacts

Oracle javax.comm API

    <dependency>
        <groupId>org.knowhowlab.comm</groupId>
        <artifactId>org.knowhowlab.comm.testing.oracle</artifactId>
        <version>0.1</version>
    </dependency>

RxTx gnu.io API

    <dependency>
        <groupId>org.knowhowlab.comm</groupId>
        <artifactId>org.knowhowlab.comm.testing.rxtx</artifactId>
        <version>0.1</version>
    </dependency>

## OSGi sample

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


## Standalone sample

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

## FAQ

* **Which communication APIs are supported?**

        Oracle javax.comm (version 2.0) and RxTx gnu.io (version 2.1-7r2)

* **Which port types are supported?**

        Only SERIAL ports

* **Are notification events supported? Which one?**

        Only SerialPortEvent.DATA_AVAILABLE

## Roadmap

* Add support for ParallelPort and other
* Add support for the other notification events