package nodomain.freeyourgadget.gadgetbridge.activities.dashboard.preferences;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentContainerView;
import androidx.gridlayout.widget.GridLayout;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import java.util.GregorianCalendar;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.AbstractDashboardWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.data.DashboardData;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.widgets.DashboardWidgetFactory;

public class DashboardWidgetPreviewPreference extends Preference {
    private final String widgetName;
    private AbstractDashboardWidget widget;
    private DashboardData dashboardData;

    public DashboardWidgetPreviewPreference(@NonNull final Context context,
                                            @Nullable final AttributeSet attrs) {
        super(context, attrs);
        setPersistent(false);

        // Obtain custom attributes
        try (TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.DashboardWidgetPreviewPreference,
                0, 0)) {
            if (attrs != null) {
                widgetName = a.getString(R.styleable.DashboardWidgetPreviewPreference_widgetName);
            } else {
                widgetName = null;
            }
        }
        setLayoutResource(R.layout.dashboard_widget_preview_preference);
        setWidgetLayoutResource(R.layout.dashboard_widget_preview_empty);
    }

    @Override
    public void onBindViewHolder(@NonNull final PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        dashboardData = new DashboardData();
        dashboardData.reloadPreferences(GregorianCalendar.getInstance());

        if (widget == null) {
            widget = DashboardWidgetFactory.createWidget(widgetName, dashboardData);
            if (widget == null) {
                return;
            }
        }

        final FragmentActivity activity = (FragmentActivity) getContext();

        final int columnSpan = 1;

        final GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, 1f),
                GridLayout.spec(GridLayout.UNDEFINED, columnSpan, GridLayout.FILL, 1f)
        );
        final float scale = activity.getResources().getDisplayMetrics().density;
        layoutParams.width = 0;
        final int pixels_8dp = (int) (8 * scale + 0.5f);
        layoutParams.setMargins(pixels_8dp, pixels_8dp, pixels_8dp, pixels_8dp);

        final FragmentContainerView fragment = new FragmentContainerView(activity);
        int fragmentId = View.generateViewId();
        fragment.setId(fragmentId);
        fragment.setLayoutParams(layoutParams);

        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(fragmentId, widget)
                .commitAllowingStateLoss();

        final GridLayout gridlayout = (GridLayout) holder.findViewById(R.id.widget_preview_gridlayout);
        gridlayout.addView(fragment);
    }

    public void refresh() {
        if (dashboardData != null) {
            dashboardData.reloadPreferences(GregorianCalendar.getInstance());
        }
        if (widget != null) {
            widget.update();
        }
    }
}
