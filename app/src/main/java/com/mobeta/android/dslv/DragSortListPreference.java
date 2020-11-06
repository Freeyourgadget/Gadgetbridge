/*
 * Sortable Preference ListView. Allows for sorting items in a view,
 * and selecting which ones to use.
 *
 * Example Usage (In a preference file)
 *
 * 	<com.mobeta.android.demodslv.SortableListPreference
 * 		android:defaultValue="@array/pref_name_defaults"
 * 		android:entries="@array/pref_name_titles"
 * 		android:entryValues="@array/pref_name_values"
 * 		android:key="name_order"
 * 		android:persistent="true"
 * 		android:title="@string/pref_name_selection" />
 *
 * Original Source: https://github.com/kd7uiy/drag-sort-listview
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 The Making of a Ham, http://www.kd7uiy.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Code snippets copied from the following sources:
 * https://gist.github.com/cardil/4754571
 *
 *
 */

package com.mobeta.android.dslv;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.preference.ListPreference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;


public class DragSortListPreference extends ListPreference {
    public HashMap<CharSequence, Boolean> getEntryChecked() {
        return entryChecked;
    }

    private final HashMap<CharSequence, Boolean> entryChecked;

    public DragSortListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.sort_list_array_dialog_preference);
        entryChecked = new HashMap<>();
    }

    public static CharSequence[] decodeValue(String input) {
        if (input == null) {
            return null;
        }
        if (input.equals("")) {
            return new CharSequence[0];
        }
        return input.split(",");
    }


    void setValueAndEvent(String value) {
        if (callChangeListener(decodeValue(value))) {
            setValue(value);
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray typedArray, int index) {
        return typedArray.getTextArray(index);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue,
                                     Object rawDefaultValue) {
        String value;
        CharSequence[] defaultValue;
        if (rawDefaultValue == null) {
            defaultValue = new CharSequence[0];
        } else {
            defaultValue = (CharSequence[]) rawDefaultValue;
        }
        List<CharSequence> joined = Arrays.asList(defaultValue);
        String joinedDefaultValue = join(joined);
        if (restoreValue) {
            value = getPersistedString(joinedDefaultValue);
        } else {
            value = joinedDefaultValue;
        }

        setValueAndEvent(value);
    }


    public int getValueIndex(CharSequence item) {
        CharSequence[] entryValues = getEntryValues();
        for (int i = 0; i < entryValues.length; i++) {
            if (entryValues[i].equals(item)) {
                return i;
            }
        }
        return -1;
    }

    CharSequence[] restoreEntries() {

        ArrayList<CharSequence> orderedList = new ArrayList<>();

        // Initially populated with all of the values in the determined list.
        CharSequence[] values = decodeValue(getValue());
        for (CharSequence value : values) {
            orderedList.add(value);
            entryChecked.put(value, true);
        }

        // This loop sets the default states, and adds to the name list if not
        // on the list.
        for (CharSequence value : getEntryValues()) {
            if (!orderedList.contains(value)) {
                orderedList.add(value);
                entryChecked.put(value, false);
            }
        }

        return orderedList.toArray(new CharSequence[0]);
    }

    public int getValueTitleIndex(CharSequence item) {
        CharSequence[] entries = getEntries();
        for (int i = 0; i < entries.length; i++) {
            if (entries[i].equals(item)) {
                return i;
            }
        }
        throw new IllegalStateException(item + " not found in value title list");
    }


    /**
     * Joins array of object to single string by separator
     * <p>
     * Credits to kurellajunior on this post
     * http://snippets.dzone.com/posts/show/91
     *
     * @param iterable any kind of iterable ex.: <code>["a", "b", "c"]</code>
     * @return joined string ex.: <code>"a,b,c"</code>
     */
    protected static String join(Iterable<?> iterable) {
        Iterator<?> oIter;
        if (iterable == null || (!(oIter = iterable.iterator()).hasNext()))
            return "";
        StringBuilder oBuilder = new StringBuilder(String.valueOf(oIter.next()));
        while (oIter.hasNext())
            oBuilder.append(",").append(oIter.next());
        return oBuilder.toString();
    }

    void persistStringValue(String value) {
        persistString(value);
    }
}
