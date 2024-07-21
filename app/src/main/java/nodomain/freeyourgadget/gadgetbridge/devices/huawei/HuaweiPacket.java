/*  Copyright (C) 2024 Damien Gaignon, Martin.JM

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */

package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import static nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiConstants.HUAWEI_MAGIC;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Alarms;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.AccountRelated;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Calls;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.CameraRemote;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.GpsAndTime;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Watchface;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Weather;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Workout;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.DeviceConfig;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.FindPhone;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.FitnessData;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.MusicControl;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Notifications;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.FileUpload;
import nodomain.freeyourgadget.gadgetbridge.util.CheckSums;

public class HuaweiPacket {
    private static final Logger LOG = LoggerFactory.getLogger(HuaweiPacket.class);

    public static class ParamsProvider {

        protected byte authVersion;
        protected byte deviceSupportType;
        protected byte[] secretKey;
        protected int slicesize = 0xf4;
        protected boolean transactionsCrypted = true;
        protected int mtu = 65535;
        protected long encryptionCounter = 0;

        protected byte[] pinCode = null;

        protected byte interval;
        protected byte authAlgo;
        protected byte encryptMethod;
        protected byte[] firstKey;

        public void setAuthVersion(byte authVersion) {
            this.authVersion = authVersion;
        }

        public byte getAuthVersion() {
            return this.authVersion;
        }

        public void setDeviceSupportType(byte deviceSupportType) {
            this.deviceSupportType = deviceSupportType;
        }

        public byte getDeviceSupportType(){
            return this.deviceSupportType;
        }

        public void setSecretKey(byte[] secretKey) {
            this.secretKey = secretKey;
        }

        public byte[] getSecretKey() {
            return this.secretKey;
        }

        public void setTransactionsCrypted(boolean transactionsCrypted) {
            this.transactionsCrypted = transactionsCrypted;
        }

        public boolean areTransactionsCrypted() {
            return this.transactionsCrypted;
        }

        public void setMtu(int mtu) {
            this.mtu = mtu;
        }

        public int getMtu() {
            return this.mtu;
        }

        public void setSliceSize(int sliceSize) {
            this.slicesize = sliceSize;
        }

        public int getSliceSize() {
            return this.slicesize;
        }
        public void setPinCode(byte[] pinCode) {
            this.pinCode = pinCode;
        }

        public byte[] getPinCode() {
            return this.pinCode;
        }

        public void setInterval(byte interval) {
            this.interval = interval;
        }

        public byte getInterval() {
            return this.interval;
        }

        public byte[] getIv() {
            byte[] iv = null;
            if (this.deviceSupportType == 0x04) {
                iv = HuaweiCrypto.generateNonce();
            } else {
                ByteBuffer ivCounter = HuaweiCrypto.initializationVector(this.encryptionCounter);
                iv = ivCounter.array();
                this.encryptionCounter = (long)ivCounter.getInt(12) & 0xFFFFFFFFL;
            }
            return iv;
        }

        public void setEncryptionCounter(long counter) {
            this.encryptionCounter = counter;
        }

        public void setAuthAlgo(byte authAlgo) {
            this.authAlgo = authAlgo;
        }

        public byte getAuthAlgo () {
            return this.authAlgo;
        }

        public void setEncryptMethod(byte encryptMethod) {
            this.encryptMethod = encryptMethod;
        }

        public byte getEncryptMethod () {
            return this.encryptMethod;
        }

        public void setFirstKey(byte[] firstKey) {
            this.firstKey = firstKey;
        }

        public byte[] getFirstKey() {
            return firstKey;
        }
    }

    public static abstract class ParseException extends Exception {
        ParseException(String message) {
            super(message);
        }

        ParseException(String message, Exception e) {
            super(message, e);
        }
    }

    public static class LengthMismatchException extends ParseException {
        public LengthMismatchException(String message) {
            super(message);
        }
    }

    public static class MagicMismatchException extends ParseException {
        MagicMismatchException(String message) {
            super(message);
        }
    }

    public static class ChecksumIncorrectException extends ParseException {
        ChecksumIncorrectException(String message) {
            super(message);
        }
    }

