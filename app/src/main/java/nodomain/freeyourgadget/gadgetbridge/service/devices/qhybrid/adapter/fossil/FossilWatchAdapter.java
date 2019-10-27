package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import java.util.ArrayDeque;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.TimeZone;

import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.PackageConfig;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.WatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.Request;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.ConfigurationGetRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.ConfigurationRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.FileGetRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.FileLookupAndGetRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.FileLookupRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.FilePutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.NotifcationFilterGetRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.PlayNotificationRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.AnimationRequest;

public class FossilWatchAdapter extends WatchAdapter {
    private Queue<Request> requestQueue = new ArrayDeque<>();

    FilePutRequest filePutRequest;
    FileGetRequest fileGetRequest;
    FileLookupRequest fileLookupRequest;

    public FossilWatchAdapter(QHybridSupport deviceSupport) {
        super(deviceSupport);
    }


    @Override
    public void initialize() {
        playPairingAnimation();

    }

    @Override
    public void playPairingAnimation() {
        queueWrite(new AnimationRequest());
    }

    @Override
    public void playNotification(PackageConfig config) {
        queueWrite(new PlayNotificationRequest(config.getPackageName(), this));
    }

    @Override
    public void setTime() {
        long millis = System.currentTimeMillis();
        TimeZone zone = new GregorianCalendar().getTimeZone();

        queueWrite(
                new ConfigurationRequest(
                        new ConfigurationRequest.TimeConfigItem(
                                (int) (millis / 1000 + getDeviceSupport().getTimeOffset() * 60),
                                (short) (millis % 1000),
                                (short) ((zone.getRawOffset() + (zone.inDaylightTime(new Date()) ? 1 : 0)) / 60000)
                        ),
                        this)
        );
    }

    @Override
    public void overwriteButtons() {

    }

    @Override
    public void setActivityHand(double progress) {

    }

    @Override
    public void setHands(short hour, short minute) {

    }

    @Override
    public void vibrate(nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.PlayNotificationRequest.VibrationType vibration) {

    }

    @Override
    public void vibrateFindMyDevicePattern() {

    }

    @Override
    public void requestHandsControl() {

    }

    @Override
    public void releaseHandsControl() {

    }

    @Override
    public void setStepGoal(int stepGoal) {

    }

    @Override
    public void setVibrationStrength(short strength) {

    }

    @Override
    public void onTestNewFunction() {
        queueWrite(new FileLookupAndGetRequest((byte)8, this));
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

    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        switch (characteristic.getUuid().toString()) {
            case "3dda0004-957f-7d4a-34a6-74696673696d":
            case "3dda0003-957f-7d4a-34a6-74696673696d": {
                if (filePutRequest != null) {

                    filePutRequest.handleResponse(characteristic);

                    if (filePutRequest.isFinished()) {
                        filePutRequest = null;
                        try {
                            queueWrite(requestQueue.remove());
                        } catch (NoSuchElementException e) {
                        }
                    }
                } else if(fileGetRequest != null){
                    boolean requestFinished;
                    try {
                        fileGetRequest.handleResponse(characteristic);
                        requestFinished = fileGetRequest.isFinished();
                    }catch (RuntimeException e){
                        e.printStackTrace();
                        requestFinished = true;
                    }

                    if(requestFinished){
                        fileGetRequest = null;
                        try {
                            queueWrite(requestQueue.remove());
                        } catch (NoSuchElementException e) {
                        }
                    }
                } else if(fileLookupRequest != null){
                    boolean requestFinished;
                    try {
                        fileLookupRequest.handleResponse(characteristic);
                        requestFinished = fileLookupRequest.isFinished();
                    }catch (RuntimeException e){
                        e.printStackTrace();
                        requestFinished = true;
                    }

                    if(requestFinished){
                        fileLookupRequest = null;
                        try {
                            queueWrite(requestQueue.remove());
                        } catch (NoSuchElementException e) {
                        }
                    }
                }
            }
        }
        return true;
    }

    public void queueWrite(Request request) {
        if (filePutRequest != null || fileGetRequest != null || fileLookupRequest != null) {
            requestQueue.add(request);
            return;
        }

        if (request instanceof FilePutRequest) filePutRequest = (FilePutRequest) request;
        else if (request instanceof FileGetRequest) fileGetRequest = (FileGetRequest) request;
        else if (request instanceof FileLookupRequest) fileLookupRequest = (FileLookupRequest) request;

        new TransactionBuilder(request.getClass().getSimpleName()).write(getDeviceSupport().getCharacteristic(request.getRequestUUID()), request.getRequestData()).queue(getDeviceSupport().getQueue());
    }
}
