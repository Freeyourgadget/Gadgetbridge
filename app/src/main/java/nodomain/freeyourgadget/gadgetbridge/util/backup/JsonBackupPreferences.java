/*  Copyright (C) 2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.util.backup;

import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;

public class JsonBackupPreferences {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapterFactory(getTypeAdapterFactory())
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    private final Map<String, PreferenceValue> preferences;

    private static final String BOOLEAN = "Boolean";
    private static final String FLOAT = "Float";
    private static final String INTEGER = "Integer";
    private static final String LONG = "Long";
    private static final String STRING = "String";
    private static final String HASHSET = "HashSet";

    public JsonBackupPreferences(final Map<String, PreferenceValue> preferences) {
        this.preferences = preferences;
    }

    public static JsonBackupPreferences fromJson(final InputStream inputStream) {
        return GSON.fromJson(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8),
                JsonBackupPreferences.class
        );
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    /**
     * @noinspection BooleanMethodIsAlwaysInverted
     */
    public boolean importInto(final SharedPreferences sharedPreferences) {
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        for (final Map.Entry<String, PreferenceValue> e : preferences.entrySet()) {
            e.getValue().put(editor, e.getKey());
        }
        return editor.commit();
    }

    public static JsonBackupPreferences exportFrom(final SharedPreferences sharedPreferences) {
        final Map<String, PreferenceValue> values = new HashMap<>();

        for (final Map.Entry<String, ?> entry : sharedPreferences.getAll().entrySet()) {
            final String key = entry.getKey();

            final Object valueObject = entry.getValue();
            // Skip this entry if the value is null;
            if (valueObject == null) continue;

            final String valueType = valueObject.getClass().getSimpleName();

            if (BOOLEAN.equals(valueType)) {
                values.put(key, new BooleanPreferenceValue((Boolean) valueObject));
            } else if (FLOAT.equals(valueType)) {
                values.put(key, new FloatPreferenceValue((Float) valueObject));
            } else if (INTEGER.equals(valueType)) {
                values.put(key, new IntegerPreferenceValue((Integer) valueObject));
            } else if (LONG.equals(valueType)) {
                values.put(key, new LongPreferenceValue((Long) valueObject));
            } else if (STRING.equals(valueType)) {
                values.put(key, new StringPreferenceValue((String) valueObject));
            } else if (HASHSET.equals(valueType)) {
                values.put(key, new StringSetPreferenceValue((HashSet) valueObject));
            } else {
                throw new IllegalArgumentException("Unknown preference type " + valueType);
            }
        }

        return new JsonBackupPreferences(values);
    }

    public interface PreferenceValue {
        void put(final SharedPreferences.Editor editor, final String key);
    }

    public static class BooleanPreferenceValue implements PreferenceValue {
        private final boolean value;

        public BooleanPreferenceValue(final boolean value) {
            this.value = value;
        }

        @Override
        public void put(final SharedPreferences.Editor editor, final String key) {
            editor.putBoolean(key, value);
        }
    }

    public static class FloatPreferenceValue implements PreferenceValue {
        private final float value;

        public FloatPreferenceValue(final float value) {
            this.value = value;
        }

        @Override
        public void put(final SharedPreferences.Editor editor, final String key) {
            editor.putFloat(key, value);
        }
    }

    public static class IntegerPreferenceValue implements PreferenceValue {
        private final int value;

        public IntegerPreferenceValue(final int value) {
            this.value = value;
        }

        @Override
        public void put(final SharedPreferences.Editor editor, final String key) {
            editor.putInt(key, value);
        }
    }

    public static class LongPreferenceValue implements PreferenceValue {
        private final long value;

        public LongPreferenceValue(final long value) {
            this.value = value;
        }

        @Override
        public void put(final SharedPreferences.Editor editor, final String key) {
            editor.putLong(key, value);
        }
    }

    public static class StringPreferenceValue implements PreferenceValue {
        private final String value;

        public StringPreferenceValue(final String value) {
            this.value = value;
        }

        @Override
        public void put(final SharedPreferences.Editor editor, final String key) {
            editor.putString(key, value);
        }
    }

    public static class StringSetPreferenceValue implements PreferenceValue {
        private final Set<String> value;

        public StringSetPreferenceValue(final Set<String> value) {
            this.value = value;
        }

        @Override
        public void put(final SharedPreferences.Editor editor, final String key) {
            editor.putStringSet(key, new HashSet<>(value));
        }
    }

    public static TypeAdapterFactory getTypeAdapterFactory() {
        return RuntimeTypeAdapterFactory
                .of(PreferenceValue.class, "type")
                .registerSubtype(BooleanPreferenceValue.class, BOOLEAN)
                .registerSubtype(FloatPreferenceValue.class, FLOAT)
                .registerSubtype(IntegerPreferenceValue.class, INTEGER)
                .registerSubtype(LongPreferenceValue.class, LONG)
                .registerSubtype(StringPreferenceValue.class, STRING)
                .registerSubtype(StringSetPreferenceValue.class, HASHSET)
                .recognizeSubtypes();
    }
}
