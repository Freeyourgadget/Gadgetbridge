package nodomain.freeyourgadget.gadgetbridge.miband;

/**
 * Created by UgoRaffaele on 30/01/2015.
 */
public class UserInfo {

    private String btAddress;
    private String alias;
    private int gender;
    private int age;
    private int height;
    private int weight;
    private int type;

    private byte[] data = new byte[20];

    /**
     * Creates a default user info.
     *
     * @param btAddress the address of the MI Band to connect to.
     */
    public static UserInfo getDefault(String btAddress) {
        return new UserInfo(btAddress, "1550050550", 0, 25, 175, 70, 0);
    }

    /**
     * Creates a user info with the given data
     *
     * @param address the address of the MI Band to connect to.
     * @throws IllegalArgumentException when the given values are not valid
     */
    public static UserInfo create(String address, String alias, int gender, int age, int height, int weight, int type) throws IllegalArgumentException {
        if (address == null || address.length() == 0 || alias == null || alias.length() == 0 || gender < 0 || age <= 0 || weight <= 0 || type < 0) {
            throw new IllegalArgumentException("Invalid parameters");
        }
        try {
            return new UserInfo(address, alias, gender, age, height, weight, type);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Illegal user info data", ex);
        }
    }

    /**
     * Creates a user info with the given data
     *
     * @param address the address of the MI Band to connect to.
     */
    private UserInfo(String address, String alias, int gender, int age, int height, int weight, int type) {
        this.btAddress = address;
        this.alias = alias;
        this.gender = gender;
        this.age = age;
        this.height = height;
        this.weight = weight;
        this.type = type;

        byte[] sequence = new byte[20];

        int uid = calculateUidFrom(alias);
        String normalizedAlias = ensureTenCharacters(alias);
        sequence[0] = (byte) uid;
        sequence[1] = (byte) (uid >>> 8);
        sequence[2] = (byte) (uid >>> 16);
        sequence[3] = (byte) (uid >>> 24);

        sequence[4] = (byte) (gender & 0xff);
        sequence[5] = (byte) (age & 0xff);
        sequence[6] = (byte) (height & 0xff);
        sequence[7] = (byte) (weight & 0xff);
        sequence[8] = (byte) (type & 0xff);

        for (int u = 9; u < 19; u++)
            sequence[u] = normalizedAlias.getBytes()[u - 9];

        byte[] crcSequence = new byte[19];
        for (int u = 0; u < crcSequence.length; u++)
            crcSequence[u] = sequence[u];

        sequence[19] = (byte) ((getCRC8(crcSequence) ^ Integer.parseInt(address.substring(address.length() - 2), 16)) & 0xff);

        this.data = sequence;
    }



    private String ensureTenCharacters(String alias) {
        char[] result = new char[10];
        int aliasLen = alias.length();
        int maxLen = Math.min(10, alias.length());
        int diff = 10 - maxLen;
        for (int i = 0; i < maxLen; i++) {
            result[i + diff] = alias.charAt(i);
        }
        for (int i = 0; i < diff; i++) {
            result[i] = '0';
        }
        return new String(result);
    }

    private int calculateUidFrom(String alias) {
        int uid = 0;
        try {
            uid = Integer.parseInt(alias);
        } catch (NumberFormatException ex) {
            uid = alias.hashCode(); // simple as that
        }
        return uid;
    }

    public byte[] getData() {
        return this.data;
    }

    protected int getCRC8(byte[] seq) {
        int len = seq.length;
        int i = 0;
        byte crc = 0x00;

        while (len-- > 0) {
            byte extract = seq[i++];
            for (byte tempI = 8; tempI != 0; tempI--) {
                byte sum = (byte) ((crc & 0xff) ^ (extract & 0xff));
                sum = (byte) ((sum & 0xff) & 0x01);
                crc = (byte) ((crc & 0xff) >>> 1);
                if (sum != 0) {
                    crc = (byte) ((crc & 0xff) ^ 0x8c);
                }
                extract = (byte) ((extract & 0xff) >>> 1);
            }
        }
        return (crc & 0xff);
    }
}
