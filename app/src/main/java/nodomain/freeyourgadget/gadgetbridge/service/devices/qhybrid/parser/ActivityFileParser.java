/*  Copyright (C) 2020-2021 Andreas Shimokawa, Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.parser;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class ActivityFileParser {
    
    // state flags;
    int heartRateQuality;
    ActivityEntry.WEARING_STATE wearingState = ActivityEntry.WEARING_STATE.WEARING;
    int currentTimestamp = 0; // Aligns with `e2 04` from my testing
    ActivityEntry currentSample = null;
    int currentId = 1;
    int spO2 = -1; // Should actually do something with this
    

    public ArrayList<ActivityEntry> parseFile(byte[] file) {
        ByteBuffer buffer = ByteBuffer.wrap(file);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // read file version
        short version = buffer.getShort(2);
        if (version != 22) throw new RuntimeException("File version " + version + ", 16 required");

        this.currentTimestamp = buffer.getInt(8); 

        short timeOffsetMinutes = buffer.getShort(12);

        short fileId = buffer.getShort(16);

        buffer.position(52); // Seem to be another 32 bytes after the initial 20 stop 

        ArrayList<ActivityEntry> samples = new ArrayList<>();
        finishCurrentPacket(samples);


        while (buffer.position() < buffer.capacity() - 4) {
            byte next = buffer.get();

            switch (next) {
                case (byte) 0xCE:
                    parseWearByte(buffer.get());
                    byte f1 = buffer.get();
                    byte f2 = buffer.get();

                    if (f1 == (byte) 0xE2 && f2 == (byte) 0x04) {
                        int timestamp = buffer.getInt();
                        buffer.getShort(); // duration
                        buffer.getShort(); // minutes offset
                        this.currentTimestamp = timestamp; 
                    
                    } else if (f1 == (byte) 0xD3) { // Workout-related 
                        int hr1 = f2 & 0xFF; // Might be min HR during workout sometimes?
                        byte[] infoB = new byte[2];
                        buffer.get(infoB);

                        byte v1 = buffer.get();
                        byte v2 = buffer.get(buffer.position()); // Could be important for 11 byte packet  
                        if (v1 == (byte) 0xDF) {
                            int hr2 = v2 & 0xFF; // Max HR during workout - extra data inside? 
                            buffer.get();
                            if (infoB[0] == (byte) 0x08) 
                                buffer.get(new byte[11]); // ?
                            
                            else if (!elemValidFlags(buffer.get(buffer.position() + 4))) 
                                buffer.get(new byte[3]);
                            

                        } else if (v1 == (byte) 0xE2 && v2 == (byte) 0x04) {
                            buffer.get(new byte[13]);

                            if (!elemValidFlags(buffer.get(buffer.position())))
                                buffer.get(new byte[3]);
    
                        } else if (!elemValidFlags(buffer.get(buffer.position() + 4))) 
                            buffer.get();
                        

                    } else if (f1 == (byte) 0xCF || f1 == (byte) 0xDF) {
                        continue; // Not sure what to do with this                                  
                    
                    } else if (f1 == (byte) 0xD6) {
                        buffer.get(new byte[4]);

                    } else if (f1 == (byte) 0xFE && f2 == (byte) 0xFE) {
                        if (buffer.get(buffer.position()) == (byte) 0xFE) { buffer.get(); } // WHY?
                                                                     
                    } else if (elemValidFlags(buffer.get(buffer.position() + 2))) {
                        parseVariabilityBytes(f1, f2);
                        int heartRate = buffer.get() & 0xFF; 
                        int calories = buffer.get() & 0xFF;
                        boolean isActive = (calories & 0x40) == 0x40; 
                        calories &= 0x3F; 

                        currentSample.heartRate = heartRate; 
                        currentSample.calories = calories; 
                        currentSample.isActive = isActive; 
                        finishCurrentPacket(samples);

                        continue;
                    } 
                    
                    if (buffer.position() > buffer.capacity() - 4) {
                        continue;
                    } 

                    parseVariabilityBytes(buffer.get(), buffer.get());
                    int heartRate = buffer.get() & 0xFF;
                    int calories = buffer.get() & 0xFF;
                    boolean isActive = (calories & 0x40) == 0x40; // upper two bits
                    calories &= 0x3F; // delete upper two bits

                    currentSample.heartRate = heartRate;
                    currentSample.calories = calories;
                    currentSample.isActive = isActive;
                    finishCurrentPacket(samples);

                    break;
             
             case (byte) 0xC2: // Or `c2 X` `ac X` as per #2884
                    buffer.get(new byte[3]); 
                    
                    break;

             case (byte) 0xE2: 
                    buffer.get(new byte[9]);

                    if (!elemValidFlags(buffer.get(buffer.position()))) {
                        buffer.get(new byte[6]);
                    }
                    
                    break;

             case (byte) 0xE0:
                    // Workout Info
                    for (int i = 0; i < 14; i++) {
                        buffer.get(); // Attribute # 
                        byte size = buffer.get();
                        buffer.get(new byte[size & 0xFF]); // Can eventually use this, nowhere to pass for now 
                    }
                    
                    break;

             case (byte) 0xDD:
                    buffer.get(new byte[20]); // No idea what this is
                    
                    break;

            case (byte) 0xD6: // Seems to only come from intentional spot-checks, despite watch's value updating independently on occasion.
                    spO2 = buffer.get() & 0xFF;

                    break;
            
            case (byte) 0xCB: // Very rare, may even be removed
            case (byte) 0xCC: // Around 73 or 74
            case (byte) 0xCF: // Almost always 128 (0x80)
                    buffer.get();
                    break;

             default:
                    ;
            }

    

        }
        return samples;
    }

    private static boolean elemValidFlags(byte value) {
    for (byte i : new byte[]{(byte) 0xCE, (byte) 0xDD, (byte) 0xCB, (byte) 0xCC, (byte) 0xCF, (byte) 0xD6, (byte) 0xE2})
        if (value == i) 
            return true;

    return false;
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

    private void parseWearByte(byte wearArg) {
        byte wearBits = (byte)((wearArg & 0b00011000) >> 3);
        if (wearBits == 0) this.wearingState = ActivityEntry.WEARING_STATE.NOT_WEARING;
        else if (wearBits == 1) this.wearingState = ActivityEntry.WEARING_STATE.WEARING;
        else this.wearingState = ActivityEntry.WEARING_STATE.UNKNOWN;

        byte heartRateQualityBits = (byte)((wearArg & 0b11100000) >> 5);
        this.heartRateQuality = heartRateQualityBits;
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
