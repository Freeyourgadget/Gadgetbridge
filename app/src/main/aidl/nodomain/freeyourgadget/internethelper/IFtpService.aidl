package nodomain.freeyourgadget.internethelper;

import nodomain.freeyourgadget.internethelper.IFtpServiceCallback;

interface IFtpService {
    int version();

    String createClient();
    void destroyClient(String client);

    void connect(String client, String host, int port, IFtpServiceCallback callback);
    void disconnect(String client);

    void login(String client, String username, String password, IFtpServiceCallback callback);

    void list(String client, String path, IFtpServiceCallback callback);
    void upload(String client, String path, in byte[] bytes, IFtpServiceCallback callback);
    void download(String client, String path, IFtpServiceCallback callback);
}
