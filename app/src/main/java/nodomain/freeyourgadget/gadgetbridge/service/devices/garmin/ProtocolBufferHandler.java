package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin;

import com.google.protobuf.InvalidProtocolBufferException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.proto.vivomovehr.GdiCalendarService;
import nodomain.freeyourgadget.gadgetbridge.proto.vivomovehr.GdiCore;
import nodomain.freeyourgadget.gadgetbridge.proto.vivomovehr.GdiDeviceStatus;
import nodomain.freeyourgadget.gadgetbridge.proto.vivomovehr.GdiFindMyWatch;
import nodomain.freeyourgadget.gadgetbridge.proto.vivomovehr.GdiSmartProto;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.GFDIMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.ProtobufMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status.ProtobufStatusMessage;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.calendar.CalendarEvent;
import nodomain.freeyourgadget.gadgetbridge.util.calendar.CalendarManager;

public class ProtocolBufferHandler implements MessageHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ProtocolBufferHandler.class);
    private final GarminSupport deviceSupport;
    private final Map<Integer, ProtobufFragment> chunkedFragmentsMap;
    private final int maxChunkSize = 375; //tested on Vívomove Style
    private int lastProtobufRequestId;

    public ProtocolBufferHandler(GarminSupport deviceSupport) {
        this.deviceSupport = deviceSupport;
        chunkedFragmentsMap = new HashMap<>();
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

            final GdiSmartProto.Smart smart;
            try {
                smart = GdiSmartProto.Smart.parseFrom(protobufFragment.fragmentBytes);
            } catch (InvalidProtocolBufferException e) {
                LOG.error("Failed to parse protobuf message ({}): {}", e.getLocalizedMessage(), GB.hexdump(protobufFragment.fragmentBytes));
                return null;
            }
            boolean processed = false;
            if (smart.hasCoreService()) { //TODO: unify request and response???
                processed = true;
                processProtobufCoreResponse(smart.getCoreService());
//                return prepareProtobufResponse(processProtobufCoreRequest(smart.getCoreService()), message.getRequestId());
            }
            if (smart.hasCalendarService()) {
                return prepareProtobufResponse(processProtobufCalendarRequest(smart.getCalendarService()), message.getRequestId());
            }
            if (smart.hasDeviceStatusService()) {
                processed = true;
                processProtobufDeviceStatusResponse(smart.getDeviceStatusService());
            }
            if (smart.hasFindMyWatchService()) {
                processed = true;
                processProtobufFindMyWatchResponse(smart.getFindMyWatchService());
            }
            if (!processed) {
                LOG.warn("Unknown protobuf request: {}", smart);
                message.setStatusMessage(new ProtobufStatusMessage(message.getMessageType(), GFDIMessage.Status.ACK, message.getRequestId(), message.getDataOffset(), ProtobufStatusMessage.ProtobufChunkStatus.DISCARDED, ProtobufStatusMessage.ProtobufStatusCode.UNKNOWN_REQUEST_ID));
            }
        }
        return null;
    }

    private ProtobufMessage processIncoming(ProtobufStatusMessage statusMessage) {
        LOG.info("Processing protobuf status message #{}@{}:  status={}, error={}", statusMessage.getRequestId(), statusMessage.getDataOffset(), statusMessage.getProtobufChunkStatus(), statusMessage.getProtobufStatusCode());
        //TODO: check status and react accordingly, right now we blindly proceed to next chunk
        if (chunkedFragmentsMap.containsKey(statusMessage.getRequestId()) && statusMessage.isOK()) {
            final ProtobufFragment protobufFragment = chunkedFragmentsMap.get(statusMessage.getRequestId());
            LOG.debug("Protobuf message #{} found in queue: {}", statusMessage.getRequestId(), GB.hexdump(protobufFragment.fragmentBytes));

            if (protobufFragment.totalLength <= (statusMessage.getDataOffset() + maxChunkSize)) {
                chunkedFragmentsMap.remove(protobufFragment);
            }

            return protobufFragment.getNextChunk(statusMessage);
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

                watchEvents.add(GdiCalendarService.CalendarService.CalendarEvent.newBuilder()
                        .setTitle(mEvt.getTitle())
                        .setAllDay(mEvt.isAllDay())
                        .setBegin(mEvt.getBeginSeconds())
                        .setEnd(mEvt.getEndSeconds())
                        .setLocation(StringUtils.defaultString(mEvt.getLocation()))
                        .setDescription(StringUtils.defaultString(mEvt.getDescription()))
                        .build()
                );
            }

            LOG.debug("CalendarService Sending {} events to watch", watchEvents.size());
            return GdiSmartProto.Smart.newBuilder().setCalendarService(
                    GdiCalendarService.CalendarService.newBuilder().setCalendarResponse(
                            GdiCalendarService.CalendarService.CalendarServiceResponse.newBuilder()
                                    .addAllCalendarEvent(watchEvents)
                                    .setUnknown(1)
                    )
            ).build();
        }
        LOG.warn("Unknown CalendarService request: {}", calendarService);
        return GdiSmartProto.Smart.newBuilder().setCalendarService(
                GdiCalendarService.CalendarService.newBuilder().setCalendarResponse(
                        GdiCalendarService.CalendarService.CalendarServiceResponse.newBuilder()
                                .setUnknown(0)
                )
        ).build();
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

