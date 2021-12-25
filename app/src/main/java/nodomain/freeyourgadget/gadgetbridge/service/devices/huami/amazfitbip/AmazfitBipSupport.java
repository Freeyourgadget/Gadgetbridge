/*  Copyright (C) 2017-2021 Andreas Shimokawa, Carsten Pfeiffer, DerFetzer,
    Matthieu Baerts, Roi Greenberg

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitbip;

import android.content.Context;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiFWHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitbip.AmazfitBipFWHelper;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.FetchActivityOperation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.FetchSportsSummaryOperation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.HuamiFetchDebugLogsOperation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.NotificationStrategy;

public class AmazfitBipSupport extends HuamiSupport {

    private static final Logger LOG = LoggerFactory.getLogger(AmazfitBipSupport.class);

    public AmazfitBipSupport() {
        super(LOG);
    }

    @Override
    public NotificationStrategy getNotificationStrategy() {
        return new AmazfitBipTextNotificationStrategy(this);
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        super.sendNotificationNew(notificationSpec, false);
    }

    @Override
    public void onFindDevice(boolean start) {
        CallSpec callSpec = new CallSpec();
        callSpec.command = start ? CallSpec.CALL_INCOMING : CallSpec.CALL_END;
        callSpec.name = "Gadgetbridge";
        onSetCallState(callSpec);
    }

    @Override
    protected AmazfitBipSupport setDisplayItems(TransactionBuilder builder) {
        Map<String, Integer> keyPosMap = new LinkedHashMap<>();
        keyPosMap.put("status", 1);
        keyPosMap.put("activity", 2);
        keyPosMap.put("weather", 3);
        keyPosMap.put("alarm", 4);
        keyPosMap.put("timer", 5);
        keyPosMap.put("compass", 6);
        keyPosMap.put("settings", 7);
        keyPosMap.put("alipay", 8);

        setDisplayItemsOld(builder, false, R.array.pref_bip_display_items_default, keyPosMap);
        return this;
    }

    @Override
    protected AmazfitBipSupport setShortcuts(TransactionBuilder builder) {
        Map<String, Integer> keyPosMap = new LinkedHashMap<>();
        keyPosMap.put("alipay", 1);
        keyPosMap.put("weather", 2);

        setDisplayItemsOld(builder, true, R.array.pref_bip_shortcuts_default, keyPosMap);
        return this;
    }

    @Override
    public void onFetchRecordedData(int dataTypes) {
        try {
            // FIXME: currently only one data type supported, these are meant to be flags
            if (dataTypes == RecordedDataTypes.TYPE_ACTIVITY) {
                new FetchActivityOperation(this).perform();
            } else if (dataTypes == RecordedDataTypes.TYPE_GPS_TRACKS) {
                new FetchSportsSummaryOperation(this).perform();
            } else if (dataTypes == RecordedDataTypes.TYPE_DEBUGLOGS) {
                new HuamiFetchDebugLogsOperation(this).perform();
            } else {
                LOG.warn("fetching multiple data types at once is not supported yet");
            }
        } catch (IOException ex) {
            LOG.error("Unable to fetch recorded data types" + dataTypes, ex);
        }
    }

    @Override
    public void phase2Initialize(TransactionBuilder builder) {
        super.phase2Initialize(builder);
        LOG.info("phase2Initialize...");
        setLanguage(builder);
        requestGPSVersion(builder);
    }

    @Override
    public HuamiFWHelper createFWHelper(Uri uri, Context context) throws IOException {
        return new AmazfitBipFWHelper(uri, context);
    }
}