    public static class MissingTagException extends ParseException {
        public MissingTagException(int tag) {
            super("Missing tag: " + Integer.toHexString(tag));
        }
    }

    public static class CryptoException extends ParseException {
        public CryptoException(String message, Exception e) {
            super(message, e);
        }
    }

    public static class JsonException extends ParseException {
        public JsonException(String message, Exception e) {
            super(message, e);
        }
    }

    public static class SupportedCommandsListException extends ParseException {
        public SupportedCommandsListException(String message) {
            super(message);
        }
    }

    public static class SerializeException extends Exception {
        public SerializeException(String message, Exception e) {
            super(message, e);
        }
    }

    protected static final int PACKET_MINIMAL_SIZE = 6;

    protected ParamsProvider paramsProvider;

    public byte serviceId = 0;
    public byte commandId = 0;
    protected HuaweiTLV tlv = null;

    private byte[] partialPacket = null;
    private byte[] payload = null;

    public boolean complete = false;

    // Encryption is enabled by default, packets which don't use it must disable it
    protected boolean isEncrypted = true;

    protected boolean isSliced = true;

    public HuaweiPacket(ParamsProvider paramsProvider) {
        this.paramsProvider = paramsProvider;
    }

    public boolean attemptDecrypt() throws ParseException {
        if (paramsProvider == null || paramsProvider.getSecretKey() == null)
            return false;
        if (this.tlv == null)
            return false;
        if (this.tlv.contains(0x7C) && this.tlv.getBoolean(0x7C)) {
            try {
                this.tlv.decrypt(paramsProvider);
                return true;
            } catch (HuaweiCrypto.CryptoException e) {
                throw new CryptoException("Decrypt exception", e);
            }
        } else {
            if (this.isEncrypted && paramsProvider.areTransactionsCrypted()) {
                // TODO: potentially a log message? We expect it to be encrypted, but it isn't.
            }
        }
        return false;
    }

    /*
     * This function is to convert the Packet into the proper subclass
     */
    protected HuaweiPacket fromPacket(HuaweiPacket packet) throws ParseException {
        this.paramsProvider = packet.paramsProvider;
        this.serviceId = packet.serviceId;
        this.commandId = packet.commandId;
        this.tlv = packet.tlv;
        this.partialPacket = packet.partialPacket;
        this.payload = packet.payload;
        this.complete = packet.complete;

        if (packet.isEncrypted)
            this.isEncrypted = true;
        else
            this.isEncrypted = this.attemptDecrypt();

        return this;
    }

    /*
     * This function is to set up the subclass for easy usage
     * Needs to be called separately so the exceptions can be used more easily
     */
    public void parseTlv() throws ParseException {}

