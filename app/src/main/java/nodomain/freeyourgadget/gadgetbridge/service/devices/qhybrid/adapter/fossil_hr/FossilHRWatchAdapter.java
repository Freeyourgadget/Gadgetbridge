package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil_hr;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Random;
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
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.image.ImageRLEEncoder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.image.ImagesPutRequest;
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
        byte[] input = new byte[]{
                0, 0, 0, 1, 0, 1, 2, 0, 4, 4, 0
        };

        ImageRLEEncoder.RLEEncode(input);

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

        try {
            FileInputStream fis = new FileInputStream("/sdcard/traditional_bg.bin");
            byte[] backgroundData = new byte[fis.available()];
            fis.read(backgroundData);
            // new Random().nextBytes(backgroundData);
            fis.close();

            CRC32 crc = new CRC32();
            crc.update(backgroundData);

            String backgroundFileName = StringUtils.bytesToHex(
                    ByteBuffer
                            .allocate(4)
                            .putInt((int) crc.getValue())
                            .array()
            );

            AssetImage bg = AssetImageFactory.createAssetImage(backgroundFileName, backgroundData, 0, 0, 0);

            queueWrite(new AssetFilePutRequest(
                    bg,
                    this
            ));
            queueWrite(new ImagesPutRequest(
                    new AssetImage[]{bg},
                    this
            ));
        } catch (Exception e) {
            e.printStackTrace();
        }

        overwriteButtons(null);

        /*try {
            FileInputStream fis = new FileInputStream("/sdcard/traditional_bg.bin");
            byte[] backgroundData = new byte[fis.available()];
            fis.read(backgroundData);
            fis.close();

            queueWrite(new AssetFilePutRequest(
                    new byte[][]{
                            new byte[]{(byte) 0x41, (byte) 0x41, (byte) 0x44, (byte) 0x33, (byte) 0x36, (byte) 0x35, (byte) 0x37, (byte) 0x37, (byte) 0x36, (byte) 0x00},
                            // new byte[]{(byte) 0x30, (byte) 0x44, (byte) 0x35, (byte) 0x38, (byte) 0x42, (byte) 0x44, (byte) 0x32, (byte) 0x34, (byte) 0x00},
                            // new byte[]{(byte) 0x43, (byte) 0x33, (byte) 0x46, (byte) 0x36, (byte) 0x39, (byte) 0x33, (byte) 0x36, (byte) 0x33, (byte) 0x00},
                            // new byte[]{(byte) 0x37, (byte) 0x32, (byte) 0x35, (byte) 0x31, (byte) 0x43, (byte) 0x38, (byte) 0x32, (byte) 0x42, (byte) 0x00},
                    },
                    new byte[][]{
                            backgroundData
                            // new byte[]{(byte) 0x18, (byte) 0x18, (byte) 0x1D, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x01, (byte) 0x01, (byte) 0x14, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x03, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x0B, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x06, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x09, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x07, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x03, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x05, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x05, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x05, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x03, (byte) 0x04, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x06, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x04, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x03, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x08, (byte) 0x00, (byte) 0x02, (byte) 0x03, (byte) 0x03, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x03, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x09, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x00, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x0A, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x02, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x02, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x0C, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x0D, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x03, (byte) 0x00, (byte) 0x04, (byte) 0x03, (byte) 0x04, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x01, (byte) 0x01, (byte) 0x09, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x03, (byte) 0x00, (byte) 0x03, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x09, (byte) 0x00, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x09, (byte) 0x00, (byte) 0x04, (byte) 0x03, (byte) 0x03, (byte) 0x00, (byte) 0x03, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x08, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x03, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x06, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x05, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x05, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x05, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x03, (byte) 0x00, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x07, (byte) 0x00, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x04, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x01, (byte) 0x01, (byte) 0x04, (byte) 0x00, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x04, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x06, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x03, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x05, (byte) 0x00, (byte) 0x02, (byte) 0x03, (byte) 0x09, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x05, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x13, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x1D, (byte) 0x00, (byte) 0xFF, (byte) 0xFF},
                            // new byte[]{(byte) 0x18, (byte) 0x18, (byte) 0x1D, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x01, (byte) 0x01, (byte) 0x14, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x03, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x12, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x10, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x0E, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x03, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x0D, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x05, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x03, (byte) 0x0D, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x04, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x0E, (byte) 0x00, (byte) 0x02, (byte) 0x03, (byte) 0x03, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x0F, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x00, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x10, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x02, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x11, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x10, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x03, (byte) 0x00, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x05, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x01, (byte) 0x01, (byte) 0x09, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x03, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x09, (byte) 0x00, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x09, (byte) 0x00, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x03, (byte) 0x00, (byte) 0x04, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x08, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x08, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x05, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x09, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x03, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x07, (byte) 0x00, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x0B, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x01, (byte) 0x01, (byte) 0x04, (byte) 0x00, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x0D, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x03, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x10, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x05, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x13, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x1D, (byte) 0x00, (byte) 0xFF, (byte) 0xFF},
                            // new byte[]{(byte) 0x18, (byte) 0x18, (byte) 0x1E, (byte) 0x00, (byte) 0x0C, (byte) 0x01, (byte) 0x09, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x10, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x05, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x03, (byte) 0x0E, (byte) 0x02, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x0E, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x10, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x10, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x10, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x10, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x10, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x10, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x10, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x10, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x10, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x00, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x0B, (byte) 0x01, (byte) 0x04, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x02, (byte) 0x04, (byte) 0x00, (byte) 0x0D, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x03, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x05, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x09, (byte) 0x02, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x0F, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x10, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x11, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x12, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x13, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x14, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x16, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x08, (byte) 0x00, (byte) 0xFF, (byte) 0xFF},
                            // new byte[]{(byte) 0x18, (byte) 0x18, (byte) 0x3B, (byte) 0x0C, (byte) 0x02, (byte) 0x0D, (byte) 0x16, (byte) 0x0C, (byte) 0x02, (byte) 0x07, (byte) 0x16, (byte) 0x0C, (byte) 0x02, (byte) 0x03, (byte) 0x15, (byte) 0x0C, (byte) 0x01, (byte) 0x0A, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x0A, (byte) 0x14, (byte) 0x0C, (byte) 0x04, (byte) 0x03, (byte) 0x13, (byte) 0x0C, (byte) 0x01, (byte) 0x09, (byte) 0x01, (byte) 0x03, (byte) 0x02, (byte) 0x0A, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x09, (byte) 0x0F, (byte) 0x0C, (byte) 0x02, (byte) 0x0D, (byte) 0x01, (byte) 0x09, (byte) 0x02, (byte) 0x03, (byte) 0x02, (byte) 0x0C, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x09, (byte) 0x02, (byte) 0x0D, (byte) 0x08, (byte) 0x0C, (byte) 0x01, (byte) 0x0A, (byte) 0x07, (byte) 0x03, (byte) 0x01, (byte) 0x06, (byte) 0x02, (byte) 0x0C, (byte) 0x01, (byte) 0x06, (byte) 0x07, (byte) 0x03, (byte) 0x01, (byte) 0x0A, (byte) 0x05, (byte) 0x0C, (byte) 0x01, (byte) 0x06, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x06, (byte) 0x02, (byte) 0x09, (byte) 0x01, (byte) 0x0D, (byte) 0x04, (byte) 0x0C, (byte) 0x01, (byte) 0x0D, (byte) 0x01, (byte) 0x09, (byte) 0x01, (byte) 0x0A, (byte) 0x01, (byte) 0x06, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x06, (byte) 0x07, (byte) 0x0C, (byte) 0x01, (byte) 0x0A, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x0D, (byte) 0x08, (byte) 0x0C, (byte) 0x01, (byte) 0x0D, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x0A, (byte) 0x09, (byte) 0x0C, (byte) 0x01, (byte) 0x09, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x09, (byte) 0x06, (byte) 0x0C, (byte) 0x01, (byte) 0x09, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x09, (byte) 0x0B, (byte) 0x0C, (byte) 0x01, (byte) 0x0D, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x0D, (byte) 0x05, (byte) 0x0C, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x0D, (byte) 0x0D, (byte) 0x0C, (byte) 0x01, (byte) 0x07, (byte) 0x01, (byte) 0x03, (byte) 0x06, (byte) 0x0C, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x07, (byte) 0x0E, (byte) 0x0C, (byte) 0x02, (byte) 0x03, (byte) 0x02, (byte) 0x0C, (byte) 0x02, (byte) 0x09, (byte) 0x02, (byte) 0x0C, (byte) 0x01, (byte) 0x07, (byte) 0x01, (byte) 0x03, (byte) 0x0D, (byte) 0x0C, (byte) 0x01, (byte) 0x0D, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x06, (byte) 0x01, (byte) 0x0D, (byte) 0x01, (byte) 0x07, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x07, (byte) 0x01, (byte) 0x0D, (byte) 0x01, (byte) 0x06, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x0D, (byte) 0x0C, (byte) 0x0C, (byte) 0x01, (byte) 0x09, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x07, (byte) 0x02, (byte) 0x03, (byte) 0x02, (byte) 0x06, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x07, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x09, (byte) 0x0C, (byte) 0x0C, (byte) 0x01, (byte) 0x06, (byte) 0x03, (byte) 0x03, (byte) 0x01, (byte) 0x0D, (byte) 0x02, (byte) 0x0C, (byte) 0x01, (byte) 0x0D, (byte) 0x03, (byte) 0x03, (byte) 0x01, (byte) 0x06, (byte) 0x0C, (byte) 0x0C, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x0A, (byte) 0x06, (byte) 0x0C, (byte) 0x01, (byte) 0x0A, (byte) 0x02, (byte) 0x03, (byte) 0x0C, (byte) 0x0C, (byte) 0x01, (byte) 0x06, (byte) 0x0A, (byte) 0x0C, (byte) 0x01, (byte) 0x06, (byte) 0x4E, (byte) 0x0C, (byte) 0xFF, (byte) 0xFF},
                    },
                    this
            ));
        } catch (IOException e) {
            e.printStackTrace();
        }*/

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
        queueWrite(new ImagesPutRequest(new AssetImage[]{background}, this));
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
