/*  Copyright (C) 2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.test;

import android.os.Handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.devices.test.TestDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.test.TestFeature;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.AbstractDeviceSupport;

public class TestDeviceSupport extends AbstractDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(TestDeviceSupport.class);

    private final Handler handler = new Handler();

    @Override
    public boolean connect() {
        LOG.info("Connecting");

        getDevice().setState(GBDevice.State.CONNECTING);
        getDevice().sendDeviceUpdateIntent(getContext());

        handler.postDelayed(() -> {
            LOG.info("Initialized");

            getDevice().setFirmwareVersion("1.0.0");
            getDevice().setFirmwareVersion2("N/A");
            getDevice().setModel("0.1.7");

            if (getCoordinator().supportsLedColor()) {
                getDevice().setExtraInfo("led_color", 0xff3061e3);
            } else {
                getDevice().setExtraInfo("led_color", null);
            }

            if (getCoordinator().supports(getDevice(), TestFeature.FM_FREQUENCY)) {
                getDevice().setExtraInfo("fm_frequency", 90.2f);
            } else {
                getDevice().setExtraInfo("fm_frequency", null);
            }

            // TODO battery percentages
            // TODO hr measurements
            // TODO app list
            // TODO screenshots

            getDevice().setState(GBDevice.State.INITIALIZED);
            getDevice().sendDeviceUpdateIntent(getContext());
        }, 1000);

        return true;
    }

    @Override
    public void dispose() {
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    protected TestDeviceCoordinator getCoordinator() {
        return (TestDeviceCoordinator) getDevice().getDeviceCoordinator();
    }
}
