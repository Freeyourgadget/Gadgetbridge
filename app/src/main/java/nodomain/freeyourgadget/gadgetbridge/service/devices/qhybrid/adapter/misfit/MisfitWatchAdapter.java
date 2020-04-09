/*  Copyright (C) 2019-2020 Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.misfit;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.net.Uri;
import android.util.SparseArray;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.TimeZone;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.NotificationConfiguration;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.model.GenericItem;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.WatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.Request;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.ActivityPointGetRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.AnimationRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.BatteryLevelRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.DownloadFileRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.EraseFileRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.FileRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.GetCountdownSettingsRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.GetCurrentStepCountRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.GetStepGoalRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.GetVibrationStrengthRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.GoalTrackingGetRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.ListFilesRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.MoveHandsRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.OTAEnterRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.OTAEraseRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.PlayNotificationRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.ReleaseHandsControlRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.RequestHandControlRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.SetCurrentStepCountRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.SetStepGoalRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.SetTimeRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.SetVibrationStrengthRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.UploadFileRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.VibrateRequest;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

import static nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport.ITEM_ACTIVITY_POINT;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport.ITEM_STEP_COUNT;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport.ITEM_STEP_GOAL;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport.ITEM_VIBRATION_STRENGTH;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport.QHYBRID_EVENT_BUTTON_PRESS;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport.QHYBRID_EVENT_FILE_UPLOADED;

public class MisfitWatchAdapter extends WatchAdapter {
    private int lastButtonIndex = -1;
    private final SparseArray<Request> responseFilters = new SparseArray<>();

    private UploadFileRequest uploadFileRequest;
    private Request fileRequest = null;

    private Queue<Request> requestQueue = new ArrayDeque<>();

    private Logger logger = LoggerFactory.getLogger(getClass());

    public MisfitWatchAdapter(QHybridSupport deviceSupport) {
        super(deviceSupport);

        fillResponseList();
    }

    @Override
    public void initialize() {
        requestQueue.add(new GetStepGoalRequest());
        requestQueue.add(new GetVibrationStrengthRequest());
        requestQueue.add(new ActivityPointGetRequest());
        requestQueue.add(prepareSetTimeRequest());
        requestQueue.add(new AnimationRequest());
        requestQueue.add(new SetCurrentStepCountRequest((int) (999999 * getDeviceSupport().calculateNotificationProgress())));

        queueWrite(new GetCurrentStepCountRequest());

        getDeviceSupport().getDevice().setState(GBDevice.State.INITIALIZED);
        getDeviceSupport().getDevice().sendDeviceUpdateIntent(getContext());
    }


    private SetTimeRequest prepareSetTimeRequest() {
        long millis = System.currentTimeMillis();
        TimeZone zone = new GregorianCalendar().getTimeZone();
        return new SetTimeRequest(
                (int) (millis / 1000 + getDeviceSupport().getTimeOffset() * 60),
                (short) (millis % 1000),
                (short) ((zone.getRawOffset() + zone.getDSTSavings()) / 60000));
    }


    @Override
    public void playPairingAnimation() {
        queueWrite(new AnimationRequest());
    }

    @Override
    public void playNotification(NotificationConfiguration config) {
        queueWrite(new PlayNotificationRequest(
                config.getVibration(),
                config.getHour(),
                config.getMin(),
                config.getSubEye()
        ));
    }

    @Override
    public void setTime() {
        queueWrite(prepareSetTimeRequest());
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        GBDevice gbDevice = getDeviceSupport().getDevice();
        switch (characteristic.getUuid().toString()) {
            case "3dda0004-957f-7d4a-34a6-74696673696d":
            case "3dda0003-957f-7d4a-34a6-74696673696d": {
                return handleFileDownloadCharacteristic(characteristic);
            }
            case "3dda0007-957f-7d4a-34a6-74696673696d": {
                return handleFileUploadCharacteristic(characteristic);
            }
            case "3dda0002-957f-7d4a-34a6-74696673696d": {
                return handleBasicCharacteristic(characteristic);
            }
            case "3dda0006-957f-7d4a-34a6-74696673696d": {
                return handleButtonCharacteristic(characteristic);
            }
            case "00002a19-0000-1000-8000-00805f9b34fb": {
                short level = characteristic.getValue()[0];
                gbDevice.setBatteryLevel(level);

                gbDevice.setBatteryThresholdPercent((short) 2);

                GBDeviceEventBatteryInfo batteryInfo = new GBDeviceEventBatteryInfo();
                batteryInfo.level = gbDevice.getBatteryLevel();
                batteryInfo.state = BatteryState.BATTERY_NORMAL;
                getDeviceSupport().handleGBDeviceEvent(batteryInfo);
                break;
            }
            default: {
                log("unknown shit on " + characteristic.getUuid().toString() + ":  " + arrayToString(characteristic.getValue()));
                try {
                    File charLog = FileUtils.getExternalFile("qFiles/charLog.txt");
                    try (FileOutputStream fos = new FileOutputStream(charLog, true)) {
                        fos.write((new Date().toString() + ": " + characteristic.getUuid().toString() + ": " + arrayToString(characteristic.getValue())).getBytes());
                    }
                } catch (IOException e) {
                    GB.log("error", GB.ERROR, e);
                }
                break;
            }
        }
        return getDeviceSupport().onCharacteristicChanged(gatt, characteristic);
    }

    private void fillResponseList() {
        Class<? extends Request>[] classes = new Class[]{
                BatteryLevelRequest.class,
                GetStepGoalRequest.class,
                GetVibrationStrengthRequest.class,
                GetCurrentStepCountRequest.class,
                OTAEnterRequest.class,
                GoalTrackingGetRequest.class,
                ActivityPointGetRequest.class,
                GetCountdownSettingsRequest.class
        };
        for (Class<? extends Request> c : classes) {
            try {
                c.getSuperclass().getDeclaredMethod("handleResponse", BluetoothGattCharacteristic.class);
                Request object = c.newInstance();
                byte[] sequence = object.getStartSequence();
                if (sequence.length > 1) {
                    responseFilters.put((int) object.getStartSequence()[1], object);
                    log("response filter " + object.getStartSequence()[1] + ": " + c.getSimpleName());
                }
            } catch (NoSuchMethodException | IllegalAccessException | InstantiationException e) {
                log("skipping class " + c.getName());
            }
        }
    }

    private boolean handleBasicCharacteristic(BluetoothGattCharacteristic characteristic) {
        byte[] values = characteristic.getValue();
        Request request = resolveAnswer(characteristic);
        GBDevice gbDevice = getDeviceSupport().getDevice();

        if (request == null) {
            StringBuilder valueString = new StringBuilder(String.valueOf(values[0]));
            for (int i = 1; i < characteristic.getValue().length; i++) {
                valueString.append(", ").append(values[i]);
            }
            log("unable to resolve " + characteristic.getUuid().toString() + ": " + valueString);
            return true;
        }
        log("response: " + request.getClass().getSimpleName());
        request.handleResponse(characteristic);

        if (request instanceof GetStepGoalRequest) {
            gbDevice.addDeviceInfo(new GenericItem(ITEM_STEP_GOAL, String.valueOf(((GetStepGoalRequest) request).stepGoal)));
        } else if (request instanceof GetVibrationStrengthRequest) {
            int strength = ((GetVibrationStrengthRequest) request).strength;
            gbDevice.addDeviceInfo(new GenericItem(ITEM_VIBRATION_STRENGTH, String.valueOf(strength)));
        } else if (request instanceof GetCurrentStepCountRequest) {
            int steps = ((GetCurrentStepCountRequest) request).steps;
            logger.debug("get current steps: " + steps);
            try {
                File file = FileUtils.getExternalFile("qFiles/steps");
                logger.debug("Writing file " + file.getPath());
                try (FileOutputStream fos = new FileOutputStream(file, true)) {
                    fos.write((System.currentTimeMillis() + ": " + steps + "\n").getBytes());
                }
                logger.debug("file written.");
            } catch (Exception e) {
                GB.log("error", GB.ERROR, e);
            }
            gbDevice.addDeviceInfo(new GenericItem(ITEM_STEP_COUNT, String.valueOf(((GetCurrentStepCountRequest) request).steps)));
        } else if (request instanceof OTAEnterRequest) {
            if (((OTAEnterRequest) request).success) {
                fileRequest = new OTAEraseRequest(1024 << 16);
                queueWrite(fileRequest);
            }
        } else if (request instanceof ActivityPointGetRequest) {
            gbDevice.addDeviceInfo(new GenericItem(ITEM_ACTIVITY_POINT, String.valueOf(((ActivityPointGetRequest) request).activityPoint)));
        }
        try {
            queueWrite(requestQueue.remove());
        } catch (NoSuchElementException e) {
        }
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(new Intent(DeviceManager.ACTION_DEVICES_CHANGED));
        return true;
    }


    private Request resolveAnswer(BluetoothGattCharacteristic characteristic) {
        byte[] values = characteristic.getValue();
        if (values[0] != 3) return null;
        return responseFilters.get(values[1]);
    }

    private boolean handleFileDownloadCharacteristic(BluetoothGattCharacteristic characteristic) {
        Request request;
        request = fileRequest;
        request.handleResponse(characteristic);
        if (request instanceof ListFilesRequest) {
            if (((ListFilesRequest) request).completed) {
                logger.debug("File count: " + ((ListFilesRequest) request).fileCount + "  size: " + ((ListFilesRequest) request).size);
                if (((ListFilesRequest) request).fileCount == 0) return true;
                // queueWrite(new DownloadFileRequest((short) (256 + ((ListFilesRequest) request).fileCount)));
            }
        } else if (request instanceof DownloadFileRequest) {
            if (((FileRequest) request).completed) {
                logger.debug("file " + ((DownloadFileRequest) request).fileHandle + " completed: " + ((DownloadFileRequest) request).size);
                // backupFile((DownloadFileRequest) request);
            }
        } else if (request instanceof EraseFileRequest) {
            if (((EraseFileRequest) request).fileHandle > 257) {
                queueWrite(new DownloadFileRequest((short) (((EraseFileRequest) request).fileHandle - 1)));
            }
        }
        return true;
    }


    private boolean handleFileUploadCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (uploadFileRequest == null) {
            logger.debug("no uploadFileRequest to handle response");
            return true;
        }

        uploadFileRequest.handleResponse(characteristic);

        switch (uploadFileRequest.state) {
            case ERROR:
                Intent fileIntent = new Intent(QHYBRID_EVENT_FILE_UPLOADED);
                fileIntent.putExtra("EXTRA_ERROR", true);
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(fileIntent);
                uploadFileRequest = null;
                break;
            case UPLOAD:
                for (byte[] packet : this.uploadFileRequest.packets) {
                    new TransactionBuilder("File upload").write(characteristic, packet).queue(getDeviceSupport().getQueue());
                }
                break;
            case UPLOADED:
                fileIntent = new Intent(QHYBRID_EVENT_FILE_UPLOADED);
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(fileIntent);
                uploadFileRequest = null;
                break;
        }
        return true;
    }

    private boolean handleButtonCharacteristic(BluetoothGattCharacteristic characteristic) {
        byte[] value = characteristic.getValue();
        if (value.length != 11) {
            logger.debug("wrong button message");
            return true;
        }
        int index = value[6] & 0xFF;
        int button = value[8] >> 4 & 0xFF;

        if (index != this.lastButtonIndex) {
            lastButtonIndex = index;
            logger.debug("Button press on button " + button);

            Intent i = new Intent(QHYBRID_EVENT_BUTTON_PRESS);
            i.putExtra("BUTTON", button);

            //ByteBuffer buffer = ByteBuffer.allocate(16);
            //buffer.put(new byte[]{0x01, 0x00, 0x08});
            //buffer.put(value, 2, 8);
            //buffer.put(new byte[]{(byte)0xFF, 0x05, 0x00, 0x01, 0x00});

            //FilePutRequest request = new FilePutRequest((short)0, buffer.array());
            //for(byte[] packet : request.packets){
            //    new TransactionBuilder("File upload").write(getCharacteristic(UUID.fromString("3dda0007-957f-7d4a-34a6-74696673696d")), packet).queue(getQueue());
            //}

            getContext().sendBroadcast(i);
        }
        return true;
    }

    private void log(String message){
        logger.debug(message);
    }

    public void setActivityHand(double progress) {
        queueWrite(new SetCurrentStepCountRequest(Math.min((int) (1000000 * progress), 999999)));
    }


    public void setHands(short hour, short minute) {
        queueWrite(new MoveHandsRequest(false, minute, hour, (short) -1));
    }

    public void vibrate(PlayNotificationRequest.VibrationType vibration) {
        queueWrite(new PlayNotificationRequest(vibration, -1, -1));
    }

    @Override
    public void vibrateFindMyDevicePattern() {
        queueWrite(new VibrateRequest(false, (short) 4, (short) 1));
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
        queueWrite(new SetStepGoalRequest(stepGoal));
    }

    @Override
    public void setVibrationStrength(short strength) {
        queueWrite(new SetVibrationStrengthRequest(strength));
    }

    @Override
    public void syncNotificationSettings() {

    }

    @Override
    public void onTestNewFunction() {

    }

    @Override
    public void setTimezoneOffsetMinutes(short offset) {
        GB.toast("old firmware does't support timezones", Toast.LENGTH_LONG, GB.ERROR);
    }

    @Override
    public void onInstallApp(Uri uri) {

    }

    @Override
    public boolean supportsFindDevice() {
        return supportsExtendedVibration();
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
        throw new UnsupportedOperationException("Model " + modelNumber + " not supported");
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
    public void onFetchActivityData() {
        requestQueue.add(new BatteryLevelRequest());
        requestQueue.add(new GetCurrentStepCountRequest());
        // requestQueue.add(new ListFilesRequest());
        queueWrite(new ActivityPointGetRequest());
    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {
        GB.toast("alarms not supported with this firmware", Toast.LENGTH_LONG, GB.ERROR);
        return;
    }

    @Override
    public void onSendConfiguration(String config) {

    }

    @Override
    public void overwriteButtons(String jsonConfigString) {
        uploadFileRequest = new UploadFileRequest((short) 0x0800, new byte[]{
                (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x03, (byte) 0x10, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x0C, (byte) 0x00, (byte) 0x00, (byte) 0x20, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x0C, (byte) 0x00, (byte) 0x00,
                (byte) 0x30, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x0C, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x0C, (byte) 0x2E, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
                (byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x03, (byte) 0x00, (byte) 0x02, (byte) 0x01, (byte) 0x0F, (byte) 0x00, (byte) 0x8B, (byte) 0x00, (byte) 0x00, (byte) 0x93, (byte) 0x00, (byte) 0x01,
                (byte) 0x08, (byte) 0x01, (byte) 0x14, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0xFE, (byte) 0x08, (byte) 0x00, (byte) 0x93, (byte) 0x00, (byte) 0x02, (byte) 0x01, (byte) 0x00, (byte) 0xBF, (byte) 0xD5, (byte) 0x54, (byte) 0xD1,
                (byte) 0x00
        });
        queueWrite(uploadFileRequest);
    }

    private void queueWrite(Request request) {
        new TransactionBuilder(request.getClass().getSimpleName()).write(getDeviceSupport().getCharacteristic(request.getRequestUUID()), request.getRequestData()).queue(getDeviceSupport().getQueue());
        // if (request instanceof FileRequest) this.fileRequest = request;

        if (!request.expectsResponse()) {
            try {
                queueWrite(requestQueue.remove());
            } catch (NoSuchElementException e) {
            }
        }
    }
}
