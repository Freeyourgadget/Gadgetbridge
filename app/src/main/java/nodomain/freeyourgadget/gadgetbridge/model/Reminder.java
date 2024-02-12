/*  Copyright (C) 2021-2024 José Rebelo

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.model;

import java.io.Serializable;
import java.util.Date;

public interface Reminder extends Serializable {
    /**
     * The {@link android.os.Bundle} name for transferring parceled reminders.
     */
    String EXTRA_REMINDER = "reminder";

    int ONCE = 0;
    int EVERY_DAY = 1;
    int EVERY_WEEK = 2;
    int EVERY_MONTH = 3;
    int EVERY_YEAR = 4;

    String getReminderId();
    String getMessage();
    Date getDate();
    int getRepetition();
}
