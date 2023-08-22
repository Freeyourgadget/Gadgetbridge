package nodomain.freeyourgadget.internethelper.aidl.http;

import nodomain.freeyourgadget.internethelper.aidl.http.HttpGetRequest;
import nodomain.freeyourgadget.internethelper.aidl.http.HttpPostRequest;
import nodomain.freeyourgadget.internethelper.aidl.http.IHttpCallback;

interface IHttpService {
    int version();

    void get(in HttpGetRequest request, IHttpCallback cb);
}
