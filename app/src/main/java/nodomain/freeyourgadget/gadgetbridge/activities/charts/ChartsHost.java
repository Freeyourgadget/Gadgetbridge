/*  Copyright (C) 2015-2018 Andreas Shimokawa, Carsten Pfeiffer

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.view.ViewGroup;

import java.util.Date;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public interface ChartsHost {
    String DATE_PREV = ChartsActivity.class.getName().concat(".date_prev");
    String DATE_NEXT = ChartsActivity.class.getName().concat(".date_next");
    String REFRESH = ChartsActivity.class.getName().concat(".refresh");

    GBDevice getDevice();

    void setStartDate(Date startDate);

    void setEndDate(Date endDate);

    Date getStartDate();

    Date getEndDate();

    void setDateInfo(String dateInfo);

    ViewGroup getDateBar();
}
