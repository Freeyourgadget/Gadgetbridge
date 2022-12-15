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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.net.Uri;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiFWHelper;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.WriteAction;

public class HuamiSupportTest {
    @Test
    public void testSetCurrentTimeWithService() {
        final TransactionBuilder testTransactionBuilder = new TransactionBuilder("test");
        final HuamiSupport huamiSupport = createSupport();

        huamiSupport.setCurrentTimeWithService(testTransactionBuilder);
        final WriteAction action = (WriteAction) testTransactionBuilder.getTransaction().getActions().get(0);

        Assert.assertArrayEquals(new byte[]{-26, 7, 12, 15, 20, 38, 53, 4, 0, 0, 4}, action.getValue());
    }

    private HuamiSupport createSupport() {
        return new HuamiSupport() {
            @Override
            public BluetoothGattCharacteristic getCharacteristic(final UUID uuid) {
                return new BluetoothGattCharacteristic(null, 0, 0);
            }

            @Override
            public HuamiFWHelper createFWHelper(final Uri uri, final Context context) throws IOException {
                return null;
            }

            @Override
            public Calendar createCalendar() {
                // 2022-12-15 20:38:53 GMT+1
                final Instant instant = Instant.ofEpochMilli(1671133133000L);
                final ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, ZoneId.of("Europe/Paris"));
                return GregorianCalendar.from(zdt);
            }
        };
    }
}
