package org.knowhowlab.comm.testing.it.osgi.tests.device;

/**
 * @author dpishchukhin
 */
public class ZoomDevice {
    private final String portName;
    private final int minValue;
    private final int maxValue;
    private final int step;

    public ZoomDevice(String portName, int minValue, int maxValue, int step) {
        this.portName = portName;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.step = step;
        // todo
    }

    public void start() {
        // todo
    }

    public void stop() {
        // todo
    }
}
