package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin;

import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Location;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.protobuf.InvalidProtocolBufferException;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminPreferences;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminRealtimeSettingsFragment;
import nodomain.freeyourgadget.gadgetbridge.externalevents.gps.GBLocationProviderType;
import nodomain.freeyourgadget.gadgetbridge.externalevents.gps.GBLocationService;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiCalendarService;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiCore;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiDataTransferService;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiDeviceStatus;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiFindMyWatch;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiHttpService;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiNotificationsService;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiSettingsService;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiSmartProto;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiSmsNotification;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http.DataTransferHandler;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http.HttpHandler;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.GFDIMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.ProtobufMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status.ProtobufStatusMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.pebble.webview.CurrentPosition;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.calendar.CalendarEvent;
import nodomain.freeyourgadget.gadgetbridge.util.calendar.CalendarManager;

public class ProtocolBufferHandler implements MessageHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ProtocolBufferHandler.class);
    private final GarminSupport deviceSupport;
    private final Map<Integer, ProtobufFragment> chunkedFragmentsMap;
    private final int maxChunkSize = 375; //tested on Vívomove Style
    private int lastProtobufRequestId;
    private final HttpHandler httpHandler;
    private final DataTransferHandler dataTransferHandler;

    private final Map<GdiSmsNotification.SmsNotificationService.CannedListType, String[]> cannedListTypeMap = new HashMap<>();

    public ProtocolBufferHandler(GarminSupport deviceSupport) {
        this.deviceSupport = deviceSupport;
        chunkedFragmentsMap = new HashMap<>();
        httpHandler = new HttpHandler(deviceSupport);
        dataTransferHandler = new DataTransferHandler();
    }

    private int getNextProtobufRequestId() {
        lastProtobufRequestId = (lastProtobufRequestId + 1) % 65536;
        return lastProtobufRequestId;
    }

    public ProtobufMessage handle(GFDIMessage protobufMessage) {
        if (protobufMessage instanceof ProtobufMessage) {
            return processIncoming((ProtobufMessage) protobufMessage);
        } else if (protobufMessage instanceof ProtobufStatusMessage) {
            return processIncoming((ProtobufStatusMessage) protobufMessage);
        }
        return null;
    }

    private ProtobufMessage processIncoming(ProtobufMessage message) {
        ProtobufFragment protobufFragment = processChunkedMessage(message);

        if (protobufFragment.isComplete()) { //message is now complete
            LOG.info("Received protobuf message #{}, {}B: {}", message.getRequestId(), protobufFragment.totalLength, GB.hexdump(protobufFragment.fragmentBytes, 0, protobufFragment.totalLength));
            chunkedFragmentsMap.remove(message.getRequestId());

            final GdiSmartProto.Smart smart;
            try {
                smart = GdiSmartProto.Smart.parseFrom(protobufFragment.fragmentBytes);
            } catch (InvalidProtocolBufferException e) {
                LOG.error("Failed to parse protobuf message ({}): {}", e.getLocalizedMessage(), GB.hexdump(protobufFragment.fragmentBytes));
                return null;
            }
            boolean processed = false;
            if (smart.hasCoreService()) { //TODO: unify request and response???
                return prepareProtobufResponse(processProtobufCoreRequest(smart.getCoreService()), message.getRequestId());
            }
            if (smart.hasCalendarService()) {
                return prepareProtobufResponse(processProtobufCalendarRequest(smart.getCalendarService()), message.getRequestId());
            }
            if (smart.hasSmsNotificationService()) {
                return prepareProtobufResponse(processProtobufSmsNotificationMessage(smart.getSmsNotificationService()), message.getRequestId());
            }
            if (smart.hasHttpService()) {
                final GdiHttpService.HttpService response = httpHandler.handle(smart.getHttpService());
                if (response == null) {
                    return null;
                }
                return prepareProtobufResponse(GdiSmartProto.Smart.newBuilder().setHttpService(response).build(), message.getRequestId());
            }
            if (smart.hasDataTransferService()) {
                final GdiDataTransferService.DataTransferService response = dataTransferHandler.handle(smart.getDataTransferService(), message.getRequestId());
                if (response == null) {
                    return null;
                }
                return prepareProtobufResponse(GdiSmartProto.Smart.newBuilder().setDataTransferService(response).build(), message.getRequestId());
            }
            if (smart.hasDeviceStatusService()) {
                processed = true;
                processProtobufDeviceStatusResponse(smart.getDeviceStatusService());
            }
            if (smart.hasFindMyWatchService()) {
                processed = true;
                processProtobufFindMyWatchResponse(smart.getFindMyWatchService());
            }
            if (smart.hasSettingsService()) {
                processed = true;
                processProtobufSettingsService(smart.getSettingsService());
            }
            if (smart.hasNotificationsService()) {
                return prepareProtobufResponse(processProtobufNotificationsServiceMessage(smart.getNotificationsService()), message.getRequestId());
            }
            if (processed) {
                message.setStatusMessage(new ProtobufStatusMessage(
                        message.getMessageType(),
                        GFDIMessage.Status.ACK,
                        message.getRequestId(),
                        message.getDataOffset(),
                        ProtobufStatusMessage.ProtobufChunkStatus.KEPT,
                        ProtobufStatusMessage.ProtobufStatusCode.NO_ERROR
                ));
            } else {
                LOG.warn("Unknown protobuf request: {}", smart);
                message.setStatusMessage(new ProtobufStatusMessage(
                        message.getMessageType(),
                        GFDIMessage.Status.ACK,
                        message.getRequestId(),
                        message.getDataOffset(),
                        ProtobufStatusMessage.ProtobufChunkStatus.DISCARDED,
                        ProtobufStatusMessage.ProtobufStatusCode.UNKNOWN_REQUEST_ID
                ));
            }
        }
        return null;
    }

    private ProtobufMessage processIncoming(ProtobufStatusMessage statusMessage) {
        //TODO: check status and react accordingly, right now we blindly proceed to next chunk
        if (statusMessage.isOK()) {
            DataTransferHandler.onDataChunkSuccessfullyReceived(statusMessage.getRequestId());

            if (chunkedFragmentsMap.containsKey(statusMessage.getRequestId())) {
                final ProtobufFragment protobufFragment = chunkedFragmentsMap.get(statusMessage.getRequestId());
                LOG.debug("Protobuf message #{} found in queue: {}", statusMessage.getRequestId(), GB.hexdump(protobufFragment.fragmentBytes));

                if (protobufFragment.totalLength <= (statusMessage.getDataOffset() + maxChunkSize)) {
                    chunkedFragmentsMap.remove(statusMessage.getRequestId());
                    return null;
                }
                return protobufFragment.getNextChunk(statusMessage);
            }
        }
        return null;
    }

    private ProtobufFragment processChunkedMessage(ProtobufMessage message) {
        if (message.isComplete()) //comment this out if for any reason also smaller messages should end up in the map
            return new ProtobufFragment(message.getMessageBytes());

        if (message.getDataOffset() == 0) { //store new messages beginning at 0, overwrite old messages
            chunkedFragmentsMap.put(message.getRequestId(), new ProtobufFragment(message));
            LOG.info("Protobuf request put in queue: #{} , {}", message.getRequestId(), GB.hexdump(message.getMessageBytes()));
        } else {
            if (chunkedFragmentsMap.containsKey(message.getRequestId())) {
                ProtobufFragment oldFragment = chunkedFragmentsMap.get(message.getRequestId());
                chunkedFragmentsMap.put(message.getRequestId(),
                        new ProtobufFragment(oldFragment, message));
            }
        }
        return chunkedFragmentsMap.get(message.getRequestId());
    }

    private GdiSmartProto.Smart processProtobufCalendarRequest(GdiCalendarService.CalendarService calendarService) {
        if (calendarService.hasCalendarRequest()) {
            GdiCalendarService.CalendarService.CalendarServiceRequest calendarServiceRequest = calendarService.getCalendarRequest();

            CalendarManager upcomingEvents = new CalendarManager(deviceSupport.getContext(), deviceSupport.getDevice().getAddress());
            List<CalendarEvent> mEvents = upcomingEvents.getCalendarEventList();
            List<GdiCalendarService.CalendarService.CalendarEvent> watchEvents = new ArrayList<>();

            for (CalendarEvent mEvt : mEvents) {
                if (mEvt.getEndSeconds() < calendarServiceRequest.getBegin() ||
                        mEvt.getBeginSeconds() > calendarServiceRequest.getEnd()) {
                    LOG.debug("CalendarService Skipping event {} that is out of requested time range", mEvt.getTitle());
                    continue;
                }
                if (!calendarServiceRequest.getIncludeAllDay() && mEvt.isAllDay()) {
                    LOG.debug("CalendarService Skipping event {} that is AllDay", mEvt.getTitle());
                    continue;
                }

                if (watchEvents.size() >= calendarServiceRequest.getMaxEvents() * 2) { //NOTE: Tested with values higher than double of the reported max without issues
                    LOG.debug("Reached the maximum number of events supported by the watch");
                    break;
                }

                final int startDateSeconds;
                final int endDateSeconds;

                if (mEvt.isAllDay()) {
                    // For all-day events, garmin expects the start and end date to match the midnight boundaries
                    // in the user's timezone. However, the calendar event will have them in the UTC timezone,
                    // so we need to convert it
                    startDateSeconds = (int) (DateTimeUtils.utcDateTimeToLocal(mEvt.getBegin()) / 1000);
                    endDateSeconds = (int) (DateTimeUtils.utcDateTimeToLocal(mEvt.getEnd()) / 1000);
                } else {
                    startDateSeconds = mEvt.getBeginSeconds();
                    endDateSeconds = mEvt.getEndSeconds();
                }

                final GdiCalendarService.CalendarService.CalendarEvent.Builder event = GdiCalendarService.CalendarService.CalendarEvent.newBuilder()
                        .setTitle(mEvt.getTitle().substring(0, Math.min(mEvt.getTitle().length(), calendarServiceRequest.getMaxTitleLength())))
                        .setAllDay(mEvt.isAllDay())
                        .setStartDate(startDateSeconds)
                        .setEndDate(endDateSeconds);

                if (calendarServiceRequest.getIncludeLocation() && mEvt.getLocation() != null) {
                    event.setLocation(mEvt.getLocation().substring(0, Math.min(mEvt.getLocation().length(), calendarServiceRequest.getMaxLocationLength())));
                }

                if (calendarServiceRequest.getIncludeDescription() && mEvt.getDescription() != null) {
                    event.setDescription(mEvt.getDescription().substring(0, Math.min(mEvt.getDescription().length(), calendarServiceRequest.getMaxDescriptionLength())));
                }
                if (calendarServiceRequest.getIncludeOrganizer() && mEvt.getOrganizer() != null) {
                    event.setDescription(mEvt.getOrganizer().substring(0, Math.min(mEvt.getOrganizer().length(), calendarServiceRequest.getMaxOrganizerLength())));
                }
                watchEvents.add(event.build());
            }

            LOG.debug("CalendarService Sending {} events to watch", watchEvents.size());
            return GdiSmartProto.Smart.newBuilder().setCalendarService(
                    GdiCalendarService.CalendarService.newBuilder().setCalendarResponse(
                            GdiCalendarService.CalendarService.CalendarServiceResponse.newBuilder()
                                    .addAllCalendarEvent(watchEvents)
                                    .setStatus(GdiCalendarService.CalendarService.CalendarServiceResponse.ResponseStatus.OK)
                    )
            ).build();
        }
        LOG.warn("Unknown CalendarService request: {}", calendarService);
        return GdiSmartProto.Smart.newBuilder().setCalendarService(
                GdiCalendarService.CalendarService.newBuilder().setCalendarResponse(
                        GdiCalendarService.CalendarService.CalendarServiceResponse.newBuilder()
                                .setStatus(GdiCalendarService.CalendarService.CalendarServiceResponse.ResponseStatus.UNKNOWN_RESPONSE_STATUS)
                )
        ).build();
    }

    private void processProtobufDeviceStatusResponse(GdiDeviceStatus.DeviceStatusService deviceStatusService) {
        if (deviceStatusService.hasRemoteDeviceBatteryStatusResponse()) {
            final GdiDeviceStatus.DeviceStatusService.RemoteDeviceBatteryStatusResponse batteryStatusResponse = deviceStatusService.getRemoteDeviceBatteryStatusResponse();
            final int batteryLevel = batteryStatusResponse.getCurrentBatteryLevel();
            LOG.info("Received remote battery status {}: level={}", batteryStatusResponse.getStatus(), batteryLevel);
            final GBDeviceEventBatteryInfo batteryEvent = new GBDeviceEventBatteryInfo();
            batteryEvent.level = (short) batteryLevel;
            deviceSupport.evaluateGBDeviceEvent(batteryEvent);
            return;
        }
        if (deviceStatusService.hasActivityStatusResponse()) {
            final GdiDeviceStatus.DeviceStatusService.ActivityStatusResponse activityStatusResponse = deviceStatusService.getActivityStatusResponse();
            LOG.info("Received activity status: {}", activityStatusResponse.getStatus());
            return;
        }
        LOG.warn("Unknown DeviceStatusService response: {}", deviceStatusService);
    }

    private GdiSmartProto.Smart processProtobufCoreRequest(GdiCore.CoreService coreService) {
        if (coreService.hasSyncResponse()) {
            final GdiCore.CoreService.SyncResponse syncResponse = coreService.getSyncResponse();
            LOG.info("Received sync status: {}", syncResponse.getStatus());
            return null;
        }

        if (coreService.hasGetLocationRequest()) {
            LOG.info("Got location request");
            final Location location = new CurrentPosition().getLastKnownLocation();
            final GdiCore.CoreService.GetLocationResponse.Builder response = GdiCore.CoreService.GetLocationResponse.newBuilder();
            if (location.getLatitude() == 0 && location.getLongitude() == 0) {
                response.setStatus(GdiCore.CoreService.GetLocationResponse.Status.NO_VALID_LOCATION);
            } else {
                response.setStatus(GdiCore.CoreService.GetLocationResponse.Status.OK)
                        .setLocationData(GarminUtils.toLocationData(location, GdiCore.CoreService.DataType.GENERAL_LOCATION));
            }
            return GdiSmartProto.Smart.newBuilder().setCoreService(
                    GdiCore.CoreService.newBuilder().setGetLocationResponse(response)).build();
        }

        if (coreService.hasLocationUpdatedSetEnabledRequest()) {
            final GdiCore.CoreService.LocationUpdatedSetEnabledRequest locationUpdatedSetEnabledRequest = coreService.getLocationUpdatedSetEnabledRequest();

            LOG.info("Received locationUpdatedSetEnabledRequest status: {}", locationUpdatedSetEnabledRequest.getEnabled());

            GdiCore.CoreService.LocationUpdatedSetEnabledResponse.Builder response = GdiCore.CoreService.LocationUpdatedSetEnabledResponse.newBuilder()
                    .setStatus(GdiCore.CoreService.LocationUpdatedSetEnabledResponse.Status.OK);

            final boolean sendGpsPref = deviceSupport.getDevicePrefs().getBoolean(DeviceSettingsPreferenceConst.PREF_WORKOUT_SEND_GPS_TO_BAND, false);

            GdiCore.CoreService.Request realtimeRequest = null;

            if (locationUpdatedSetEnabledRequest.getEnabled()) {
                for (final GdiCore.CoreService.Request request : locationUpdatedSetEnabledRequest.getRequestsList()) {
                    final GdiCore.CoreService.LocationUpdatedSetEnabledResponse.Requested.RequestedStatus requestedStatus;
                    if (GdiCore.CoreService.DataType.REALTIME_TRACKING.equals(request.getRequested())) {
                        realtimeRequest = request;
                        if (sendGpsPref) {
                            requestedStatus = GdiCore.CoreService.LocationUpdatedSetEnabledResponse.Requested.RequestedStatus.OK;
                        } else {
                            requestedStatus = GdiCore.CoreService.LocationUpdatedSetEnabledResponse.Requested.RequestedStatus.KO;
                        }
                    } else {
                        requestedStatus = GdiCore.CoreService.LocationUpdatedSetEnabledResponse.Requested.RequestedStatus.KO;
                    }

                    response.addRequests(
                            GdiCore.CoreService.LocationUpdatedSetEnabledResponse.Requested.newBuilder()
                                    .setRequested(request.getRequested())
                                    .setStatus(requestedStatus)
                    );
                }
            }

            if (sendGpsPref) {
                if (realtimeRequest != null) {
                    GBLocationService.start(
                            deviceSupport.getContext(),
                            deviceSupport.getDevice(),
                            GBLocationProviderType.GPS,
                            1000 // TODO from realtimeRequest
                    );
                } else {
                    GBLocationService.stop(deviceSupport.getContext(), deviceSupport.getDevice());
                }
            }

            return GdiSmartProto.Smart.newBuilder().setCoreService(
                    GdiCore.CoreService.newBuilder().setLocationUpdatedSetEnabledResponse(response)).build();
        }

        LOG.warn("Unknown CoreService request: {}", coreService);
        return null;
    }

    private GdiSmartProto.Smart processProtobufNotificationsServiceMessage(final GdiNotificationsService.NotificationsService notificationsService) {
        if (notificationsService.hasPictureRequest()) {
            final GdiNotificationsService.PictureRequest pictureRequest = notificationsService.getPictureRequest();
            final int notificationId = pictureRequest.getNotificationId();
            final Bitmap bmp = deviceSupport.getNotificationAttachmentBitmap(notificationId);
            if (bmp == null) {
                return null;
            }

            final GdiNotificationsService.PictureParameters parameters = pictureRequest.getParameters();
            final int targetHeight = (int) Math.round(bmp.getHeight() * ((double) parameters.getWidth() / bmp.getWidth()));

            final Bitmap scaledBmp = Bitmap.createScaledBitmap(bmp, parameters.getWidth(), targetHeight, true);
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            scaledBmp.compress(Bitmap.CompressFormat.JPEG, parameters.getQuality(), baos);
            final byte[] imageBytes = baos.toByteArray();

            final int transferId = DataTransferHandler.registerData(imageBytes);

            final GdiNotificationsService.PictureResponse response = GdiNotificationsService.PictureResponse.newBuilder()
                    .setUnk1(1)
                    .setNotificationId(notificationId)
                    .setUnk3(0)
                    .setUnk4(1)
                    .setDataTransferItem(
                            GdiNotificationsService.DataTransferItem.newBuilder()
                                    .setId(transferId)
                                    .setSize(imageBytes.length)
                                    .build()
                    )
                    .build();
            return GdiSmartProto.Smart.newBuilder().setNotificationsService(
                    GdiNotificationsService.NotificationsService.newBuilder().setPictureResponse(response)
            ).build();
        }

        LOG.warn("Protobuf notificationsService request not implemented: {}", notificationsService);
        return null;
    }

    private GdiSmartProto.Smart processProtobufSmsNotificationMessage(GdiSmsNotification.SmsNotificationService smsNotificationService) {
        if (smsNotificationService.hasSmsCannedListRequest()) {
            LOG.debug("Got request for sms canned list");

            // Mark canned messages as supported
            deviceSupport.evaluateGBDeviceEvent(new GBDeviceEventUpdatePreferences(GarminPreferences.PREF_FEAT_CANNED_MESSAGES, true));

            List<GdiSmsNotification.SmsNotificationService.CannedListType> requestedTypes = smsNotificationService.getSmsCannedListRequest().getRequestedTypesList();

            populateCannedListTypeMap(requestedTypes);

            GdiSmsNotification.SmsNotificationService.SmsCannedListResponse.Builder builder = GdiSmsNotification.SmsNotificationService.SmsCannedListResponse.newBuilder()
                    .setStatus(GdiSmsNotification.SmsNotificationService.ResponseStatus.SUCCESS);
            boolean found = false;
            for (GdiSmsNotification.SmsNotificationService.CannedListType requestedType : requestedTypes) {
                if (this.cannedListTypeMap.containsKey(requestedType)) {
                    found = true;
                    builder.addLists(GdiSmsNotification.SmsNotificationService.SmsCannedList.newBuilder()
                            .addAllResponse(Arrays.asList(Objects.requireNonNull(this.cannedListTypeMap.get(requestedType))))
                            .setType(requestedType)
                    );
                } else {
                    LOG.warn("Missing canned messages data for type {}", requestedType);
                }
            }
            if (!found)
                builder.setStatus(GdiSmsNotification.SmsNotificationService.ResponseStatus.GENERIC_ERROR);

            return GdiSmartProto.Smart.newBuilder().setSmsNotificationService(GdiSmsNotification.SmsNotificationService.newBuilder().setSmsCannedListResponse(builder)).build();
        } else {
            LOG.warn("Protobuf smsNotificationService request not implemented: {}", smsNotificationService);
            return null;
        }
    }

    private void populateCannedListTypeMap(List<GdiSmsNotification.SmsNotificationService.CannedListType> requestedTypes) {
        if (this.cannedListTypeMap.isEmpty()) {
            for (GdiSmsNotification.SmsNotificationService.CannedListType type :
                    requestedTypes) {
                String preferencesPrefix = "";
                if (GdiSmsNotification.SmsNotificationService.CannedListType.SMS_MESSAGE_RESPONSE.equals(type))
                    preferencesPrefix = "canned_reply_";
                else if (GdiSmsNotification.SmsNotificationService.CannedListType.PHONE_CALL_RESPONSE.equals(type))
                    preferencesPrefix = "canned_message_dismisscall_";
                else
                    continue;
                final ArrayList<String> messages = new ArrayList<>();
                for (int i = 1; i <= 16; i++) {
                    String message = deviceSupport.getDevicePrefs().getString(preferencesPrefix + i, null);
                    if (message != null && !message.isEmpty()) {
                        messages.add(message);
                    }
                }
                if (!messages.isEmpty())
                    this.cannedListTypeMap.put(type, messages.toArray(new String[0]));
            }
        }
    }

    private void processProtobufFindMyWatchResponse(GdiFindMyWatch.FindMyWatchService findMyWatchService) {
        if (findMyWatchService.hasCancelRequest()) {
            LOG.info("Watch found");
        }
        if (findMyWatchService.hasCancelResponse() || findMyWatchService.hasFindResponse()) {
            LOG.debug("Received findMyWatch response");
        }
        LOG.warn("Unknown FindMyWatchService response: {}", findMyWatchService);
    }

    private boolean processProtobufSettingsService(final GdiSettingsService.SettingsService settingsService) {
        boolean processed = false;

        if (settingsService.hasDefinitionResponse()) {
            processed = true;
            final Intent intent = new Intent(GarminRealtimeSettingsFragment.ACTION_SCREEN_DEFINITION);
            intent.putExtra(GarminRealtimeSettingsFragment.EXTRA_PROTOBUF, settingsService.getDefinitionResponse().getDefinition().toByteArray());
            LocalBroadcastManager.getInstance(deviceSupport.getContext()).sendBroadcast(intent);
        }

        if (settingsService.hasStateResponse()) {
            processed = true;
            final Intent intent = new Intent(GarminRealtimeSettingsFragment.ACTION_SCREEN_STATE);
            intent.putExtra(GarminRealtimeSettingsFragment.EXTRA_PROTOBUF, settingsService.getStateResponse().getState().toByteArray());
            LocalBroadcastManager.getInstance(deviceSupport.getContext()).sendBroadcast(intent);
        }

        if (settingsService.hasChangeResponse()) {
            processed = true;
            final Intent intent = new Intent(GarminRealtimeSettingsFragment.ACTION_CHANGE);
            intent.putExtra(GarminRealtimeSettingsFragment.EXTRA_PROTOBUF, settingsService.getChangeResponse().toByteArray());
            LocalBroadcastManager.getInstance(deviceSupport.getContext()).sendBroadcast(intent);
        }

        return processed;
    }

    public ProtobufMessage prepareProtobufRequest(GdiSmartProto.Smart protobufPayload) {
        if (null == protobufPayload)
            return null;
        final int requestId = getNextProtobufRequestId();
        return prepareProtobufMessage(protobufPayload.toByteArray(), GFDIMessage.GarminMessage.PROTOBUF_REQUEST, requestId);
    }

    private ProtobufMessage prepareProtobufResponse(GdiSmartProto.Smart protobufPayload, int requestId) {
        if (null == protobufPayload)
            return null;
        return prepareProtobufMessage(protobufPayload.toByteArray(), GFDIMessage.GarminMessage.PROTOBUF_RESPONSE, requestId);
    }

    private ProtobufMessage prepareProtobufMessage(byte[] bytes, GFDIMessage.GarminMessage garminMessage, int requestId) {
        if (bytes == null || bytes.length == 0)
            return null;
        LOG.info("Preparing protobuf message. Type {}, #{}, {}B: {}", garminMessage, requestId, bytes.length, GB.hexdump(bytes, 0, bytes.length));

        if (bytes.length > maxChunkSize) {
            chunkedFragmentsMap.put(requestId, new ProtobufFragment(bytes));
            return new ProtobufMessage(garminMessage,
                    requestId,
                    0,
                    bytes.length,
                    maxChunkSize,
                    ArrayUtils.subarray(bytes, 0, maxChunkSize));
        }
        return new ProtobufMessage(garminMessage, requestId, 0, bytes.length, bytes.length, bytes);
    }

    public ProtobufMessage setCannedMessages(CannedMessagesSpec cannedMessagesSpec) {
        final GdiSmsNotification.SmsNotificationService.CannedListType cannedListType;
        switch (cannedMessagesSpec.type) {
            case CannedMessagesSpec.TYPE_REJECTEDCALLS:
                cannedListType = GdiSmsNotification.SmsNotificationService.CannedListType.PHONE_CALL_RESPONSE;
                break;
            case CannedMessagesSpec.TYPE_GENERIC:
            case CannedMessagesSpec.TYPE_NEWSMS:
                cannedListType = GdiSmsNotification.SmsNotificationService.CannedListType.SMS_MESSAGE_RESPONSE;
                break;
            default:
                LOG.warn("Unknown canned messages type, ignoring.");
                return null;
        }

        this.cannedListTypeMap.put(cannedListType, cannedMessagesSpec.cannedMessages);

        GdiSmartProto.Smart smart = GdiSmartProto.Smart.newBuilder()
                .setSmsNotificationService(GdiSmsNotification.SmsNotificationService.newBuilder()
                        .setSmsCannedListChangedNotification(
                                GdiSmsNotification.SmsNotificationService.SmsCannedListChangedNotification.newBuilder().addChangedType(cannedListType)
                        )
                ).build();

        return prepareProtobufRequest(smart);
    }

    private class ProtobufFragment {
        private final byte[] fragmentBytes;
        private final int totalLength;

        public ProtobufFragment(byte[] fragmentBytes) {
            this.fragmentBytes = fragmentBytes;
            this.totalLength = fragmentBytes.length;
        }

        public ProtobufFragment(ProtobufMessage message) {
            if (message.getDataOffset() != 0)
                throw new IllegalArgumentException("Cannot create fragment if message is not the first of the sequence");
            this.fragmentBytes = message.getMessageBytes();
            this.totalLength = message.getTotalProtobufLength();
        }

        public ProtobufFragment(ProtobufFragment existing, ProtobufMessage toMerge) {
            if (toMerge.getDataOffset() != existing.fragmentBytes.length)
                throw new IllegalArgumentException("Cannot merge fragment: incoming message has different offset than needed");
            this.fragmentBytes = ArrayUtils.addAll(existing.fragmentBytes, toMerge.getMessageBytes());
            this.totalLength = existing.totalLength;
        }

        public ProtobufMessage getNextChunk(ProtobufStatusMessage protobufStatusMessage) {
            int start = protobufStatusMessage.getDataOffset() + maxChunkSize;
            int length = Math.min(maxChunkSize, this.fragmentBytes.length - start);

            return new ProtobufMessage(protobufStatusMessage.getMessageType(),
                    protobufStatusMessage.getRequestId(),
                    start,
                    this.totalLength,
                    length,
                    ArrayUtils.subarray(this.fragmentBytes, start, start + length));
        }

        public boolean isComplete() {
            return totalLength == fragmentBytes.length;
        }
    }


}
