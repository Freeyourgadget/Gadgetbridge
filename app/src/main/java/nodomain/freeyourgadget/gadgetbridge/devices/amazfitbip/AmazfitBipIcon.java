/*  Copyright (C) 2017 Andreas Shimokawa

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

package nodomain.freeyourgadget.gadgetbridge.devices.amazfitbip;


import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;

public class AmazfitBipIcon {
    // icons which are unsure which app they are for are suffixed with _NN
    public static final int CHAT = 0;
    public static final int PENGUIN_1 = 1;
    public static final int MI_CHAT_2 = 2;
    public static final int FACEBOOK = 3;
    public static final int TWITTER = 4;
    public static final int MI_APP_5 = 5;
    public static final int SNAPCHAT = 6;
    public static final int WHATSAPP = 7;
    public static final int RED_WHITE_FIRE_8 = 8;
    public static final int CHINESE_9 = 9;
    public static final int ALARM_CLOCK = 10;
    public static final int APP_11 = 11;
    public static final int CAMERA_12 = 12;
    public static final int CHAT_BLUE_13 = 13;
    public static final int COW_14 = 14;
    public static final int CHINESE_15 = 15;
    public static final int CHINESE_16 = 16;
    public static final int STAR_17 = 17;
    public static final int APP_18 = 18;
    public static final int CHINESE_19 = 19;
    public static final int CHINESE_20 = 20;
    public static final int CALENDAR = 21;
    public static final int FACEBOOK_MESSENGER = 22;
    public static final int WHATSAPP_CALL_23 = 23;
    public static final int LINE = 24;
    public static final int TELEGRAM = 25;
    public static final int KAKAOTALK = 26;
    public static final int SKYPE = 27;
    public static final int VKONTAKTE = 28;
    public static final int POKEMONGO = 29;
    public static final int HANGOUTS = 30;
    public static final int MI_31 = 31;
    public static final int CHINESE_32 = 32;
    public static final int CHINESE_33 = 33;
    public static final int EMAIL = 34;
    public static final int WEATHER = 35;
    public static final int HR_WARNING_36 = 36;


    public static int mapToIconId(NotificationType type) {
        switch (type) {
            case UNKNOWN:
                return APP_11;
            case CONVERSATIONS:
                return CHAT;
            case GENERIC_EMAIL:
                return EMAIL;
            case GENERIC_NAVIGATION:
                return APP_11;
            case GENERIC_SMS:
                return CHAT;
            case GENERIC_CALENDAR:
                return CALENDAR;
            case FACEBOOK:
                return FACEBOOK;
            case FACEBOOK_MESSENGER:
                return FACEBOOK_MESSENGER;
            case RIOT:
                return CHAT;
            case SIGNAL:
                return CHAT_BLUE_13;
            case TWITTER:
                return TWITTER;
            case TELEGRAM:
                return TELEGRAM;
            case WHATSAPP:
                return WHATSAPP;
            case GENERIC_ALARM_CLOCK:
                return ALARM_CLOCK;
        }
        return APP_11;
    }
}