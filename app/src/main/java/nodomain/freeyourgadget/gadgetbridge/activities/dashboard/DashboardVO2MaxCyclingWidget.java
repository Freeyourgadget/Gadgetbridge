package nodomain.freeyourgadget.gadgetbridge.activities.dashboard;

import android.os.Bundle;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.DashboardFragment;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Vo2MaxSample;

public class DashboardVO2MaxCyclingWidget extends AbstractDashboardVO2MaxWidget {

    public DashboardVO2MaxCyclingWidget() {
        super(R.string.vo2max_cycling, "vo2max");
    }

    public static DashboardVO2MaxCyclingWidget newInstance(final DashboardFragment.DashboardData dashboardData) {
        final DashboardVO2MaxCyclingWidget fragment = new DashboardVO2MaxCyclingWidget();
        final Bundle args = new Bundle();
        args.putSerializable(ARG_DASHBOARD_DATA, dashboardData);
        fragment.setArguments(args);
        return fragment;
    }

    public Vo2MaxSample.Type getVO2MaxType() {
        return Vo2MaxSample.Type.CYCLING;
    }

    public String getWidgetKey() {
        return "vo2max_cycling";
    }

    @Override
    protected boolean isSupportedBy(final GBDevice device) {
        return device.getDeviceCoordinator().supportsVO2MaxCycling();
    }
}
