package nodomain.freeyourgadget.internethelper.aidl.http;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Pair;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpHeaders implements Parcelable {
    private final List<Pair<String, String>> headers = new ArrayList<>();

    protected HttpHeaders(final Parcel in) {
        final int numHeaders = in.readInt();
        for (int i = 0; i < numHeaders; i++) {
            final String k = in.readString();
            final String v = in.readString();
            headers.add(Pair.create(k, v));
        }
    }

    public HttpHeaders() {
    }

    public static final Creator<HttpHeaders> CREATOR = new Creator<HttpHeaders>() {
        @Override
        public HttpHeaders createFromParcel(final Parcel in) {
            return new HttpHeaders(in);
        }

        @Override
        public HttpHeaders[] newArray(final int size) {
            return new HttpHeaders[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull final Parcel dest, final int flags) {
        dest.writeInt(headers.size());
        for (final Pair<String, String> header : headers) {
            dest.writeString(header.first);
            dest.writeString(header.second);
        }
    }

    public List<Pair<String, String>> getHeaders() {
        return headers;
    }

    public void addHeader(final String key, final String value) {
        headers.add(Pair.create(key, value));
    }

    public String get(final String key) {
        for (final Pair<String, String> header : headers) {
            if (header.first.equalsIgnoreCase(key)) {
                return header.second;
            }
        }

        return null;
    }

    public Map<String, String> toMap() {
        final Map<String, String> ret = new HashMap<>();
        for (final Pair<String, String> header : headers) {
            ret.put(header.first, header.second);
        }
        return ret;
    }
}
