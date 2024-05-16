package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.communicator.v2;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.communicator.CobsCoDec;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.communicator.ICommunicator;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class CommunicatorV2 implements ICommunicator {
    public static final UUID UUID_SERVICE_GARMIN_ML_GFDI = UUID.fromString("6A4E2800-667B-11E3-949A-0800200C9A66");
    public static final UUID UUID_CHARACTERISTIC_GARMIN_ML_GFDI_SEND = UUID.fromString("6a4e2822-667b-11e3-949a-0800200c9a66");
    public static final UUID UUID_CHARACTERISTIC_GARMIN_ML_GFDI_RECEIVE = UUID.fromString("6a4e2812-667b-11e3-949a-0800200c9a66");

    public int maxWriteSize = 20;
    private static final Logger LOG = LoggerFactory.getLogger(CommunicatorV2.class);
    public final CobsCoDec cobsCoDec;
    private final GarminSupport mSupport;
    private final long gadgetBridgeClientID = 2L;
    private int gfdiHandle = 0;

    public CommunicatorV2(final GarminSupport garminSupport) {
        this.mSupport = garminSupport;
        this.cobsCoDec = new CobsCoDec();
    }

    @Override
    public void onMtuChanged(final int mtu) {
        maxWriteSize = mtu - 3;
    }

    @Override
    public void initializeDevice(final TransactionBuilder builder) {

        builder.notify(this.mSupport.getCharacteristic(UUID_CHARACTERISTIC_GARMIN_ML_GFDI_RECEIVE), true);
        builder.write(this.mSupport.getCharacteristic(UUID_CHARACTERISTIC_GARMIN_ML_GFDI_SEND), closeAllServices());
    }

    @Override
    public void sendMessage(final byte[] message) {
        if (null == message)
            return;
        if (0 == gfdiHandle) {
            LOG.error("CANNOT SENT GFDI MESSAGE, HANDLE NOT YET SET. MESSAGE {}", message);
            return;
        }
        final byte[] payload = cobsCoDec.encode(message);
//        LOG.debug("SENDING MESSAGE: {} - COBS ENCODED: {}", GB.hexdump(message), GB.hexdump(payload));
        final TransactionBuilder builder = new TransactionBuilder("sendMessage()");
        int remainingBytes = payload.length;
        if (remainingBytes > maxWriteSize - 1) {
            int position = 0;
            while (remainingBytes > 0) {
                final byte[] fragment = Arrays.copyOfRange(payload, position, position + Math.min(remainingBytes, maxWriteSize - 1));
                builder.write(this.mSupport.getCharacteristic(UUID_CHARACTERISTIC_GARMIN_ML_GFDI_SEND), ArrayUtils.addAll(new byte[]{(byte) gfdiHandle}, fragment));
                position += fragment.length;
                remainingBytes -= fragment.length;
            }
        } else {
            builder.write(this.mSupport.getCharacteristic(UUID_CHARACTERISTIC_GARMIN_ML_GFDI_SEND), ArrayUtils.addAll(new byte[]{(byte) gfdiHandle}, payload));
        }
        builder.queue(this.mSupport.getQueue());
    }

    @Override
    public boolean onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
        ByteBuffer message = ByteBuffer.wrap(characteristic.getValue()).order(ByteOrder.LITTLE_ENDIAN);
//        LOG.debug("RECEIVED: {}", GB.hexdump(message.array()));
        final byte handle = message.get();
        if (0x00 == handle) { //handle management message

            final byte type = message.get();
            final long incomingClientID = message.getLong();

            if (incomingClientID != this.gadgetBridgeClientID) {
                LOG.debug("Ignoring incoming message, client ID is not ours. Message: {}", GB.hexdump(message.array()));
            }
            RequestType requestType = RequestType.fromCode(type);
            if (null == requestType) {
                LOG.error("Unknown request type. Message: {}", message.array());
                return true;
            }
            switch (requestType) {
                case REGISTER_ML_REQ: //register service request
                case CLOSE_HANDLE_REQ: //close handle request
                case CLOSE_ALL_REQ: //close all handles request
                case UNK_REQ: //unknown request
                    LOG.warn("Received handle request, expecting responses. Message: {}", message.array());
                case REGISTER_ML_RESP: //register service response
                    LOG.debug("Received register response. Message: {}", message.array());
                    final short registeredService = message.getShort();
                    final byte status = message.get();
                    if (0 == status && 1 == registeredService) { //success
                        this.gfdiHandle = message.get();
                    }
                    break;
                case CLOSE_HANDLE_RESP: //close handle response
                    LOG.debug("Received close handle response. Message: {}", message.array());
                    break;
                case CLOSE_ALL_RESP: //close all handles response
                    LOG.debug("Received close all handles response. Message: {}", message.array());
                    new TransactionBuilder("open GFDI")
                            .write(this.mSupport.getCharacteristic(UUID_CHARACTERISTIC_GARMIN_ML_GFDI_SEND), registerGFDI())
                            .queue(this.mSupport.getQueue());
                    break;
                case UNK_RESP: //unknown response
                    LOG.debug("Received unknown. Message: {}", message.array());
                    break;
            }

            return true;
        } else if (this.gfdiHandle == handle) {

            byte[] partial = new byte[message.remaining()];
            message.get(partial);
            this.cobsCoDec.receivedBytes(partial);

            this.mSupport.onMessage(this.cobsCoDec.retrieveMessage());

            return true;
        }
        return false;
    }

    protected byte[] closeAllServices() {
        ByteBuffer toSend = ByteBuffer.allocate(13);
        toSend.order(ByteOrder.BIG_ENDIAN);
        toSend.putShort((short) RequestType.CLOSE_ALL_REQ.ordinal()); //close all services
        toSend.order(ByteOrder.LITTLE_ENDIAN);
        toSend.putLong(this.gadgetBridgeClientID);
        toSend.putShort((short) 0);
        return toSend.array();
    }

    protected byte[] registerGFDI() {
        ByteBuffer toSend = ByteBuffer.allocate(13);
        toSend.order(ByteOrder.BIG_ENDIAN);
        toSend.putShort((short) RequestType.REGISTER_ML_REQ.ordinal()); //register service request
        toSend.order(ByteOrder.LITTLE_ENDIAN);
        toSend.putLong(this.gadgetBridgeClientID);
        toSend.putShort((short) 1); //service GFDI
        return toSend.array();
    }

    enum RequestType {
        REGISTER_ML_REQ,
        REGISTER_ML_RESP,
        CLOSE_HANDLE_REQ,
        CLOSE_HANDLE_RESP,
        UNK_HANDLE,
        CLOSE_ALL_REQ,
        CLOSE_ALL_RESP,
        UNK_REQ,
        UNK_RESP;

        public static RequestType fromCode(final int code) {
            for (final RequestType requestType : RequestType.values()) {
                if (requestType.ordinal() == code) {
                    return requestType;
                }
            }

            return null;
        }
    }
}
