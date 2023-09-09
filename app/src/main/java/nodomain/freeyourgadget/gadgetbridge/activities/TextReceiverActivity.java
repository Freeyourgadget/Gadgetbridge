/*  Copyright (C) 2023 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.activities;

import android.content.Intent;
import android.os.Bundle;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;

/**
 * Receive any shared plaintext and forward it directly to the devices as a notification.
 */
public class TextReceiverActivity extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(TextReceiverActivity.class);

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();

        final String action = intent.getAction();
        if (!Intent.ACTION_SEND.equals(action)) {
            LOG.warn("Unknown action '{}'", action);
            finish();
            return;
        }

        final String type = intent.getType();
        if (!"text/plain".equals(type)) {
            LOG.warn("Unknown type '{}'", type);
            finish();
            return;
        }

        final String text = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (StringUtils.isBlank(text)) {
            LOG.warn("Text is null or empty");
            finish();
            return;
        }

        LOG.info("Sending '{}' to all devices", text);

        final NotificationSpec notificationSpec = new NotificationSpec();
        final String appName = getApplicationContext().getApplicationInfo()
                .loadLabel(getApplicationContext().getPackageManager())
                .toString();
        notificationSpec.title = appName;
        notificationSpec.body = text;
        notificationSpec.sourceAppId = BuildConfig.APPLICATION_ID;
        notificationSpec.sourceName = appName;
        notificationSpec.type = NotificationType.UNKNOWN;
        notificationSpec.pebbleColor = notificationSpec.type.color;

        GBApplication.deviceService().onNotification(notificationSpec);
        finish();
    }
}
