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

package org.knowhowlab.comm.testing.oracle;

import org.knowhowlab.comm.testing.common.DataListener;
import org.knowhowlab.comm.testing.common.Linkable;

import javax.comm.SerialPort;
import javax.comm.SerialPortEvent;
import javax.comm.SerialPortEventListener;
import javax.comm.UnsupportedCommOperationException;
import java.io.*;
import java.util.TooManyListenersException;

/**
 * @author dpishchukhin
 */
public class MockOracleSerialPort extends SerialPort implements Linkable, DataListener {
    private SerialPortEventListener listener;
    private boolean notifyOnDataAvailable = false;
    private PipedInputStream inputStream;
    private PipedOutputStream outputStream;
    private boolean connected;
    private Linkable linkTo;

    public MockOracleSerialPort(String name) {
        this.name = name;
        inputStream = new PipedInputStream();
    }

    @Override
    public int getBaudRate() {
        return 0;
    }

    @Override
    public int getDataBits() {
        return 0;
    }

    @Override
    public int getStopBits() {
        return 0;
    }

    @Override
    public int getParity() {
        return 0;
    }

    @Override
    public void sendBreak(int i) {

    }

    @Override
    public void setFlowControlMode(int i) throws UnsupportedCommOperationException {

    }

    @Override
    public int getFlowControlMode() {
        return 0;
    }

    @Override
    public void setSerialPortParams(int i, int i2, int i3, int i4) throws UnsupportedCommOperationException {

    }

    @Override
    public void setDTR(boolean b) {

    }

    @Override
    public boolean isDTR() {
        return false;
    }

    @Override
    public void setRTS(boolean b) {

    }

    @Override
    public boolean isRTS() {
        return false;
    }

    @Override
    public boolean isCTS() {
        return false;
    }

    @Override
    public boolean isDSR() {
        return false;
    }

    @Override
    public boolean isRI() {
        return false;
    }

    @Override
    public boolean isCD() {
        return false;
    }

    @Override
    public void addEventListener(SerialPortEventListener serialPortEventListener) throws TooManyListenersException {
        if (listener != null) {
            throw new TooManyListenersException("Listener is already set");
        }
        listener = serialPortEventListener;
    }

    @Override
    public void removeEventListener() {
        listener = null;
    }

    @Override
    public void notifyOnDataAvailable(boolean b) {
        notifyOnDataAvailable = b;
    }

    @Override
    public void notifyOnOutputEmpty(boolean b) {

    }

    @Override
    public void notifyOnCTS(boolean b) {

    }

    @Override
    public void notifyOnDSR(boolean b) {

    }

    @Override
    public void notifyOnRingIndicator(boolean b) {

    }

    @Override
    public void notifyOnCarrierDetect(boolean b) {

    }

    @Override
    public void notifyOnOverrunError(boolean b) {

    }

    @Override
    public void notifyOnParityError(boolean b) {

    }

    @Override
    public void notifyOnFramingError(boolean b) {

    }

    @Override
    public void notifyOnBreakInterrupt(boolean b) {

    }

    @Override
    public InputStream getInputStream() throws IOException {
        return inputStream;
    }

    @Override
    public PipedOutputStream getOutputStream(final DataListener listener) throws IOException {
        outputStream = new PipedOutputStream() {
            @Override
            public void write(byte[] b) throws IOException {
                super.write(b);
                listener.dataAvailable();
            }
        };
        return outputStream;
    }

    @Override
    public void reset() throws IOException {
        ((MockOracleSerialPort)linkTo).resetInternal();
        linkTo.linkTo(this);
    }

    private void resetInternal() throws IOException {
        inputStream = new PipedInputStream();
        this.inputStream.connect(linkTo.getOutputStream(this));
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return outputStream;
    }

    @Override
    public void enableReceiveThreshold(int i) throws UnsupportedCommOperationException {

    }

    @Override
    public void disableReceiveThreshold() {

    }

    @Override
    public boolean isReceiveThresholdEnabled() {
        return false;
    }

    @Override
    public int getReceiveThreshold() {
        return 0;
    }

    @Override
    public void enableReceiveTimeout(int i) throws UnsupportedCommOperationException {

    }

    @Override
    public void disableReceiveTimeout() {

    }

    @Override
    public boolean isReceiveTimeoutEnabled() {
        return false;
    }

    @Override
    public int getReceiveTimeout() {
        return 0;
    }

    @Override
    public void enableReceiveFraming(int i) throws UnsupportedCommOperationException {

    }

    @Override
    public void disableReceiveFraming() {

    }

    @Override
    public boolean isReceiveFramingEnabled() {
        return false;
    }

    @Override
    public int getReceiveFramingByte() {
        return 0;
    }

    @Override
    public void setInputBufferSize(int i) {

    }

    @Override
    public int getInputBufferSize() {
        return 0;
    }

    @Override
    public void setOutputBufferSize(int i) {

    }

    @Override
    public int getOutputBufferSize() {
        return 0;
    }

    @Override
    public void linkTo(Linkable linkTo) throws IOException {
        if (!connected) {
            connected = true;
            this.inputStream.connect(linkTo.getOutputStream(this));
            this.linkTo = linkTo;
            this.linkTo.linkTo(this);
        }
    }

    @Override
    public void dataAvailable() {
        if (notifyOnDataAvailable && listener != null) {
            listener.serialEvent(new SerialPortEvent(this, SerialPortEvent.DATA_AVAILABLE, false, true));
        }
    }

    @Override
    public void close() {
        try {
            this.inputStream.close();
            this.outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.close();
    }
}
