package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.proto.vivomovehr.GdiDeviceStatus;
import nodomain.freeyourgadget.gadgetbridge.proto.vivomovehr.GdiFindMyWatch;
import nodomain.freeyourgadget.gadgetbridge.proto.vivomovehr.GdiSmartProto;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.communicator.ICommunicator;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.communicator.v1.CommunicatorV1;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.communicator.v2.CommunicatorV2;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.ConfigurationMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.GFDIMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.MusicControlEntityUpdateMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.ProtobufMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.SetDeviceSettingsMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.SystemEventMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status.ProtobufStatusMessage;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;


public class GarminSupport extends AbstractBTLEDeviceSupport implements ICommunicator.Callback {
    private static final Logger LOG = LoggerFactory.getLogger(GarminSupport.class);
    private final ProtocolBufferHandler protocolBufferHandler;
    private ICommunicator communicator;
    private MusicStateSpec musicStateSpec;
    private Timer musicStateTimer;

    public GarminSupport() {
        super(LOG);
        addSupportedService(CommunicatorV1.UUID_SERVICE_GARMIN_GFDI);
        addSupportedService(CommunicatorV2.UUID_SERVICE_GARMIN_ML_GFDI);
        protocolBufferHandler = new ProtocolBufferHandler(this);
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    @Override
    protected TransactionBuilder initializeDevice(final TransactionBuilder builder) {
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));

