/*  Copyright (C) 2023-2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;

public class ZeppOsServicesService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsServicesService.class);

    private static final short ENDPOINT = 0x0000;

    public static final byte CMD_GET_LIST = 0x03;
    public static final byte CMD_RET_LIST = 0x04;

    public ZeppOsServicesService(final Huami2021Support support) {
        super(support, false);
    }

    @Override
    public short getEndpoint() {
        return ENDPOINT;
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

            final AbstractZeppOsService service = getSupport().getService(endpoint);

            LOG.debug("Service: endpoint={} encrypted={} known={}", String.format("%04x", endpoint), encrypted, service != null);

            if (service != null && encrypted != null) {
                service.setEncrypted(encrypted);
            }

            getSupport().addSupportedService(endpoint, encrypted != null && encrypted);
        }

        getSupport().initializeServices();

        final int remainingBytes = buf.limit() - buf.position();
        if (remainingBytes != 0) {
            LOG.warn("There are {} bytes remaining in the buffer", remainingBytes);
        }
    }
}
