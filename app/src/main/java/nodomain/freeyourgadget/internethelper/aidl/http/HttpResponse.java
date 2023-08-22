package nodomain.freeyourgadget.internethelper.aidl.http;

import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class HttpResponse implements Parcelable {
    private final int status;
    private final HttpHeaders headers;
    private final ParcelFileDescriptor body;

    protected HttpResponse(final Parcel in) {
        status = in.readInt();
        headers = in.readParcelable(HttpResponse.class.getClassLoader());
        body = in.readParcelable(HttpResponse.class.getClassLoader());
    }

    public HttpResponse(final int status, final HttpHeaders headers, final ParcelFileDescriptor body) {
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
        dest.writeParcelable(headers, 0);
        dest.writeParcelable(body, 0);
    }

    public int getStatus() {
        return status;
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

    public ParcelFileDescriptor getBody() {
        return body;
    }
}
