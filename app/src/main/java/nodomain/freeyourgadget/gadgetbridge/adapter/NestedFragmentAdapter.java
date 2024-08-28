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
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.adapter;

import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.List;
import java.util.stream.Collectors;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBFragment;

public abstract class NestedFragmentAdapter extends FragmentStateAdapter {
    protected FragmentManager fragmentManager;

    public NestedFragmentAdapter(AbstractGBFragment fragment, FragmentManager childFragmentManager) {
        super(fragment);
        fragmentManager = childFragmentManager;
    }

    @Override
    public int getItemCount() {
        return 3;
    }

    public void updateFragments(int position) {
        List<AbstractGBFragment> fragments = fragmentManager.getFragments()
                .stream()
                .map(e -> (AbstractGBFragment) e)
                .collect(Collectors.toList());
        for (AbstractGBFragment fragment : fragments) {
            if (position < 0 || fragment != fragmentManager.findFragmentByTag("f" + position)) {
                fragment.onMadeInvisibleInActivity();
            } else {
                fragment.onMadeVisibleInActivityInternal();
            }
        }
    }
}
