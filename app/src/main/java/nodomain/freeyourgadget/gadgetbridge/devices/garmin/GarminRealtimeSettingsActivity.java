package nodomain.freeyourgadget.gadgetbridge.devices.garmin;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Optional;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractSettingsActivityV2;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class GarminRealtimeSettingsActivity extends AbstractSettingsActivityV2 {
    private GBDevice device;
    private int screenId;

    public static final String EXTRA_SCREEN_ID = "screenId";

    @Override
    protected PreferenceFragmentCompat newFragment() {
        return GarminRealtimeSettingsFragment.newInstance(device, screenId);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        device = getIntent().getParcelableExtra(GBDevice.EXTRA_DEVICE);
        screenId = getIntent().getIntExtra(EXTRA_SCREEN_ID, GarminRealtimeSettingsFragment.ROOT_SCREEN_ID);

        super.onCreate(savedInstanceState);

        if (device == null || !device.isInitialized()) {
            GB.toast(getString(R.string.watch_not_connected), Toast.LENGTH_SHORT, GB.INFO);
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_garmin_realtime_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        final int itemId = item.getItemId();

        if (itemId == R.id.garmin_rt_debug_toggle) {
            getFragment().ifPresent(GarminRealtimeSettingsFragment::toggleDebug);
            return true;
        } else if (itemId == R.id.garmin_rt_debug_share) {
            getFragment().ifPresent(GarminRealtimeSettingsFragment::shareDebug);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        getFragment().ifPresent(GarminRealtimeSettingsFragment::refreshFromDevice);
    }

    private Optional<GarminRealtimeSettingsFragment> getFragment() {
        final FragmentManager fragmentManager = getSupportFragmentManager();
        final Fragment fragment = fragmentManager.findFragmentByTag(GarminRealtimeSettingsFragment.FRAGMENT_TAG);
        if (fragment == null) {
            return Optional.empty();
        }

        if (fragment instanceof GarminRealtimeSettingsFragment) {
            return Optional.of((GarminRealtimeSettingsFragment) fragment);
        }

        return Optional.empty();
    }
}
