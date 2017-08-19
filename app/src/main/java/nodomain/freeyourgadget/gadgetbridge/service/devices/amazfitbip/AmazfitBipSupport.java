/*  Copyright (C) 2017 Andreas Shimokawa

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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventCallControl;
import nodomain.freeyourgadget.gadgetbridge.devices.amazfitbip.AmazfitBipService;
import nodomain.freeyourgadget.gadgetbridge.devices.amazfitbip.AmazfitBipWeatherConditions;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBand2Service;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.common.SimpleNotification;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.NotificationStrategy;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband2.MiBand2Support;
import nodomain.freeyourgadget.gadgetbridge.util.NotificationUtils;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class AmazfitBipSupport extends MiBand2Support {
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
        String message = NotificationUtils.getPreferredTextFor(notificationSpec, 40, 40, getContext()).trim();

        // Fixup purging notification text in getPreferredTextFor() in case of UNKNOWN
        if (notificationSpec.type == NotificationType.UNKNOWN) {
            message = StringUtils.getFirstOf(notificationSpec.title, notificationSpec.body);

        }
        String origin = notificationSpec.type.getGenericType();
        SimpleNotification simpleNotification = new SimpleNotification(message, BLETypeConversions.toAlertCategory(notificationSpec.type));
        performPreferredNotification(origin + " received", origin, simpleNotification, MiBand2Service.ALERT_LEVEL_MESSAGE, null);
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

        if (value[0] == 0x07) {
            callCmd.event = GBDeviceEventCallControl.Event.REJECT;
        } else if (value[0] == 0x09) {
            callCmd.event = GBDeviceEventCallControl.Event.ACCEPT;
        } else {
            return;
        }
        evaluateGBDeviceEvent(callCmd);
    }

    @Override
    public void onSendWeather(WeatherSpec weatherSpec) {
        try {
            TransactionBuilder builder = performInitialized("Sending weather forecast");
            final byte NR_DAYS = 2;
            ByteBuffer buf = ByteBuffer.allocate(7 + 4 * NR_DAYS);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.put((byte) 1);
            buf.putInt(weatherSpec.timestamp);
            buf.put((byte) 0);

            buf.put(NR_DAYS);

            byte condition = AmazfitBipWeatherConditions.mapToAmazfitBipWeatherCode(weatherSpec.currentConditionCode);
            buf.put(condition);
            buf.put(condition);
            buf.put((byte) (weatherSpec.todayMaxTemp - 273));
            buf.put((byte) (weatherSpec.todayMinTemp - 273));

            condition = AmazfitBipWeatherConditions.mapToAmazfitBipWeatherCode(weatherSpec.tomorrowConditionCode);

            buf.put(condition);
            buf.put(condition);
            buf.put((byte) (weatherSpec.tomorrowMaxTemp - 273));
            buf.put((byte) (weatherSpec.tomorrowMinTemp - 273));

            builder.write(getCharacteristic(AmazfitBipService.UUID_CHARACTERISTIC_WEATHER), buf.array());
            builder.queue(getQueue());
        } catch (IOException ignore) {
        }
    }
}