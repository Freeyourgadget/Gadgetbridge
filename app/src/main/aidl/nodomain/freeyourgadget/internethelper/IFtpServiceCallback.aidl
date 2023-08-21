package nodomain.freeyourgadget.internethelper;

oneway interface IFtpServiceCallback {
    void onConnect(boolean success, String msg);
    void onLogin(boolean success, String msg);
    void onList(String path, in List<String> directories, in List<String> files);
    void onUpload(String path, boolean success, String msg);
    void onDownload(String path, boolean success, String msg);
}
