/*  Copyright (C) 2022 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.PeriodicExporter;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class IntentApiReceiver extends BroadcastReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(IntentApiReceiver.class);
    
    private static final String msgDebugNotAllowed = "Intent API Allow Debug Commands not allowed";

    public static final String COMMAND_ACTIVITY_SYNC = "nodomain.freeyourgadget.gadgetbridge.command.ACTIVITY_SYNC";
    public static final String COMMAND_TRIGGER_EXPORT = "nodomain.freeyourgadget.gadgetbridge.command.TRIGGER_EXPORT";
    public static final String COMMAND_DEBUG_SEND_NOTIFICATION = "nodomain.freeyourgadget.gadgetbridge.command.DEBUG_SEND_NOTIFICATION";
    public static final String COMMAND_DEBUG_INCOMING_CALL = "nodomain.freeyourgadget.gadgetbridge.command.DEBUG_INCOMING_CALL";

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent.getAction() == null) {
            LOG.warn("Action is null");
            return;
        }

        final Prefs prefs = GBApplication.getPrefs();

        switch (intent.getAction()) {
            case COMMAND_ACTIVITY_SYNC:
                if (!prefs.getBoolean("intent_api_allow_activity_sync", false)) {
                    LOG.warn("Intent API activity sync trigger not allowed");
                    return;
                }

                final int dataTypes;
                final String dataTypesHex = intent.getStringExtra("dataTypesHex");
                if (dataTypesHex != null) {
                    final Matcher matcher = Pattern.compile("^0[xX]([0-9a-fA-F]+)$").matcher(dataTypesHex);
                    if (!matcher.find()) {
                        LOG.warn("Failed to parse dataTypesHex '{}' as hex", dataTypesHex);
                        return;
                    }
                    dataTypes = Integer.parseInt(matcher.group(1), 16);
                } else {
                    dataTypes = RecordedDataTypes.TYPE_ACTIVITY;
                }

                LOG.info("Triggering activity sync for data types {}", String.format("0x%08x", dataTypes));

                GBApplication.deviceService().onFetchRecordedData(dataTypes);
                break;

            case COMMAND_TRIGGER_EXPORT:
                if (!prefs.getBoolean("intent_api_allow_trigger_export", false)) {
                    LOG.warn("Intent API export trigger not allowed");
                    return;
                }

                LOG.info("Triggering export");

                final Intent exportIntent = new Intent(context, PeriodicExporter.class);
                context.sendBroadcast(exportIntent);
                break;

            case COMMAND_DEBUG_SEND_NOTIFICATION:
                if (!prefs.getBoolean("intent_api_allow_debug_commands", false)) {
                    LOG.warn(msgDebugNotAllowed);
                    return;
                }
                LOG.info("Triggering Debug Send notification message");
                NotificationSpec notificationSpec = new NotificationSpec();
                notificationSpec.sender = intent.getStringExtra("sender");
                if (notificationSpec.sender == null) {
                    notificationSpec.sender = "DEBUG Sender";
                }
                notificationSpec.phoneNumber = intent.getStringExtra("phoneNumber");
                if (notificationSpec.phoneNumber == null) {
                    notificationSpec.phoneNumber = "DEBUG PhoneNumber";
                }
                notificationSpec.subject = intent.getStringExtra("subject");
                if (notificationSpec.subject == null) {
                    notificationSpec.subject = "DEBUG Subject";
                }
                notificationSpec.body = intent.getStringExtra("body");
                if (notificationSpec.body == null) {
                    notificationSpec.body = "DEBUG Body";
                }
                notificationSpec.type = NotificationType.GENERIC_SMS;
                if (intent.getStringExtra("type") != null) {
                    try {
                        notificationSpec.type = NotificationType.valueOf(intent.getStringExtra("type"));
                    } catch(IllegalArgumentException e) {}
                }
                if (notificationSpec.type != NotificationType.GENERIC_SMS) {
                    // SMS notifications don't have a source app ID when sent by the SMSReceiver,
                    // so let's not set it here as well for consistency
                    notificationSpec.sourceAppId = BuildConfig.APPLICATION_ID;
                }
                notificationSpec.sourceName = context.getApplicationInfo()
                        .loadLabel(context.getPackageManager())
                        .toString();
                notificationSpec.pebbleColor = notificationSpec.type.color;
                notificationSpec.attachedActions = new ArrayList<>();
                if (notificationSpec.type == NotificationType.GENERIC_SMS) {
                    // REPLY action
                    NotificationSpec.Action replyAction = new NotificationSpec.Action();
                    replyAction.title = "Reply";
                    replyAction.type = NotificationSpec.Action.TYPE_SYNTECTIC_REPLY_PHONENR;
                    notificationSpec.attachedActions.add(replyAction);
                }
                GBApplication.deviceService().onNotification(notificationSpec);
                break;

            case COMMAND_DEBUG_INCOMING_CALL:
                if (!prefs.getBoolean("intent_api_allow_debug_commands", false)) {
                    LOG.warn(msgDebugNotAllowed);
                    return;
                }
                LOG.info("Triggering Debug Incoming Call");
                CallSpec callSpec = new CallSpec();
                callSpec.command = CallSpec.CALL_INCOMING;
                callSpec.number = intent.getStringExtra("caller");
                if (callSpec.number == null) {
                    callSpec.number = "DEBUG_INCOMING_CALL";
                }
                GBApplication.deviceService().onSetCallState(callSpec);
                break;
        }
    }

    public IntentFilter buildFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(COMMAND_ACTIVITY_SYNC);
        intentFilter.addAction(COMMAND_TRIGGER_EXPORT);
        intentFilter.addAction(COMMAND_DEBUG_SEND_NOTIFICATION);
        intentFilter.addAction(COMMAND_DEBUG_INCOMING_CALL);
        return intentFilter;
    }
}
