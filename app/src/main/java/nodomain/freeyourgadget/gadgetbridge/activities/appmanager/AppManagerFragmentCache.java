package nodomain.freeyourgadget.gadgetbridge.activities.appmanager;

public class AppManagerFragmentCache extends AbstractAppManagerFragment {
    @Override
    public void refreshList() {
        appList.clear();
        appList.addAll(getCachedApps(null));
    }

    @Override
    public String getSortFilename() {
        return "pbwcacheorder.txt";
    }
}
