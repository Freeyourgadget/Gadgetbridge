/*  Copyright (C) 2023-2024 Johannes Krude

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.casio;

import java.time.ZonedDateTime;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import java.util.UUID;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.Logging;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.devices.casio.CasioConstants;


// this class is for those Casio watches which request reads on the 2C characteristic and write on the 2D characteristic

public abstract class Casio2C2DSupport extends CasioSupport {

    public static final byte FEATURE_CURRENT_TIME = 0x09;
    public static final byte FEATURE_ALERT_LEVEL = 0x0a;
    public static final byte FEATURE_BLE_FEATURES = 0x10;
    public static final byte FEATURE_SETTING_FOR_BLE = 0x11;
    public static final byte FEATURE_SETTING_FOR_BASIC = 0x13;
    public static final byte FEATURE_SETTING_FOR_ALM = 0x15;
    public static final byte FEATURE_SETTING_FOR_ALM2 = 0x16;
    public static final byte FEATURE_VERSION_INFORMATION = 0x20;
    public static final byte FEATURE_APP_INFORMATION = 0x22;
    public static final byte FEATURE_WATCH_NAME = 0x23;
    public static final byte FEATURE_MODULE_ID = 0x26;
    public static final byte FEATURE_WATCH_CONDITION = 0x28;
    public static final byte FEATURE_DST_WATCH_STATE = 0x1d;
    public static final byte FEATURE_DST_SETTING = 0x1e;
    public static final byte FEATURE_WORLD_CITY = 0x1f;
    public static final byte FEATURE_CURRENT_TIME_MANAGER = 0x39;
    public static final byte FEATURE_CONNECTION_PARAMETER_MANAGER = 0x3a;
    public static final byte FEATURE_ADVERTISE_PARAMETER_MANAGER = 0x3b;
    public static final byte FEATURE_SETTING_FOR_TARGET_VALUE = 0x43;
    public static final byte FEATURE_SETTING_FOR_USER_PROFILE = 0x45;
    public static final byte FEATURE_SERVICE_DISCOVERY_MANAGER = 0x47;

    private static Logger LOG;
    LinkedList<RequestWithHandler> requests = new LinkedList<>();

    public Casio2C2DSupport(Logger logger) {
        super(logger);
        LOG = logger;
    }

    @Override
    public boolean connect() {
        requests.clear();
        return super.connect();
    }

    public void writeAllFeatures(TransactionBuilder builder, byte[] arr) {
        if (!requests.isEmpty()) {
            LOG.warn("writing while waiting for a response may lead to incorrect received responses");
        }
        builder.write(getCharacteristic(CasioConstants.CASIO_ALL_FEATURES_CHARACTERISTIC_UUID), arr);
    }

    public void writeAllFeaturesRequest(TransactionBuilder builder, byte[] arr) {
        builder.write(getCharacteristic(CasioConstants.CASIO_READ_REQUEST_FOR_ALL_FEATURES_CHARACTERISTIC_UUID), arr);
    }

    public interface ResponseHandler {
        void handle(byte[] response);
    }

    public interface ResponsesHandler {
        void handle(Map<FeatureRequest, byte[]> responses);
    }

    public static class FeatureRequest {
        byte data[];

        public FeatureRequest(byte arg0) {
            data = new byte[] {arg0};
        }

        public FeatureRequest(byte arg0, byte arg1) {
            data = new byte[] {arg0, arg1};
        }

        public byte[] getData() {
            return data.clone();
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(data);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof FeatureRequest))
                return false;

            FeatureRequest fr = (FeatureRequest) o;
            return Arrays.equals(data, fr.data);
        }

        public boolean matches(byte[] response) {
            if (response.length > 2 && response[0] == 0xFF && response[1] == 0x81) {
                if (data.length < response.length - 2)
                    return false;
                for (int i = 2; i < response.length; i++) {
                    if (response[i] != data[i-2])
                        return false;
                }
                return true;
            } else {
                if (response.length < data.length)
                    return false;
                for (int i = 0; i < data.length; i++) {
                    if (response[i] != data[i])
                        return false;
                }
                return true;
            }
        }
    }

    private static class RequestWithHandler {
        public FeatureRequest request;
        public ResponseHandler handler;

        public RequestWithHandler(FeatureRequest request, ResponseHandler handler) {
            this.request = request;
            this.handler = handler;
        }
    }

    public void requestFeature(TransactionBuilder builder, FeatureRequest request, ResponseHandler handler) {
        builder.notify(getCharacteristic(CasioConstants.CASIO_ALL_FEATURES_CHARACTERISTIC_UUID), true);
        writeAllFeaturesRequest(builder, request.getData());
        builder.run((gatt) -> requests.add(new RequestWithHandler(request, handler)));
    }

    public void requestFeatures(TransactionBuilder builder, Set<FeatureRequest> requests, ResponsesHandler handler) {
        HashMap<FeatureRequest, byte[]> responses = new HashMap();

        HashSet<FeatureRequest> missing = new HashSet();
        for (FeatureRequest request: requests) {
            missing.add(request);
        }

        for (FeatureRequest request: requests) {
            requestFeature(builder, request, data -> {
                responses.put(request, data);
                missing.remove(request);
                if (missing.isEmpty()) {
                    handler.handle(responses);
                }
            });
        }
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        UUID characteristicUUID = characteristic.getUuid();

        if (characteristicUUID.equals(CasioConstants.CASIO_ALL_FEATURES_CHARACTERISTIC_UUID)) {
            byte[] response = characteristic.getValue();
            Iterator<RequestWithHandler> it = requests.iterator();
            while (it.hasNext()) {
                RequestWithHandler rh = it.next();
                if (rh.request.matches(response)) {
                    it.remove();
                    rh.handler.handle(response);
                    return true;
                }
            }
            LOG.warn("unhandled response: " + Logging.formatBytes(response));
        }

        return super.onCharacteristicChanged(gatt, characteristic);
    }

    public void writeCurrentTime(TransactionBuilder builder, ZonedDateTime time) {
        byte[] arr = new byte[11];
        arr[0] = FEATURE_CURRENT_TIME;
        byte[] tmp = prepareCurrentTime(time);
        System.arraycopy(tmp, 0, arr, 1, 10);

        writeAllFeatures(builder, arr);
    }

}
