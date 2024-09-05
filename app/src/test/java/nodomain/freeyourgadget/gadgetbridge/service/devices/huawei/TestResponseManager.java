/*  Copyright (C) 2022-2023 Martin.JM

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Workout;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.Transaction;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetEventAlarmList;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.Request;

@RunWith(MockitoJUnitRunner.class)
public class TestResponseManager {

    HuaweiSupportProvider supportProvider = new HuaweiSupportProvider(new HuaweiLESupport()) {

        @Override
        public boolean isBLE() {
            return true;
        }

        @Override
        public Context getContext() {
            return null;
        }

        @Override
        public GBDevice getDevice() {
            return null;
        }

        @Override
        public byte[] getSerial() {
            return new byte[0];
        }

        @Override
        public String getDeviceMac() {
            return null;
        }

        @Override
        public byte[] getMacAddress() {
            return new byte[0];
        }

        @Override
        public byte[] getAndroidId() {
            return new byte[0];
        }

        @Override
        public short getNotificationId() {
            return 0;
        }

        @Override
        public TransactionBuilder createBrTransactionBuilder(String taskName) {
            return null;
        }

        @Override
        public nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder createLeTransactionBuilder(String taskName) {
            return null;
        }

        @Override
        public void performConnected(Transaction transaction) throws IOException {

        }

        @Override
        public void performConnected(nodomain.freeyourgadget.gadgetbridge.service.btle.Transaction transaction) throws IOException {

        }

        @Override
        public void evaluateGBDeviceEvent(GBDeviceEvent deviceEvent) {

        }

        @Override
        public BluetoothGattCharacteristic getLeCharacteristic(UUID uuid) {
            return null;
        }

        @Override
        public HuaweiPacket.ParamsProvider getParamsProvider() {
            return null;
        }

        @Override
        public void addInProgressRequest(Request request) {

        }

        @Override
        public void removeInProgressRequests(Request request) {

        }

        @Override
        public void setSecretKey(byte[] authKey) {

        }

        @Override
        public byte[] getSecretKey() {
            return new byte[0];
        }

        @Override
        public void addTotalFitnessData(int steps, int calories, int distance) {

        }

        @Override
        public void addStepData(int timestamp, short steps, short calories, short distance, byte spo, byte heartrate) {

        }

        @Override
        public Long addWorkoutTotalsData(Workout.WorkoutTotals.Response packet) {
            return null;
        }

        @Override
        public void addWorkoutSampleData(Long workoutId, List<Workout.WorkoutData.Response.Data> dataList) {

        }

        @Override
        public void addWorkoutPaceData(Long workoutId, List<Workout.WorkoutPace.Response.Block> paceList) {

        }

        @Override
        public void sendSetMusic() {

        }
    };

    Field handlersField;
    Field receivedPacketField;
    Field asynchronousResponseField;

    @Before
    public void beforeClass() throws NoSuchFieldException {
        handlersField = ResponseManager.class.getDeclaredField("handlers");
        handlersField.setAccessible(true);

        asynchronousResponseField = ResponseManager.class.getDeclaredField("asynchronousResponse");
        asynchronousResponseField.setAccessible(true);

        receivedPacketField = ResponseManager.class.getDeclaredField("receivedPacket");
        receivedPacketField.setAccessible(true);
    }

    @Test
    public void testAddHandler() throws IllegalAccessException {
        Request input = new Request(supportProvider);

        List<Request> expectedHandlers = Collections.synchronizedList(new ArrayList<Request>());
        expectedHandlers.add(input);

        ResponseManager responseManager = new ResponseManager(supportProvider);
        responseManager.addHandler(input);

        Assert.assertEquals(expectedHandlers, handlersField.get(responseManager));
    }

    @Test
    public void testRemoveHandler() throws IllegalAccessException {
        Request input = new Request(supportProvider);
        Request extra = new Request(supportProvider);

        List<Request> inputHandlers = Collections.synchronizedList(new ArrayList<Request>());
        inputHandlers.add(extra);
        inputHandlers.add(input);
        inputHandlers.add(extra);

        List<Request> expectedHandlers = Collections.synchronizedList(new ArrayList<Request>());
        expectedHandlers.add(extra);
        expectedHandlers.add(extra);

        ResponseManager responseManager = new ResponseManager(supportProvider);
        handlersField.set(responseManager, inputHandlers);

        responseManager.removeHandler(input);

        Assert.assertEquals(expectedHandlers, handlersField.get(responseManager));
    }

    @Test
    public void testRemoveHandlerClass() throws IllegalAccessException {
        Request input1 = new GetEventAlarmList(supportProvider);
        Request input2 = new GetEventAlarmList(supportProvider);
        Request extra = new Request(supportProvider);

        List<Request> inputHandlers = Collections.synchronizedList(new ArrayList<Request>());
        inputHandlers.add(extra);
        inputHandlers.add(input1);
        inputHandlers.add(extra);
        inputHandlers.add(input2);

        List<Request> expectedHandlers = Collections.synchronizedList(new ArrayList<Request>());
        expectedHandlers.add(extra);
        expectedHandlers.add(extra);

        ResponseManager responseManager = new ResponseManager(supportProvider);
        handlersField.set(responseManager, inputHandlers);

        responseManager.removeHandler(GetEventAlarmList.class);

        Assert.assertEquals(expectedHandlers, handlersField.get(responseManager));
    }

    @Test
    public void testHandleDataCompletePacketSynchronous() throws Exception {
        // Note that this is not a proper packet, but that doesn't matter as we're not testing
        // the packet parsing.
        byte[] input = {0x01, 0x02, 0x03, 0x04};

        AsynchronousResponse mockAsynchronousResponse = Mockito.mock(AsynchronousResponse.class);

        HuaweiPacket mockHuaweiPacket = Mockito.mock(HuaweiPacket.class);
        mockHuaweiPacket.complete = true;
        when(mockHuaweiPacket.parse((byte[]) any()))
                .thenReturn(mockHuaweiPacket);

        Request request1 = Mockito.mock(Request.class);
        when(request1.handleResponse((HuaweiPacket) any()))
                .thenReturn(true);
        when(request1.autoRemoveFromResponseHandler())
                .thenReturn(true);
        Request request2 = Mockito.mock(Request.class);

        List<Request> inputHandlers = Collections.synchronizedList(new ArrayList<Request>());
        inputHandlers.add(request1);
        inputHandlers.add(request2);

        List<Request> expectedHandlers = Collections.synchronizedList(new ArrayList<Request>());
        expectedHandlers.add(request2);

        ResponseManager responseManager = new ResponseManager(supportProvider);
        handlersField.set(responseManager, inputHandlers);
        receivedPacketField.set(responseManager, mockHuaweiPacket);
        asynchronousResponseField.set(responseManager, mockAsynchronousResponse);

        responseManager.handleData(input);

        Assert.assertEquals(expectedHandlers, handlersField.get(responseManager));
        Assert.assertNull(receivedPacketField.get(responseManager));

        verify(mockHuaweiPacket, times(1)).parse(input);
        verify(mockAsynchronousResponse, times(0)).handleResponse((HuaweiPacket) any());
        verify(request1, times(1)).handleResponse(mockHuaweiPacket);
        verify(request1, times(1)).handleResponse();
        verify(request2, times(0)).handleResponse((HuaweiPacket) any());
        verify(request2, times(0)).handleResponse();
    }

    @Test
    public void testHandleDataCompletePacketAsynchronous() throws Exception {
        // Note that this is not a proper packet, but that doesn't matter as we're not testing
        // the packet parsing.
        byte[] input = {0x01, 0x02, 0x03, 0x04};

        AsynchronousResponse mockAsynchronousResponse = Mockito.mock(AsynchronousResponse.class);

        HuaweiPacket mockHuaweiPacket = Mockito.mock(HuaweiPacket.class);
        mockHuaweiPacket.complete = true;
        when(mockHuaweiPacket.parse((byte[]) any()))
                .thenReturn(mockHuaweiPacket);

        Request request1 = Mockito.mock(Request.class);
        when(request1.handleResponse((HuaweiPacket) any()))
                .thenReturn(false);
        Request request2 = Mockito.mock(Request.class);
        when(request2.handleResponse((HuaweiPacket) any()))
                .thenReturn(false);

        List<Request> inputHandlers = Collections.synchronizedList(new ArrayList<Request>());
        inputHandlers.add(request1);
        inputHandlers.add(request2);

        List<Request> expectedHandlers = Collections.synchronizedList(new ArrayList<Request>());
        expectedHandlers.add(request1);
        expectedHandlers.add(request2);

        ResponseManager responseManager = new ResponseManager(supportProvider);
        handlersField.set(responseManager, inputHandlers);
        receivedPacketField.set(responseManager, mockHuaweiPacket);
        asynchronousResponseField.set(responseManager, mockAsynchronousResponse);

        responseManager.handleData(input);

        Assert.assertEquals(expectedHandlers, handlersField.get(responseManager));
        Assert.assertNull(receivedPacketField.get(responseManager));

        verify(mockHuaweiPacket, times(1)).parse(input);
        verify(mockAsynchronousResponse, times(1)).handleResponse(mockHuaweiPacket);
        verify(request1, times(1)).handleResponse(mockHuaweiPacket);
        verify(request1, times(0)).handleResponse();
        verify(request2, times(1)).handleResponse(mockHuaweiPacket);
        verify(request2, times(0)).handleResponse();
    }

    @Test
    public void testHandleDataTwoPartialPacketsSynchronous() throws Exception {
        // Note that this is not a proper packet, but that doesn't matter as we're not testing
        // the packet parsing.
        byte[] input1 = {0x01, 0x02, 0x03, 0x04};
        byte[] input2 = {0x05, 0x06, 0x07, 0x08};

        AsynchronousResponse mockAsynchronousResponse = Mockito.mock(AsynchronousResponse.class);

        HuaweiPacket mockHuaweiPacket = Mockito.mock(HuaweiPacket.class);
        mockHuaweiPacket.complete = false;
        when(mockHuaweiPacket.parse((byte[]) any()))
                .thenReturn(mockHuaweiPacket);

        Request request1 = Mockito.mock(Request.class);
        when(request1.handleResponse((HuaweiPacket) any()))
                .thenReturn(true);
        when(request1.autoRemoveFromResponseHandler())
                .thenReturn(true);
        Request request2 = Mockito.mock(Request.class);

        List<Request> inputHandlers = Collections.synchronizedList(new ArrayList<Request>());
        inputHandlers.add(request1);
        inputHandlers.add(request2);

        List<Request> expectedHandlers1 = Collections.synchronizedList(new ArrayList<Request>());
        expectedHandlers1.add(request1);
        expectedHandlers1.add(request2);

        List<Request> expectedHandlers2 = Collections.synchronizedList(new ArrayList<Request>());
        expectedHandlers2.add(request2);

        ResponseManager responseManager = new ResponseManager(supportProvider);
        handlersField.set(responseManager, inputHandlers);
        receivedPacketField.set(responseManager, mockHuaweiPacket);
        asynchronousResponseField.set(responseManager, mockAsynchronousResponse);

        responseManager.handleData(input1);

        Assert.assertEquals(expectedHandlers1, handlersField.get(responseManager));
        Assert.assertEquals(mockHuaweiPacket, receivedPacketField.get(responseManager));

        verify(mockHuaweiPacket, times(1)).parse(input1);
        verify(mockAsynchronousResponse, times(0)).handleResponse((HuaweiPacket) any());
        verify(request1, times(0)).handleResponse(mockHuaweiPacket);
        verify(request1, times(0)).handleResponse();
        verify(request2, times(0)).handleResponse((HuaweiPacket) any());
        verify(request2, times(0)).handleResponse();

        mockHuaweiPacket.complete = true;
        responseManager.handleData(input2);

        Assert.assertEquals(expectedHandlers2, handlersField.get(responseManager));
        Assert.assertNull(receivedPacketField.get(responseManager));

        verify(mockHuaweiPacket, times(1)).parse(input2);
        verify(mockAsynchronousResponse, times(0)).handleResponse((HuaweiPacket) any());
        verify(request1, times(1)).handleResponse(mockHuaweiPacket);
        verify(request1, times(1)).handleResponse();
        verify(request2, times(0)).handleResponse((HuaweiPacket) any());
        verify(request2, times(0)).handleResponse();
    }

    @Test
    public void testHandleDataTwoPartialPacketsAsynchronous() throws Exception {
        // Note that this is not a proper packet, but that doesn't matter as we're not testing
        // the packet parsing.
        byte[] input1 = {0x01, 0x02, 0x03, 0x04};
        byte[] input2 = {0x05, 0x06, 0x07, 0x08};

        AsynchronousResponse mockAsynchronousResponse = Mockito.mock(AsynchronousResponse.class);

        HuaweiPacket mockHuaweiPacket = Mockito.mock(HuaweiPacket.class);
        mockHuaweiPacket.complete = false;
        when(mockHuaweiPacket.parse((byte[]) any()))
                .thenReturn(mockHuaweiPacket);

        Request request1 = Mockito.mock(Request.class);
        when(request1.handleResponse((HuaweiPacket) any()))
                .thenReturn(false);
        Request request2 = Mockito.mock(Request.class);
        when(request2.handleResponse((HuaweiPacket) any()))
                .thenReturn(false);

        List<Request> inputHandlers = Collections.synchronizedList(new ArrayList<Request>());
        inputHandlers.add(request1);
        inputHandlers.add(request2);

        List<Request> expectedHandlers = Collections.synchronizedList(new ArrayList<Request>());
        expectedHandlers.add(request1);
        expectedHandlers.add(request2);

        ResponseManager responseManager = new ResponseManager(supportProvider);
        handlersField.set(responseManager, inputHandlers);
        receivedPacketField.set(responseManager, mockHuaweiPacket);
        asynchronousResponseField.set(responseManager, mockAsynchronousResponse);

        responseManager.handleData(input1);

        Assert.assertEquals(expectedHandlers, handlersField.get(responseManager));
        Assert.assertEquals(mockHuaweiPacket, receivedPacketField.get(responseManager));

        verify(mockHuaweiPacket, times(1)).parse(input1);
        verify(mockAsynchronousResponse, times(0)).handleResponse((HuaweiPacket) any());
        verify(request1, times(0)).handleResponse(mockHuaweiPacket);
        verify(request1, times(0)).handleResponse();
        verify(request2, times(0)).handleResponse((HuaweiPacket) any());
        verify(request2, times(0)).handleResponse();

        mockHuaweiPacket.complete = true;
        responseManager.handleData(input2);

        Assert.assertEquals(expectedHandlers, handlersField.get(responseManager));
        Assert.assertNull(receivedPacketField.get(responseManager));

        verify(mockHuaweiPacket, times(1)).parse(input2);
        verify(mockAsynchronousResponse, times(1)).handleResponse((HuaweiPacket) any());
        verify(request1, times(1)).handleResponse(mockHuaweiPacket);
        verify(request1, times(0)).handleResponse();
        verify(request2, times(1)).handleResponse((HuaweiPacket) any());
        verify(request2, times(0)).handleResponse();
    }

    @Test
    public void testOnSocketReadMultiplePacketSplit() throws IllegalAccessException, HuaweiPacket.ParseException {
        byte[] expected = {(byte) 0x5A, (byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x04, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x99, (byte) 0x6B};

        byte[] data1 = {(byte) 0x5A, (byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x04, (byte) 0x01, (byte) 0x01};
        byte[] data2 = {(byte) 0x01, (byte) 0x02, (byte) 0x99, (byte) 0x6B, (byte) 0x5A, (byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x04, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x99, (byte) 0x6B};

        HuaweiPacket expectedPacket = new HuaweiPacket(supportProvider.getParamsProvider()).parse(expected);

        AsynchronousResponse mockAsynchronousResponse = Mockito.mock(AsynchronousResponse.class);

        ResponseManager responseManager = new ResponseManager(supportProvider);
        asynchronousResponseField.set(responseManager, mockAsynchronousResponse);

        responseManager.handleData(data1);
        responseManager.handleData(data2);

        verify(mockAsynchronousResponse, times(2)).handleResponse(expectedPacket);
    }
}