    private void parseData(byte[] data) throws ParseException {
        if (partialPacket != null) {
            int newCapacity = partialPacket.length + data.length;
            data = ByteBuffer.allocate(newCapacity)
                    .put(partialPacket)
                    .put(data)
                    .array();
        }

        ByteBuffer buffer = ByteBuffer.wrap(data);

        if (buffer.capacity() < PACKET_MINIMAL_SIZE) {
            throw new LengthMismatchException("Packet length mismatch : "
                    + buffer.capacity()
                    + " != 6");
        }

        byte magic = buffer.get();
        short expectedSize = buffer.getShort();
        int isSliced = buffer.get();
        if (isSliced == 1 || isSliced == 2 || isSliced == 3) {
            buffer.get(); // Throw away slice flag
        }
        byte[] newPayload = new byte[buffer.remaining() - 2];
        buffer.get(newPayload, 0, buffer.remaining() - 2);
        short expectedChecksum = buffer.getShort();
        buffer.rewind();

        if (magic != HUAWEI_MAGIC) {
            throw new MagicMismatchException("Magic mismatch : "
                    + Integer.toHexString(magic)
                    + " != 0x5A");
        }

        int newPayloadLen = newPayload.length + 1;
        if (isSliced == 1 || isSliced == 2 || isSliced == 3) {
            newPayloadLen = newPayload.length + 2;
        }
        if (expectedSize != (short) newPayloadLen) {
            if (expectedSize > (short) newPayloadLen) {
                // Older band and BT version do not handle message with more than 256 bits.
                this.partialPacket = data;
                return;
            } else {
                throw new LengthMismatchException("Expected length mismatch : "
                    + expectedSize
                    + " < "
                    + (short) newPayloadLen);
            }
        }
        this.partialPacket = null;

        byte[] dataNoCRC = new byte[buffer.capacity() - 2];
        buffer.get(dataNoCRC, 0, buffer.capacity() - 2);
        short actualChecksum = (short) CheckSums.getCRC16(dataNoCRC, 0x0000);
        if (actualChecksum != expectedChecksum) {
            throw new ChecksumIncorrectException("Checksum mismatch : "
                    + String.valueOf(actualChecksum)
                    + " != "
                    + String.valueOf(expectedChecksum));
        }

        if (isSliced == 1 || isSliced == 2 || isSliced == 3) {
            if (payload != null) {
                int newCapacity = payload.length + newPayload.length;
                newPayload = ByteBuffer.allocate(newCapacity)
                        .put(payload)
                        .put(newPayload)
                        .array();
            }

            if (isSliced != 3) {
                // Sliced packet isn't complete yet
                this.payload = newPayload;
                return;
            }
        }

        this.serviceId = newPayload[0];
        this.commandId = newPayload[1];
        this.complete = true;

        if (
                (serviceId == 0x0a && commandId == 0x05) ||
                (serviceId == 0x28 && commandId == 0x06)
        ) {
            // TODO: this doesn't seem to be TLV
            return;
        }

        this.tlv = new HuaweiTLV();
        this.tlv.parse(newPayload, 2, newPayload.length - 2);
    }

