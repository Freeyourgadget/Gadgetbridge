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


import android.view.View;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.ListPreference;
import androidx.preference.ListPreferenceDialogFragmentCompat;
import androidx.preference.Preference;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;


public class DragSortListPreferenceFragment extends ListPreferenceDialogFragmentCompat implements ListPreference.TargetFragment {
	protected DragSortListView mListView;
	protected ArrayAdapter<CharSequence> mAdapter;


	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);

		mListView = (DragSortListView) view.findViewById(android.R.id.list);
		mAdapter = new ArrayAdapter<>(mListView.getContext(),
				R.layout.list_item_checkable, R.id.text);
		mListView.setAdapter(mAdapter);
		// This will drop the item in the new location
		mListView.setDropListener(new DragSortListView.DropListener() {
			@Override
			public void drop(int from, int to) {
				CharSequence item = mAdapter.getItem(from);
				mAdapter.remove(item);
				mAdapter.insert(item, to);
				// Updates checked states
				mListView.moveCheckState(from, to);
			}
		});


		DragSortListPreference dslp = ((DragSortListPreference)getPreference());
		CharSequence[] entries = dslp.getEntries();
		CharSequence[] entryValues = dslp.getEntryValues();
		if (entries == null || entryValues == null
				|| entries.length != entryValues.length) {
			throw new IllegalStateException(
					"SortableListPreference requires an entries array and an entryValues "
							+ "array which are both the same length");
		}

		CharSequence[] restoredValues = ((DragSortListPreference)getPreference()).restoreEntries();
		int i = 0;
		for (CharSequence value : restoredValues) {
			int index = dslp.getValueIndex(value);
			if (index >=0) {
				mAdapter.add(entries[index]);
				Boolean checked = dslp.getEntryChecked().get(value);
				if (checked != null && checked.equals(true)) {
					mListView.setItemChecked(i, true);
				}
			}
			i++;
		}
	}

	@Override
	protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
		// must be empty
	}

	public void onDialogClosed(boolean positiveResult) {
		DragSortListPreference dslp = ((DragSortListPreference)getPreference());

		List<CharSequence> values = new ArrayList<>();

		CharSequence[] entryValues = dslp.getEntryValues();
		if (positiveResult && entryValues != null) {
			for (int i = 0; i < entryValues.length; i++) {
				String val = (String) mAdapter.getItem(i);
				boolean isChecked = mListView.isItemChecked(i);
				if (isChecked) {
					values.add(entryValues[dslp.getValueTitleIndex(val)]);
				}
			}

			String value = DragSortListPreference.join(values);
			dslp.setValueAndEvent(value);
			dslp.persistStringValue(value);
		}

	}

	@Override
	public Preference findPreference(@NonNull CharSequence key) {
		return getPreference();
	}

}
