/*  Copyright (C) 2016-2020 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti
    Copyright (C) 2020 Yukai Li

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.lefun;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.net.Uri;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.devices.lefun.LefunConstants;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.lefun.requests.GetBatteryLevelRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.lefun.requests.GetFirmwareInfoRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.lefun.requests.Request;
import nodomain.freeyourgadget.gadgetbridge.service.devices.lefun.requests.SetTimeRequest;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class LefunDeviceSupport extends AbstractBTLEDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(LefunDeviceSupport.class);

    private final List<Request> inProgressRequests = Collections.synchronizedList(new ArrayList<Request>());

    public LefunDeviceSupport() {
        super(LOG);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ACCESS);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ATTRIBUTE);
        addSupportedService(GattService.UUID_SERVICE_DEVICE_INFORMATION);
        addSupportedService(LefunConstants.UUID_SERVICE_LEFUN);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        builder.setGattCallback(this);
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));

        // Enable notification
        builder.notify(getCharacteristic(LefunConstants.UUID_CHARACTERISTIC_LEFUN_NOTIFY), true);

        // Init device (just get version and battery)
        try {
            GetFirmwareInfoRequest firmwareReq = new GetFirmwareInfoRequest(this, builder);
            firmwareReq.perform();
            inProgressRequests.add(firmwareReq);

            SetTimeRequest timeReq = new SetTimeRequest(this, builder);
            timeReq.perform();
            inProgressRequests.add(timeReq);

            GetBatteryLevelRequest batReq = new GetBatteryLevelRequest(this, builder);
            batReq.perform();
            inProgressRequests.add(batReq);
        } catch (IOException e) {
            GB.toast(getContext(), "Failed to initialize Lefun device", Toast.LENGTH_SHORT,
                    GB.ERROR, e);
        }

        return builder;
    }

    @Override
    public boolean useAutoConnect() {
        return true;
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {

    }

    @Override
    public void onDeleteNotification(int id) {

    }

    @Override
    public void onSetTime() {

    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {

    }

    @Override
    public void onSetCallState(CallSpec callSpec) {

    }

    @Override
    public void onSetCannedMessages(CannedMessagesSpec cannedMessagesSpec) {

    }

    @Override
    public void onSetMusicState(MusicStateSpec stateSpec) {

    }

    @Override
    public void onSetMusicInfo(MusicSpec musicSpec) {

    }

    @Override
    public void onEnableRealtimeSteps(boolean enable) {

    }

    @Override
    public void onInstallApp(Uri uri) {

    }

    @Override
    public void onAppInfoReq() {

    }

    @Override
    public void onAppStart(UUID uuid, boolean start) {

    }

    @Override
    public void onAppDelete(UUID uuid) {

    }

    @Override
    public void onAppConfiguration(UUID appUuid, String config, Integer id) {

    }

    @Override
    public void onAppReorder(UUID[] uuids) {

    }

    @Override
    public void onFetchRecordedData(int dataTypes) {

    }

    @Override
    public void onReset(int flags) {

    }

    @Override
    public void onHeartRateTest() {

    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(boolean enable) {

    }

    @Override
    public void onFindDevice(boolean start) {

    }

    @Override
    public void onSetConstantVibration(int integer) {

    }

    @Override
    public void onScreenshotReq() {

    }

    @Override
    public void onEnableHeartRateSleepSupport(boolean enable) {

    }

    @Override
    public void onSetHeartRateMeasurementInterval(int seconds) {

    }

    @Override
    public void onAddCalendarEvent(CalendarEventSpec calendarEventSpec) {

    }

    @Override
    public void onDeleteCalendarEvent(byte type, long id) {

    }

    @Override
    public void onSendConfiguration(String config) {

    }

    @Override
    public void onReadConfiguration(String config) {

    }

    @Override
    public void onTestNewFunction() {

    }

    @Override
    public void onSendWeather(WeatherSpec weatherSpec) {

    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (characteristic.getUuid().equals(LefunConstants.UUID_CHARACTERISTIC_LEFUN_NOTIFY)) {
            byte[] data = characteristic.getValue();
            // Parse response
            if (data.length >= LefunConstants.CMD_HEADER_LENGTH && data[0] == LefunConstants.CMD_RESPONSE_ID) {
                // Note: full validation is done within the request
                byte commandId = data[2];
                synchronized (inProgressRequests) {
                    for (Request req : inProgressRequests) {
                        if (req.expectsResponse() && req.getCommandId() == commandId) {
                            try {
                                req.handleResponse(data);
                                inProgressRequests.remove(req);
                                return true;
                            } catch (IllegalArgumentException e) {
                                LOG.error("Failed to handle response", e);
                            }
                        }
                    }
                }

                if (handleAsynchronousResponse(data))
                    return true;

                LOG.error(String.format("No handler for response 0x%02x", commandId));
                return false;
            }

            LOG.error("Invalid response received");
            return false;
        }

        return super.onCharacteristicChanged(gatt, characteristic);
    }

    private boolean handleAsynchronousResponse(byte[] data) {
        // Assume data already checked for correct response code and length
        return false;
    }

    public void completeInitialization() {
        gbDevice.setState(GBDevice.State.INITIALIZED);
        gbDevice.sendDeviceUpdateIntent(getContext());
    }
}
