package nodomain.freeyourgadget.gadgetbridge.model;

import java.util.HashMap;

public class AppNotificationType extends HashMap<String, NotificationType> {

	private static AppNotificationType _instance;

	public static AppNotificationType getInstance() {
		if(_instance == null) {
			return (_instance = new AppNotificationType());
		}

		return _instance;
	}

	private AppNotificationType() {
		// Generic Email
		put("com.fsck.k9", NotificationType.GENERIC_EMAIL);
		put("com.android.email", NotificationType.GENERIC_EMAIL);
		
		// Generic SMS
		put("com.moez.QKSMS", NotificationType.GENERIC_SMS);
		put("com.android.mms", NotificationType.GENERIC_SMS);
		put("com.android.messaging", NotificationType.GENERIC_SMS);
		put("com.sonyericsson.conversations", NotificationType.GENERIC_SMS);
		put("org.smssecure.smssecure", NotificationType.GENERIC_SMS);

		// Conversations
		put("eu.siacs.conversations", NotificationType.CONVERSATIONS);

		// Signal
		put("org.thoughtcrime.securesms", NotificationType.SIGNAL);

		// Telegram
		put("org.telegram.messenger", NotificationType.TELEGRAM);

		// Twitter
		put("org.mariotaku.twidere", NotificationType.TWITTER);
		put("com.twitter.android", NotificationType.TWITTER);
		put("org.andstatus.app", NotificationType.TWITTER);
		put("org.mustard.android", NotificationType.TWITTER);
		
		// Facebook
		put("me.zeeroooo.materialfb", NotificationType.FACEBOOK);
		put("it.rignanese.leo.slimfacebook", NotificationType.FACEBOOK);
		put("me.jakelane.wrapperforfacebook", NotificationType.FACEBOOK);
		put("com.facebook.katana", NotificationType.FACEBOOK);
		put("org.indywidualni.fblite", NotificationType.FACEBOOK);

		// Facebook Messenger
		put("com.facebook.orca", NotificationType.FACEBOOK_MESSENGER);

		// WhatsApp
		put("com.whatsapp", NotificationType.WHATSAPP);
	}

}
