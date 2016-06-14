package nodomain.freeyourgadget.gadgetbridge.activities.appmanager;

public class AppManagerFragmentInstalledApps extends AbstractAppManagerFragment {
    @Override
    public void refreshList() {
        appList.addAll(getSystemApps());
    }
}
