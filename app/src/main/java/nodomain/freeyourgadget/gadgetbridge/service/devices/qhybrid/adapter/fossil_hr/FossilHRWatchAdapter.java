package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil_hr;

import android.bluetooth.BluetoothGattCharacteristic;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventCallControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventFindPhone;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventMusicControl;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.HRConfigActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.HybridHRActivitySampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.NotificationHRConfiguration;
import nodomain.freeyourgadget.gadgetbridge.entities.HybridHRActivitySample;
import nodomain.freeyourgadget.gadgetbridge.externalevents.NotificationListener;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.Weather;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.parser.ActivityEntry;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.parser.ActivityFileParser;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.RequestMtuRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.SetDeviceStateRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.configuration.ConfigurationPutRequest.TimeConfigItem;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FileDeleteRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FileLookupRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.notification.PlayCallNotificationRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.notification.PlayTextNotificationRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.authentication.VerifyPrivateKeyRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.buttons.ButtonConfigurationPutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.configuration.ConfigurationGetRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.configuration.ConfigurationPutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.file.AssetFile;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.file.AssetFilePutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.file.FileEncryptedGetRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.file.FirmwareFilePutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.image.AssetImage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.image.AssetImageFactory;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.image.ImagesSetRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.json.JsonPutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.menu.SetCommuteMenuMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.music.MusicControlRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.music.MusicInfoSetRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.notification.NotificationFilterPutHRRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.widget.CustomBackgroundWidgetElement;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.widget.CustomTextWidgetElement;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.widget.CustomWidget;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.widget.CustomWidgetElement;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.widget.Widget;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.widget.WidgetsPutRequest;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;
import nodomain.freeyourgadget.gadgetbridge.util.UriHelper;

import static nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.music.MusicControlRequest.MUSIC_PHONE_REQUEST;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.music.MusicControlRequest.MUSIC_WATCH_REQUEST;

public class FossilHRWatchAdapter extends FossilWatchAdapter {
    private byte[] phoneRandomNumber;
    private byte[] watchRandomNumber;

    private ArrayList<Widget> widgets = new ArrayList<>();

    private NotificationHRConfiguration[] notificationConfigurations;

    private MusicSpec currentSpec = null;

    int imageNameIndex = 0;
    private byte jsonIndex = 0;

    private AssetImage backGroundImage = null;

    public FossilHRWatchAdapter(QHybridSupport deviceSupport) {
        super(deviceSupport);
    }

    private boolean saveRawActivityFiles = false;

    HashMap<String, Bitmap> appIconCache = new HashMap<>();
    String lastPostedApp = null;

    @Override
    public void initialize() {
        saveRawActivityFiles = getDeviceSpecificPreferences().getBoolean("save_raw_activity_files", false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            queueWrite(new RequestMtuRequest(512));
        }

        queueWrite(new SetDeviceStateRequest(GBDevice.State.AUTHENTICATING));

        negotiateSymmetricKey();

        queueWrite(new SetDeviceStateRequest(GBDevice.State.INITIALIZING));

        loadNotificationConfigurations();
        queueWrite(new NotificationFilterPutHRRequest(this.notificationConfigurations, this));
        setVibrationStrength();

        syncSettings();

        setTime();

        overwriteButtons(null);


        loadBackground();
        loadWidgets();
        // renderWidgets();
        // dunno if there is any point in doing this at start since when no watch is connected the QHybridSupport will not receive any intents anyway

        queueWrite(new SetDeviceStateRequest(GBDevice.State.INITIALIZED));
    }

    private void setVibrationStrength() {
        Prefs prefs = new Prefs(getDeviceSpecificPreferences());
        int vibrationStrengh = prefs.getInt(DeviceSettingsPreferenceConst.PREF_VIBRATION_STRENGH_PERCENTAGE, 2);
        if (vibrationStrengh > 0) {
            vibrationStrengh = (vibrationStrengh + 1) * 25; // Seems 0,50,75,100 are working...
        }
        setVibrationStrength((short) (vibrationStrengh));
    }

    @Override
    public void setVibrationStrength(short strength) {
        negotiateSymmetricKey();
        queueWrite(new ConfigurationPutRequest(new nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.configuration.ConfigurationPutRequest.VibrationStrengthConfigItem((byte) strength), this));
    }

