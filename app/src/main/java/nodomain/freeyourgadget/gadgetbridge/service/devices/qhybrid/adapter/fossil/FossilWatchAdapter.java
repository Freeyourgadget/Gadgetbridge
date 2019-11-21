package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.NotificationConfiguration;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.PackageConfigHelper;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.GenericItem;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.WatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.Request;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.FossilRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.RequestMtuRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.SetDeviceStateRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.configuration.ConfigurationGetRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.configuration.ConfigurationPutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FilePutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.notification.NotificationFilterPutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.notification.PlayNotificationRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.AnimationRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.MoveHandsRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.ReleaseHandsControlRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.RequestHandControlRequest;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

import static nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport.QHYBRID_EVENT_BUTTON_PRESS;

public class FossilWatchAdapter extends WatchAdapter {
    private ArrayList<Request> requestQueue = new ArrayList<>();

    private FossilRequest fossilRequest;

    private int MTU = 23;

    private String ITEM_MTU = "MTU";

    private int lastButtonIndex = -1;

    Logger logger = LoggerFactory.getLogger(getClass());

    public FossilWatchAdapter(QHybridSupport deviceSupport) {
        super(deviceSupport);
    }


    @Override
    public void initialize() {
        playPairingAnimation();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            queueWrite(new RequestMtuRequest(512), false);
        }
        queueWrite(new ConfigurationGetRequest(this), false);

        syncNotificationSettings();

