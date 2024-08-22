package nodomain.freeyourgadget.gadgetbridge.adapter;

import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.List;
import java.util.stream.Collectors;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBFragment;

abstract class NestedFragmentAdapter extends FragmentStateAdapter {
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
