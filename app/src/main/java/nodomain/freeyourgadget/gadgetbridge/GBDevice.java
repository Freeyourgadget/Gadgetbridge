package nodomain.freeyourgadget.gadgetbridge;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

public class GBDevice {
    public static final String ACTION_DEVICE_CHANGED
            = "nodomain.freeyourgadget.gadgetbride.gbdevice.action.device_changed";

    private final String name;
    private final String address;
    private final Type type;
    private String firmwareVersion = null;
    private State state = State.NOT_CONNECTED;

    public GBDevice(String address, String name, Type type) {
        this.address = address;
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    public boolean isConnected() {
        return state.ordinal() >= State.CONNECTED.ordinal();
    }

    public boolean isInitialized() {
        return state.ordinal() >= State.INITIALIZED.ordinal();
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    String getStateString() {
        switch (state) {
            case NOT_CONNECTED:
                return "not connected"; // TODO: do not hardcode
            case CONNECTING:
                return "connecting";
            case CONNECTED:
                return "connected";
            case INITIALIZED:
                return "initialized";
        }
        return "unknown state";
    }

    public String getInfoString() {
        if (firmwareVersion != null) {
            return getStateString() + " (FW: " + firmwareVersion + ")";
        } else {
            return getStateString();
        }
    }

    public Type getType() {
        return type;
    }

    // TODO: this doesn't really belong here
    public void sendDeviceUpdateIntent(Context context) {
        Intent deviceUpdateIntent = new Intent(ACTION_DEVICE_CHANGED);
        deviceUpdateIntent.putExtra("device_address", getAddress());
        deviceUpdateIntent.putExtra("device_state", getState().ordinal());
        deviceUpdateIntent.putExtra("firmware_version", getFirmwareVersion());

        LocalBroadcastManager.getInstance(context).sendBroadcast(deviceUpdateIntent);
    }

    public enum State {
        // Note: the order is important!
        NOT_CONNECTED,
        CONNECTING,
        CONNECTED,
        INITIALIZED
    }

    public enum Type {
        UNKNOWN,
        PEBBLE,
        MIBAND
    }
}
