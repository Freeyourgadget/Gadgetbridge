/*  Copyright (C) 2015-2024 Arjan Schrijver, Carsten Pfeiffer, Taavi Eomäe

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.model;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import java.text.Collator;
import java.util.Objects;

public class GenericItem implements ItemWithDetails {
    private String name;
    private String details;
    private int icon;
    private boolean warning = false;
    private Bitmap preview;

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator<GenericItem>() {
        @Override
        public GenericItem createFromParcel(Parcel source) {
            GenericItem item = new GenericItem();
            item.setName(source.readString());
            item.setDetails(source.readString());
            item.setIcon(source.readInt());
            return item;
        }

        @Override
        public GenericItem[] newArray(int size) {
            return new GenericItem[size];
        }
    };

    public GenericItem(String name, String details) {
        this.name = name;
        this.details = details;
    }

    public GenericItem(String name) {
        this.name = name;
    }

    public GenericItem() {
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(details);
        dest.writeInt(icon);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDetails(String details) {
        if (details == null || details.equals("(Unknown version)")) {
            this.warning = true;
        }
        this.details = details;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public boolean getWarning() {
        return this.warning;
    }

    public void setWarning(boolean enable) {
        this.warning = enable;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDetails() {
        return details;
    }

    @Override
    public int getIcon() {
        return icon;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public void setPreview(Bitmap preview) {
        this.preview = preview;
    }

    public Bitmap getPreview() {
        return preview;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        if (this == o) {
            return true;
        }

        GenericItem that = (GenericItem) o;

        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    @Override
    public int compareTo(ItemWithDetails another) {
        if (name.equals(another.getName())) {
            return 0;
        }

        if (name == null) {
            return +1;
        } else if (another.getName() == null) {
            return -1;
        }

        return Collator.getInstance().compare(name, another.getName());
    }
}
