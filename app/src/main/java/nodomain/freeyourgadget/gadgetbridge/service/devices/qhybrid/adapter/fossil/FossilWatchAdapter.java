package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.NoSuchElementException;
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
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.ConfigurationGetRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.ConfigurationPutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.FileGetRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.FileLookupRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.FilePrepareRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.FilePutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.NotificationFilterPutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.PlayNotificationRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.AnimationRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.MoveHandsRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.ReleaseHandsControlRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.RequestHandControlRequest;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class FossilWatchAdapter extends WatchAdapter {
    private Queue<Request> requestQueue = new ArrayDeque<>();

    FilePutRequest filePutRequest;
    FileGetRequest fileGetRequest;
    FileLookupRequest fileLookupRequest;
    FilePrepareRequest filePrepareRequest;

    public FossilWatchAdapter(QHybridSupport deviceSupport) {
        super(deviceSupport);
    }


    @Override
    public void initialize() {
        playPairingAnimation();
        queueWrite(new ConfigurationGetRequest(this) {
            @Override
            public void handleConfigurationLoaded() {
                super.handleConfigurationLoaded();

                syncNotificationSettings();
            }
        });
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
                                throw new RuntimeException("Error uploading notification settings");
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
        String modelNumber = getDeviceSupport().getDevice().getModel();
        switch (modelNumber) {
            case "HW.0.0":
                return true;
            case "HL.0.0":
                return false;
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
                if (fileGetRequest == null && fileLookupRequest == null && filePutRequest == null && filePrepareRequest == null) {
                    try {
                        queueWrite(requestQueue.remove());
                    } catch (NoSuchElementException e) {
                        log("requestsQueue empty");
                    }
                }
                break;
            }
            case "3dda0004-957f-7d4a-34a6-74696673696d":
            case "3dda0003-957f-7d4a-34a6-74696673696d": {
                if (filePutRequest != null) {

                    filePutRequest.handleResponse(characteristic);

                    if (filePutRequest.isFinished()) {
                        log("filePutRequets finished");
                        filePutRequest = null;
                        try {
                            queueWrite(requestQueue.remove());
                        } catch (NoSuchElementException e) {
                            log("requestsQueue empty");
                        }
                    }
                } else if (fileGetRequest != null) {
                    boolean requestFinished;
                    try {
                        fileGetRequest.handleResponse(characteristic);
                        requestFinished = fileGetRequest.isFinished();
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                        requestFinished = true;
                    }

                    if (requestFinished) {
                        log("fileGetRequest finished");
                        fileGetRequest = null;
                        try {
                            queueWrite(requestQueue.remove());
                        } catch (NoSuchElementException e) {
                            log("requestsQueue empty");
                        }
                    }
                } else if (fileLookupRequest != null) {
                    boolean requestFinished;
                    try {
                        fileLookupRequest.handleResponse(characteristic);
                        requestFinished = fileLookupRequest.isFinished();
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                        requestFinished = true;
                    }

                    if (requestFinished) {
                        log("fileLookupRequest finished");
                        fileLookupRequest = null;
                        try {
                            queueWrite(requestQueue.remove());
                        } catch (NoSuchElementException e) {
                            log("requestsQueue empty");
                        }
                    }
                } else if (filePrepareRequest != null) {
                    boolean requestFinished;
                    try {
                        filePrepareRequest.handleResponse(characteristic);
                        requestFinished = filePrepareRequest.isFinished();
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                        requestFinished = true;
                    }

                    if (requestFinished) {
                        log("filePrepareRequest finished");
                        filePrepareRequest = null;
                        try {
                            queueWrite(requestQueue.remove());
                        } catch (NoSuchElementException e) {
                            log("requestsQueue empty");
                        }
                    }
                } else {
                    try {
                        queueWrite(requestQueue.remove());
                    } catch (NoSuchElementException e) {
                        log("requestsQueue empty");
                    }
                }
            }
        }
        return true;
    }

    private void log(String message) {
        Log.d("FossilWatchAdapter", message);
    }

    public void queueWrite(Request request) {
        if (request.isBasicRequest()) {
            try {
                queueWrite(requestQueue.remove());
            } catch (NoSuchElementException e) {
            }
        } else {
            if (filePutRequest != null || fileGetRequest != null || fileLookupRequest != null) {
                Log.d("FossilWatchAdapter", "queing request: " + request.getName());
                requestQueue.add(request);
                return;
            }
            log("executing request directly: " + request.getName());

            if (request instanceof FilePutRequest) filePutRequest = (FilePutRequest) request;
            else if (request instanceof FileGetRequest) fileGetRequest = (FileGetRequest) request;
            else if (request instanceof FileLookupRequest)
                fileLookupRequest = (FileLookupRequest) request;
            else if (request instanceof FilePrepareRequest)
                filePrepareRequest = (FilePrepareRequest) request;
        }

        new TransactionBuilder(request.getClass().getSimpleName()).write(getDeviceSupport().getCharacteristic(request.getRequestUUID()), request.getRequestData()).queue(getDeviceSupport().getQueue());
    }
}
