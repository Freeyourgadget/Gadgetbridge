package nodomain.freeyourgadget.gadgetbridge;

/**
 * Some more or less useful utility methods to aid local (non-device) testing.
 */
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
