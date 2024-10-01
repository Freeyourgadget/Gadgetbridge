package nodomain.freeyourgadget.gadgetbridge.service.devices.casio;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import org.slf4j.Logger;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventFindPhone;
import nodomain.freeyourgadget.gadgetbridge.devices.casio.CasioConstants;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;

public abstract class BasicCasio2C2DSupport extends Casio2C2DSupport {
    public BasicCasio2C2DSupport(Logger logger) {
        super(logger);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        // remove this workaround once Gadgetbridge does discovery on initial pairing
        if (getCharacteristic(CasioConstants.CASIO_READ_REQUEST_FOR_ALL_FEATURES_CHARACTERISTIC_UUID) == null ||
                getCharacteristic(CasioConstants.CASIO_ALL_FEATURES_CHARACTERISTIC_UUID) == null) {
            LOG.info("Reconnecting to discover characteristics");
            disconnect();
            connect();
            return builder;
        }

        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));

        super.initializeDevice(builder);

        // which button was pressed?
        requestFeature(builder, new FeatureRequest(FEATURE_BLE_FEATURES), data -> {
            if (data.length > 8 && data[8] == CasioConstants.CONNECT_FIND) {
                setInitialized();
            } else {
                requestWorldClocks();
            }
        });

        return builder;
    }

    protected void requestWorldClocks() {
        TransactionBuilder builder = createTransactionBuilder("requestWorldClocks");
        HashSet<FeatureRequest> requests = new HashSet();

        for (byte i = 0; i < 6; i++) {
            requests.addAll(CasioTimeZone.requests(i));
        }

        requestFeatures(builder, requests, responses -> {
            TransactionBuilder clockBuilder = createTransactionBuilder("setClocks");
            setClocks(clockBuilder, responses);
            clockBuilder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));
            clockBuilder.queue(getQueue());
        });
        builder.queue(getQueue());
    }

    private void setClocks(TransactionBuilder builder, Map<FeatureRequest, byte[]> responses) {
        ZoneId tz = ZoneId.systemDefault();
        Instant now = Instant.now().plusSeconds(2);
        CasioTimeZone[] timezones = {
                CasioTimeZone.fromZoneId(tz, now, tz.getDisplayName(TextStyle.SHORT, Locale.getDefault())),
                CasioTimeZone.fromWatchResponses(responses, 1),
                CasioTimeZone.fromWatchResponses(responses, 2),
                CasioTimeZone.fromWatchResponses(responses, 3),
                CasioTimeZone.fromWatchResponses(responses, 4),
                CasioTimeZone.fromWatchResponses(responses, 5),
        };
        for (int i = 5; i >= 0; i--) {
            if (i%2 == 0)
                writeAllFeatures(builder, CasioTimeZone.dstWatchStateBytes(i, timezones[i], i+1, timezones[i+1]));
            writeAllFeatures(builder, timezones[i].dstSettingBytes(i));
            writeAllFeatures(builder, timezones[i].worldCityBytes(i));
        }
        writeCurrentTime(builder, ZonedDateTime.ofInstant(now, tz));
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        UUID characteristicUUID = characteristic.getUuid();
        byte[] data = characteristic.getValue();
        if (data.length == 0)
            return true;

        if (characteristicUUID.equals(CasioConstants.CASIO_ALL_FEATURES_CHARACTERISTIC_UUID)) {
            if(data[0] == FEATURE_ALERT_LEVEL) {
                GBDeviceEventFindPhone event = new GBDeviceEventFindPhone();
                if(data[1] == 0x02) {
                    event.event = GBDeviceEventFindPhone.Event.START_VIBRATE;
                } else {
                    event.event = GBDeviceEventFindPhone.Event.STOP;
                }
                evaluateGBDeviceEvent(event);
                return true;
            }
        }
        return super.onCharacteristicChanged(gatt, characteristic);
    }
}
