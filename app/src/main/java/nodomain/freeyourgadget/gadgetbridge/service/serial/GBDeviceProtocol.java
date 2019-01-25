/*  Copyright (C) 2015-2018 Andreas Shimokawa, Carsten Pfeiffer, Julien
    Pivotto, Steffen Liebergeld

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
package nodomain.freeyourgadget.gadgetbridge.service.serial;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;

public abstract class GBDeviceProtocol {

    public static final int RESET_FLAGS_REBOOT = 1;
    public static final int RESET_FLAGS_FACTORY_RESET = 2;

    private GBDevice mDevice;

    protected GBDeviceProtocol(GBDevice device) {
        mDevice = device;
    }

    public byte[] encodeNotification(NotificationSpec notificationSpec) {
        return null;
    }

    public byte[] encodeDeleteNotification(int id) {
        return null;
    }

    public byte[] encodeSetTime() {
        return null;
    }

    public byte[] encodeSetCallState(String number, String name, int command) {
        return null;
    }

    public byte[] encodeSetCannedMessages(CannedMessagesSpec cannedMessagesSpec) {
        return null;
    }

    public byte[] encodeSetMusicInfo(String artist, String album, String track, int duration, int trackCount, int trackNr) {
        return null;
    }

    public byte[] encodeSetMusicState(byte state, int position, int playRate, byte shuffle, byte repeat) {
        return null;
    }

    public byte[] encodeFirmwareVersionReq() {
        return null;
    }

    public byte[] encodeAppInfoReq() {
        return null;
    }

    public byte[] encodeScreenshotReq() {
        return null;
    }

    public byte[] encodeAppDelete(UUID uuid) {
        return null;
    }

    public byte[] encodeAppStart(UUID uuid, boolean start) {
        return null;
    }

    public byte[] encodeAppReorder(UUID[] uuids) {
        return null;
    }

    public byte[] encodeSynchronizeActivityData() {
        return null;
    }

    public byte[] encodeReset(int flags) {
        return null;
    }

    public byte[] encodeFindDevice(boolean start) {
        return null;
    }

    public byte[] encodeEnableRealtimeSteps(boolean enable) {
        return null;
    }

    public byte[] encodeEnableHeartRateSleepSupport(boolean enable) {
        return null;
    }

    public byte[] encodeEnableRealtimeHeartRateMeasurement(boolean enable) { return null; }

    public byte[] encodeAddCalendarEvent(CalendarEventSpec calendarEventSpec) {
        return null;
    }

    public byte[] encodeDeleteCalendarEvent(byte type, long id) {
        return null;
    }

    public byte[] encodeSendConfiguration(String config) {
        return null;
    }

    public byte[] encodeTestNewFunction() { return null; }

    public GBDeviceEvent[] decodeResponse(byte[] responseData) {
        return null;
    }

    public GBDevice getDevice() {
        return mDevice;
    }

    public byte[] encodeSendWeather(WeatherSpec weatherSpec) {
        return null;
    }

    public byte[] encodeLedColor(int color) {
        return null;
    }

    public byte[] encodeFmFrequency(float frequency) {
        return null;
    }
}
