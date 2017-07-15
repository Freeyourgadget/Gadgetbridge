package nodomain.freeyourgadget.gadgetbridge.util;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

public class TimePreference extends DialogPreference {
    private int hour = 0;
    private int minute = 0;

    private TimePicker picker = null;

    public TimePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected View onCreateDialogView() {
        picker = new TimePicker(getContext());
        picker.setIs24HourView(DateFormat.is24HourFormat(getContext()));
        picker.setPadding(0, 50, 0, 50);

        return picker;
    }

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);

        picker.setCurrentHour(hour);
        picker.setCurrentMinute(minute);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            hour = picker.getCurrentHour();
            minute = picker.getCurrentMinute();

            String time = getTime24h();

            if (callChangeListener(time)) {
                persistString(time);

                updateSummary();
            }
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        String time;

        if (restoreValue) {
            if (defaultValue == null) {
                time = getPersistedString("00:00");
            } else {
                time = getPersistedString(defaultValue.toString());
            }
        } else {
            time = defaultValue.toString();
        }

        String[] pieces = time.split(":");

        hour = Integer.parseInt(pieces[0]);
        minute = Integer.parseInt(pieces[1]);

        updateSummary();
    }

    public void updateSummary() {
        if (DateFormat.is24HourFormat(getContext()))
            setSummary(getTime24h());
        else
            setSummary(getTime12h());
    }

    public String getTime24h() {
        return String.format("%02d", hour) + ":" + String.format("%02d", minute);
    }

    public String getTime12h() {
        String suffix = hour < 12 ? " AM" : " PM";
        int h = hour > 12 ? hour - 12 : hour;

        return String.valueOf(h) + ":" + String.format("%02d", minute) + suffix;
    }
}
