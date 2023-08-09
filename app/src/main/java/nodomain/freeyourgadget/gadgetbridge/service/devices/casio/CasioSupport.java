/*  Copyright (C) 2021-2023 Andreas Böhler, Johannes Krude

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.casio;

import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.threeten.bp.ZonedDateTime;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiSupport;
import nodomain.freeyourgadget.gadgetbridge.devices.casio.CasioConstants;

public abstract class CasioSupport extends AbstractBTLEDeviceSupport {

    protected boolean mFirstConnect = false;
    private static final Logger LOG = LoggerFactory.getLogger(HuamiSupport.class);

    public CasioSupport(Logger logger) {
        super(logger);
        addSupportedService(CasioConstants.WATCH_FEATURES_SERVICE_UUID);
    }

    @Override
    public boolean connectFirstTime() {
        mFirstConnect = true;
        return connect();
    }

    static protected byte[] prepareCurrentTime(ZonedDateTime time) {
        // somehow everyone uses reason=1 for Casio watches
        return BLETypeConversions.toCurrentTime(time, 1);
    }

    public void setInitialized() {
        mFirstConnect = false;
        gbDevice.setState(GBDevice.State.INITIALIZED);
        gbDevice.sendDeviceUpdateIntent(getContext());
    }
}