    private void loadNotificationConfigurations() {
        this.notificationConfigurations = new NotificationHRConfiguration[]{
                new NotificationHRConfiguration("generic", 0),
                new NotificationHRConfiguration("call", new byte[]{(byte) 0x80, (byte) 0x00, (byte) 0x59, (byte) 0xB7}, 0)
        };
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
                    logger.error("Backgroundimage error", e2);
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
        Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(getDeviceSupport().getDevice().getAddress()));
        boolean forceWhiteBackground = prefs.getBoolean("force_white_color_scheme", false);
        String fontColor = forceWhiteBackground ? "black" : "default";

        this.widgets.clear();
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
                this.widgets.add(widget);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        uploadWidgets();
    }

    private void uploadWidgets() {
        negotiateSymmetricKey();
        ArrayList<Widget> systemWidgets = new ArrayList<>(widgets.size());
        for (Widget widget : this.widgets) {
            if (!(widget instanceof CustomWidget) && !widget.getWidgetType().isCustom())
                systemWidgets.add(widget);
        }
        queueWrite(new WidgetsPutRequest(systemWidgets.toArray(new Widget[0]), this));
    }

    private void renderWidgets() {
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


            for (int i = 0; i < this.widgets.size(); i++) {
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

                            Bitmap icon = appIconCache.get(this.lastPostedApp);

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
                            logger.debug("Image file " + element.getValue() + " not found");
                            continue;
                        }
                        Bitmap imageBitmap = BitmapFactory.decodeFile(element.getValue());
                        if (imageBitmap == null) {
                            logger.debug("image file " + element.getValue() + " could not be decoded");
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
                        (byte) 0x00,
                        this
                ));
            }
            // queueWrite(new FileDeleteRequest((short) 0x0503));
            queueWrite(new ImagesSetRequest(
                    images,
                    this
            ));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setWidgetContent(String widgetID, String content, boolean renderOnWatch) {
        boolean update = false;
        for (Widget widget : this.widgets) {
            if (!(widget instanceof CustomWidget)) continue;
            if (((CustomWidget) widget).updateElementValue(widgetID, content)) update = true;
        }

        if (renderOnWatch && update) renderWidgets();
    }

    private void negotiateSymmetricKey() {
        queueWrite(new VerifyPrivateKeyRequest(
                this.getSecretKey(),
                this
        ));
    }

    @Override
    public void setTime() {
        negotiateSymmetricKey();

        long millis = System.currentTimeMillis();
        TimeZone zone = new GregorianCalendar().getTimeZone();

        queueWrite(
                new ConfigurationPutRequest(
                        new TimeConfigItem(
                                (int) (millis / 1000 + getDeviceSupport().getTimeOffset() * 60),
                                (short) (millis % 1000),
                                (short) (zone.getOffset(millis) / 60000)
                        ),
                        this), false
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
        syncSettings();

        queueWrite(new VerifyPrivateKeyRequest(this.getSecretKey(), this));
        queueWrite(new FileLookupRequest((byte) 0x01, this) {
            @Override
            public void handleFileLookup(final short fileHandle) {
                queueWrite(new FileEncryptedGetRequest(fileHandle, FossilHRWatchAdapter.this) {
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
                            GB.toast("synced activity data", Toast.LENGTH_SHORT, GB.INFO);
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
        negotiateSymmetricKey();

        queueWrite(new ConfigurationGetRequest(this));
    }

    @Override
    public void setActivityHand(double progress) {
        // super.setActivityHand(progress);
    }

    public boolean playRawNotification(NotificationSpec notificationSpec) {
        String sourceAppId = notificationSpec.sourceAppId;

        String senderOrTitle = StringUtils.getFirstOf(notificationSpec.sender, notificationSpec.title);

        try {
            for (NotificationHRConfiguration configuration : this.notificationConfigurations) {
                if (configuration.getPackageName().equals(sourceAppId)) {
                    queueWrite(new PlayTextNotificationRequest(sourceAppId, senderOrTitle, notificationSpec.body, this));
                    return true;
                }
            }
            queueWrite(new PlayTextNotificationRequest("generic", senderOrTitle, notificationSpec.body, this));
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (sourceAppId != null) {
            if (!sourceAppId.equals(this.lastPostedApp)) {
                if (appIconCache.get(sourceAppId) == null) {
                    try {
                        PackageManager pm = getContext().getPackageManager();
                        Drawable icon = pm.getApplicationIcon(sourceAppId);

                        Bitmap iconBitmap = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888);
                        icon.setBounds(0, 0, 40, 40);
                        icon.draw(new Canvas(iconBitmap));

                        appIconCache.put(sourceAppId, iconBitmap);
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                this.lastPostedApp = sourceAppId;
                renderWidgets();
            }
        }
        return true;
    }

    @Override
    public void onDeleteNotification(int id) {
        super.onDeleteNotification(id);

        // only delete app icon when no notification of said app is present
        for (String app : NotificationListener.notificationStack) {
            if (app.equals(this.lastPostedApp)) return;
        }

        this.lastPostedApp = null;
        renderWidgets();
    }

    @Override
    public void onFindDevice(boolean start) {
        if (start) {
            new TransactionBuilder("vibrate find")
                    .write(
                            getDeviceSupport().getCharacteristic(UUID.fromString("3dda0005-957f-7d4a-34a6-74696673696d")),
                            new byte[]{(byte) 0x01, (byte) 0x04, (byte) 0x30, (byte) 0x75, (byte) 0x00, (byte) 0x00}
                    )
                    .queue(getDeviceSupport().getQueue());
        } else {
            new TransactionBuilder("vibrate find")
                    .write(
                            getDeviceSupport().getCharacteristic(UUID.fromString("3dda0005-957f-7d4a-34a6-74696673696d")),
                            new byte[]{(byte) 0x02, (byte) 0x05, (byte) 0x04}
                    )
                    .queue(getDeviceSupport().getQueue());
        }
    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
        super.onSetCallState(callSpec);
        queueWrite(new PlayCallNotificationRequest(StringUtils.getFirstOf(callSpec.name, callSpec.number), callSpec.command == CallSpec.CALL_INCOMING, this));
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
            logger.error("JSON exception: ", e);
        }
    }

    @Override
    public void onTestNewFunction() {
        /*queueWrite(new ActivityFilesGetRequest(this){
            @Override
            public void handleFileData(byte[] fileData) {
                super.handleFileData(fileData);
                File activityDir = new File(getContext().getExternalFilesDir(null), "activity_hr");
                activityDir.mkdir();
                File f = new File(activityDir, String.valueOf(System.currentTimeMillis()));
                try {
                    f.createNewFile();
                    FileOutputStream fos = new FileOutputStream(f);
                    fos.write(fileData);
                    fos.close();
                    GB.toast("saved file data", Toast.LENGTH_SHORT, GB.INFO);
                } catch (IOException e) {
                    GB.log("activity file error", GB.ERROR, e);
                }
                queueWrite(new FileDeleteRequest((short) 0x0101));
            }
        });*/
    }

    @Override
    public void onInstallApp(Uri uri) {
        UriHelper uriHelper = null;
        try {
            uriHelper = UriHelper.get(uri, getContext());
        } catch (IOException e) {
            GB.toast(getContext(), "Could not open firmare: " + e.getMessage(), Toast.LENGTH_LONG, GB.ERROR, e);
        }
        if (uriHelper != null) {
            try (InputStream in = new BufferedInputStream(uriHelper.openInputStream())) {
                byte[] firmwareBytes = FileUtils.readAll(in, 1024 * 2024); // 2MB
                queueWrite(new FirmwareFilePutRequest(firmwareBytes, this));
            } catch (Exception e) {
                GB.toast(getContext(), "Firmware cannot be installed: " + e.getMessage(), Toast.LENGTH_LONG, GB.ERROR, e);
            }
        }
    }


    public byte[] getSecretKey() {
        byte[] authKeyBytes = new byte[16];

        SharedPreferences sharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(getDeviceSupport().getDevice().getAddress());

        String authKey = sharedPrefs.getString("authkey", null);
        if (authKey != null && !authKey.isEmpty()) {
            byte[] srcBytes = authKey.trim().getBytes();
            if (authKey.length() == 34 && authKey.startsWith("0x")) {
                srcBytes = GB.hexStringToByteArray(authKey.substring(2));
            }
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
            JSONArray jsonArray = new JSONArray(
                    GBApplication.getPrefs().getString(HRConfigActivity.CONFIG_KEY_Q_ACTIONS, "[]")
            );
            String[] menuItems = new String[jsonArray.length()];
            for (int i = 0; i < jsonArray.length(); i++) menuItems[i] = jsonArray.getString(i);

            SharedPreferences prefs = getDeviceSpecificPreferences();
            String upperButtonApp = prefs.getString(DeviceSettingsPreferenceConst.PREF_BUTTON_1_FUNCTION, "weatherApp");
            String middleButtonApp = prefs.getString(DeviceSettingsPreferenceConst.PREF_BUTTON_2_FUNCTION, "commuteApp");
            String lowerButtonApp = prefs.getString(DeviceSettingsPreferenceConst.PREF_BUTTON_3_FUNCTION, "musicApp");

            queueWrite(new ButtonConfigurationPutRequest(
                    menuItems,
                    upperButtonApp,
                    middleButtonApp,
                    lowerButtonApp,
                    this
            ));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSendConfiguration(String config) {
        switch (config) {
            case DeviceSettingsPreferenceConst.PREF_BUTTON_1_FUNCTION:
            case DeviceSettingsPreferenceConst.PREF_BUTTON_2_FUNCTION:
            case DeviceSettingsPreferenceConst.PREF_BUTTON_3_FUNCTION:
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
        }
    }

    @Override
    public void handleHeartRateCharacteristic(BluetoothGattCharacteristic characteristic) {
        super.handleHeartRateCharacteristic(characteristic);

        byte[] value = characteristic.getValue();

        int heartRate = value[1];

        logger.debug("heart rate: " + heartRate);
    }

    @Override
    protected void handleBackgroundCharacteristic(BluetoothGattCharacteristic characteristic) {
        super.handleBackgroundCharacteristic(characteristic);

        byte[] value = characteristic.getValue();

        byte requestType = value[1];

        if (requestType == (byte) 0x04) {
            handleCallRequest(value);
        } else if (requestType == (byte) 0x05) {
            handleMusicRequest(value);
        } else if (requestType == (byte) 0x01) {
            int eventId = value[2];
            logger.info("got event id " + eventId);
            try {
                String jsonString = new String(value, 3, value.length - 3);
                logger.info(jsonString);
                JSONObject requestJson = new JSONObject(jsonString);

                JSONObject request = requestJson.getJSONObject("req");
                int requestId = request.getInt("id");

                if (request.has("ringMyPhone")) {
                    String action = request.getJSONObject("ringMyPhone").getString("action");
                    logger.info("got ringMyPhone request; " + action);
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
                    logger.info("Got weatherInfo request");
                    WeatherSpec weatherSpec = Weather.getInstance().getWeatherSpec();
                    if (weatherSpec != null) {
                        onSendWeather(weatherSpec);
                    } else {
                        logger.info("no weather data available  - ignoring request");
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

                    queueWrite(new SetCommuteMenuMessage("Anfrage wird weitergeleitet...", false, this));

                    Intent menuIntent = new Intent(QHybridSupport.QHYBRID_EVENT_COMMUTE_MENU);
                    menuIntent.putExtra("EXTRA_ACTION", action);
                    getContext().sendBroadcast(menuIntent);
                } else {
                    logger.warn("Unhandled request from watch: " + requestJson.toString());
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleCallRequest(byte[] value) {
        boolean acceptCall = value[7] == (byte) 0x00;
        queueWrite(new PlayCallNotificationRequest("", false, this));

        GBDeviceEventCallControl callControlEvent = new GBDeviceEventCallControl();
        callControlEvent.event = acceptCall ? GBDeviceEventCallControl.Event.START : GBDeviceEventCallControl.Event.REJECT;

        getDeviceSupport().evaluateGBDeviceEvent(callControlEvent);
    }

    private void handleMusicRequest(byte[] value) {
        byte command = value[3];
        logger.info("got music command: " + command);
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
}
