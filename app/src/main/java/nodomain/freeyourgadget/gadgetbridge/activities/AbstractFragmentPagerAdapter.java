package nodomain.freeyourgadget.gadgetbridge.activities;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

import java.util.HashSet;
import java.util.Set;

public abstract class AbstractFragmentPagerAdapter extends FragmentStatePagerAdapter {
    private Set<AbstractGBFragment> fragments = new HashSet<>();
    private Object primaryFragment;

    public AbstractFragmentPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Object fragment = super.instantiateItem(container, position);
        if (fragment instanceof AbstractGBFragment) {
            fragments.add((AbstractGBFragment) fragment);
        }
        return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        super.destroyItem(container, position, object);
        fragments.remove(object);
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        super.setPrimaryItem(container, position, object);
        if (object != primaryFragment) {
            primaryFragment = object;
            setCurrentFragment(primaryFragment);
        }
    }

    private void setCurrentFragment(Object newCurrentFragment) {
        for (AbstractGBFragment frag : fragments) {
            if (frag != newCurrentFragment) {
                frag.onMadeInvisibleInActivity();
            } else {
                frag.onMadeVisibleInActivityInternal();
            }
        }
    }
}
