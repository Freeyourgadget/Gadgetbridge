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
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.impl;

import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.TIME_END;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.TIME_START;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_UNIX_EPOCH_SECONDS;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class XiaomiSimpleActivityParser {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiSimpleActivityParser.class);

    public static final String XIAOMI_WORKOUT_TYPE = "xiaomiWorkoutType";

    private final int headerSize;
    private final List<XiaomiSimpleDataEntry> dataEntries;

    public XiaomiSimpleActivityParser(final int headerSize, final List<XiaomiSimpleDataEntry> dataEntries) {
        this.headerSize = headerSize;
        this.dataEntries = dataEntries;
    }

    public void parse(final BaseActivitySummary summary, final ByteBuffer buf) {
        final JSONObject summaryData = new JSONObject();

        final byte[] header = new byte[headerSize];
        buf.get(header);

        LOG.debug("Header: {}", GB.hexdump(header));

        for (int i = 0; i < dataEntries.size(); i++) {
            final XiaomiSimpleDataEntry dataEntry = dataEntries.get(i);

            final Number value = dataEntry.get(buf);
            if (value == null) {
                LOG.debug("Skipping unknown field {}", i);
                continue;
            }

            // Each bit in the header marks whether the data is valid or not, in order of the fields
            final boolean validData = (header[i / 8] & (1 << (7 - (i % 8)))) != 0;
            // FIXME: We can't use the header before identifying the correct field lenggths for unknown fields
            // or parsing gets out of sync with the header and we will potentially ignore valid data
            //if (!validData) {
            //    LOG.debug("Ignoring non-valid data {}", i);
            //    continue;
            //}

            if (dataEntry.getKey().equals(TIME_END)) {
                if (dataEntry.getUnit().equals(UNIT_UNIX_EPOCH_SECONDS)) {
                    summary.setEndTime(new Date(value.intValue() * 1000L));
                } else {
                    throw new IllegalArgumentException("endTime should be an unix epoch");
                }
            } else if (dataEntry.getKey().equals(TIME_START)) {
                // ignored
            } else if (dataEntry.getKey().equals(XIAOMI_WORKOUT_TYPE)) {
                // TODO use XiaomiWorkoutType
                switch (value.intValue()) {
                    case 2:
                        summary.setActivityKind(ActivityKind.TYPE_WALKING);
                        break;
                    case 6:
                        summary.setActivityKind(ActivityKind.TYPE_CYCLING);
                        break;
                    default:
                        summary.setActivityKind(ActivityKind.TYPE_UNKNOWN);
                }
            } else {
                addSummaryData(summaryData, dataEntry.getKey(), value.floatValue(), dataEntry.getUnit());
            }
        }

        summary.setSummaryData(summaryData.toString());
    }

    protected void addSummaryData(final JSONObject summaryData, final String key, final float value, final String unit) {
        if (value > 0) {
            try {
                final JSONObject innerData = new JSONObject();
                innerData.put("value", value);
                innerData.put("unit", unit);
                summaryData.put(key, innerData);
            } catch (final JSONException ignore) {
            }
        }
    }

    protected void addSummaryData(final JSONObject summaryData, final String key, final String value) {
        if (key != null && !key.equals("") && value != null && !value.equals("")) {
            try {
                final JSONObject innerData = new JSONObject();
                innerData.put("value", value);
                innerData.put("unit", "string");
                summaryData.put(key, innerData);
            } catch (final JSONException ignore) {
            }
        }
    }

    public static class Builder {
        private int headerSize;
        private List<XiaomiSimpleDataEntry> dataEntries = new ArrayList<>();

        public Builder setHeaderSize(final int headerSize) {
            this.headerSize = headerSize;
            return this;
        }

        public Builder addByte(final String key, final String unit) {
            dataEntries.add(new XiaomiSimpleDataEntry(key, unit, buf -> buf.get() & 0xff));
            return this;
        }

        public Builder addShort(final String key, final String unit) {
            dataEntries.add(new XiaomiSimpleDataEntry(key, unit, ByteBuffer::getShort));
            return this;
        }

        public Builder addInt(final String key, final String unit) {
            dataEntries.add(new XiaomiSimpleDataEntry(key, unit, ByteBuffer::getInt));
            return this;
        }

        public Builder addFloat(final String key, final String unit) {
            dataEntries.add(new XiaomiSimpleDataEntry(key, unit, ByteBuffer::getFloat));
            return this;
        }

        public Builder addUnknown(final int sizeBytes) {
            dataEntries.add(new XiaomiSimpleDataEntry(null, null, buf -> {
                for (int i = 0; i < sizeBytes; i++) {
                    buf.get();
                }
                return null;
            }));
            return this;
        }

        public XiaomiSimpleActivityParser build() {
            return new XiaomiSimpleActivityParser(headerSize, dataEntries);
        }
    }
}
