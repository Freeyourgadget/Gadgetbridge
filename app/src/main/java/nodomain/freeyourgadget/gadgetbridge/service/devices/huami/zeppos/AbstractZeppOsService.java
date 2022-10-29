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

import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Support;

public abstract class AbstractZeppOsService {
    private final Huami2021Support mSupport;

    public AbstractZeppOsService(final Huami2021Support support) {
        this.mSupport = support;
    }

    public abstract short getEndpoint();
    public abstract boolean isEncrypted();
    public abstract void handlePayload(final byte[] payload);

    protected Huami2021Support getSupport() {
        return mSupport;
    }

    protected void write(final String taskName, final byte[] data) {
        this.mSupport.writeToChunked2021(taskName, getEndpoint(), data, isEncrypted());
    }

    protected void write(final TransactionBuilder builder, final byte[] data) {
        this.mSupport.writeToChunked2021(builder, getEndpoint(), data, isEncrypted());
    }
}
