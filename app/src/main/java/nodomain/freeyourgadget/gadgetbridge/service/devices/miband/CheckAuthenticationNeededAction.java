/*  Copyright (C) 2016-2019 Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.miband;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.AbortTransactionAction;

public class CheckAuthenticationNeededAction extends AbortTransactionAction {
    private final GBDevice mDevice;

    public CheckAuthenticationNeededAction(GBDevice device) {
        super();
        mDevice = device;
    }

    @Override
    protected boolean shouldAbort() {
        // the state is set in MiBandSupport.handleNotificationNotif()
        switch (mDevice.getState()) {
            case AUTHENTICATION_REQUIRED: // fall through
            case AUTHENTICATING:
                return true; // abort the whole thing
            default:
                return false;
        }
    }
}
