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
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.impl;

import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.entities.XiaomiActivitySample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.XiaomiActivityFileId;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.XiaomiActivityParser;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class DailyDetailsParser extends XiaomiActivityParser {
    private static final Logger LOG = LoggerFactory.getLogger(DailyDetailsParser.class);

    @Override
    public boolean parse(final XiaomiSupport support, final XiaomiActivityFileId fileId, final byte[] bytes) {
        final int version = fileId.getVersion();
        final int headerSize;
        switch (version) {
            case 1:
            case 2:
                headerSize = 4;
                break;
            case 3:
                headerSize = 5;
                break;
            case 4:
                headerSize = 6;
                break;
            default:
                LOG.warn("Unable to parse daily details version {}", fileId.getVersion());
                return false;
        }

        final ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        buf.limit(buf.limit() - 4); // discard crc at the end
        buf.get(new byte[7]); // skip fileId bytes
        final byte fileIdPadding = buf.get();
        if (fileIdPadding != 0) {
            LOG.warn("Expected 0 padding after fileId, got {} - parsing might fail", fileIdPadding);
        }

        final byte[] header = new byte[headerSize];
        buf.get(header);

        LOG.debug("Daily Details Header: {}", GB.hexdump(header));

        final XiaomiComplexActivityParser complexParser = new XiaomiComplexActivityParser(header, buf);

        final Calendar timestamp = Calendar.getInstance();
        timestamp.setTime(fileId.getTimestamp());

        final List<XiaomiActivitySample> samples = new ArrayList<>();
        while (buf.position() < buf.limit()) {
            complexParser.reset();

            final XiaomiActivitySample sample = new XiaomiActivitySample();
            sample.setTimestamp((int) (timestamp.getTimeInMillis() / 1000));

            int includeExtraEntry = 0;
            if (complexParser.nextGroup(16)) {
                // TODO what's the first bit?

                if (complexParser.hasSecond()) {
                    includeExtraEntry = complexParser.get(1, 1);
                }
                if (complexParser.hasThird()) {
                    sample.setSteps(complexParser.get(2, 14));
                }
            }

            if (complexParser.nextGroup(8)) {
                // TODO activity type?
                if (complexParser.hasSecond()) {
                    final int calories = complexParser.get(2, 6);
                }
            }

            if (complexParser.nextGroup(8)) {
                // TODO
            }

            if (complexParser.nextGroup(16)) {
                // TODO distance
            }

            if (complexParser.nextGroup(8)) {
                if (complexParser.hasFirst()) {
                    // hr, 8 bits
                    sample.setHeartRate(complexParser.get(0, 8));
                }
            }

            if (complexParser.nextGroup(8)) {
                if (complexParser.hasFirst()) {
                    // energy, 8 bits
                }
            }

            if (complexParser.nextGroup(16)) {
                // TODO
            }

            if (version >= 3) {
                if (complexParser.nextGroup(8)) {
                    if (complexParser.hasFirst()) {
                        // spo2, 8 bits
                        sample.setSpo2(complexParser.get(0, 8));
                    }
                }
                if (complexParser.nextGroup(8)) {
                    if (complexParser.hasFirst()) {
                        // stress, 8 bits
                        final int stress = complexParser.get(0, 8);
                        if (stress != 255) {
                            sample.setStress(stress);
                        }
                    }
                }
            }

            if (includeExtraEntry == 1) {
                if (complexParser.nextGroup(8)) {
                    // TODO
                }
            }

            if (version >= 4) {
                // TODO: light value (short)
                complexParser.nextGroup(16);

                // TODO: body momentum (short)
                complexParser.nextGroup(16);
            }

            samples.add(sample);

            timestamp.add(Calendar.MINUTE, 1);
        }

        // save all the samples that we got
        try (DBHandler handler = GBApplication.acquireDB()) {
            final DaoSession session = handler.getDaoSession();

            final GBDevice gbDevice = support.getDevice();
            final DeviceCoordinator coordinator = gbDevice.getDeviceCoordinator();
            final SampleProvider<XiaomiActivitySample> sampleProvider = (SampleProvider<XiaomiActivitySample>) coordinator.getSampleProvider(gbDevice, session);
            final Device device = DBHelper.getDevice(gbDevice, session);
            final User user = DBHelper.getUser(session);

            for (final XiaomiActivitySample sample : samples) {
                sample.setDevice(device);
                sample.setUser(user);
                sample.setProvider(sampleProvider);
            }
            sampleProvider.addGBActivitySamples(samples.toArray(new XiaomiActivitySample[0]));

            return true;
        } catch (final Exception e) {
            GB.toast(support.getContext(), "Error saving activity samples", Toast.LENGTH_LONG, GB.ERROR);
            LOG.error("Error saving activity samples", e);
            return false;
        }
    }
}
