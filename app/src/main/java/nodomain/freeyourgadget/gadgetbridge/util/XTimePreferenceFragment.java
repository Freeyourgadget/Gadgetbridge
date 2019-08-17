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
import android.text.format.DateFormat;
import android.view.View;
import android.widget.TimePicker;

import androidx.preference.DialogPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceDialogFragmentCompat;

public class XTimePreferenceFragment extends PreferenceDialogFragmentCompat implements DialogPreference.TargetFragment {
    private TimePicker picker = null;

    @Override
    protected View onCreateDialogView(Context context) {
        picker = new TimePicker(context);
        picker.setIs24HourView(DateFormat.is24HourFormat(getContext()));
        picker.setPadding(0, 50, 0, 50);

        return picker;
    }

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);
        XTimePreference pref = (XTimePreference) getPreference();

        picker.setCurrentHour(pref.hour);
        picker.setCurrentMinute(pref.minute);
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {

        if (positiveResult) {
            XTimePreference pref = (XTimePreference) getPreference();

            pref.hour = picker.getCurrentHour();
            pref.minute = picker.getCurrentMinute();

            String time = pref.getTime24h();
            if (pref.callChangeListener(time)) {
                pref.persistStringValue(time);
                pref.updateSummary();
            }
        }
    }


    @Override
    public Preference findPreference(CharSequence key) {
        return getPreference();
    }
}
