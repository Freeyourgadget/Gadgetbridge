/*  Copyright (C) 2016-2018 Daniele Gobbetti

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
package nodomain.freeyourgadget.gadgetbridge.devices.liveview;
//Changed by Renze: Fixed brightness constants

import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Message constants reverse-engineered by Andrew de Quincey (<a
 * href="http://adq.livejournal.com">http://adq.livejournal.com</a>).
 *
 * @author Robert &lt;xperimental@solidproject.de&gt;
 */
public final class LiveviewConstants {

    public static Charset ENCODING = StandardCharsets.ISO_8859_1;
    public static ByteOrder BYTE_ORDER = ByteOrder.BIG_ENDIAN;

    public static final byte CLOCK_24H = 0;
    public static final byte CLOCK_12H = 1;

    public static final byte MSG_GETCAPS = 1;
    public static final byte MSG_GETCAPS_RESP = 2;

    public static final byte MSG_DISPLAYTEXT = 3;
    public static final byte MSG_DISPLAYTEXT_ACK = 4;

    public static final byte MSG_DISPLAYPANEL = 5;
    public static final byte MSG_DISPLAYPANEL_ACK = 6;

    public static final byte MSG_DEVICESTATUS = 7;
    public static final byte MSG_DEVICESTATUS_ACK = 8;

    public static final byte MSG_DISPLAYBITMAP = 19;
    public static final byte MSG_DISPLAYBITMAP_ACK = 20;

    public static final byte MSG_CLEARDISPLAY = 21;
    public static final byte MSG_CLEARDISPLAY_ACK = 22;

    public static final byte MSG_SETMENUSIZE = 23;
    public static final byte MSG_SETMENUSIZE_ACK = 24;

    public static final byte MSG_GETMENUITEM = 25;
    public static final byte MSG_GETMENUITEM_RESP = 26;

    public static final byte MSG_GETALERT = 27;
    public static final byte MSG_GETALERT_RESP = 28;

    public static final byte MSG_NAVIGATION = 29;
    public static final byte MSG_NAVIGATION_RESP = 30;

    public static final byte MSG_SETSTATUSBAR = 33;
    public static final byte MSG_SETSTATUSBAR_ACK = 34;

    public static final byte MSG_GETMENUITEMS = 35;

    public static final byte MSG_SETMENUSETTINGS = 36;
    public static final byte MSG_SETMENUSETTINGS_ACK = 37;

    public static final byte MSG_GETTIME = 38;
    public static final byte MSG_GETTIME_RESP = 39;

    public static final byte MSG_SETLED = 40;
    public static final byte MSG_SETLED_ACK = 41;

    public static final byte MSG_SETVIBRATE = 42;
    public static final byte MSG_SETVIBRATE_ACK = 43;

    public static final byte MSG_ACK = 44;

    public static final byte MSG_SETSCREENMODE = 64;
    public static final byte MSG_SETSCREENMODE_ACK = 65;

    public static final byte MSG_GETSCREENMODE = 66;
    public static final byte MSG_GETSCREENMODE_RESP = 67;

    public static final int DEVICESTATUS_OFF = 0;
    public static final int DEVICESTATUS_ON = 1;
    public static final int DEVICESTATUS_MENU = 2;

    public static final byte RESULT_OK = 0;
    public static final byte RESULT_ERROR = 1;
    public static final byte RESULT_OOM = 2;
    public static final byte RESULT_EXIT = 3;
    public static final byte RESULT_CANCEL = 4;

    public static final int NAVACTION_PRESS = 0;
    public static final int NAVACTION_LONGPRESS = 1;
    public static final int NAVACTION_DOUBLEPRESS = 2;

    public static final int NAVTYPE_UP = 0;
    public static final int NAVTYPE_DOWN = 1;
    public static final int NAVTYPE_LEFT = 2;
    public static final int NAVTYPE_RIGHT = 3;
    public static final int NAVTYPE_SELECT = 4;
    public static final int NAVTYPE_MENUSELECT = 5;

    public static final int ALERTACTION_CURRENT = 0;
    public static final int ALERTACTION_FIRST = 1;
    public static final int ALERTACTION_LAST = 2;
    public static final int ALERTACTION_NEXT = 3;
    public static final int ALERTACTION_PREV = 4;

    public static final int BRIGHTNESS_OFF = 49;
    public static final int BRIGHTNESS_DIM = 50;
    public static final int BRIGHTNESS_MAX = 51;

    public static final String CLIENT_SOFTWARE_VERSION = "0.0.3";

}
