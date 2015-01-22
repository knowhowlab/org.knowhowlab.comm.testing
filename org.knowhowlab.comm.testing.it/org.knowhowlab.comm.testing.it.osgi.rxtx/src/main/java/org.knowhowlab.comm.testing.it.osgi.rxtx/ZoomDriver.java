package org.knowhowlab.comm.testing.it.osgi.rxtx;

/**
 * @author dpishchukhin
 */
public interface ZoomDriver {
    boolean zoomIn();

    boolean zoomOut();

    boolean reset();

    int getZoom();
}
