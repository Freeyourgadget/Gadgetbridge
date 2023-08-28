/*  Copyright (C) 2023-2024 Johannes Krude

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.casio.gwb5600;

import java.util.UUID;
import java.io.IOException;

import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;

import nodomain.freeyourgadget.gadgetbridge.devices.casio.CasioConstants;
import nodomain.freeyourgadget.gadgetbridge.service.devices.casio.Casio2C2DSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.casio.gwb5600.InitOperation;

public class CasioGWB5600DeviceSupport extends Casio2C2DSupport {
    private static final Logger LOG = LoggerFactory.getLogger(CasioGWB5600DeviceSupport.class);

    public CasioGWB5600DeviceSupport() {
        super(LOG);
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    @Override
    public boolean connectFirstTime() {
    // remove this workaround in case Gadgetbridge fixes
    // https://codeberg.org/Freeyourgadget/Gadgetbridge/issues/3216
        setAutoReconnect(true);
        return connect();
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        // remove this workaround once Gadgetbridge does discovery on initial pairing
        if (getCharacteristic(CasioConstants.CASIO_READ_REQUEST_FOR_ALL_FEATURES_CHARACTERISTIC_UUID) == null ||
           getCharacteristic(CasioConstants.CASIO_ALL_FEATURES_CHARACTERISTIC_UUID) == null) {
            LOG.info("Reconnecting to discover characteristics");
            disconnect();
            connect();
            return builder;
        }
        try {
            new InitOperation(this, builder).perform();
        } catch (IOException e) {
            GB.toast(getContext(), "Initializing watch failed", Toast.LENGTH_SHORT, GB.ERROR, e);
        }

        return builder;
    }

}
