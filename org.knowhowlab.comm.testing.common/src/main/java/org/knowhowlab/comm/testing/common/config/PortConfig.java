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

/**
 * @author dpishchukhin
 */
public class PortConfig {
    private String name;
    private PortConfig linkPort;
    private PortType type;

    public PortConfig(String name, PortType type) {
        this(name, type, null);
    }

    public PortConfig(String name, PortType type, PortConfig linkPort) {
        this.name = name;
        this.type = type;
        this.linkPort = linkPort;
        if (linkPort != null) {
            linkPort.setLinkPort(this);
        }
    }

    public String getName() {
        return name;
    }

    public PortConfig getLinkPort() {
        return linkPort;
    }

    public PortType getType() {
        return type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLinkPort(PortConfig linkPort) {
        this.linkPort = linkPort;
        if (linkPort != null && !linkPort.getLinkPort().equals(this)) {
            linkPort.setLinkPort(this);
        }
    }

    public void setType(PortType type) {
        this.type = type;
    }
}
