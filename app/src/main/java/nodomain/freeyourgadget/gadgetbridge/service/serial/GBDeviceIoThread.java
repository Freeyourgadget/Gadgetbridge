/*  Copyright (C) 2015-2021 Andreas Shimokawa, Carsten Pfeiffer

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

import android.content.Context;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public abstract class GBDeviceIoThread extends Thread {
    protected final GBDevice gbDevice;
    private final Context context;

    public GBDeviceIoThread(GBDevice gbDevice, Context context) {
        this.gbDevice = gbDevice;
        this.context = context;
    }

    public Context getContext() {
        return context;
    }

    public GBDevice getDevice() {
        return gbDevice;
    }

    protected boolean connect() {
        return false;
    }

    @Override
    public void run() {
    }

    synchronized public void write(byte[] bytes) {
    }

    public void quit() {
    }
}