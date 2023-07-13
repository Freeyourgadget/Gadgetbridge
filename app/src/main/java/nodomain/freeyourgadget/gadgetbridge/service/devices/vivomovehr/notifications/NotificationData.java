/*  Copyright (C) 2020-2023 Petr Kadlec

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.notifications;

import android.util.SparseIntArray;
import nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr.VivomoveConstants;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ancs.AncsAttribute;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ancs.AncsCategory;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ancs.AncsEventFlag;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class NotificationData {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationData.class);

    private static final SparseIntArray POSITIVE_NOTIFICATION_ACTIONS = indexingMap(NotificationSpec.Action.TYPE_SYNTECTIC_OPEN, NotificationSpec.Action.TYPE_WEARABLE_REPLY, NotificationSpec.Action.TYPE_SYNTECTIC_REPLY_PHONENR, NotificationSpec.Action.TYPE_WEARABLE_SIMPLE);
    private static final SparseIntArray NEGATIVE_NOTIFICATION_ACTIONS = indexingMap(NotificationSpec.Action.TYPE_SYNTECTIC_DISMISS, NotificationSpec.Action.TYPE_SYNTECTIC_DISMISS_ALL, NotificationSpec.Action.TYPE_SYNTECTIC_MUTE);

    public final NotificationSpec spec;
    public final AncsCategory category;
    public final Set<AncsEventFlag> flags;
    public final String title;
    public final String subtitle;
    public final String message;
    public final NotificationSpec.Action positiveAction;
    public final NotificationSpec.Action negativeAction;

    public NotificationData(NotificationSpec spec) {
        this.spec = spec;

        final AncsCategory category;
        final Set<AncsEventFlag> flags = new HashSet<>();
        switch (spec.type) {
            case GENERIC_SMS:
                category = AncsCategory.SMS;
                break;
            case GENERIC_PHONE:
                category = AncsCategory.INCOMING_CALL;
                flags.add(AncsEventFlag.IMPORTANT);
                break;
            case GENERIC_EMAIL:
            case GMAIL:
            case BBM:
            case MAILBOX:
            case OUTLOOK:
                category = AncsCategory.EMAIL;
                break;
            case GENERIC_NAVIGATION:
            case GOOGLE_MAPS:
                category = AncsCategory.LOCATION;
                break;
            case GENERIC_CALENDAR:
            case GENERIC_ALARM_CLOCK:
                category = AncsCategory.SCHEDULE;
                break;
            case FACEBOOK:
            case LINKEDIN:
                flags.add(AncsEventFlag.SILENT);
                category = AncsCategory.SOCIAL;
                break;
            // TODO: The rest
            default:
                category = AncsCategory.OTHER;
                break;
        }

        this.positiveAction = findNotificationAction(spec, POSITIVE_NOTIFICATION_ACTIONS);
        this.negativeAction = findNotificationAction(spec, NEGATIVE_NOTIFICATION_ACTIONS);

        if (this.positiveAction != null) flags.add(AncsEventFlag.POSITIVE_ACTION);
        if (this.negativeAction != null) flags.add(AncsEventFlag.NEGATIVE_ACTION);

        this.category = category;
        this.flags = flags;

        if (!StringUtils.isEmpty(spec.title)) {
            this.title = spec.title;
            this.subtitle = spec.subject;
            this.message = spec.body;
        } else if (!StringUtils.isEmpty(spec.subject)) {
            this.title = spec.subject;
            this.subtitle = null;
            this.message = spec.body;
        } else if (!StringUtils.isEmpty(spec.body)) {
            this.title = spec.body;
            this.subtitle = null;
            this.message = spec.body;
        } else {
            // everything empty!?!
            this.title = spec.type.name();
            this.subtitle = null;
            this.message = spec.sender;
        }
    }

    public String getAttribute(AncsAttribute attribute) {
        switch (attribute) {
            case DATE:
                final long notificationTimestamp = spec.when == 0 ? System.currentTimeMillis() : spec.when;
                return VivomoveConstants.ANCS_DATE_FORMAT.format(new Date(notificationTimestamp));
            case TITLE:
                return title;
            case SUBTITLE:
                return subtitle;
            case APP_IDENTIFIER:
                return spec.sourceAppId;
            case MESSAGE:
                return message;
            case MESSAGE_SIZE:
                return Integer.toString(message.length());
            case POSITIVE_ACTION_LABEL:
                return positiveAction == null ? null : positiveAction.title;
            case NEGATIVE_ACTION_LABEL:
                return negativeAction == null ? null : negativeAction.title;
            case PHONE_NUMBER:
                return spec.phoneNumber;
            default:
                LOG.warn("Unknown attribute {}", attribute);
                return null;
        }
    }


    private static NotificationSpec.Action findNotificationAction(NotificationSpec notificationSpec, SparseIntArray expectedActionsMap) {
        if (notificationSpec == null || notificationSpec.attachedActions == null) return null;

        int bestIndex = Integer.MAX_VALUE;
        NotificationSpec.Action bestAction = null;
        for (final NotificationSpec.Action action : notificationSpec.attachedActions) {
            final int index = expectedActionsMap.get(action.type);
            if (index > 0 && index < bestIndex) {
                bestIndex = index;
                bestAction = action;
            }
        }
        return bestAction;
    }

    private static SparseIntArray indexingMap(int... data) {
        final SparseIntArray result = new SparseIntArray(data.length);
        for (int i = 0; i < data.length; ++i) {
            result.put(data[i], i + 1);
        }
        return result;
    }
}
