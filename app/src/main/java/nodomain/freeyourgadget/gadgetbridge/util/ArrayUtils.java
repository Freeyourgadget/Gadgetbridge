package nodomain.freeyourgadget.gadgetbridge.util;

import java.util.Collection;

public class ArrayUtils {
    /**
     * Checks the two given arrays for equality, but comparing only a subset of the second
     * array with the whole first array.
     *
     * @param first            the whole array to compare against
     * @param second           the array, of which a subset shall be compared against the whole first array
     * @param secondStartIndex the start index (inclusive) of the second array from which to start the comparison
     * @param secondEndIndex   the end index (exclusive) of the second array until which to compare
     * @return whether the first byte array is equal to the specified subset of the second byte array
     * @throws IllegalArgumentException when one of the arrays is null or start and end index are wrong
     */
    public static boolean equals(byte[] first, byte[] second, int secondStartIndex, int secondEndIndex) {
        if (first == null) {
            throw new IllegalArgumentException("first must not be null");
        }
        if (second == null) {
            throw new IllegalArgumentException("second must not be null");
        }
        if (secondStartIndex >= secondEndIndex) {
            throw new IllegalArgumentException("secondStartIndex must be smaller than secondEndIndex");
        }
        if (second.length < secondEndIndex) {
            throw new IllegalArgumentException("secondStartIndex must be smaller than secondEndIndex");
        }
        int len = secondEndIndex - secondStartIndex;
        if (first.length != len) {
            return false;
        }
        for (int i = 0; i < len; i++) {
            if (first[i] != second[secondStartIndex + i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Converts a collection of Integer values to an int[] array.
     * @param values
     * @return null if the given collection is null, otherwise an array of the same size as the collection
     * @throws NullPointerException when an element of the collection is null
     */
    public static int[] toIntArray(Collection<Integer> values) {
        if (values == null) {
            return null;
        }
        int i = 0;
        int[] result = new int[values.size()];
        for (Integer value : values) {
            result[i++] = value;
        }
        return result;
    }
}