    public HuaweiPacket parse(byte[] data) throws ParseException {
        this.isEncrypted = false; // Will be changed if decrypt has been performed

        parseData(data);
        if (!this.complete)
            return this;

        switch (this.serviceId) {
            case DeviceConfig.id:
                switch (this.commandId) {
                    case DeviceConfig.LinkParams.id:
                        return new DeviceConfig.LinkParams.Response(paramsProvider).fromPacket(this);
                    case DeviceConfig.SupportedServices.id:
                        return new DeviceConfig.SupportedServices.Response(paramsProvider).fromPacket(this);
                    case DeviceConfig.SupportedCommands.id:
                        return new DeviceConfig.SupportedCommands.Response(paramsProvider).fromPacket(this);
                    case DeviceConfig.ProductInfo.id:
                        return new DeviceConfig.ProductInfo.Response(paramsProvider).fromPacket(this);
                    case DeviceConfig.BondParams.id:
                        return new DeviceConfig.BondParams.Response(paramsProvider).fromPacket(this);
                    case DeviceConfig.PhoneInfo.id:
                        return new DeviceConfig.PhoneInfo.Response(paramsProvider).fromPacket(this);
                    case DeviceConfig.Auth.id:
                        return new DeviceConfig.Auth.Response(paramsProvider).fromPacket(this);
                    case DeviceConfig.BatteryLevel.id:
                        return new DeviceConfig.BatteryLevel.Response(paramsProvider).fromPacket(this);
                    case DeviceConfig.DeviceStatus.id:
                        return new DeviceConfig.DeviceStatus.Response(paramsProvider).fromPacket(this);
                    case DeviceConfig.DndLiftWristType.id:
                        return new DeviceConfig.DndLiftWristType.Response(paramsProvider).fromPacket(this);
                    case DeviceConfig.HiChain.id:
                        return new DeviceConfig.HiChain.Response(paramsProvider).fromPacket(this);
                    case DeviceConfig.PinCode.id:
                        return new DeviceConfig.PinCode.Response(paramsProvider).fromPacket(this);
                    case DeviceConfig.ExpandCapability.id:
                        return new DeviceConfig.ExpandCapability.Response(paramsProvider).fromPacket(this);
                    case DeviceConfig.ActivityType.id:
                        return new DeviceConfig.ActivityType.Response(paramsProvider).fromPacket(this);
                    case DeviceConfig.SettingRelated.id:
                        return new DeviceConfig.SettingRelated.Response(paramsProvider).fromPacket(this);
                    case DeviceConfig.SecurityNegotiation.id:
                        return new DeviceConfig.SecurityNegotiation.Response(paramsProvider).fromPacket(this);
                    case DeviceConfig.WearStatus.id:
                        return new DeviceConfig.WearStatus.Response(paramsProvider).fromPacket(this);

                    // Camera remote has same ID as DeviceConfig
                    case CameraRemote.CameraRemoteStatus.id:
                        return new CameraRemote.CameraRemoteStatus.Response(paramsProvider).fromPacket(this);

                    default:
                        this.isEncrypted = this.attemptDecrypt(); // Helps with debugging
                        return this;
                }
            case Notifications.id:
                switch (this.commandId) {
                    case Notifications.NotificationConstraints.id:
                        return new Notifications.NotificationConstraints.Response(paramsProvider).fromPacket(this);
                    case Notifications.NotificationCapabilities.id:
                        return new Notifications.NotificationCapabilities.Response(paramsProvider).fromPacket(this);
                    default:
                        return this;
                }
            case Calls.id:
                if (this.commandId == Calls.AnswerCallResponse.id)
                    return new Calls.AnswerCallResponse(paramsProvider).fromPacket(this);
                this.isEncrypted = this.attemptDecrypt(); // Helps with debugging
                return this;
            case FitnessData.id:
                switch (this.commandId) {
                    case FitnessData.FitnessTotals.id:
                        return new FitnessData.FitnessTotals.Response(paramsProvider).fromPacket(this);
                    case FitnessData.MessageCount.stepId:
                        return new FitnessData.MessageCount.Response(paramsProvider).fromPacket(this);
                    case FitnessData.MessageData.stepId:
                        return new FitnessData.MessageData.StepResponse(paramsProvider).fromPacket(this);
                    case FitnessData.MessageCount.sleepId:
                        return new FitnessData.MessageCount.Response(paramsProvider).fromPacket(this);
                    case FitnessData.MessageData.sleepId:
                        return new FitnessData.MessageData.SleepResponse(paramsProvider).fromPacket(this);
                    default:
                        this.isEncrypted = this.attemptDecrypt(); // Helps with debugging
                        return this;
                }
            case Alarms.id:
                switch (this.commandId) {
                    case Alarms.EventAlarmsList.id:
                        return new Alarms.EventAlarmsList.Response(paramsProvider).fromPacket(this);
                    case Alarms.SmartAlarmList.id:
                        return new Alarms.SmartAlarmList.Response(paramsProvider).fromPacket(this);
                    default:
                        this.isEncrypted = this.attemptDecrypt(); // Helps with debugging
                        return this;
                }
            case FindPhone.id:
                if (this.commandId == FindPhone.Response.id)
                    return new FindPhone.Response(paramsProvider).fromPacket(this);
                this.isEncrypted = this.attemptDecrypt(); // Helps with debugging
                return this;
            case Weather.id:
                switch (this.commandId) {
                    case Weather.WeatherSupport.id:
                        return new Weather.WeatherSupport.Response(paramsProvider).fromPacket(this);
                    case Weather.WeatherExtendedSupport.id:
                        return new Weather.WeatherExtendedSupport.Response(paramsProvider).fromPacket(this);
                    case Weather.WeatherStart.id:
                        return new Weather.WeatherStart.Response(paramsProvider).fromPacket(this);
                    case Weather.WeatherSunMoonSupport.id:
                        return new Weather.WeatherSunMoonSupport.Response(paramsProvider).fromPacket(this);
                    default:
                        this.isEncrypted = this.attemptDecrypt(); // Helps with debugging
                        return this;
                }
            case Workout.id:
                switch (this.commandId) {
                    case Workout.WorkoutCount.id:
                        return new Workout.WorkoutCount.Response(paramsProvider).fromPacket(this);
                    case Workout.WorkoutTotals.id:
                        return new Workout.WorkoutTotals.Response(paramsProvider).fromPacket(this);
                    case Workout.WorkoutData.id:
                        return new Workout.WorkoutData.Response(paramsProvider).fromPacket(this);
                    case Workout.WorkoutPace.id:
                        return new Workout.WorkoutPace.Response(paramsProvider).fromPacket(this);
                    default:
                        this.isEncrypted = this.attemptDecrypt(); // Helps with debugging
                        return this;
                }
            case GpsAndTime.id:
                switch (this.commandId) {
                    case GpsAndTime.GpsParameters.id:
                        return new GpsAndTime.GpsParameters.Response(paramsProvider).fromPacket(this);
                    case GpsAndTime.GpsStatus.id:
                        return new GpsAndTime.GpsStatus.Response(paramsProvider).fromPacket(this);
                    case GpsAndTime.GpsData.id:
                        return new GpsAndTime.GpsData.Response(paramsProvider).fromPacket(this);
                    default:
                        this.isEncrypted = this.attemptDecrypt(); // Helps with debugging
                        return this;
                }
            case MusicControl.id:
                switch (this.commandId) {
                    case MusicControl.MusicStatusResponse.id:
                        return new MusicControl.MusicStatusResponse(paramsProvider).fromPacket(this);
                    case MusicControl.MusicInfo.id:
                        return new MusicControl.MusicInfo.Response(paramsProvider).fromPacket(this);
                    case MusicControl.Control.id:
                        return new MusicControl.Control.Response(paramsProvider).fromPacket(this);
                    default:
                        this.isEncrypted = this.attemptDecrypt(); // Helps with debugging
                        return this;
                }
            case AccountRelated.id:
                switch(this.commandId) {
                    case AccountRelated.SendAccountToDevice.id:
                        return new AccountRelated.SendAccountToDevice.Response(paramsProvider).fromPacket(this);
                    case AccountRelated.SendExtendedAccountToDevice.id:
                        return new AccountRelated.SendExtendedAccountToDevice.Response(paramsProvider).fromPacket(this);
                    default:
                        this.isEncrypted = this.attemptDecrypt(); // Helps with debugging
                        return this;
                }
            case FileUpload.id:
                switch(this.commandId) {
                    case FileUpload.FileNextChunkParams.id:
                        return new FileUpload.FileNextChunkParams(paramsProvider).fromPacket(this);
                    case FileUpload.FileUploadConsultAck.id:
                        return new FileUpload.FileUploadConsultAck.Response(paramsProvider).fromPacket(this);
                    case FileUpload.FileHashSend.id:
                        return new FileUpload.FileHashSend.Response(paramsProvider).fromPacket(this);
                    default:
                        this.isEncrypted = this.attemptDecrypt(); // Helps with debugging
                        return this;
                }
            case Watchface.id:
                switch (this.commandId) {
                    case Watchface.WatchfaceParams.id:
                        return new Watchface.WatchfaceParams.Response(paramsProvider).fromPacket(this);
                    case Watchface.DeviceWatchInfo.id:
                        return new Watchface.DeviceWatchInfo.Response(paramsProvider).fromPacket(this);
                    case Watchface.WatchfaceNameInfo.id:
                        return new Watchface.WatchfaceNameInfo.Response(paramsProvider).fromPacket(this);
                    case Watchface.WatchfaceConfirm.id:
                        return new Watchface.WatchfaceConfirm.Response(paramsProvider).fromPacket(this);
                    default:
                        this.isEncrypted = this.attemptDecrypt(); // Helps with debugging
                        return this;
                }
            default:
                this.isEncrypted = this.attemptDecrypt(); // Helps with debugging
                return this;
        }
    }

