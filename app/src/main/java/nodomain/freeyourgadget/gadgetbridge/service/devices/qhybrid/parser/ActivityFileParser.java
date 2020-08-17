package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.parser;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class ActivityFileParser {
    // state flags;
    int heartRateQuality;
    ActivityEntry.WEARING_STATE wearingState = ActivityEntry.WEARING_STATE.UNKNOWN;
    int currentTimestamp = -1;
    ActivityEntry currentSample = null;
    int currentId = 1;

    public ArrayList<ActivityEntry> parseFile(byte[] file) {
        ByteBuffer buffer = ByteBuffer.wrap(file);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // read file version
        short version = buffer.getShort(2);
        if (version != 22) throw new RuntimeException("File version " + version + ", 16 required");

        int startTime = buffer.getInt(8);
        short timeOffsetMinutes = buffer.getShort(12);

        short fileId = buffer.getShort(16);

        buffer.position(20);

        ArrayList<ActivityEntry> samples = new ArrayList<>();
        finishCurrentPacket(samples);

        while (buffer.position() < buffer.capacity() - 4) {
            byte next = buffer.get();

            if (paraseFlag(next, buffer, samples)) continue;

            if(currentSample != null) {
                parseVariabilityBytes(next, buffer.get());

                int heartRate = buffer.get() & 0xFF;
                int calories = buffer.get() & 0xFF;
                boolean isActive = (calories & 0x40) == 0x40; // upper two bits
                calories &= 0x3F; // delete upper two bits

                currentSample.heartRate = heartRate;
                currentSample.calories = calories;
                currentSample.isActive = isActive;
                finishCurrentPacket(samples);
            }
        }
        return samples;
    }

    private boolean paraseFlag(byte flag, ByteBuffer buffer, ArrayList<ActivityEntry> samples) {
        switch (flag) {
            case (byte) 0xCA:
            case (byte) 0xCB:
            case (byte) 0xCC:
            case (byte) 0xCD:
                buffer.get();
                break;
            case (byte) 0xCE:
                byte arg = buffer.get();
                byte wearBits = (byte)((arg & 0b00011000) >> 3);
                if(wearBits == 0) this.wearingState = ActivityEntry.WEARING_STATE.NOT_WEARING;
                else if(wearBits == 1) this.wearingState = ActivityEntry.WEARING_STATE.WEARING;
                else this.wearingState = ActivityEntry.WEARING_STATE.UNKNOWN;

                byte heartRateQualityBits = (byte)((arg & 0b11100000) >> 5);
                this.heartRateQuality = heartRateQualityBits;
                break;
            case (byte) 0xCF:
            case (byte) 0xDE:
            case (byte) 0xDF:
            case (byte) 0xE1:
                buffer.get();
                break;
            case (byte) 0xE2:
                byte type = buffer.get();
                int timestamp = buffer.getInt();
                short duration = buffer.getShort();
                short minutesOffset = buffer.getShort();
                if (type == 0x04) {
                    this.currentTimestamp = timestamp;
                }
                break;
            case (byte) 0xDD:
            case (byte) 0xFD:
                buffer.get();
                break;
            case (byte) 0xFE:
                byte arg2 = buffer.get();
                if(arg2 == (byte) 0xFE) {
                    // this.currentSample = new ActivitySample();
                    // this.currentSample.id = currentId++;
                }
                break;
            default:
                return false;
        }
        return true;
    }

    private void parseVariabilityBytes(byte lower, byte higher){
        if((lower & 0b0000001) == 0b0000001){
            currentSample.maxVariability = (higher & 0b00000011) * 25 + 1;
            currentSample.stepCount = lower & 0b1110;
            if((lower & 0b10000000) == 0b10000000){
                int factor = (lower >> 4) & 0b111;
                currentSample.variability = 512 + factor * 64 + (higher >> 2 & 0b111111);
            }else {
                currentSample.variability = lower & 0b01110000;
                currentSample.variability <<= 2;
                currentSample.variability |= (higher >> 2) & 0b111111;
            }
        }else{
            currentSample.stepCount = lower & 0b11111110;
            currentSample.variability = (int) higher * (int) higher * 64;
            currentSample.maxVariability = 10000;
        }
    }

    private void finishCurrentPacket(ArrayList<ActivityEntry> samples) {
        if (currentSample != null) {
            currentSample.timestamp = currentTimestamp;
            currentSample.heartRateQuality = this.heartRateQuality;
            currentSample.wearingState = wearingState;
            currentTimestamp += 60;
            samples.add(currentSample);
            currentSample = null;
        }
        this.currentSample = new ActivityEntry();
        this.currentSample.id = currentId++;
    }
}
