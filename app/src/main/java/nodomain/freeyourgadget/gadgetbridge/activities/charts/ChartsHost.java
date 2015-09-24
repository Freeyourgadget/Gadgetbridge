package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import java.util.Date;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public interface ChartsHost {
    static final String DATE_PREV = ChartsActivity.class.getName().concat(".date_prev");
    static final String DATE_NEXT = ChartsActivity.class.getName().concat(".date_next");
    static final String REFRESH = ChartsActivity.class.getName().concat(".refresh");

    GBDevice getDevice();

    void setStartDate(Date startDate);

    void setEndDate(Date endDate);

    Date getStartDate();

    Date getEndDate();

    void setDateInfo(String dateInfo);
}
