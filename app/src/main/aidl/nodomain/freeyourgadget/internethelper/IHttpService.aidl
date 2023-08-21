package nodomain.freeyourgadget.internethelper;

import nodomain.freeyourgadget.internethelper.IHttpServiceCallback;

interface IHttpService {
    int version();

    void get(String url, IHttpServiceCallback cb);
}
