/*  Copyright (C) 2016-2024 Andreas Shimokawa, Carsten Pfeiffer, Petr Kadlec,
    Petr Vaněk

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

    /**
     * Converts an array to string representation
     *
     * @param array the array to convert
     * @return
     */
    public static String arrayToString(byte[] array) {
        StringBuilder stringBuilder = new StringBuilder();
        for (byte i : array) {
            stringBuilder.append(String.format("0x%02X ", i));
        }
        return stringBuilder.toString();
    }


    /**
     * Finds a value in a byte array
     * @param value the value to find in the array
     * @param array the array to search
     * @return index of the first occurrence of the value in the array, -1 if the array does not contain the value
     */
    public static int indexOf(byte value, byte[] array) {
        return indexOf(value, array, 0, array.length);
    }

    /**
     * Finds a value in a byte array
     * @param value  the value to find in the array
     * @param array  the array to search
     * @param offset initial offset in the array to be searched (0 = start at the beginning)
     * @param size   number of bytes to search beginning at the given offset
     * @return index of the first occurrence of the value in the array (from the beginning of the array, i.e. not from
     * the offset position), -1 if the array does not contain the value
     */
    public static int indexOf(byte value, byte[] array, int offset, int size) {
        for (int i = offset; i < offset + size; ++i) {
            if (array[i] == value) return i;
        }
        return -1;
    /**
     * Check if a byte array contains all zeros
     * @param array The array to check
     * @param startIndex The starting position
     * @param length Number of elements to check
     * @return true if all checked elements were == 0, false otherwise
     */
    public static boolean isAllZeros(byte[] array, int startIndex, int length)
    {
        for(int i = startIndex; i < startIndex + length; i++)
        {
            if (array[i] != 0)
                return false;
        }
        return true;
    }

    /**
     * Check if a byte array contains all zeros
     * @param array The array to check
     * @return true if all checked elements were == 0, false otherwise
     */
    public static boolean isAllZeros(byte[] array)
    {
        return isAllZeros(array, 0, array.length);
    }
}
