package nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.alertnotification;

/**
 * https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.alert_category_id.xml
 * https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.alert_category_id_bit_mask.xml
 */
public class SupportedNewAlertCategory {
    private final int id;
//
//    public static Ca(byte[] categoryBytes) {
//
//    }

    public SupportedNewAlertCategory(int id) {
        this.id = id;
    }

    /**
     * Returns the numerical ID value of this category
     * To be used as uin8 value
     * @return the uint8 value for this category
     */
    public int getId() {
        return id;
    }

    private int realBitNumber() {
        // the ID corresponds to the bit for the bitset
        return id;
    }

    private int bitNumberPerByte() {
        // the ID corresponds to the bit for the bitset (per byte)
        return realBitNumber() % 8;
    }

    private int asBit() {
        return 1 << bitNumberPerByte();
    }

    private int byteNumber() {
        return id <= 7 ? 0 : 1;
    }

    /**
     * Converts the given categories to an array of bytes.
     * @param categories
     * @return
     */
    public static byte[] toBitmask(SupportedNewAlertCategory... categories) {
        byte[] result = new byte[2];

        for (SupportedNewAlertCategory category : categories) {
            result[category.byteNumber()] |= category.asBit();
        }
        return result;
    }
}
