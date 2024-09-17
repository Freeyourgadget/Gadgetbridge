package nodomain.freeyourgadget.gadgetbridge.activities.dashboard;

import nodomain.freeyourgadget.gadgetbridge.model.Vo2MaxSample;

public interface DashboardVO2MaxWidgetInterface {
    Vo2MaxSample.Type getVO2MaxType();
    String getWidgetKey();
}
