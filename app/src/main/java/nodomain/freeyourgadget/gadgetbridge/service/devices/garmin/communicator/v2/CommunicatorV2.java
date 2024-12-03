package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.communicator.v2;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminActivitySampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.communicator.CobsCoDec;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.communicator.ICommunicator;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class CommunicatorV2 implements ICommunicator {
    private static final Logger LOG = LoggerFactory.getLogger(CommunicatorV2.class);

    public static final String BASE_UUID = "6A4E%04X-667B-11E3-949A-0800200C9A66";
    public static final UUID UUID_SERVICE_GARMIN_ML_GFDI = UUID.fromString(String.format(BASE_UUID, 0x2800));

    private static final long GADGETBRIDGE_CLIENT_ID = 2L;

    private BluetoothGattCharacteristic characteristicSend;
    private BluetoothGattCharacteristic characteristicReceive;

    private final GarminSupport mSupport;

    private int gfdiHandle = 0;
    public int maxWriteSize = 20;
    public final CobsCoDec cobsCoDec;

    private int realtimeHrHandle = 0;
    private boolean realtimeHrOneShot = false;

    private int realtimeStepsHandle = 0;
    private int previousSteps = -1;

    private int realtimeAccelHandle = 0;

    private int realtimeSpo2Handle = 0;
    private int realtimeRespirationHandle = 0;
    private int realtimeHrvHandle = 0;

    public CommunicatorV2(final GarminSupport garminSupport) {
        this.mSupport = garminSupport;
        this.cobsCoDec = new CobsCoDec();
    }

    @Override
    public void onMtuChanged(final int mtu) {
        maxWriteSize = mtu - 3;
    }

    @Override
    public boolean initializeDevice(final TransactionBuilder builder) {
        // Iterate through the known ML characteristics until we find a known pair
        // send characteristic = read characteristic + 0x10 (eg. 2810 / 2820)
        for (int i = 0x2810; i <= 0x2814; i++) {
            characteristicReceive = mSupport.getCharacteristic(UUID.fromString(String.format(BASE_UUID, i)));
            characteristicSend = mSupport.getCharacteristic(UUID.fromString(String.format(BASE_UUID, i + 0x10)));

            if (characteristicSend != null && characteristicReceive != null) {
                LOG.debug("Using characteristics receive/send = {}/{}", characteristicReceive.getUuid(), characteristicSend.getUuid());

                builder.notify(characteristicReceive, true);
                builder.write(characteristicSend, closeAllServices());

                return true;
            }
        }

        LOG.warn("Failed to find any known ML characteristics");

        return false;
    }

    @Override
    public void sendMessage(final String taskName, final byte[] message) {
        if (null == message)
            return;
        if (0 == gfdiHandle) {
            LOG.error("CANNOT SENT GFDI MESSAGE, HANDLE NOT YET SET. MESSAGE {}", message);
            return;
        }
        final byte[] payload = cobsCoDec.encode(message);
//        LOG.debug("SENDING MESSAGE: {} - COBS ENCODED: {}", GB.hexdump(message), GB.hexdump(payload));
        final TransactionBuilder builder = new TransactionBuilder(taskName);
        int remainingBytes = payload.length;
        if (remainingBytes > maxWriteSize - 1) {
            int position = 0;
            while (remainingBytes > 0) {
                final byte[] fragment = Arrays.copyOfRange(payload, position, position + Math.min(remainingBytes, maxWriteSize - 1));
                builder.write(characteristicSend, ArrayUtils.addAll(new byte[]{(byte) gfdiHandle}, fragment));
                position += fragment.length;
                remainingBytes -= fragment.length;
            }
        } else {
            builder.write(characteristicSend, ArrayUtils.addAll(new byte[]{(byte) gfdiHandle}, payload));
        }
        builder.queue(this.mSupport.getQueue());
    }

    @Override
    public boolean onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
        if (!characteristic.getUuid().equals(characteristicReceive.getUuid())) {
            // Not ML
            return false;
        }

        final ByteBuffer message = ByteBuffer.wrap(characteristic.getValue()).order(ByteOrder.LITTLE_ENDIAN);
        final byte handle = message.get();

        if (0x00 == handle) {
            processHandleManagement(message);
        } else if (this.gfdiHandle == handle) {
            processGfdi(message);
        } else if (this.realtimeHrHandle == handle) {
            processRealtimeHeartRate(message);
        } else if (this.realtimeStepsHandle == handle) {
            processRealtimeSteps(message);
        } else if (this.realtimeAccelHandle == handle) {
            processRealtimeAccelerometer(message);
        } else if (this.realtimeSpo2Handle == handle) {
            processRealtimeSpo2(message);
        } else if (this.realtimeRespirationHandle == handle) {
            processRealtimeRespiration(message);
        } else if (this.realtimeHrvHandle == handle) {
            processRealtimeHrv(message);
        } else {
            LOG.warn("Got message for unknown handle {}: {}", handle, GB.hexdump(characteristic.getValue()));
        }

        return true;
    }

    @Override
    public void onHeartRateTest() {
        realtimeHrOneShot = true;
        if (realtimeHrHandle == 0) {
            new TransactionBuilder("heart rate test")
                    .write(characteristicSend, registerService(Service.REALTIME_HR, false))
                    .queue(this.mSupport.getQueue());
        }
    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(final boolean enable) {
        toggleService(Service.REALTIME_HR, realtimeHrHandle, enable);
    }

    @Override
    public void onEnableRealtimeSteps(final boolean enable) {
        if (toggleService(Service.REALTIME_STEPS, realtimeStepsHandle, enable)) {
            previousSteps = -1;
        }
    }

    private boolean toggleService(final Service service, final int currentHandle, final boolean enable) {
        if (enable && currentHandle == 0) {
            new TransactionBuilder(service + " = true")
                    .write(characteristicSend, registerService(service, false))
                    .queue(this.mSupport.getQueue());
            return true;
        } else if (!enable && currentHandle != 0) {
            new TransactionBuilder(service + " = false")
                    .write(characteristicSend, closeService(service, currentHandle))
                    .queue(this.mSupport.getQueue());
            return true;
        }

        return false;
    }

    private void processHandleManagement(final ByteBuffer message) {
        final byte type = message.get();
        final long incomingClientID = message.getLong();

        if (incomingClientID != GADGETBRIDGE_CLIENT_ID) {
            LOG.warn("Ignoring incoming message, client ID {} is not ours. Message: {}", incomingClientID, GB.hexdump(message.array()));
            return;
        }

        final RequestType requestType = RequestType.fromCode(type);
        if (null == requestType) {
            LOG.error("Unknown request type {}. Message: {}", type, message.array());
            return;
        }

        switch (requestType) {
            case REGISTER_ML_REQ:
            case CLOSE_HANDLE_REQ:
            case CLOSE_ALL_REQ:
            case UNK_REQ:
                LOG.warn("Received handle request, expecting responses. Message: {}", message.array());
                return;
            case REGISTER_ML_RESP: {
                final short registeredServiceCode = message.getShort();
                final Service registeredService = Service.fromCode(registeredServiceCode);
                final byte status = message.get();
                if (registeredService == null) {
                    LOG.error("Got register response status={} for unknown service {}", status, registeredServiceCode);
                    return;
                }
                if (status != 0) {
                    LOG.warn("Failed to register {}, status={}", registeredService, status);
                    return;
                }
                final int handle = message.get();
                final int reliable = message.get();
                LOG.debug("Got register response for {}, handle={}, reliable={}", registeredService, handle, reliable);

                switch (registeredService) {
                    case GFDI:
                        this.gfdiHandle = handle;
                        break;
                    case REALTIME_HR:
                        this.realtimeHrHandle = handle;
                        break;
                    case REALTIME_STEPS:
                        this.realtimeStepsHandle = handle;
                        break;
                    case REALTIME_ACCELEROMETER:
                        this.realtimeAccelHandle = handle;
                        new TransactionBuilder("start realtime accel")
                                .write(characteristicSend, new byte[]{(byte) handle, 0x01})
                                .queue(this.mSupport.getQueue());
                        break;
                    case REALTIME_SPO2:
                        this.realtimeSpo2Handle = handle;
                        break;
                    case REALTIME_RESPIRATION:
                        this.realtimeRespirationHandle = handle;
                        break;
                    case REALTIME_HRV:
                        this.realtimeHrvHandle = handle;
                        break;
                }
                break;
            }
            case CLOSE_HANDLE_RESP: {
                final short serviceCode = message.getShort();
                final Service service = Service.fromCode(serviceCode);
                final int handle = message.get();
                final byte status = message.get();
                LOG.debug("Received close handle response: service={}, handle={}, status={}", service, handle, status);
                if (service != null) {
                    switch (service) {
                        case GFDI:
                            this.gfdiHandle = 0;
                            break;
                        case REALTIME_HR:
                            this.realtimeHrHandle = 0;
                            break;
                        case REALTIME_STEPS:
                            this.realtimeStepsHandle = 0;
                            break;
                        case REALTIME_ACCELEROMETER:
                            this.realtimeAccelHandle = 0;
                            break;
                        case REALTIME_SPO2:
                            this.realtimeSpo2Handle = 0;
                            break;
                        case REALTIME_RESPIRATION:
                            this.realtimeRespirationHandle = 0;
                            break;
                        case REALTIME_HRV:
                            this.realtimeHrvHandle = 0;
                            break;
                    }
                }
                break;
            }
            case CLOSE_ALL_RESP:
                LOG.debug("Received close all handles response. Message: {}", message.array());
                this.gfdiHandle = 0;
                this.realtimeHrHandle = 0;
                this.realtimeStepsHandle = 0;
                this.realtimeAccelHandle = 0;
                this.realtimeSpo2Handle = 0;
                this.realtimeRespirationHandle = 0;
                this.realtimeHrvHandle = 0;
                new TransactionBuilder("open GFDI")
                        .write(characteristicSend, registerService(Service.GFDI, false))
                        .queue(this.mSupport.getQueue());
                break;
            case UNK_RESP:
                LOG.debug("Received unknown. Message: {}", message.array());
                break;
        }
    }

    private void processGfdi(final ByteBuffer message) {
        final byte[] partial = new byte[message.remaining()];
        message.get(partial);
        this.cobsCoDec.receivedBytes(partial);

        this.mSupport.onMessage(this.cobsCoDec.retrieveMessage());
    }

    private void processRealtimeHeartRate(final ByteBuffer buf) {
        final byte type = buf.get(); // 0/2/3? 3 == realtime?
        final int hr = buf.get();
        final int resting = buf.get();
        // ff ff after
        LOG.debug("Got realtime HR: type={} hr={} resting={}", type, hr, resting);

        if (hr > 0) {
            broadcastRealtimeActivity(hr, -1);

            if (realtimeHrOneShot && realtimeHrHandle != 0) {
                onEnableRealtimeHeartRateMeasurement(false);
            }
        }
    }

    private void processRealtimeSteps(final ByteBuffer buf) {
        final int steps = buf.getInt();
        final int goal = buf.getInt();
        LOG.debug("Got realtime steps: steps={} goal={}", steps, goal);

        if (previousSteps == -1) {
            previousSteps = steps;
        }

        broadcastRealtimeActivity(-1, steps - previousSteps);

        previousSteps = steps;
    }

    private void processRealtimeAccelerometer(final ByteBuffer message) {
        final byte[] partial = new byte[message.remaining()];
        message.get(partial);

        LOG.debug("Got realtime accel: {}", GB.hexdump(partial));
    }

    private void processRealtimeSpo2(final ByteBuffer message) {
        final int spo2 = message.get(); // -1 when unknown, and the ts is not valid in that case
        final int garminTs = message.getInt();

        LOG.debug("Got realtime SpO2 at {}: {}", new Date(GarminTimeUtils.garminTimestampToJavaMillis(garminTs)), spo2);
    }

    private void processRealtimeRespiration(final ByteBuffer message) {
        final int breathsPerMinute = message.get(); // can be negative if unknown, usually -2

        LOG.debug("Got realtime respiration: {}", breathsPerMinute);
    }

    private void processRealtimeHrv(final ByteBuffer message) {
        final short rr = message.getShort();
        final int unk = message.getInt();

        LOG.debug("Got realtime HRV: rr={}, unk={}", rr, unk);
    }

    private byte[] closeAllServices() {
        final ByteBuffer toSend = ByteBuffer.allocate(13).order(ByteOrder.LITTLE_ENDIAN);
        toSend.put((byte) 0); // handle
        toSend.put((byte) RequestType.CLOSE_ALL_REQ.ordinal());
        toSend.putLong(GADGETBRIDGE_CLIENT_ID);
        toSend.putShort((short) 0);
        return toSend.array();
    }

    private byte[] registerService(final Service service, final boolean reliable) {
        final ByteBuffer toSend = ByteBuffer.allocate(13).order(ByteOrder.LITTLE_ENDIAN);
        toSend.put((byte) 0);
        toSend.put((byte) RequestType.REGISTER_ML_REQ.ordinal());
        toSend.putLong(GADGETBRIDGE_CLIENT_ID);
        toSend.putShort(service.getCode());
        toSend.put((byte) (reliable ? 2 : 0));
        return toSend.array();
    }

    private byte[] closeService(final Service service, final int handle) {
        final ByteBuffer toSend = ByteBuffer.allocate(13).order(ByteOrder.LITTLE_ENDIAN);
        toSend.put((byte) 0);
        toSend.put((byte) RequestType.CLOSE_HANDLE_REQ.ordinal());
        toSend.putLong(GADGETBRIDGE_CLIENT_ID);
        toSend.putShort(service.getCode());
        toSend.put((byte) handle);
        return toSend.array();
    }

    private void broadcastRealtimeActivity(final int hr, final int steps) {
        final GarminActivitySample sample;
        try (final DBHandler dbHandler = GBApplication.acquireDB()) {
            final DaoSession session = dbHandler.getDaoSession();

            final GBDevice gbDevice = mSupport.getDevice();
            final Device device = DBHelper.getDevice(gbDevice, session);
            final User user = DBHelper.getUser(session);
            final GarminActivitySampleProvider provider = new GarminActivitySampleProvider(gbDevice, session);
            sample = provider.createActivitySample();

            sample.setDeviceId(device.getId());
            sample.setUserId(user.getId());
            sample.setTimestamp((int) (System.currentTimeMillis() / 1000));
            sample.setHeartRate(hr);
            sample.setSteps(steps);
            sample.setRawKind(ActivityKind.UNKNOWN.getCode());
            sample.setRawIntensity(ActivitySample.NOT_MEASURED);
            sample.setRawKind(ActivityKind.UNKNOWN.getCode());
        } catch (final Exception e) {
            LOG.error("Error creating activity sample", e);
            return;
        }

        final Intent intent = new Intent(DeviceService.ACTION_REALTIME_SAMPLES)
                .putExtra(GBDevice.EXTRA_DEVICE, mSupport.getDevice())
                .putExtra(DeviceService.EXTRA_REALTIME_SAMPLE, sample);
        LocalBroadcastManager.getInstance(mSupport.getContext()).sendBroadcast(intent);
    }

    private enum RequestType {
        REGISTER_ML_REQ,
        REGISTER_ML_RESP,
        CLOSE_HANDLE_REQ,
        CLOSE_HANDLE_RESP,
        UNK_HANDLE,
        CLOSE_ALL_REQ,
        CLOSE_ALL_RESP,
        UNK_REQ,
        UNK_RESP;

        @Nullable
        public static RequestType fromCode(final int code) {
            for (final RequestType requestType : RequestType.values()) {
                if (requestType.ordinal() == code) {
                    return requestType;
                }
            }

            return null;
        }
    }

    private enum Service {
        GFDI(1),
        REGISTRATION(4),
        REALTIME_HR(6),
        REALTIME_STEPS(7),
        REALTIME_CALORIES(8),
        REALTIME_INTENSITY(10),
        REALTIME_HRV(12),
        REALTIME_STRESS(13),
        REALTIME_ACCELEROMETER(16),
        REALTIME_SPO2(19),
        REALTIME_BODY_BATTERY(20),
        REALTIME_RESPIRATION(21),
        ;

        private final short code;

        Service(final int code) {
            this.code = (short) code;
        }

        public short getCode() {
            return code;
        }

        @Nullable
        public static Service fromCode(final int code) {
            for (final Service service : Service.values()) {
                if (service.code == code) {
                    return service;
                }
            }

            return null;
        }
    }
}
