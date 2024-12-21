/*  Copyright (C) 2020-2024 Petr Vaněk, Taavi Eomäe

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
package nodomain.freeyourgadget.gadgetbridge.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class AboutActivity extends AbstractGBActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        TextView about_version = findViewById(R.id.about_version);
        TextView about_hash = findViewById(R.id.about_hash);
        String versionName = BuildConfig.VERSION_NAME;
        String versionHASH = BuildConfig.GIT_HASH_SHORT;
        about_version.setText(String.format(getString(R.string.about_version), versionName));
        about_version.setOnClickListener(this::copyVersionToClipboard);
        about_hash.setText(String.format(getString(R.string.about_hash), versionHASH));
        about_hash.setOnClickListener(this::copyVersionToClipboard);

        TextView link1 = findViewById(R.id.links1);
        link1.setMovementMethod(LinkMovementMethod.getInstance());
        TextView link2 = findViewById(R.id.links2);
        link2.setMovementMethod(LinkMovementMethod.getInstance());
        TextView link3 = findViewById(R.id.links3);
        link3.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void copyVersionToClipboard(View view) {
        String versions = "Version: " + BuildConfig.VERSION_NAME +
                "\nCommit: " + BuildConfig.GIT_HASH_SHORT +
                "\nFlavor: " + BuildConfig.FLAVOR;
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Build data", versions);
        clipboard.setPrimaryClip(clip);
        GB.toast(getString(R.string.about_build_details_copied_to_clipboard), Toast.LENGTH_LONG, GB.INFO);
    }
}
