package nodomain.freeyourgadget.internethelper.aidl.ftp;

import nodomain.freeyourgadget.internethelper.aidl.ftp.IFtpCallback;

interface IFtpService {
    int version();

    String createClient(IFtpCallback callback);
    void destroyClient(String client);

    void connect(String client, String host, int port);
    void disconnect(String client);

    void login(String client, String username, String password);

    void list(String client, String path);
    void upload(String client, String localSrc, String remoteDest);
    void download(String client, String remoteSrc, String localDest);
}
