package nodomain.freeyourgadget.gadgetbridge.util;

import java.util.Collection;

public class ArrayUtils {
    /**
     * Checks the two given arrays for equality, but comparing the second array with a specified
     * subset of the first array.
     *
     * @param first            the array in which to look for the second array
     * @param second           the data to look for inside the first array
     * @param startIndex the start index (inclusive) inside the first array from which to start the comparison
     * @return whether the second byte array is equal to the specified subset of the first byte array
     * @throws IllegalArgumentException when one of the arrays is null or start index/length are wrong
     */
    public static boolean equals(byte[] first, byte[] second, int startIndex) {
        if (first == null) {
            throw new IllegalArgumentException("first must not be null");
        }
        if (second == null) {
            throw new IllegalArgumentException("second must not be null");
        }
        if (startIndex < 0) {
            throw new IllegalArgumentException("startIndex must be >= 0");
        }

        if (second.length + startIndex > first.length) {
            return false;
        }
        for (int i = 0; i < second.length; i++) {
            if (first[startIndex + i] != second[i]) {
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

    /**
     * Returns true if the given byte array starts with the given values
     * @param array the array to check
     * @param values the values which the other array is checked to start with
     * @return
     */
    public static boolean startsWith(byte[] array, byte[] values) {
        return equals(array, values, 0);
    }
}
