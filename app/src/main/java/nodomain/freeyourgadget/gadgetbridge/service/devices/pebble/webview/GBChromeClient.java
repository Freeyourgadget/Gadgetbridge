/*  Copyright (C) 2017 Daniele Gobbetti

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble.webview;

import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.widget.Toast;

import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class GBChromeClient extends WebChromeClient {

    @Override
    public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
        if (ConsoleMessage.MessageLevel.ERROR.equals(consoleMessage.messageLevel())) {
            GB.toast(formatConsoleMessage(consoleMessage), Toast.LENGTH_LONG, GB.ERROR);
            //TODO: show error page
        }
        return super.onConsoleMessage(consoleMessage);
    }

    private static String formatConsoleMessage(ConsoleMessage message) {
        String sourceId = message.sourceId();
        if (sourceId == null || sourceId.length() == 0) {
            sourceId = "unknown";
        }
        return String.format("%s (at %s: %d)", message.message(), sourceId, message.lineNumber());
    }


}
