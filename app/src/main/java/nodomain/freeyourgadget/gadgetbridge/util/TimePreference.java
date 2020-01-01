/*  Copyright (C) 2017-2019 Carsten Pfeiffer, José Rebelo

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
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
            if (defaultValue != null) {
                time = defaultValue.toString();
            } else {
                time = "00:00";
            }
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
