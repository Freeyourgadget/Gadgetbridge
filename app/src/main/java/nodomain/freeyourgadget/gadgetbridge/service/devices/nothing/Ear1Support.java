/*  Copyright (C) 2021-2024 Arjan Schrijver, Daniele Gobbetti, Petr Vaněk

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.nothing;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventCallControl;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.service.serial.AbstractSerialDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceIoThread;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;

public class Ear1Support extends AbstractSerialDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(Ear1Support.class);


    @Override
    public void onSetCallState(CallSpec callSpec) {
        SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress());

        if (!prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_AUTO_REPLY_INCOMING_CALL, false))
            return;

        if(CallSpec.CALL_INCOMING != callSpec.command)
            return;

        LOG.debug("Incoming call, scheduling auto answer in 10 seconds.");
        Looper mainLooper = Looper.getMainLooper();
        new Handler(mainLooper).postDelayed(new Runnable() {
            @Override
            public void run() {
                GBDeviceEventCallControl callCmd = new GBDeviceEventCallControl();
                callCmd.event = GBDeviceEventCallControl.Event.ACCEPT;
                evaluateGBDeviceEvent(callCmd);
            }
        }, 15000); //15s

    }

    @Override
    public void onSendConfiguration(String config) {
        super.onSendConfiguration(config);
    }

    @Override
    public void onTestNewFunction() {
        //getDeviceIOThread().write(((NothingProtocol) getDeviceProtocol()).encodeBatteryStatusReq());
    }

    @Override
    public boolean connect() {
        getDeviceIOThread().start();
        return true;
    }

    @Override
    public synchronized NothingIOThread getDeviceIOThread() {
        return (NothingIOThread) super.getDeviceIOThread();
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    protected GBDeviceProtocol createDeviceProtocol() {
        return new NothingProtocol(getDevice());
    }

    @Override
    protected GBDeviceIoThread createDeviceIOThread() {
        return new NothingIOThread(getDevice(), getContext(), (NothingProtocol) getDeviceProtocol(),
                Ear1Support.this, getBluetoothAdapter());
    }
}
