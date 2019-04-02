package nodomain.freeyourgadget.gadgetbridge.devices.bfh16;

import java.util.UUID;

import static nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport.BASE_UUID;

public final class BFH16Constants {

    //public static final UUID BFH16_IDENTIFICATION_SERVICE1 = UUID.fromString(String.format(BASE_UUID, "FEF5") );
    public static final UUID BFH16_IDENTIFICATION_SERVICE1 = UUID.fromString("0000fef5-0000-1000-8000-00805f9b34fb");
    public static final UUID BFH16_IDENTIFICATION_SERVICE2 = UUID.fromString(String.format(BASE_UUID, "FEE7") );

    public static final UUID BFH16_MAIN_SERVICE = UUID.fromString(String.format(BASE_UUID, "33F4") );

}
