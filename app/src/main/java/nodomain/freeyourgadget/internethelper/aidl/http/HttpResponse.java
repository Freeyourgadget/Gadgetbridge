package nodomain.freeyourgadget.internethelper.aidl.http;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class HttpResponse implements Parcelable {
    private final int status;
    private final Bundle headers;
    private final byte[] body;

    protected HttpResponse(final Parcel in) {
        status = in.readInt();
        headers = in.readBundle(ClassLoader.getSystemClassLoader());
        final int bodyLength = in.readInt();
        body = new byte[bodyLength];
        in.readByteArray(body);
    }

    public HttpResponse(final int status, final Bundle headers, final byte[] body) {
        this.status = status;
        this.headers = headers;
        this.body = body;
    }

    public static final Creator<HttpResponse> CREATOR = new Creator<HttpResponse>() {
        @Override
        public HttpResponse createFromParcel(final Parcel in) {
            return new HttpResponse(in);
        }

        @Override
        public HttpResponse[] newArray(final int size) {
            return new HttpResponse[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull final Parcel dest, final int flags) {
        dest.writeInt(status);
        dest.writeBundle(headers);
        dest.writeInt(body.length);
        dest.writeByteArray(body);
    }
}
