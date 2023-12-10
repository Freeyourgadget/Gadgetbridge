/*  Copyright (C) 2023 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services;

import android.bluetooth.BluetoothAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class ZeppOsPhoneService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsPhoneService.class);

    private static final short ENDPOINT = 0x000b;

    public static final byte CMD_CAPABILITIES_REQUEST = 0x01;
    public static final byte CMD_CAPABILITIES_RESPONSE = 0x02;
    public static final byte CMD_PAIRED_GET = 0x03;
    public static final byte CMD_PAIRED_RET = 0x04;
    public static final byte CMD_START_PAIR = 0x05;
    public static final byte CMD_ENABLED_REQUEST = 0x06;
    public static final byte CMD_ENABLED_RESPONSE = 0x07;
    public static final byte CMD_ENABLED_SET = 0x08;
    public static final byte CMD_ENABLED_SET_ACK = 0x09;

    public static final String PREF_VERSION = "zepp_os_phone_service_version";

    private int version = 0;

    public ZeppOsPhoneService(final Huami2021Support support) {
        super(support);
    }

    @Override
    public short getEndpoint() {
        return ENDPOINT;
    }

    @Override
    public boolean isEncrypted() {
        return true;
    }

    @Override
    public void handlePayload(final byte[] payload) {
        switch (payload[0]) {
            case CMD_CAPABILITIES_RESPONSE:
                version = payload[1];
                getSupport().evaluateGBDeviceEvent(new GBDeviceEventUpdatePreferences(PREF_VERSION, version));
                if (version != 1) {
                    LOG.warn("Unsupported phone service version {}", version);
                    return;
                }
                LOG.info("Phone version={}", version);
                break;
            case CMD_PAIRED_RET:
                final byte pairedStatus = payload[1];
                // 0 = unpaired
                // 1 = paired
                // 4 = ?
                LOG.info("Got phone pair status = {}", pairedStatus);
                break;
            case CMD_ENABLED_RESPONSE:
                if (payload.length != 4) {
                    LOG.error("Unexpected phone enabled payload size {}", payload.length);
                    return;
                }

                if (payload[1] != 0x01 || payload[2] != 0x01) {
                    LOG.error("Unexpected phone enabled bytes");
                    return;
                }

                final Boolean phoneEnabled = booleanFromByte(payload[3]);
                if (phoneEnabled == null) {
                    LOG.error("Unexpected phone enabled byte");
                    return;
                }
                LOG.info("Got phone enabled = {}", phoneEnabled);
                getSupport().evaluateGBDeviceEvent(new GBDeviceEventUpdatePreferences(DeviceSettingsPreferenceConst.PREF_BLUETOOTH_CALLS_ENABLED, phoneEnabled));
                break;
            case CMD_ENABLED_SET_ACK:
                LOG.info("Got phone enabled set ack, status = {}", payload[1]);
                break;
            default:
                LOG.warn("Unexpected phone byte {}", String.format("0x%02x", payload[0]));
        }
    }

    @Override
    public boolean onSendConfiguration(final String config, final Prefs prefs) {
        switch (config) {
            case DeviceSettingsPreferenceConst.PREF_BLUETOOTH_CALLS_PAIR:
                if (!isSupported()) {
                    LOG.warn("Phone service is not supported.");
                    return false;
                }

                startPairing();
                return true;
            case DeviceSettingsPreferenceConst.PREF_BLUETOOTH_CALLS_ENABLED:
                final boolean bluetoothCallsEnabled = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_BLUETOOTH_CALLS_ENABLED, false);
                LOG.info("Setting bluetooth calls enabled = {}", bluetoothCallsEnabled);
                setEnabled(bluetoothCallsEnabled);
                return true;
        }

        return false;
    }

    @Override
    public void initialize(final TransactionBuilder builder) {
    }

    public boolean isSupported() {
        return version == 1;
    }

    public void requestCapabilities(final TransactionBuilder builder) {
        write(builder, CMD_CAPABILITIES_REQUEST);
    }

    public void getPairedStatus() {
        final String bluetoothName = getBluetoothName();
        if (bluetoothName == null) {
            LOG.error("bluetoothName is null");
            return;
        }

        final byte[] nameBytes = bluetoothName.getBytes(StandardCharsets.UTF_8);

        final ByteBuffer buf = ByteBuffer.allocate(2 + nameBytes.length)
                .order(ByteOrder.LITTLE_ENDIAN);

        buf.put(CMD_PAIRED_GET);
        buf.put(nameBytes);
        buf.put((byte) 0);

        write("get paired status", buf.array());
    }

    public void startPairing() {
        final String bluetoothName = getBluetoothName();
        if (bluetoothName == null) {
            LOG.error("bluetoothName is null");
            return;
        }

        final byte[] nameBytes = bluetoothName.getBytes(StandardCharsets.UTF_8);

        final ByteBuffer buf = ByteBuffer.allocate(2 + nameBytes.length)
                .order(ByteOrder.LITTLE_ENDIAN);

        buf.put(CMD_START_PAIR);
        buf.put(nameBytes);
        buf.put((byte) 0);

        write("start phone pairing", buf.array());
    }

    public void requestEnabled(final TransactionBuilder builder) {
        write(builder, CMD_ENABLED_REQUEST);
    }

    public void setEnabled(final boolean enabled) {
        final byte[] cmd = new byte[]{
                CMD_ENABLED_SET,
                0x01,
                0x01,
                (byte) (enabled ? 0x01 : 0x00)
        };

        write("set phone enabled", cmd);
    }

    @Nullable
    public String getBluetoothName() {
        final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            LOG.error("bluetoothAdapter is null");
            return null;
        }

        return bluetoothAdapter.getName();
    }

    public static boolean isSupported(final Prefs devicePrefs) {
        return devicePrefs.getInt(PREF_VERSION, 0) == 1;
    }
}
