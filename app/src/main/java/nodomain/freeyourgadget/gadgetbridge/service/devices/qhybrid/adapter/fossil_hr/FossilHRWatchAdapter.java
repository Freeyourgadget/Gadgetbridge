package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil_hr;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.CpuUsageInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TimeZone;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.HRConfigActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.NotificationHRConfiguration;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.RequestMtuRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.SetDeviceStateRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.configuration.ConfigurationPutRequest.TimeConfigItem;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.notification.PlayNotificationRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.authentication.VerifyPrivateKeyRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.buttons.ButtonConfigurationPutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.configuration.ConfigurationGetRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.configuration.ConfigurationPutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.file.AssetFilePutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.image.AssetImage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.image.AssetImageFactory;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.image.ImagesSetRequest;
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
import nodomain.freeyourgadget.gadgetbridge.util.GB;

import static nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.music.MusicControlRequest.MUSIC_PHONE_REQUEST;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.music.MusicControlRequest.MUSIC_WATCH_REQUEST;

public class FossilHRWatchAdapter extends FossilWatchAdapter {
    private byte[] phoneRandomNumber;
    private byte[] watchRandomNumber;

    ArrayList<Widget> widgets = new ArrayList<>();

    NotificationHRConfiguration[] notificationConfigurations;

    private MusicSpec currentSpec = null;

    int imageNameIndex = 0;
    private byte jsonIndex = 0;

    private AssetImage backGroundImage = null;

    public FossilHRWatchAdapter(QHybridSupport deviceSupport) {
        super(deviceSupport);
    }

