package nodomain.freeyourgadget.internethelper;

import nodomain.freeyourgadget.internethelper.IWifiServiceCallback;

interface IWifiService {
    int version();
    String getCurrentSsid();
    void connect(String ssid, String password, IWifiServiceCallback cb);
}
