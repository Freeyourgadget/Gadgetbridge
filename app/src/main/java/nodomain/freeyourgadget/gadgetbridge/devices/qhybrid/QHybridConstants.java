/*  Copyright (C) 2022 Arjan Schrijver

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
package nodomain.freeyourgadget.gadgetbridge.devices.qhybrid;

import java.util.HashMap;
import java.util.Map;

public final class QHybridConstants {
    public static final String HYBRIDHR_WATCHFACE_VERSION = "1.1";

    public static Map<String, String> KNOWN_WAPP_VERSIONS = new HashMap<String, String>() {
        {
            put("buddyChallengeApp", "2.10");
            put("commuteApp", "2.5");
            put("launcherApp", "3.8");
            put("musicApp", "3.4");
            put("notificationsPanelApp", "3.5");
            put("ringPhoneApp", "3.7");
            put("settingApp", "3.12");
            put("stopwatchApp", "3.5");
            put("timerApp", "3.8");
            put("weatherApp", "3.10");
            put("wellnessApp", "3.15");
        }
    };
}
