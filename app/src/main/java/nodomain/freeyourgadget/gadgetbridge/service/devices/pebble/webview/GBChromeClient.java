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
