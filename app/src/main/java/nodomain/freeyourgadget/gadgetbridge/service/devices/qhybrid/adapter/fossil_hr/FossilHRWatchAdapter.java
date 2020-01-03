package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil_hr;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.zip.CRC32;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.HRConfigActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.encoder.RLEEncoder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.RequestMtuRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.SetDeviceStateRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.configuration.ConfigurationPutRequest.*;
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
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.utils.StringUtils;

import static nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.music.MusicControlRequest.*;

public class FossilHRWatchAdapter extends FossilWatchAdapter {
    private byte[] secretKey = new byte[]{(byte) 0x60, (byte) 0x26, (byte) 0xB7, (byte) 0xFD, (byte) 0xB2, (byte) 0x6D, (byte) 0x05, (byte) 0x5E, (byte) 0xDA, (byte) 0xF7, (byte) 0x4B, (byte) 0x49, (byte) 0x98, (byte) 0x78, (byte) 0x02, (byte) 0x38};
    private byte[] phoneRandomNumber;
    private byte[] watchRandomNumber;

    private MusicSpec currentSpec = null;

    public FossilHRWatchAdapter(QHybridSupport deviceSupport) {
        super(deviceSupport);
    }

    @Override
    public void initialize() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            queueWrite(new RequestMtuRequest(512));
        }

        negotiateSymmetricKey();

        // icons

        // queueWrite(new NotificationFilterPutHRRequest(new NotificationHRConfiguration[]{
        //         new NotificationHRConfiguration("com.whatsapp", -1),
        //         new NotificationHRConfiguration("asdasdasdasdasd", -1),
        //         // new NotificationHRConfiguration("twitter", -1),
        // }, this));

        // queueWrite(new PlayNotificationRequest("com.whatsapp", "WhatsAp", "wHATSaPP", this));
        // queueWrite(new PlayNotificationRequest("twitterrrr", "Twitterr", "tWITTER", this));

        syncSettings();

        setTime();

        overwriteButtons(null);

        drawWidgetText("-");

        queueWrite(new SetDeviceStateRequest(GBDevice.State.INITIALIZED));
    }

    @Override
    public void setWidgetContent(String widgetID, String content) {
        drawWidgetText(content);
    }

    private void drawWidgetText(String text){

        try {
            Bitmap testBitmap = Bitmap.createBitmap(76, 76, Bitmap.Config.ARGB_8888);

            Canvas testCanvas = new Canvas(testBitmap);

            Paint circlePaint = new Paint();
            circlePaint.setColor(Color.WHITE);
            circlePaint.setStyle(Paint.Style.STROKE);
            circlePaint.setStrokeWidth(3);

            testCanvas.drawCircle(38, 38, 37, circlePaint);

            circlePaint.setStrokeWidth(4);
            circlePaint.setTextSize(17f);
            circlePaint.setStyle(Paint.Style.FILL);
            circlePaint.setTextAlign(Paint.Align.CENTER);

            testCanvas.drawText("ETH", 38, 76f / 3f * 1f - (circlePaint.descent() + circlePaint.ascent()) / 2f, circlePaint);
            testCanvas.drawText(text, 38, 76f / 3f * 2f - (circlePaint.descent() + circlePaint.ascent()) / 2f, circlePaint);

            circlePaint.setStrokeWidth(1);
            circlePaint.setStyle(Paint.Style.STROKE);

            // for(int i = 0; i <= 3; i++) testCanvas.drawLine(0, 76f / 3f * i - (i / 3), 76, 76f / 3f * i - (i / 3), circlePaint);

            AssetImage image = AssetImageFactory.createAssetImage(
                    StringUtils.bytesToHex(
                            ByteBuffer.allocate(4)
                                    .putInt((int) System.currentTimeMillis())
                                    .array()
                    )
                    , testBitmap, true, 0, 60, 1);

            AssetImage[] images = new AssetImage[]{
                    image,
            };

            queueWrite(new AssetFilePutRequest(
                    images,
                    this
            ));

            queueWrite(new ImagesSetRequest(
                    images,
                    this
            ));
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        queueWrite(new MusicInfoSetRequest(
                musicSpec.artist,
                musicSpec.album,
                musicSpec.track,
                this
        ));
    }

    @Override
    public void setMusicState(MusicStateSpec stateSpec) {
        super.setMusicState(stateSpec);

        queueWrite(new MusicControlRequest(
                stateSpec.state == MusicStateSpec.STATE_PLAYING ? MUSIC_PHONE_REQUEST.MUSIC_REQUEST_SET_PLAYING : MUSIC_PHONE_REQUEST.MUSIC_REQUEST_SET_PAUSED
        ));
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
        queueWrite(new PlayNotificationRequest("generic", notificationSpec.sourceName, notificationSpec.body, this));
        return true;
    }

    public byte[] getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(byte[] secretKey) {
        this.secretKey = secretKey;
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
    protected void handleBackgroundCharacteristic(BluetoothGattCharacteristic characteristic) {
        super.handleBackgroundCharacteristic(characteristic);

        byte[] value = characteristic.getValue();

        byte requestType = value[1];

        if (requestType == (byte) 0x05) {
            handleMusicRequest(value);
            return;
        }

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
}
