package nodomain.freeyourgadget.gadgetbridge.util;

import android.content.Context;
import android.support.annotation.NonNull;

import nodomain.freeyourgadget.gadgetbridge.R;

public class StringUtils {

    public static String truncate(String s, int maxLength){
        if (s == null) {
            return "";
        }

        int length = Math.min(s.length(), maxLength);
        if(length < 0) {
            return "";
        }

        return s.substring(0, length);
    }

    public static String pad(String s, int length){
        return pad(s, length, ' ');
    }

    public static String pad(String s, int length, char padChar){
        while(s.length() < length) {
            s += padChar;
        }
        return s;
    }

    @NonNull
    public static String formatSender(String sender, Context context) {
        if (sender == null || sender.length() == 0) {
            return "";
        }
        return context.getString(R.string.StringUtils_sender, sender);
    }

    @NonNull
    public static String getFirstOf(String first, String second) {
        if (first != null && first.length() > 0) {
            return first;
        }
        if (second != null) {
            return second;
        }
        return "";
    }
}
