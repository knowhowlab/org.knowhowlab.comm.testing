package org.knowhowlab.comm.testing.it.osgi.tests;

import org.junit.Test;
import org.knowhowlab.osgi.testing.utils.FilterUtils;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.monitor.MonitorAdmin;
import org.osgi.service.monitor.Monitorable;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.knowhowlab.osgi.testing.assertions.ServiceAssert.assertServiceAvailable;
import static org.knowhowlab.osgi.testing.assertions.ServiceAssert.assertServiceUnavailable;
import static org.knowhowlab.osgi.testing.utils.FilterUtils.and;
import static org.knowhowlab.osgi.testing.utils.FilterUtils.create;
import static org.knowhowlab.osgi.testing.utils.FilterUtils.eq;
import static org.ops4j.pax.exam.CoreOptions.*;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.osgi.framework.Constants.SERVICE_PID;

/**
 * @author dpishchukhin
 */
public class SpeedometerIntegrationTest extends AbstractTest {
    @Configuration
    public static Option[] customTestConfiguration() {
        Option[] options = options(
                mavenBundle().groupId("org.knowhowlab.osgi").artifactId("monitoradmin").version("1.0.2"),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.scr").version("1.8.2"),
                mavenBundle().groupId("org.knowhowlab.comm").artifactId("org.knowhowlab.comm.testing.it.osgi.oracle").version(System.getProperty("project.version"))
        );
        return combine(options, baseConfiguration());
    }

    @Test
    public void testAllServices() throws InvalidSyntaxException {
        // assert PackageAdmin service is available in OSGi registry
        assertServiceAvailable(MonitorAdmin.class, 5, SECONDS);
        // assert PackageAdmin service is available in OSGi registry
        assertServiceAvailable(and(create(Monitorable.class), eq(SERVICE_PID, "speedometer")), 5, SECONDS);
    }
}
