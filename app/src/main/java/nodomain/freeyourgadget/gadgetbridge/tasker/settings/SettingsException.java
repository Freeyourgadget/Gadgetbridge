package nodomain.freeyourgadget.gadgetbridge.tasker.settings;

public class SettingsException extends RuntimeException {

    public SettingsException(String setting, String message) {
        super("Exception in setting [name=" + setting + "]: " + message);
    }

    public SettingsException(String setting, String message, Throwable cause) {
        super("Exception in setting [name=" + setting + "]: " + message, cause);
    }

}