        queueWrite(new SetDeviceStateRequest(GBDevice.State.INITIALIZED), false);
    }

    public int getMTU() {
        if (this.MTU < 0) throw new RuntimeException("MTU not configured");

        return this.MTU;
    }

    @Override
    public void playPairingAnimation() {
        queueWrite(new AnimationRequest(), false);
    }

    @Override
    public void playNotification(NotificationConfiguration config) {
        if (config.getPackageName() == null) {
            log("package name in notification not set");
            return;
        }
        queueWrite(new PlayNotificationRequest(config.getPackageName(), this), false);
    }

    @Override
    public void setTime() {
        long millis = System.currentTimeMillis();
        TimeZone zone = new GregorianCalendar().getTimeZone();

        queueWrite(
                new ConfigurationPutRequest(
                        new ConfigurationPutRequest.TimeConfigItem(
                                (int) (millis / 1000 + getDeviceSupport().getTimeOffset() * 60),
                                (short) (millis % 1000),
                                (short) ((zone.getRawOffset() + (zone.inDaylightTime(new Date()) ? 1 : 0)) / 60000)
                        ),
                        this), false
        );
    }

    @Override
    public void overwriteButtons() {
        FilePutRequest fileUploadRequets = new FilePutRequest((short) 0x0600, new byte[]{
                (byte) 0x01, (byte) 0x00, (byte) 0x00,
                (byte) 0x03,
                (byte) 0x10, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x0C, (byte) 0x00, (byte) 0x00,
                (byte) 0x20, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x0C, (byte) 0x00, (byte) 0x00,
                (byte) 0x30, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x0C, (byte) 0x00, (byte) 0x00,
                (byte) 0x01,
                (byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x0C, (byte) 0x2E, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x03, (byte) 0x00, (byte) 0x02, (byte) 0x01, (byte) 0x0F, (byte) 0x00, (byte) 0x8B, (byte) 0x00, (byte) 0x00, (byte) 0x93, (byte) 0x00, (byte) 0x01, (byte) 0x08, (byte) 0x01, (byte) 0x14, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0xFE, (byte) 0x08, (byte) 0x00, (byte) 0x93, (byte) 0x00, (byte) 0x02, (byte) 0x01, (byte) 0x00, (byte) 0xBF, (byte) 0xD5, (byte) 0x54, (byte) 0xD1,
                (byte) 0x00,
                (byte) 0x4F, (byte) 0x79, (byte) 0x97, (byte) 0x78,
        }, this){
            @Override
            public void onFilePut(boolean success) {
                if(success) GB.toast("successfully overwritten button settings", Toast.LENGTH_SHORT, GB.INFO);
                else GB.toast("error overwriting button settings", Toast.LENGTH_SHORT, GB.INFO);
            }
        };
        queueWrite(fileUploadRequets);
    }

    @Override
    public void setActivityHand(double progress) {
        queueWrite(new ConfigurationPutRequest(
                new ConfigurationPutRequest.CurrentStepCountConfigItem(Math.min(999999, (int) (1000000 * progress))),
                this
        ), false);
    }

    @Override
    public void setHands(short hour, short minute) {
        queueWrite(new MoveHandsRequest(false, minute, hour, (short) -1), false);
    }


    public void vibrate(nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.PlayNotificationRequest.VibrationType vibration) {
        // queueWrite(new nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.PlayNotificationRequest(vibration, -1, -1));
    }

    @Override
    public void vibrateFindMyDevicePattern() {

    }


    @Override
    public void requestHandsControl() {
        queueWrite(new RequestHandControlRequest(), false);
    }

    @Override
    public void releaseHandsControl() {
        queueWrite(new ReleaseHandsControlRequest(), false);
    }

    @Override
    public void setStepGoal(int stepGoal) {
        queueWrite(new ConfigurationPutRequest(new ConfigurationPutRequest.DailyStepGoalConfigItem(stepGoal), this){
            @Override
            public void onFilePut(boolean success) {
                if(success) GB.toast("successfully updated step goal", Toast.LENGTH_SHORT, GB.INFO);
                else GB.toast("error updating step goal", Toast.LENGTH_SHORT, GB.INFO);
            }
        }, false);
    }

    @Override
    public void setVibrationStrength(short strength) {
        ConfigurationPutRequest.ConfigItem vibrationItem = new ConfigurationPutRequest.VibrationStrengthConfigItem((byte) strength);


        queueWrite(
                new ConfigurationPutRequest(new ConfigurationPutRequest.ConfigItem[]{vibrationItem}, this){
                    @Override
                    public void onFilePut(boolean success) {
                        if(success) GB.toast("successfully updated vibration strength", Toast.LENGTH_SHORT, GB.INFO);
                        else GB.toast("error updating vibration strength", Toast.LENGTH_SHORT, GB.INFO);
                    }
                }, false
        );
        // queueWrite(new FileVerifyRequest((short) 0x0800));
    }

    @Override
    public void syncNotificationSettings() {
        log("syncing notification settings...");
        try {
            PackageConfigHelper helper = new PackageConfigHelper(getContext());
            final ArrayList<NotificationConfiguration> configurations = helper.getNotificationConfigurations();
            if (configurations.size() == 1) configurations.add(configurations.get(0));
            queueWrite(new NotificationFilterPutRequest(configurations, FossilWatchAdapter.this) {
                @Override
                public void onFilePut(boolean success) {
                    super.onFilePut(success);

                    if (!success) {
                        GB.toast("error writing notification settings", Toast.LENGTH_SHORT, GB.ERROR);

                        getDeviceSupport().getDevice().setState(GBDevice.State.NOT_CONNECTED);
                        getDeviceSupport().getDevice().sendDeviceUpdateIntent(getContext());
                    }

                    getDeviceSupport().getDevice().setState(GBDevice.State.INITIALIZED);
                    getDeviceSupport().getDevice().sendDeviceUpdateIntent(getContext());
                }
            }, false);
        } catch (GBException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onTestNewFunction() {
        queueWrite(new ConfigurationPutRequest(new ConfigurationPutRequest.ConfigItem[0], this), false);
    }

    @Override
    public boolean supportsFindDevice() {
        return false;
    }

    @Override
    public boolean supportsExtendedVibration() {
        String modelNumber = getDeviceSupport().getDevice().getModel();
        switch (modelNumber) {
            case "HW.0.0":
                return true;
            case "HL.0.0":
                return false;
            case "DN.1.0":
                return true;
        }
        throw new UnsupportedOperationException("model " + modelNumber + " not supported");
    }

    @Override
    public boolean supportsActivityHand() {
        String modelNumber = getDeviceSupport().getDevice().getModel();
        switch (modelNumber) {
            case "HW.0.0":
                return true;
            case "HL.0.0":
                return false;
            case "DN.1.0":
                return false;
        }
        throw new UnsupportedOperationException("Model " + modelNumber + " not supported");
    }

    @Override
    public String getModelName() {
        String modelNumber = getDeviceSupport().getDevice().getModel();
        switch (modelNumber) {
            case "HW.0.0":
                return "Q Commuter";
            case "HL.0.0":
                return "Q Activist";
            case "DN.1.0":
                return "Hybrid HR";
        }
        return "unknwon Q";
    }

    @Override
    public void onFetchActivityData() {

        // queueWrite(new ConfigurationPutRequest(new ConfigurationPutRequest.ConfigItem[0], this));
        setVibrationStrength((byte) 50);
        // queueWrite(new FileCloseRequest((short) 0x0800));
        // queueWrite(new ConfigurationGetRequest(this));
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        switch (characteristic.getUuid().toString()) {
            case "3dda0006-957f-7d4a-34a6-74696673696d": {
                handleBackgroundCharacteristic(characteristic);
                break;
            }
            case "3dda0002-957f-7d4a-34a6-74696673696d":
            case "3dda0004-957f-7d4a-34a6-74696673696d":
            case "3dda0003-957f-7d4a-34a6-74696673696d": {
                if (fossilRequest != null) {
                    boolean requestFinished;
                    try {
                        if (characteristic.getUuid().toString().equals("3dda0003-957f-7d4a-34a6-74696673696d")) {
                            byte requestType = (byte) (characteristic.getValue()[0] & 0x0F);

                            if (requestType != 0x0A && requestType != fossilRequest.getType()) {
                                // throw new RuntimeException("Answer type " + requestType + " does not match current request " + fossilRequest.getType());
                            }
                        }

                        fossilRequest.handleResponse(characteristic);
                        requestFinished = fossilRequest.isFinished();
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                        getDeviceSupport().notifiyException(e);
                        GB.toast(fossilRequest.getName() + " failed", Toast.LENGTH_SHORT, GB.ERROR);
                        requestFinished = true;
                    }

                    if (requestFinished) {
                        log(fossilRequest.getName() + " finished");
                        fossilRequest = null;
                    } else {
                        return true;
                    }
                }
                queueNextRequest();
            }
        }
        return true;
    }

    private void handleBackgroundCharacteristic(BluetoothGattCharacteristic characteristic) {
        byte[] value = characteristic.getValue();
        switch (value[1]) {
            case 2: {
                byte syncId = value[2];
                getDeviceSupport().getDevice().addDeviceInfo(new GenericItem(QHybridSupport.ITEM_LAST_HEARTBEAT, DateFormat.getTimeInstance().format(new Date())));
                break;
            }
            case 8: {
                if (value.length != 12) {
                    throw new RuntimeException("wrong button message");
                }
                int index = value[2] & 0xFF;
                int button = value[9] >> 4 & 0xFF;

                if (index != this.lastButtonIndex) {
                    lastButtonIndex = index;
                    log("Button press on button " + button);

                    Intent i = new Intent(QHYBRID_EVENT_BUTTON_PRESS);
                    i.putExtra("BUTTON", button);
                    getContext().sendBroadcast(i);
                }
                break;
            }
        }
    }


    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        super.onMtuChanged(gatt, mtu, status);
        this.MTU = mtu;

        getDeviceSupport().getDevice().addDeviceInfo(new GenericItem(ITEM_MTU, String.valueOf(mtu)));
        getDeviceSupport().getDevice().sendDeviceUpdateIntent(getContext());

        ((RequestMtuRequest) fossilRequest).setFinished(true);
        queueNextRequest();
    }

    public void queueWrite(RequestMtuRequest request, boolean priorise) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            new TransactionBuilder("requestMtu")
                    .requestMtu(512)
                    .queue(getDeviceSupport().getQueue());

            this.fossilRequest = request;
        }
    }

    private void log(String message) {
        logger.debug(message);
    }

    public void queueWrite(SetDeviceStateRequest request, boolean priorise) {
        if (fossilRequest != null && !fossilRequest.isFinished()) {
            log("queing request: " + request.getName());
            if (priorise) {
                requestQueue.add(0, request);
            } else {
                requestQueue.add(request);
            }
            return;
        }
        log("setting device state: " + request.getDeviceState());
        getDeviceSupport().getDevice().setState(request.getDeviceState());
        getDeviceSupport().getDevice().sendDeviceUpdateIntent(getContext());
        queueNextRequest();
    }

    public void queueWrite(FossilRequest request, boolean priorise) {
        if (fossilRequest != null && !fossilRequest.isFinished()) {
            log("queing request: " + request.getName());
            if (priorise) {
                requestQueue.add(0, request);
            } else {
                requestQueue.add(request);
            }
            return;
        }
        log("executing request: " + request.getName());
        this.fossilRequest = request;
        new TransactionBuilder(request.getClass().getSimpleName()).write(getDeviceSupport().getCharacteristic(request.getRequestUUID()), request.getRequestData()).queue(getDeviceSupport().getQueue());
    }

    public void queueWrite(Request request, boolean priorise) {
        new TransactionBuilder(request.getClass().getSimpleName()).write(getDeviceSupport().getCharacteristic(request.getRequestUUID()), request.getRequestData()).queue(getDeviceSupport().getQueue());

        queueNextRequest();
    }

    void queueWrite(Request request) {
        if (request instanceof SetDeviceStateRequest)
            queueWrite((SetDeviceStateRequest) request, false);
        else if (request instanceof RequestMtuRequest)
            queueWrite((RequestMtuRequest) request, false);
        else if (request instanceof FossilRequest) queueWrite((FossilRequest) request, false);
        else queueWrite(request, false);
    }

    private void queueNextRequest() {
        try {
            Request request = requestQueue.remove(0);
            queueWrite(request);
        } catch (IndexOutOfBoundsException e) {
            log("requestsQueue empty");
        }
    }
}
