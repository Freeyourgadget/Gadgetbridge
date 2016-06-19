package nodomain.freeyourgadget.gadgetbridge.model;

import java.util.Date;

public interface ValidByDate {
    Date getValidFromUTC();
    Date getValidToUTC();
}
