/*  Copyright (C) 2023 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services;

import android.annotation.SuppressLint;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class ZeppOsServicesService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsServicesService.class);

    private static final short ENDPOINT = 0x0000;

    public static final byte CMD_GET_LIST = 0x03;
    public static final byte CMD_RET_LIST = 0x04;

    public ZeppOsServicesService(final Huami2021Support support) {
        super(support);
    }

    @Override
    public short getEndpoint() {
        return ENDPOINT;
    }

    @Override
    public boolean isEncrypted() {
        return false;
    }

    @Override
    public void handlePayload(final byte[] payload) {
        switch (payload[0]) {
            case CMD_RET_LIST:
                handleSupportedServices(payload);
                break;
            default:
                LOG.warn("Unexpected services payload byte {}", String.format("0x%02x", payload[0]));
        }
    }

    @Override
    public void initialize(final TransactionBuilder builder) {
        //requestServices(builder);
    }

    public void requestServices(final TransactionBuilder builder) {
        write(builder, CMD_GET_LIST);
    }

    private void handleSupportedServices(final byte[] payload) {
        final ByteBuffer buf = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);
        buf.get(); // discard first byte
        final short numServices = buf.getShort();

        LOG.info("Number of services: {}", numServices);

        for (int i = 0; i < numServices; i++) {
            final short endpoint = buf.getShort();
            final byte encryptedByte = buf.get();
            final Boolean encrypted = booleanFromByte(encryptedByte);

            LOG.debug("Service: endpoint={} encrypted={}", String.format("%04x", endpoint), encrypted);

            // TODO use this to initialize the services supported by the device
        }

        final int remainingBytes = buf.limit() - buf.position();
        if (remainingBytes != 0) {
            LOG.warn("There are {} bytes remaining in the buffer", remainingBytes);
        }
    }
}
