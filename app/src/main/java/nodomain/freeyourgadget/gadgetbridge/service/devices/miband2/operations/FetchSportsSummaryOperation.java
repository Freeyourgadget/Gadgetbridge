/*  Copyright (C) 2017 Andreas Shimokawa, Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.miband2.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.GregorianCalendar;

import nodomain.freeyourgadget.gadgetbridge.Logging;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband2.MiBand2Support;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

/**
 * An operation that fetches activity data. For every fetch, a new operation must
 * be created, i.e. an operation may not be reused for multiple fetches.
 */
public class FetchSportsSummaryOperation extends AbstractFetchOperation {
    private static final Logger LOG = LoggerFactory.getLogger(FetchSportsSummaryOperation.class);

//    private List<MiBandActivitySample> samples = new ArrayList<>(60*24); // 1day per default

    private byte lastPacketCounter;

    public FetchSportsSummaryOperation(MiBand2Support support) {
        super(support);
    }

    @Override
    protected void startFetching(TransactionBuilder builder) {
        GregorianCalendar sinceWhen = getLastSuccessfulSyncTime();
//        builder.write(characteristicFetch, BLETypeConversions.join(new byte[] {
//                MiBand2Service.COMMAND_ACTIVITY_DATA_START_DATE,
//                AmazfitBipService.COMMAND_ACTIVITY_DATA_TYPE_SPORTS_SUMMARIES},
//                getSupport().getTimeBytes(sinceWhen, TimeUnit.MINUTES)));
//        builder.add(new WaitAction(1000)); // TODO: actually wait for the success-reply
//        builder.notify(characteristicActivityData, true);
//        builder.write(characteristicFetch, new byte[] { MiBand2Service.COMMAND_FETCH_DATA });
    }

    @Override
    protected void handleActivityFetchFinish() {
        LOG.info("Fetching activity data has finished round " + fetchCount);
//        GregorianCalendar lastSyncTimestamp = saveSamples();
//        if (lastSyncTimestamp != null && needsAnotherFetch(lastSyncTimestamp)) {
//            try {
//                startFetching();
//                return;
//            } catch (IOException ex) {
//                LOG.error("Error starting another round of fetching activity data", ex);
//            }
//        }

        super.handleActivityFetchFinish();
    }

    @Override
    public boolean onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        LOG.warn("characteristic read: " + characteristic.getUuid() + ": " + Logging.formatBytes(characteristic.getValue()));
        return super.onCharacteristicRead(gatt, characteristic, status);
    }

    /**
     * Method to handle the incoming activity data.
     * There are two kind of messages we currently know:
     * - the first one is 11 bytes long and contains metadata (how many bytes to expect, when the data starts, etc.)
     * - the second one is 20 bytes long and contains the actual activity data
     * <p/>
     * The first message type is parsed by this method, for every other length of the value param, bufferActivityData is called.
     *
     * @param value
     */
    @Override
    protected void handleActivityNotif(byte[] value) {
        LOG.warn("sports data: " + Logging.formatBytes(value));

        if (!isOperationRunning()) {
            LOG.error("ignoring activity data notification because operation is not running. Data length: " + value.length);
            getSupport().logMessageContent(value);
            return;
        }

        if ((value.length % 4) == 1) {
            if ((byte) (lastPacketCounter + 1) == value[0] ) {
                lastPacketCounter++;
                bufferActivityData(value);
            } else {
                GB.toast("Error fetching activity data, invalid package counter: " + value[0], Toast.LENGTH_LONG, GB.ERROR);
                handleActivityFetchFinish();
                return;
            }
        } else {
            GB.toast("Error fetching activity data, unexpected package length: " + value.length, Toast.LENGTH_LONG, GB.ERROR);
            LOG.warn("Unexpected activity data: " + Logging.formatBytes(value));
        }
    }

    /**
     * Creates samples from the given 17-length array
     * @param value
     */
    @Override
    protected void bufferActivityData(byte[] value) {
        // TODO: implement
//        int len = value.length;
//
//        if (len % 4 != 1) {
//            throw new AssertionError("Unexpected activity array size: " + len);
//        }
//
//        for (int i = 1; i < len; i+=4) {
//        }
    }

    @Override
    protected String getLastSyncTimeKey() {
        return getDevice().getAddress() + "_" + "lastSportsSyncTimeMillis";
    }


    protected GregorianCalendar getLastSuccessfulSyncTime() {
        // FIXME: remove this!
        GregorianCalendar calendar = BLETypeConversions.createCalendar();
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        return calendar;
    }
}
