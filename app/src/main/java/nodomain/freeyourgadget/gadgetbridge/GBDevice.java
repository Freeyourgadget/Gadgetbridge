package nodomain.freeyourgadget.gadgetbridge;

public class GBDevice {
    private final String name;
    private final String address;
    private String firmwareVersion = null;
    private State state = State.NOT_CONNECTED;

    public void setState(State state) {
        this.state = state;
    }

    public enum State {
        NOT_CONNECTED,
        CONNECTING,
        CONNECTED
    }


    public GBDevice(String address, String name) {
        this.address = address;
        this.name = name;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
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

    public State getState() {
        return state;
    }

    String getStateString() {
        switch (state) {
            case NOT_CONNECTED:
                return "not connected"; // TODO: do not hardcode
            case CONNECTING:
                return "connecting";
            case CONNECTED:
                return "connected";
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
}
