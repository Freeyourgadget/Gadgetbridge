package nodomain.freeyourgadget.gadgetbridge.tasker.settings;

import android.content.SharedPreferences;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;

public final class PreferenceSettingSupplier<T> implements SettingSupplier<T> {

    private String key;
    private Class<T> type;
    private SharedPreferences sharedPreferences;

    public PreferenceSettingSupplier(String key, Type type) {
        if (key == null || key.isEmpty()) {
            throw new SettingsException("Undefined", "Key can not be empty!");
        }
        if (type == null ||
                !Boolean.class.isAssignableFrom(type.getClass()) &&
                !Long.class.isAssignableFrom(type.getClass()) &&
                !Integer.class.isAssignableFrom(type.getClass()) &&
                !Float.class.isAssignableFrom(type.getClass()) &&
                !String.class.isAssignableFrom(type.getClass()) &&
                (Set.class.isAssignableFrom(type.getClass()) &&
                        !(((ParameterizedType) type).getActualTypeArguments()[0] instanceof Class) &&
                        !String.class.isAssignableFrom((Class<?>) ((ParameterizedType) type).getActualTypeArguments()[0]))) {
            throw new SettingsException(key, "Type is must be one of [types={Boolean,Long,Integer,Float,String, Set<String>}]");
        }
        this.key = key;
        this.type = (Class<T>) ((type instanceof Class) ? type : ((ParameterizedType) type).getRawType());
        this.sharedPreferences = GBApplication.getPrefs().getPreferences();
    }

    @Override
    public T get() {
        if (!isPresent()) {
            return null;
        }
        if (Boolean.class.isAssignableFrom(type)) {
            return (T) Boolean.valueOf(sharedPreferences.getBoolean(key, false));
        }
        if (Long.class.isAssignableFrom(type)) {
            return (T) Long.valueOf(sharedPreferences.getLong(key, 0L));
        }
        if (Integer.class.isAssignableFrom(type)) {
            return (T) Integer.valueOf(sharedPreferences.getInt(key, 0));
        }
        if (Float.class.isAssignableFrom(type)) {
            return (T) Float.valueOf(sharedPreferences.getFloat(key, 0F));
        }
        if (Set.class.isAssignableFrom(type)) {
            return (T) sharedPreferences.getStringSet(key, new HashSet<String>());
        }
        return (T) sharedPreferences.getString(key, "");
    }

    @Override
    public void set(T object) {
        if (object == null) {
            sharedPreferences.edit().remove(key).commit();
        }
        if (Boolean.class.isAssignableFrom(type)) {
            sharedPreferences.edit().putBoolean(key, (Boolean) object).commit();
        }
        if (Long.class.isAssignableFrom(type)) {
            sharedPreferences.edit().putLong(key, (Long) object).commit();
        }
        if (Integer.class.isAssignableFrom(type)) {
            sharedPreferences.edit().putInt(key, (Integer) object).commit();
        }
        if (Float.class.isAssignableFrom(type)) {
            sharedPreferences.edit().putFloat(key, (Float) object).commit();
        }
        if (Set.class.isAssignableFrom(type)) {
            sharedPreferences.edit().putStringSet(key, (Set<String>) object).commit();
        }
        if (String.class.isAssignableFrom(type)) {
            sharedPreferences.edit().putString(key, (String) object).commit();
        }
    }

    @Override
    public boolean isPresent() {
        return sharedPreferences.contains(key);
    }

    @Override
    public SettingSupplier<T> onChanged(final SettingListener<T> onChanged) {
        sharedPreferences.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                onChanged.changed(get());
            }
        });
        return this;
    }

}
