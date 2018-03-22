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
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSendBytes;
import nodomain.freeyourgadget.gadgetbridge.devices.EventHandler;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.AbstractDeviceSupport;

/**
 * An abstract base class for devices speaking a serial protocol, like via
 * an rfcomm bluetooth socket or a TCP socket.
 * <p/>
 * This class uses two helper classes to deal with that:
 * - GBDeviceIoThread, which creates and maintains the actual socket connection and implements the transport layer
 * - GBDeviceProtocol, which implements the encoding and decoding of messages, i.e. the actual device specific protocol
 * <p/>
 * Note that these two classes need to be implemented in a device specific way.
 * <p/>
 * This implementation implements all methods of {@link EventHandler}, calls the {@link GBDeviceProtocol device protocol}
 * to create the device specific message for the respective events and sends them to the device via {@link #sendToDevice(byte[])}.
 */
public abstract class AbstractSerialDeviceSupport extends AbstractDeviceSupport {
    private GBDeviceProtocol gbDeviceProtocol;
    protected GBDeviceIoThread gbDeviceIOThread;

    /**
     * Factory method to create the device specific GBDeviceProtocol instance to be used.
     */
    protected abstract GBDeviceProtocol createDeviceProtocol();

    /**
     * Factory method to create the device specific GBDeviceIoThread instance to be used.
     */
    protected abstract GBDeviceIoThread createDeviceIOThread();

    @Override
    public void dispose() {
        // currently only one thread allowed
        if (gbDeviceIOThread != null) {
            gbDeviceIOThread.quit();
            gbDeviceIOThread.interrupt();
            gbDeviceIOThread = null;
        }
    }

    /**
     * Lazily creates and returns the GBDeviceProtocol instance to be used.
     */
    protected synchronized GBDeviceProtocol getDeviceProtocol() {
        if (gbDeviceProtocol == null) {
            gbDeviceProtocol = createDeviceProtocol();
        }
        return gbDeviceProtocol;
    }

    /**
     * Lazily creates and returns the GBDeviceIoThread instance to be used.
     */
    public synchronized GBDeviceIoThread getDeviceIOThread() {
        if (gbDeviceIOThread == null) {
            gbDeviceIOThread = createDeviceIOThread();
        }
        return gbDeviceIOThread;
    }

    /**
     * Sends the given message to the device. This implementation delegates the
     * writing to the {@link #getDeviceIOThread device io thread}
     *
     * @param bytes the message to send to the device
     */
    private void sendToDevice(byte[] bytes) {
        if (bytes != null && gbDeviceIOThread != null) {
            gbDeviceIOThread.write(bytes);
        }
    }

    private void handleGBDeviceEvent(GBDeviceEventSendBytes sendBytes) {
        sendToDevice(sendBytes.encodedBytes);
    }

    @Override
    public void evaluateGBDeviceEvent(GBDeviceEvent deviceEvent) {
        if (deviceEvent instanceof GBDeviceEventSendBytes) {
            handleGBDeviceEvent((GBDeviceEventSendBytes) deviceEvent);
            return;
        }
        super.evaluateGBDeviceEvent(deviceEvent);
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        byte[] bytes = gbDeviceProtocol.encodeNotification(notificationSpec);
        sendToDevice(bytes);
    }

    @Override
    public void onDeleteNotification(int id) {
        byte[] bytes = gbDeviceProtocol.encodeDeleteNotification(id);
        sendToDevice(bytes);
    }

    @Override
    public void onSetTime() {
        byte[] bytes = gbDeviceProtocol.encodeSetTime();
        sendToDevice(bytes);
    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
        byte[] bytes = gbDeviceProtocol.encodeSetCallState(callSpec.number, callSpec.name, callSpec.command);
        sendToDevice(bytes);
    }

