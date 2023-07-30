/*  Copyright (C) 2020-2023 Petr Kadlec

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Build;
import android.widget.Toast;
import com.google.protobuf.InvalidProtocolBufferException;
import de.greenrobot.dao.query.Query;
import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventFindPhone;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr.GarminCapability;
import nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr.VivomoveConstants;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminFitFile;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminFitFileDao;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.proto.vivomovehr.GdiCore;
import nodomain.freeyourgadget.gadgetbridge.proto.vivomovehr.GdiDeviceStatus;
import nodomain.freeyourgadget.gadgetbridge.proto.vivomovehr.GdiFindMyWatch;
import nodomain.freeyourgadget.gadgetbridge.proto.vivomovehr.GdiSmartProto;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ams.AmsEntity;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ams.AmsEntityAttribute;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ancs.AncsAttribute;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ancs.AncsAttributeRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ancs.AncsEvent;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ancs.AncsGetNotificationAttributeCommand;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ancs.AncsGetNotificationAttributesResponse;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ancs.GncsDataSourceQueue;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.downloads.DirectoryData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.downloads.DirectoryEntry;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.downloads.FileDownloadListener;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.downloads.FileDownloadQueue;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.fit.FitBool;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.fit.FitDbImporter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.fit.FitMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.fit.FitMessageDefinitions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.fit.FitParser;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.fit.FitWeatherConditions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.AuthNegotiationMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.AuthNegotiationResponseMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.BatteryStatusMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.ConfigurationMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.CreateFileResponseMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.CurrentTimeRequestMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.CurrentTimeRequestResponseMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.DeviceInformationMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.DeviceInformationResponseMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.DirectoryFileFilterRequestMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.DirectoryFileFilterResponseMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.DownloadRequestResponseMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.FileTransferDataMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.FileTransferDataResponseMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.FindMyPhoneRequestMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.FitDataMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.FitDataResponseMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.FitDefinitionMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.FitDefinitionResponseMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.GenericResponseMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.GncsControlPointMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.GncsControlPointResponseMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.GncsDataSourceResponseMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.GncsNotificationSourceMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.MusicControlCapabilitiesMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.MusicControlCapabilitiesResponseMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.MusicControlEntityUpdateMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.NotificationServiceSubscriptionMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.NotificationServiceSubscriptionResponseMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.ProtobufRequestMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.ProtobufRequestResponseMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.ResponseMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.SetDeviceSettingsMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.SetDeviceSettingsResponseMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.SupportedFileTypesRequestMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.SupportedFileTypesResponseMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.SyncRequestMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.SystemEventMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.SystemEventResponseMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.UploadRequestResponseMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.WeatherRequestMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.WeatherRequestResponseMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.notifications.NotificationData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.notifications.NotificationStorage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.uploads.FileUploadQueue;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VivomoveHrSupport extends AbstractBTLEDeviceSupport implements FileDownloadListener {
    private static final Logger LOG = LoggerFactory.getLogger(VivomoveHrSupport.class);

    // Should all FIT data files be fully stored into the database? (Currently, there is no user-friendly way to
    // retrieve them, anyway.)
    // TODO: Choose what to do with them. Either store them or remove the field from DB.
    private static final boolean STORE_FIT_FILES = false;

    private final GfdiPacketParser gfdiPacketParser = new GfdiPacketParser();
    private Set<GarminCapability> capabilities;

    private int lastProtobufRequestId;
    private int maxPacketSize;

    private final FitParser fitParser = new FitParser(FitMessageDefinitions.ALL_DEFINITIONS);
    private final NotificationStorage notificationStorage = new NotificationStorage();
    private VivomoveHrCommunicator communicator;
    private RealTimeActivityHandler realTimeActivityHandler;
    private GncsDataSourceQueue gncsDataSourceQueue;
    private FileDownloadQueue fileDownloadQueue;
    private FileUploadQueue fileUploadQueue;
    private FitDbImporter fitImporter;
    private boolean notificationSubscription;

    public VivomoveHrSupport() {
        super(LOG);

        addSupportedService(VivomoveConstants.UUID_SERVICE_GARMIN_GFDI);
        addSupportedService(VivomoveConstants.UUID_SERVICE_GARMIN_REALTIME);
    }

    private int getNextProtobufRequestId() {
        lastProtobufRequestId = (lastProtobufRequestId + 1) % 65536;
        return lastProtobufRequestId;
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        LOG.info("Initializing");

        gbDevice.setState(GBDevice.State.INITIALIZING);
        gbDevice.sendDeviceUpdateIntent(getContext());

        communicator = new VivomoveHrCommunicator(this);
        realTimeActivityHandler = new RealTimeActivityHandler(this);

        builder.setCallback(this);
        communicator.start(builder);
        fileDownloadQueue = new FileDownloadQueue(communicator, this);
        fileUploadQueue = new FileUploadQueue(communicator);

        LOG.info("Initialization Done");

        // OK, this is not perfect: we should not be INITIALIZED until “connected AND all the necessary initialization
        // steps have been performed. At the very least, this means that basic information like device name, firmware
        // version, hardware revision (as applicable) is available in the GBDevice”. But we cannot send any message
        // until we are INITIALIZED. So what can we do…
        gbDevice.setState(GBDevice.State.INITIALIZED);
        gbDevice.sendDeviceUpdateIntent(getContext());

        sendMessage(new AuthNegotiationMessage(AuthNegotiationMessage.LONG_TERM_KEY_AVAILABILITY_NONE, AuthNegotiationMessage.ENCRYPTION_ALGORITHM_NONE).packet);

        return builder;
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        final UUID characteristicUUID = characteristic.getUuid();
        if (super.onCharacteristicChanged(gatt, characteristic)) {
            LOG.debug("Change of characteristic {} handled by parent", characteristicUUID);
            return true;
        }

        final byte[] data = characteristic.getValue();
        if (data.length == 0) {
            LOG.debug("No data received on change of characteristic {}", characteristicUUID);
            return true;
        }

        if (VivomoveConstants.UUID_CHARACTERISTIC_GARMIN_GFDI_RECEIVE.equals(characteristicUUID)) {
            handleReceivedGfdiBytes(data);
        } else if (realTimeActivityHandler.tryHandleChangedCharacteristic(characteristicUUID, data)) {
            // handled by real-time activity handler
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Unknown characteristic {} changed: {}", characteristicUUID, GB.hexdump(data));
            }
        }

        return true;
    }

    private void sendMessage(byte[] messageBytes) {
        communicator.sendMessage(messageBytes);
    }

    private void handleReceivedGfdiBytes(byte[] data) {
        gfdiPacketParser.receivedBytes(data);
        LOG.debug("Received {} GFDI bytes", data.length);
        byte[] packet;
        while ((packet = gfdiPacketParser.retrievePacket()) != null) {
            LOG.debug("Processing a {}B GFDI packet", packet.length);
            processGfdiPacket(packet);
        }
    }

    private void processGfdiPacket(byte[] packet) {
        final int size = BLETypeConversions.toUint16(packet, 0);
        if (size != packet.length) {
            LOG.error("Received GFDI packet with invalid length: {} vs {}", size, packet.length);
            return;
        }
        final int crc = BLETypeConversions.toUint16(packet, packet.length - 2);
        final int correctCrc = ChecksumCalculator.computeCrc(packet, 0, packet.length - 2);
        if (crc != correctCrc) {
            LOG.error("Received GFDI packet with invalid CRC: {} vs {}", crc, correctCrc);
            return;
        }

        final int messageType = BLETypeConversions.toUint16(packet, 2);
        switch (messageType) {
            case VivomoveConstants.MESSAGE_RESPONSE:
                processResponseMessage(ResponseMessage.parsePacket(packet), packet);
                break;

            case VivomoveConstants.MESSAGE_FILE_TRANSFER_DATA:
                fileDownloadQueue.onFileTransferData(FileTransferDataMessage.parsePacket(packet));
                break;

            case VivomoveConstants.MESSAGE_DEVICE_INFORMATION:
                processDeviceInformationMessage(DeviceInformationMessage.parsePacket(packet));
                break;

            case VivomoveConstants.MESSAGE_WEATHER_REQUEST:
                processWeatherRequest(WeatherRequestMessage.parsePacket(packet));
                break;

            case VivomoveConstants.MESSAGE_MUSIC_CONTROL_CAPABILITIES:
                processMusicControlCapabilities(MusicControlCapabilitiesMessage.parsePacket(packet));
                break;

            case VivomoveConstants.MESSAGE_CURRENT_TIME_REQUEST:
                processCurrentTimeRequest(CurrentTimeRequestMessage.parsePacket(packet));
                break;

            case VivomoveConstants.MESSAGE_SYNC_REQUEST:
                processSyncRequest(SyncRequestMessage.parsePacket(packet));
                break;

            case VivomoveConstants.MESSAGE_FIND_MY_PHONE:
                processFindMyPhoneRequest(FindMyPhoneRequestMessage.parsePacket(packet));
                break;

            case VivomoveConstants.MESSAGE_CANCEL_FIND_MY_PHONE:
                processCancelFindMyPhoneRequest();
                break;

            case VivomoveConstants.MESSAGE_NOTIFICATION_SERVICE_SUBSCRIPTION:
                processNotificationServiceSubscription(NotificationServiceSubscriptionMessage.parsePacket(packet));
                break;

            case VivomoveConstants.MESSAGE_GNCS_CONTROL_POINT_REQUEST:
                processGncsControlPointRequest(GncsControlPointMessage.parsePacket(packet));
                break;

            case VivomoveConstants.MESSAGE_CONFIGURATION:
                processConfigurationMessage(ConfigurationMessage.parsePacket(packet));
                break;

            case VivomoveConstants.MESSAGE_PROTOBUF_RESPONSE:
                processProtobufResponse(ProtobufRequestMessage.parsePacket(packet));
                break;

            default:
                if (LOG.isInfoEnabled()) {
                    LOG.info("Unknown message type {}: {}", messageType, GB.hexdump(packet, 0, packet.length));
                }
                break;
        }
    }

    private void processCancelFindMyPhoneRequest() {
        LOG.info("Processing request to cancel find-my-phone");
        sendMessage(new GenericResponseMessage(VivomoveConstants.MESSAGE_FIND_MY_PHONE, 0).packet);

        final GBDeviceEventFindPhone findPhoneEvent = new GBDeviceEventFindPhone();
        findPhoneEvent.event = GBDeviceEventFindPhone.Event.STOP;
        evaluateGBDeviceEvent(findPhoneEvent);
    }

    private void processFindMyPhoneRequest(FindMyPhoneRequestMessage requestMessage) {
        LOG.info("Processing find-my-phone request ({} s)", requestMessage.duration);

        sendMessage(new GenericResponseMessage(VivomoveConstants.MESSAGE_FIND_MY_PHONE, 0).packet);

        final GBDeviceEventFindPhone findPhoneEvent = new GBDeviceEventFindPhone();
        findPhoneEvent.event = GBDeviceEventFindPhone.Event.START;
        evaluateGBDeviceEvent(findPhoneEvent);
    }

    private void processGncsControlPointRequest(GncsControlPointMessage requestMessage) {
        if (requestMessage == null) {
            // TODO: Proper error handling with specific error code
            sendMessage(new GncsControlPointResponseMessage(VivomoveConstants.STATUS_ACK, GncsControlPointResponseMessage.RESPONSE_ANCS_ERROR_OCCURRED, GncsControlPointResponseMessage.ANCS_ERROR_UNKNOWN_ANCS_COMMAND).packet);
            return;
        }
        switch (requestMessage.command.command) {
            case GET_NOTIFICATION_ATTRIBUTES:
                final AncsGetNotificationAttributeCommand getNotificationAttributeCommand = (AncsGetNotificationAttributeCommand) requestMessage.command;
                LOG.info("Processing ANCS request to get attributes of notification #{}", getNotificationAttributeCommand.notificationUID);
                sendMessage(new GncsControlPointResponseMessage(VivomoveConstants.STATUS_ACK, GncsControlPointResponseMessage.RESPONSE_SUCCESSFUL, GncsControlPointResponseMessage.ANCS_ERROR_NO_ERROR).packet);
                final NotificationData notificationData = notificationStorage.retrieveNotification(getNotificationAttributeCommand.notificationUID);
                if (notificationData == null) {
                    LOG.warn("Notification #{} not registered", getNotificationAttributeCommand.notificationUID);
                }
                final Map<AncsAttribute, String> attributes = new LinkedHashMap<>();
                for (final AncsAttributeRequest attributeRequest : getNotificationAttributeCommand.attributes) {
                    final AncsAttribute attribute = attributeRequest.attribute;
                    final String attributeValue = notificationData == null ? null : notificationData.getAttribute(attributeRequest.attribute);
                    final String valueShortened = attributeRequest.maxLength > 0 && attributeValue != null && attributeValue.length() > attributeRequest.maxLength ? attributeValue.substring(0, attributeRequest.maxLength) : attributeValue;
                    LOG.debug("Requested ANCS attribute {}: '{}'", attribute, valueShortened);
                    attributes.put(attribute, valueShortened == null ? "" : valueShortened);
                }
                gncsDataSourceQueue.addToQueue(new AncsGetNotificationAttributesResponse(getNotificationAttributeCommand.notificationUID, attributes).packet);
                break;

            default:
                LOG.error("Unknown GNCS control point command {}", requestMessage.command.command);
                sendMessage(new GncsControlPointResponseMessage(VivomoveConstants.STATUS_ACK, GncsControlPointResponseMessage.RESPONSE_ANCS_ERROR_OCCURRED, GncsControlPointResponseMessage.ANCS_ERROR_UNKNOWN_ANCS_COMMAND).packet);
                break;
        }
    }

    private void processNotificationServiceSubscription(NotificationServiceSubscriptionMessage requestMessage) {
        LOG.info("Processing notification service subscription request message: intent={}, flags={}", requestMessage.intentIndicator, requestMessage.featureFlags);
        notificationSubscription = requestMessage.intentIndicator == NotificationServiceSubscriptionMessage.INTENT_SUBSCRIBE;
        sendMessage(new NotificationServiceSubscriptionResponseMessage(0, 0, requestMessage.intentIndicator, requestMessage.featureFlags).packet);
    }

    private void processSyncRequest(SyncRequestMessage requestMessage) {
        if (LOG.isInfoEnabled()) {
            final StringBuilder requestedTypes = new StringBuilder();
            for (GarminMessageType type : requestMessage.fileTypes) {
                if (requestedTypes.length() > 0) {
                    requestedTypes.append(", ");
                }
                requestedTypes.append(type);
            }
            LOG.info("Processing sync request message: option={}, types: {}", requestMessage.option, requestedTypes);
        }
        sendMessage(new GenericResponseMessage(VivomoveConstants.MESSAGE_SYNC_REQUEST, 0).packet);
        if (requestMessage.option != SyncRequestMessage.OPTION_INVISIBLE) {
            doSync();
        }
    }

    private void processProtobufResponse(ProtobufRequestMessage requestMessage) {
        LOG.info("Received protobuf response #{}, {}B@{}/{}: {}", requestMessage.requestId, requestMessage.protobufDataLength, requestMessage.dataOffset, requestMessage.totalProtobufLength, GB.hexdump(requestMessage.messageBytes, 0, requestMessage.messageBytes.length));
        sendMessage(new GenericResponseMessage(VivomoveConstants.MESSAGE_PROTOBUF_RESPONSE, 0).packet);
        final GdiSmartProto.Smart smart;
        try {
            smart = GdiSmartProto.Smart.parseFrom(requestMessage.messageBytes);
        } catch (InvalidProtocolBufferException e) {
            LOG.error("Failed to parse protobuf message ({}): {}", e.getLocalizedMessage(), GB.hexdump(requestMessage.messageBytes, 0, requestMessage.messageBytes.length));
            return;
        }
        boolean processed = false;
        if (smart.hasDeviceStatusService()) {
            processProtobufDeviceStatusResponse(smart.getDeviceStatusService());
            processed = true;
        }
        if (smart.hasFindMyWatchService()) {
            processProtobufFindMyWatchResponse(smart.getFindMyWatchService());
            processed = true;
        }
        if (smart.hasCoreService()) {
            processProtobufCoreResponse(smart.getCoreService());
            processed = true;
        }
        if (!processed) {
            LOG.warn("Unknown protobuf response: {}", smart);
        }
    }

    private void processProtobufCoreResponse(GdiCore.CoreService coreService) {
        if (coreService.hasSyncResponse()) {
            final GdiCore.CoreService.SyncResponse syncResponse = coreService.getSyncResponse();
            LOG.info("Received sync status: {}", syncResponse.getStatus());
        }
        LOG.warn("Unknown CoreService response: {}", coreService);
    }

    private void processProtobufDeviceStatusResponse(GdiDeviceStatus.DeviceStatusService deviceStatusService) {
        if (deviceStatusService.hasRemoteDeviceBatteryStatusResponse()) {
            final GdiDeviceStatus.DeviceStatusService.RemoteDeviceBatteryStatusResponse batteryStatusResponse = deviceStatusService.getRemoteDeviceBatteryStatusResponse();
            final int batteryLevel = batteryStatusResponse.getCurrentBatteryLevel();
            LOG.info("Received remote battery status {}: level={}", batteryStatusResponse.getStatus(), batteryLevel);
            final GBDeviceEventBatteryInfo batteryEvent = new GBDeviceEventBatteryInfo();
            batteryEvent.level = (short) batteryLevel;
            handleGBDeviceEvent(batteryEvent);
            return;
        }
        if (deviceStatusService.hasActivityStatusResponse()) {
            final GdiDeviceStatus.DeviceStatusService.ActivityStatusResponse activityStatusResponse = deviceStatusService.getActivityStatusResponse();
            LOG.info("Received activity status: {}", activityStatusResponse.getStatus());
            return;
        }
        LOG.warn("Unknown DeviceStatusService response: {}", deviceStatusService);
    }

    private void processProtobufFindMyWatchResponse(GdiFindMyWatch.FindMyWatchService findMyWatchService) {
        if (findMyWatchService.hasCancelRequest()) {
            LOG.info("Watch search cancelled, watch found");
            GBApplication.deviceService().onFindDevice(false);
            return;
        }
        if (findMyWatchService.hasCancelResponse() || findMyWatchService.hasFindResponse()) {
            LOG.debug("Received findMyWatch response");
            return;
        }
        LOG.warn("Unknown FindMyWatchService response: {}", findMyWatchService);
    }

    private void processMusicControlCapabilities(MusicControlCapabilitiesMessage capabilitiesMessage) {
        LOG.info("Processing music control capabilities request caps={}", capabilitiesMessage.supportedCapabilities);
        sendMessage(new MusicControlCapabilitiesResponseMessage(0, GarminMusicControlCommand.values()).packet);
    }

    private void processWeatherRequest(WeatherRequestMessage requestMessage) {
        LOG.info("Processing weather request fmt={}, {} hrs, {}/{}", requestMessage.format, requestMessage.hoursOfForecast, requestMessage.latitude, requestMessage.longitude);
        sendMessage(new WeatherRequestResponseMessage(0, 0, 1, 300).packet);
    }

    private void processCurrentTimeRequest(CurrentTimeRequestMessage requestMessage) {
        long now = System.currentTimeMillis();
        final TimeZone timeZone = TimeZone.getDefault();
        final Calendar calendar = Calendar.getInstance(timeZone);
        calendar.setTimeInMillis(now);
        int dstOffset = calendar.get(Calendar.DST_OFFSET) / 1000;
        int timeZoneOffset = timeZone.getOffset(now) / 1000;
        int garminTimestamp = GarminTimeUtils.javaMillisToGarminTimestamp(now);

        LOG.info("Processing current time request #{}: time={}, DST={}, ofs={}", requestMessage.referenceID, garminTimestamp, dstOffset, timeZoneOffset);
        sendMessage(new CurrentTimeRequestResponseMessage(0, requestMessage.referenceID, garminTimestamp, timeZoneOffset, dstOffset).packet);
    }

    private void processResponseMessage(ResponseMessage responseMessage, byte[] packet) {
        switch (responseMessage.requestID) {
            case VivomoveConstants.MESSAGE_DIRECTORY_FILE_FILTER_REQUEST:
                processDirectoryFileFilterResponse(DirectoryFileFilterResponseMessage.parsePacket(packet));
                break;
            case VivomoveConstants.MESSAGE_DOWNLOAD_REQUEST:
                fileDownloadQueue.onDownloadRequestResponse(DownloadRequestResponseMessage.parsePacket(packet));
                break;
            case VivomoveConstants.MESSAGE_UPLOAD_REQUEST:
                fileUploadQueue.onUploadRequestResponse(UploadRequestResponseMessage.parsePacket(packet));
                break;
            case VivomoveConstants.MESSAGE_FILE_TRANSFER_DATA:
                fileUploadQueue.onFileTransferResponse(FileTransferDataResponseMessage.parsePacket(packet));
                break;
            case VivomoveConstants.MESSAGE_CREATE_FILE_REQUEST:
                fileUploadQueue.onCreateFileRequestResponse(CreateFileResponseMessage.parsePacket(packet));
                break;
            case VivomoveConstants.MESSAGE_FIT_DEFINITION:
                processFitDefinitionResponse(FitDefinitionResponseMessage.parsePacket(packet));
                break;
            case VivomoveConstants.MESSAGE_FIT_DATA:
                processFitDataResponse(FitDataResponseMessage.parsePacket(packet));
                break;
            case VivomoveConstants.MESSAGE_PROTOBUF_REQUEST:
                processProtobufRequestResponse(ProtobufRequestResponseMessage.parsePacket(packet));
                break;
            case VivomoveConstants.MESSAGE_DEVICE_SETTINGS:
                processDeviceSettingsResponse(SetDeviceSettingsResponseMessage.parsePacket(packet));
                break;
            case VivomoveConstants.MESSAGE_SYSTEM_EVENT:
                processSystemEventResponse(SystemEventResponseMessage.parsePacket(packet));
                break;
            case VivomoveConstants.MESSAGE_SUPPORTED_FILE_TYPES_REQUEST:
                processSupportedFileTypesResponse(SupportedFileTypesResponseMessage.parsePacket(packet));
                break;
            case VivomoveConstants.MESSAGE_GNCS_DATA_SOURCE:
                gncsDataSourceQueue.responseReceived(GncsDataSourceResponseMessage.parsePacket(packet));
                break;
            case VivomoveConstants.MESSAGE_AUTH_NEGOTIATION:
                processAuthNegotiationRequestResponse(AuthNegotiationResponseMessage.parsePacket(packet));
                break;
            default:
                LOG.info("Received response to message {}: {}", responseMessage.requestID, responseMessage.getStatusStr());
                break;
        }
    }

    private void processDirectoryFileFilterResponse(DirectoryFileFilterResponseMessage responseMessage) {
        if (responseMessage.status == VivomoveConstants.STATUS_ACK && responseMessage.response == DirectoryFileFilterResponseMessage.RESPONSE_DIRECTORY_FILTER_APPLIED) {
            LOG.info("Received response to directory file filter request: {}/{}, requesting download of directory data", responseMessage.status, responseMessage.response);
            fileDownloadQueue.addToDownloadQueue(0, 0);
        } else {
            LOG.error("Directory file filter request failed: {}/{}", responseMessage.status, responseMessage.response);
        }
    }

    private void processSupportedFileTypesResponse(SupportedFileTypesResponseMessage responseMessage) {
        final StringBuilder supportedTypes = new StringBuilder();
        for (SupportedFileTypesResponseMessage.FileTypeInfo type : responseMessage.fileTypes) {
            if (supportedTypes.length() > 0) {
                supportedTypes.append(", ");
            }
            supportedTypes.append(String.format(Locale.ROOT, "%d/%d: %s", type.fileDataType, type.fileSubType, type.garminDeviceFileType));
        }
        LOG.info("Received the list of supported file types (status={}): {}", responseMessage.status, supportedTypes);
    }

    private void processDeviceSettingsResponse(SetDeviceSettingsResponseMessage responseMessage) {
        LOG.info("Received response to device settings message: status={}, response={}", responseMessage.status, responseMessage.response);
    }

    private void processAuthNegotiationRequestResponse(AuthNegotiationResponseMessage responseMessage) {
        LOG.info("Received response to auth negotiation message: status={}, response={}, LTK={}, algorithms={}", responseMessage.status, responseMessage.response, responseMessage.longTermKeyAvailability, responseMessage.supportedEncryptionAlgorithms);
    }

    private void processSystemEventResponse(SystemEventResponseMessage responseMessage) {
        LOG.info("Received response to system event message: status={}, response={}", responseMessage.status, responseMessage.response);
    }

    private void processFitDefinitionResponse(FitDefinitionResponseMessage responseMessage) {
        LOG.info("Received response to FIT definition message: status={}, FIT response={}", responseMessage.status, responseMessage.fitResponse);
    }

    private void processFitDataResponse(FitDataResponseMessage responseMessage) {
        LOG.info("Received response to FIT data message: status={}, FIT response={}", responseMessage.status, responseMessage.fitResponse);
    }

    private void processProtobufRequestResponse(ProtobufRequestResponseMessage responseMessage) {
        LOG.info("Received response to protobuf message #{}@{}:  status={}, error={}", responseMessage.requestId, responseMessage.dataOffset, responseMessage.protobufStatus, responseMessage.error);
    }

    private void processDeviceInformationMessage(DeviceInformationMessage deviceInformationMessage) {
        LOG.info("Received device information: protocol {}, product {}, unit {}, SW {}, max packet {}, BT name {}, device name {}, device model {}", deviceInformationMessage.protocolVersion, deviceInformationMessage.productNumber, deviceInformationMessage.unitNumber, deviceInformationMessage.getSoftwareVersionStr(), deviceInformationMessage.maxPacketSize, deviceInformationMessage.bluetoothFriendlyName, deviceInformationMessage.deviceName, deviceInformationMessage.deviceModel);

        this.maxPacketSize = deviceInformationMessage.maxPacketSize;
        this.gncsDataSourceQueue = new GncsDataSourceQueue(communicator, maxPacketSize);

        final GBDeviceEventVersionInfo deviceEventVersionInfo = new GBDeviceEventVersionInfo();
        deviceEventVersionInfo.fwVersion = deviceInformationMessage.getSoftwareVersionStr();
        handleGBDeviceEvent(deviceEventVersionInfo);

        gbDevice.setState(GBDevice.State.INITIALIZED);
        gbDevice.sendDeviceUpdateIntent(getContext());

        // prepare and send response
        final boolean protocolVersionSupported = deviceInformationMessage.protocolVersion / 100 == 1;
        if (!protocolVersionSupported) {
            LOG.error("Unsupported protocol version {}", deviceInformationMessage.protocolVersion);
        }
        final int protocolFlags = protocolVersionSupported ? 1 : 0;
        final DeviceInformationResponseMessage deviceInformationResponseMessage = new DeviceInformationResponseMessage(VivomoveConstants.STATUS_ACK, 112, -1, VivomoveConstants.GADGETBRIDGE_UNIT_NUMBER, BuildConfig.VERSION_CODE, 16384, getBluetoothAdapter().getName(), Build.MANUFACTURER, Build.DEVICE, protocolFlags);

        sendMessage(deviceInformationResponseMessage.packet);
    }

    private void processConfigurationMessage(ConfigurationMessage configurationMessage) {
        this.capabilities = GarminCapability.setFromBinary(configurationMessage.configurationPayload);

        if (LOG.isInfoEnabled()) {
            LOG.info("Received configuration message; capabilities: {}", GarminCapability.setToString(capabilities));
        }

        // prepare and send response
        sendMessage(new GenericResponseMessage(VivomoveConstants.MESSAGE_CONFIGURATION, VivomoveConstants.STATUS_ACK).packet);

        // and report our own configuration/capabilities
        final byte[] ourCapabilityFlags = GarminCapability.setToBinary(VivomoveConstants.OUR_CAPABILITIES);
        sendMessage(new ConfigurationMessage(ourCapabilityFlags).packet);

        // initialize current time and settings
        sendCurrentTime();
        sendSettings();

        // and everything is ready now
        sendSyncReady();
        requestBatteryStatusUpdate();
        sendFitDefinitions();
        sendFitConnectivityMessage();
        requestSupportedFileTypes();
    }

    private void sendProtobufRequest(byte[] protobufMessage) {
        final int requestId = getNextProtobufRequestId();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Sending {}B protobuf request #{}: {}", protobufMessage.length, requestId, GB.hexdump(protobufMessage, 0, protobufMessage.length));
        }
        sendMessage(new ProtobufRequestMessage(requestId, 0, protobufMessage.length, protobufMessage.length, protobufMessage).packet);
    }

    private void requestBatteryStatusUpdate() {
        sendProtobufRequest(
                GdiSmartProto.Smart.newBuilder()
                        .setDeviceStatusService(
                                GdiDeviceStatus.DeviceStatusService.newBuilder()
                                        .setRemoteDeviceBatteryStatusRequest(
                                                GdiDeviceStatus.DeviceStatusService.RemoteDeviceBatteryStatusRequest.newBuilder()
                                        )
                        )
                        .build()
                        .toByteArray());
    }

    private void requestActivityStatus() {
        sendProtobufRequest(
                GdiSmartProto.Smart.newBuilder()
                        .setDeviceStatusService(
                                GdiDeviceStatus.DeviceStatusService.newBuilder()
                                        .setActivityStatusRequest(
                                                GdiDeviceStatus.DeviceStatusService.ActivityStatusRequest.newBuilder()
                                        )
                        )
                        .build()
                        .toByteArray());
    }

    private void sendRequestSync() {
        sendProtobufRequest(
                GdiSmartProto.Smart.newBuilder()
                        .setCoreService(
                                GdiCore.CoreService.newBuilder()
                                        .setSyncRequest(
                                                GdiCore.CoreService.SyncRequest.newBuilder()
                                        )
                        )
                        .build()
                        .toByteArray());
    }

    private void requestSupportedFileTypes() {
        LOG.info("Requesting list of supported file types");
        sendMessage(new SupportedFileTypesRequestMessage().packet);
    }

    private void sendSyncReady() {
        sendMessage(new SystemEventMessage(GarminSystemEventType.SYNC_READY, 0).packet);
    }

    private void sendCurrentTime() {
        final Map<GarminDeviceSetting, Object> settings = new LinkedHashMap<>(3);

        long now = System.currentTimeMillis();
        final TimeZone timeZone = TimeZone.getDefault();
        final Calendar calendar = Calendar.getInstance(timeZone);
        calendar.setTimeInMillis(now);
        int dstOffset = calendar.get(Calendar.DST_OFFSET) / 1000;
        int timeZoneOffset = timeZone.getOffset(now) / 1000;
        int garminTimestamp = GarminTimeUtils.javaMillisToGarminTimestamp(now);

        settings.put(GarminDeviceSetting.CURRENT_TIME, garminTimestamp);
        settings.put(GarminDeviceSetting.DAYLIGHT_SAVINGS_TIME_OFFSET, dstOffset);
        settings.put(GarminDeviceSetting.TIME_ZONE_OFFSET, timeZoneOffset);
        // TODO: NEXT_DAYLIGHT_SAVINGS_START, NEXT_DAYLIGHT_SAVINGS_END
        LOG.info("Setting time to {}, dstOffset={}, tzOffset={} (DST={})", garminTimestamp, dstOffset, timeZoneOffset, timeZone.inDaylightTime(new Date(now)) ? 1 : 0);
        sendMessage(new SetDeviceSettingsMessage(settings).packet);
    }

    private void sendSettings() {
        final Map<GarminDeviceSetting, Object> settings = new LinkedHashMap<>(3);

        settings.put(GarminDeviceSetting.WEATHER_CONDITIONS_ENABLED, true);
        settings.put(GarminDeviceSetting.WEATHER_ALERTS_ENABLED, true);
        settings.put(GarminDeviceSetting.AUTO_UPLOAD_ENABLED, true);
        LOG.info("Sending settings");
        sendMessage(new SetDeviceSettingsMessage(settings).packet);
    }

    private void sendFitDefinitions() {
        sendMessage(new FitDefinitionMessage(
                FitMessageDefinitions.DEFINITION_CONNECTIVITY,
                FitMessageDefinitions.DEFINITION_WEATHER_CONDITIONS,
                FitMessageDefinitions.DEFINITION_WEATHER_ALERT,
                FitMessageDefinitions.DEFINITION_DEVICE_SETTINGS
        ).packet);
    }

    private void sendFitConnectivityMessage() {
        final FitMessage connectivityMessage = new FitMessage(FitMessageDefinitions.DEFINITION_CONNECTIVITY);
        connectivityMessage.setField(0, FitBool.TRUE);
        connectivityMessage.setField(1, FitBool.TRUE);
        connectivityMessage.setField(2, FitBool.INVALID);
        connectivityMessage.setField(4, FitBool.TRUE);
        connectivityMessage.setField(5, FitBool.TRUE);
        connectivityMessage.setField(6, FitBool.TRUE);
        connectivityMessage.setField(7, FitBool.TRUE);
        connectivityMessage.setField(8, FitBool.TRUE);
        connectivityMessage.setField(9, FitBool.TRUE);
        connectivityMessage.setField(10, FitBool.TRUE);
        connectivityMessage.setField(13, FitBool.TRUE);
        sendMessage(new FitDataMessage(connectivityMessage).packet);
    }

    private void sendWeatherConditions(WeatherSpec weather) {
        final FitMessage weatherConditionsMessage = new FitMessage(FitMessageDefinitions.DEFINITION_WEATHER_CONDITIONS);
        weatherConditionsMessage.setField(253, GarminTimeUtils.unixTimeToGarminTimestamp(weather.timestamp));
        weatherConditionsMessage.setField(0, 0); // 0 = current, 2 = hourly_forecast, 3 = daily_forecast
        weatherConditionsMessage.setField(1, weather.currentTemp);
        weatherConditionsMessage.setField(2, FitWeatherConditions.openWeatherCodeToFitWeatherStatus(weather.currentConditionCode));
        weatherConditionsMessage.setField(3, weather.windDirection);
        weatherConditionsMessage.setField(4, Math.round(weather.windSpeed * 1000.0 / 3.6));
        weatherConditionsMessage.setField(7, weather.currentHumidity);
        weatherConditionsMessage.setField(8, weather.location);
        final Calendar timestamp = Calendar.getInstance();
        timestamp.setTimeInMillis(weather.timestamp * 1000L);
        weatherConditionsMessage.setField(12, timestamp.get(Calendar.DAY_OF_WEEK));
        weatherConditionsMessage.setField(13, weather.todayMaxTemp);
        weatherConditionsMessage.setField(14, weather.todayMinTemp);

        sendMessage(new FitDataMessage(weatherConditionsMessage).packet);
    }

    /*
    private void sendWeatherAlert() {
        final FitMessage weatherConditionsMessage = new FitMessage(FitMessageDefinitions.DEFINITION_WEATHER_ALERT);
        weatherConditionsMessage.setField(253, GarminTimeUtils.javaMillisToGarminTimestamp(System.currentTimeMillis()));
        weatherConditionsMessage.setField(0, "TESTRPT");
        final Calendar issue = Calendar.getInstance();
        issue.set(2019, 8, 27, 0, 0, 0);
        final Calendar expiry = Calendar.getInstance();
        issue.set(2019, 8, 29, 0, 0, 0);
        weatherConditionsMessage.setField(1, GarminTimeUtils.javaMillisToGarminTimestamp(issue.getTimeInMillis()));
        weatherConditionsMessage.setField(2, GarminTimeUtils.javaMillisToGarminTimestamp(expiry.getTimeInMillis()));
        weatherConditionsMessage.setField(3, FitWeatherConditions.ALERT_SEVERITY_ADVISORY);
        weatherConditionsMessage.setField(4, FitWeatherConditions.ALERT_TYPE_SEVERE_THUNDERSTORM);

        sendMessage(new FitDataMessage(weatherConditionsMessage).packet);
    }
    */

    private void sendNotification(AncsEvent event, NotificationData notification) {
        if (event == AncsEvent.NOTIFICATION_ADDED) {
            notificationStorage.registerNewNotification(notification);
        } else {
            notificationStorage.deleteNotification(notification.spec.getId());
        }
        sendMessage(new GncsNotificationSourceMessage(event, notification.flags, notification.category, notificationStorage.getCountInCategory(notification.category), notification.spec.getId()).packet);
    }

    private void listFiles(int filterType) {
        LOG.info("Requesting file list (filter={})", filterType);
        sendMessage(new DirectoryFileFilterRequestMessage(filterType).packet);
    }

    private void downloadFile(int fileIndex) {
        LOG.info("Requesting download of file {}", fileIndex);
        fileDownloadQueue.addToDownloadQueue(fileIndex, 0);
    }

    private void downloadGarminDeviceXml() {
        LOG.info("Requesting Garmin device XML download");
        fileDownloadQueue.addToDownloadQueue(VivomoveConstants.GARMIN_DEVICE_XML_FILE_INDEX, 0);
    }

    private void sendBatteryStatus(int batteryLevel) {
        LOG.info("Sending battery status");
        sendMessage(new BatteryStatusMessage(batteryLevel).packet);
    }

    private void doSync() {
        LOG.info("Starting sync");
        fitImporter = new FitDbImporter(getDevice());
        // sendMessage(new SystemEventMessage(GarminSystemEventType.PAIR_START, 0).packet);
        listFiles(DirectoryFileFilterRequestMessage.FILTER_NO_FILTER);
        // TODO: Localization
        GB.updateTransferNotification(null, "Downloading list of files", true, 0, getContext());
    }

    @Override
    public boolean useAutoConnect() {
        return true;
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        if (notificationSubscription) {
            sendNotification(AncsEvent.NOTIFICATION_ADDED, new NotificationData(notificationSpec));
        } else {
            LOG.debug("No notification subscription is active, ignoring notification");
        }
    }

    @Override
    public void onDeleteNotification(int id) {
        final NotificationData notificationData = notificationStorage.retrieveNotification(id);
        if (notificationData != null) {
            sendNotification(AncsEvent.NOTIFICATION_REMOVED, notificationData);
        }
    }

    @Override
    public void onSetTime() {
        sendCurrentTime();
    }

    @Override
    public void onSetMusicInfo(MusicSpec musicSpec) {
        sendMessage(new MusicControlEntityUpdateMessage(
                new AmsEntityAttribute(AmsEntity.TRACK, AmsEntityAttribute.TRACK_ATTRIBUTE_ARTIST, 0, musicSpec.artist),
                new AmsEntityAttribute(AmsEntity.TRACK, AmsEntityAttribute.TRACK_ATTRIBUTE_ALBUM, 0, musicSpec.album),
                new AmsEntityAttribute(AmsEntity.TRACK, AmsEntityAttribute.TRACK_ATTRIBUTE_TITLE, 0, musicSpec.track),
                new AmsEntityAttribute(AmsEntity.TRACK, AmsEntityAttribute.TRACK_ATTRIBUTE_DURATION, 0, String.valueOf(musicSpec.duration))
        ).packet);
    }

    @Override
    public void onEnableRealtimeSteps(boolean enable) {
        communicator.enableRealtimeSteps(enable);
    }

    @Override
    public void onFetchRecordedData(int dataTypes) {
        doSync();
    }

    @Override
    public void onReset(int flags) {
        switch (flags) {
            case GBDeviceProtocol.RESET_FLAGS_FACTORY_RESET:
                LOG.warn("Requesting factory reset");
                sendMessage(new SystemEventMessage(GarminSystemEventType.FACTORY_RESET, 1).packet);
                break;

            default:
                GB.toast(getContext(), "This kind of reset not supported for this device", Toast.LENGTH_LONG, GB.ERROR);
                break;
        }
    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(boolean enable) {
        communicator.enableRealtimeHeartRate(enable);
    }

    @Override
    public void onFindDevice(boolean start) {
        if (start) {
            sendProtobufRequest(
                    GdiSmartProto.Smart.newBuilder()
                            .setFindMyWatchService(
                                    GdiFindMyWatch.FindMyWatchService.newBuilder()
                                            .setFindRequest(
                                                    GdiFindMyWatch.FindMyWatchService.FindMyWatchRequest.newBuilder()
                                                            .setTimeout(60)
                                            )
                            )
                            .build()
                            .toByteArray());
        } else {
            sendProtobufRequest(
                    GdiSmartProto.Smart.newBuilder()
                            .setFindMyWatchService(
                                    GdiFindMyWatch.FindMyWatchService.newBuilder()
                                            .setCancelRequest(
                                                    GdiFindMyWatch.FindMyWatchService.FindMyWatchCancelRequest.newBuilder()
                                            )
                            )
                            .build()
                            .toByteArray());
        }
    }

    private void updateDeviceSettings() {
        final FitMessage deviceSettingsMessage = new FitMessage(FitMessageDefinitions.DEFINITION_DEVICE_SETTINGS);
        deviceSettingsMessage.setField("bluetooth_connection_alerts_enabled", 0);
        deviceSettingsMessage.setField("auto_lock_enabled", 0);
        deviceSettingsMessage.setField("activity_tracker_enabled", 1);
        deviceSettingsMessage.setField("alarm_time", 0);
        deviceSettingsMessage.setField("ble_auto_upload_enabled", 1);
        deviceSettingsMessage.setField("autosync_min_steps", 1000);
        deviceSettingsMessage.setField("vibration_intensity", 2);
        deviceSettingsMessage.setField("screen_timeout", 0);
        deviceSettingsMessage.setField("mounting_side", 1);
        deviceSettingsMessage.setField("phone_notification_activity_filter", 0);
        deviceSettingsMessage.setField("auto_goal_enabled", 1);
        deviceSettingsMessage.setField("autosync_min_time", 60);
        deviceSettingsMessage.setField("glance_mode_layout", 0);
        deviceSettingsMessage.setField("time_offset", 7200);
        deviceSettingsMessage.setField("phone_notification_default_filter", 0);
        deviceSettingsMessage.setField("alarm_mode", -1);
        deviceSettingsMessage.setField("backlight_timeout", 5);
        deviceSettingsMessage.setField("sedentary_hr_alert_threshold", 100);
        deviceSettingsMessage.setField("backlight_brightness", 0);
        deviceSettingsMessage.setField("time_zone", 254);
        deviceSettingsMessage.setField("sedentary_hr_alert_state", 0);
        deviceSettingsMessage.setField("auto_activity_start_enabled", 0);
        deviceSettingsMessage.setField("alarm_days", 0);
        deviceSettingsMessage.setField("default_page", 1);
        deviceSettingsMessage.setField("message_tones_enabled", 2);
        deviceSettingsMessage.setField("key_tones_enabled", 2);
        deviceSettingsMessage.setField("date_mode", 0);
        deviceSettingsMessage.setField("backlight_gesture", 1);
        deviceSettingsMessage.setField("backlight_mode", 3);
        deviceSettingsMessage.setField("move_alert_enabled", 1);
        deviceSettingsMessage.setField("sleep_do_not_disturb_enabled", 0);
        deviceSettingsMessage.setField("display_orientation", 2);
        deviceSettingsMessage.setField("time_mode", 1);
        deviceSettingsMessage.setField("pages_enabled", 127);
        deviceSettingsMessage.setField("smart_notification_display_orientation", 0);
        deviceSettingsMessage.setField("display_steps_goal_enabled", 1);
        sendMessage(new FitDataMessage(deviceSettingsMessage).packet);
    }

    private boolean foreground;

    @Override
    public void onSendWeather(WeatherSpec weatherSpec) {
        sendWeatherConditions(weatherSpec);
    }

    private final Map<Integer, DirectoryEntry> filesToDownload = new ConcurrentHashMap<>();
    private long totalDownloadSize;
    private long lastTransferNotificationTimestamp;

    private GarminFitFile findDownloadedFitFile(DaoSession session, Device device, User user, int fileNumber, int fileDataType, int fileSubType) {
        final GarminFitFileDao fileDao = session.getGarminFitFileDao();
        final Query<GarminFitFile> query = fileDao.queryBuilder()
                .where(
                        GarminFitFileDao.Properties.DeviceId.eq(device.getId()),
                        GarminFitFileDao.Properties.UserId.eq(user.getId()),
                        GarminFitFileDao.Properties.FileNumber.eq(fileNumber),
                        GarminFitFileDao.Properties.FileDataType.eq(fileDataType),
                        GarminFitFileDao.Properties.FileSubType.eq(fileSubType)
                )
                .build();

        final List<GarminFitFile> files = query.list();
        return files.size() > 0 ? files.get(0) : null;
    }

    @Override
    public void onDirectoryDownloaded(DirectoryData directoryData) {
        if (filesToDownload.size() != 0) {
            throw new IllegalStateException("File download already in progress!");
        }

        long totalSize = 0;
        try {
            try (final DBHandler dbHandler = GBApplication.acquireDB()) {
                final DaoSession session = dbHandler.getDaoSession();
                final GBDevice gbDevice = getDevice();
                final Device device = DBHelper.getDevice(gbDevice, session);
                final User user = DBHelper.getUser(session);

                for (final DirectoryEntry entry : directoryData.entries) {
                    LOG.info("File #{}: type {}/{} #{}, {}B, flags {}/{}, timestamp {}", entry.fileIndex, entry.fileDataType, entry.fileSubType, entry.fileNumber, entry.fileSize, entry.specificFlags, entry.fileFlags, entry.fileDate);
                    if (entry.fileIndex == 0) {
                        // ?
                        LOG.warn("File #0 reported?");
                        continue;
                    }

                    final long timestamp = entry.fileDate.getTime();
                    final GarminFitFile alreadyDownloadedFile = findDownloadedFitFile(session, device, user, entry.fileNumber, entry.fileDataType, entry.fileSubType);
                    if (alreadyDownloadedFile == null) {
                        LOG.debug("File not yet downloaded");
                    } else {
                        if (alreadyDownloadedFile.getFileTimestamp() == timestamp && alreadyDownloadedFile.getFileSize() == entry.fileSize) {
                            LOG.debug("File already downloaded, skipping");
                            continue;
                        } else {
                            LOG.info("File #{} modified after previous download, removing previous version and re-downloading", entry.fileIndex);
                            alreadyDownloadedFile.delete();
                        }
                    }

                    filesToDownload.put(entry.fileIndex, entry);
                    fileDownloadQueue.addToDownloadQueue(entry.fileIndex, entry.fileSize);
                    totalSize += entry.fileSize;
                }
            }
        } catch (Exception e) {
            LOG.error("Error storing data to DB", e);
        }

        totalDownloadSize = totalSize;
    }

    @Override
    public void onFileDownloadComplete(int fileIndex, byte[] data) {
        LOG.info("Downloaded file {}: {} bytes", fileIndex, data.length);
        final DirectoryEntry downloadedDirectoryEntry = filesToDownload.get(fileIndex);
        if (downloadedDirectoryEntry == null) {
            LOG.warn("Unexpected file {} downloaded", fileIndex);
        } else {
            try (final DBHandler dbHandler = GBApplication.acquireDB()) {
                final DaoSession session = dbHandler.getDaoSession();

                final GBDevice gbDevice = getDevice();
                final Device device = DBHelper.getDevice(gbDevice, session);
                final User user = DBHelper.getUser(session);
                final int ts = (int) (System.currentTimeMillis() / 1000);

                final GarminFitFile garminFitFile = new GarminFitFile(null, ts, device.getId(), user.getId(), downloadedDirectoryEntry.fileNumber, downloadedDirectoryEntry.fileDataType, downloadedDirectoryEntry.fileSubType, downloadedDirectoryEntry.fileDate.getTime(), downloadedDirectoryEntry.specificFlags, downloadedDirectoryEntry.fileSize, STORE_FIT_FILES ? data : null);
                session.getGarminFitFileDao().insert(garminFitFile);
            } catch (Exception e) {
                LOG.error("Error saving downloaded file to database", e);
            }
        }

        if (fileIndex <= 0x8000) {
            fitImporter.processFitFile(fitParser.parseFitFile(data));
        } else {
            LOG.debug("Not importing file {} as FIT", fileIndex);
        }
    }

    @Override
    public void onFileDownloadError(int fileIndex) {
        LOG.error("Failed to download file {}", fileIndex);
    }

    @Override
    public void onDownloadProgress(long remainingBytes) {
        LOG.debug("{}B/{} remaining to download", remainingBytes, totalDownloadSize);
        if (remainingBytes == 0) {
            GB.updateTransferNotification(null, null, false, 100, getContext());
        } else if (totalDownloadSize > 0) {
            final long now = System.currentTimeMillis();
            if (now - lastTransferNotificationTimestamp < 1000) {
                // do not issue updates too often
                return;
            }
            lastTransferNotificationTimestamp = now;
            // TODO: Localization
            GB.updateTransferNotification(null, "Downloading data", true, Math.round(100.0f * (totalDownloadSize - remainingBytes) / totalDownloadSize), getContext());
        }
    }

    @Override
    public void onAllDownloadsCompleted() {
        LOG.info("All downloads completed");
        GB.updateTransferNotification(null, null, false, 100, getContext());
        sendMessage(new SystemEventMessage(GarminSystemEventType.SYNC_COMPLETE, 0).packet);
        if (fitImporter != null) {
            fitImporter.processData();
            GB.signalActivityDataFinish();
        }
        fitImporter = null;
    }
}