    public HuaweiPacket parseOutgoing(byte[] data) throws ParseException {
        this.isEncrypted = false; // Will be changed if decrypt has been performed

        parseData(data);
        if (!this.complete)
            return this;

        // TODO: complete

        switch (this.serviceId) {
            case DeviceConfig.id:
                switch (this.commandId) {
                    case DeviceConfig.SupportedServices.id:
                        return new DeviceConfig.SupportedServices.OutgoingRequest(paramsProvider).fromPacket(this);
                    case DeviceConfig.DateFormat.id:
                        return new DeviceConfig.DateFormat.OutgoingRequest(paramsProvider).fromPacket(this);
                    case DeviceConfig.Bond.id:
                        return new DeviceConfig.Bond.OutgoingRequest(paramsProvider).fromPacket(this);
                    case DeviceConfig.HiChain.id:
                        return new DeviceConfig.HiChain.OutgoingRequest(paramsProvider).fromPacket(this);
                    default:
                        return this;
                }
            case Weather.id:
                switch (this.commandId) {
                    case Weather.WeatherForecastData.id:
                        return new Weather.WeatherForecastData.OutgoingRequest(paramsProvider).fromPacket(this);
                    default:
                        return this;
                }
            default:
                return this;
        }
    }

    private List<byte[]> serializeSliced(byte[] serializedTLV) {
        List<byte[]> retv = new ArrayList<>();
        int headerLength = 5; // Magic + (short)(bodyLength + 1) + 0x00 + extra slice info
        int bodyHeaderLength = 2; // sID + cID
        int footerLength = 2; //CRC16
        int maxBodySize = paramsProvider.getSliceSize() - headerLength - footerLength;
        int packetCount = (int) Math.ceil(((double) serializedTLV.length + (double) bodyHeaderLength) / (double) maxBodySize);

        if (packetCount == 1)
            return serializeUnsliced(serializedTLV);

        ByteBuffer buffer = ByteBuffer.wrap(serializedTLV);
        byte slice = 0x01;
        byte flag = 0x00;
        for (int i = 0; i < packetCount; i++) {
            short packetSize = (short) Math.min(paramsProvider.getSliceSize(), buffer.remaining() + headerLength + footerLength);

            ByteBuffer packet = ByteBuffer.allocate(packetSize);

            short contentSize = (short) (packetSize - headerLength - footerLength);
            int start = packet.position();

            packet.put((byte) 0x5a);                                // Magic byte
            packet.putShort((short) (packetSize - headerLength));   // Length

            if (i == packetCount - 1)
                slice = 0x03;

            packet.put(slice);                                      // Slice
            packet.put(flag);                                       // Flag
            flag += 1;

            if (slice == 0x01) {
                packet.put(this.serviceId);                         // Service ID
                packet.put(this.commandId);                         // Command ID
                slice = 0x02;
                contentSize -= 2; // To prevent taking too much data
            }

            byte[] packetContent = new byte[contentSize];
            buffer.get(packetContent);
            packet.put(packetContent);                              // Packet data

            int length = packet.position() - start;
            if (length != packetSize - footerLength) {
                // TODO: exception?
                LOG.error(String.format(GBApplication.getLanguage(), "Packet lengths don't match! %d != %d", length, packetSize + headerLength));
            }

            byte[] complete = new byte[length];
            packet.position(start);
            packet.get(complete, 0, length);
            int crc16 = CheckSums.getCRC16(complete, 0x0000);

            packet.putShort((short) crc16);                         // CRC16

            retv.add(packet.array());
        }
        return retv;
    }

