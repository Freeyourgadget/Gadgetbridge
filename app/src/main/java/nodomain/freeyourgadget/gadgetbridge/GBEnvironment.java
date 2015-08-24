package nodomain.freeyourgadget.gadgetbridge;

public class GBEnvironment {
    private boolean localTest;
    private boolean deviceTest;

    public static GBEnvironment createLocalTestEnvironment() {
        GBEnvironment env = new GBEnvironment();
        env.localTest = true;
        return env;
    }

    public static GBEnvironment createDeviceEnvironment() {
        GBEnvironment env = new GBEnvironment();
        return env;
    }

    public final boolean isTest() {
        return localTest || deviceTest;
    }

    public boolean isLocalTest() {
        return localTest;
    }

}
