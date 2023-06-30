package nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.adablefs;

class AdaBleFsAction {
    public enum Method {
        UPLOAD,
        DELETE,
    };
    public String filenameorpath;
    public String secondFilenameorpath;
    public Method method;
    public byte[] data;

    public AdaBleFsAction(AdaBleFsAction.Method method, String filenameorpath, byte[] data) {
        this.filenameorpath = filenameorpath;
        this.method = method;
        this.data = data;
    }
}
