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

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class DriverConfigTest {
    @Test
    public void testMarshal() throws Exception {
        String configStr = createConfigString();

        assertThat(configStr, notNullValue());
        assertThat(configStr.contains("port.0.name=COM1"), is(equalTo(true)));
        assertThat(configStr.contains("port.0.type=SERIAL"), is(equalTo(true)));
        assertThat(configStr.contains("port.0.linkTo=COM2"), is(equalTo(true)));
        assertThat(configStr.contains("port.1.name=COM2"), is(equalTo(true)));
        assertThat(configStr.contains("port.1.type=SERIAL"), is(equalTo(true)));
        assertThat(configStr.contains("port.1.linkTo=COM1"), is(equalTo(true)));
    }

    @Test
    public void testUnmarshal() throws Exception {
        String configStr = createConfigString();
        DriverConfig config = DriverConfig.unmarshal(configStr);

        assertThat(config, notNullValue());

        assertThat(config.getPorts().size(), is(equalTo(2)));
        assertThat(config.getPorts().get(0).getName(), is(equalTo("COM1")));
        assertThat(config.getPorts().get(0).getType(), is(equalTo(PortType.SERIAL)));
        assertThat(config.getPorts().get(0).getLinkPort().getName(), is(equalTo("COM2")));
        assertThat(config.getPorts().get(1).getName(), is(equalTo("COM2")));
        assertThat(config.getPorts().get(1).getType(), is(equalTo(PortType.SERIAL)));
        assertThat(config.getPorts().get(1).getLinkPort().getName(), is(equalTo("COM1")));
    }

    private String createConfigString() throws IOException {
        List<PortConfig> ports = new ArrayList<PortConfig>();
        PortConfig com1 = new PortConfig("COM1", PortType.SERIAL);
        ports.add(com1);
        PortConfig com2 = new PortConfig("COM2", PortType.SERIAL, com1);
        ports.add(com2);
        DriverConfig config = new DriverConfig(ports);
        return config.marshal();
    }
}