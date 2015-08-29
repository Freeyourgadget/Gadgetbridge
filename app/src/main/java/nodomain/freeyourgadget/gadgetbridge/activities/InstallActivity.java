package nodomain.freeyourgadget.gadgetbridge.activities;

import nodomain.freeyourgadget.gadgetbridge.model.ItemWithDetails;

public interface InstallActivity {
    void setInfoText(String text);
    void setInstallEnabled(boolean enable);

    void clearInstallItems();

    void setInstallItem(ItemWithDetails item);
}
