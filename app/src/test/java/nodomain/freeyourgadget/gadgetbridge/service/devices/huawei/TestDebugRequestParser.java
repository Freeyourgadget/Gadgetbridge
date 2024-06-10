/*  Copyright (C) 2023 Martin.JM

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

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Workout;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.Transaction;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.DebugRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.Request;

public class TestDebugRequestParser {

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
        public void addSleepActivity(int timestamp, short duration, byte type) {

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

    @Test
    public void emptyPacket() throws Request.RequestCreationException {
        DebugRequest debugRequest = new DebugRequest(supportProvider);

        HuaweiPacket expected = new HuaweiPacket(supportProvider.getParamsProvider());
        expected.serviceId = 1;
        expected.commandId = 1;
        expected.setEncryption(false);
        expected.complete = true;

        HuaweiPacket packet = debugRequest.parseDebugString("1,1,false,false");

        Assert.assertEquals(expected, packet);
    }

    @Test
    public void emptyPacketShortBooleans() throws Request.RequestCreationException {
        DebugRequest debugRequest = new DebugRequest(supportProvider);

        HuaweiPacket expected = new HuaweiPacket(supportProvider.getParamsProvider());
        expected.serviceId = 1;
        expected.commandId = 1;
        expected.setEncryption(false);
        expected.complete = true;

        HuaweiPacket packet = debugRequest.parseDebugString("1,1,f,f");

        Assert.assertEquals(expected, packet);
    }

    @Test
    public void emptyTag() throws Request.RequestCreationException {
        DebugRequest debugRequest = new DebugRequest(supportProvider);

        HuaweiPacket expected = new HuaweiPacket(supportProvider.getParamsProvider());
        expected.serviceId = 1;
        expected.commandId = 1;
        expected.setEncryption(false);
        expected.setTlv(new HuaweiTLV().put(1));
        expected.complete = true;

        HuaweiPacket packet = debugRequest.parseDebugString("1,1,false,false,(1,/)");

        Assert.assertEquals(expected, packet);
    }

    @Test
    public void byteTag() throws Request.RequestCreationException {
        DebugRequest debugRequest = new DebugRequest(supportProvider);

        HuaweiPacket expected = new HuaweiPacket(supportProvider.getParamsProvider());
        expected.serviceId = 1;
        expected.commandId = 1;
        expected.setEncryption(false);
        expected.setTlv(new HuaweiTLV().put(1, (byte) 1));
        expected.complete = true;

        HuaweiPacket packet = debugRequest.parseDebugString("1,1,false,false,(1,B1)");

        Assert.assertEquals(expected, packet);
    }

    @Test
    public void shortTag() throws Request.RequestCreationException {
        DebugRequest debugRequest = new DebugRequest(supportProvider);

        HuaweiPacket expected = new HuaweiPacket(supportProvider.getParamsProvider());
        expected.serviceId = 1;
        expected.commandId = 1;
        expected.setEncryption(false);
        expected.setTlv(new HuaweiTLV().put(1, (short) 1));
        expected.complete = true;

        HuaweiPacket packet = debugRequest.parseDebugString("1,1,false,false,(1,S1)");

        Assert.assertEquals(expected, packet);
    }

    @Test
    public void integerTag() throws Request.RequestCreationException {
        DebugRequest debugRequest = new DebugRequest(supportProvider);

        HuaweiPacket expected = new HuaweiPacket(supportProvider.getParamsProvider());
        expected.serviceId = 1;
        expected.commandId = 1;
        expected.setEncryption(false);
        expected.setTlv(new HuaweiTLV().put(1, (int) 1));
        expected.complete = true;

        HuaweiPacket packet = debugRequest.parseDebugString("1,1,false,false,(1,I1)");

        Assert.assertEquals(expected, packet);
    }

    @Test
    public void booleanTag() throws Request.RequestCreationException {
        DebugRequest debugRequest = new DebugRequest(supportProvider);

        HuaweiPacket expected = new HuaweiPacket(supportProvider.getParamsProvider());
        expected.serviceId = 1;
        expected.commandId = 1;
        expected.setEncryption(false);
        expected.setTlv(new HuaweiTLV().put(1, true));
        expected.complete = true;

        HuaweiPacket packet = debugRequest.parseDebugString("1,1,false,false,(1,b1)");

        Assert.assertEquals(expected, packet);
    }

    @Test
    public void arrayTag() throws Request.RequestCreationException {
        DebugRequest debugRequest = new DebugRequest(supportProvider);

        HuaweiPacket expected = new HuaweiPacket(supportProvider.getParamsProvider());
        expected.serviceId = 1;
        expected.commandId = 1;
        expected.setEncryption(false);
        expected.setTlv(new HuaweiTLV().put(1, new byte[] {(byte) 0xCA, (byte) 0xFE}));
        expected.complete = true;

        HuaweiPacket packet = debugRequest.parseDebugString("1,1,false,false,(1,aCAFE)");

        Assert.assertEquals(expected, packet);
    }

    @Test
    public void stringTag() throws Request.RequestCreationException {
        DebugRequest debugRequest = new DebugRequest(supportProvider);

        HuaweiPacket expected = new HuaweiPacket(supportProvider.getParamsProvider());
        expected.serviceId = 1;
        expected.commandId = 1;
        expected.setEncryption(false);
        expected.setTlv(new HuaweiTLV().put(1, new byte[] {0x79, 0x65, 0x73}));
        expected.complete = true;

        HuaweiPacket packet = debugRequest.parseDebugString("1,1,false,false,(1,-yes)");

        Assert.assertEquals(expected, packet);
    }

    @Test
    public void hexValues() throws Request.RequestCreationException {
        DebugRequest debugRequest = new DebugRequest(supportProvider);

        HuaweiPacket expected = new HuaweiPacket(supportProvider.getParamsProvider());
        expected.serviceId = 1;
        expected.commandId = 1;
        expected.setEncryption(false);
        expected.setTlv(new HuaweiTLV().put(1, new byte[] {0x79, 0x65, 0x73}));
        expected.complete = true;

        HuaweiPacket packet = debugRequest.parseDebugString("0x01,0x1,false,false,(0x01,-yes)");

        Assert.assertEquals(expected, packet);
    }

    @Test
    public void largeServiceCommand() throws Request.RequestCreationException {
        DebugRequest debugRequest = new DebugRequest(supportProvider);

        HuaweiPacket expected = new HuaweiPacket(supportProvider.getParamsProvider());
        expected.serviceId = (byte) 0xff;
        expected.commandId = (byte) 255;
        expected.setEncryption(false);
        expected.complete = true;

        HuaweiPacket packet = debugRequest.parseDebugString("0xff,255,false,false");

        Assert.assertEquals(expected, packet);
    }

    @Test
    public void subTlv() throws Request.RequestCreationException {
        DebugRequest debugRequest = new DebugRequest(supportProvider);

        HuaweiPacket expected = new HuaweiPacket(supportProvider.getParamsProvider());
        expected.serviceId = 1;
        expected.commandId = 1;
        expected.setEncryption(false);
        expected.setTlv(new HuaweiTLV()
                .put(129, new HuaweiTLV()
                        .put(1)
                        .put(2)
                ));
        expected.complete = true;

        HuaweiPacket packet = debugRequest.parseDebugString("1,1,false,false,(129,(1,/),(2,/))");

        Assert.assertEquals(expected, packet);
    }

    @Test
    public void subSubSubTlv() throws Request.RequestCreationException {
        DebugRequest debugRequest = new DebugRequest(supportProvider);

        HuaweiPacket expected = new HuaweiPacket(supportProvider.getParamsProvider());
        expected.serviceId = 1;
        expected.commandId = 1;
        expected.setEncryption(false);
        expected.setTlv(new HuaweiTLV()
                .put(129, new HuaweiTLV()
                        .put(129, new HuaweiTLV()
                                .put(129, new HuaweiTLV()
                                        .put(1)
                                )
                        )
                ));
        expected.complete = true;

        HuaweiPacket packet = debugRequest.parseDebugString("1,1,false,false,(129,(129,(129,(1,/))))");

        Assert.assertEquals(expected, packet);
    }

    @Test
    public void subTlvVCombined() throws Request.RequestCreationException {
        DebugRequest debugRequest = new DebugRequest(supportProvider);

        HuaweiPacket expected = new HuaweiPacket(supportProvider.getParamsProvider());
        expected.serviceId = 1;
        expected.commandId = 1;
        expected.setEncryption(false);
        expected.setTlv(new HuaweiTLV()
                .put(129, new HuaweiTLV()
                        .put(1)
                        .put(2, true)
                )
                .put(1, true)
        );
        expected.complete = true;

        HuaweiPacket packet = debugRequest.parseDebugString("1,1,false,false,(129,(1,/),(2,b1)),(1,b1)");

        Assert.assertEquals(expected, packet);
    }
}
