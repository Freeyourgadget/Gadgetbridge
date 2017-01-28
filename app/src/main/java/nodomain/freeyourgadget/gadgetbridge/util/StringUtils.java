package nodomain.freeyourgadget.gadgetbridge.util;

public class StringUtils {

    public static String truncate(String s, int maxLength){
        int length = Math.min(s.length(), maxLength);

        if(length < 0)
            return "";

        return s.substring(0, length);
    }

    public static String pad(String s, int length){
        return pad(s, length, ' ');
    }

    public static String pad(String s, int length, char padChar){

        while(s.length() < length)
            s += padChar;

        return s;
    }
}
