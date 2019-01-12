/*      Copyright (C) 2018 Andreas Böhler
        based on code from BlueWatcher, https://github.com/masterjc/bluewatcher

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.casiogb6900;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.GregorianCalendar;

import nodomain.freeyourgadget.gadgetbridge.devices.casiogb6900.CasioGB6900Constants;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceIoThread;

public class CasioHandlerThread extends GBDeviceIoThread {
    private static final Logger LOG = LoggerFactory.getLogger(CasioHandlerThread.class);
    private boolean mQuit = false;
    private CasioGB6900DeviceSupport mDeviceSupport;
    private final Object waitObject = new Object();
    //private CasioGATTServer mServer = null;

    private int TX_PERIOD = 60;

    private Calendar mTxTime = GregorianCalendar.getInstance();

    public CasioHandlerThread(GBDevice gbDevice, Context context, CasioGB6900DeviceSupport deviceSupport) {
        super(gbDevice, context);
        LOG.info("Initializing Casio Handler Thread");
        mQuit = false;
        //mServer = new CasioGATTServer(context, deviceSupport);
        mDeviceSupport = deviceSupport;
    }

    @Override
    public void run() {
        mQuit = false;

        /*
        if(!mServer.initialize()) {
            LOG.error("Error initializing CasioGATTServer. Has the context been set?");
            return;
        }
        */

        long waitTime = TX_PERIOD * 1000;
        while (!mQuit) {

            if (waitTime > 0) {
                synchronized (waitObject) {
                    try {
                        waitObject.wait(waitTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (mQuit) {
                break;
            }

            if (gbDevice.getState() == GBDevice.State.NOT_CONNECTED) {
                quit();
            }

            Calendar now = GregorianCalendar.getInstance();

            if (now.compareTo(mTxTime) > 0) {
                requestTxPowerLevel();
            }

            now = GregorianCalendar.getInstance();
            waitTime = mTxTime.getTimeInMillis() - now.getTimeInMillis();
        }

    }

    public void requestTxPowerLevel() {
        try {
            mDeviceSupport.readTxPowerLevel();

        } catch(Exception e) {

        }

        mTxTime = GregorianCalendar.getInstance();
        mTxTime.add(Calendar.SECOND, TX_PERIOD);
        synchronized (waitObject) {
            waitObject.notify();
        }
    }

    @Override
    public void quit() {
        LOG.info("CasioHandlerThread: Quit Handler Thread");
        mQuit = true;
        synchronized (waitObject) {
            waitObject.notify();
        }
    }

}
