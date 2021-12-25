/*  Copyright (C) 2021 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.deviceevents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.Request;

public class SonyHeadphonesEnqueueRequestEvent extends GBDeviceEvent {
    private final List<Request> requests = new ArrayList<>();

    public SonyHeadphonesEnqueueRequestEvent(final Request request) {
        this.requests.add(request);
    }

    public SonyHeadphonesEnqueueRequestEvent(final Collection<Request> requests) {
        this.requests.addAll(requests);
    }

    public List<Request> getRequests() {
        return this.requests;
    }
}
