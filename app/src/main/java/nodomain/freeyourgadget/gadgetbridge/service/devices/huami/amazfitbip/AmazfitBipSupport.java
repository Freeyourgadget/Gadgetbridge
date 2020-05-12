/*  Copyright (C) 2017-2020 Andreas Shimokawa, Carsten Pfeiffer, DerFetzer,
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
import android.content.SharedPreferences;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiFWHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiService;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitbip.AmazfitBipFWHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitbip.AmazfitBipService;
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
        if (gbDevice.getFirmwareVersion() == null) {
            LOG.warn("Device not initialized yet, won't set menu items");
            return this;
        }

        SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress());
        Set<String> pages = prefs.getStringSet(HuamiConst.PREF_DISPLAY_ITEMS, new HashSet<>(Arrays.asList(getContext().getResources().getStringArray(R.array.pref_bip_display_items_default))));

        LOG.info("Setting display items to " + (pages == null ? "none" : pages));
        byte[] command = AmazfitBipService.COMMAND_CHANGE_SCREENS.clone();

        boolean shortcut_weather = false;
        boolean shortcut_alipay = false;

        if (pages != null) {
            if (pages.contains("status")) {
                command[1] |= 0x02;
            }
            if (pages.contains("activity")) {
                command[1] |= 0x04;
            }
            if (pages.contains("weather")) {
                command[1] |= 0x08;
            }
            if (pages.contains("alarm")) {
                command[1] |= 0x10;
            }
            if (pages.contains("timer")) {
                command[1] |= 0x20;
            }
            if (pages.contains("compass")) {
                command[1] |= 0x40;
            }
            if (pages.contains("settings")) {
                command[1] |= 0x80;
            }
            if (pages.contains("alipay")) {
                command[2] |= 0x01;
            }
            if (pages.contains("shortcut_weather")) {
                shortcut_weather = true;
            }
            if (pages.contains("shortcut_alipay")) {
                shortcut_alipay = true;
            }
            builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION), command);
            setShortcuts(builder, shortcut_weather, shortcut_alipay);
        }

        return this;
    }

    private void setShortcuts(TransactionBuilder builder, boolean weather, boolean alipay) {
        LOG.info("Setting shortcuts: weather=" + weather + " alipay=" + alipay);

        // Basically a hack to put weather first always, if alipay is the only enabled one
        // there are actually two alipays set but the second one disabled.... :P
        byte[] command = new byte[]{0x10,
                (byte) ((alipay || weather) ? 0x80 : 0x00), (byte) (weather ? 0x02 : 0x01),
                (byte) ((alipay && weather) ? 0x81 : 0x01), 0x01,
        };

        builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION), command);
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
