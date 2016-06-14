package nodomain.freeyourgadget.gadgetbridge.activities.appmanager;

public class AppManagerFragmentCache extends AbstractAppManagerFragment {
    @Override
    public void refreshList() {
        appList.addAll(getCachedApps());
    }
}
