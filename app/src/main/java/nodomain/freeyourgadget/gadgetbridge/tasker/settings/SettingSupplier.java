package nodomain.freeyourgadget.gadgetbridge.tasker.settings;

/**
 * Simple setting supplier. Can listen to changes throw {@link SettingListener}.
 *
 * @param <T> Setting
 */
public interface SettingSupplier<T> {

    T get();

    void set(T object);

    boolean isPresent();

    SettingSupplier<T> onChanged(SettingListener<T> onChanged);

    /**
     * Listen to changes in {@link SettingSupplier}.
     *
     * @param <T>
     */
    interface SettingListener<T> {
        void changed(T object);
    }

}