    private List<byte[]> serializeUnsliced(byte[] serializedTLV) {
        List<byte[]> retv = new ArrayList<>();
        int headerLength = 4; // Magic + (short)(bodyLength + 1) + 0x00
        int bodyHeaderLength = 2; // sID + cID
        int footerLength = 2; //CRC16
        int bodyLength = bodyHeaderLength + serializedTLV.length;
        ByteBuffer buffer = ByteBuffer.allocate(headerLength + bodyLength);
        buffer.put((byte) 0x5A);
        buffer.putShort((short)(bodyLength + 1));
        buffer.put((byte) 0x00);
        buffer.put(this.serviceId);
        buffer.put(this.commandId);
        buffer.put(serializedTLV);
        int crc16 = CheckSums.getCRC16(buffer.array(), 0x0000);
        ByteBuffer finalBuffer = ByteBuffer.allocate(buffer.capacity() + footerLength);
        finalBuffer.put(buffer.array());
        finalBuffer.putShort((short)crc16);
        retv.add(finalBuffer.array());
        return retv;
    }

    public List<byte[]> serializeFileChunk(byte[] fileChunk, int uploadPosition, short unitSize, byte fileId) {
        List<byte[]> retv = new ArrayList<>();
        int headerLength = 5; // Magic + (short)(bodyLength + 1) + 0x00
        int sliceHeaderLenght =7;

        int footerLength = 2; //CRC16

        int packetCount = (int) Math.ceil(((double) fileChunk.length ) / (double) unitSize);

        ByteBuffer buffer = ByteBuffer.wrap(fileChunk);

        int sliceStart = uploadPosition;

        for (int i = 0; i < packetCount; i++) {

            short contentSize = (short) Math.min(unitSize, buffer.remaining());
            short packetSize = (short)(contentSize + headerLength + sliceHeaderLenght + footerLength);
            ByteBuffer packet = ByteBuffer.allocate(packetSize);

            int start = packet.position();
            packet.put((byte) 0x5a);                                // Magic byte
            packet.putShort((short) (packetSize - headerLength));   // Length

            packet.put((byte) 0x00);
            packet.put(this.serviceId);
            packet.put(this.commandId);

            packet.put(fileId);                                      // Slice
            packet.put((byte)i);                                       // Flag
            packet.putInt(sliceStart);

            byte[] packetContent = new byte[contentSize];
            buffer.get(packetContent);
            packet.put(packetContent);                              // Packet databyte[] packetContent = new byte[contentSize];

            int length = packet.position() - start;
            if (length != packetSize - footerLength) {
                // TODO: exception?
                LOG.error(String.format(GBApplication.getLanguage(), "Packet lengths don't match! %d != %d", length, packetSize + headerLength));
            }

            byte[] complete = new byte[length];
            packet.position(start);
            packet.get(complete, 0, length);
            int crc16 = CheckSums.getCRC16(complete, 0x0000);

            packet.putShort((short) crc16);                         // CRC16

            sliceStart += contentSize;

            retv.add(packet.array());
        }
        return retv;
    }

