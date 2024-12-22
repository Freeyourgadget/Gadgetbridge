/*  Copyright (C) 2024 José Rebelo

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

import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.SWIM_STYLE;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.TIME_END;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.TIME_START;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_UNIX_EPOCH_SECONDS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryData;
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
        final ActivitySummaryData summaryData = new ActivitySummaryData();

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
            } else if (dataEntry.getKey().equals(SWIM_STYLE)) {
                String swimStyleName = "unknown";
                final float swimStyle = value.floatValue();

                if (swimStyle == 0) {
                    swimStyleName = "medley";
                } else if (swimStyle == 1) {
                    swimStyleName = "breaststroke";
                } else if (swimStyle == 2) {
                    swimStyleName = "freestyle";
                } else if (swimStyle == 3) {
                    swimStyleName = "backstroke";
                } else if (swimStyle == 4) {
                    swimStyleName = "butterfly";
                }

                summaryData.add(dataEntry.getKey(), swimStyleName);
            } else if (dataEntry.getKey().equals(XIAOMI_WORKOUT_TYPE)) {
                switch (value.intValue()) {
                    case 1:
                        summary.setActivityKind(ActivityKind.OUTDOOR_RUNNING.getCode());
                        break;
                    case 2:
                        summary.setActivityKind(ActivityKind.WALKING.getCode());
                        break;
                    case 4:
                        summary.setActivityKind(ActivityKind.TREKKING.getCode());
                        break;
                    case 5:
                        summary.setActivityKind(ActivityKind.TRAIL_RUN.getCode());
                        break;
                    case 6:
                        summary.setActivityKind(ActivityKind.OUTDOOR_CYCLING.getCode());
                        break;
                    case 7:   // indoor cycling   0x0007
                        summary.setActivityKind(ActivityKind.INDOOR_CYCLING.getCode());
                        break;
                    case 8:   // freestyle        0x0008
                        summary.setActivityKind(ActivityKind.FREE_TRAINING.getCode());
                        break;
                    case 12:  // yoga             0x000c
                        summary.setActivityKind(ActivityKind.YOGA.getCode());
                        break;
                    case 15:
                        summary.setActivityKind(ActivityKind.OUTDOOR_WALKING.getCode());
                        break;
                    case 16:  // HIIT             0x0010
                        summary.setActivityKind(ActivityKind.HIIT.getCode());
                        break;
                    case 201: // skateboard       0x00c9
                        summary.setActivityKind(ActivityKind.SKATEBOARDING.getCode());
                        break;
                    case 202: // roller skating   0x00ca
                        summary.setActivityKind(ActivityKind.ROLLER_SKATING.getCode());
                        break;
                    case 301: // stair climbing   0x012d
                        summary.setActivityKind(ActivityKind.STAIRS.getCode());
                        break;
                    case 303: // core training    0x012f
                        summary.setActivityKind(ActivityKind.CORE_TRAINING.getCode());
                        break;
                    case 304: // flexibility      0x0130
                        summary.setActivityKind(ActivityKind.FLEXIBILITY.getCode());
                        break;
                    case 305: // pilates          0x0131
                        summary.setActivityKind(ActivityKind.PILATES.getCode());
                        break;
                    case 307: // stretching       0x0133
                        summary.setActivityKind(ActivityKind.STRETCHING.getCode());
                        break;
                    case 308: // strength         0x0134
                        summary.setActivityKind(ActivityKind.STRENGTH_TRAINING.getCode());
                        break;
                    case 310: // aerobics         0x0136
                        summary.setActivityKind(ActivityKind.AEROBICS.getCode());
                        break;
                    case 399: // indoor-Fitness   0x018f
                        summary.setActivityKind(ActivityKind.INDOOR_FITNESS.getCode());
                        break;
                    case 499: // dancing          0x01f3
                        summary.setActivityKind(ActivityKind.DANCE.getCode());
                        break;
                    case 600: // Soccer           0x0258
                        summary.setActivityKind(ActivityKind.SOCCER.getCode());
                        break;
                    case 601: // basketball       0x0259
                        summary.setActivityKind(ActivityKind.BASKETBALL.getCode());
                        break;
                    case 607: // table tennis     0x025f
                        summary.setActivityKind(ActivityKind.TABLE_TENNIS.getCode());
                        break;
                    case 608: // badminton        0x0260
                        summary.setActivityKind(ActivityKind.BADMINTON.getCode());
                        break;
                    case 609: // tennis           0x0261
                        summary.setActivityKind(ActivityKind.TENNIS.getCode());
                        break;
                    case 614: // billiard          0x0266
                        summary.setActivityKind(ActivityKind.BILLIARDS.getCode());
                        break;
                    case 619: // golf             0x026b
                        summary.setActivityKind(ActivityKind.GOLF.getCode());
                        break;
                    case 700: // ice skating      0x02bc
                        summary.setActivityKind(ActivityKind.ICE_SKATING.getCode());
                        break;
                    case 708: // snowboard        0x02c4
                        summary.setActivityKind(ActivityKind.SNOWBOARDING.getCode());
                        break;
                    case 709: // skiing           0x02c5
                        summary.setActivityKind(ActivityKind.SKIING.getCode());
                        break;
                    case 808: // shuttlecock      0x0328
                        summary.setActivityKind(ActivityKind.SHUTTLECOCK.getCode());
                        break;
                    default:
                        summary.setActivityKind(ActivityKind.UNKNOWN.getCode());
                }
            } else {
                summaryData.add(dataEntry.getKey(), value.floatValue(), dataEntry.getUnit());
            }
        }

        summary.setSummaryData(summaryData.toString());
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
