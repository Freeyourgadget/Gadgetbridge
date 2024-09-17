package nodomain.freeyourgadget.gadgetbridge.activities.dashboard;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.DashboardFragment;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.Vo2MaxSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Vo2MaxSample;

public abstract class AbstractDashboardVO2MaxWidget extends AbstractGaugeWidget implements DashboardVO2MaxWidgetInterface {
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractDashboardVO2MaxWidget.class);

    public AbstractDashboardVO2MaxWidget(int label, @Nullable String targetActivityTab) {
        super(label, targetActivityTab);
    }

    @Override
    protected void populateData(final DashboardFragment.DashboardData dashboardData) {
        final List<GBDevice> devices = getSupportedDevices(dashboardData);
        final VO2MaxData data = new VO2MaxData();

        // Latest vo2max sample.
        Vo2MaxSample sample = null;
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            for (GBDevice dev : devices) {
                final Vo2MaxSampleProvider sampleProvider = (Vo2MaxSampleProvider) dev.getDeviceCoordinator().getVo2MaxSampleProvider(dev, dbHandler.getDaoSession());
                final Vo2MaxSample latestSample = sampleProvider.getLatestSample(getVO2MaxType(), dashboardData.timeTo * 1000L);
                if (latestSample != null && (sample == null || latestSample.getTimestamp() > sample.getTimestamp())) {
                    sample = latestSample;
                }
            }

            if (sample != null) {
                data.value = sample.getValue();
            }

        } catch (final Exception e) {
            LOG.error("Could not get vo2max for today", e);
        }

        dashboardData.put(getWidgetKey(), data);
    }

    public static int[] getColors() {
        return new int[]{
                ContextCompat.getColor(GBApplication.getContext(), R.color.vo2max_value_poor_color),
                ContextCompat.getColor(GBApplication.getContext(), R.color.vo2max_value_fair_color),
                ContextCompat.getColor(GBApplication.getContext(), R.color.vo2max_value_good_color),
                ContextCompat.getColor(GBApplication.getContext(), R.color.vo2max_value_excellent_color),
                ContextCompat.getColor(GBApplication.getContext(), R.color.vo2max_value_superior_color),
        };
    }

    public static float[] getSegments() {
        return new float[] {
                0.20F,
                0.20F,
                0.20F,
                0.20F,
                0.20F,
        };
    }

    public static float[] getVO2MaxRanges() {
        return new float[] {
                55.4F,
                51.1F,
                45.4F,
                41.7F,
                0.0F,
        };
    }

    @Override
    protected void draw(final DashboardFragment.DashboardData dashboardData) {
        final VO2MaxData vo2MaxData = (VO2MaxData) dashboardData.get(getWidgetKey());
        if (vo2MaxData == null) {
            drawSimpleGauge(0, -1);
            return;
        }

        final int[] colors = getColors();
        final float[] segments = getSegments();
        final float[] vo2MaxRanges = getVO2MaxRanges();
        float vo2MaxValue = calculateVO2maxGaugeValue(vo2MaxRanges, vo2MaxData.value != -1 ? vo2MaxData.value : 0);
        setText(String.valueOf(vo2MaxData.value != -1 ? Math.round(vo2MaxData.value) : "-"));
        drawSegmentedGauge(
                colors,
                segments,
                vo2MaxValue,
                false,
                true
        );
    }

    private float calculateVO2maxGaugeValue(float[] vo2MaxRanges, float vo2MaxValue) {
        float value = -1;
        for (int i = 0; i < vo2MaxRanges.length; i++) {
            if (vo2MaxValue - vo2MaxRanges[i] > 0) {
                float rangeValue = i - 1 >= 0 ? vo2MaxRanges[i-1] : 60F;
                float rangeDiff = rangeValue - vo2MaxRanges[i];
                float valueDiff = vo2MaxValue - vo2MaxRanges[i];
                float multiplayer = valueDiff / rangeDiff;
                value = (4 - i) * 0.2F + 0.2F * (multiplayer > 1 ? 1 : multiplayer) ;
                break;
            }
        }
        return value;
    }

    private static class VO2MaxData implements Serializable {
        private float value = -1;
    }
}