    @Override
    public void onSetCannedMessages(CannedMessagesSpec cannedMessagesSpec) {
        byte[] bytes = gbDeviceProtocol.encodeSetCannedMessages(cannedMessagesSpec);
        sendToDevice(bytes);
    }

    @Override
    public void onSetMusicState(MusicStateSpec stateSpec) {
        byte[] bytes = gbDeviceProtocol.encodeSetMusicState(stateSpec.state, stateSpec.position, stateSpec.playRate, stateSpec.shuffle, stateSpec.repeat);
        sendToDevice(bytes);
    }

    @Override
    public void onSetMusicInfo(MusicSpec musicSpec) {
        byte[] bytes = gbDeviceProtocol.encodeSetMusicInfo(musicSpec.artist, musicSpec.album, musicSpec.track, musicSpec.duration, musicSpec.trackCount, musicSpec.trackNr);
        sendToDevice(bytes);
    }

    @Override
    public void onAppInfoReq() {
        byte[] bytes = gbDeviceProtocol.encodeAppInfoReq();
        sendToDevice(bytes);
    }

    @Override
    public void onAppStart(UUID uuid, boolean start) {
        byte[] bytes = gbDeviceProtocol.encodeAppStart(uuid, start);
        sendToDevice(bytes);
    }

    @Override
    public void onAppDelete(UUID uuid) {
        byte[] bytes = gbDeviceProtocol.encodeAppDelete(uuid);
        sendToDevice(bytes);
    }

    @Override
    public void onAppReorder(UUID[] uuids) {
        byte[] bytes = gbDeviceProtocol.encodeAppReorder(uuids);
        sendToDevice(bytes);
    }

    @Override
    public void onFetchActivityData() {
        byte[] bytes = gbDeviceProtocol.encodeSynchronizeActivityData();
        sendToDevice(bytes);
    }

    @Override
    public void onReboot() {
        byte[] bytes = gbDeviceProtocol.encodeReboot();
        sendToDevice(bytes);
    }

    @Override
    public void onFindDevice(boolean start) {
        byte[] bytes = gbDeviceProtocol.encodeFindDevice(start);
        sendToDevice(bytes);
    }

    @Override
    public void onScreenshotReq() {
        byte[] bytes = gbDeviceProtocol.encodeScreenshotReq();
        sendToDevice(bytes);
    }

    @Override
    public void onEnableRealtimeSteps(boolean enable) {
        byte[] bytes = gbDeviceProtocol.encodeEnableRealtimeSteps(enable);
        sendToDevice(bytes);
    }

    @Override
    public void onEnableHeartRateSleepSupport(boolean enable) {
        byte[] bytes = gbDeviceProtocol.encodeEnableHeartRateSleepSupport(enable);
        sendToDevice(bytes);
    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(boolean enable) {
        byte[] bytes = gbDeviceProtocol.encodeEnableRealtimeHeartRateMeasurement(enable);
        sendToDevice(bytes);
    }

    @Override
    public void onAddCalendarEvent(CalendarEventSpec calendarEventSpec) {
        byte[] bytes = gbDeviceProtocol.encodeAddCalendarEvent(calendarEventSpec);
        sendToDevice(bytes);
    }

    @Override
    public void onDeleteCalendarEvent(byte type, long id) {
        byte[] bytes = gbDeviceProtocol.encodeDeleteCalendarEvent(type, id);
        sendToDevice(bytes);
    }

    @Override
    public void onSendConfiguration(String config) {
        byte[] bytes = gbDeviceProtocol.encodeSendConfiguration(config);
        sendToDevice(bytes);
    }

    @Override
    public void onTestNewFunction() {
        byte[] bytes = gbDeviceProtocol.encodeTestNewFunction();
        sendToDevice(bytes);
    }

    @Override
    public void onSendWeather(WeatherSpec weatherSpec) {
        byte[] bytes = gbDeviceProtocol.encodeSendWeather(weatherSpec);
        sendToDevice(bytes);
    }
}
