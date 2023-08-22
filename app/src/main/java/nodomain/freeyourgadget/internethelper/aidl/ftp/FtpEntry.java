package nodomain.freeyourgadget.internethelper.aidl.ftp;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class FtpEntry implements Parcelable {
    public enum Type {
        DIRECTORY,
        FILE,
        SYMLINK,
        UNKNOWN,
    }

    private final String name;
    private final Type type;
    private final long size;
    private final String user;
    private final String group;
    private final long timestamp;
    private final String link;

    protected FtpEntry(final Parcel in) {
        name = in.readString();
        type = Type.values()[in.readInt()];
        size = in.readLong();
        user = in.readString();
        group = in.readString();
        timestamp = in.readLong();
        link = in.readString();
    }

    public FtpEntry(final String name, final Type type, final long size, final String user, final String group, final long timestamp, final String link) {
        this.name = name;
        this.type = type;
        this.size = size;
        this.user = user;
        this.group = group;
        this.timestamp = timestamp;
        this.link = link;
    }

    public static final Creator<FtpEntry> CREATOR = new Creator<FtpEntry>() {
        @Override
        public FtpEntry createFromParcel(final Parcel in) {
            return new FtpEntry(in);
        }

        @Override
        public FtpEntry[] newArray(final int size) {
            return new FtpEntry[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull final Parcel dest, final int flags) {
        dest.writeString(name);
        dest.writeInt(type.ordinal());
        dest.writeLong(size);
        dest.writeString(user);
        dest.writeString(group);
        dest.writeLong(timestamp);
        dest.writeString(link);
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public long getSize() {
        return size;
    }

    public String getUser() {
        return user;
    }

    public String getGroup() {
        return group;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getLink() {
        return link;
    }
}
