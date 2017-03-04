package nodomain.freeyourgadget.gadgetbridge.util;

import android.content.Context;
import android.support.annotation.NonNull;

import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;

public class NotificationUtils {
    @NonNull
    public static String getPreferredTextFor(NotificationSpec notificationSpec, int lengthBody, int lengthSubject, Context context) {
        switch (notificationSpec.type) {
            case GENERIC_ALARM_CLOCK:
                return StringUtils.getFirstOf(notificationSpec.title, notificationSpec.subject);
            case GENERIC_SMS:
            case GENERIC_EMAIL:
                return formatText(notificationSpec.sender, notificationSpec.subject, notificationSpec.body, lengthBody, lengthSubject, context);
            case GENERIC_NAVIGATION:
                return StringUtils.getFirstOf(notificationSpec.title, notificationSpec.body);
            case RIOT:
            case SIGNAL:
            case TELEGRAM:
            case TWITTER:
            case WHATSAPP:
            case CONVERSATIONS:
            case FACEBOOK:
            case FACEBOOK_MESSENGER:
                return notificationSpec.body;
        }
        return "";
    }

    @NonNull
    public static String formatText(String sender, String subject, String body, int lengthBody, int lengthSubject, Context context) {
        StringBuilder builder = new StringBuilder();
        builder.append(StringUtils.truncate(body, lengthBody));
        builder.append(StringUtils.truncate(subject, lengthSubject));
        builder.append(StringUtils.formatSender(sender, context));

        return builder.toString();
    }

    public static String getPreferredTextFor(CallSpec callSpec) {
        return StringUtils.getFirstOf(callSpec.name, callSpec.number);
    }
}
