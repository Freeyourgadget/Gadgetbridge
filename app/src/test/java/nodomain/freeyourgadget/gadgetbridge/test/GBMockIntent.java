package nodomain.freeyourgadget.gadgetbridge.test;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GBMockIntent extends Intent {
    private String mAction;
    private final Map<String,Object> extras = new HashMap<>();

    @NonNull
    @Override
    public Intent setAction(String action) {
        mAction = action;
        return this;
    }

    @Override
    public String getAction() {
        return mAction;
    }

    @NonNull
    @Override
    public Intent putExtra(String name, boolean value) {
        extras.put(name, value);
        return this;
    }

    @NonNull
    @Override
    public Intent putExtra(String name, byte value) {
        extras.put(name, value);
        return this;
    }

    @NonNull
    @Override
    public Intent putExtra(String name, char value) {
        extras.put(name, value);
        return this;
    }

    @NonNull
    @Override
    public Intent putExtra(String name, short value) {
        extras.put(name, value);
        return this;
    }

    @NonNull
    @Override
    public Intent putExtra(String name, int value) {
        extras.put(name, value);
        return this;
    }

    @NonNull
    @Override
    public Intent putExtra(String name, long value) {
        extras.put(name, value);
        return this;
    }

    @NonNull
    @Override
    public Intent putExtra(String name, float value) {
        extras.put(name, value);
        return this;
    }

    @NonNull
    @Override
    public Intent putExtra(String name, double value) {
        extras.put(name, value);
        return this;
    }

    @NonNull
    @Override
    public Intent putExtra(String name, String value) {
        extras.put(name, value);
        return this;
    }

    @NonNull
    @Override
    public Intent putExtra(String name, CharSequence value) {
        extras.put(name, value);
        return this;
    }

    @NonNull
    @Override
    public Intent putExtra(String name, Parcelable value) {
        extras.put(name, value);
        return this;
    }

    @NonNull
    @Override
    public Intent putExtra(String name, Parcelable[] value) {
        extras.put(name, value);
        return this;
    }

    @Override
    public Intent putParcelableArrayListExtra(String name, ArrayList<? extends Parcelable> value) {
        extras.put(name, value);
        return this;
    }

    @NonNull
    @Override
    public Intent putIntegerArrayListExtra(String name, ArrayList<Integer> value) {
        extras.put(name, value);
        return this;
    }

    @NonNull
    @Override
    public Intent putStringArrayListExtra(String name, ArrayList<String> value) {
        extras.put(name, value);
        return this;
    }

    @NonNull
    @Override
    public Intent putCharSequenceArrayListExtra(String name, ArrayList<CharSequence> value) {
        extras.put(name, value);
        return this;
    }

    @NonNull
    @Override
    public Intent putExtra(String name, Serializable value) {
        extras.put(name, value);
        return this;
    }

    @NonNull
    @Override
    public Intent putExtra(String name, boolean[] value) {
        extras.put(name, value);
        return this;
    }

    @NonNull
    @Override
    public Intent putExtra(String name, byte[] value) {
        extras.put(name, value);
        return this;
    }

    @NonNull
    @Override
    public Intent putExtra(String name, short[] value) {
        extras.put(name, value);
        return this;
    }

    @NonNull
    @Override
    public Intent putExtra(String name, char[] value) {
        extras.put(name, value);
        return this;
    }

    @NonNull
    @Override
    public Intent putExtra(String name, int[] value) {
        extras.put(name, value);
        return this;
    }

    @NonNull
    @Override
    public Intent putExtra(String name, long[] value) {
        extras.put(name, value);
        return this;
    }

    @NonNull
    @Override
    public Intent putExtra(String name, float[] value) {
        extras.put(name, value);
        return this;
    }

    @NonNull
    @Override
    public Intent putExtra(String name, double[] value) {
        extras.put(name, value);
        return this;
    }

    @NonNull
    @Override
    public Intent putExtra(String name, String[] value) {
        extras.put(name, value);
        return this;
    }

    @NonNull
    @Override
    public Intent putExtra(String name, CharSequence[] value) {
        extras.put(name, value);
        return this;
    }

    @NonNull
    @Override
    public Intent putExtra(String name, Bundle value) {
        extras.put(name, value);
        return this;
    }

    @Override
    public boolean getBooleanExtra(String name, boolean defaultValue) {
        if (extras.containsKey(name)) {
            return (boolean) extras.get(name);
        }
        return defaultValue;
    }

    @Override
    public byte getByteExtra(String name, byte defaultValue) {
        if (extras.containsKey(name)) {
            return (byte) extras.get(name);
        }
        return defaultValue;
    }

    @Override
    public short getShortExtra(String name, short defaultValue) {
        if (extras.containsKey(name)) {
            return (short) extras.get(name);
        }
        return defaultValue;
    }

    @Override
    public char getCharExtra(String name, char defaultValue) {
        if (extras.containsKey(name)) {
            return (char) extras.get(name);
        }
        return defaultValue;
    }

    @Override
    public int getIntExtra(String name, int defaultValue) {
        if (extras.containsKey(name)) {
            return (int) extras.get(name);
        }
        return defaultValue;
    }

    @Override
    public long getLongExtra(String name, long defaultValue) {
        if (extras.containsKey(name)) {
            return (long) extras.get(name);
        }
        return defaultValue;
    }

    @Override
    public float getFloatExtra(String name, float defaultValue) {
        if (extras.containsKey(name)) {
            return (float) extras.get(name);
        }
        return defaultValue;
    }

    @Override
    public double getDoubleExtra(String name, double defaultValue) {
        if (extras.containsKey(name)) {
            return (double) extras.get(name);
        }
        return defaultValue;
    }

    @Override
    public CharSequence getCharSequenceExtra(String name) {
        return (CharSequence) extras.get(name);
    }

    @Override
    public <T extends Parcelable> T getParcelableExtra(String name) {
        return (T) extras.get(name);
    }

    @Override
    public Parcelable[] getParcelableArrayExtra(String name) {
        return (Parcelable[]) extras.get(name);
    }

    @Override
    public <T extends Parcelable> ArrayList<T> getParcelableArrayListExtra(String name) {
        return (ArrayList<T>) extras.get(name);
    }

    @Override
    public Serializable getSerializableExtra(String name) {
        return (Serializable) extras.get(name);
    }

    @Override
    public ArrayList<Integer> getIntegerArrayListExtra(String name) {
        return (ArrayList<Integer>) extras.get(name);
    }

    @Override
    public ArrayList<String> getStringArrayListExtra(String name) {
        return (ArrayList<String>) extras.get(name);
    }

    @Override
    public ArrayList<CharSequence> getCharSequenceArrayListExtra(String name) {
        return (ArrayList<CharSequence>) extras.get(name);
    }

    @Override
    public boolean[] getBooleanArrayExtra(String name) {
        return (boolean[]) extras.get(name);
    }

    @Override
    public byte[] getByteArrayExtra(String name) {
        return (byte[]) extras.get(name);
    }

    @Override
    public short[] getShortArrayExtra(String name) {
        return (short[]) extras.get(name);
    }

    @Override
    public char[] getCharArrayExtra(String name) {
        return (char[]) extras.get(name);
    }

    @Override
    public int[] getIntArrayExtra(String name) {
        return (int[]) extras.get(name);
    }

    @Override
    public long[] getLongArrayExtra(String name) {
        return (long[]) extras.get(name);
    }

    @Override
    public float[] getFloatArrayExtra(String name) {
        return (float[]) extras.get(name);
    }

    @Override
    public double[] getDoubleArrayExtra(String name) {
        return (double[]) extras.get(name);
    }

    @Override
    public String[] getStringArrayExtra(String name) {
        return (String[]) extras.get(name);
    }

    @Override
    public CharSequence[] getCharSequenceArrayExtra(String name) {
        return (CharSequence[]) extras.get(name);
    }

    @Override
    public String getStringExtra(String name) {
        return (String) extras.get(name);
    }

    @Override
    public String toString() {
        return "GBMockIntent: " + mAction;
    }
}
