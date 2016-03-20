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

    public static String getModel(String hwRev) {
        //TODO: get real data?
        String model;
        if (hwRev.startsWith("snowy")) {
            model = "pebble_time_black";
        } else if (hwRev.startsWith("spalding")) {
            model = "pebble_time_round_black_20mm";
        } else {
            model = "pebble_black";
        }
        return model;
    }
}
