/*  Copyright (C) 2015-2018 Andreas Shimokawa, Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.service.btle.actions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

/**
 * A special action that is executed at the very front of the initialization
 * sequence (transaction). It will abort the entire initialization sequence
 * by returning false, when the device is already initialized.
 */
public class CheckInitializedAction extends AbortTransactionAction {
    private static final Logger LOG = LoggerFactory.getLogger(CheckInitializedAction.class);

    private final GBDevice device;

    public CheckInitializedAction(GBDevice gbDevice) {
        device = gbDevice;
    }

    @Override
    protected boolean shouldAbort() {
        boolean abort = device.isInitialized();
        if (abort) {
            LOG.info("Aborting device initialization, because already initialized: " + device);
        }
        return abort;
    }
}
