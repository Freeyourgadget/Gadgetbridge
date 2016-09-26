package nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.alertnotification;

/**
 * https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.alert_category_id.xml
 * https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.alert_category_id_bit_mask.xml
 */
public enum Category {
    Simple(0),
    Email(1),
    News(2),
    IncomingCall(3),
    MissedCall(4),
    SMS(5),
    VoiceMail(6),
    Schedule(7),
    HighPriorityAlert(8),
    InstantMessage(9);
    // 10-250 reserved for future use
    // 251-255 defined by service specification

    private final int id;

    Category(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    private int asBit() {
        return byteNumber() <=7 ? 0 : 1;
    }

    private int byteNumber() {
        return bitNumber() > 7 ? 0 : 1;
    }

    private int bitNumber() {
        // the ID corresponds to the bit for the bitset
        return id;
    }

    public static byte[] toBitmask(Category... categories) {
        byte[] result = new byte[2];

        for (Category category : categories) {
            result[category.byteNumber()] |= category.asBit();
        }
        return result;
    }
}
