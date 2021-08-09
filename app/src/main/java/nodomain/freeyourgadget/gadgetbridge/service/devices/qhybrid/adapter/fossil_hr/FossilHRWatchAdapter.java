/*  Copyright (C) 2019-2021 Andreas Shimokawa, Carsten Pfeiffer, Daniel Dakhno, Arjan Schrijver

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil_hr;

import static nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.configuration.ConfigurationPutRequest.UnitsConfigItem;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.configuration.ConfigurationPutRequest.VibrationStrengthConfigItem;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.music.MusicControlRequest.MUSIC_PHONE_REQUEST;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.music.MusicControlRequest.MUSIC_WATCH_REQUEST;
import static nodomain.freeyourgadget.gadgetbridge.util.BitmapUtil.convertDrawableToBitmap;
import static nodomain.freeyourgadget.gadgetbridge.util.StringUtils.shortenPackageName;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventAppInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventCallControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventFindPhone;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventMusicControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventNotificationControl;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.CommuteActionsActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.FossilFileReader;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.HybridHRActivitySampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.NotificationHRConfiguration;
import nodomain.freeyourgadget.gadgetbridge.entities.HybridHRActivitySample;
import nodomain.freeyourgadget.gadgetbridge.externalevents.NotificationListener;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceApp;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.Weather;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.file.FileHandle;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.parser.ActivityEntry;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.parser.ActivityFileParser;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.FossilRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.RequestMtuRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.SetDeviceStateRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FileDeleteRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FileGetRawRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FileLookupRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FilePutRawRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FilePutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.notification.DismissTextNotificationRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.notification.PlayCallNotificationRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.notification.PlayTextNotificationRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.application.ApplicationInformation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.application.ApplicationsListRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.async.ConfirmAppStatusRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.authentication.VerifyPrivateKeyRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.buttons.ButtonConfiguration;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.buttons.ButtonConfigurationPutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.commute.CommuteConfigPutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.configuration.ConfigurationGetRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.configuration.ConfigurationPutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.file.AssetFilePutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.file.FileEncryptedGetRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.file.FileEncryptedInterface;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.image.AssetImage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.image.AssetImageFactory;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.image.ImagesSetRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.json.JsonPutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.menu.SetCommuteMenuMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.music.MusicControlRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.music.MusicInfoSetRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.notification.NotificationFilterPutHRRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.notification.NotificationImage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.notification.NotificationImagePutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.quickreply.QuickReplyConfigurationPutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.quickreply.QuickReplyConfirmationPutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.theme.SelectedThemePutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.widget.CustomBackgroundWidgetElement;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.widget.CustomTextWidgetElement;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.widget.CustomWidget;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.widget.CustomWidgetElement;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.widget.Widget;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.widget.WidgetsPutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.FactoryResetRequest;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;
import nodomain.freeyourgadget.gadgetbridge.util.UriHelper;
import nodomain.freeyourgadget.gadgetbridge.util.Version;

public class FossilHRWatchAdapter extends FossilWatchAdapter {
    private byte[] phoneRandomNumber;
    private byte[] watchRandomNumber;

    private static ArrayList<Widget> widgets = new ArrayList<>();

    private NotificationHRConfiguration[] notificationConfigurations;

    private CallSpec currentCallSpec = null;
    private MusicSpec currentSpec = null;

    private byte jsonIndex = 0;

    private AssetImage backGroundImage = null;

    public FossilHRWatchAdapter(QHybridSupport deviceSupport) {
        super(deviceSupport);
    }

    private boolean saveRawActivityFiles = false;

    HashMap<String, Bitmap> appIconCache = new HashMap<>();
    String lastPostedApp = null;

    List<ApplicationInformation> installedApplications = new ArrayList();

    enum CONNECTION_MODE {
        NOT_INITIALIZED,
        AUTHENTICATED,
        NOT_AUTHENTICATED
    }

    CONNECTION_MODE connectionMode = CONNECTION_MODE.NOT_INITIALIZED;

    @Override
    public void initialize() {
        saveRawActivityFiles = getDeviceSpecificPreferences().getBoolean("save_raw_activity_files", false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            queueWrite(new RequestMtuRequest(512));
        }

        listApplications();
        getDeviceInfos();
    }

    @Override
    protected void initializeWithSupportedFileVersions() {
        if (getDeviceSupport().getDevice().getFirmwareVersion().contains("prod")) {
            GB.toast("Dummy FW, skipping initialization", Toast.LENGTH_LONG, GB.INFO);
            queueWrite(new SetDeviceStateRequest(GBDevice.State.INITIALIZED), false);
            return;
        }

        queueWrite(new SetDeviceStateRequest(GBDevice.State.AUTHENTICATING));

        negotiateSymmetricKey();
    }

    public void listApplications() {
        queueWrite(new ApplicationsListRequest(this));
    }

    private void initializeAfterAuthentication(boolean authenticated) {
        queueWrite(new SetDeviceStateRequest(GBDevice.State.INITIALIZING));

        if (!authenticated)
            GB.toast(getContext().getString(R.string.fossil_hr_auth_failed), Toast.LENGTH_LONG, GB.ERROR);

        setNotificationConfigurations();
        setQuickRepliesConfiguration();

        if (authenticated) {
            setVibrationStrength();
            setUnitsConfig();
            syncSettings();
            setTime();
        }

        overwriteButtons(null);
        loadBackground();
        loadWidgets();
        // renderWidgets();
        // dunno if there is any point in doing this at start since when no watch is connected the QHybridSupport will not receive any intents anyway

        queueWrite(new SetDeviceStateRequest(GBDevice.State.INITIALIZED));
    }

    private void handleAuthenticationResult(boolean success) {
        if (this.connectionMode != CONNECTION_MODE.NOT_INITIALIZED) return;
        this.connectionMode = success ? CONNECTION_MODE.AUTHENTICATED : CONNECTION_MODE.NOT_AUTHENTICATED;
        this.initializeAfterAuthentication(success);
    }

    @Override
    public void uninstallApp(String appName) {
        for (ApplicationInformation appInfo : this.installedApplications) {
            if (appInfo.getAppName().equals(appName)) {
                byte handle = appInfo.getFileHandle();
                short fullFileHandle = (short) ((FileHandle.APP_CODE.getMajorHandle()) << 8 | handle);
                queueWrite(new FileDeleteRequest(fullFileHandle));
                listApplications();
                break;
            }
        }
    }

    public void activateWatchface(String appName) {
        queueWrite(new SelectedThemePutRequest(this, appName));
    }

    private void setVibrationStrength() {
        Prefs prefs = new Prefs(getDeviceSpecificPreferences());
        int vibrationStrengh = prefs.getInt(DeviceSettingsPreferenceConst.PREF_VIBRATION_STRENGH_PERCENTAGE, 2);
        if (vibrationStrengh > 0) {
            vibrationStrengh = (vibrationStrengh + 1) * 25; // Seems 0,50,75,100 are working...
        }
        setVibrationStrength((short) (vibrationStrengh));
    }

    private void setUnitsConfig() {
        Prefs prefs = GBApplication.getPrefs();
        String unit = prefs.getString(SettingsActivity.PREF_MEASUREMENT_SYSTEM, GBApplication.getContext().getString(R.string.p_unit_metric));
        int value = 8; // dont know what this bit means but it was set for me before tampering
        if (!unit.equals("metric")) {
            value |= (4 | 1); // temperature and distance
        }
        queueWrite(
                (FileEncryptedInterface) new ConfigurationPutRequest(new UnitsConfigItem(value), this)
        );

    }

    @Override
    public void setVibrationStrength(short strength) {
        if (connectionMode == CONNECTION_MODE.NOT_AUTHENTICATED) {
            GB.toast(getContext().getString(R.string.fossil_hr_unavailable_unauthed), Toast.LENGTH_LONG, GB.ERROR);
            return;
        }

        queueWrite(
                (FileEncryptedInterface) new ConfigurationPutRequest(new VibrationStrengthConfigItem((byte) strength), this)
        );
    }

    private void setNotificationConfigurations() {
        // Set default icons
        ArrayList<NotificationImage> images = new ArrayList<>();
        images.add(new NotificationImage("icIncomingCall.icon", NotificationImage.getEncodedIconFromDrawable(getContext().getResources().getDrawable(R.drawable.ic_phone_outline)), 24, 24));
        images.add(new NotificationImage("icMissedCall.icon", NotificationImage.getEncodedIconFromDrawable(getContext().getResources().getDrawable(R.drawable.ic_phone_missed_outline)), 24,24));
        images.add(new NotificationImage("icMessage.icon", NotificationImage.getEncodedIconFromDrawable(getContext().getResources().getDrawable(R.drawable.ic_message_outline)),24,24));
        images.add(new NotificationImage("general_white.bin", NotificationImage.getEncodedIconFromDrawable(getContext().getResources().getDrawable(R.drawable.ic_alert_circle_outline)),24,24));

        // Set default notification filters
        ArrayList<NotificationHRConfiguration> notificationFilters = new ArrayList<>();
        notificationFilters.add(new NotificationHRConfiguration("generic", "general_white.bin"));
        notificationFilters.add(new NotificationHRConfiguration("call", new byte[]{(byte) 0x80, (byte) 0x00, (byte) 0x59, (byte) 0xB7}, "icIncomingCall.icon"));

        // Add icons and notification filters from cached past notifications
        Set<Map.Entry<String, Bitmap>> entrySet = this.appIconCache.entrySet();
        for (Map.Entry<String, Bitmap> entry : entrySet) {
            String iconName = shortenPackageName(entry.getKey()) + ".icon";
            images.add(new NotificationImage(iconName, entry.getValue()));
            notificationFilters.add(new NotificationHRConfiguration(entry.getKey(), iconName));
        }

        // Send notification icons
        try {
            queueWrite(new NotificationImagePutRequest(images.toArray(new NotificationImage[images.size()]), this));
        } catch (IOException e) {
            LOG.error("Error while sending notification icons", e);
        }

        // Send notification filters configuration
        this.notificationConfigurations = notificationFilters.toArray(new NotificationHRConfiguration[notificationFilters.size()]);
        queueWrite(new NotificationFilterPutHRRequest(this.notificationConfigurations, this));
    }

    private String[] getQuickReplies() {
        ArrayList<String> configuredReplies = new ArrayList<>();
        Prefs prefs = new Prefs(getDeviceSpecificPreferences());
        for (int i=1; i<=16; i++) {
            String quickReply = prefs.getString("canned_message_dismisscall_" + i, null);
            if (quickReply != null) {
                configuredReplies.add(quickReply);
            }
        }
        return configuredReplies.toArray(new String[0]);
    }

    public void setQuickRepliesConfiguration() {
        String[] quickReplies = getQuickReplies();
        if (quickReplies.length > 0) {
            NotificationImage quickReplyIcon = new NotificationImage("icMessage.icon", NotificationImage.getEncodedIconFromDrawable(getContext().getResources().getDrawable(R.drawable.ic_message_outline)), 24, 24);
            queueWrite(new NotificationImagePutRequest(quickReplyIcon, this));
            queueWrite(new QuickReplyConfigurationPutRequest(quickReplies, this));
        }
    }

    private File getBackgroundFile() {
        return new File(getContext().getExternalFilesDir(null), "hr_background.bin");
    }

    private void loadBackground() {
        this.backGroundImage = null;
        try {
            FileInputStream fis = new FileInputStream(getBackgroundFile());
            int count = fis.available();
            if (count != 14400) {
                throw new RuntimeException("wrong background file length");
            }
            byte[] file = new byte[14400];
            fis.read(file);
            fis.close();
            this.backGroundImage = AssetImageFactory.createAssetImage(file, 0, 0, 0);
        } catch (FileNotFoundException e) {
            SharedPreferences preferences = getDeviceSpecificPreferences();
            if (preferences.getBoolean("force_white_color_scheme", false)) {
                Bitmap whiteBitmap = Bitmap.createBitmap(239, 239, Bitmap.Config.ARGB_8888);
                new Canvas(whiteBitmap).drawColor(Color.WHITE);

                try {
                    this.backGroundImage = AssetImageFactory.createAssetImage(whiteBitmap, true, 0, 1, 0);
                } catch (IOException e2) {
                    LOG.error("Backgroundimage error", e2);
                }
            }
        } catch (IOException | RuntimeException e) {
            GB.log("error opening background file", GB.ERROR, e);
            GB.toast("error opening background file", Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    @Override
    public void setBackgroundImage(byte[] pixels) {
        if (pixels == null) {
            getBackgroundFile().delete();
            loadBackground(); // recreates the white background in force-white mode, else backgroundImage=null
        } else {
            this.backGroundImage = AssetImageFactory.createAssetImage(pixels, 0, 0, 0);
            try {
                FileOutputStream fos = new FileOutputStream(getBackgroundFile(), false);
                fos.write(pixels);
            } catch (IOException e) {
                GB.log("error saving background", GB.ERROR, e);
                GB.toast("error persistent saving background", Toast.LENGTH_LONG, GB.ERROR);
            }
        }
        renderWidgets();
    }

    private void loadWidgets() {
        Version firmwareVersion = getCleanFWVersion();
        if (firmwareVersion != null && firmwareVersion.compareTo(new Version("1.0.2.20")) >= 0) {
            return; // this does not work on newer firmware versions
        }
        Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(getDeviceSupport().getDevice().getAddress()));
        boolean forceWhiteBackground = prefs.getBoolean("force_white_color_scheme", false);
        String fontColor = forceWhiteBackground ? "black" : "default";

        Widget[] oldWidgets = widgets.toArray(new Widget[0]);

        widgets.clear();
        String widgetJson = GBApplication.getPrefs().getPreferences().getString("FOSSIL_HR_WIDGETS", "{}");
        String customWidgetJson = GBApplication.getPrefs().getString("QHYBRID_CUSTOM_WIDGETS", "[]");

        try {
            JSONObject widgetConfig = new JSONObject(widgetJson);
            JSONArray customWidgets = new JSONArray(customWidgetJson);

            Iterator<String> keyIterator = widgetConfig.keys();
            HashMap<String, Integer> positionMap = new HashMap<>(4);
            positionMap.put("top", 0);
            positionMap.put("right", 90);
            positionMap.put("bottom", 180);
            positionMap.put("left", 270);

            while (keyIterator.hasNext()) {
                String position = keyIterator.next();
                String identifier = widgetConfig.getString(position);
                Widget.WidgetType type = Widget.WidgetType.fromJsonIdentifier(identifier);

                Widget widget = null;
                if (type != null) {
                    widget = new Widget(type, positionMap.get(position), 63, fontColor);
                } else {
                    identifier = identifier.substring(7);
                    for (int i = 0; i < customWidgets.length(); i++) {
                        JSONObject customWidget = customWidgets.getJSONObject(i);
                        if (customWidget.getString("name").equals(identifier)) {
                            boolean drawCircle = false;
                            if (customWidget.has("drawCircle"))
                                drawCircle = customWidget.getBoolean("drawCircle");
                            CustomWidget newWidget = new CustomWidget(
                                    customWidget.getString("name"),
                                    positionMap.get(position),
                                    63,
                                    fontColor
                            );
                            JSONArray elements = customWidget.getJSONArray("elements");

                            for (int i2 = 0; i2 < elements.length(); i2++) {
                                JSONObject element = elements.getJSONObject(i2);
                                if (element.getString("type").equals("text")) {
                                    newWidget.addElement(new CustomTextWidgetElement(
                                            element.getString("id"),
                                            element.getString("value"),
                                            element.getInt("x"),
                                            element.getInt("y")
                                    ));
                                } else if (element.getString("type").equals("background")) {
                                    newWidget.addElement(new CustomBackgroundWidgetElement(
                                            element.getString("id"),
                                            element.getString("value")
                                    ));
                                }
                            }
                            widget = newWidget;
                        }
                    }
                }

                if (widget == null) continue;
                widgets.add(widget);
            }
        } catch (JSONException e) {
            LOG.error("Error while updating widgets", e);
        }

        for (Widget oldWidget : oldWidgets) {
            if (!(oldWidget instanceof CustomWidget)) continue;
            CustomWidget customOldWidget = (CustomWidget) oldWidget;
            for (CustomWidgetElement oldElement : customOldWidget.getElements()) {
                for (Widget newWidget : widgets) {
                    if (newWidget instanceof CustomWidget) {
                        ((CustomWidget) newWidget).updateElementValue(oldElement.getId(), oldElement.getValue());
                    }
                }
            }

        }

        uploadWidgets();
    }

    public void setInstalledApplications(List<ApplicationInformation> installedApplications) {
        this.installedApplications = installedApplications;
        GBDeviceEventAppInfo appInfoEvent = new GBDeviceEventAppInfo();
        appInfoEvent.apps = new GBDeviceApp[installedApplications.size()];
        for (int i = 0; i < installedApplications.size(); i++) {
            String appName = installedApplications.get(i).getAppName();
            String appVersion = installedApplications.get(i).getAppVersion();
            UUID appUUID = UUID.nameUUIDFromBytes(appName.getBytes(StandardCharsets.UTF_8));
            GBDeviceApp.Type appType;
            if (installedApplications.get(i).getAppName().endsWith("App")) {
                appType = GBDeviceApp.Type.APP_GENERIC;
            } else {
                appType = GBDeviceApp.Type.WATCHFACE;
            }
            appInfoEvent.apps[i] = new GBDeviceApp(appUUID, appName, "(unknown)", appVersion, appType);
        }
        getDeviceSupport().evaluateGBDeviceEvent(appInfoEvent);
    }

    private void uploadWidgets() {
        ArrayList<Widget> systemWidgets = new ArrayList<>(widgets.size());
        for (Widget widget : widgets) {
            if (!(widget instanceof CustomWidget) && !widget.getWidgetType().isCustom())
                systemWidgets.add(widget);
        }
        queueWrite(new WidgetsPutRequest(systemWidgets.toArray(new Widget[0]), this));
    }

    private void renderWidgets() {
        Version firmwareVersion = getCleanFWVersion();
        if (firmwareVersion != null && firmwareVersion.compareTo(new Version("1.0.2.20")) >= 0) {
            return; // this does not work on newer firmware versions
        }
        Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(getDeviceSupport().getDevice().getAddress()));
        boolean forceWhiteBackground = prefs.getBoolean("force_white_color_scheme", false);
        boolean drawCircles = prefs.getBoolean("widget_draw_circles", false);

        Bitmap circleBitmap = null;
        if (drawCircles) {
            circleBitmap = Bitmap.createBitmap(76, 76, Bitmap.Config.ARGB_8888);
            Canvas circleCanvas = new Canvas(circleBitmap);
            Paint circlePaint = new Paint();
            circlePaint.setAntiAlias(true);
            circlePaint.setColor(forceWhiteBackground ? Color.WHITE : Color.BLACK);
            circlePaint.setStyle(Paint.Style.FILL);
            circlePaint.setStrokeWidth(3);
            circleCanvas.drawCircle(38, 38, 35, circlePaint);

            circlePaint.setColor(forceWhiteBackground ? Color.BLACK : Color.WHITE);
            circlePaint.setStyle(Paint.Style.STROKE);
            circlePaint.setStrokeWidth(3);
            circleCanvas.drawCircle(38, 38, 35, circlePaint);
        }

        try {
            ArrayList<AssetImage> widgetImages = new ArrayList<>();

            if (this.backGroundImage != null) {
                widgetImages.add(this.backGroundImage);
            }


            for (int i = 0; i < widgets.size(); i++) {
                Widget w = widgets.get(i);
                if (!(w instanceof CustomWidget)) {
                    if (w.getWidgetType() == Widget.WidgetType.LAST_NOTIFICATION) {
                        Bitmap widgetBitmap = Bitmap.createBitmap(76, 76, Bitmap.Config.ARGB_8888);
                        Canvas widgetCanvas = new Canvas(widgetBitmap);
                        if (drawCircles) {
                            widgetCanvas.drawBitmap(circleBitmap, 0, 0, null);
                        }

                        Paint p = new Paint();
                        p.setStyle(Paint.Style.FILL);
                        p.setTextSize(10);
                        p.setColor(Color.WHITE);

                        if (this.lastPostedApp != null) {

                            Bitmap icon = Bitmap.createScaledBitmap(appIconCache.get(this.lastPostedApp), 40, 40, true);

                            if (icon != null) {

                                widgetCanvas.drawBitmap(
                                        icon,
                                        (float) (38 - (icon.getWidth() / 2.0)),
                                        (float) (38 - (icon.getHeight() / 2.0)),
                                        null
                                );
                            }
                        }

                        widgetImages.add(AssetImageFactory.createAssetImage(
                                widgetBitmap,
                                true,
                                w.getAngle(),
                                w.getDistance(),
                                1
                        ));
                    } else if (drawCircles) {
                        widgetImages.add(AssetImageFactory.createAssetImage(
                                circleBitmap,
                                true,
                                w.getAngle(),
                                w.getDistance(),
                                1
                        ));
                    }
                    continue;
                }

                CustomWidget widget = (CustomWidget) w;

                Bitmap widgetBitmap = Bitmap.createBitmap(76, 76, Bitmap.Config.ARGB_8888);

                Canvas widgetCanvas = new Canvas(widgetBitmap);

                if (drawCircles) {
                    widgetCanvas.drawBitmap(circleBitmap, 0, 0, null);
                }

                for (CustomWidgetElement element : widget.getElements()) {
                    if (element.getWidgetElementType() == CustomWidgetElement.WidgetElementType.TYPE_BACKGROUND) {
                        File imageFile = new File(element.getValue());

                        if (!imageFile.exists() || !imageFile.isFile()) {
                            LOG.debug("Image file " + element.getValue() + " not found");
                            continue;
                        }
                        Bitmap imageBitmap = BitmapFactory.decodeFile(element.getValue());
                        if (imageBitmap == null) {
                            LOG.debug("image file " + element.getValue() + " could not be decoded");
                            continue;
                        }
                        Bitmap scaledBitmap = Bitmap.createScaledBitmap(imageBitmap, 76, 76, false);

                        widgetCanvas.drawBitmap(
                                scaledBitmap,
                                0,
                                0,
                                null);
                        break;
                    }
                }

                for (CustomWidgetElement element : widget.getElements()) {
                    if (element.getWidgetElementType() == CustomWidgetElement.WidgetElementType.TYPE_TEXT) {
                        Paint textPaint = new Paint();
                        textPaint.setStrokeWidth(4);
                        textPaint.setTextSize(17f);
                        textPaint.setStyle(Paint.Style.FILL);
                        textPaint.setColor(forceWhiteBackground ? Color.BLACK : Color.WHITE);
                        textPaint.setTextAlign(Paint.Align.CENTER);

                        widgetCanvas.drawText(element.getValue(), element.getX(), element.getY() - (textPaint.descent() + textPaint.ascent()) / 2f, textPaint);
                    } else if (element.getWidgetElementType() == CustomWidgetElement.WidgetElementType.TYPE_IMAGE) {
                        Bitmap imageBitmap = BitmapFactory.decodeFile(element.getValue());

                        widgetCanvas.drawBitmap(imageBitmap, element.getX() - imageBitmap.getWidth() / 2f, element.getY() - imageBitmap.getHeight() / 2f, null);
                    }
                }
                widgetImages.add(AssetImageFactory.createAssetImage(
                        widgetBitmap,
                        true,
                        widget.getAngle(),
                        widget.getDistance(),
                        1
                ));
            }


            AssetImage[] images = widgetImages.toArray(new AssetImage[0]);

            ArrayList<AssetImage> pushFiles = new ArrayList<>(4);
            imgloop:
            for (AssetImage image : images) {
                for (AssetImage pushedImage : pushFiles) {
                    // no need to send same file multiple times, filtering by name since name is hash
                    if (image.getFileName().equals(pushedImage.getFileName())) continue imgloop;
                }
                pushFiles.add(image);
            }

            if (pushFiles.size() > 0) {
                queueWrite(new AssetFilePutRequest(
                        pushFiles.toArray(new AssetImage[0]),
                        FileHandle.ASSET_BACKGROUND_IMAGES,
                        this
                ));
            }
            // queueWrite(new FileDeleteRequest((short) 0x0503));
            queueWrite(new ImagesSetRequest(
                    images,
                    this
            ));
        } catch (IOException e) {
            LOG.error("Error while rendering widgets", e);
        }
    }

    private void handleFileDownload(FileHandle handle, byte[] file) {
        Intent resultIntent = new Intent(QHybridSupport.QHYBRID_ACTION_DOWNLOADED_FILE);
        File outputFile = new File(getContext().getExternalFilesDir("download"), handle.name() + "_" + System.currentTimeMillis() + ".bin");
        try {
            FileOutputStream fos = new FileOutputStream(outputFile);
            fos.write(file);
            fos.close();
            resultIntent.putExtra("EXTRA_SUCCESS", true);
            resultIntent.putExtra("EXTRA_PATH", outputFile.getAbsolutePath());
        } catch (IOException e) {
            LOG.error("Error while downloading file", e);
            resultIntent.putExtra("EXTRA_SUCCESS", false);
        }
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(resultIntent);
    }

    @Override
    public void uploadFileGenerateHeader(FileHandle handle, String filePath, boolean fileIsEncrypted) {
        final Intent resultIntent = new Intent(QHybridSupport.QHYBRID_ACTION_UPLOADED_FILE);
        byte[] fileData;

        try {
            FileInputStream fis = new FileInputStream(filePath);
            fileData = new byte[fis.available()];
            fis.read(fileData);
            fis.close();
        } catch (IOException e) {
            LOG.error("Error while reading file", e);
            resultIntent.putExtra("EXTRA_SUCCESS", false);
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(resultIntent);
            return;
        }

        queueWrite(new FilePutRequest(handle, fileData, this) {
            @Override
            public void onFilePut(boolean success) {
                resultIntent.putExtra("EXTRA_SUCCESS", success);
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(resultIntent);
            }
        });
    }

    @Override
    public void uploadFileIncludesHeader(String filePath) {
        final Intent resultIntent = new Intent(QHybridSupport.QHYBRID_ACTION_UPLOADED_FILE);
        try {
            FileInputStream fis = new FileInputStream(filePath);
            uploadFileIncludesHeader(fis);
            fis.close();
        } catch (Exception e) {
            LOG.error("Error while uploading file", e);
            resultIntent.putExtra("EXTRA_SUCCESS", false);
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(resultIntent);
        }
    }

    private void uploadFileIncludesHeader(InputStream fis) throws IOException {
        final Intent resultIntent = new Intent(QHybridSupport.QHYBRID_ACTION_UPLOADED_FILE);
        byte[] fileData = new byte[fis.available()];
        fis.read(fileData);

        short handleBytes = (short) (fileData[0] & 0xFF | ((fileData[1] & 0xFF) << 8));
        FileHandle handle = FileHandle.fromHandle(handleBytes);

        if (handle == null) {
            throw new RuntimeException("unknown handle");
        }

        queueWrite(new FilePutRawRequest(handle, fileData, this) {
            @Override
            public void onFilePut(boolean success) {
                resultIntent.putExtra("EXTRA_SUCCESS", success);
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(resultIntent);
            }
        });

        if (handle == FileHandle.APP_CODE) {
            listApplications();
        }
    }

    @Override
    public void downloadFile(final FileHandle handle, boolean fileIsEncrypted) {
        if (fileIsEncrypted) {
            queueWrite((FileEncryptedInterface) new FileEncryptedGetRequest(handle, this) {
                @Override
                public void handleFileData(byte[] fileData) {
                    LOG.debug("downloaded encrypted file");
                    handleFileDownload(handle, fileData);
                }
            });
        } else {
            queueWrite(new FileGetRawRequest(handle, this) {
                @Override
                public void handleFileRawData(byte[] fileData) {
                    LOG.debug("downloaded regular file");
                    handleFileDownload(handle, fileData);
                }
            });
        }
    }

    @Override
    public void setWidgetContent(String widgetID, String content, boolean renderOnWatch) {
        boolean update = false;
        for (Widget widget : widgets) {
            if (!(widget instanceof CustomWidget)) continue;
            if (((CustomWidget) widget).updateElementValue(widgetID, content)) update = true;
        }

        if (renderOnWatch && update) renderWidgets();
    }

    private void queueWrite(final FileEncryptedInterface request) {
        try {
            queueWrite(new VerifyPrivateKeyRequest(
                    this.getSecretKey(),
                    this
            ) {
                @Override
                protected void handleAuthenticationResult(boolean success) {
                    if (success) {
                        GB.log("success auth", GB.INFO, null);
                        queueWrite((FossilRequest) request, true);
                    }
                }
            });
        } catch (IllegalAccessException e) {
            GB.toast("error getting key: " + e.getMessage(), Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }

    @Override
    public void onInstallApp(Uri uri) {
        FossilFileReader fossilFile;
        try {
            fossilFile = new FossilFileReader(uri, getContext());
            if (fossilFile.isFirmware()) {
                super.onInstallApp(uri);
            } else if (fossilFile.isApp() || fossilFile.isWatchface()) {
                UriHelper uriHelper = UriHelper.get(uri, getContext());
                InputStream in = new BufferedInputStream(uriHelper.openInputStream());
                uploadFileIncludesHeader(in);
                in.close();
            }
        } catch (Exception ignored) {
        }
    }

    private void negotiateSymmetricKey() {
        try {
            queueWrite(new VerifyPrivateKeyRequest(
                    this.getSecretKey(),
                    this
            ) {
                @Override
                protected void handleAuthenticationResult(boolean success) {
                    FossilHRWatchAdapter.this.handleAuthenticationResult(success);
                }
            });
        } catch (IllegalAccessException e) {
            GB.toast("error getting key: " + e.getMessage(), Toast.LENGTH_LONG, GB.ERROR, e);
            this.handleAuthenticationResult(false);
        }
    }

    @Override
    public void setTime() {
        if (connectionMode == CONNECTION_MODE.NOT_AUTHENTICATED) {
            GB.toast(getContext().getString(R.string.fossil_hr_unavailable_unauthed), Toast.LENGTH_LONG, GB.ERROR);
            return;
        }
        queueWrite(
                (FileEncryptedInterface) new ConfigurationPutRequest(this.generateTimeConfigItemNow(), this)
        );
    }

    @Override
    public void setMusicInfo(MusicSpec musicSpec) {
        if (
                currentSpec != null
                        && currentSpec.album.equals(musicSpec.album)
                        && currentSpec.artist.equals(musicSpec.artist)
                        && currentSpec.track.equals(musicSpec.track)
        ) return;
        currentSpec = musicSpec;
        try {
            queueWrite(new MusicInfoSetRequest(
                    musicSpec.artist,
                    musicSpec.album,
                    musicSpec.track,
                    this
            ));
        } catch (BufferOverflowException e) {
            GB.log("musicInfo: " + musicSpec, GB.ERROR, e);
        }
    }

    @Override
    public void setMusicState(MusicStateSpec stateSpec) {
        super.setMusicState(stateSpec);

        queueWrite(new MusicControlRequest(
                stateSpec.state == MusicStateSpec.STATE_PLAYING ? MUSIC_PHONE_REQUEST.MUSIC_REQUEST_SET_PLAYING : MUSIC_PHONE_REQUEST.MUSIC_REQUEST_SET_PAUSED
        ));
    }

    @Override
    public void updateWidgets() {
        loadWidgets();
        renderWidgets();
    }

    @Override
    public void onFetchActivityData() {
        if (connectionMode == CONNECTION_MODE.NOT_AUTHENTICATED) {
            GB.toast(getContext().getString(R.string.fossil_hr_unavailable_unauthed), Toast.LENGTH_LONG, GB.ERROR);
            return;
        }

        syncSettings();

        queueWrite(new FileLookupRequest(FileHandle.ACTIVITY_FILE, this) {
            @Override
            public void handleFileLookup(final short fileHandle) {
                queueWrite((FileEncryptedInterface) new FileEncryptedGetRequest(fileHandle, FossilHRWatchAdapter.this) {
                    @Override
                    public void handleFileData(byte[] fileData) {
                        try (DBHandler dbHandler = GBApplication.acquireDB()) {
                            ActivityFileParser parser = new ActivityFileParser();
                            ArrayList<ActivityEntry> entries = parser.parseFile(fileData);
                            HybridHRActivitySampleProvider provider = new HybridHRActivitySampleProvider(getDeviceSupport().getDevice(), dbHandler.getDaoSession());

                            HybridHRActivitySample[] samples = new HybridHRActivitySample[entries.size()];

                            Long userId = DBHelper.getUser(dbHandler.getDaoSession()).getId();
                            Long deviceId = DBHelper.getDevice(getDeviceSupport().getDevice(), dbHandler.getDaoSession()).getId();
                            for (int i = 0; i < entries.size(); i++) {
                                samples[i] = entries.get(i).toDAOActivitySample(userId, deviceId);
                            }

                            provider.addGBActivitySamples(samples);

                            if (saveRawActivityFiles) {
                                writeFile(String.valueOf(System.currentTimeMillis()), fileData);
                            }
                            queueWrite(new FileDeleteRequest(fileHandle));
                            GB.toast(getContext().getString(R.string.fossil_hr_synced_activity_data), Toast.LENGTH_SHORT, GB.INFO);
                        } catch (Exception ex) {
                            GB.toast(getContext(), "Error saving steps data: " + ex.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
                            GB.updateTransferNotification(null, "Data transfer failed", false, 0, getContext());
                        }
                        getDeviceSupport().getDevice().sendDeviceUpdateIntent(getContext());
                    }
                });
            }

            @Override
            public void handleFileLookupError(FILE_LOOKUP_ERROR error) {
                if (error == FILE_LOOKUP_ERROR.FILE_EMPTY) {
                    GB.toast("activity file empty yet", Toast.LENGTH_LONG, GB.ERROR);
                } else {
                    throw new RuntimeException("strange lookup stuff");
                }
                getDeviceSupport().getDevice().sendDeviceUpdateIntent(getContext());
            }
        });
    }

    private void writeFile(String fileName, byte[] value) {
        File activityDir = new File(getContext().getExternalFilesDir(null), "activity_hr");
        activityDir.mkdir();
        File f = new File(activityDir, fileName);
        try {
            f.createNewFile();
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(value);
            fos.close();
            GB.toast("saved file data", Toast.LENGTH_SHORT, GB.INFO);
        } catch (IOException e) {
            GB.log("file error", GB.ERROR, e);
        }
    }

    private void syncSettings() {
        if (connectionMode == CONNECTION_MODE.NOT_AUTHENTICATED) {
            GB.toast(getContext().getString(R.string.fossil_hr_unavailable_unauthed), Toast.LENGTH_LONG, GB.ERROR);
            return;
        }

        queueWrite((FileEncryptedInterface) new ConfigurationGetRequest(this));
    }

    @Override
    public void setActivityHand(double progress) {
        // super.setActivityHand(progress);
    }

    private boolean isNotificationWidgetVisible() {
        for (Widget widget : widgets) {
            if (widget.getWidgetType() == Widget.WidgetType.LAST_NOTIFICATION) {
                return true;
            }
        }
        return false;
    }

    public boolean playRawNotification(NotificationSpec notificationSpec) {
        String sourceAppId = notificationSpec.sourceAppId;
        String senderOrTitle = StringUtils.getFirstOf(notificationSpec.sender, notificationSpec.title);

        // Retrieve and store notification or app icon
        if (sourceAppId != null) {
            if (appIconCache.get(sourceAppId) == null) {
                try {
                    Drawable icon = null;
                    if (notificationSpec.iconId != 0) {
                        Context sourcePackageContext = getContext().createPackageContext(sourceAppId, 0);
                        icon = sourcePackageContext.getResources().getDrawable(notificationSpec.iconId);
                    }
                    if (icon == null) {
                        PackageManager pm = getContext().getPackageManager();
                        icon = pm.getApplicationIcon(sourceAppId);
                    }
                    Bitmap iconBitmap = convertDrawableToBitmap(icon);
                    appIconCache.put(sourceAppId, iconBitmap);
                    setNotificationConfigurations();
                } catch (PackageManager.NameNotFoundException e) {
                    LOG.error("Error while updating notification icons", e);
                }
            }
        }

        boolean packageFound = false;

        // Send notification to watch
        try {
            for (NotificationHRConfiguration configuration : this.notificationConfigurations) {
                if (configuration.getPackageName().equals(sourceAppId)) {
                    LOG.info("Package found in notificationConfigurations, using custom icon: " + sourceAppId);
                    queueWrite(new PlayTextNotificationRequest(sourceAppId, senderOrTitle, notificationSpec.body, notificationSpec.getId(), this));
                    packageFound = true;
                }
            }

            if(!packageFound) {
                LOG.info("Package not found in notificationConfigurations, using generic icon: " + sourceAppId);
                queueWrite(new PlayTextNotificationRequest("generic", senderOrTitle, notificationSpec.body, notificationSpec.getId(), this));
            }
        } catch (Exception e) {
            LOG.error("Error while forwarding notification", e);
        }

        // Update notification icon custom widget
        if (isNotificationWidgetVisible() && sourceAppId != null) {
            if (!sourceAppId.equals(this.lastPostedApp)) {
                this.lastPostedApp = sourceAppId;
                renderWidgets();
            }
        }
        return true;
    }

    @Override
    public void onDeleteNotification(int id) {
        super.onDeleteNotification(id);

        // send notification dismissal message to watch
        try {
            queueWrite(new DismissTextNotificationRequest(id, this));
        } catch (Exception e) {
            LOG.error("Error while dismissing notification", e);
        }

        // only delete app icon when no notification of said app is present
        for (String app : NotificationListener.notificationStack) {
            if (app.equals(this.lastPostedApp)) return;
        }

        this.lastPostedApp = null;

        if (isNotificationWidgetVisible()) {
            renderWidgets();
        }
    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
        super.onSetCallState(callSpec);
        String[] quickReplies = getQuickReplies();
        boolean quickRepliesEnabled = quickReplies.length > 0 && callSpec.number != null && callSpec.number.matches("^\\+(?:[0-9] ?){6,14}[0-9]$");
        if (callSpec.command == CallSpec.CALL_INCOMING) {
            currentCallSpec = callSpec;
            queueWrite(new PlayCallNotificationRequest(StringUtils.getFirstOf(callSpec.name, callSpec.number), true, quickRepliesEnabled, this));
        } else {
            currentCallSpec = null;
            queueWrite(new PlayCallNotificationRequest(StringUtils.getFirstOf(callSpec.name, callSpec.number), false, quickRepliesEnabled, this));
        }
    }

    // this method is based on the one from AppMessageHandlerYWeather.java
    private int getIconForConditionCode(int conditionCode, boolean isNight) {
        final int CLEAR_DAY = 0;
        final int CLEAR_NIGHT = 1;
        final int CLOUDY = 2;
        final int PARTLY_CLOUDY_DAY = 3;
        final int PARTLY_CLOUDY_NIGHT = 4;
        final int RAIN = 5;
        final int SNOW = 6;
        final int SNOW_2 = 7; // same as 6?
        final int THUNDERSTORM = 8;
        final int CLOUDY_2 = 9; // same as 2?
        final int WINDY = 10;

        if (conditionCode == 800 || conditionCode == 951) {
            return isNight ? CLEAR_NIGHT : CLEAR_DAY;
        } else if (conditionCode > 800 && conditionCode < 900) {
            return isNight ? PARTLY_CLOUDY_NIGHT : PARTLY_CLOUDY_DAY;
        } else if (conditionCode >= 300 && conditionCode < 400) {
            return RAIN; // drizzle mapped to rain
        } else if (conditionCode >= 500 && conditionCode < 600) {
            return RAIN;
        } else if (conditionCode >= 700 && conditionCode < 732) {
            return CLOUDY;
        } else if (conditionCode == 741 || conditionCode == 751 || conditionCode == 761 || conditionCode == 762) {
            return CLOUDY; // fog mapped to cloudy
        } else if (conditionCode == 771) {
            return CLOUDY; // squalls mapped to cloudy
        } else if (conditionCode == 781) {
            return WINDY; // tornato mapped to windy
        } else if (conditionCode >= 200 && conditionCode < 300) {
            return THUNDERSTORM;
        } else if (conditionCode >= 600 && conditionCode <= 602) {
            return SNOW;
        } else if (conditionCode >= 611 && conditionCode <= 622) {
            return RAIN;
        } else if (conditionCode == 906) {
            return RAIN; // hail mapped to rain
        } else if (conditionCode >= 907 && conditionCode < 957) {
            return WINDY;
        } else if (conditionCode == 905) {
            return WINDY;
        } else if (conditionCode == 900) {
            return WINDY;
        } else if (conditionCode == 901 || conditionCode == 902 || conditionCode == 962) {
            return WINDY;
        }
        return isNight ? CLEAR_NIGHT : CLEAR_DAY;
    }

    @Override
    public void onSendWeather(WeatherSpec weatherSpec) {
        long ts = System.currentTimeMillis();
        ts /= 1000;
        try {
            JSONObject responseObject = new JSONObject()
                    .put("res", new JSONObject()
                            .put("id", 0) // seems the id does not matter?
                            .put("set", new JSONObject()
                                    .put("weatherInfo", new JSONObject()
                                            .put("alive", ts + 60 * 60)
                                            .put("unit", "c") // FIXME: do not hardcode
                                            .put("temp", weatherSpec.currentTemp - 273)
                                            .put("cond_id", getIconForConditionCode(weatherSpec.currentConditionCode, false)) // FIXME do not assume daylight
                                    )
                            )
                    );

            queueWrite(new JsonPutRequest(responseObject, this));

            JSONArray forecastWeekArray = new JSONArray();
            final String[] weekdays = {"", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(weatherSpec.timestamp * 1000L);
            int i = 0;
            for (WeatherSpec.Forecast forecast : weatherSpec.forecasts) {
                cal.add(Calendar.DATE, 1);
                int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
                forecastWeekArray.put(new JSONObject()
                        .put("day", weekdays[dayOfWeek])
                        .put("cond_id", getIconForConditionCode(forecast.conditionCode, false)) // FIXME do not assume daylight
                        .put("high", forecast.maxTemp - 273)
                        .put("low", forecast.minTemp - 273)
                );
                if (++i == 3) break; // max 3
            }

            JSONArray forecastDayArray = new JSONArray();
            final int[] hours = {0, 0, 0};

            for (int hour : hours) {
                forecastDayArray.put(new JSONObject()
                        .put("hour", hour)
                        .put("cond_id", 0)
                        .put("temp", 0)
                );
            }


            JSONObject forecastResponseObject = new JSONObject()
                    .put("res", new JSONObject()
                            .put("id", 0)
                            .put("set", new JSONObject()
                                    .put("weatherApp._.config.locations", new JSONArray()
                                            .put(new JSONObject()
                                                    .put("alive", ts + 60 * 60)
                                                    .put("city", weatherSpec.location)
                                                    .put("unit", "c") // FIXME: do not hardcode
                                                    .put("temp", weatherSpec.currentTemp - 273)
                                                    .put("high", weatherSpec.todayMaxTemp - 273)
                                                    .put("low", weatherSpec.todayMinTemp - 273)
                                                    .put("rain", 0)
                                                    .put("cond_id", getIconForConditionCode(weatherSpec.currentConditionCode, false)) // FIXME do not assume daylight
                                                    .put("forecast_day", forecastDayArray)
                                                    .put("forecast_week", forecastWeekArray)
                                            )
                                    )
                            )
                    );

            queueWrite(new JsonPutRequest(forecastResponseObject, this));

        } catch (JSONException e) {
            LOG.error("JSON exception: ", e);
        }
    }

    @Override
    public void factoryReset() {
        queueWrite(new FactoryResetRequest());
    }

    @Override
    public void onTestNewFunction() {
        /*queueWrite(new ButtonConfigurationPutRequest(
                new String[]{"test"},
                new ButtonConfiguration[]{
                        new ButtonConfiguration(
                            "short_press_release", "stopwatchApp"
                        )
                },
                this
        ));*/
        /*queueWrite(new FileLookupAndGetRequest(FileHandle.APP_CODE, this) {
            @Override
            public void handleFileData(byte[] fileData) {
                log("test");
            }

            @Override
            public void handleFileLookupError(FILE_LOOKUP_ERROR error) {

            }
        });*/
        /*queueWrite(new TranslationsGetRequest(this){
            @Override
            public void handleTranslations(TranslationData translationData) {
                translationData.replaceByOriginal("ON", "oi m8");
                translationData.replaceByOriginal("OFF", "nah go away");
                translationData.replaceByOriginal("Device is about to reset.", "oh frick no no no");
                translationData.replaceByOriginal("Release button to stop reset.", "please don't let me die like that");
                translationData.replaceByOriginal("Serial number", "Gadgetbridge :)");
                translationData.replaceByOriginal("Dial Info", "Widgets");
                TranslationsPutRequest request = new TranslationsPutRequest(translationData, FossilHRWatchAdapter.this);
                queueWrite(request);
            }
        });*/
    }

    public byte[] getSecretKey() throws IllegalAccessException {
        byte[] authKeyBytes = new byte[16];

        SharedPreferences sharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(getDeviceSupport().getDevice().getAddress());

        String authKey = sharedPrefs.getString("authkey", null);
        if (authKey != null && !authKey.isEmpty()) {
            authKey = authKey.replace(" ", "");
            authKey = authKey.replace("0x", "");
            if (authKey.length() != 32) {
                throw new IllegalAccessException("Key should be 16 bytes long as hex string");
            }
            byte[] srcBytes = GB.hexStringToByteArray(authKey);

            System.arraycopy(srcBytes, 0, authKeyBytes, 0, Math.min(srcBytes.length, 16));
        }

        return authKeyBytes;
    }

    public void setPhoneRandomNumber(byte[] phoneRandomNumber) {
        this.phoneRandomNumber = phoneRandomNumber;
    }

    public byte[] getPhoneRandomNumber() {
        return phoneRandomNumber;
    }

    public void setWatchRandomNumber(byte[] watchRandomNumber) {
        this.watchRandomNumber = watchRandomNumber;
    }

    public byte[] getWatchRandomNumber() {
        return watchRandomNumber;
    }

    @Override
    public void overwriteButtons(String jsonConfigString) {
        try {
            SharedPreferences prefs = getDeviceSpecificPreferences();

            String singlePressEvent = "short_press_release";

            Version firmwareVersion = getCleanFWVersion();
            if (firmwareVersion != null && firmwareVersion.compareTo(new Version("1.0.2.19")) < 0) {
                singlePressEvent = "single_click";
            }
            ArrayList<ButtonConfiguration> configs = new ArrayList<>(5);
            configs.add(new ButtonConfiguration("top_" + singlePressEvent, prefs.getString(DeviceSettingsPreferenceConst.PREF_BUTTON_1_FUNCTION_SHORT, "weatherApp")));
            configs.add(new ButtonConfiguration("top_hold", prefs.getString(DeviceSettingsPreferenceConst.PREF_BUTTON_1_FUNCTION_LONG, "weatherApp")));
            // configs.add(new ButtonConfiguration("top_double_click", prefs.getString(DeviceSettingsPreferenceConst.PREF_BUTTON_1_FUNCTION_DOUBLE, "weatherApp")));
            configs.add(new ButtonConfiguration("middle_" + singlePressEvent, prefs.getString(DeviceSettingsPreferenceConst.PREF_BUTTON_2_FUNCTION_SHORT, "commuteApp")));
            // configs.add(new ButtonConfiguration("middle_hold", prefs.getString(DeviceSettingsPreferenceConst.PREF_BUTTON_2_FUNCTION_LONG, "commuteApp")));
            // configs.add(new ButtonConfiguration("middle_double_click", prefs.getString(DeviceSettingsPreferenceConst.PREF_BUTTON_2_FUNCTION_DOUBLE, "commuteApp")));
            configs.add(new ButtonConfiguration("bottom_" + singlePressEvent, prefs.getString(DeviceSettingsPreferenceConst.PREF_BUTTON_3_FUNCTION_SHORT, "musicApp")));
            configs.add(new ButtonConfiguration("bottom_hold", prefs.getString(DeviceSettingsPreferenceConst.PREF_BUTTON_3_FUNCTION_LONG, "musicApp")));
            // configs.add(new ButtonConfiguration("bottom_double_click", prefs.getString(DeviceSettingsPreferenceConst.PREF_BUTTON_3_FUNCTION_DOUBLE, "musicApp")));

            // filter out all apps not installed on watch
            ArrayList<ButtonConfiguration> availableConfigs = new ArrayList<>();
            outerLoop:
            for (ButtonConfiguration config : configs) {
                for (ApplicationInformation installedApp : installedApplications) {
                    if (installedApp.getAppName().equals(config.getAction())) {
                        availableConfigs.add(config);
                        continue outerLoop;
                    }
                }
            }

            queueWrite(new ButtonConfigurationPutRequest(
                    availableConfigs.toArray(new ButtonConfiguration[0]),
                    this
            ));

            for (ApplicationInformation info : installedApplications) {
                if (info.getAppName().equals("commuteApp")) {
                    JSONArray jsonArray = new JSONArray(
                            GBApplication.getPrefs().getString(CommuteActionsActivity.CONFIG_KEY_Q_ACTIONS, "[]")
                    );
                    String[] menuItems = new String[jsonArray.length()];
                    for (int i = 0; i < jsonArray.length(); i++)
                        menuItems[i] = jsonArray.getString(i);
                    queueWrite(new CommuteConfigPutRequest(menuItems, this));
                    break;
                }
            }
        } catch (JSONException e) {
            LOG.error("Error while configuring buttons", e);
        }
    }

    @Override
    public void onSendConfiguration(String config) {
        switch (config) {
            case DeviceSettingsPreferenceConst.PREF_BUTTON_1_FUNCTION_SHORT:
            case DeviceSettingsPreferenceConst.PREF_BUTTON_2_FUNCTION_SHORT:
            case DeviceSettingsPreferenceConst.PREF_BUTTON_3_FUNCTION_SHORT:
            case DeviceSettingsPreferenceConst.PREF_BUTTON_1_FUNCTION_LONG:
            case DeviceSettingsPreferenceConst.PREF_BUTTON_2_FUNCTION_LONG:
            case DeviceSettingsPreferenceConst.PREF_BUTTON_3_FUNCTION_LONG:
            case DeviceSettingsPreferenceConst.PREF_BUTTON_1_FUNCTION_DOUBLE:
            case DeviceSettingsPreferenceConst.PREF_BUTTON_2_FUNCTION_DOUBLE:
            case DeviceSettingsPreferenceConst.PREF_BUTTON_3_FUNCTION_DOUBLE:
                overwriteButtons(null);
                break;
            case DeviceSettingsPreferenceConst.PREF_VIBRATION_STRENGH_PERCENTAGE:
                setVibrationStrength();
                break;
            case "force_white_color_scheme":
                loadBackground();
                // not break here
            case "widget_draw_circles": {
                renderWidgets();
                break;
            }
            case DeviceSettingsPreferenceConst.PREF_HYBRID_HR_SAVE_RAW_ACTIVITY_FILES: {
                saveRawActivityFiles = getDeviceSpecificPreferences().getBoolean("save_raw_activity_files", false);
                break;
            }
            case SettingsActivity.PREF_MEASUREMENT_SYSTEM:
                setUnitsConfig();
                break;
        }
    }

    @Override
    public void handleHeartRateCharacteristic(BluetoothGattCharacteristic characteristic) {
        super.handleHeartRateCharacteristic(characteristic);

        byte[] value = characteristic.getValue();

        int heartRate = value[1];

        LOG.debug("heart rate: " + heartRate);
    }

    @Override
    protected void handleBackgroundCharacteristic(BluetoothGattCharacteristic characteristic) {
        super.handleBackgroundCharacteristic(characteristic);

        byte[] value = characteristic.getValue();

        byte requestType = value[1];

        if (requestType == (byte) 0x04) {
            if (value[7] == 0x00 || value[7] == 0x01) {
                handleCallRequest(value);
            } else if (value[7] == 0x02) {
                handleDeleteNotification(value);
            } else if (value[7] == 0x03) {
                handleQuickReplyRequest(value);
            }
        } else if (requestType == (byte) 0x05) {
            handleMusicRequest(value);
        } else if (requestType == (byte) 0x01) {
            int eventId = value[2];
            LOG.info("got event id " + eventId);
            try {
                String jsonString = new String(value, 3, value.length - 3);
                // logger.info(jsonString);
                JSONObject requestJson = new JSONObject(jsonString);

                JSONObject request = requestJson.getJSONObject("req");
                int requestId = request.getInt("id");

                if (request.has("ringMyPhone")) {
                    String action = request.getJSONObject("ringMyPhone").getString("action");
                    LOG.info("got ringMyPhone request; " + action);
                    GBDeviceEventFindPhone findPhoneEvent = new GBDeviceEventFindPhone();

                    JSONObject responseObject = new JSONObject()
                            .put("res", new JSONObject()
                                    .put("id", requestId)
                                    .put("set", new JSONObject()
                                            .put("ringMyPhone", new JSONObject()
                                            )
                                    )
                            );

                    if ("on".equals(action)) {
                        findPhoneEvent.event = GBDeviceEventFindPhone.Event.START;
                        getDeviceSupport().evaluateGBDeviceEvent(findPhoneEvent);
                        responseObject
                                .getJSONObject("res")
                                .getJSONObject("set")
                                .getJSONObject("ringMyPhone")
                                .put("result", "on");
                        queueWrite(new JsonPutRequest(responseObject, this));
                    } else if ("off".equals(action)) {
                        findPhoneEvent.event = GBDeviceEventFindPhone.Event.STOP;
                        getDeviceSupport().evaluateGBDeviceEvent(findPhoneEvent);
                        responseObject
                                .getJSONObject("res")
                                .getJSONObject("set")
                                .getJSONObject("ringMyPhone")
                                .put("result", "off");
                        queueWrite(new JsonPutRequest(responseObject, this));
                    }
                } else if (request.has("weatherInfo") || request.has("weatherApp._.config.locations")) {
                    LOG.info("Got weatherInfo request");
                    WeatherSpec weatherSpec = Weather.getInstance().getWeatherSpec();
                    if (weatherSpec != null) {
                        onSendWeather(weatherSpec);
                    } else {
                        LOG.info("no weather data available  - ignoring request");
                    }
                } else if (request.has("commuteApp._.config.commute_info")) {
                    String action = request.getJSONObject("commuteApp._.config.commute_info")
                            .getString("dest");

                    String startStop = request.getJSONObject("commuteApp._.config.commute_info")
                            .getString("action");

                    if (startStop.equals("stop")) {
                        // overwriteButtons(null);
                        return;
                    }

                    queueWrite(new SetCommuteMenuMessage(getContext().getString(R.string.fossil_hr_commute_processing), false, this));

                    Intent menuIntent = new Intent(QHybridSupport.QHYBRID_EVENT_COMMUTE_MENU);
                    menuIntent.putExtra("EXTRA_ACTION", action);
                    getContext().sendBroadcast(menuIntent);
                } else if (request.has("master._.config.app_status")) {
                    queueWrite(new ConfirmAppStatusRequest(requestId, this));
                } else {
                    LOG.warn("Unhandled request from watch: " + requestJson.toString());
                }
            } catch (JSONException e) {
                LOG.error("Error while handling received characteristic", e);
            }
        }
    }

    private void handleDeleteNotification(byte[] value) {
        ByteBuffer buffer = ByteBuffer.wrap(value);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        int notifId = buffer.getInt(3);

        Intent deleteIntent = new Intent(NotificationListener.ACTION_DISMISS);
        deleteIntent.putExtra("handle", (long) notifId);
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(deleteIntent);
    }

    private void handleCallRequest(byte[] value) {
        boolean acceptCall = value[7] == (byte) 0x00;
        queueWrite(new PlayCallNotificationRequest("", false, false, this));

        GBDeviceEventCallControl callControlEvent = new GBDeviceEventCallControl();
        callControlEvent.event = acceptCall ? GBDeviceEventCallControl.Event.START : GBDeviceEventCallControl.Event.REJECT;

        getDeviceSupport().evaluateGBDeviceEvent(callControlEvent);
    }

    private void handleQuickReplyRequest(byte[] value) {
        if (currentCallSpec == null) {
            return;
        }
        String[] quickReplies = getQuickReplies();
        byte callId = value[3];
        byte replyChoice = value[8];
        if (replyChoice >= quickReplies.length) {
            return;
        }
        GBDeviceEventNotificationControl devEvtNotificationControl = new GBDeviceEventNotificationControl();
        devEvtNotificationControl.handle = callId;
        devEvtNotificationControl.phoneNumber = currentCallSpec.number;
        devEvtNotificationControl.reply = quickReplies[replyChoice];
        devEvtNotificationControl.event = GBDeviceEventNotificationControl.Event.REPLY;
        getDeviceSupport().evaluateGBDeviceEvent(devEvtNotificationControl);
        queueWrite(new QuickReplyConfirmationPutRequest(callId));
    }

    private void handleMusicRequest(byte[] value) {
        byte command = value[3];
        LOG.info("got music command: " + command);
        MUSIC_WATCH_REQUEST request = MUSIC_WATCH_REQUEST.fromCommandByte(command);

        GBDeviceEventMusicControl deviceEventMusicControl = new GBDeviceEventMusicControl();
        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PLAY;

        // TODO add skipping/seeking

        switch (request) {
            case MUSIC_REQUEST_PLAY_PAUSE: {
                queueWrite(new MusicControlRequest(MUSIC_PHONE_REQUEST.MUSIC_REQUEST_PLAY_PAUSE));
                deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PLAYPAUSE;
                break;
            }
            case MUSIC_REQUEST_NEXT: {
                queueWrite(new MusicControlRequest(MUSIC_PHONE_REQUEST.MUSIC_REQUEST_NEXT));
                deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.NEXT;
                break;
            }
            case MUSIC_REQUEST_PREVIOUS: {
                queueWrite(new MusicControlRequest(MUSIC_PHONE_REQUEST.MUSIC_REQUEST_PREVIOUS));
                deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PREVIOUS;
                break;
            }
            case MUSIC_REQUEST_LOUDER: {
                queueWrite(new MusicControlRequest(MUSIC_PHONE_REQUEST.MUSIC_REQUEST_LOUDER));
                deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.VOLUMEUP;
                break;
            }
            case MUSIC_REQUEST_QUITER: {
                queueWrite(new MusicControlRequest(MUSIC_PHONE_REQUEST.MUSIC_REQUEST_QUITER));
                deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.VOLUMEDOWN;
                break;
            }
        }

        getDeviceSupport().evaluateGBDeviceEvent(deviceEventMusicControl);
    }

    @Override
    public void setCommuteMenuMessage(String message, boolean finished) {
        queueWrite(new SetCommuteMenuMessage(message, finished, this));
    }

    public byte getJsonIndex() {
        return jsonIndex++;
    }

    private Version getCleanFWVersion() {
        String firmware = getDeviceSupport().getDevice().getFirmwareVersion();
        Matcher matcher = Pattern.compile("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+").matcher(firmware); // DN1.0.2.19r.v5
        if (matcher.find()) {
            firmware = matcher.group(0);
            return new Version(firmware);
        }
        return null;
    }

    public String getInstalledAppNameFromUUID(UUID uuid) {
        for (ApplicationInformation appInfo : installedApplications) {
            if (UUID.nameUUIDFromBytes(appInfo.getAppName().getBytes(StandardCharsets.UTF_8)).equals(uuid)) {
                return appInfo.getAppName();
            }
        }
        return null;
    }
}
