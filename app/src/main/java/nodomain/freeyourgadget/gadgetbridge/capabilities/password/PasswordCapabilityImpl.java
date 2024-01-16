/*  Copyright (C) 2022-2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.capabilities.password;

import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler;

public class PasswordCapabilityImpl {
    public static final String PREF_PASSWORD = "pref_password";
    public static final String PREF_PASSWORD_ENABLED = "pref_password_enabled";

    public enum Mode {
        NONE,
        NUMBERS_4_DIGITS_0_TO_9,
        NUMBERS_4_DIGITS_1_TO_4,
        NUMBERS_6,
    }

    public void registerPreferences(final Context context, final Mode mode, final DeviceSpecificSettingsHandler handler) {
        if (mode == Mode.NONE) {
            return;
        }

        final EditTextPreference password = handler.findPreference(PREF_PASSWORD);
        if (password == null) {
            return;
        }

        handler.addPreferenceHandlerFor(PREF_PASSWORD);
        handler.addPreferenceHandlerFor(PREF_PASSWORD_ENABLED);

        switch (mode) {
            case NUMBERS_6:
                password.setSummary(R.string.prefs_password_6_digits_0_to_9_summary);
                break;
            case NUMBERS_4_DIGITS_0_TO_9:
                password.setSummary(R.string.prefs_password_4_digits_0_to_9_summary);
                break;
            case NUMBERS_4_DIGITS_1_TO_4:
                password.setSummary(R.string.prefs_password_4_digits_1_to_4_summary);
                break;
            default:
                break;
        }

        password.setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
            @Override
            public void onBindEditText(@NonNull final EditText editText) {
                final int expectedLength;
                final List<InputFilter> inputFilters = new ArrayList<>();

                switch (mode) {
                    case NUMBERS_6:
                        password.setSummary(R.string.prefs_password_6_digits_0_to_9_summary);
                        editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
                        expectedLength = 6;
                        break;
                    case NUMBERS_4_DIGITS_0_TO_9:
                        password.setSummary(R.string.prefs_password_4_digits_0_to_9_summary);
                        editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
                        expectedLength = 4;
                        break;
                    case NUMBERS_4_DIGITS_1_TO_4:
                        password.setSummary(R.string.prefs_password_4_digits_1_to_4_summary);
                        editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
                        expectedLength = 4;
                        inputFilters.add(new InputFilter_Digits_1to4());
                        break;
                    default:
                        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        expectedLength = -1;
                        break;
                }

                if (expectedLength != -1) {
                    inputFilters.add(new InputFilter.LengthFilter(expectedLength));
                }

                editText.setSelection(editText.getText().length());
                editText.setFilters(inputFilters.toArray(new InputFilter[0]));
                editText.addTextChangedListener(new ExpectedLengthTextWatcher(editText, expectedLength));
            }
        });
    }

    private static class InputFilter_Digits_1to4 implements InputFilter {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            for (int i = start; i < end; i++) {
                if (!Character.isDigit(source.charAt(i))) {
                    return "";
                }

                if (source.charAt(i) < '1' || source.charAt(i) > '4') {
                    return "";
                }
            }
            return null;
        }
    }

    private static class ExpectedLengthTextWatcher implements TextWatcher {
        private final EditText editText;
        private final int expectedLength;

        private ExpectedLengthTextWatcher(final EditText editText, final int expectedLength) {
            this.editText = editText;
            this.expectedLength = expectedLength;
        }

        @Override
        public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
        }

        @Override
        public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
        }

        @Override
        public void afterTextChanged(final Editable editable) {
            editText.getRootView().findViewById(android.R.id.button1)
                    .setEnabled(expectedLength == -1 || editable.length() == expectedLength);
        }
    }
}
