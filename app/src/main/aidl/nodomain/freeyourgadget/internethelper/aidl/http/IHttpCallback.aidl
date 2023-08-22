package nodomain.freeyourgadget.internethelper.aidl.http;

import nodomain.freeyourgadget.internethelper.aidl.http.HttpResponse;

oneway interface IHttpCallback {
    void onResponse(in HttpResponse response);
    void onException(in String message);
}
