package nodomain.freeyourgadget.gadgetbridge.tasker.event;

/**
 * Simple supplier.
 *
 * @param <T> Setting
 */
public interface SettingSupplier<T> {

    T get();

    void set(T object);

    boolean isPresent();

    SettingSupplier<T> onChanged(SettingListener<T> onChanged);

    interface SettingListener<T> {
        void changed(T object);
    }

}
