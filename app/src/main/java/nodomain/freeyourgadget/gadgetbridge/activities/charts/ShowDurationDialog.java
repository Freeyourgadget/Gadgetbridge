/*  Copyright (C) 2015-2020 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, vanous, Vebryn

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
package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.TextView;

import nodomain.freeyourgadget.gadgetbridge.R;

public class ShowDurationDialog extends Dialog {
    private final String mDuration;
    private TextView durationLabel;

    ShowDurationDialog(final String duration, final Context context) {
        super(context);
        mDuration = duration;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_charts_durationdialog);

        durationLabel = findViewById(R.id.charts_duration_label);
        setDuration(mDuration);
    }

    public void setDuration(final String duration) {
        if (mDuration != null) {
            durationLabel.setText(duration);
        } else {
            durationLabel.setText("");
        }
    }
}