    public List<byte[]> serialize() throws CryptoException {
        // TODO: necessary for this to work:
        //       - serviceId
        //       - commandId
        //       - tlv
        // TODO: maybe use the complete flag to know if it can be serialized?

        HuaweiTLV serializableTlv;
        if (this.isEncrypted && this.paramsProvider.areTransactionsCrypted()) {
            try {
                serializableTlv = this.tlv.encrypt(paramsProvider);
            } catch (HuaweiCrypto.CryptoException e) {
                throw new CryptoException("Encrypt exception", e);
            }
        } else {
            serializableTlv = this.tlv;
        }

        byte[] serializedTLV = serializableTlv.serialize();
        List<byte[]> retv;
        if (isSliced) {
            retv = serializeSliced(serializedTLV);
        } else {
            retv = serializeUnsliced(serializedTLV);
        }
        return retv;
    }

    public HuaweiTLV getTlv() {
        return this.tlv;
    }

    public void setTlv(HuaweiTLV tlv) {
        this.tlv = tlv;
    }

    public void setEncryption(boolean b) {
        this.isEncrypted = b;
    }

    public void setSliced(boolean b) {
        this.isSliced = b;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HuaweiPacket that = (HuaweiPacket) o;

        if (serviceId != that.serviceId) return false;
        if (commandId != that.commandId) return false;
        if (complete != that.complete) return false;
        if (isEncrypted != that.isEncrypted) return false;
        return Objects.equals(tlv, that.tlv);
    }

    @Override
    public String toString() {
        return "HuaweiPacket{" +
                "paramsProvider=" + paramsProvider +
                ", serviceId=" + serviceId +
                ", commandId=" + commandId +
                ", tlv=" + tlv +
                ", partialPacket=" + Arrays.toString(partialPacket) +
                ", payload=" + Arrays.toString(payload) +
                ", complete=" + complete +
                ", isEncrypted=" + isEncrypted +
                ", isSliced=" + isSliced +
                '}';
    }
}
