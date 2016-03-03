package nodomain.freeyourgadget.gadgetbridge.util;

public class PebbleUtils {
    public static String getPlatformName(String hwRev) {
        String platformName;
        if (hwRev.startsWith("snowy")) {
            platformName = "basalt";
        } else if (hwRev.startsWith("spalding")) {
            platformName = "chalk";
        } else {
            platformName = "aplite";
        }
        return platformName;
    }
}
