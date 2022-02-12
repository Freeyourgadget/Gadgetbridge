package nodomain.freeyourgadget.gadgetbridge.service.devices.cycling.protocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class CSCProtocol {
    final int FLAG_WHEEL_DATA_AVAILABKE = 1;
    final int FLAG_CRANK_DATA_AVAILABLE = 2;

    public CSCMeasurement parsePacket(long timeOfArrival, byte[] packet){
        ByteBuffer buffer = ByteBuffer.wrap(packet);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        int wheelRevolutions = 0, crankRevolutions = 0;
        boolean wheelDataAvailable = false, crankDataAvailable = false;
        int lastWheelEvent = 0, lastCrankEvent = 0;

        byte flags = buffer.get();
        if((flags & FLAG_WHEEL_DATA_AVAILABKE) == FLAG_WHEEL_DATA_AVAILABKE){
            wheelRevolutions = buffer.getInt() & 0xFFFFFFFF;
            lastWheelEvent = buffer.getShort() & 0xFFFF;
            wheelDataAvailable = true;
        }
        if((flags & FLAG_CRANK_DATA_AVAILABLE) == FLAG_CRANK_DATA_AVAILABLE){
            crankRevolutions = buffer.getInt() & 0xFFFFFFFF;
            lastCrankEvent = buffer.getShort() & 0xFFFF;
            crankDataAvailable = true;
        }

        return new CSCMeasurement(timeOfArrival, wheelRevolutions, lastWheelEvent, wheelDataAvailable, crankRevolutions, lastCrankEvent, crankDataAvailable);
    }
}
