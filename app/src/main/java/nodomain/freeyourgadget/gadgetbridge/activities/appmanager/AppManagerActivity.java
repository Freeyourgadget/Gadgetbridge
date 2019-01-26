/*  Copyright (C) 2016-2018 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti

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
package nodomain.freeyourgadget.gadgetbridge.activities.appmanager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import androidx.core.app.NavUtils;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractFragmentPagerAdapter;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBFragmentActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.FwAppInstallerActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;


public class AppManagerActivity extends AbstractGBFragmentActivity {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractAppManagerFragment.class);
    private int READ_REQUEST_CODE = 42;

    private GBDevice mGBDevice = null;

    public GBDevice getGBDevice() {
        return mGBDevice;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fragmentappmanager);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mGBDevice = extras.getParcelable(GBDevice.EXTRA_DEVICE);
        }
        if (mGBDevice == null) {
            throw new IllegalArgumentException("Must provide a device when invoking this activity");
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        assert fab != null;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                startActivityForResult(intent, READ_REQUEST_CODE);
            }
        });

        // Set up the ViewPager with the sections adapter.
        ViewPager viewPager = (ViewPager) findViewById(R.id.appmanager_pager);
        if (viewPager != null) {
            viewPager.setAdapter(getPagerAdapter());
        }
    }

    @Override
    protected AbstractFragmentPagerAdapter createFragmentPagerAdapter(FragmentManager fragmentManager) {
        return new SectionsPagerAdapter(fragmentManager);
    }

    public static synchronized void deleteFromAppOrderFile(String filename, UUID uuid) {
        ArrayList<UUID> uuids = getUuidsFromFile(filename);
        uuids.remove(uuid);
        rewriteAppOrderFile(filename, uuids);
    }

    public class SectionsPagerAdapter extends AbstractFragmentPagerAdapter {

        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            switch (position) {
                case 0:
                    return new AppManagerFragmentCache();
                case 1:
                    return new AppManagerFragmentInstalledApps();
                case 2:
                    return new AppManagerFragmentInstalledWatchfaces();
            }
            return null;
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.appmanager_cached_watchapps_watchfaces);
                case 1:
                    return getString(R.string.appmanager_installed_watchapps);
                case 2:
                    return getString(R.string.appmanager_installed_watchfaces);
                case 3:
            }
            return super.getPageTitle(position);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    static synchronized void rewriteAppOrderFile(String filename, List<UUID> uuids) {
        try (BufferedWriter out = new BufferedWriter(new FileWriter(FileUtils.getExternalFilesDir() + "/" + filename))) {
            for (UUID uuid : uuids) {
                out.write(uuid.toString());
                out.newLine();
            }
        } catch (IOException e) {
            LOG.warn("can't write app order to file!");
        }
    }

    synchronized public static void addToAppOrderFile(String filename, UUID uuid) {
        ArrayList<UUID> uuids = getUuidsFromFile(filename);
        if (!uuids.contains(uuid)) {
            uuids.add(uuid);
            rewriteAppOrderFile(filename, uuids);
        }
    }

    static synchronized ArrayList<UUID> getUuidsFromFile(String filename) {
        ArrayList<UUID> uuids = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(new FileReader(FileUtils.getExternalFilesDir() + "/" + filename))) {
            String line;
            while ((line = in.readLine()) != null) {
                uuids.add(UUID.fromString(line));
            }
        } catch (IOException e) {
            LOG.warn("could not read sort file");
        }
        return uuids;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Intent startIntent = new Intent(AppManagerActivity.this, FwAppInstallerActivity.class);
            startIntent.setAction(Intent.ACTION_VIEW);
            startIntent.setDataAndType(resultData.getData(), null);
            startActivity(startIntent);
        }
    }
}
