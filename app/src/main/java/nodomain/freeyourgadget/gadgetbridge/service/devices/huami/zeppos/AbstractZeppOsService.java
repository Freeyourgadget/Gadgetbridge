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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos;

import android.content.Context;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.Huami2021Coordinator;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Support;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public abstract class AbstractZeppOsService {
    private final Huami2021Support mSupport;

    public AbstractZeppOsService(final Huami2021Support support) {
        this.mSupport = support;
    }

    public abstract short getEndpoint();

    public abstract boolean isEncrypted();

    public abstract void handlePayload(final byte[] payload);

    /**
     * Handle a preference change.
     * @param config the preference key
     * @param prefs the device preferences
     * @return true if the preference was handled, false otherwise
     */
    public boolean onSendConfiguration(final String config, final Prefs prefs) {
        return false;
    }

    public void initialize(final TransactionBuilder builder) {
        // Do nothing by default
    }

    protected Huami2021Support getSupport() {
        return mSupport;
    }

    protected Huami2021Coordinator getCoordinator() {
        final DeviceCoordinator coordinator = getSupport().getDevice().getDeviceCoordinator();
        return (Huami2021Coordinator) coordinator;
    }

    protected Prefs getDevicePrefs() {
        return new Prefs(GBApplication.getDeviceSpecificSharedPrefs(getSupport().getDevice().getAddress()));
    }

    protected void write(final String taskName, final byte b) {
        this.write(taskName, new byte[]{b});
    }

    protected void write(final String taskName, final byte[] data) {
        this.mSupport.writeToChunked2021(taskName, getEndpoint(), data, isEncrypted());
    }

    protected void write(final TransactionBuilder builder, final byte b) {
        this.write(builder, new byte[]{b});
    }

    protected void write(final TransactionBuilder builder, final byte[] data) {
        this.mSupport.writeToChunked2021(builder, getEndpoint(), data, isEncrypted());
    }

    protected void evaluateGBDeviceEvent(final GBDeviceEvent event) {
        getSupport().evaluateGBDeviceEvent(event);
    }

    protected Context getContext() {
        return getSupport().getContext();
    }

    protected static Boolean booleanFromByte(final byte b) {
        switch (b) {
            case 0x00:
                return false;
            case 0x01:
                return true;
            default:
        }

        return null;
    }

    protected byte bool(final boolean bool) {
        return (byte) (bool ? 0x01 : 0x00);
    }
}
