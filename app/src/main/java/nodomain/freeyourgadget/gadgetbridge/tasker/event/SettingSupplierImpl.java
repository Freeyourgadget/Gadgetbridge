package nodomain.freeyourgadget.gadgetbridge.tasker.event;

public class SettingSupplierImpl<T> implements SettingSupplier<T> {

    private T object;
    private SettingListener<T> onChanged;

    public SettingSupplierImpl() {
    }

    public SettingSupplierImpl(T object) {
        this.object = object;
    }

    @Override
    public T get() {
        return object;
    }

    @Override
    public void set(T object) {
        this.object = object;
        onChanged.changed(object);
    }

    @Override
    public boolean isPresent() {
        return object != null;
    }

    @Override
    public SettingSupplier<T> onChanged(SettingListener<T> onChanged) {
        this.onChanged = onChanged;
        return this;
    }
}
