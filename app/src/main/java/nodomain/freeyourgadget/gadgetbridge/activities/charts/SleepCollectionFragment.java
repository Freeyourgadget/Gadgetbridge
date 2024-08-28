/*  Copyright (C) 2024 a0z, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.os.Bundle;

import androidx.fragment.app.FragmentManager;

import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBFragment;
import nodomain.freeyourgadget.gadgetbridge.adapter.NestedFragmentAdapter;
import nodomain.freeyourgadget.gadgetbridge.adapter.SleepFragmentAdapter;

public class SleepCollectionFragment extends AbstractCollectionFragment {
    public SleepCollectionFragment() {

    }

    public static SleepCollectionFragment newInstance(final boolean allowSwipe) {
        final SleepCollectionFragment fragment = new SleepCollectionFragment();
        final Bundle args = new Bundle();
        args.putBoolean(ARG_ALLOW_SWIPE, allowSwipe);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public NestedFragmentAdapter getNestedFragmentAdapter(AbstractGBFragment fragment, FragmentManager childFragmentManager) {
        return new SleepFragmentAdapter(this, getChildFragmentManager());
    }
}