        if (getSupportedServices().contains(CommunicatorV2.UUID_SERVICE_GARMIN_ML_GFDI)) {
            communicator = new CommunicatorV2(this);
        } else if (getSupportedServices().contains(CommunicatorV1.UUID_SERVICE_GARMIN_GFDI)) {
            communicator = new CommunicatorV1(this);
        } else {
            LOG.warn("Failed to find a known Garmin service");
            builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.NOT_CONNECTED, getContext()));
            return builder;
        }

        communicator.initializeDevice(builder);

        return builder;
    }

    @Override
    public boolean onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
        final UUID characteristicUUID = characteristic.getUuid();
        if (super.onCharacteristicChanged(gatt, characteristic)) {
            LOG.debug("Change of characteristic {} handled by parent", characteristicUUID);
            return true;
        }

        return communicator.onCharacteristicChanged(gatt, characteristic);
    }

    @Override
    public void onMessage(final byte[] message) {
        if (null == message) {
            return; //message is not complete yet TODO check before calling
        }
//        LOG.debug("COBS decoded MESSAGE: {}", GB.hexdump(message));

        GFDIMessage parsedMessage = GFDIMessage.parseIncoming(message);

        if (null == parsedMessage) {
            return; //message cannot be handled
        }

        evaluateGBDeviceEvent(parsedMessage.getGBDeviceEvent());

        if (parsedMessage instanceof ProtobufMessage) {
            ProtobufMessage protobufMessage = protocolBufferHandler.processIncoming((ProtobufMessage) parsedMessage);
            if (protobufMessage != null) {
                communicator.sendMessage(protobufMessage.getOutgoingMessage());
                communicator.sendMessage(protobufMessage.getAckBytestream());
            }
        }

        communicator.sendMessage(parsedMessage.getAckBytestream());

        byte[] response = parsedMessage.getOutgoingMessage();
        if (null != response) {
//            LOG.debug("sending response {}", GB.hexdump(response));
            communicator.sendMessage(response);
        }

        if (parsedMessage instanceof ConfigurationMessage) { //the last forced message exchange
            completeInitialization();
        }

        if (parsedMessage instanceof ProtobufStatusMessage) {
            ProtobufMessage protobufMessage = protocolBufferHandler.processIncoming((ProtobufStatusMessage) parsedMessage);
            if (protobufMessage != null) {
                communicator.sendMessage(protobufMessage.getOutgoingMessage());
                communicator.sendMessage(protobufMessage.getAckBytestream());
            }
        }
    }

    private void completeInitialization() {

        enableWeather();

        onSetTime();

        //following is needed for vivomove style
        communicator.sendMessage(new SystemEventMessage(SystemEventMessage.GarminSystemEventType.SYNC_READY, 0).getOutgoingMessage());

        enableBatteryLevelUpdate();

        gbDevice.setState(GBDevice.State.INITIALIZED);
        gbDevice.sendDeviceUpdateIntent(getContext());

    }

    private void enableBatteryLevelUpdate() {
        final ProtobufMessage batteryLevelProtobufRequest = protocolBufferHandler.prepareProtobufRequest(GdiSmartProto.Smart.newBuilder()
                .setDeviceStatusService(
                        GdiDeviceStatus.DeviceStatusService.newBuilder()
                                .setRemoteDeviceBatteryStatusRequest(
                                        GdiDeviceStatus.DeviceStatusService.RemoteDeviceBatteryStatusRequest.newBuilder()
                                )
                )
                .build());
        communicator.sendMessage(batteryLevelProtobufRequest.getOutgoingMessage());
    }

    private void enableWeather() {
        final Map<SetDeviceSettingsMessage.GarminDeviceSetting, Object> settings = new LinkedHashMap<>(2);
        settings.put(SetDeviceSettingsMessage.GarminDeviceSetting.WEATHER_CONDITIONS_ENABLED, true);
        settings.put(SetDeviceSettingsMessage.GarminDeviceSetting.WEATHER_ALERTS_ENABLED, true);
        communicator.sendMessage(new SetDeviceSettingsMessage(settings).getOutgoingMessage());
    }

    @Override
    public void onSetTime() {
        communicator.sendMessage(new SystemEventMessage(SystemEventMessage.GarminSystemEventType.TIME_UPDATED, 0).getOutgoingMessage());
    }

    @Override
    public void onFindDevice(boolean start) {
        if (start) {
            final ProtobufMessage findMyWatch = protocolBufferHandler.prepareProtobufRequest(
                    GdiSmartProto.Smart.newBuilder()
                            .setFindMyWatchService(
                                    GdiFindMyWatch.FindMyWatchService.newBuilder()
                                            .setFindRequest(
                                                    GdiFindMyWatch.FindMyWatchService.FindMyWatchRequest.newBuilder()
                                                            .setTimeout(60)
                                            )
                            )
                            .build());
            communicator.sendMessage(findMyWatch.getOutgoingMessage());
        } else {
            final ProtobufMessage cancelFindMyWatch = protocolBufferHandler.prepareProtobufRequest(
                    GdiSmartProto.Smart.newBuilder()
                            .setFindMyWatchService(
                                    GdiFindMyWatch.FindMyWatchService.newBuilder()
                                            .setCancelRequest(
                                                    GdiFindMyWatch.FindMyWatchService.FindMyWatchCancelRequest.newBuilder()
                                            )
                            )
                            .build());
            communicator.sendMessage(cancelFindMyWatch.getOutgoingMessage());
        }
    }

    @Override
    public void onSetMusicInfo(MusicSpec musicSpec) {

        Map<MusicControlEntityUpdateMessage.MusicEntity, String> attributes = new HashMap<>();

        attributes.put(MusicControlEntityUpdateMessage.TRACK.ARTIST, musicSpec.artist);
        attributes.put(MusicControlEntityUpdateMessage.TRACK.ALBUM, musicSpec.album);
        attributes.put(MusicControlEntityUpdateMessage.TRACK.TITLE, musicSpec.track);
        attributes.put(MusicControlEntityUpdateMessage.TRACK.DURATION, String.valueOf(musicSpec.duration));

        communicator.sendMessage(new MusicControlEntityUpdateMessage(attributes).getOutgoingMessage());
    }

    @Override
    public void onSetMusicState(MusicStateSpec stateSpec) {
        musicStateSpec = stateSpec;

        if (musicStateTimer != null) {
            musicStateTimer.cancel();
            musicStateTimer.purge();
            musicStateTimer = null;
        }

        musicStateTimer = new Timer();
        int updatePeriod = 4000; //milliseconds
        LOG.debug("onSetMusicState: {}", stateSpec.toString());

        if (stateSpec.state == MusicStateSpec.STATE_PLAYING) {
            musicStateTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    String playing = "1";
                    String playRate = "1.0";
                    String position = new DecimalFormat("#.000").format(musicStateSpec.position);
                    musicStateSpec.position += updatePeriod / 1000;

                    Map<MusicControlEntityUpdateMessage.MusicEntity, String> attributes = new HashMap<>();
                    attributes.put(MusicControlEntityUpdateMessage.PLAYER.PLAYBACK_INFO, StringUtils.join(",", playing, playRate, position).toString());
                    communicator.sendMessage(new MusicControlEntityUpdateMessage(attributes).getOutgoingMessage());

                }
            }, 0, updatePeriod);
        } else {
            String playing = "0";
            String playRate = "0.0";
            String position = new DecimalFormat("#.###").format(stateSpec.position);

            Map<MusicControlEntityUpdateMessage.MusicEntity, String> attributes = new HashMap<>();
            attributes.put(MusicControlEntityUpdateMessage.PLAYER.PLAYBACK_INFO, StringUtils.join(",", playing, playRate, position).toString());
            communicator.sendMessage(new MusicControlEntityUpdateMessage(attributes).getOutgoingMessage());
        }
    }

}
