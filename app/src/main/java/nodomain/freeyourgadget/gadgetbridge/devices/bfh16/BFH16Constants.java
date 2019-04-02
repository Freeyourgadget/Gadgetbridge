package nodomain.freeyourgadget.gadgetbridge.devices.bfh16;

import java.util.UUID;

import static nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport.BASE_UUID;

public final class BFH16Constants {

    //public static final UUID BFH16_IDENTIFICATION_SERVICE1 = UUID.fromString(String.format(BASE_UUID, "FEF5") );
    public static final UUID BFH16_IDENTIFICATION_SERVICE1 = UUID.fromString("0000fef5-0000-1000-8000-00805f9b34fb");
    //public static final UUID BFH16_IDENTIFICATION_SERVICE2 = UUID.fromString(String.format(BASE_UUID, "FEE7") );
    public static final UUID BFH16_IDENTIFICATION_SERVICE2 = UUID.fromString("0000fee7-0000-1000-8000-00805f9b34fb");

    public static final UUID BFH16_MAIN_SERVICE = UUID.fromString(String.format(BASE_UUID, "33F4") );

    
    //Known Services
    public static final UUID BFH16_SERVICE_1 = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");
    public static final UUID BFH16_SERVICE_2 = UUID.fromString("00001801-0000-1000-8000-00805f9b34fb"); //Attribute
    public static final UUID BFH16_SERVICE_3 = UUID.fromString("000056ff-0000-1000-8000-00805f9b34fb"); //Service
    public static final UUID BFH16_SERVICE_4 = UUID.fromString("0000fee7-0000-1000-8000-00805f9b34fb"); //Service

}
