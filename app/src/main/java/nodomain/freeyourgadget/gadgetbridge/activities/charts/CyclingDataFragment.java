package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;

public class CyclingDataFragment extends AbstractChartFragment{
    private PieChart currentSpeedChart;
    private LineChart speedHistoryChart;

    @Override
    public String getTitle() {
        return "Cycling data";
    }

    @Override
    protected ChartsData refreshInBackground(ChartsHost chartsHost, DBHandler db, GBDevice device) {
        List<? extends ActivitySample> samples = getSamples(db, device);
        return null;
    }

    @Override
    protected void renderCharts() {

    }

    @Override
    protected List<? extends ActivitySample> getSamples(DBHandler db, GBDevice device, int tsFrom, int tsTo) {
        return null;
    }

    @Override
    protected void setupLegend(Chart chart) {

    }

    @Override
    protected void updateChartsnUIThread(ChartsData chartsData) {

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_cycling, container, false);

        currentSpeedChart = rootView.findViewById(R.id.chart_cycling_speed);
        speedHistoryChart = rootView.findViewById(R.id.chart_cycling_speed_history);

        currentSpeedChart.setNoDataText("");
        currentSpeedChart.getLegend().setEnabled(false);
        currentSpeedChart.setDrawHoleEnabled(true);
        currentSpeedChart.setHoleColor(Color.WHITE);
        currentSpeedChart.getDescription().setText("");
        // currentSpeedChart.setTransparentCircleColor(Color.WHITE);
        // currentSpeedChart.setTransparentCircleAlpha(110);
        currentSpeedChart.setHoleRadius(70f);
        currentSpeedChart.setTransparentCircleRadius(75f);
        currentSpeedChart.setDrawCenterText(true);
        currentSpeedChart.setRotationEnabled(true);
        currentSpeedChart.setHighlightPerTapEnabled(true);
        currentSpeedChart.setCenterTextOffset(0, 0);

        return rootView;
    }
}
