package nodomain.freeyourgadget.gadgetbridge.tasker.event;

/**
 * Simple supplier.
 *
 * @param <T> Setting
 */
public interface SettingSupplier<T> {

    T get();

    boolean isPresent();

}