    @Override
    public void initialize() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            queueWrite(new RequestMtuRequest(512));
        }

        queueWrite(new SetDeviceStateRequest(GBDevice.State.AUTHENTICATING));

        negotiateSymmetricKey();

        queueWrite(new SetDeviceStateRequest(GBDevice.State.INITIALIZING));

        // icons

        loadNotificationConfigurations();
        queueWrite(new NotificationFilterPutHRRequest(this.notificationConfigurations, this));
        // queueWrite(new NotificationFilterPutHRRequest(this.notificationConfigurations,this));

        /*try {
            final String[] appNames = {"instagram", "snapchat", "line", "whatsapp"};
            final String[] paths = {
                    "/storage/emulated/0/Q/images/icInstagram.icon",
                    "/storage/emulated/0/Q/images/icSnapchat.icon",
                    "/storage/emulated/0/Q/images/icLine.icon",
                    "/storage/emulated/0/Q/images/icWhatsapp.icon"
            };

            NotificationHRConfiguration[] configs = new NotificationHRConfiguration[4];
            NotificationImage[] images = new NotificationImage[4];
            for(int i = 0; i < 4; i++){
                FileInputStream fis = new FileInputStream(paths[i]);
                byte[] imageData = new byte[fis.available()];
                fis.read(imageData);
                fis.close();
                configs[i] = new NotificationHRConfiguration(appNames[i], i);
                images[i] = new NotificationImage(appNames[i], imageData);
            }
            queueWrite(new NotificationImagePutRequest(images, this));
            queueWrite(new NotificationFilterPutHRRequest(configs, this));

            for(String appName : appNames){
                queueWrite(new PlayNotificationHRRequest(
                        appName,
                        appName.toUpperCase(),
                        "this is some strange message",
                        this
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }*/

        setVibrationStrength((short) 75);

        syncSettings();

        setTime();

        overwriteButtons(null);


        loadBackground();
        loadWidgets();
        // renderWidgets();
        // dunno if there is any point in doing this at start since when no watch is connected the QHybridSupport will not receive any intents anyway

        queueWrite(new SetDeviceStateRequest(GBDevice.State.INITIALIZED));
    }

    @Override
    public void setVibrationStrength(short strength) {
        negotiateSymmetricKey();
        queueWrite(new ConfigurationPutRequest(new nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.configuration.ConfigurationPutRequest.VibrationStrengthConfigItem((byte) strength), this));
    }

    private void loadNotificationConfigurations(){
        this.notificationConfigurations = new NotificationHRConfiguration[]{
                new NotificationHRConfiguration("generic", 0),
        };
    }

    private void loadBackground(){
        /*Bitmap backgroundBitmap = BitmapFactory
                .decodeFile("/sdcard/DCIM/Camera/IMG_20191129_200726.jpg");

        try {
            this.backGroundImage = AssetImageFactory.createAssetImage(backgroundBitmap, false, 0,:wq
             0, 0);
        } catch (IOException e) {
            GB.log("Backgroundimage error", GB.ERROR, e);
        }*/
    }

    private void loadWidgets() {
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

            while(keyIterator.hasNext()){
                String position = keyIterator.next();
                String identifier = widgetConfig.getString(position);
                Widget.WidgetType type = Widget.WidgetType.fromJsonIdentifier(identifier);

                Widget widget = null;
                if(type != null) {
                    widget = new Widget(type, positionMap.get(position), 63);
                }else{
                    identifier = identifier.substring(7);
                    for(int i = 0; i < customWidgets.length(); i++){
                        JSONObject customWidget = customWidgets.getJSONObject(i);
                        if(customWidget.getString("name").equals(identifier)){
                            CustomWidget newWidget = new CustomWidget(
                                    customWidget.getString("name"),
                                    positionMap.get(position),
                                    63
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

                if(widget == null) continue;
                this.widgets.add(widget);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        uploadWidgets();
    }

    private void uploadWidgets(){
        negotiateSymmetricKey();
        ArrayList<Widget> systemWidgets = new ArrayList<>(widgets.size());
        for(Widget widget : this.widgets){
            if(!(widget instanceof CustomWidget)) systemWidgets.add(widget);
        }
        queueWrite(new WidgetsPutRequest(systemWidgets.toArray(new Widget[0]), this));
    }

    private void renderWidgets() {
        try {
            ArrayList<AssetImage> widgetImages = new ArrayList<>();

            if(this.backGroundImage != null){
                widgetImages.add(this.backGroundImage);
            }


            for (int i = 0; i < this.widgets.size(); i++) {
                Widget w = widgets.get(i);
                if(!(w instanceof CustomWidget)) continue;
                CustomWidget widget = (CustomWidget) w;

                Bitmap widgetBitmap = Bitmap.createBitmap(76, 76, Bitmap.Config.ARGB_8888);

                Canvas widgetCanvas = new Canvas(widgetBitmap);

                boolean backgroundDrawn = false;

                Paint circlePaint = new Paint();
                if(!backgroundDrawn){
                    circlePaint.setColor(Color.BLACK);
                    circlePaint.setStyle(Paint.Style.FILL);
                    circlePaint.setStrokeWidth(3);
                    widgetCanvas.drawCircle(38, 38, 37, circlePaint);

                    circlePaint.setColor(Color.WHITE);
                    circlePaint.setStyle(Paint.Style.STROKE);
                    circlePaint.setStrokeWidth(3);
                    widgetCanvas.drawCircle(38, 38, 37, circlePaint);
                }

                for (CustomWidgetElement element : widget.getElements()) {
                    if (element.getWidgetElementType() == CustomWidgetElement.WidgetElementType.TYPE_BACKGROUND) {
                        File imageFile = new File(element.getValue());

                        if(!imageFile.exists() || !imageFile.isFile()){
                            logger.debug("Image file " + element.getValue() + " not found");
                            continue;
                        }
                        Bitmap imageBitmap = BitmapFactory.decodeFile(element.getValue());
                        if(imageBitmap == null){
                            logger.debug("image file " + element.getValue() + " could not be decoded");
                            continue;
                        }
                        Bitmap scaledBitmap = Bitmap.createScaledBitmap(imageBitmap, 76, 76, false);

                        widgetCanvas.drawBitmap(
                                scaledBitmap,
                                0,
                                0,
                                null);
                        backgroundDrawn = true;
                        break;
                    }
                }

                for (CustomWidgetElement element : widget.getElements()) {
                    if (element.getWidgetElementType() == CustomWidgetElement.WidgetElementType.TYPE_TEXT) {
                        Paint textPaint = new Paint();
                        textPaint.setStrokeWidth(4);
                        textPaint.setTextSize(17f);
                        textPaint.setStyle(Paint.Style.FILL);
                        textPaint.setColor(Color.WHITE);
                        textPaint.setTextAlign(Paint.Align.CENTER);

                        widgetCanvas.drawText(element.getValue(), element.getX(), element.getY() - (textPaint.descent() + textPaint.ascent()) / 2f, textPaint);
                    }else if(element.getWidgetElementType() == CustomWidgetElement.WidgetElementType.TYPE_IMAGE) {
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

            // queueWrite(new FileDeleteRequest((short) 0x0700));
            queueWrite(new AssetFilePutRequest(
                    images,
                    (byte) 0x00,
                    this
            ));

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
            if(!(widget instanceof CustomWidget)) continue;
            if(((CustomWidget) widget).updateElementValue(widgetID, content)) update = true;
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
                                (short) ((zone.getRawOffset() + (zone.inDaylightTime(new Date()) ? 1 : 0)) / 60000)
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
        }catch (BufferOverflowException e){
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

    private void setBackgroundImages(AssetImage background, AssetImage[] complications) {
        queueWrite(new ImagesSetRequest(new AssetImage[]{background}, this));
    }

    @Override
    public void onFetchActivityData() {
        syncSettings();
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
        String sender = notificationSpec.sender;
        if (sender == null) sender = notificationSpec.sourceName;
        try {
            for (NotificationHRConfiguration configuration : this.notificationConfigurations){
                if(configuration.getPackageName().equals(notificationSpec.sourceAppId)){
                    queueWrite(new PlayNotificationRequest(notificationSpec.sourceAppId, notificationSpec.sourceName, notificationSpec.body, this));
                    return true;
                }
            }
            queueWrite(new PlayNotificationRequest("generic", notificationSpec.sourceName, notificationSpec.body, this));
        }catch (Exception e){
            e.printStackTrace();
        }
        return true;
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

            queueWrite(new ButtonConfigurationPutRequest(
                    menuItems,
                    this
            ));
        } catch (JSONException e) {
            e.printStackTrace();
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

        if (requestType == (byte) 0x05) {
            handleMusicRequest(value);
        } else if (requestType == (byte) 0x01) {
            int eventId = value[2];

            try {
                JSONObject requestJson = new JSONObject(new String(value, 3, value.length - 3));

                String action = requestJson.getJSONObject("req").getJSONObject("commuteApp._.config.commute_info")
                        .getString("dest");

                String startStop = requestJson.getJSONObject("req").getJSONObject("commuteApp._.config.commute_info")
                        .getString("action");

                if (startStop.equals("stop")) {
                    // overwriteButtons(null);
                    return;
                }

                queueWrite(new SetCommuteMenuMessage("Anfrage wird weitergeleitet...", false, this));

                Intent menuIntent = new Intent(QHybridSupport.QHYBRID_EVENT_COMMUTE_MENU);
                menuIntent.putExtra("EXTRA_ACTION", action);
                getContext().sendBroadcast(menuIntent);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleMusicRequest(byte[] value) {
        byte command = value[3];

        MUSIC_WATCH_REQUEST request = MUSIC_WATCH_REQUEST.fromCommandByte(command);

        MusicControlRequest r = new MusicControlRequest(MUSIC_PHONE_REQUEST.MUSIC_REQUEST_PLAY_PAUSE);

        switch (request) {
            case MUSIC_REQUEST_PLAY_PAUSE: {
                queueWrite(new MusicControlRequest(MUSIC_PHONE_REQUEST.MUSIC_REQUEST_PLAY_PAUSE));
                break;
            }
            case MUSIC_REQUEST_LOUDER: {
                queueWrite(new MusicControlRequest(MUSIC_PHONE_REQUEST.MUSIC_REQUEST_LOUDER));
                break;
            }
            case MUSIC_REQUEST_QUITER: {
                queueWrite(new MusicControlRequest(MUSIC_PHONE_REQUEST.MUSIC_REQUEST_QUITER));
                break;
            }
        }
    }

    @Override
    public void setCommuteMenuMessage(String message, boolean finished) {
        queueWrite(new SetCommuteMenuMessage(message, finished, this));
    }

    public byte getJsonIndex() {
        return jsonIndex++;
    }
}
