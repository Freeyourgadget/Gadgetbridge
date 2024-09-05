/*  Copyright (C) 2024 Arjan Schrijver

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
package nodomain.freeyourgadget.gadgetbridge.activities.welcome;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class WelcomeFragmentIntro extends Fragment {
    private static final Logger LOG = LoggerFactory.getLogger(WelcomeFragmentIntro.class);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        final View view = inflater.inflate(R.layout.fragment_welcome_intro, container, false);
        final String[] themes = getResources().getStringArray(R.array.pref_theme_values);
        final Prefs prefs = GBApplication.getPrefs();
        final String currentTheme = prefs.getString("pref_key_theme", getString(R.string.pref_theme_value_system));
        final int currentThemeIndex = Arrays.asList(themes).indexOf(currentTheme);

        final MaterialAutoCompleteTextView themeMenu = view.findViewById(R.id.app_theme_dropdown_menu);
        themeMenu.setSaveEnabled(false);  // https://github.com/material-components/material-components-android/issues/1464#issuecomment-1258051448
        themeMenu.setText(getResources().getStringArray(R.array.pref_theme_options)[currentThemeIndex], false);
        themeMenu.setOnItemClickListener((adapterView, view1, i, l) -> {
            final SharedPreferences.Editor editor = prefs.getPreferences().edit();
            editor.putString("pref_key_theme", themes[i]).apply();
            final Handler handler = new Handler();
            handler.postDelayed(() -> {
                // Delay recreation of the Activity to give the dropdown some time to settle.
                // If we recreate it immediately, the theme popup will reopen, which is not what the user expects.
                Intent intent = new Intent();
                intent.setAction(GBApplication.ACTION_THEME_CHANGE);
                LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent);
            }, 500);
        });
        return view;
    }
}
