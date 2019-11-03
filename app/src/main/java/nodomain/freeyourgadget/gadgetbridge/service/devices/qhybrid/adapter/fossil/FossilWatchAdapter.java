package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Queue;
import java.util.TimeZone;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.NotificationConfiguration;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.PackageConfigHelper;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.WatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.Request;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.FossilRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.configuration.ConfigurationGetRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.configuration.ConfigurationPutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FileGetRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FileLookupRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FilePrepareRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FilePutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.notification.NotificationFilterPutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.notification.PlayNotificationRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.AnimationRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.MoveHandsRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.ReleaseHandsControlRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.RequestHandControlRequest;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class FossilWatchAdapter extends WatchAdapter {
    private ArrayList<Request> requestQueue = new ArrayList<>();

    private FossilRequest fossilRequest;

    public FossilWatchAdapter(QHybridSupport deviceSupport) {
        super(deviceSupport);
    }


    @Override
    public void initialize() {
        playPairingAnimation();
        queueWrite(new ConfigurationGetRequest(this));

        syncNotificationSettings();
    }

    @Override
    public void playPairingAnimation() {
        queueWrite(new AnimationRequest());
    }

    @Override
    public void playNotification(NotificationConfiguration config) {
        if (config.getPackageName() == null) {
            log("package name in notification not set");
            return;
        }
        queueWrite(new PlayNotificationRequest(config.getPackageName(), this));
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
                        this)
        );
    }

    @Override
    public void overwriteButtons() {
        FilePutRequest uploadFileRequest = new FilePutRequest((short) 0x0600, new byte[]{
                (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x03, (byte) 0x10, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x0C, (byte) 0x00, (byte) 0x00, (byte) 0x20, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x0C, (byte) 0x00, (byte) 0x00,
                (byte) 0x30, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x0C, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x0C, (byte) 0x2E, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
                (byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x03, (byte) 0x00, (byte) 0x02, (byte) 0x01, (byte) 0x0F, (byte) 0x00, (byte) 0x8B, (byte) 0x00, (byte) 0x00, (byte) 0x93, (byte) 0x00, (byte) 0x01,
                (byte) 0x08, (byte) 0x01, (byte) 0x14, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0xFE, (byte) 0x08, (byte) 0x00, (byte) 0x93, (byte) 0x00, (byte) 0x02, (byte) 0x01, (byte) 0x00, (byte) 0xBF, (byte) 0xD5, (byte) 0x54, (byte) 0xD1,
                (byte) 0x00
        }, this);
        queueWrite(uploadFileRequest);
    }

    @Override
    public void setActivityHand(double progress) {
        queueWrite(new ConfigurationPutRequest(
                new ConfigurationPutRequest.CurrentStepCountConfigItem(Math.min(999999, (int) (1000000 * progress))),
                this
        ));
    }

    @Override
    public void setHands(short hour, short minute) {
        queueWrite(new MoveHandsRequest(false, minute, hour, (short) -1));
    }


    public void vibrate(nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.PlayNotificationRequest.VibrationType vibration) {
        // queueWrite(new nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.PlayNotificationRequest(vibration, -1, -1));
    }

    @Override
    public void vibrateFindMyDevicePattern() {

    }


    @Override
    public void requestHandsControl() {
        queueWrite(new RequestHandControlRequest());
    }

    @Override
    public void releaseHandsControl() {
        queueWrite(new ReleaseHandsControlRequest());
    }

    @Override
    public void setStepGoal(int stepGoal) {
        queueWrite(new ConfigurationPutRequest(new ConfigurationPutRequest.DailyStepGoalConfigItem(stepGoal), this));
    }

    @Override
    public void setVibrationStrength(short strength) {
        queueWrite(
                new ConfigurationPutRequest(
                        new ConfigurationPutRequest.VibrationStrengthConfigItem(
                                (byte) strength
                        ),
                        this
                )
        );
    }

    @Override
    public void syncNotificationSettings() {
        log("syncing notification settings...");
        try {
            PackageConfigHelper helper = new PackageConfigHelper(getContext());
            final ArrayList<NotificationConfiguration> configurations = helper.getNotificationConfigurations();
            if (configurations.size() == 1) configurations.add(configurations.get(0));
            queueWrite(new FilePrepareRequest((short) 0x0C00) {
                @Override
                public void onPrepare() {
                    super.onPrepare();
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
                    });
                }
            });
        } catch (GBException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onTestNewFunction() {
        try {
            queueWrite(new NotificationFilterPutRequest(new PackageConfigHelper(getContext()).getNotificationConfigurations(), this));
        } catch (GBException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean supportsExtendedVibration() {
        /*String modelNumber = getDeviceSupport().getDevice().getModel();
        switch (modelNumber) {
            case "HW.0.0":
                return true;
            case "HL.0.0":
                return false;
        }
        throw new UnsupportedOperationException("model " + modelNumber + " not supported");*/

        return false;
    }

    @Override
    public boolean supportsActivityHand() {
        String modelNumber = getDeviceSupport().getDevice().getModel();
        switch (modelNumber) {
            case "HW.0.0":
                return true;
            case "HL.0.0":
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
        }
        return "unknwon Q";
    }

    @Override
    public void onFetchActivityData() {
        NotificationConfiguration config = new NotificationConfiguration((short) 0, (short) 0, (short) 0, null);
        config.setPackageName("org.telegram.messenger");
        playNotification(config);
        // queueWrite(new ConfigurationGetRequest(this));
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        switch (characteristic.getUuid().toString()) {
            case "3dda0002-957f-7d4a-34a6-74696673696d": {
                break;
            }
            case "3dda0004-957f-7d4a-34a6-74696673696d":
            case "3dda0003-957f-7d4a-34a6-74696673696d": {
                if (fossilRequest != null) {
                    boolean requestFinished;
                    try {
                        fossilRequest.handleResponse(characteristic);
                        requestFinished = fossilRequest.isFinished();
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                        getDeviceSupport().notifiyException(e);
                        requestFinished = true;
                    }

                    if (requestFinished) {
                        log(fossilRequest.getName() + " finished");
                        fossilRequest = null;
                    }else{
                        return true;
                    }
                }
                try {
                    queueWrite(requestQueue.remove(0));
                } catch (IndexOutOfBoundsException e) {
                    log("requestsQueue empty");
                }
            }
        }
        return true;
    }

    private void log(String message) {
        Log.d("FossilWatchAdapter", message);
    }

    public void queueWrite(Request request){
        this.queueWrite(request, false);
    }

    public void queueWrite(Request request, boolean priorise) {
        if (request.isBasicRequest()) {
            try {
                queueWrite(requestQueue.remove(0));
            } catch (IndexOutOfBoundsException e) {
            }
        } else {
            if (fossilRequest != null) {
                log( "queing request: " + request.getName());
                if(priorise){
                    requestQueue.add(0, request);
                }else {
                    requestQueue.add(request);
                }
                return;
            }
            log("executing request: " + request.getName());

            if (request instanceof FossilRequest) this.fossilRequest = (FossilRequest) request;
        }

        new TransactionBuilder(request.getClass().getSimpleName()).write(getDeviceSupport().getCharacteristic(request.getRequestUUID()), request.getRequestData()).queue(getDeviceSupport().getQueue());
    }
}
