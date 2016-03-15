package nodomain.freeyourgadget.gadgetbridge.model;

import android.os.Parcelable;

public interface ItemWithDetails extends Parcelable, Comparable<ItemWithDetails> {
    String getName();

    String getDetails();

    int getIcon();

    /**
     * Equality is based on #getName() only.
     * @param other
     */
    boolean equals(Object other);
}
