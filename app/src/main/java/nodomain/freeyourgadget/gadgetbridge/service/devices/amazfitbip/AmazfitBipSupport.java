/*  Copyright (C) 2017 Andreas Shimokawa, Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.amazfitbip;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.net.Uri;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.SimpleTimeZone;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.Logging;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventCallControl;
import nodomain.freeyourgadget.gadgetbridge.devices.miband2.MiBand2Icon;
import nodomain.freeyourgadget.gadgetbridge.devices.amazfitbip.AmazfitBipService;
import nodomain.freeyourgadget.gadgetbridge.devices.amazfitbip.AmazfitBipWeatherConditions;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBand2Service;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.alertnotification.AlertCategory;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.alertnotification.AlertNotificationProfile;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.alertnotification.NewAlert;
import nodomain.freeyourgadget.gadgetbridge.service.devices.amazfitbip.operations.AmazfitBipUpdateFirmwareOperation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.NotificationStrategy;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband2.MiBand2Support;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Version;

public class AmazfitBipSupport extends MiBand2Support {

    private static final Logger LOG = LoggerFactory.getLogger(AmazfitBipSupport.class);

    @Override
    public NotificationStrategy getNotificationStrategy() {
        return new AmazfitBipTextNotificationStrategy(this);
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        if (notificationSpec.type == NotificationType.GENERIC_ALARM_CLOCK) {
            onAlarmClock(notificationSpec);
            return;
        }

        String senderOrTiltle = StringUtils.getFirstOf(notificationSpec.sender, notificationSpec.title);

        String message = StringUtils.truncate(senderOrTiltle, 32) + "\0";
        if (notificationSpec.subject != null) {
            message += StringUtils.truncate(notificationSpec.subject, 128) + "\n\n";
        }
        if (notificationSpec.body != null) {
            message += StringUtils.truncate(notificationSpec.body, 128);
        }

        try {
            TransactionBuilder builder = performInitialized("new notification");
            AlertNotificationProfile<?> profile = new AlertNotificationProfile(this);
            profile.setMaxLength(230);

            byte customIconId = MiBand2Icon.mapToIconId(notificationSpec.type);

            AlertCategory alertCategory = AlertCategory.CustomMiBand2;

            // The SMS icon for AlertCategory.SMS is unique and not available as iconId
            if (notificationSpec.type == NotificationType.GENERIC_SMS) {
                alertCategory = AlertCategory.SMS;
            }
            // EMAIL icon does not work in FW 0.0.8.74, it did in 0.0.7.90
            else if (notificationSpec.type == NotificationType.GENERIC_EMAIL) {
                alertCategory = AlertCategory.Email;
            }

            NewAlert alert = new NewAlert(alertCategory, 1, message, customIconId);
            profile.newAlert(builder, alert);
            builder.queue(getQueue());
        } catch (IOException ex) {
            LOG.error("Unable to send notification to Amazfit Bip", ex);
        }
    }

    @Override
    public void onFindDevice(boolean start) {
        CallSpec callSpec = new CallSpec();
        callSpec.command = start ? CallSpec.CALL_INCOMING : CallSpec.CALL_END;
        callSpec.name = "Gadgetbridge";
        onSetCallState(callSpec);
    }

    @Override
    public void handleButtonPressed(byte[] value) {
        if (value == null || value.length != 1) {
            return;
        }
        GBDeviceEventCallControl callCmd = new GBDeviceEventCallControl();

        switch (value[0]) {
            case AmazfitBipEvent.CALL_REJECT:
                callCmd.event = GBDeviceEventCallControl.Event.REJECT;
                evaluateGBDeviceEvent(callCmd);
                break;
            case AmazfitBipEvent.CALL_ACCEPT:
                callCmd.event = GBDeviceEventCallControl.Event.ACCEPT;
                evaluateGBDeviceEvent(callCmd);
                break;
            case AmazfitBipEvent.BUTTON_PRESSED:
                LOG.info("button pressed");
                break;
            case AmazfitBipEvent.BUTTON_PRESSED_LONG:
                LOG.info("button long-pressed ");
                break;
            case AmazfitBipEvent.START_NONWEAR:
                LOG.info("non-wear start detected");
                break;
            case AmazfitBipEvent.ALARM_TOGGLED:
                LOG.info("An alarm was toggled"); // TODO: sync alarms watch -> GB
                break;
            default:
                LOG.warn("unhandled event " + value[0]);
        }
    }

    @Override
    public void onInstallApp(Uri uri) {
        try {
            new AmazfitBipUpdateFirmwareOperation(uri, this).perform();
        } catch (IOException ex) {
            GB.toast(getContext(), "Firmware cannot be installed: " + ex.getMessage(), Toast.LENGTH_LONG, GB.ERROR, ex);
        }
    }

    @Override
    public void onSendWeather(WeatherSpec weatherSpec) {
        if (gbDevice.getFirmwareVersion() == null) {
            LOG.warn("Device not initialized yet, so not sending weather info");
            return;
        }

        try {
            TransactionBuilder builder = performInitialized("Sending weather forecast");
            boolean supportsConditionString = false;

            Version version = new Version(gbDevice.getFirmwareVersion());
            if (version.compareTo(new Version("0.0.8.74")) >= 0) {
                supportsConditionString = true;
            }

            final byte NR_DAYS = 2;
            int bytesPerDay = 4;
            int conditionsLength = 0;
            if (supportsConditionString) {
                bytesPerDay = 5;
                conditionsLength = weatherSpec.currentCondition.getBytes().length;
            }
            int length = 7 + bytesPerDay * NR_DAYS + conditionsLength;
            ByteBuffer buf = ByteBuffer.allocate(length);

            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.put((byte) 1);
            buf.putInt(weatherSpec.timestamp);
            int tz_offset_hours = SimpleTimeZone.getDefault().getOffset(weatherSpec.timestamp * 1000L) / (1000 * 60 * 60);
            buf.put((byte) (tz_offset_hours * 4));

            buf.put(NR_DAYS);

            byte condition = AmazfitBipWeatherConditions.mapToAmazfitBipWeatherCode(weatherSpec.currentConditionCode);
            buf.put(condition);
            buf.put(condition);
            buf.put((byte) (weatherSpec.todayMaxTemp - 273));
            buf.put((byte) (weatherSpec.todayMinTemp - 273));
            if (supportsConditionString) {
                buf.put(weatherSpec.currentCondition.getBytes());
                buf.put((byte) 0); //
            }
            condition = AmazfitBipWeatherConditions.mapToAmazfitBipWeatherCode(weatherSpec.tomorrowConditionCode);

            buf.put(condition);
            buf.put(condition);
            buf.put((byte) (weatherSpec.tomorrowMaxTemp - 273));
            buf.put((byte) (weatherSpec.tomorrowMinTemp - 273));
            if (supportsConditionString) {
                buf.put((byte) 0); // not yet in weatherspec
            }

            builder.write(getCharacteristic(AmazfitBipService.UUID_CHARACTERISTIC_WEATHER), buf.array());
            builder.queue(getQueue());
        } catch (Exception ex) {
            LOG.error("Error sending weather information to the Bip", ex);
        }
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        boolean handled = super.onCharacteristicChanged(gatt, characteristic);
        if (!handled) {
            UUID characteristicUUID = characteristic.getUuid();
            if (MiBand2Service.UUID_CHARACTERISTIC_3_CONFIGURATION.equals(characteristicUUID)) {
                return handleConfigurationInfo(characteristic.getValue());
            }
        }
        return false;
    }

    private boolean handleConfigurationInfo(byte[] value) {
        if (value == null || value.length < 4) {
            return false;
        }
        if (value[0] == 0x10 && value[1] == 0x0e && value[2] == 0x01) {
            String gpsVersion = new String(value, 3, value.length - 3);
            LOG.info("got gps version = " + gpsVersion);
            gbDevice.setFirmwareVersion2(gpsVersion);
            return true;
        }
        return false;
    }

    // this probably does more than only getting the GPS version...
    private AmazfitBipSupport requestGPSVersion(TransactionBuilder builder) {
        LOG.info("Requesting GPS version");
        builder.write(getCharacteristic(MiBand2Service.UUID_CHARACTERISTIC_3_CONFIGURATION), AmazfitBipService.COMMAND_REQUEST_GPS_VERSION);
        return this;
    }

    @Override
    public void phase2Initialize(TransactionBuilder builder) {
        super.phase2Initialize(builder);
        LOG.info("phase2Initialize...");
        requestGPSVersion(builder);
    }
}
