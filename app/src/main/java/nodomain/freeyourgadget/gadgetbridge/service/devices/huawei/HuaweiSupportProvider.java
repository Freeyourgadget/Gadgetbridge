/*  Copyright (C) 2024 Damien Gaignon, Martin.JM, Vitalii Tomin

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import de.greenrobot.dao.query.DeleteQuery;
import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventAppInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventCameraRemote;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventDisplayMessage;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiCoordinatorSupplier;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiCoordinatorSupplier.HuaweiDeviceType;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiCrypto;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiDictTypes;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiGpsParser;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTruSleepParser;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.CameraRemote;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.GpsAndTime;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Weather;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Workout;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummaryDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiDictData;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiDictDataDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiDictDataValues;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiDictDataValuesDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutDataSample;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutDataSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutPaceSample;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutPaceSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutSummarySample;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutSummarySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutSwimSegmentsSample;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutSwimSegmentsSampleDao;
import nodomain.freeyourgadget.gadgetbridge.export.ActivityTrackExporter;
import nodomain.freeyourgadget.gadgetbridge.export.GPXExporter;
import nodomain.freeyourgadget.gadgetbridge.externalevents.gps.GBLocationProviderType;
import nodomain.freeyourgadget.gadgetbridge.externalevents.gps.GBLocationService;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.entities.Alarm;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceApp;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrack;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.Contact;
import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;

import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.p2p.HuaweiP2PCalendarService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.p2p.HuaweiP2PTrackService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.p2p.HuaweiP2PDataDictionarySyncService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.AcceptAgreementsRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetAppInfoParams;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetContactsCount;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetEventAlarmList;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetGpsParameterRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetExtendedMusicInfoParams;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetMusicInfoParams;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetNotificationConstraintsRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetSmartAlarmList;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetWatchfaceParams;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendCameraRemoteSetupEvent;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendDeviceReportThreshold;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendExtendedAccountRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendFitnessUserInfoRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendGpsDataRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendFileUploadInfo;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendHeartRateZonesConfig;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendRunPaceConfigRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendSetContactsRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendNotifyHeartRateCapabilityRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendNotifyRestHeartRateCapabilityRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SetAutomaticHeartrateRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SetAutomaticSpoRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SetDisconnectNotification;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SetMediumToStrengthThresholdRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SetSkinTemperatureMeasurement;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SetTemperatureUnitSetting;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.StopFindPhoneRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.StopNotificationRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetFitnessTotalsRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetHiChainRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetSleepDataCountRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetStepDataCountRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetWorkoutCountRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendNotificationRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SetMusicRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.AlarmsRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.DebugRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetActivityTypeRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.Request;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.Request.RequestCallback;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetAuthRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetBatteryLevelRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetBondParamsRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetBondRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetConnectStatusRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetDeviceStatusRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetDndLiftWristTypeRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetExpandCapabilityRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetLinkParamsRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetPincodeRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetProductInformationRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetSecurityNegotiationRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetSettingRelatedRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetSupportedServicesRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetWearStatusRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendDndAddRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendFactoryResetRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendFitnessGoalRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendMenstrualCapabilityRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendDndDeleteRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendSetUpDeviceStatusRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SetActivateOnLiftRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SetActivityReminderRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SetDateFormatRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SetLanguageSettingRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SetNotificationRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SetNavigateOnRotateRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SetTimeRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SetTimeZoneIdRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SetTruSleepRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SetWearLocationRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SetWearMessagePushRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetNotificationCapabilitiesRequest;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.FitnessData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SetWorkModeRequest;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.MediaManager;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class HuaweiSupportProvider {
    private static final Logger LOG = LoggerFactory.getLogger(HuaweiSupportProvider.class);

    // TODO: Potentially use translatable messages for the toast messages

    private final HuaweiSyncState syncState = new HuaweiSyncState(this);

    private final int initTimeout = 2000;

    private HuaweiBRSupport brSupport;
    private HuaweiLESupport leSupport;

    private GBDevice gbDevice;
    private Context context;
    private HuaweiCoordinatorSupplier.HuaweiDeviceType huaweiType;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable batteryRunner = () -> {
        LOG.info("Running retrieving battery through runner.");
        getBatteryLevel();
    };

    private boolean firstConnection = false;
    protected byte protocolVersion;
    public String deviceMac; //get it from GB
    protected String macAddress;
    protected String androidID;
    protected short msgId = 0;

    private MediaManager mediaManager = null;

    private GpsAndTime.GpsParameters.Response gpsParametersResponse = null;
    private boolean gpsEnabled = false;
    private Location gpsLastLocation;

    private final HuaweiPacket.ParamsProvider paramsProvider = new HuaweiPacket.ParamsProvider();

    protected ResponseManager responseManager = new ResponseManager(this);
    protected HuaweiUploadManager huaweiUploadManager = new HuaweiUploadManager(this);

    protected HuaweiWatchfaceManager huaweiWatchfaceManager = new HuaweiWatchfaceManager(this);

    protected HuaweiFileDownloadManager huaweiFileDownloadManager = new HuaweiFileDownloadManager(this);

    protected HuaweiAppManager huaweiAppManager = new HuaweiAppManager(this);

    protected HuaweiWeatherManager huaweiWeatherManager = new HuaweiWeatherManager(this);

    protected HuaweiEphemerisManager huaweiEphemerisManager = new HuaweiEphemerisManager(this);

    protected HuaweiMusicManager huaweiMusicManager = new HuaweiMusicManager(this);

    //TODO: we need only one instance of manager and all it services.
    protected HuaweiP2PManager huaweiP2PManager = new HuaweiP2PManager(this);

    public HuaweiCoordinatorSupplier getCoordinator() {
        return ((HuaweiCoordinatorSupplier) this.gbDevice.getDeviceCoordinator());
    }

    public HuaweiCoordinator getHuaweiCoordinator() {
        return getCoordinator().getHuaweiCoordinator();
    }

    public HuaweiUploadManager getUploadManager() {
        return huaweiUploadManager;
    }

    public HuaweiWatchfaceManager getHuaweiWatchfaceManager() {
        return huaweiWatchfaceManager;
    }

    public HuaweiAppManager getHuaweiAppManager() {
        return huaweiAppManager;
    }

    public HuaweiP2PManager getHuaweiP2PManager() {
        return huaweiP2PManager;
    }

    public HuaweiEphemerisManager getHuaweiEphemerisManager() {
        return huaweiEphemerisManager;
    }

    public HuaweiMusicManager getHuaweiMusicManager() {
        return huaweiMusicManager;
    }

    public HuaweiSupportProvider(HuaweiBRSupport support) {
        this.brSupport = support;
    }

    public HuaweiSupportProvider(HuaweiLESupport support) {
        this.leSupport = support;
    }

    public void setContext(Context context) {
        mediaManager = new MediaManager(context);
    }

    public boolean isBLE() {
        return huaweiType == HuaweiDeviceType.AW || huaweiType == HuaweiDeviceType.BLE || huaweiType == HuaweiDeviceType.SMART;
    }

    public nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder createLeTransactionBuilder(String taskName) {
        return leSupport.createTransactionBuilder(taskName);
    }

    public nodomain.freeyourgadget.gadgetbridge.service.btbr.TransactionBuilder createBrTransactionBuilder(String taskName) {
        return brSupport.createTransactionBuilder(taskName);
    }

    public BluetoothGattCharacteristic getLeCharacteristic(UUID uuid) {
        return leSupport.getCharacteristic(uuid);
    }

    public void performConnected(nodomain.freeyourgadget.gadgetbridge.service.btle.Transaction transaction) throws IOException {
        leSupport.performConnected(transaction);
    }

    public void performConnected(nodomain.freeyourgadget.gadgetbridge.service.btbr.Transaction transaction) throws IOException {
        brSupport.performConnected(transaction);
    }

    public void evaluateGBDeviceEvent(GBDeviceEvent deviceEvent) {
        if (isBLE()) {
            leSupport.evaluateGBDeviceEvent(deviceEvent);
        } else {
            brSupport.evaluateGBDeviceEvent(deviceEvent);
        }
    }

    public void handleGBDeviceEvent(GBDeviceEventDisplayMessage message) {
        if (isBLE()) {
            leSupport.handleGBDeviceEvent(message);
        } else {
            brSupport.handleGBDeviceEvent(message);
        }
    }

    public void setGps(boolean start) {
        if (start) {
            if (!GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress()).getBoolean(DeviceSettingsPreferenceConst.PREF_WORKOUT_SEND_GPS_TO_BAND, false))
                return;
            if (gpsParametersResponse == null) {
                GetGpsParameterRequest gpsParameterRequest = new GetGpsParameterRequest(this);
                gpsParameterRequest.setFinalizeReq(new RequestCallback() {
                    @Override
                    public void call() {
                        if (gpsEnabled) {
                            // Prevent adding multiple GPS providers
                            LOG.info("GPS is already enabled.");
                            return;
                        }
                        gpsEnabled = true;
                        GBLocationService.start(getContext(), getDevice(), GBLocationProviderType.GPS, 1000);
                    }
                });
                try {
                    gpsParameterRequest.doPerform();
                } catch (IOException e) {
                    GB.toast(context, "Failed to get GPS parameters", Toast.LENGTH_SHORT, GB.ERROR, e);
                    LOG.error("Failed to get GPS parameters", e);
                }
            } else {
                if (gpsEnabled) {
                    // Prevent adding multiple GPS providers
                    LOG.info("GPS is already enabled.");
                    return;
                }
                gpsEnabled = true;
                GBLocationService.start(getContext(), getDevice(), GBLocationProviderType.GPS, 1000);
            }
        } else {
            gpsEnabled = false;
            GBLocationService.stop(getContext(), getDevice());
            gpsLastLocation = null;
        }
    }

    public void setGpsParametersResponse(GpsAndTime.GpsParameters.Response response) {
        this.gpsParametersResponse = response;
    }

    public void setup(GBDevice device, Context context) {
        this.gbDevice = device;
        this.context = context;
        this.huaweiType = getCoordinator().getHuaweiType();
        this.paramsProvider.setTransactionsCrypted(this.getHuaweiCoordinator().isTransactionCrypted());
    }

    protected nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder initializeDevice(nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder builder) {
        setup(leSupport.getDevice(), leSupport.getContext());
        builder.setCallback(leSupport);
        final BluetoothGattCharacteristic characteristicRead = leSupport.getCharacteristic(HuaweiConstants.UUID_CHARACTERISTIC_HUAWEI_READ);
        if (characteristicRead == null) {
            LOG.warn("Read characteristic is null, will attempt to reconnect");
            builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.WAITING_FOR_RECONNECT, getContext()));
            return builder;
        }
        builder.notify(characteristicRead, true);
        builder.add(new nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction(getDevice(), GBDevice.State.AUTHENTICATING, getContext()));
        final GetLinkParamsRequest linkParamsReq = new GetLinkParamsRequest(this, builder);
        initializeDevice(linkParamsReq);
        getCoordinator().setDevice(this.gbDevice);
        return builder;
    }

    protected nodomain.freeyourgadget.gadgetbridge.service.btbr.TransactionBuilder initializeDevice(nodomain.freeyourgadget.gadgetbridge.service.btbr.TransactionBuilder builder) {
        setup(brSupport.getDevice(), brSupport.getContext());
        builder.setCallback(brSupport);
        builder.add(new nodomain.freeyourgadget.gadgetbridge.service.btbr.actions.SetDeviceStateAction(getDevice(), GBDevice.State.AUTHENTICATING, getContext()));
        final GetLinkParamsRequest linkParamsReq = new GetLinkParamsRequest(this, builder);
        initializeDevice(linkParamsReq);
        getCoordinator().setDevice(this.gbDevice);
        return builder;
    }

    protected void initializeDevice(final Request linkParamsReq) {
        deviceMac = this.gbDevice.getAddress();
        createRandomMacAddress();
        createAndroidID();
        try {
            RequestCallback finalizeReq = new RequestCallback() {
                @Override
                public void call() {
                    initializeDeviceCheckStatus(linkParamsReq);
                }

                @Override
                public void handleException(Request.ResponseParseException e) {
                    LOG.error("Link params TLV exception", e);
                }
            };
            linkParamsReq.setFinalizeReq(finalizeReq);
            linkParamsReq.doPerform();
        } catch (IOException e) {
            GB.toast(context, "Initialization of authenticating to Huawei device failed", Toast.LENGTH_SHORT, GB.ERROR, e);
            LOG.error("Initialization of authenticating to Huawei device failed", e);
        }

        /* This is to have the setting match the default Huawei behaviour */
        SharedPreferences sharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(getDeviceMac());
        if (!sharedPrefs.contains(DeviceSettingsPreferenceConst.PREF_DISCONNECTNOTIF_NOSHED)) {
            sharedPrefs.edit().putBoolean(DeviceSettingsPreferenceConst.PREF_DISCONNECTNOTIF_NOSHED, true).apply();
        }
    }

    protected void initializeDeviceCheckStatus(final Request linkParamsReq) {
        try {
            final GetDeviceStatusRequest deviceStatusReq = new GetDeviceStatusRequest(this, true);
            RequestCallback finalizeReq = new RequestCallback() {
                @Override
                public void call() {
                    int status = deviceStatusReq.status;
                    if (status == -0x01 || status == 0x00 || status == 0x01) {
                        initializeDeviceDealHiChain(linkParamsReq);
                    } else {
                        initializeDeviceNotify();
                    }
                }

                @Override
                public void handleException(Request.ResponseParseException e) {
                    LOG.error("Status TLV exception", e);
                }
            };
            if (huaweiType == HuaweiDeviceType.BLE) { //Only BLE known, check later for AW and SMART
                initializeDeviceDealHiChain(linkParamsReq);
            } else {
                deviceStatusReq.setFinalizeReq(finalizeReq);
                deviceStatusReq.doPerform();
            }
        } catch (IOException e) {
            GB.toast(context, "Status of authenticating to Huawei device failed", Toast.LENGTH_SHORT, GB.ERROR, e);
            LOG.error("Status of authenticating to Huawei device failed", e);
        }
    }

    protected boolean isHiChain() {
        // In HH
        // HiChain : 1 || 3
        // HiChainLite : 2 || 3 || 8
        // HiChain3 : 4 & API>=23 - API is always >=23
        // For GB we will consider for authMode
        // 0 : No HiChain
        // 1 or 3 : HiChain
        // 2 or 8 : HiChainLite -> normal mode
        // 4 : HiChain3
        byte authMode = paramsProvider.getDeviceSupportType();
        return authMode == 0x01 || authMode == 0x03 || authMode == 0x04 || isHiChainLite();
    }

    protected boolean isHiChainLite() {
        byte authMode = paramsProvider.getDeviceSupportType();
        return authMode == 0x02;
    }

    protected boolean isHiChain3(int authType) {
        return (authType ^ 0x01) == 0x04 || (authType ^ 0x02) == 0x04;
    }

    protected void initializeDeviceDealHiChain(final Request linkParamsReq) {
        try {
            if (isHiChain()) {

                if (paramsProvider.getDeviceSupportType() == 4)
                    paramsProvider.setAuthMode((byte) 4);
                else
                    paramsProvider.setAuthMode((byte) 2);
                final GetSecurityNegotiationRequest securityNegoReq = new GetSecurityNegotiationRequest(this);
                RequestCallback securityFinalizeReq = new RequestCallback(this) {
                    @Override
                    public void call() {
                        if (securityNegoReq.authType == 0x0186A0 || isHiChain3(securityNegoReq.authType)) {
                            LOG.debug("HiChain mode");
                            initializeDeviceHiChainMode();
                        } else if (securityNegoReq.authType == 0x01 || securityNegoReq.authType == 0x02) {
                            LOG.debug("HiChain Lite mode");
                            // Keep track the gadget is connected
                            initializeDeviceHiChainLiteMode(linkParamsReq);
                        }
                    }
                };
                securityNegoReq.setFinalizeReq(securityFinalizeReq);
                securityNegoReq.doPerform();
            } else {
                LOG.debug("Normal mode");
                initializeDeviceNormalMode(linkParamsReq);
            }
        } catch (IOException e) {
            GB.toast(context, "Init deal with HiChain of Huawei device failed", Toast.LENGTH_SHORT, GB.ERROR, e);
            LOG.error("Init deal with HiChain of Huawei device failed", e);
        }
    }

    protected void initializeDeviceNotify() {
        // TODO: Implement
    }

    RequestCallback configureReq = new RequestCallback() {
        @Override
        public void call() {
            initializeDeviceConfigure();
        }

        @Override
        public void timeout(Request request) {
            LOG.error("Authentication timed out");
            GB.toast(context, R.string.authentication_failed_negotiation, Toast.LENGTH_LONG, GB.ERROR);
            // Disconnect as no communication can succeed after this point
            final GBDevice device = getDevice();
            if (device != null) {
                GBApplication.deviceService(device).disconnect();
            }
        }

        @Override
        public void handleException(Request.ResponseParseException e) {
            LOG.error("Authentication exception", e);
            GB.toast(context, R.string.authentication_failed_negotiation, Toast.LENGTH_LONG, GB.ERROR);
            // Disconnect as no communication can succeed after this point
            final GBDevice device = getDevice();
            if (device != null) {
                GBApplication.deviceService(device).disconnect();
            }
        }
    };

    protected void initializeDeviceHiChainMode() {
        try {
            GetHiChainRequest hiChainReq = new GetHiChainRequest(this, firstConnection);
            hiChainReq.setFinalizeReq(configureReq);
            if (firstConnection) {
                GetPincodeRequest pincodeReq = new GetPincodeRequest(this);
                pincodeReq.nextRequest(hiChainReq);
                pincodeReq.doPerform();
            } else
                hiChainReq.doPerform();
        } catch (IOException e) {
            GB.toast(context, "HiChain Mode init of Huawei device failed", Toast.LENGTH_SHORT, GB.ERROR, e);
            LOG.error("HiChain Mode init of Huawei device failed", e);
        }
    }

    protected void initializeDeviceHiChainLiteMode(Request linkParamsReq) {
        try {
            createSecretKey();
            GetAuthRequest authReq = new GetAuthRequest(this, linkParamsReq);
            GetBondParamsRequest bondParamsReq = new GetBondParamsRequest(this);
            GetBondRequest bondReq = new GetBondRequest(this);
            authReq.nextRequest(bondParamsReq);
            bondParamsReq.nextRequest(bondReq);
            bondParamsReq.setFinalizeReq(configureReq);
            bondReq.setFinalizeReq(configureReq);
            if (paramsProvider.getPinCode() == null & paramsProvider.getAuthVersion() != 0x02) {
                GetPincodeRequest pinCodeReq = new GetPincodeRequest(this);
                pinCodeReq.nextRequest(authReq);
                pinCodeReq.doPerform();
            } else {
                authReq.doPerform();
            }
        } catch (IOException e) {
            GB.toast(context, "HiChainLite mode init of Huawei device failed", Toast.LENGTH_SHORT, GB.ERROR, e);
            LOG.error("HiChainLite mode init of Huawei device failed", e);
        }
    }

    protected void initializeDeviceNormalMode(Request linkParamsReq) {
        try {
            createSecretKey();
            GetAuthRequest authReq = new GetAuthRequest(this, linkParamsReq);
            if (getHuaweiType() == HuaweiDeviceType.BLE || getHuaweiType() == HuaweiDeviceType.AW) {
                GetBondParamsRequest bondParamsReq = new GetBondParamsRequest(this);
                GetBondRequest bondReq = new GetBondRequest(this);
                authReq.nextRequest(bondParamsReq);
                bondParamsReq.nextRequest(bondReq);
                bondParamsReq.setFinalizeReq(configureReq);
                bondReq.setFinalizeReq(configureReq);
            } else {
                authReq.setFinalizeReq(configureReq);
            }
            authReq.doPerform();
        } catch (IOException e) {
            GB.toast(context, "Normal mode init of Huawei device failed", Toast.LENGTH_SHORT, GB.ERROR, e);
            LOG.error("Normal mode init of Huawei device failed", e);
        }
    }

    protected void initializeDeviceConfigure() {
        if (isBLE()) {
            nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder leBuilder = createLeTransactionBuilder("Initializing");
            leBuilder.setCallback(leSupport);
            if (!GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getBoolean("force_new_protocol", false))
                leBuilder.notify(leSupport.getCharacteristic(HuaweiConstants.UUID_CHARACTERISTIC_HUAWEI_READ), true);
            leBuilder.add(new nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction(gbDevice, GBDevice.State.INITIALIZING, context));
        } else {
            nodomain.freeyourgadget.gadgetbridge.service.btbr.TransactionBuilder brBuilder = createBrTransactionBuilder("Initializing");
            brBuilder.setCallback(brSupport);
            brBuilder.add(new nodomain.freeyourgadget.gadgetbridge.service.btbr.actions.SetDeviceStateAction(gbDevice, GBDevice.State.INITIALIZING, context));
        }
        try {
            if (firstConnection) {
                // Workaround to enable PREF_HUAWEI_ROTATE_WRIST_TO_SWITCH_INFO preference
                SharedPreferences sharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(deviceMac);
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putString(DeviceSettingsPreferenceConst.PREF_ACTIVATE_DISPLAY_ON_LIFT, "p_on");
                editor.apply();
            }

            huaweiP2PManager.unregisterAllService();
            stopBatteryRunnerDelayed();
            GetBatteryLevelRequest batteryLevelReq = new GetBatteryLevelRequest(this);
            batteryLevelReq.setFinalizeReq(new RequestCallback() {
                @Override
                public void timeout(Request request) {
                    request.handleNext();
                    // Start the battery runner again so it keeps running even if the timeout is hit
                    startBatteryRunnerDelayed();
                }
            });

            final List<Request> initRequestQueue = new ArrayList<>();
            initRequestQueue.add(new GetProductInformationRequest(this));
            initRequestQueue.add(new SetTimeRequest(this, true));
            initRequestQueue.add(batteryLevelReq);
            initRequestQueue.add(new GetSupportedServicesRequest(this)); // MUST BE LAST - it indirectly kicks off initializeDynamicServices

            // Queue all the requests
            for (int i = 1; i < initRequestQueue.size(); i++) {
                initRequestQueue.get(i - 1).setupTimeoutUntilNext(initTimeout);
                initRequestQueue.get(i - 1).nextRequest(initRequestQueue.get(i));
            }
            initRequestQueue.get(initRequestQueue.size() - 1).setupTimeoutUntilNext(initTimeout);

            initRequestQueue.get(0).doPerform();
        } catch (IOException e) {
            GB.toast(context, "Final initialization of Huawei device failed", Toast.LENGTH_SHORT, GB.ERROR, e);
            LOG.error("Final initialization of Huawei device failed", e);
        }
    }

    public void createSecretKey() {
        SharedPreferences sharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(deviceMac);

        String authKey = sharedPrefs.getString("authkey", null);
        if (authKey == null || authKey.isEmpty()) {
            SharedPreferences.Editor editor = sharedPrefs.edit();

            authKey = StringUtils.bytesToHex(HuaweiCrypto.generateNonce());
            editor.putString("authkey", authKey);
            editor.apply();
        }
        paramsProvider.setSecretKey(GB.hexStringToByteArray(authKey));
    }

    public byte[] getSecretKey() {
        SharedPreferences sharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(deviceMac);

        String authKey = sharedPrefs.getString("authkey", null);

        // TODO: Handle null key - maybe error out of the entire connection?

        return GB.hexStringToByteArray(authKey);
    }

    public void setSecretKey(byte[] authKey) {
        SharedPreferences sharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(deviceMac);

        SharedPreferences.Editor editor = sharedPrefs.edit();

        editor.putString("authkey", StringUtils.bytesToHex(authKey));
        editor.apply();
        paramsProvider.setSecretKey(authKey);
    }

    public HuaweiCoordinatorSupplier.HuaweiDeviceType getHuaweiType() {
        return this.huaweiType;
    }

    public HuaweiPacket.ParamsProvider getParamsProvider() {
        return paramsProvider;
    }

    public void setFirstConnection(boolean firstConnection) {
        this.firstConnection = firstConnection;
    }

    protected void createRandomMacAddress() {
        SharedPreferences sharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(deviceMac);

        macAddress = sharedPrefs.getString(HuaweiConstants.PREF_HUAWEI_ADDRESS, null);
        if (macAddress == null || macAddress.isEmpty()) {
            StringBuilder mac = new StringBuilder("FF:FF:FF");
            Random r = new Random();
            for (int i = 0; i < 3; i++) {
                int n = r.nextInt(255);
                mac.append(String.format(":%02x", n));
            }
            macAddress = mac.toString().toUpperCase();
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putString(HuaweiConstants.PREF_HUAWEI_ADDRESS, macAddress);
            editor.apply();
        }
    }

    public byte[] getMacAddress() {
        return macAddress.getBytes(StandardCharsets.UTF_8);
    }

    public byte[] getSerial() {
        return macAddress.replace(":", "").substring(6, 12).getBytes(StandardCharsets.UTF_8);
    }

    public String getDeviceMac() {
        return deviceMac;
    }

    protected void createAndroidID() {
        SharedPreferences sharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(deviceMac);

        androidID = sharedPrefs.getString(DeviceSettingsPreferenceConst.PREF_FAKE_ANDROID_ID, null);
        if (androidID == null || androidID.isEmpty()) {
            androidID = StringUtils.bytesToHex(HuaweiCrypto.generateNonce());
            LOG.debug("Created androidID: {}", androidID);
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putString(DeviceSettingsPreferenceConst.PREF_FAKE_ANDROID_ID, androidID);
            editor.apply();
        }
    }

    public byte[] getAndroidId() {
        return androidID.getBytes(StandardCharsets.UTF_8);
    }

    public Context getContext() {
        return context;
    }

    public GBDevice getDevice() {
        return gbDevice;
    }

    /**
     * Initialize the services that may or may not be supported on the device
     * To be called after the commandsPerService is filled in the coordinator
     */
    public void initializeDynamicServices() {
        try {
            // All of the below check that they are supported and otherwise they skip themselves
            final List<Request> initRequestQueue = new ArrayList<>();
            initRequestQueue.add(new GetExpandCapabilityRequest(this));
            initRequestQueue.add(new SendExtendedAccountRequest(this));
            initRequestQueue.add(new GetSettingRelatedRequest(this));
            initRequestQueue.add(new AcceptAgreementsRequest(this));
            initRequestQueue.add(new GetActivityTypeRequest(this));
            initRequestQueue.add(new GetConnectStatusRequest(this));
            initRequestQueue.add(new GetDndLiftWristTypeRequest(this));
            initRequestQueue.add(new SendDndDeleteRequest(this));
            initRequestQueue.add(new SendDndAddRequest(this));
            initRequestQueue.add(new SendSetUpDeviceStatusRequest(this));
            initRequestQueue.add(new GetWearStatusRequest(this));
            initRequestQueue.add(new SendMenstrualCapabilityRequest(this));
            initRequestQueue.add(new SendNotifyHeartRateCapabilityRequest(this));
            initRequestQueue.add(new SendNotifyRestHeartRateCapabilityRequest(this));
            initRequestQueue.add(new SendFitnessUserInfoRequest(this));
            initRequestQueue.add(new SendRunPaceConfigRequest(this));
            initRequestQueue.add(new SendDeviceReportThreshold(this));
            initRequestQueue.add(new SendHeartRateZonesConfig(this));
            initRequestQueue.add(new SetMediumToStrengthThresholdRequest(this));
            initRequestQueue.add(new SendFitnessGoalRequest(this));
            initRequestQueue.add(new GetNotificationCapabilitiesRequest(this));
            initRequestQueue.add(new GetNotificationConstraintsRequest(this));
            initRequestQueue.add(new GetWatchfaceParams(this));
            initRequestQueue.add(new SendCameraRemoteSetupEvent(this, CameraRemote.CameraRemoteSetup.Request.Event.ENABLE_CAMERA));
            initRequestQueue.add(new GetAppInfoParams(this));
            initRequestQueue.add(new GetMusicInfoParams(this));
            initRequestQueue.add(new GetExtendedMusicInfoParams(this));
            initRequestQueue.add(new SetActivateOnLiftRequest(this));
            initRequestQueue.add(new SetWearLocationRequest(this));
            initRequestQueue.add(new SetNavigateOnRotateRequest(this));
            initRequestQueue.add(new SetNotificationRequest(this));
            initRequestQueue.add(new SetWearMessagePushRequest(this));
            initRequestQueue.add(new SetTimeZoneIdRequest(this));
            initRequestQueue.add(new SetLanguageSettingRequest(this));
            initRequestQueue.add(new SetDateFormatRequest(this));
            initRequestQueue.add(new SetActivityReminderRequest(this));
            initRequestQueue.add(new SetTruSleepRequest(this));
            initRequestQueue.add(new GetContactsCount(this));
            initRequestQueue.add(new GetEventAlarmList(this));
            initRequestQueue.add(new GetSmartAlarmList(this));

            // Setup the alarms if necessary
            if (!getHuaweiCoordinator().supportsChangingAlarm() && firstConnection)
                initializeAlarms();

            // Queue all the requests
            for (int i = 1; i < initRequestQueue.size(); i++) {
                initRequestQueue.get(i - 1).setupTimeoutUntilNext(initTimeout);
                if (initRequestQueue.get(i - 1) instanceof SendSetUpDeviceStatusRequest) {
                    // NOTE: The watch is never answer to this command. To decrease init time timeout for it is 50 ms
                    initRequestQueue.get(i - 1).setupTimeoutUntilNext(50);
                }
                initRequestQueue.get(i - 1).nextRequest(initRequestQueue.get(i));
            }

            initRequestQueue.get(initRequestQueue.size() - 1).setupTimeoutUntilNext(initTimeout);
            initRequestQueue.get(initRequestQueue.size() - 1).setFinalizeReq(new RequestCallback() {
                @Override
                public void call() {
                    gbDevice.setState(GBDevice.State.INITIALIZED);
                    gbDevice.sendDeviceUpdateIntent(getContext(), GBDevice.DeviceUpdateSubject.DEVICE_STATE);

                    if (getHuaweiCoordinator().supportsP2PService()) {
                        if (getHuaweiCoordinator().supportsCalendar()) {
                            if (HuaweiP2PCalendarService.getRegisteredInstance(huaweiP2PManager) == null) {
                                HuaweiP2PCalendarService calendarService = new HuaweiP2PCalendarService(huaweiP2PManager);
                                calendarService.register();
                            }
                        }
                        if (getHuaweiCoordinator().supportsTrack()) {
                            if (HuaweiP2PTrackService.getRegisteredInstance(huaweiP2PManager) == null) {
                                HuaweiP2PTrackService trackService = new HuaweiP2PTrackService(huaweiP2PManager);
                                trackService.register();
                            }
                        }
                        if (HuaweiP2PDataDictionarySyncService.getRegisteredInstance(huaweiP2PManager) == null) {
                            HuaweiP2PDataDictionarySyncService trackService = new HuaweiP2PDataDictionarySyncService(huaweiP2PManager);
                            trackService.register();
                        }

                    }
                }
            });

            initRequestQueue.get(0).doPerform();
        } catch (IOException e) {
            GB.toast("Initialize dynamic services of Huawei device failed", Toast.LENGTH_SHORT, GB.ERROR, e);
            LOG.error("Initializing dynamic services of Huawei device failed", e);
        }
    }

    public void setProtocolVersion(byte protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public byte getProtocolVersion() {
        return this.protocolVersion;
    }

    private void initializeAlarms() {
        // TODO: check for smart alarm && overwrite for smart alarm
        //       note that lowering the alarm count shouldn't delete the alarm of course...

        // Populate alarms in order to specify important data
        List<Alarm> alarms = DBHelper.getAlarms(gbDevice);
        DeviceCoordinator coordinator = this.gbDevice.getDeviceCoordinator();
        int supportedNumAlarms = coordinator.getAlarmSlotCount(gbDevice);
        if (alarms.isEmpty()) {
            try (DBHandler db = GBApplication.acquireDB()) {
                DaoSession daoSession = db.getDaoSession();
                Device device = DBHelper.getDevice(gbDevice, daoSession);
                User user = DBHelper.getUser(daoSession);
                for (int position = 0; position < supportedNumAlarms; position++) {
                    LOG.info("Adding missing alarm at position {}", position);
                    DBHelper.store(createDefaultAlarm(device, user, position));
                }
            } catch (Exception e) {
                // TODO: show user?
                // TODO: What exceptions can happen here?
                LOG.error("Error accessing database", e);
            }
        }
    }

    private Alarm createDefaultAlarm(@NonNull Device device, @NonNull User user, int position) {
        boolean smartWakeup = false;
        String title = context.getString(R.string.menuitem_alarm);
        String description = context.getString(R.string.huawei_alarm_event_description);
        if (position == 0) {
            smartWakeup = true;
            title = context.getString(R.string.alarm_smart_wakeup);
            description = context.getString(R.string.huawei_alarm_smart_description);
        }
        return new Alarm(device.getId(), user.getId(), position, false, smartWakeup, null, false, 0, 6, 30, true, title, description);
    }

    private void getAlarms() {
        if (!getHuaweiCoordinator().supportsChangingAlarm())
            return;

        GetEventAlarmList getEventAlarmList = new GetEventAlarmList(this);
        getEventAlarmList.setFinalizeReq(new RequestCallback() {
            @Override
            public void call() {
                if (!getHuaweiCoordinator().supportsSmartAlarm(getDevice()))
                    return; // Don't get smart alarms when not supported

                GetSmartAlarmList getSmartAlarmList = new GetSmartAlarmList(HuaweiSupportProvider.this);
                try {
                    getSmartAlarmList.doPerform();
                } catch (IOException e) {
                    GB.toast(context, "Error sending smart alarm list request", Toast.LENGTH_SHORT, GB.ERROR, e);
                    LOG.error("Error sending smart alarm list request", e);
                }
            }

            @Override
            public void handleException(Request.ResponseParseException e) {
                GB.toast(context, "Error parsing event list", Toast.LENGTH_SHORT, GB.ERROR, e);
                LOG.error("Error parsing event list", e);
            }
        });
        try {
            getEventAlarmList.doPerform();
        } catch (IOException e) {
            GB.toast(context, "Error sending event alarm list request", Toast.LENGTH_SHORT, GB.ERROR, e);
            LOG.error("Error sending event alarm list request", e);
        }
    }

    public void saveAlarms(Alarm[] alarms) {
        try (DBHandler db = GBApplication.acquireDB()) {
            DaoSession daoSession = db.getDaoSession();
            Device device = DBHelper.getDevice(gbDevice, daoSession);
            User user = DBHelper.getUser(daoSession);
            for (Alarm alarm : alarms) {
                alarm.setDeviceId(device.getId());
                alarm.setUserId(user.getId());
                DBHelper.store(alarm);
            }
        } catch (Exception e) {
            GB.toast(context, "Error saving alarms", Toast.LENGTH_SHORT, GB.ERROR, e);
            LOG.error("Error saving alarms", e);
        }
    }

    public boolean onCharacteristicChanged(BluetoothGattCharacteristic characteristic) {
        byte[] data = characteristic.getValue();
        responseManager.handleData(data);
        return true;
    }

    public void onSocketRead(byte[] data) {
        responseManager.handleData(data);
    }

    public void removeInProgressRequests(Request req) {
        responseManager.removeHandler(req);
    }

    public void onSendConfiguration(String config) {
        try {
            switch (config) {
                case DeviceSettingsPreferenceConst.PREF_DATEFORMAT:
                case DeviceSettingsPreferenceConst.PREF_TIMEFORMAT: {
                    setDateFormat();
                    break;
                }
                case SettingsActivity.PREF_MEASUREMENT_SYSTEM:
                case DeviceSettingsPreferenceConst.PREF_LANGUAGE: {
                    setLanguageSetting();
                    break;
                }
                case DeviceSettingsPreferenceConst.PREF_WEARLOCATION: {
                    setWearLocation();
                    break;
                }
                case DeviceSettingsPreferenceConst.PREF_LIFTWRIST_NOSHED: {
                    setActivateOnLift();
                    break;
                }
                case MiBandConst.PREF_MI2_ROTATE_WRIST_TO_SWITCH_INFO: {
                    setNavigateOnRotate();
                    break;
                }
                case DeviceSettingsPreferenceConst.PREF_INACTIVITY_ENABLE:
                case DeviceSettingsPreferenceConst.PREF_INACTIVITY_THRESHOLD:
                case DeviceSettingsPreferenceConst.PREF_INACTIVITY_START:
                case DeviceSettingsPreferenceConst.PREF_INACTIVITY_END:
                case DeviceSettingsPreferenceConst.PREF_INACTIVITY_MO:
                case DeviceSettingsPreferenceConst.PREF_INACTIVITY_TU:
                case DeviceSettingsPreferenceConst.PREF_INACTIVITY_WE:
                case DeviceSettingsPreferenceConst.PREF_INACTIVITY_TH:
                case DeviceSettingsPreferenceConst.PREF_INACTIVITY_FR:
                case DeviceSettingsPreferenceConst.PREF_INACTIVITY_SA:
                case DeviceSettingsPreferenceConst.PREF_INACTIVITY_SU: {
                    setActivityReminder();
                    break;
                }
                case HuaweiConstants.PREF_HUAWEI_TRUSLEEP: {
                    setTrusleep();
                    break;
                }
                case HuaweiConstants.PREF_HUAWEI_CONTINUOUS_SKIN_TEMPERATURE_MEASUREMENT: {
                    setContinuousSkinTemperatureMeasurement();
                    break;
                }
                case DeviceSettingsPreferenceConst.PREF_TEMPERATURE_SCALE_CF: {
                    setTemperatureUnit();
                    break;
                }
                case DeviceSettingsPreferenceConst.PREF_NOTIFICATION_ENABLE: {
                    setNotificationStatus();
                    break;
                }
                case HuaweiConstants.PREF_HUAWEI_WORKMODE:
                    SetWorkModeRequest setWorkModeReq = new SetWorkModeRequest(this);
                    setWorkModeReq.doPerform();
                    break;
                case DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB:
                case DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_START:
                case DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_END:
                case DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_LIFT_WRIST:
                case DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_MO:
                case DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_TU:
                case DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_WE:
                case DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_TH:
                case DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_FR:
                case DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_SA:
                case DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_SU: {
                    setDnd();
                    break;
                }
                case DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_NOT_WEAR:
                    setDndNotWear();
                    break;
                case DeviceSettingsPreferenceConst.PREF_FIND_PHONE:
                case DeviceSettingsPreferenceConst.PREF_FIND_PHONE_DURATION:
                    // TODO: enable/disable the find phone applet on band
                    break;
                case DeviceSettingsPreferenceConst.PREF_DISCONNECTNOTIF_NOSHED:
                    setDisconnectNotification();
                    break;
                case DeviceSettingsPreferenceConst.PREF_HEARTRATE_AUTOMATIC_ENABLE:
                    setHeartrateAutomatic();
                    break;
                case DeviceSettingsPreferenceConst.PREF_SPO_AUTOMATIC_ENABLE:
                    setSpoAutomatic();
                    break;
                case DeviceSettingsPreferenceConst.PREF_FORCE_ENABLE_SMART_ALARM:
                    getAlarms();
                    break;
                case HuaweiConstants.PREF_HUAWEI_DEBUG_REQUEST:
                    sendDebugRequest();
                    break;
                case ActivityUser.PREF_USER_STEPS_GOAL:
                    new SendFitnessGoalRequest(this).doPerform();
                    break;
                case DeviceSettingsPreferenceConst.PREF_CAMERA_REMOTE:
                    if (GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getBoolean(DeviceSettingsPreferenceConst.PREF_CAMERA_REMOTE, false)) {
                        SendCameraRemoteSetupEvent sendCameraRemoteSetupEvent = new SendCameraRemoteSetupEvent(this, CameraRemote.CameraRemoteSetup.Request.Event.ENABLE_CAMERA);
                        sendCameraRemoteSetupEvent.doPerform();
                    } else {
                        // Somehow it is impossible to disable the camera remote
                        // But it will disappear after reconnection - until it is enabled again
                        GB.toast(context, context.getString(R.string.toast_setting_requires_reconnect), Toast.LENGTH_SHORT, GB.INFO);
                    }
                case DeviceSettingsPreferenceConst.PREF_BATTERY_POLLING_ENABLE:
                    if (!GBApplication.getDevicePrefs(gbDevice).getBatteryPollingEnabled()) {
                        stopBatteryRunnerDelayed();
                        break;
                    }
                    // Fall through if enabled
                case DeviceSettingsPreferenceConst.PREF_BATTERY_POLLING_INTERVAL:
                    if (!startBatteryRunnerDelayed()) {
                        GB.toast(context, R.string.battery_polling_failed_start, Toast.LENGTH_SHORT, GB.ERROR);
                        LOG.error("Failed to start the battery polling");
                    }
                    break;
                case ActivityUser.PREF_USER_WEIGHT_KG:
                case ActivityUser.PREF_USER_HEIGHT_CM:
                case ActivityUser.PREF_USER_GENDER:
                case ActivityUser.PREF_USER_DATE_OF_BIRTH:
                    sendUserInfo();
                    break;
                case DeviceSettingsPreferenceConst.PREF_SYNC_CALENDAR:
                case DeviceSettingsPreferenceConst.PREF_CALENDAR_LOOKAHEAD_DAYS:
                    HuaweiP2PCalendarService.getRegisteredInstance(huaweiP2PManager).restartSynchronization();
                    break;
            }
        } catch (IOException e) {
            GB.toast(context, "Configuration of Huawei device failed", Toast.LENGTH_SHORT, GB.ERROR, e);
            LOG.error("Configuration of Huawei device failed", e);
        }
    }

    public void onFetchRecordedData(int dataTypes) {
        for (int i = 1; i > -1; i <<= 1) {
            if ((dataTypes & i) != 0) {
                switch (i) {
                    case RecordedDataTypes.TYPE_ACTIVITY:
                        this.syncState.addActivitySyncToQueue();
                        break;
                    case RecordedDataTypes.TYPE_GPS_TRACKS:
                        this.syncState.addWorkoutSyncToQueue();
                        break;
                    // Ignore the following because we know/they are included in the others
                    case RecordedDataTypes.TYPE_SPO2:
                    case RecordedDataTypes.TYPE_STRESS:
                    case RecordedDataTypes.TYPE_HEART_RATE:
                    case RecordedDataTypes.TYPE_PAI:
                    case RecordedDataTypes.TYPE_SLEEP_RESPIRATORY_RATE:
                        break;
                    default:
                        LOG.warn("Recorded data type {} not implemented yet.", i);
                }
            }
        }
        if (gbDevice.isBusy())
            LOG.warn("Device is already busy with {}, so won't fetch data now.", gbDevice.getBusyTask());
        else
            fetchRecodedDataFromQueue();

        // Get the battery level as well
        getBatteryLevel();

        // Get the alarms as they cannot be retrieved on opening the alarm window
        // TODO: get the alarms if the alarm settings are opened instead of here
        getAlarms();
    }

    protected void fetchRecodedDataFromQueue() {
        int dataType = this.syncState.getCurrentSyncType();
        if (dataType == -1)
            return; // Empty queue

        if (dataType == RecordedDataTypes.TYPE_ACTIVITY) {
            fetchActivityData();
        } else if (dataType == RecordedDataTypes.TYPE_GPS_TRACKS) {
            fetchWorkoutData();
        }
    }

    private void fetchActivityData() {
        syncState.setActivitySync(true);
        fetchActivityDataP2P();

        int sleepStart = 0;
        int stepStart = 0;
        final int end = (int) (System.currentTimeMillis() / 1000);

        SharedPreferences sharedPreferences = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress());
        long prefLastSyncTime = sharedPreferences.getLong("lastSyncTimeMillis", 0);
        if (prefLastSyncTime != 0) {
            sleepStart = (int) (prefLastSyncTime / 1000);
            stepStart = (int) (prefLastSyncTime / 1000);

            // Reset for next calls
            sharedPreferences.edit().putLong("lastSyncTimeMillis", 0).apply();
        } else {
            try (DBHandler db = GBApplication.acquireDB()) {
                HuaweiSampleProvider sampleProvider = new HuaweiSampleProvider(gbDevice, db.getDaoSession());
                sleepStart = sampleProvider.getLastSleepFetchTimestamp();
                stepStart = sampleProvider.getLastStepFetchTimestamp();
            } catch (Exception e) {
                LOG.warn("Exception for getting start times, using 01/01/2000 - 00:00:00.");
            }

            // Some bands don't work with zero timestamp, so starting later
            if (sleepStart == 0)
                sleepStart = 946684800;
            if (stepStart == 0)
                stepStart = 946684800;
        }
        final GetSleepDataCountRequest getSleepDataCountRequest;
        if (isBLE()) {
            nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder leBuilder = createLeTransactionBuilder("FetchRecordedData");
            leBuilder.add(new nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceBusyAction(gbDevice, context.getString(R.string.busy_task_fetch_activity_data), context));
            getSleepDataCountRequest = new GetSleepDataCountRequest(this, leBuilder, sleepStart, end);
        } else {
            nodomain.freeyourgadget.gadgetbridge.service.btbr.TransactionBuilder brBuilder = createBrTransactionBuilder("FetchRecordedData");
            brBuilder.add(new nodomain.freeyourgadget.gadgetbridge.service.btbr.actions.SetDeviceBusyAction(gbDevice, context.getString(R.string.busy_task_fetch_activity_data), context));
            getSleepDataCountRequest = new GetSleepDataCountRequest(this, brBuilder, sleepStart, end);
        }

        final GetStepDataCountRequest getStepDataCountRequest = new GetStepDataCountRequest(this, stepStart, end);
        //noinspection ExtractMethodRecommender
        final GetFitnessTotalsRequest getFitnessTotalsRequest = new GetFitnessTotalsRequest(this);

        final int start = sleepStart;
        getFitnessTotalsRequest.setFinalizeReq(new RequestCallback() {
            @Override
            public void call() {
                if (!downloadTruSleepData(start, end))
                    syncState.setActivitySync(false);
            }

            @Override
            public void handleException(Request.ResponseParseException e) {
                LOG.error("Fitness totals exception", e);
                syncState.setActivitySync(false);
            }
        });


        getStepDataCountRequest.setFinalizeReq(new RequestCallback() {
            @Override
            public void call() {
                try {
                    getFitnessTotalsRequest.doPerform();
                } catch (IOException e) {
                    LOG.error("Exception on starting fitness totals request", e);
                    syncState.setActivitySync(false);
                }
            }

            @Override
            public void handleException(Request.ResponseParseException e) {
                LOG.error("Step data count exception", e);
                syncState.setActivitySync(false);
            }
        });

        getSleepDataCountRequest.setFinalizeReq(new RequestCallback() {
            @Override
            public void call() {
                try {
                    getStepDataCountRequest.doPerform();
                } catch (IOException e) {
                    LOG.error("Exception on starting step data count request", e);
                    syncState.setActivitySync(false);
                }
            }

            @Override
            public void handleException(Request.ResponseParseException e) {
                LOG.error("Sleep data count exception", e);
                syncState.setActivitySync(false);
            }
        });

        try {
            getSleepDataCountRequest.doPerform();
        } catch (IOException e) {
            LOG.error("Exception on starting sleep data count request", e);
            syncState.setActivitySync(false);
        }
    }

    private void fetchActivityDataP2P() {
        HuaweiP2PDataDictionarySyncService P2PSyncService = HuaweiP2PDataDictionarySyncService.getRegisteredInstance(huaweiP2PManager);

        if (P2PSyncService != null) {
            List<Integer> list = P2PSyncService.checkSupported(this.getHuaweiCoordinator(), Arrays.asList(HuaweiDictTypes.SKIN_TEMPERATURE_CLASS, HuaweiDictTypes.BLOOD_PRESSURE_CLASS));
            if(!list.isEmpty()) {
                syncState.setP2pSync(true);
                P2PSyncService.startSync(list, new HuaweiP2PDataDictionarySyncService.DictionarySyncCallback() {
                    @Override
                    public void onComplete(boolean complete) {
                        LOG.info("Sync P2P Data complete");
                        syncState.setP2pSync(false);
                    }
                });
            }
        }
    }

    private void fetchWorkoutData() {
        syncState.setWorkoutSync(true);

        int start = 0;
        int end = (int) (System.currentTimeMillis() / 1000);

        SharedPreferences sharedPreferences = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress());
        long prefLastSyncTime = sharedPreferences.getLong("lastSportsActivityTimeMillis", 0);
        if (prefLastSyncTime != 0) {
            start = (int) (prefLastSyncTime / 1000);

            // Reset for next calls
            sharedPreferences.edit().putLong("lastSportsActivityTimeMillis", 0).apply();
        } else {
            try (DBHandler db = GBApplication.acquireDB()) {
                Long userId = DBHelper.getUser(db.getDaoSession()).getId();
                Long deviceId = DBHelper.getDevice(gbDevice, db.getDaoSession()).getId();

                QueryBuilder<HuaweiWorkoutSummarySample> qb1 = db.getDaoSession().getHuaweiWorkoutSummarySampleDao().queryBuilder().where(
                        HuaweiWorkoutSummarySampleDao.Properties.DeviceId.eq(deviceId),
                        HuaweiWorkoutSummarySampleDao.Properties.UserId.eq(userId)
                ).orderDesc(
                        HuaweiWorkoutSummarySampleDao.Properties.StartTimestamp
                ).limit(1);

                List<HuaweiWorkoutSummarySample> samples1 = qb1.list();
                if (!samples1.isEmpty())
                    start = samples1.get(0).getEndTimestamp();

                QueryBuilder<BaseActivitySummary> qb2 = db.getDaoSession().getBaseActivitySummaryDao().queryBuilder().where(
                        BaseActivitySummaryDao.Properties.DeviceId.eq(deviceId),
                        BaseActivitySummaryDao.Properties.UserId.eq(userId)
                ).orderDesc(
                        BaseActivitySummaryDao.Properties.StartTime
                ).limit(1);

                List<BaseActivitySummary> samples2 = qb2.list();
                if (!samples2.isEmpty())
                    start = Math.min(start, (int) (samples2.get(0).getEndTime().getTime() / 1000L));

                start = start + 1;
            } catch (Exception e) {
                LOG.warn("Exception for getting start time, using 10/06/2022 - 00:00:00.");
            }

            if (start == 0 || start == 1)
                start = 1654819200;
        }

        final GetWorkoutCountRequest getWorkoutCountRequest;
        if (isBLE()) {
            nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder leBuilder = createLeTransactionBuilder("FetchWorkoutData");
            leBuilder.add(new nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceBusyAction(gbDevice, context.getString(R.string.busy_task_fetch_activity_data), context));
            getWorkoutCountRequest = new GetWorkoutCountRequest(this, leBuilder, start, end);
        } else {
            nodomain.freeyourgadget.gadgetbridge.service.btbr.TransactionBuilder brBuilder = createBrTransactionBuilder("FetchWorkoutData");
            brBuilder.add(new nodomain.freeyourgadget.gadgetbridge.service.btbr.actions.SetDeviceBusyAction(gbDevice, context.getString(R.string.busy_task_fetch_activity_data), context));
            getWorkoutCountRequest = new GetWorkoutCountRequest(this, brBuilder, start, end);
        }

        getWorkoutCountRequest.setFinalizeReq(new RequestCallback() {
            @Override
            public void handleException(Request.ResponseParseException e) {
                // This is propagated through the workout requests, hence the slightly generic error message
                GB.toast(context, "Exception synchronizing workout", Toast.LENGTH_SHORT, GB.ERROR);
                LOG.error("Exception synchronizing workout", e);
                endOfWorkoutSync();
            }
        });

        try {
            getWorkoutCountRequest.doPerform();
        } catch (IOException e) {
            GB.toast(context, "Exception synchronizing workout", Toast.LENGTH_SHORT, GB.ERROR);
            LOG.error("Error sending workout count - showing user that the workout sync failed", e);
            endOfWorkoutSync();
        }
    }

    public void onReset(int flags) {
        try {
            if (flags == GBDeviceProtocol.RESET_FLAGS_FACTORY_RESET) {
                SendFactoryResetRequest sendFactoryResetReq = new SendFactoryResetRequest(this);
                sendFactoryResetReq.doPerform();
            }
        } catch (IOException e) {
            GB.toast(context, "Factory resetting Huawei device failed", Toast.LENGTH_SHORT, GB.ERROR, e);
            LOG.error("Factory resetting Huawei device failed", e);
        }
    }

    public void setNotificationStatus() {
        /*
         * TODO: this doesn't work as expected
         *       We thought it would disable(/enable) the notifications on the device side,
         *       but at least the disabling doesn't work - so we don't send notifications to the
         *       device at all if the setting is disabled now.
         *      TRYING to debug this as it should really be handled on device side...
         */
        try {
            SetNotificationRequest setNotificationReq = new SetNotificationRequest(this);
            setNotificationReq.doPerform();
//            SetWearMessagePushRequest setWearMessagePushReq = new SetWearMessagePushRequest(this);
//            setWearMessagePushReq.doPerform();
        } catch (IOException e) {
            GB.toast(context, "Setting notification failed", Toast.LENGTH_SHORT, GB.ERROR, e);
            LOG.error("Setting notification failed", e);
        }
    }

    public short getNotificationId() {
        if (msgId < 256) {
            msgId += 1;
        } else {
            msgId = 0;
        }
        return msgId;
    }

    public void onNotification(NotificationSpec notificationSpec) {
        if (!GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress()).getBoolean(DeviceSettingsPreferenceConst.PREF_NOTIFICATION_ENABLE, false)) {
            // Don't send notifications when they are disabled
            LOG.info("Stopped notification as they are disabled.");
            return;
        }

        SendNotificationRequest sendNotificationReq = new SendNotificationRequest(this);
        try {
            sendNotificationReq.buildNotificationTLVFromNotificationSpec(notificationSpec);
            sendNotificationReq.doPerform();
        } catch (IOException e) {
            LOG.error("Sending notification failed", e);
        }
    }

    public void setDateFormat() {
        try {
            SetDateFormatRequest setDateFormatReq = new SetDateFormatRequest(this);
            setDateFormatReq.doPerform();
        } catch (IOException e) {
            GB.toast(context, "Failed to configure date format", Toast.LENGTH_SHORT, GB.ERROR, e);
            LOG.error("Failed to configure date format", e);
        }
    }

    public void onSetTime() {
        try {
            new SetTimeRequest(this, true).doPerform();
        } catch (IOException e) {
            GB.toast(context, "Failed to configure time", Toast.LENGTH_SHORT, GB.ERROR, e);
            LOG.error("Failed to configure time", e);
        }
    }

    public void onSetAlarms(ArrayList<? extends nodomain.freeyourgadget.gadgetbridge.model.Alarm> alarms) {
        boolean smartAlarmEnabled = getHuaweiCoordinator().supportsSmartAlarm(getDevice());

        AlarmsRequest smartAlarmReq = new AlarmsRequest(this, true);
        AlarmsRequest eventAlarmReq = new AlarmsRequest(this, false);
        for (nodomain.freeyourgadget.gadgetbridge.model.Alarm alarm : alarms) {
            if (alarm.getPosition() == 0 && smartAlarmEnabled) {
                smartAlarmReq.buildSmartAlarm(alarm);
            } else {
                eventAlarmReq.addEventAlarm(alarm, !smartAlarmEnabled);
            }
        }
        try {
            if (smartAlarmEnabled)
                smartAlarmReq.doPerform();
            eventAlarmReq.doPerform();
        } catch (IOException e) {
            GB.toast(context, "Failed to configure alarms", Toast.LENGTH_SHORT, GB.ERROR, e);
            LOG.error("Failed to configure alarms", e);
        }
    }

    public void onSetCallState(CallSpec callSpec) {
        if (callSpec.command == CallSpec.CALL_INCOMING) {
            SendNotificationRequest sendNotificationReq = new SendNotificationRequest(this);
            try {
                sendNotificationReq.buildNotificationTLVFromCallSpec(callSpec);
                sendNotificationReq.doPerform();
            } catch (IOException e) {
                LOG.error("Failed to send start call notification", e);
            }
        } else if (
                callSpec.command == CallSpec.CALL_ACCEPT ||
                        callSpec.command == CallSpec.CALL_START ||
                        callSpec.command == CallSpec.CALL_REJECT ||
                        callSpec.command == CallSpec.CALL_END
        ) {
            StopNotificationRequest stopNotificationRequest = new StopNotificationRequest(this);
            try {
                stopNotificationRequest.doPerform();
            } catch (IOException e) {
                LOG.error("Failed to send stop call notification", e);
            }
        }
    }

    public void onSetMusicState(MusicStateSpec stateSpec) {
        if (mediaManager.onSetMusicState(stateSpec))
            sendSetMusic();
    }

    public void onSetMusicInfo(MusicSpec musicSpec) {
        if (mediaManager.onSetMusicInfo(musicSpec))
            sendSetMusic();
    }

    public void onSetPhoneVolume() {
        sendSetMusic();
    }

    public void refreshMediaManager() {
        mediaManager.refresh();
    }

    public void sendSetMusic() {
        // This often gets called twice in a row because of onSetMusicState and onSetMusicInfo
        // Maybe we can consolidate that into just one request?
        SetMusicRequest setMusicRequest = new SetMusicRequest(
                this,
                mediaManager.getBufferMusicStateSpec(),
                mediaManager.getBufferMusicSpec()
        );
        try {
            setMusicRequest.doPerform();
        } catch (IOException e) {
            LOG.error("Failed to send set music request", e);
        }
    }

    public void addInProgressRequest(Request request) {
        responseManager.addHandler(request);
    }

    public void addSleepActivity(int timestamp_start, int timestamp_end, byte type, byte source) {
        LOG.debug("Adding sleep activity between {} and {}", timestamp_start, timestamp_end);

        try (DBHandler db = GBApplication.acquireDB()) {
            Long userId = DBHelper.getUser(db.getDaoSession()).getId();
            Long deviceId = DBHelper.getDevice(gbDevice, db.getDaoSession()).getId();
            HuaweiSampleProvider sampleProvider = new HuaweiSampleProvider(gbDevice, db.getDaoSession());

            HuaweiActivitySample activitySample = new HuaweiActivitySample(
                    timestamp_start,
                    deviceId,
                    userId,
                    timestamp_end,
                    source,
                    type,
                    1,
                    ActivitySample.NOT_MEASURED,
                    ActivitySample.NOT_MEASURED,
                    ActivitySample.NOT_MEASURED,
                    ActivitySample.NOT_MEASURED,
                    ActivitySample.NOT_MEASURED
            );
            activitySample.setProvider(sampleProvider);

            sampleProvider.addGBActivitySample(activitySample);
        } catch (Exception e) {
            LOG.error("Failed to add sleep activity to database", e);
        }
    }

    public void addStepData(int timestamp, short steps, short calories, short distance, byte spo, byte heartrate) {
        try (DBHandler db = GBApplication.acquireDB()) {
            Long userId = DBHelper.getUser(db.getDaoSession()).getId();
            Long deviceId = DBHelper.getDevice(gbDevice, db.getDaoSession()).getId();
            HuaweiSampleProvider sampleProvider = new HuaweiSampleProvider(gbDevice, db.getDaoSession());

            HuaweiActivitySample activitySample = new HuaweiActivitySample(
                    timestamp,
                    deviceId,
                    userId,
                    timestamp + 60,
                    FitnessData.MessageData.stepId,
                    ActivitySample.NOT_MEASURED,
                    1,
                    steps,
                    calories,
                    distance,
                    spo,
                    heartrate
            );
            activitySample.setProvider(sampleProvider);

            sampleProvider.addGBActivitySample(activitySample);
        } catch (Exception e) {
            LOG.error("Failed to add step data to database", e);
        }
    }

    public void addTotalFitnessData(int steps, int calories, int distance) {
        LOG.debug("FITNESS total steps: {}", steps);
        LOG.debug("FITNESS total calories: {}", calories); // TODO: May actually be kilocalories
        LOG.debug("FITNESS total distance: {} m", distance);

        // TODO: potentially do more with this, maybe through realtime data?
    }

    public Long addWorkoutTotalsData(Workout.WorkoutTotals.Response packet) {
        try (DBHandler db = GBApplication.acquireDB()) {
            Long userId = DBHelper.getUser(db.getDaoSession()).getId();
            Long deviceId = DBHelper.getDevice(gbDevice, db.getDaoSession()).getId();

            // Avoid duplicates
            QueryBuilder<HuaweiWorkoutSummarySample> qb = db.getDaoSession().getHuaweiWorkoutSummarySampleDao().queryBuilder().where(
                    HuaweiWorkoutSummarySampleDao.Properties.UserId.eq(userId),
                    HuaweiWorkoutSummarySampleDao.Properties.DeviceId.eq(deviceId),
                    HuaweiWorkoutSummarySampleDao.Properties.WorkoutNumber.eq(packet.number),
                    HuaweiWorkoutSummarySampleDao.Properties.StartTimestamp.eq(packet.startTime),
                    HuaweiWorkoutSummarySampleDao.Properties.EndTimestamp.eq(packet.endTime)
            );
            List<HuaweiWorkoutSummarySample> results = qb.build().list();
            Long workoutId = null;
            if (!results.isEmpty())
                workoutId = results.get(0).getWorkoutId();

            byte[] raw;
            if (packet.rawData == null)
                raw = null;
            else
                raw = StringUtils.bytesToHex(packet.rawData).getBytes(StandardCharsets.UTF_8);


            byte[] recoveryHeartRates;
            if (packet.recoveryHeartRates == null)
                recoveryHeartRates = null;
            else
                recoveryHeartRates = StringUtils.bytesToHex(packet.recoveryHeartRates).getBytes(StandardCharsets.UTF_8);

            HuaweiWorkoutSummarySample summarySample = new HuaweiWorkoutSummarySample(
                    workoutId,
                    deviceId,
                    userId,
                    packet.number,
                    packet.status,
                    packet.startTime,
                    packet.endTime,
                    packet.calories,
                    packet.distance,
                    packet.stepCount,
                    packet.totalTime,
                    packet.duration,
                    packet.type,
                    packet.strokes,
                    packet.avgStrokeRate,
                    packet.poolLength,
                    packet.laps,
                    packet.avgSwolf,
                    raw,
                    null,
                    packet.maxAltitude,
                    packet.minAltitude,
                    packet.elevationGain,
                    packet.elevationLoss,
                    packet.workoutLoad,
                    packet.workoutAerobicEffect,
                    packet.workoutAnaerobicEffect,
                    packet.recoveryTime,
                    packet.minHeartRatePeak,
                    packet.maxHeartRatePeak,
                    recoveryHeartRates,
                    packet.swimType,
                    packet.maxMET,
                    packet.hrZoneType,
                    packet.runPaceZone1Min,
                    packet.runPaceZone2Min,
                    packet.runPaceZone3Min,
                    packet.runPaceZone4Min,
                    packet.runPaceZone5Min,
                    packet.runPaceZone5Max,
                    packet.runPaceZone1Time,
                    packet.runPaceZone2Time,
                    packet.runPaceZone3Time,
                    packet.runPaceZone4Time,
                    packet.runPaceZone5Time,
                    packet.algType,
                    packet.trainingPoints
            );


            db.getDaoSession().getHuaweiWorkoutSummarySampleDao().insertOrReplace(summarySample);

            return summarySample.getWorkoutId();
        } catch (Exception e) {
            LOG.error("Failed to add workout totals data to database", e);
            return null;
        }
    }

    public void addWorkoutSampleData(Long workoutId, List<Workout.WorkoutData.Response.Data> dataList) {
        if (workoutId == null)
            return;

        try (DBHandler db = GBApplication.acquireDB()) {
            HuaweiWorkoutDataSampleDao dao = db.getDaoSession().getHuaweiWorkoutDataSampleDao();

            for (Workout.WorkoutData.Response.Data data : dataList) {
                byte[] unknown;
                if (data.unknownData == null)
                    unknown = null;
                else
                    unknown = StringUtils.bytesToHex(data.unknownData).getBytes(StandardCharsets.UTF_8);

                HuaweiWorkoutDataSample dataSample = new HuaweiWorkoutDataSample(
                        workoutId,
                        data.timestamp,
                        data.heartRate,
                        data.speed,
                        data.stepRate,
                        data.cadence,
                        data.stepLength,
                        data.groundContactTime,
                        data.impact,
                        data.swingAngle,
                        data.foreFootLanding,
                        data.midFootLanding,
                        data.backFootLanding,
                        data.eversionAngle,
                        data.swolf,
                        data.strokeRate,
                        unknown,
                        data.calories,
                        data.cyclingPower,
                        data.frequency,
                        data.altitude
                );
                dao.insertOrReplace(dataSample);
            }
        } catch (Exception e) {
            LOG.error("Failed to add workout data to database", e);
        }
    }

    public void addWorkoutPaceData(Long workoutId, List<Workout.WorkoutPace.Response.Block> paceList, short number) {
        if (workoutId == null)
            return;

        try (DBHandler db = GBApplication.acquireDB()) {
            HuaweiWorkoutPaceSampleDao dao = db.getDaoSession().getHuaweiWorkoutPaceSampleDao();

            if (number == 0) {
                final DeleteQuery<HuaweiWorkoutPaceSample> tableDeleteQuery = dao.queryBuilder()
                        .where(HuaweiWorkoutPaceSampleDao.Properties.WorkoutId.eq(workoutId))
                        .buildDelete();
                tableDeleteQuery.executeDeleteWithoutDetachingEntities();
            }

            int paceIndex = (int) dao.queryBuilder().where(HuaweiWorkoutPaceSampleDao.Properties.WorkoutId.eq(workoutId)).count();
            for (Workout.WorkoutPace.Response.Block block : paceList) {

                Integer correction = block.hasCorrection ? (int) block.correction : null;
                HuaweiWorkoutPaceSample paceSample = new HuaweiWorkoutPaceSample(
                        workoutId,
                        paceIndex++,
                        block.distance,
                        block.type,
                        block.pace,
                        block.pointIndex,
                        correction
                );
                dao.insertOrReplace(paceSample);
            }
        } catch (Exception e) {
            LOG.error("Failed to add workout pace data to database", e);
        }
    }


    public void addWorkoutSwimSegmentsData(Long workoutId, List<Workout.WorkoutSwimSegments.Response.Block> paceList, short number) {
        if (workoutId == null)
            return;

        try (DBHandler db = GBApplication.acquireDB()) {
            HuaweiWorkoutSwimSegmentsSampleDao dao = db.getDaoSession().getHuaweiWorkoutSwimSegmentsSampleDao();

            if (number == 0) {
                final DeleteQuery<HuaweiWorkoutSwimSegmentsSample> tableDeleteQuery = dao.queryBuilder()
                        .where(HuaweiWorkoutSwimSegmentsSampleDao.Properties.WorkoutId.eq(workoutId))
                        .buildDelete();
                tableDeleteQuery.executeDeleteWithoutDetachingEntities();
            }

            int paceIndex = (int) dao.queryBuilder().where(HuaweiWorkoutSwimSegmentsSampleDao.Properties.WorkoutId.eq(workoutId)).count();
            for (Workout.WorkoutSwimSegments.Response.Block block : paceList) {
                HuaweiWorkoutSwimSegmentsSample swimSectionSample = new HuaweiWorkoutSwimSegmentsSample(
                        workoutId,
                        paceIndex++,
                        block.distance,
                        block.type,
                        block.pace,
                        block.pointIndex,
                        block.segment,
                        block.swimType,
                        block.strokes,
                        block.avgSwolf,
                        block.time
                );
                dao.insertOrReplace(swimSectionSample);
            }
        } catch (Exception e) {
            LOG.error("Failed to add workout swim section data to database", e);
        }
    }

    public void addDictData(List<HuaweiP2PDataDictionarySyncService.DictData> dictData) {
        try (DBHandler db = GBApplication.acquireDB()) {
            Long userId = DBHelper.getUser(db.getDaoSession()).getId();
            Long deviceId = DBHelper.getDevice(gbDevice, db.getDaoSession()).getId();

            for (HuaweiP2PDataDictionarySyncService.DictData data : dictData) {
                // Avoid duplicates
                QueryBuilder<HuaweiDictData> qb = db.getDaoSession().getHuaweiDictDataDao().queryBuilder().where(
                        HuaweiDictDataDao.Properties.UserId.eq(userId),
                        HuaweiDictDataDao.Properties.DeviceId.eq(deviceId),
                        HuaweiDictDataDao.Properties.DictClass.eq(data.getDictClass()),
                        HuaweiDictDataDao.Properties.StartTimestamp.eq(data.getStartTimestamp())
                );
                List<HuaweiDictData> results = qb.build().list();
                Long dictId = null;
                if (!results.isEmpty())
                    dictId = results.get(0).getDictId();

                HuaweiDictData dictSample = new HuaweiDictData(
                        dictId,
                        deviceId,
                        userId,
                        data.getDictClass(),
                        data.getStartTimestamp(),
                        data.getEndTimestamp(),
                        data.getModifyTimestamp()
                );
                db.getDaoSession().getHuaweiDictDataDao().insertOrReplace(dictSample);
                addDictDataValue(dictSample.getDictId(), data.getData());
            }
        } catch (Exception e) {
            LOG.error("Failed to add dict data", e);
        }
    }

    public void addDictDataValue(Long dictId, List<HuaweiP2PDataDictionarySyncService.DictData.DictDataValue> dictDataValues) {
        if (dictId == null)
            return;

        try (DBHandler db = GBApplication.acquireDB()) {
            HuaweiDictDataValuesDao dao = db.getDaoSession().getHuaweiDictDataValuesDao();

            for (HuaweiP2PDataDictionarySyncService.DictData.DictDataValue dataValues : dictDataValues) {

                HuaweiDictDataValues dictValue = new HuaweiDictDataValues(
                        dictId,
                        dataValues.getDataType(),
                        dataValues.getTag(),
                        dataValues.getValue()
                );
                dao.insertOrReplace(dictValue);
            }
        } catch (Exception e) {
            LOG.error("Failed to add dict value to database", e);
        }
    }

    public long getLastDataDictLastTimestamp(int dictClass) {
        long lastTimestamp = 0;
        if (dictClass == 0)
            return lastTimestamp;

        try (DBHandler db = GBApplication.acquireDB()) {
            Long userId = DBHelper.getUser(db.getDaoSession()).getId();
            Long deviceId = DBHelper.getDevice(gbDevice, db.getDaoSession()).getId();

            QueryBuilder<HuaweiDictData> qb = db.getDaoSession().getHuaweiDictDataDao().queryBuilder().where(
                    HuaweiDictDataDao.Properties.UserId.eq(userId),
                    HuaweiDictDataDao.Properties.DeviceId.eq(deviceId),
                    HuaweiDictDataDao.Properties.DictClass.eq(dictClass)
            );
            List<HuaweiDictData> results = qb.build().list();
            for (HuaweiDictData data : results) {
                if (data.getModifyTimestamp() != null) {
                    lastTimestamp = Math.max(lastTimestamp, data.getModifyTimestamp());
                }
                if (data.getEndTimestamp() != null) {
                    lastTimestamp = Math.max(lastTimestamp, data.getEndTimestamp());
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to select last timestamp value to database", e);
        }
        return lastTimestamp;
    }


    public void setWearLocation() {
        try {
            SetWearLocationRequest setWearLocationReq = new SetWearLocationRequest(this);
            setWearLocationReq.doPerform();
        } catch (IOException e) {
            GB.toast(context, "Failed to configure Wear Location", Toast.LENGTH_SHORT, GB.ERROR, e);
            LOG.error("Failed to configure Wear Location", e);
        }
    }

    public void getBatteryLevel() {
        try {
            stopBatteryRunnerDelayed();
            GetBatteryLevelRequest batteryLevelReq = new GetBatteryLevelRequest(this);
            batteryLevelReq.doPerform();
        } catch (IOException e) {
            GB.toast(context, "Failed to get battery Level", Toast.LENGTH_SHORT, GB.ERROR, e);
            LOG.error("Failed to get battery Level", e);
        }
    }

    public void sendUserInfo() {
        try {
            SendFitnessUserInfoRequest sendFitnessUserInfoRequest = new SendFitnessUserInfoRequest(this);
            sendFitnessUserInfoRequest.doPerform();
        } catch (IOException e) {
            GB.toast(context, "Failed to set user info", Toast.LENGTH_SHORT, GB.ERROR, e);
            LOG.error("Failed to set user info", e);
        }
    }

    public void setActivateOnLift() {
        try {
            SetActivateOnLiftRequest setActivateOnLiftReq = new SetActivateOnLiftRequest(this);
            setActivateOnLiftReq.doPerform();
            SharedPreferences sharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(deviceMac);
            boolean statusDndLiftWrist = sharedPrefs.getBoolean(DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_LIFT_WRIST, false);
            if (statusDndLiftWrist) {
                setDnd();
            }
        } catch (IOException e) {
            GB.toast(context, "Failed to configure Activate on Rotate", Toast.LENGTH_SHORT, GB.ERROR, e);
            LOG.error("Failed to configure Activate on Rotate", e);
        }
    }

    public void setNavigateOnRotate() {
        try {
            SetNavigateOnRotateRequest setNavigateOnRotateReq = new SetNavigateOnRotateRequest(this);
            setNavigateOnRotateReq.doPerform();
        } catch (IOException e) {
            GB.toast(context, "Failed to configure Navigate on Rotate", Toast.LENGTH_SHORT, GB.ERROR, e);
            LOG.error("Failed to configure Navigate on Rotate", e);
        }
    }

    public void setActivityReminder() {
        try {
            SetActivityReminderRequest setActivityReminderReq = new SetActivityReminderRequest(this);
            setActivityReminderReq.doPerform();
        } catch (IOException e) {
            GB.toast(context, "Failed to configure Activity reminder", Toast.LENGTH_SHORT, GB.ERROR, e);
            LOG.error("Failed to configure Activity reminder", e);
        }
    }

    public void setTrusleep() {
        try {
            SetTruSleepRequest setTruSleepReq = new SetTruSleepRequest(this);
            setTruSleepReq.doPerform();
        } catch (IOException e) {
            GB.toast(context, "Failed to configure truSleep", Toast.LENGTH_SHORT, GB.ERROR, e);
            LOG.error("Failed to configure truSleep", e);
        }
    }

    public void setTemperatureUnit() {
        try {
            SetTemperatureUnitSetting setTemperatureUnitSetting = new SetTemperatureUnitSetting(this);
            setTemperatureUnitSetting.doPerform();
        } catch (IOException e) {
            GB.toast(context, "Failed to set temperature unit", Toast.LENGTH_SHORT, GB.ERROR, e);
            LOG.error("Failed to configure TemperatureUnitSetting", e);
        }
    }

    public void setContinuousSkinTemperatureMeasurement() {
        try {
            SetSkinTemperatureMeasurement skinTemperatureMeasurement = new SetSkinTemperatureMeasurement(this);
            skinTemperatureMeasurement.doPerform();
        } catch (IOException e) {
            GB.toast(context, "Failed to configure continuous skin temperature measurement", Toast.LENGTH_SHORT, GB.ERROR, e);
            LOG.error("Failed to configure SkinTemperatureMeasurement", e);
        }
    }

    public void setDnd() {
        try {
            SendDndDeleteRequest sendDndDeleteReq = new SendDndDeleteRequest(this);
            SendDndAddRequest sendDndAddReq = new SendDndAddRequest(this);
            sendDndDeleteReq.nextRequest(sendDndAddReq);
            sendDndDeleteReq.doPerform();
        } catch (IOException e) {
            GB.toast(context, "Failed to set DND", Toast.LENGTH_SHORT, GB.ERROR, e);
            LOG.error("Failed to set DND", e);
        }
    }

    public void setDndNotWear() {
        try {
            SetWearMessagePushRequest setWearMessagePushReq = new SetWearMessagePushRequest(this);
            setWearMessagePushReq.doPerform();
        } catch (IOException e) {
            GB.toast(context, "Setting DND not wear failed", Toast.LENGTH_SHORT, GB.ERROR, e);
            LOG.error("Setting DND not wear failed", e);
        }

    }

    private void setDisconnectNotification() {
        try {
            SetDisconnectNotification req = new SetDisconnectNotification(this);
            req.doPerform();
        } catch (IOException e) {
            GB.toast(context, "Failed to set disconnect notification", Toast.LENGTH_SHORT, GB.ERROR, e);
            LOG.error("Failed to set disconnect notification", e);
        }
    }

    private void setHeartrateAutomatic() {
        try {
            SetAutomaticHeartrateRequest req = new SetAutomaticHeartrateRequest(this);
            req.doPerform();
        } catch (IOException e) {
            GB.toast(context, "Failed to set automatic heart rate", Toast.LENGTH_SHORT, GB.ERROR, e);
            LOG.error("Failed to set automatic heart rate", e);
        }
    }

    private void setSpoAutomatic() {
        try {
            SetAutomaticSpoRequest req = new SetAutomaticSpoRequest(this);
            req.doPerform();
        } catch (IOException e) {
            GB.toast(context, "Failed to set automatic SpO", Toast.LENGTH_SHORT, GB.ERROR, e);
            LOG.error("Failed to set automatic SpO", e);
        }
    }

    public void sendDebugRequest() {
        try {
            LOG.debug("Send debug request");
            DebugRequest req = new DebugRequest(this);
            req.doPerform();
        } catch (IOException e) {
            GB.toast(context, "Failed to send debug request", Toast.LENGTH_SHORT, GB.ERROR, e);
            LOG.error("Failed to send debug request", e);
        }
    }

    public void onStopFindPhone() {
        try {
            LOG.debug("Send stop find phone request");
            StopFindPhoneRequest stopFindPhoneRequest = new StopFindPhoneRequest(this);
            stopFindPhoneRequest.doPerform();
        } catch (IOException e) {
            GB.toast(context, "Failed to send stop find phone request", Toast.LENGTH_SHORT, GB.ERROR, e);
            LOG.error("Failed to send stop find phone request", e);
        }
    }

    public void setLanguageSetting() {
        try {
            SetLanguageSettingRequest setLocaleReq = new SetLanguageSettingRequest(this);
            setLocaleReq.doPerform();
        } catch (IOException e) {
            GB.toast(context, "Failed to set language settings request", Toast.LENGTH_SHORT, GB.ERROR, e);
            LOG.error("Failed to set language settings request", e);
        }
    }

    public Weather.WeatherIcon openWeatherMapConditionCodeToHuaweiIcon(int conditionCode) {
        return huaweiWeatherManager.openWeatherMapConditionCodeToHuaweiIcon(conditionCode);
    }

    public void onSendWeather(ArrayList<WeatherSpec> weatherSpecs) {
        huaweiWeatherManager.sendWeather(weatherSpecs.get(0));
    }

    public void onSetGpsLocation(Location location) {
        if (gpsParametersResponse == null) {
            GB.toast(context, "Received location without knowing supported parameters", Toast.LENGTH_SHORT, GB.ERROR);
            LOG.error("Received location without knowing supported parameters");
            return;
        }

        if (!gpsEnabled) {
            LOG.warn("Received GPS data without GPS being enabled! Attempting to stop again.");
            GBLocationService.stop(getContext(), getDevice());
            return;
        }

        SendGpsDataRequest sendGpsDataRequest = new SendGpsDataRequest(this, location, gpsLastLocation, gpsParametersResponse);
        try {
            sendGpsDataRequest.doPerform();
        } catch (IOException e) {
            GB.toast(context, "Failed to send GPS data", Toast.LENGTH_SHORT, GB.ERROR, e);
            LOG.error("Failed to send GPS data", e);
        }
        gpsLastLocation = location;
    }

    public void onInstallApp(Uri uri) {
        LOG.info("enter onAppInstall uri: {}", uri);
        HuaweiFwHelper huaweiFwHelper = new HuaweiFwHelper(uri, getContext());

        HuaweiUploadManager.FileUploadInfo fileInfo = new HuaweiUploadManager.FileUploadInfo();

        if (huaweiFwHelper.isMusic()) {
            getHuaweiMusicManager().addUploadMusic(huaweiFwHelper.getMusicInfo());
        }

        fileInfo.setFileType(huaweiFwHelper.getFileType());
        if (huaweiFwHelper.isWatchface()) {
            fileInfo.setFileName(huaweiWatchfaceManager.getRandomName());
        } else {
            fileInfo.setFileName(huaweiFwHelper.getFileName());
        }
        fileInfo.setBytes(huaweiFwHelper.getBytes());

        fileInfo.setFileUploadCallback(new HuaweiUploadManager.FileUploadCallback() {
            @Override
            public void onUploadStart() {
                HuaweiSupportProvider.this.huaweiUploadManager.setDeviceBusy();
            }

            @Override
            public void onUploadProgress(int progress) {
                HuaweiSupportProvider.this.onUploadProgress(R.string.updatefirmwareoperation_update_in_progress, progress, true);
            }

            @Override
            public void onUploadComplete() {
                HuaweiSupportProvider.this.huaweiUploadManager.unsetDeviceBusy();
                HuaweiSupportProvider.this.onUploadProgress(R.string.updatefirmwareoperation_update_complete, 100, false);
            }

            @Override
            public void onError(int code) {
                if (code == 140004) {
                    LOG.error("Too many watchfaces installed or musics uploaded");
                    HuaweiSupportProvider.this.handleGBDeviceEvent(new GBDeviceEventDisplayMessage(HuaweiSupportProvider.this.getContext().getString(R.string.cannot_upload_watchface_too_many_watchfaces_installed), Toast.LENGTH_LONG, GB.ERROR));
                } else if (code == 140008) {
                    LOG.error("File already exists");
                    HuaweiSupportProvider.this.handleGBDeviceEvent(new GBDeviceEventDisplayMessage(HuaweiSupportProvider.this.getContext().getString(R.string.file_already_exists), Toast.LENGTH_LONG, GB.ERROR));
                } else if (code == 140009) {
                    LOG.error("Insufficient space for upload");
                    HuaweiSupportProvider.this.handleGBDeviceEvent(new GBDeviceEventDisplayMessage(HuaweiSupportProvider.this.getContext().getString(R.string.insufficient_space_for_upload), Toast.LENGTH_LONG, GB.ERROR));
                }
            }
        });

        huaweiUploadManager.setFileUploadInfo(fileInfo);

        try {
            SendFileUploadInfo sendFileUploadInfo = new SendFileUploadInfo(this, huaweiUploadManager);
            sendFileUploadInfo.doPerform();
        } catch (IOException e) {
            GB.toast(context, "Failed to send file upload info", Toast.LENGTH_SHORT, GB.ERROR, e);
            LOG.error("Failed to send file upload info", e);
        }
    }

    public void onUploadProgress(int textRsrc, int progressPercent, boolean ongoing) {
        try {
            if (isBLE()) {
                nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder leBuilder = createLeTransactionBuilder("FetchRecordedData");
                leBuilder.add(new nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetProgressAction(
                        context.getString(textRsrc),
                        ongoing,
                        progressPercent,
                        context
                ));
                leBuilder.queue(leSupport.getQueue());
            } else {
                nodomain.freeyourgadget.gadgetbridge.service.btbr.TransactionBuilder brBuilder = createBrTransactionBuilder("FetchRecordedData");
                brBuilder.add(new nodomain.freeyourgadget.gadgetbridge.service.btbr.actions.SetProgressAction(
                        context.getString(textRsrc),
                        ongoing,
                        progressPercent,
                        context));
                brBuilder.queue(brSupport.getQueue());

            }

        } catch (final Exception e) {
            LOG.error("Failed to update progress notification", e);
        }
    }

    private List<GBDeviceApp> gbWatchFaces = null;
    private List<GBDeviceApp> gbWatchApps = null;

    public void setGbWatchFaces(List<GBDeviceApp> gbWatchFaces) {
        this.gbWatchFaces = gbWatchFaces;
        updateAppList();
    }

    public void setGbWatchApps(List<GBDeviceApp> gbWatchApps) {
        this.gbWatchApps = gbWatchApps;
        updateAppList();
    }

    private void updateAppList() {
        ArrayList<GBDeviceApp> gbDeviceApps = new ArrayList<>();
        if (this.gbWatchFaces != null)
            gbDeviceApps.addAll(this.gbWatchFaces);
        if (this.gbWatchApps != null)
            gbDeviceApps.addAll(this.gbWatchApps);
        final GBDeviceEventAppInfo appInfoCmd = new GBDeviceEventAppInfo();
        appInfoCmd.apps = gbDeviceApps.toArray(new GBDeviceApp[0]);
        evaluateGBDeviceEvent(appInfoCmd);
    }

    public void onAppInfoReq() {
        this.gbWatchFaces = null;
        this.gbWatchApps = null;
        huaweiWatchfaceManager.requestWatchfaceList();
        huaweiAppManager.requestAppList();
    }

    public void onAppStart(final UUID uuid, boolean start) {
        if (start) {
            //NOTE: to prevent exception in watchfaces code
            if (!huaweiAppManager.startApp(uuid)) {
                huaweiWatchfaceManager.setWatchface(uuid);
            }
        }
    }

    public void onAppDelete(final UUID uuid) {
        //NOTE: to prevent exception in watchfaces code
        if (!huaweiAppManager.deleteApp(uuid)) {
            huaweiWatchfaceManager.deleteWatchface(uuid);
        }
    }

    public void onCameraStatusChange(GBDeviceEventCameraRemote.Event event, String filename) {
        if (event == GBDeviceEventCameraRemote.Event.OPEN_CAMERA) {
            // Somehow a delay is necessary for the watch
            new Handler(GBApplication.getContext().getMainLooper()).postDelayed(
                    () -> {
                        SendCameraRemoteSetupEvent sendCameraRemoteSetupEvent = new SendCameraRemoteSetupEvent(HuaweiSupportProvider.this, CameraRemote.CameraRemoteSetup.Request.Event.CAMERA_STARTED);
                        try {
                            sendCameraRemoteSetupEvent.doPerform();
                        } catch (IOException e) {
                            GB.toast("Failed to send open camera request", Toast.LENGTH_SHORT, GB.ERROR, e);
                            LOG.error("Failed to send open camera request", e);
                        }
                    },
                    3000
            );
        } else if (event == GBDeviceEventCameraRemote.Event.CLOSE_CAMERA) {
            SendCameraRemoteSetupEvent sendCameraRemoteSetupEvent2 = new SendCameraRemoteSetupEvent(this, CameraRemote.CameraRemoteSetup.Request.Event.CAMERA_STOPPED);
            try {
                sendCameraRemoteSetupEvent2.doPerform();
            } catch (IOException e) {
                GB.toast("Failed to send open camera request", Toast.LENGTH_SHORT, GB.ERROR, e);
                LOG.error("Failed to send open camera request", e);
            }
        }
    }

    public void onSetContacts(ArrayList<? extends Contact> contacts) {
        SendSetContactsRequest sendSetContactsRequest = new SendSetContactsRequest(
                this,
                contacts,
                this.getHuaweiCoordinator().getContactsSlotCount(getDevice())
        );
        try {
            sendSetContactsRequest.doPerform();
        } catch (IOException e) {
            GB.toast(context, "Failed to set contacts", Toast.LENGTH_SHORT, GB.ERROR, e);
            LOG.error("Failed to send set contacts request", e);
        }

    }

    public void onAddCalendarEvent(final CalendarEventSpec calendarEventSpec) {
        HuaweiP2PCalendarService service = HuaweiP2PCalendarService.getRegisteredInstance(huaweiP2PManager);
        if (service != null) {
            service.onAddCalendarEvent(calendarEventSpec);
        }
    }

    public void onDeleteCalendarEvent(final byte type, long id) {
        HuaweiP2PCalendarService service = HuaweiP2PCalendarService.getRegisteredInstance(huaweiP2PManager);
        if (service != null) {
            service.onDeleteCalendarEvent(type, id);
        }
    }

    public boolean startBatteryRunnerDelayed() {
        int interval_minutes = GBApplication.getDevicePrefs(gbDevice).getBatteryPollingIntervalMinutes();
        int interval = interval_minutes * 60 * 1000;
        LOG.debug("Starting battery runner delayed by {} ({} minutes)", interval, interval_minutes);
        handler.removeCallbacks(batteryRunner);
        return handler.postDelayed(batteryRunner, interval);
    }

    public void stopBatteryRunnerDelayed() {
        LOG.debug("Stopping battery runner delayed");
        handler.removeCallbacks(batteryRunner);
    }

    public void dispose() {
        stopBatteryRunnerDelayed();
        huaweiFileDownloadManager.dispose();
        huaweiP2PManager.unregisterAllService();
    }

    public boolean downloadTruSleepData(int start, int end) {
        // We only get the data if TruSleep is supported
        if (!getHuaweiCoordinator().supportsTruSleep())
            return false;

        huaweiFileDownloadManager.addToQueue(HuaweiFileDownloadManager.FileRequest.sleepStateFileRequest(
                getHuaweiCoordinator().getSupportsTruSleepNewSync(),
                start,
                end,
                new HuaweiFileDownloadManager.FileDownloadCallback() {
                    @Override
                    public void downloadComplete(HuaweiFileDownloadManager.FileRequest fileRequest) {
                        if (fileRequest.getData().length != 0) {
                            LOG.debug("Parsing sleep state file");
                            HuaweiTruSleepParser.TruSleepStatus[] results = HuaweiTruSleepParser.parseState(fileRequest.getData());
                            for (HuaweiTruSleepParser.TruSleepStatus status : results)
                                addSleepActivity(status.startTime, status.endTime, (byte) 0x06, (byte) 0x0a);
                        } else
                            LOG.debug("Sleep state file empty");
                        syncState.setActivitySync(false);
                    }

                    @Override
                    public void downloadException(HuaweiFileDownloadManager.HuaweiFileDownloadException e) {
                        super.downloadException(e);
                        syncState.setActivitySync(false);
                    }
                }
        ), true);
        return true;
    }

    public void endOfWorkoutSync() {
        this.syncState.setWorkoutSync(false);
    }

    public void downloadWorkoutGpsFiles(short workoutId, Long databaseId, Runnable extraCallbackAction) {
        syncState.startWorkoutGpsDownload();

        huaweiFileDownloadManager.addToQueue(HuaweiFileDownloadManager.FileRequest.workoutGpsFileRequest(
                getHuaweiCoordinator().isSupportsGpsNewSync(),
                workoutId,
                databaseId,
                new HuaweiFileDownloadManager.FileDownloadCallback() {
                    @Override
                    public void downloadComplete(HuaweiFileDownloadManager.FileRequest fileRequest) {
                        extraCallbackAction.run();

                        if (fileRequest.getData().length == 0) {
                            LOG.debug("GPS file empty");
                            syncState.stopWorkoutGpsDownload();
                            return;
                        }

                        LOG.debug("Parsing GPS file");

                        HuaweiGpsParser.GpsPoint[] points = HuaweiGpsParser.parseHuaweiGps(fileRequest.getData());

                        LOG.debug("Received {} GPS points", points.length);

                        if (points.length == 0) {
                            LOG.debug("No GPS points returned");
                            syncState.stopWorkoutGpsDownload();
                            return;
                        }

                        ActivityTrack track = new ActivityTrack();

                        track.setName("Workout " + fileRequest.getWorkoutId());
                        track.setBaseTime(DateTimeUtils.parseTimeStamp(points[0].timestamp));

                        try (DBHandler db = GBApplication.acquireDB()) {
                            track.setUser(DBHelper.getUser(db.getDaoSession()));
                            track.setDevice(DBHelper.getDevice(gbDevice, db.getDaoSession()));
                        } catch (Exception e) {
                            LOG.error("Cannot acquire DB, set user, or set device for Activity track, continuing anyway");
                        }

                        for (HuaweiGpsParser.GpsPoint point : points) {
                            GPSCoordinate coordinate;
                            if (point.altitudeSupported)
                                coordinate = new GPSCoordinate(point.longitude, point.latitude, point.altitude);
                            else
                                coordinate = new GPSCoordinate(point.longitude, point.latitude);

                            ActivityPoint activityPoint = new ActivityPoint();
                            activityPoint.setTime(DateTimeUtils.parseTimeStamp(point.timestamp));
                            activityPoint.setLocation(coordinate);

                            track.addTrackPoint(activityPoint);
                        }

                        String filename = FileUtils.makeValidFileName("workout_" + fileRequest.getWorkoutId() + "_" + points[0].timestamp + ".gpx");
                        File targetFile;
                        try {
                            targetFile = new File(
                                    getDevice().getDeviceCoordinator().getWritableExportDirectory(getDevice()),
                                    filename
                            );
                        } catch (IOException e) {
                            GB.toast(context, "Could not open Workout GPS file to write to", Toast.LENGTH_SHORT, GB.ERROR, e);
                            LOG.error("Could not open Workout GPS file to write to", e);
                            syncState.stopWorkoutGpsDownload();
                            return;
                        }

                        GPXExporter exporter = new GPXExporter();
                        exporter.setCreator(GBApplication.app().getNameAndVersion());
                        try {
                            exporter.performExport(track, targetFile);
                        } catch (IOException | ActivityTrackExporter.GPXTrackEmptyException e) {
                            GB.toast(context, "Failed to export Workout GPX file", Toast.LENGTH_SHORT, GB.ERROR, e);
                            LOG.error("Failed to export Workout GPX file", e);
                            syncState.stopWorkoutGpsDownload();
                            return;
                        }

                        Long databaseId = fileRequest.getDatabaseId();
                        if (databaseId == null) {
                            GB.toast(context, "Cannot link GPX to workout", Toast.LENGTH_SHORT, GB.ERROR);
                            LOG.error("Cannot link GPX to workout");
                            syncState.stopWorkoutGpsDownload();
                            return;
                        }

                        try (DBHandler db = GBApplication.acquireDB()) {
                            DaoSession daoSession = db.getDaoSession();
                            HuaweiWorkoutSummarySample sample = daoSession.getHuaweiWorkoutSummarySampleDao().load(databaseId);
                            sample.setGpxFileLocation(targetFile.getAbsolutePath());
                            sample.update();
                        } catch (Exception e) {
                            GB.toast(context, "Failed to save Workout GPX file location", Toast.LENGTH_SHORT, GB.ERROR, e);
                            LOG.error("Failed to save Workout GPX file location", e);
                            syncState.stopWorkoutGpsDownload();
                            return;
                        }

                        new HuaweiWorkoutGbParser(getDevice(), getContext()).parseWorkout(databaseId);

                        LOG.debug("Completed workout GPS parsing and inserting");
                        syncState.stopWorkoutGpsDownload();
                    }

                    @Override
                    public void downloadException(HuaweiFileDownloadManager.HuaweiFileDownloadException e) {
                        super.downloadException(e);
                        syncState.stopWorkoutGpsDownload();

                        extraCallbackAction.run();
                    }
                }
        ), true);
    }

    /**
     * Called when there are no more files left to download
     */
    public void downloadQueueEmpty(boolean needSync) {
        syncState.updateState(needSync);
    }

    public void onTestNewFunction() {
        // Show to user
        gbDevice.setBusyTask("Downloading files...");
        gbDevice.sendDeviceUpdateIntent(getContext());

        HuaweiTruSleepParser.SleepFileDownloadCallback callback = new HuaweiTruSleepParser.SleepFileDownloadCallback(this) {
            @Override
            public void syncComplete(byte[] statusData, byte[] sleepData) {
                LOG.debug("Sync of TruSleep status and data finished");

                if (statusData == null || statusData.length == 0)
                    return;

                HuaweiTruSleepParser.TruSleepStatus[] results = HuaweiTruSleepParser.parseState(statusData);
                if (results.length == 0)
                    return;

                HuaweiTruSleepParser.TruSleepData data = HuaweiTruSleepParser.parseData(sleepData);
                HuaweiTruSleepParser.analyze(this.provider, results, data);
            }
        };

        huaweiFileDownloadManager.addToQueue(HuaweiFileDownloadManager.FileRequest.sleepStateFileRequest(
                getHuaweiCoordinator().getSupportsTruSleepNewSync(),
                0,
                (int) (System.currentTimeMillis() / 1000),
                callback
        ), true);
        huaweiFileDownloadManager.addToQueue(HuaweiFileDownloadManager.FileRequest.sleepDataFileRequest(
                getHuaweiCoordinator().getSupportsTruSleepNewSync(),
                0,
                (int) (System.currentTimeMillis() / 1000),
                callback
        ), true);
    }

    public void onMusicListReq() {
        getHuaweiMusicManager().startSyncMusicData();
    }

    public void onMusicOperation(int operation, int playlistIndex, String playlistName, ArrayList<Integer> musicIds) {
        getHuaweiMusicManager().onMusicOperation(operation, playlistIndex, playlistName,  musicIds);
    }
}
