package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.application;

public class ApplicationInformation implements Comparable<ApplicationInformation> {
    String appName, version;
    int hash;
    byte fileHandle;

    public ApplicationInformation(String appName, String version, int hash, byte fileHandle) {
        this.appName = appName;
        this.version = version;
        this.hash = hash;
        this.fileHandle = fileHandle;
    }

    public String getAppName() {
        return appName;
    }

    public String getAppVersion() {
        return version;
    }

    public byte getFileHandle() {
        return fileHandle;
    }

    @Override
    public int compareTo(ApplicationInformation o) {
        return this.appName.toLowerCase().compareTo(o.getAppName().toLowerCase());
    }
}
