package nodomain.freeyourgadget.gadgetbridge.activities.appmanager;

public class AppManagerFragmentInstalledWatchfaces extends AbstractAppManagerFragment {
    @Override
    public void refreshList() {
        appList.addAll(getSystemWatchfaces());
    }
}
