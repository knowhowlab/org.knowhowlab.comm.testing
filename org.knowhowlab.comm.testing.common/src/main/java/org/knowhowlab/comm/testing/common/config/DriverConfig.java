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

package org.knowhowlab.comm.testing.common.config;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;

/**
 * @author dpishchukhin
 */
public class DriverConfig {
    private static final String PORT_PREFIX = "port.";
    private static final String NAME_SUFFIX = ".name";
    private static final String TYPE_SUFFIX = ".type";
    private static final String LINK_TO_SUFFIX = ".linkTo";

    private List<PortConfig> ports = new ArrayList<PortConfig>();

    public DriverConfig(List<PortConfig> ports) {
        this.ports = ports;
    }


    public List<PortConfig> getPorts() {
        return ports;
    }

    public static DriverConfig unmarshal(String str) throws IOException {
        Properties props = new Properties();
        props.load(new StringReader(str));
        List<PortConfig> ports = new ArrayList<PortConfig>();
        TreeSet<Object> keys = new TreeSet<Object>(props.keySet());
        int processingIndex = 0;
        for (Object key : keys) {
            String keyStr = (String) key;
            int index = readIndex(keyStr);
            if (index == processingIndex) {
                String name = props.getProperty(PORT_PREFIX + index + NAME_SUFFIX);
                PortType type = PortType.valueOf(props.getProperty(PORT_PREFIX + index + TYPE_SUFFIX));
                PortConfig linkToPort = findLinkToPort(ports, props.getProperty(PORT_PREFIX + index + LINK_TO_SUFFIX));
                PortConfig port = new PortConfig(name, type, linkToPort);
                ports.add(port);
                processingIndex++;
            }
        }
        return new DriverConfig(ports);
    }

    private static PortConfig findLinkToPort(List<PortConfig> ports, String name) {
        for (PortConfig port : ports) {
            if (name.equals(port.getName())) {
                return port;
            }
        }
        return null;
    }

    private static int readIndex(String key) {
        return Integer.parseInt(key.substring(PORT_PREFIX.length(), key.indexOf(".", PORT_PREFIX.length())));
    }

    public String marshal() throws IOException {
        Properties props = new Properties();
        if (ports != null) {
            for (int i = 0; i < ports.size(); i++) {
                PortConfig portConfig = ports.get(i);
                marshal(props, i, portConfig);
            }
        }
        StringWriter writer = new StringWriter();
        props.store(writer, null);
        return writer.toString();
    }

    private void marshal(Properties props, int index, PortConfig port) {
        props.put(PORT_PREFIX + index + NAME_SUFFIX, port.getName());
        props.put(PORT_PREFIX + index + TYPE_SUFFIX, port.getType().name());
        props.put(PORT_PREFIX + index + LINK_TO_SUFFIX, port.getLinkPort().getName());

    }
}
