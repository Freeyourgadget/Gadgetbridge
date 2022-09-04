package nodomain.freeyourgadget.gadgetbridge.service.devices.flipper.zero.support;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.IntentListener;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class FlipperZeroSupport extends FlipperZeroBaseSupport{
    private BatteryInfoProfile batteryInfoProfile = new BatteryInfoProfile(this);

    private final String UUID_SERIAL_SERVICE = "8fe5b3d5-2e7f-4a98-2a48-7acc60fe0000";
    private final String UUID_SERIAL_CHARACTERISTIC_WRITE = "19ed82ae-ed21-4c9d-4145-228e62fe0000";
    private final String UUID_SERIAL_CHARACTERISTIC_RESPONSE = "19ed82ae-ed21-4c9d-4145-228e61fe0000";

    private final String COMMAND_PLAY_FILE = "nodomain.freeyourgadget.gadgetbridge.flipper.zero.PLAY_FILE";
    private final String ACTION_PLAY_DONE = "nodomain.freeyourgadget.gadgetbridge.flipper.zero.PLAY_DONE";

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if(COMMAND_PLAY_FILE.equals(intent.getAction())){
                        handlePlaySubGHZ(intent);
                    }
                }
            }).start();
        }
    };

    private void handlePlaySubGHZ(Intent intent) {
        String appName = intent.getExtras().getString("EXTRA_APP_NAME", "Sub-GHz");

        String filePath = intent.getStringExtra("EXTRA_FILE_PATH");
        if(filePath == null){
            GB.log("missing EXTRA_FILE_PATH in intent", GB.ERROR, null);
            return;
        }
        if(filePath.isEmpty()){
            GB.log("empty EXTRA_FILE_PATH in intent", GB.ERROR, null);
            return;
        }

        GB.toast(String.format("playing %s file", appName), Toast.LENGTH_SHORT, GB.INFO);
        playFile(appName, filePath);

        Intent response = new Intent(ACTION_PLAY_DONE);
        getContext().sendBroadcast(response);
    }

    public FlipperZeroSupport() {
        super();

        batteryInfoProfile.addListener(new IntentListener() {
            @Override
            public void notify(Intent intent) {
                BatteryInfo info = intent.getParcelableExtra(BatteryInfoProfile.EXTRA_BATTERY_INFO);
                GBDeviceEventBatteryInfo batteryEvent = new GBDeviceEventBatteryInfo();
                batteryEvent.state = BatteryState.BATTERY_NORMAL;
                batteryEvent.level = info.getPercentCharged();
                evaluateGBDeviceEvent(batteryEvent);
            }
        });
        addSupportedService(GattService.UUID_SERVICE_BATTERY_SERVICE);
        addSupportedProfile(batteryInfoProfile);

        addSupportedService(UUID.fromString(UUID_SERIAL_SERVICE));
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        getContext().registerReceiver(receiver, new IntentFilter(COMMAND_PLAY_FILE));

        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));

        batteryInfoProfile.requestBatteryInfo(builder);
        batteryInfoProfile.enableNotify(builder, true);

        return builder
                .notify(getCharacteristic(UUID.fromString(UUID_SERIAL_CHARACTERISTIC_RESPONSE)), true)
                .requestMtu(512)
                .add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));
    }

    @Override
    public void dispose() {
        super.dispose();

        getContext().unregisterReceiver(receiver);
    }

    private void sendSerialData(byte[] data){
        new TransactionBuilder("send serial data")
                .write(getCharacteristic(UUID.fromString(UUID_SERIAL_CHARACTERISTIC_WRITE)), data)
                .queue(getQueue());
    }

    private void sendProtobufPacket(byte[] packet){
        byte[] fullPacket = new byte[packet.length + 1];
        fullPacket[0] = (byte) packet.length;
        System.arraycopy(packet, 0, fullPacket, 1, packet.length);
        sendSerialData(fullPacket);
    }

    private void openApp(String appName){
        // sub ghz payload: 13-08-15-82-01-0E-0A-07-   53-75-62-2D-47-48-7A   -12-03-52-50-43
        ByteBuffer buffer = ByteBuffer.allocate(12 + appName.length());
        buffer.put((byte)0x08);
        buffer.put((byte)0x15);
        // buffer.put((byte)(Math.random() * 256));
        buffer.put((byte)0x82);
        buffer.put((byte)0x01);
        buffer.put((byte) (appName.length() + 7));
        buffer.put((byte)0x0A);

        buffer.put((byte) appName.length());
        buffer.put(appName.getBytes());

        buffer.put((byte)0x12);
        buffer.put((byte)0x03);
        buffer.put((byte)0x52);
        buffer.put((byte)0x50);
        buffer.put((byte)0x43);

        sendProtobufPacket(buffer.array());
    }

    private void openSubGhzApp(){
        openApp("Sub-GHz");
    }

    private void appLoadFile(String filePath){
        // example payload 1C-08-16-82-03-17-0A-15-   2F-61-6E-79-2F-73-75-62-67-68-7A-2F-74-65-73-6C-61-2E-73-75-62
        ByteBuffer buffer = ByteBuffer.allocate(7 + filePath.length());

        buffer.put((byte) 0x08);
        buffer.put((byte) 0x16);
        buffer.put((byte) 0x82);
        buffer.put((byte) 0x03);
        buffer.put((byte) (filePath.length() + 2));
        buffer.put((byte) 0x0A);
        buffer.put((byte) filePath.length());
        buffer.put(filePath.getBytes());

        sendProtobufPacket(buffer.array());
    }

    private void appButtonPress(){
        sendProtobufPacket(new byte[]{
                (byte) 0x08, (byte) 0x17, (byte) 0x8A, (byte) 0x03, (byte) 0x00}
        );
    }

    private void appButtonRelease(){
        sendProtobufPacket(new byte[]{
                (byte) 0x08, (byte) 0x18, (byte) 0x92, (byte) 0x03, (byte) 0x00}
        );
    }
    private void appExitRequest(){
        sendProtobufPacket(new byte[]{
                (byte) 0x08, (byte) 0x19, (byte) 0xFA, (byte) 0x02, (byte) 0x00
        });
    }

    @Override
    public void onTestNewFunction() {
        openApp("Infrared");
    }

    private void playFile(String appName, String filePath){
        openApp(appName);
        try {
            Thread.sleep(500);
            appLoadFile(filePath);
            Thread.sleep(500);
            appButtonPress();
            Thread.sleep(500);
            appButtonRelease();
            Thread.sleep(1000);
            appExitRequest();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onFetchRecordedData(int dataTypes) {
        super.onFetchRecordedData(dataTypes);

        onTestNewFunction();
    }
}
