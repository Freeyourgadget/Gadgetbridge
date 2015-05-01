package nodomain.freeyourgadget.gadgetbridge;

import android.app.Application;
import android.content.Context;

public class GBApplication extends Application {
    private static GBApplication context;

    public GBApplication() {
        context = this;
    }

    public static Context getContext() {
        return context;
    }
}
