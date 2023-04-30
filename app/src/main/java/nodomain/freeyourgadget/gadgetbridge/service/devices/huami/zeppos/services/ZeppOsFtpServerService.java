/*  Copyright (C) 2022 José Rebelo

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

import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class ZeppOsFtpServerService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsFtpServerService.class);

    private static final short ENDPOINT = 0x0006;

    public static final byte FTP_CMD_START = 0x01;
    public static final byte FTP_CMD_STOP = 0x02;
    public static final byte FTP_CMD_INFO = 0x03;
    public static final byte FTP_INFO_STOP = 0x00;
    public static final byte FTP_INFO_STARTED = 0x02;

    private Callback mCallback = null;

    public ZeppOsFtpServerService(final Huami2021Support support) {
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
            case FTP_CMD_INFO:
                switch (payload[1]) {
                    case FTP_INFO_STOP:
                        LOG.info("FTP Server stopped");

                        if (mCallback != null) {
                            mCallback.onFtpServerStop();
                        } else {
                            // If there's no callback, show a toast (eg. used from developer options)
                            GB.toast("FTP Server stopped", Toast.LENGTH_SHORT, GB.INFO);
                        }

                        // TODO: The toast + preference update should not be here
                        final GBDeviceEventUpdatePreferences wifiStoppedEvent = new GBDeviceEventUpdatePreferences()
                                .withPreference(DeviceSettingsPreferenceConst.FTP_SERVER_STATUS, "Stopped");
                        getSupport().evaluateGBDeviceEvent(wifiStoppedEvent);
                        return;
                    case FTP_INFO_STARTED:
                        final String address = StringUtils.untilNullTerminator(payload, 2);
                        if (address == null) {
                            LOG.error("Unable to parse address from FTP info payload");
                            return;
                        }
                        final String username = StringUtils.untilNullTerminator(payload, 2 + address.length() + 1);
                        if (username == null) {
                            LOG.error("Unable to parse username from FTP info payload");
                            return;
                        }
                        LOG.info("FTP Server started, address = {}, username = {}", address, username);

                        if (mCallback != null) {
                            mCallback.onFtpServerStart(address, username);
                        } else {
                            // If there's no callback, show a toast (eg. used from developer options)
                            GB.toast("FTP Server started", Toast.LENGTH_SHORT, GB.INFO);
                        }

                        // TODO: The toast + preference update should not be here
                        final GBDeviceEventUpdatePreferences wifiStartedEvent = new GBDeviceEventUpdatePreferences()
                                .withPreference(DeviceSettingsPreferenceConst.FTP_SERVER_ADDRESS, address)
                                .withPreference(DeviceSettingsPreferenceConst.FTP_SERVER_USERNAME, username)
                                .withPreference(DeviceSettingsPreferenceConst.FTP_SERVER_STATUS, "Started");
                        getSupport().evaluateGBDeviceEvent(wifiStartedEvent);
                        return;
                    default:
                        LOG.warn("Unexpected FTP info byte {}", String.format("0x%02x", payload[1]));
                        break;
                }
                return;
            default:
                LOG.warn("Unexpected FTP byte {}", String.format("0x%02x", payload[0]));
        }
    }

    @Override
    public boolean onSendConfiguration(final String config, final Prefs prefs) {
        switch (config) {
            case DeviceSettingsPreferenceConst.FTP_SERVER_START:
                startFtpServer(prefs.getString(DeviceSettingsPreferenceConst.FTP_SERVER_ROOT_DIR, ""));
                return true;
            case DeviceSettingsPreferenceConst.FTP_SERVER_STOP:
                stopFtpServer();
                return true;
            case DeviceSettingsPreferenceConst.FTP_SERVER_ROOT_DIR:
            case DeviceSettingsPreferenceConst.FTP_SERVER_ADDRESS:
            case DeviceSettingsPreferenceConst.FTP_SERVER_USERNAME:
            case DeviceSettingsPreferenceConst.FTP_SERVER_STATUS:
                // Ignore preferences that are not reloadable
                return true;
        }

        return false;
    }

    @Override
    public void initialize(final TransactionBuilder builder) {
    }

    public void setCallback(final Callback callback) {
        mCallback = callback;
    }

    public void removeCallback() {
        mCallback = null;
    }

    public void startFtpServer(final String rootDir) {
        LOG.info("Starting FTP Server, rootDir={}", rootDir);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            baos.write(FTP_CMD_START);
            if (!StringUtils.isNullOrEmpty(rootDir)) {
                baos.write(rootDir.getBytes(StandardCharsets.UTF_8));
                baos.write(0);
            }
        } catch (final Exception e) {
            LOG.error("Failed to create command", e);
            return;
        }

        write("start ftp server", baos.toByteArray());
    }

    public void stopFtpServer() {
        LOG.info("Stopping FTP Server");

        write("stop ftp server", FTP_CMD_STOP);
    }

    public interface Callback {
        void onFtpServerStart(String address, String username);

        void onFtpServerStop();
    }
}
