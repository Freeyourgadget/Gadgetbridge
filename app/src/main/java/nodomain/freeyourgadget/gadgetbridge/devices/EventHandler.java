package nodomain.freeyourgadget.gadgetbridge.devices;

import android.net.Uri;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.model.ServiceCommand;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;

/**
 * Specifies all events that GadgetBridge intends to send to the gadget device.
 * Implementations can decide to ignore events that they do not support.
 * Implementations need to send/encode event to the connected device.
 */
public interface EventHandler {
    void onSMS(String from, String body);

    void onEmail(String from, String subject, String body);

    void onGenericNotification(String title, String details, int handle);

    void onSetTime();

    void onSetAlarms(ArrayList<? extends Alarm> alarms);

    void onSetCallState(@Nullable String number, @Nullable String name, ServiceCommand command);

    void onSetMusicInfo(String artist, String album, String track);

    void onEnableRealtimeSteps(boolean enable);

    void onInstallApp(Uri uri);

    void onAppInfoReq();

    void onAppStart(UUID uuid);

    void onAppDelete(UUID uuid);

    void onFetchActivityData();

    void onReboot();

    void onFindDevice(boolean start);

    void onScreenshotReq();
}
