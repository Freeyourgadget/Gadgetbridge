package nodomain.freeyourgadget.internethelper;

interface IFtpService {
    int version();

    String createClient();
    void destroyClient(String client);

    void connect(String client, String host, int port);
    void disconnect(String client);

    void login(String client, String username, String password);

    List<String> list(String client, String path);
    void upload(String client, String path, in byte[] bytes);
    byte[] download(String client, String path);
}
