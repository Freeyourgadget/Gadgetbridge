/*  Copyright (C) 2022-2024 Daniel Dakhno, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos;

import android.content.Context;

import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.ZeppOsCoordinator;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsAlexaService;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public abstract class AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsAlexaService.class);

    private final ZeppOsSupport mSupport;
    private boolean encrypted;

    public AbstractZeppOsService(final ZeppOsSupport support, final boolean encryptedDefault) {
        this.mSupport = support;
        this.encrypted = encryptedDefault;
    }

    public abstract short getEndpoint();

    public final boolean isEncrypted() {
        return this.encrypted;
    }

    public final void setEncrypted(final boolean encrypted) {
        if (encrypted != this.encrypted) {
            LOG.warn("Replacing encrypted flag for {}, {} -> {}", this.getClass().getSimpleName(), this.encrypted, encrypted);
        }

        this.encrypted = encrypted;
    }

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
        // TODO implement a "quick initialize" that runs for the same firmware + Gb versions, since
        // we will already know the capabilities
    }

    protected ZeppOsSupport getSupport() {
        return mSupport;
    }

    protected ZeppOsCoordinator getCoordinator() {
        final DeviceCoordinator coordinator = getSupport().getDevice().getDeviceCoordinator();
        return (ZeppOsCoordinator) coordinator;
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

    @Nullable
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
