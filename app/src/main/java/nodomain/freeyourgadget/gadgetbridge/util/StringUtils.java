/*  Copyright (C) 2017-2020 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, João Paulo Barraca, Nephiel, Roi Greenberg

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
package nodomain.freeyourgadget.gadgetbridge.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

import androidx.annotation.NonNull;

public class StringUtils {



    @NonNull
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

    public static int utf8ByteLength(String string, int length) {
        if (string == null) {
            return 0;
        }
        ByteBuffer outBuf = ByteBuffer.allocate(length);
        CharBuffer inBuf = CharBuffer.wrap(string.toCharArray());
        StandardCharsets.UTF_8.newEncoder().encode(inBuf, outBuf, true);
        return outBuf.position();
    }

    public static String pad(String s, int length){
        return pad(s, length, ' ');
    }

    public static String pad(String s, int length, char padChar) {
        StringBuilder sBuilder = new StringBuilder(s);
        while (sBuilder.length() < length) {
            sBuilder.append(padChar);
        }
        s = sBuilder.toString();
        return s;
    }

    /**
     * Joins the given elements and adds a separator between each element in the resulting string.
     * There will be no separator at the start or end of the string. There will be no consecutive
     * separators (even in case an element is null or empty).
     * @param separator the separator string
     * @param elements the elements to concatenate to a new string
     * @return the joined strings, separated by the separator
     */
    @NonNull
    public static StringBuilder join(String separator, String... elements) {
        StringBuilder builder = new StringBuilder();
        if (elements == null) {
            return builder;
        }
        boolean hasAdded = false;
        for (String element : elements) {
            if (element != null && element.length() > 0) {
                if (hasAdded) {
                    builder.append(separator);
                }
                builder.append(element);
                hasAdded = true;
            }
        }
        return builder;
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

    public static boolean isEmpty(String string) {
        return string != null && string.length() == 0;
    }

    public static String ensureNotNull(String message) {
        if (message != null) {
            return message;
        }
        return "";
    }

    public static String terminateNull(String input) {
        if (input == null || input.length() == 0) {
            return new String(new byte[]{(byte) 0});
        }
        char lastChar = input.charAt(input.length() - 1);
        if (lastChar == 0) return input;

        byte[] newArray = new byte[input.getBytes().length + 1];
        System.arraycopy(input.getBytes(), 0, newArray, 0, input.getBytes().length);

        newArray[newArray.length - 1] = 0;

        return new String(newArray);
    }

    public static String bytesToHex(byte[] array) {
        return GB.hexdump(array, 0, -1);
    }
}
