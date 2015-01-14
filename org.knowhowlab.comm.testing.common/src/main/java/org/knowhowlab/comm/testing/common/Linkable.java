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

package org.knowhowlab.comm.testing.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedOutputStream;

/**
 * @author dpishchukhin
 */
public interface Linkable {
    String getName();

    void linkTo(Linkable linkTo) throws IOException;

    InputStream getInputStream() throws IOException;

    PipedOutputStream getOutputStream(DataListener listener) throws IOException;

    void reset() throws IOException;
}
