package nodomain.freeyourgadget.gadgetbridge.service.btle;

import java.util.UUID;

public class GattDescriptor {

    //part of the generic BLE specs see https://developer.bluetooth.org/gatt/descriptors/Pages/DescriptorsHomePage.aspx
    //the list is complete as of 2015-09-28
    public static final UUID UUID_DESCRIPTOR_GATT_CHARACTERISTIC_EXTENDED_PROPERTIES = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2900")));
    public static final UUID UUID_DESCRIPTOR_GATT_CHARACTERISTIC_USER_DESCRIPTION = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2901")));
    public static final UUID UUID_DESCRIPTOR_GATT_CLIENT_CHARACTERISTIC_CONFIGURATION = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2902")));
    public static final UUID UUID_DESCRIPTOR_GATT_SERVER_CHARACTERISTIC_CONFIGURATION = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2903")));
    public static final UUID UUID_DESCRIPTOR_GATT_CHARACTERISTIC_PRESENTATION_FORMAT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2904")));
    public static final UUID UUID_DESCRIPTOR_GATT_CHARACTERISTIC_AGGREGATE_FORMAT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2905")));
    public static final UUID UUID_DESCRIPTOR_VALID_RANGE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2906")));
    public static final UUID UUID_DESCRIPTOR_EXTERNAL_REPORT_REFERENCE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2907")));
    public static final UUID UUID_DESCRIPTOR_REPORT_REFERENCE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2908")));
    public static final UUID UUID_DESCRIPTOR_NUMBER_OF_DIGITALS = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2909")));
    public static final UUID UUID_DESCRIPTOR_VALUE_TRIGGER_SETTING = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "290A")));
    public static final UUID UUID_DESCRIPTOR_ES_CONFIGURATION = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "290B")));
    public static final UUID UUID_DESCRIPTOR_ES_MEASUREMENT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "290C")));
    public static final UUID UUID_DESCRIPTOR_ES_TRIGGER_SETTING = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "290D")));
    public static final UUID UUID_DESCRIPTOR_TIME_TRIGGER_SETTING = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "290E")));

}
