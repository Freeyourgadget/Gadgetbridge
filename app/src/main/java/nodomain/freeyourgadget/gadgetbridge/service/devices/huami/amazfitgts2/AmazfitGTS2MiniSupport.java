/*  Copyright (C) 2017-2021 Andreas Shimokawa, Carsten Pfeiffer, Dmytro
    Bielik, pangwalla

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitgts2;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiFWHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitgts2.AmazfitGTS2MiniFWHelper;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiSupport;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.alertnotification.AlertCategory;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.alertnotification.AlertNotificationProfile;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.alertnotification.NewAlert;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiIcon;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitgts.AmazfitGTSSupport;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class AmazfitGTS2MiniSupport extends AmazfitGTS2Support {

    private static final Logger LOG = LoggerFactory.getLogger(AmazfitGTS2MiniSupport.class);

    @Override
    protected HuamiSupport setLanguage(TransactionBuilder builder) {
        return setLanguageByIdNew(builder);
    }

    @Override
    public HuamiFWHelper createFWHelper(Uri uri, Context context) throws IOException {
        return new AmazfitGTS2MiniFWHelper(uri, context);
    }

    @Override
    protected void sendNotificationNew(NotificationSpec notificationSpec, boolean hasExtraHeader, int maxLength) {
        if (notificationSpec.type == NotificationType.GENERIC_ALARM_CLOCK) {
            onAlarmClock(notificationSpec);
            return;
        }

        String senderOrTitle = StringUtils.getFirstOf(notificationSpec.sender, notificationSpec.title);

        String message = StringUtils.truncate(senderOrTitle, 64) + "\0";

        if (notificationSpec.subject != null) {
            message += StringUtils.truncate(notificationSpec.subject, 128) + "\n\n";
        } else {
            if (!notificationSpec.type.getGenericType().equals("generic_chat") && !senderOrTitle.equals(notificationSpec.sourceName)) {
                // amazfit devices will disregard the title if not a chat app, so we have to include it again
                message = "-\0"; //leave title blank, it's useless
                message += StringUtils.truncate(senderOrTitle, 64) + "\n";
            }
        }
        if (notificationSpec.body != null) {
            message += StringUtils.truncate(notificationSpec.body, 512);
        }
        if (notificationSpec.body == null && notificationSpec.subject == null) {
            message += " "; // if we have no body we have to send at least something on some devices, else they reboot (Bip S)
        }

        try {
            TransactionBuilder builder = performInitialized("new notification");

            byte customIconId = HuamiIcon.mapToIconId(notificationSpec.type);
            AlertCategory alertCategory = AlertCategory.CustomHuami;

            // The SMS icon for AlertCategory.SMS is unique and not available as iconId
            if (notificationSpec.type == NotificationType.GENERIC_SMS) {
                alertCategory = AlertCategory.SMS;
            }
            // EMAIL icon does not work in FW 0.0.8.74, it did in 0.0.7.90
            else if (customIconId == HuamiIcon.EMAIL) {
                alertCategory = AlertCategory.Email;
            }

            // if I understood correctly, we don't need the extra logic for mi band 2 here
            int prefixlength = 2;

            // We also need a (fake) source name for Mi Band 3 for SMS/EMAIL, else the message is not displayed
            byte[] appSuffix = "\0 \0".getBytes();
            int suffixlength = appSuffix.length;

            if (alertCategory == AlertCategory.CustomHuami) {
                String appName;
                prefixlength = 3;
                final PackageManager pm = getContext().getPackageManager();
                ApplicationInfo ai = null;
                try {
                    ai = pm.getApplicationInfo(notificationSpec.sourceAppId, 0);
                } catch (PackageManager.NameNotFoundException ignored) {
                }

                if (ai != null) {
                    appName = "\0" + pm.getApplicationLabel(ai) + "\0";
                } else {
                    appName = "\0" + "UNKNOWN" + "\0";
                }
                appSuffix = appName.getBytes();
                suffixlength = appSuffix.length;
            }
            if (hasExtraHeader) {
                prefixlength += 4;
            }

            byte[] rawmessage = message.getBytes();
            int length = Math.min(rawmessage.length, maxLength - prefixlength);
            if (length < rawmessage.length) {
                length = StringUtils.utf8ByteLength(message, length);
            }

            byte[] command = new byte[length + prefixlength + suffixlength];
            int pos = 0;
            command[pos++] = (byte) alertCategory.getId();
            if (hasExtraHeader) {
                command[pos++] = 0; // TODO
                command[pos++] = 0;
                command[pos++] = 0;
                command[pos++] = 0;
            }
            command[pos++] = 1;
            if (alertCategory == AlertCategory.CustomHuami) {
                command[pos] = customIconId;
            }

            System.arraycopy(rawmessage, 0, command, prefixlength, length);
            System.arraycopy(appSuffix, 0, command, prefixlength + length, appSuffix.length);

            writeToChunked(builder, 0, command);

            builder.queue(getQueue());
        } catch (IOException ex) {
            LOG.error("Unable to send notification to device", ex);
        }
    }
}
