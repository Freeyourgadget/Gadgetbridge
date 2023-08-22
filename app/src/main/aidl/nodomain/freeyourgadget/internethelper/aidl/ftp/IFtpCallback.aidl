package nodomain.freeyourgadget.internethelper.aidl.ftp;

import nodomain.freeyourgadget.internethelper.aidl.ftp.FtpEntry;

oneway interface IFtpCallback {
    void onConnect(boolean success, String msg);
    void onLogin(boolean success, String msg);
    void onList(String path, in List<FtpEntry> entries);
    void onUpload(String path, boolean success, String msg);
    void onDownload(String path, boolean success, String msg);
}
