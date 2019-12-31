package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil_hr;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.HRConfigActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.NotificationHRConfiguration;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.RequestMtuRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.SetDeviceStateRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.configuration.ConfigurationPutRequest.*;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.notification.PlayNotificationRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.authentication.VerifyPrivateKeyRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.buttons.ButtonConfigurationPutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.configuration.ConfigurationGetRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.configuration.ConfigurationPutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.image.Image;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.image.ImagesPutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.menu.SetCommuteMenuMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.notification.NotificationFilterPutHRRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.notification.NotificationImagePutRequest;

public class FossilHRWatchAdapter extends FossilWatchAdapter {
    private byte[] secretKey = new byte[]{(byte) 0x60, (byte) 0x26, (byte) 0xB7, (byte) 0xFD, (byte) 0xB2, (byte) 0x6D, (byte) 0x05, (byte) 0x5E, (byte) 0xDA, (byte) 0xF7, (byte) 0x4B, (byte) 0x49, (byte) 0x98, (byte) 0x78, (byte) 0x02, (byte) 0x38};
    private byte[] phoneRandomNumber;
    private byte[] watchRandomNumber;

    public FossilHRWatchAdapter(QHybridSupport deviceSupport) {
        super(deviceSupport);
    }

    @Override
    public void initialize() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            queueWrite(new RequestMtuRequest(512));
        }

        negotiateSymmetricKey();

        try {
            FileInputStream fis = new FileInputStream("/sdcard/Q/images/icWhatsapp.icon");
            byte[] whatsappData = new byte[fis.available()];
            fis.read(whatsappData);
            fis.close();

            fis = new FileInputStream("/sdcard/Q/images/icEmail.icon");
            byte[] twitterData = new byte[fis.available()];
            fis.read(twitterData);
            fis.close();

            queueWrite(new NotificationImagePutRequest(
                    new String[]{
                            "twitter",
                            "com.whatsapp",
                    },
                    new byte[][]{
                            twitterData,
                            whatsappData,
                    },
                    this));
        } catch (IOException e) {
            e.printStackTrace();
        } // icons

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

        // negotiateSymmetricKey();
        // queueWrite(
        //         new ConfigurationPutRequest(
        //                 new nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.configuration.ConfigurationPutRequest.CurrentStepCountConfigItem(99999),
        //                 this
        //         )
        // );

        queueWrite(new SetDeviceStateRequest(GBDevice.State.INITIALIZED));
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

    private void setBackgroundImages(Image background, Image[] complications){
        background.setAngle(0);
        background.setDistance(0);
        background.setIndexZ(0);

        queueWrite(new ImagesPutRequest(new Image[]{background}, this));
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
            for(int i = 0; i < jsonArray.length(); i++) menuItems[i] = jsonArray.getString(i);

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

        int eventId = value[2];

        try {
            JSONObject requestJson = new JSONObject(new String(value, 3, value.length - 3));

            String action = requestJson.getJSONObject("req").getJSONObject("commuteApp._.config.commute_info")
                    .getString("dest");

            String startStop = requestJson.getJSONObject("req").getJSONObject("commuteApp._.config.commute_info")
                    .getString("action");

            if(startStop.equals("stop")){
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

    @Override
    public void setCommuteMenuMessage(String message, boolean finished) {
        queueWrite(new SetCommuteMenuMessage(message, finished, this));
    }
}
