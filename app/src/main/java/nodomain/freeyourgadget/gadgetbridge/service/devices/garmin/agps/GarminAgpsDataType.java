package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.agps;

public enum GarminAgpsDataType {
    GLONASS("CPE_GLO.BIN"), QZSS("CPE_QZSS.BIN"), GPS("CPE_GPS.BIN"),
    GALILEO("CPE_GAL.BIN");

    private final String fileName;

    GarminAgpsDataType(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    public static boolean isValidAgpsDataFileName(String fileName) {
        for (GarminAgpsDataType type: GarminAgpsDataType.values()) {
            if (fileName.equals(type.fileName)) {
                return true;
            }
        }
        return false;
    }
}
