package nodomain.freeyourgadget.internethelper.aidl.http;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class HttpGetRequest implements Parcelable {
    private final String url;
    private final Bundle headers;

    protected HttpGetRequest(final Parcel in) {
        url = in.readString();
        headers = in.readBundle(ClassLoader.getSystemClassLoader());
    }

    public static final Creator<HttpGetRequest> CREATOR = new Creator<HttpGetRequest>() {
        @Override
        public HttpGetRequest createFromParcel(final Parcel in) {
            return new HttpGetRequest(in);
        }

        @Override
        public HttpGetRequest[] newArray(final int size) {
            return new HttpGetRequest[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull final Parcel dest, final int flags) {
        dest.writeString(url);
        dest.writeBundle(headers);
    }

    public String getUrl() {
        return url;
    }

    public Bundle getHeaders() {
        return headers;
    }
}
