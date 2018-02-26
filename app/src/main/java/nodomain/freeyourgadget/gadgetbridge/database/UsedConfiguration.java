/*  Copyright (C) 2015-2018 Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.database;

/**
 * Contains the configuration used for particular activity samples.
 */
public class UsedConfiguration {
    String fwVersion;
    String userName;
    short userWeight;
    short userSize;
    // ...
    int usedFrom; // timestamp
    int usedUntil; // timestamp
    short sleepGoal; // minutes
    short stepsGoal;
}
