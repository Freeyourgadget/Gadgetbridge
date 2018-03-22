/*  Copyright (C) 2015-2018 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti

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

import android.bluetooth.BluetoothGatt;
import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class SetProgressAction extends PlainAction {
    private static final Logger LOG = LoggerFactory.getLogger(SetProgressAction.class);

    private final String text;
    private final boolean ongoing;
    private final int percentage;
    private final Context context;

    /**
     * When run, will update the progress notification.
     *
     * @param text
     * @param ongoing
     * @param percentage
     * @param context
     */

    public SetProgressAction(String text, boolean ongoing, int percentage, Context context) {
        this.text = text;
        this.ongoing = ongoing;
        this.percentage = percentage;
        this.context = context;
    }

    @Override
    public boolean run(BluetoothGatt gatt) {
        LOG.info(toString());
        GB.updateInstallNotification(this.text, this.ongoing, this.percentage, this.context);
        return true;
    }

    @Override
    public String toString() {
        return getCreationTime() + ": " + getClass().getSimpleName() + ": " + text + "; " + percentage + "%";
    }
}