//    private GdiSmartProto.Smart processProtobufCoreRequest(GdiCore.CoreService coreService) {
//        if (coreService.hasLocationUpdatedSetEnabledRequest()) { //TODO: enable location support in devicesupport
//            LOG.debug("Location CoreService: {}", coreService);
//
//            final GdiCore.CoreService.LocationUpdatedSetEnabledRequest locationUpdatedSetEnabledRequest = coreService.getLocationUpdatedSetEnabledRequest();
//
//            LOG.info("Received locationUpdatedSetEnabledRequest status: {}", locationUpdatedSetEnabledRequest.getEnabled());
//
//            GdiCore.CoreService.LocationUpdatedSetEnabledResponse.Builder response = GdiCore.CoreService.LocationUpdatedSetEnabledResponse.newBuilder()
//                            .setStatus(GdiCore.CoreService.LocationUpdatedSetEnabledResponse.Status.OK);
//
//            //TODO: check and follow the preference in coordinator (see R.xml.devicesettings_workout_send_gps_to_band )
//            if(locationUpdatedSetEnabledRequest.getEnabled()) {
//                response.addRequests(GdiCore.CoreService.LocationUpdatedSetEnabledResponse.Requested.newBuilder()
//                        .setRequested(locationUpdatedSetEnabledRequest.getRequests(0).getRequested())
//                        .setStatus(GdiCore.CoreService.LocationUpdatedSetEnabledResponse.Requested.RequestedStatus.OK));
//            }
//
//            deviceSupport.processLocationUpdateRequest(locationUpdatedSetEnabledRequest.getEnabled(), locationUpdatedSetEnabledRequest.getRequestsList());
//
//            return GdiSmartProto.Smart.newBuilder().setCoreService(
//                    GdiCore.CoreService.newBuilder().setLocationUpdatedSetEnabledResponse(response)).build();
//        }
//        LOG.warn("Unknown CoreService request: {}", coreService);
//        return null;
//    }

    private void processProtobufFindMyWatchResponse(GdiFindMyWatch.FindMyWatchService findMyWatchService) {
        if (findMyWatchService.hasCancelRequest()) {
            LOG.info("Watch found");
        }
        if (findMyWatchService.hasCancelResponse() || findMyWatchService.hasFindResponse()) {
            LOG.debug("Received findMyWatch response");
        }
        LOG.warn("Unknown FindMyWatchService response: {}", findMyWatchService);
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
        LOG.info("Preparing protobuf message. Type{}, #{}, {}B: {}", garminMessage, requestId, bytes.length, GB.hexdump(bytes, 0, bytes.length));

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
