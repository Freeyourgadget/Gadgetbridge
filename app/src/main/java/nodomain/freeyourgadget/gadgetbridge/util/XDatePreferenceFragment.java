/*  Copyright (C) 2024 José Rebelo

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.util;

import android.content.Context;
import android.view.View;
import android.widget.DatePicker;

import androidx.annotation.NonNull;
import androidx.preference.DialogPreference;
import androidx.preference.Preference;

import nodomain.freeyourgadget.gadgetbridge.util.dialogs.MaterialPreferenceDialogFragment;

public class XDatePreferenceFragment extends MaterialPreferenceDialogFragment implements DialogPreference.TargetFragment {
    private DatePicker picker = null;

    @Override
    protected View onCreateDialogView(final Context context) {
        picker = new DatePicker(context);
        picker.setPadding(0, 50, 0, 50);

        return picker;
    }

    @Override
    protected void onBindDialogView(final View v) {
        super.onBindDialogView(v);
        final XDatePreference pref = (XDatePreference) getPreference();

        picker.init(pref.getYear(), pref.getMonth() - 1, pref.getDay(), null);

        if (pref.getMinDate() != 0) {
            picker.setMinDate(pref.getMinDate());
        }

        if (pref.getMaxDate() != 0) {
            picker.setMaxDate(pref.getMaxDate());
        }
    }

    @Override
    public void onDialogClosed(final boolean positiveResult) {
        if (!positiveResult) {
            return;
        }

        final XDatePreference pref = (XDatePreference) getPreference();
        pref.setValue(
                picker.getYear(),
                picker.getMonth() + 1,
                picker.getDayOfMonth()
        );

        final String date = pref.getPrefValue();
        if (pref.callChangeListener(date)) {
            pref.persistStringValue(date);
            pref.updateSummary();
        }
    }

    @Override
    public Preference findPreference(@NonNull final CharSequence key) {
        return getPreference();
    }
}
