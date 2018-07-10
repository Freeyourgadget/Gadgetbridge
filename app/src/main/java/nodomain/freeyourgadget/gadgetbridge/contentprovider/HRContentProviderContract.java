/*  Copyright (C) 2018 Benedikt Elser

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
package nodomain.freeyourgadget.gadgetbridge.contentprovider;

import android.net.Uri;

public final class HRContentProviderContract {

    public static final String COLUMN_STATUS = "Status";
    public static final String COLUMN_NAME = "Name";
    public static final String COLUMN_ADDRESS = "Address";
    public static final String COLUMN_MODEL = "Model";
    public static final String COLUMN_MESSAGE = "Message";
    public static final String COLUMN_HEARTRATE = "HeartRate";
    public static final String COLUMN_STEPS = "Steps";
    public static final String COLUMN_BATTERY = "Battery";

    public static final String[] deviceColumnNames = new String[]{COLUMN_NAME, COLUMN_MODEL, COLUMN_ADDRESS};
    public static final String[] activityColumnNames = new String[]{COLUMN_STATUS, COLUMN_MESSAGE};
    public static final String[] realtimeColumnNames = new String[]{COLUMN_STATUS, COLUMN_HEARTRATE, COLUMN_STEPS, COLUMN_BATTERY};

    public static final String AUTHORITY = "nodomain.freeyourgadget.gadgetbridge.realtimesamples.provider";

    static final String ACTIVITY_START_URL = "content://" + AUTHORITY + "/activity_start";
    static final String ACTIVITY_STOP_URL = "content://" + AUTHORITY + "/activity_stop";
    static final String REALTIME_URL = "content://" + AUTHORITY + "/realtime";
    static final String DEVICES_URL = "content://" + AUTHORITY + "/devices";

    public static final Uri ACTIVITY_START_URI = Uri.parse(ACTIVITY_START_URL);
    public static final Uri ACTIVITY_STOP_URI = Uri.parse(ACTIVITY_STOP_URL);
    public static final Uri REALTIME_URI = Uri.parse(REALTIME_URL);
    public static final Uri DEVICES_URI = Uri.parse(DEVICES_URL);
}
