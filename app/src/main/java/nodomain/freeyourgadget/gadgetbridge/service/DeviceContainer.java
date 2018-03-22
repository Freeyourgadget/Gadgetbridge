/*  Copyright (C) 2015-2018 Taavi Eomäe

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

package nodomain.freeyourgadget.gadgetbridge.service;

import java.util.Random;

import nodomain.freeyourgadget.gadgetbridge.externalevents.AlarmClockReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.AlarmReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.BluetoothConnectReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.BluetoothPairingRequestReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.CMWeatherReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.CalendarReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.MusicPlaybackReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.OmniJawsObserver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.PebbleReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.PhoneCallReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.SMSReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.TimeChangeReceiver;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class DeviceContainer {
    private GBDevice GBDevice;
    private boolean started = false;
    private DeviceSupport deviceSupport;

    private PhoneCallReceiver phoneCallReceiver = null;
    private SMSReceiver SMSReceiver = null;
    private PebbleReceiver pebbleReceiver = null;
    private MusicPlaybackReceiver musicPlaybackReceiver = null;
    private TimeChangeReceiver timeChangeReceiver = null;
    private BluetoothConnectReceiver blueToothConnectReceiver = null;
    private BluetoothPairingRequestReceiver blueToothPairingRequestReceiver = null;
    private AlarmClockReceiver alarmClockReceiver = null;

    private AlarmReceiver alarmReceiver = null;
    private CalendarReceiver calendarReceiver = null;
    private CMWeatherReceiver CMWeatherReceiver = null;
    private OmniJawsObserver omniJawsObserver = null;
    private Random random = new Random();
    private DeviceSupportFactory deviceSupportFactory;

    public DeviceContainer(GBDevice GBDevice, DeviceSupport deviceSupport) {
        this.GBDevice = GBDevice;
        this.deviceSupport = deviceSupport;
    }

    public DeviceContainer(GBDevice gbDevice){
        this.GBDevice = gbDevice;
    }

    public nodomain.freeyourgadget.gadgetbridge.impl.GBDevice getGBDevice() {
        return GBDevice;
    }

    public void setGBDevice(nodomain.freeyourgadget.gadgetbridge.impl.GBDevice GBDevice) {
        this.GBDevice = GBDevice;
    }

    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    public DeviceSupport getDeviceSupport() {
        return deviceSupport;
    }

    public void setDeviceSupport(DeviceSupport deviceSupport) {
        this.deviceSupport = deviceSupport;
    }

    public PhoneCallReceiver getPhoneCallReceiver() {
        return phoneCallReceiver;
    }

    public void setPhoneCallReceiver(PhoneCallReceiver phoneCallReceiver) {
        this.phoneCallReceiver = phoneCallReceiver;
    }

    public nodomain.freeyourgadget.gadgetbridge.externalevents.SMSReceiver getSMSReceiver() {
        return SMSReceiver;
    }

    public void setSMSReceiver(nodomain.freeyourgadget.gadgetbridge.externalevents.SMSReceiver SMSReceiver) {
        this.SMSReceiver = SMSReceiver;
    }

    public PebbleReceiver getPebbleReceiver() {
        return pebbleReceiver;
    }

    public void setPebbleReceiver(PebbleReceiver pebbleReceiver) {
        this.pebbleReceiver = pebbleReceiver;
    }

    public MusicPlaybackReceiver getMusicPlaybackReceiver() {
        return musicPlaybackReceiver;
    }

    public void setMusicPlaybackReceiver(MusicPlaybackReceiver musicPlaybackReceiver) {
        this.musicPlaybackReceiver = musicPlaybackReceiver;
    }

    public TimeChangeReceiver getTimeChangeReceiver() {
        return timeChangeReceiver;
    }

    public void setTimeChangeReceiver(TimeChangeReceiver timeChangeReceiver) {
        this.timeChangeReceiver = timeChangeReceiver;
    }

    public BluetoothConnectReceiver getBlueToothConnectReceiver() {
        return blueToothConnectReceiver;
    }

    public void setBlueToothConnectReceiver(BluetoothConnectReceiver blueToothConnectReceiver) {
        this.blueToothConnectReceiver = blueToothConnectReceiver;
    }

    public BluetoothPairingRequestReceiver getBlueToothPairingRequestReceiver() {
        return blueToothPairingRequestReceiver;
    }

    public void setBlueToothPairingRequestReceiver(BluetoothPairingRequestReceiver blueToothPairingRequestReceiver) {
        this.blueToothPairingRequestReceiver = blueToothPairingRequestReceiver;
    }

    public AlarmClockReceiver getAlarmClockReceiver() {
        return alarmClockReceiver;
    }

    public void setAlarmClockReceiver(AlarmClockReceiver alarmClockReceiver) {
        this.alarmClockReceiver = alarmClockReceiver;
    }

    public AlarmReceiver getAlarmReceiver() {
        return alarmReceiver;
    }

    public void setAlarmReceiver(AlarmReceiver alarmReceiver) {
        this.alarmReceiver = alarmReceiver;
    }

    public CalendarReceiver getCalendarReceiver() {
        return calendarReceiver;
    }

    public void setCalendarReceiver(CalendarReceiver calendarReceiver) {
        this.calendarReceiver = calendarReceiver;
    }

    public nodomain.freeyourgadget.gadgetbridge.externalevents.CMWeatherReceiver getCMWeatherReceiver() {
        return CMWeatherReceiver;
    }

    public void setCMWeatherReceiver(nodomain.freeyourgadget.gadgetbridge.externalevents.CMWeatherReceiver CMWeatherReceiver) {
        this.CMWeatherReceiver = CMWeatherReceiver;
    }

    public OmniJawsObserver getOmniJawsObserver() {
        return omniJawsObserver;
    }

    public void setOmniJawsObserver(OmniJawsObserver omniJawsObserver) {
        this.omniJawsObserver = omniJawsObserver;
    }

    public Random getRandom() {
        return random;
    }

    public void setRandom(Random random) {
        this.random = random;
    }

    public DeviceSupportFactory getDeviceSupportFactory() {
        return deviceSupportFactory;
    }

    public void setDeviceSupportFactory(DeviceSupportFactory deviceSupportFactory) {
        this.deviceSupportFactory = deviceSupportFactory;
    }
}
