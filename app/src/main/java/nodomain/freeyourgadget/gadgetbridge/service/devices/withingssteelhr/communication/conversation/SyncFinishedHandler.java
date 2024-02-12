/*  Copyright (C) 2023-2024 Frank Ertl

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.conversation;

import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.WithingsSteelHRDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.message.Message;
import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.message.WithingsMessageType;

public class SyncFinishedHandler extends AbstractResponseHandler {

    public SyncFinishedHandler(WithingsSteelHRDeviceSupport support) {
        super(support);
    }



    @Override
    public void handleResponse(Message response) {
        if (response.getType() == WithingsMessageType.SYNC_OK) {
            support.finishSync();
        }
    }
}
