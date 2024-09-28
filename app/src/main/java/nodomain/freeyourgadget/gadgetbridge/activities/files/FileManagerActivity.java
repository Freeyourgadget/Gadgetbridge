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
package nodomain.freeyourgadget.gadgetbridge.activities.files;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class FileManagerActivity extends AbstractGBActivity implements MenuProvider {
    private static final Logger LOG = LoggerFactory.getLogger(FileManagerActivity.class);

    public static final String EXTRA_PATH = "path";

    private SearchView searchView;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_manager);
        addMenuProvider(this);

        final RecyclerView fileListView = findViewById(R.id.fileListView);
        fileListView.setLayoutManager(new LinearLayoutManager(this));

        final File directory;
        if (getIntent().hasExtra(EXTRA_PATH)) {
            directory = new File(getIntent().getStringExtra(EXTRA_PATH));
        } else {
            try {
                directory = FileUtils.getExternalFilesDir();
            } catch (final IOException e) {
                GB.toast("Failed to list external files dir", Toast.LENGTH_LONG, GB.ERROR);
                LOG.error("Failed to list external files dir", e);
                finish();
                return;
            }
        }

        if (!directory.isDirectory()) {
            GB.toast("Not a directory", Toast.LENGTH_LONG, GB.ERROR);
            finish();
            return;
        }

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(directory.getName());
        }

        final FileManagerAdapter appListAdapter = new FileManagerAdapter(this, directory);

        fileListView.setAdapter(appListAdapter);

        searchView = findViewById(R.id.fileListSearchView);
        searchView.setIconifiedByDefault(false);
        searchView.setVisibility(View.GONE);
        searchView.setIconified(false);
        searchView.setQuery("", false);
        searchView.clearFocus();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(final String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(final String newText) {
                appListAdapter.getFilter().filter(newText);
                return true;
            }
        });
    }

    @Override
    public void onCreateMenu(@NonNull final Menu menu, @NonNull final MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.menu_file_manager, menu);
    }

    @Override
    public boolean onMenuItemSelected(@NonNull final MenuItem menuItem) {
        final int itemId = menuItem.getItemId();
        if (itemId == R.id.file_manager_search) {
            searchView.setVisibility(View.VISIBLE);
            searchView.requestFocus();
            searchView.setIconified(true);
            searchView.setIconified(false);
            return true;
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
