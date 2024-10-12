/*  Copyright (C) 2024 Martin.JM

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
package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;

import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiFileDownloadManager;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class HuaweiTruSleepParser {
    private static final Logger LOG = LoggerFactory.getLogger(HuaweiTruSleepParser.class);
    public static final int TAG_ACC = 1;
    public static final int TAG_PPG = 2;

    public static class TruSleepStatus {
        public final int startTime;
        public final int endTime;

        public TruSleepStatus(int startTime, int endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            TruSleepStatus that = (TruSleepStatus) o;
            return startTime == that.startTime && endTime == that.endTime;
        }

        @Override
        public String toString() {
            return "TruSleepStatus{" +
                    "endTime=" + endTime +
                    ", startTime=" + startTime +
                    '}';
        }
    }
    public static class TruSleepDataAcc {
        public final int startTime;
        public final short flags;

        /*
            Accelerometer (ACC):
             Accelerometer data is sampled every 60 seconds, 24h a day and provides activity data on 3 axis.
             Each axis activity is represented as byte, where 0x00 is no activity and 0xff is high activity.
             The 3rd axis is ignored as it's always high (due to gravity??).
         */
        public TruSleepDataAcc(int startTime, short flags)  {
            this.startTime = startTime;
            this.flags = flags;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            TruSleepDataAcc that = (TruSleepDataAcc) o;
            if (startTime != that.startTime)
                return false;
            return flags == that.flags;
        }

        @Override
        public String toString() {
            return "TruSleepDataAcc{" +
                    "startTime=" + startTime +
                    ", flags=" + flags +
                    '}';
        }
    }

    /*
        Photoplethysmogram (PPG):
         Every sample has a millisecond timestamp.
         At night data is sampled every second, but at day it's sampled less often (about every 5 minutes).
    */
    public static class TruSleepDataPpg {
        public final long startTime;
        public final short flags;

        TruSleepDataPpg(long startTime, short flags) {
            this.startTime = startTime;
            this.flags = flags;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            TruSleepDataPpg that = (TruSleepDataPpg) o;
            if (startTime != that.startTime)
                return false;
            return flags == that.flags;
        }

        @Override
        public String toString() {
            return "TruSleepDataPpg{" +
                    "startTime=" + startTime +
                    ", flags=" + flags +
                    '}';
        }
    }
    public static class TruSleepData {
        public ArrayList<TruSleepDataAcc> dataACCs;
        public ArrayList<TruSleepDataPpg> dataPPGs;

        public TruSleepData() {
            this.dataACCs = new ArrayList<>();
            this.dataPPGs = new ArrayList<>();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            TruSleepData that = (TruSleepData) o;
            if (!dataACCs.equals(that.dataACCs))
                return false;
            return dataPPGs.equals(that.dataPPGs);
        }

        @Override
        public String toString() {
            return "TruSleepData{" +
                    "dataPPGs=" + dataPPGs.toString() +
                    ", dataACCs=" + dataACCs.toString() +
                    '}';
        }

        private static final byte TAG_COMPRESSION_RAW = (byte)0xaa;
        private static final byte TAG_COMPRESSION_COMP = (byte)0xbb;
        private static final byte TAG_COMPRESSION_RESTART = (byte)0xff;

        public void decodeAcc(byte[] data) throws IllegalArgumentException {
            /*
                Format:
                 - Timestamp (4 byte)
                 - Flags (3 bytes)
             */
            ByteBuffer buffer = ByteBuffer.wrap(data);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            if (buffer.remaining() < 4)
                throw new IllegalArgumentException("Timestamp is missing");

            int startTime = buffer.getInt();
            if (buffer.remaining() < 2)
                throw new IllegalArgumentException("Flags are missing");

            short flags = buffer.getShort();
            dataACCs.add(new TruSleepDataAcc(startTime, flags));
        }

        public void decodePpg(byte[] data) throws IllegalArgumentException, IllegalStateException {
            /*
                Format:
                 - Timestamp (4 byte)
                 - Number of UINT16 (1 byte)
                 - Compression tag (1 byte)
                 - Compressed data
                 - Compression tag (1 byte)
                 - Compressed data
             */
            ByteBuffer buffer = ByteBuffer.wrap(data);
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            if (buffer.remaining() < 4)
                throw new IllegalArgumentException("Timestamp is missing");

            int startTime = buffer.getInt();

            if (buffer.remaining() < 1)
                throw new IllegalArgumentException("Count of short is missing");

            byte countShort = buffer.get();

            ArrayList<Short> peak = decodePeak(countShort, buffer);
            ArrayList<Short> amp = decodeAmp(countShort, buffer);

            // Sanity check
            if (peak.size() != countShort || amp.size() != countShort)
                throw new IllegalStateException("Decoded arrays have different length");

            for (int i = 0; i < countShort; i++) {
                dataPPGs.add(new TruSleepDataPpg((long)startTime * 1000 + peak.get(i) * 10, amp.get(i)));
            }
            LOG.debug("Buffer remaining {}", buffer.remaining());
        }

        private ArrayList<Short> decodePeak(byte countShort, ByteBuffer buffer) throws IllegalArgumentException {
            if (countShort == 0)
                throw new IllegalArgumentException("Number of short to generate is invalid");

            if (buffer.remaining() < 1)
                throw new IllegalArgumentException("Compression tag is missing");

            byte tag = buffer.get();

            ArrayList<Short> al = new ArrayList<Short>();
            if (tag == TAG_COMPRESSION_RAW) {
                if (buffer.remaining() < countShort * 2)
                    throw new IllegalArgumentException("Not enough elements in buffer");

                while (countShort > 0) {
                    al.add(buffer.getShort());
                    countShort--;
                }
            } else if (tag == TAG_COMPRESSION_COMP) {
                short working = buffer.getShort();
                al.add(working);
                countShort--;

                while (countShort > 0) {
                    byte c = buffer.get();
                    if (c == TAG_COMPRESSION_RESTART) {
                        if (buffer.remaining() < 2)
                            throw new IllegalArgumentException("Not enough elements in buffer");
                        working = buffer.getShort();
                    } else {
                        working += c;
                    }
                    al.add(working);
                    countShort--;
                }
            } else {
                throw new IllegalArgumentException("Compression " + String.format("%02x", tag) + " is unsupported");
            }
            return al;
        }

        public ArrayList<Short> decodeAmp(byte countShort, ByteBuffer buffer) throws IllegalArgumentException {
            if (countShort == 0)
                throw new IllegalArgumentException("Number of short to generate is invalid");

            if (buffer.remaining() < 1)
                throw new IllegalArgumentException("Compression tag is missing");

            byte tag = buffer.get();

            ArrayList<Short> al = new ArrayList<Short>();
            if (tag == TAG_COMPRESSION_RAW) {
                if (buffer.remaining() < countShort * 2)
                    throw new IllegalArgumentException("Not enough elements in buffer");

                while (countShort > 0) {
                    al.add(buffer.getShort());
                    countShort--;
                }
            } else if (tag == TAG_COMPRESSION_COMP) {
                if (buffer.remaining() < 2)
                    throw new IllegalArgumentException("Offset buffer is missing");

                short offset = buffer.getShort();
                short working = 0;
                while (buffer.remaining() > 0) {
                    byte c = buffer.get();
                    if (c == TAG_COMPRESSION_RESTART) {
                        if (buffer.remaining() < 2)
                            throw new IllegalArgumentException("Raw buffer is missing");
                        working = (short)(offset + buffer.getShort());
                    } else {
                        working = (short)(offset + c);
                    }
                    al.add(working);
                }
            } else {
                throw new IllegalArgumentException("Compression " + String.format("%02x", tag) + " is unsupported");
            }
            return al;
        }

        public void parsePpgData(byte[] data) {
            LOG.debug("Decoding PPG: len= {}, data = {}", data.length, GB.hexdump(data));

            try {
                decodePpg(data);
            } catch (Exception e) {
                LOG.error("Failed to parse TrueSleep PPG data: {}", e.toString());
            }
        }
        public void parseAccData(byte[] data) {
            LOG.debug("Decoding ACC: len= {}, data = {}", data.length, GB.hexdump(data));

            try {
                decodeAcc(data);
            } catch (Exception e) {
                LOG.error("Failed to parse TrueSleep ACC data: {}", e.toString());
            }
        }
    }

    public static TruSleepData parseData(byte[] data) {
        /*
            Format:
            - Zero padded
            - ACC data interleaved with PPG data
            - PPG data is compressed and has variable length
         */
        TruSleepData sleepData = new TruSleepData();
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        byte c;
        while (buffer.remaining() > 0) {
            c = buffer.get();

            // Skip over padding
            if (c != TAG_ACC && c != TAG_PPG)
                continue;

            // The tag identifies the data packet
            if (c == TAG_ACC) {
                if (buffer.remaining() < 1)
                    break;

                // ACC has 1 byte length
                byte length = buffer.get();
                if (buffer.remaining() < length)
                    break;

                byte[] accData = new byte[length];
                buffer.get(accData);

                sleepData.parseAccData(accData);
            } else if (c == TAG_PPG) {
                if (buffer.remaining() < 2)
                    break;

                // PPG has 2 byte length
                short length = buffer.getShort();
                if (buffer.remaining() < length)
                    break;

                byte[] ppgData = new byte[length];
                buffer.get(ppgData);

                sleepData.parsePpgData(ppgData);
            }
        }

        return sleepData;
    }
    public static TruSleepStatus[] parseState(byte[] stateData) {
        /*
            Format:
             - Start time (int)
             - End time (int)
             - Unknown (short)
             - Unknown (byte)
             - Padding (5 bytes)
            Could be multiple available
         */
        ByteBuffer buffer = ByteBuffer.wrap(stateData);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        TruSleepStatus[] retv = new TruSleepStatus[buffer.remaining() / 0x10];
        int c = 0;
        while (stateData.length - buffer.position() >= 0x10) {
            int startTime = buffer.getInt();
            int endTime = buffer.getInt();

            // Throw away for now because we don't know what it means, and we don't think we can implement this soon
            buffer.get(); buffer.get(); buffer.get();
            buffer.get(); buffer.get(); buffer.get(); buffer.get(); buffer.get();

            retv[c++] = new TruSleepStatus(startTime, endTime);
        }
        return retv;
    }

    private static void analyzeOneTimeframe(HuaweiSupportProvider provider, TruSleepStatus status, ArrayList<TruSleepDataAcc> acc, ArrayList<TruSleepDataPpg> ppg) {
        java.util.Date rangeStartTime = new java.util.Date((long)status.startTime*1000);
        java.util.Date rangeEndTime = new java.util.Date((long)status.endTime*1000);
        LOG.debug("FIXME: Analyse sleep range {} - {} not implemented", rangeStartTime, rangeEndTime);
        //FIXME: Sleep analysis algorithm implemented here
        for (HuaweiTruSleepParser.TruSleepDataAcc i : acc) {
            java.util.Date startTime = new java.util.Date((long)i.startTime*1000);
            LOG.debug("TruSleepDataAcc {} : {}", startTime, String.format("%04x", i.flags));
        }
        for (HuaweiTruSleepParser.TruSleepDataPpg i : ppg) {
            java.util.Date startTime = new java.util.Date((long) i.startTime);
            LOG.debug("TruSleepDataPpg {} : {}", startTime, String.format("%04x", i.flags));
        }
    }

    public static void analyze(HuaweiSupportProvider provider, TruSleepStatus[] status, TruSleepData data) {

        // Analyse each time range as specified by TruSleepStatus
        for (TruSleepStatus s : status) {

            ArrayList<TruSleepDataAcc> acc = new ArrayList<>();
            ArrayList<TruSleepDataPpg> ppg = new ArrayList<>();

            // Collect Accelerometer data for current time frame
            for (TruSleepDataAcc i : data.dataACCs) {
                if (i.startTime >= s.startTime && i.startTime <= s.endTime)
                    acc.add(i);
            }

            // Collect PPG data for current time frame
            for (TruSleepDataPpg i : data.dataPPGs) {
                if (i.startTime >= s.startTime && i.startTime <= s.endTime)
                    ppg.add(i);
            }

            // Analyse time frame
            analyzeOneTimeframe(provider, s, acc, ppg);
        }
    }

    public static class SleepFileDownloadCallback extends HuaweiFileDownloadManager.FileDownloadCallback {
        private byte[] statusData;
        private byte[] sleepData;

        private boolean statusSynced;
        private boolean dataSynced;
        protected HuaweiSupportProvider provider;
        public SleepFileDownloadCallback(HuaweiSupportProvider provider) {
            this.dataSynced = false;
            this.statusSynced = false;
            this.provider = provider;
        }

        public void syncComplete(byte[] statusData, byte[] sleepData) { }
        @Override
        public void downloadException(HuaweiFileDownloadManager.HuaweiFileDownloadException e) {
            if (e.fileRequest == null) {
                LOG.error("Failed to download TruSleep file: {}", e.toString());
                syncComplete(statusData, sleepData);
                return;
            }

            if (e.fileRequest.getFileType() == HuaweiFileDownloadManager.FileType.SLEEP_STATE) {
                statusSynced = true;
            } else if (e.fileRequest.getFileType() == HuaweiFileDownloadManager.FileType.SLEEP_DATA) {
                dataSynced = true;
            }
            if (statusSynced && dataSynced)
                syncComplete(statusData, sleepData);
        }

        @Override
        public void downloadComplete(HuaweiFileDownloadManager.FileRequest fileRequest) {
            if (fileRequest.getFileType() == HuaweiFileDownloadManager.FileType.SLEEP_STATE) {
                statusData = fileRequest.getData();
                statusSynced = true;
            } else if (fileRequest.getFileType() == HuaweiFileDownloadManager.FileType.SLEEP_DATA) {
                sleepData = fileRequest.getData();
                dataSynced = true;
            }
            LOG.debug("Downloaded TruSleep file {}", fileRequest.getFileType());
            if (statusSynced && dataSynced)
                syncComplete(statusData, sleepData);
        }

    }
}
