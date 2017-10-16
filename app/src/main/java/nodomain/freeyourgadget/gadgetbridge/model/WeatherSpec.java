/*  Copyright (C) 2016-2017 Andreas Shimokawa, Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.model;

// FIXME: document me and my fields, including units
public class WeatherSpec {
    public int timestamp;
    public String location;
    public int currentTemp;
    public int currentConditionCode;
    public String currentCondition;
    public int todayMaxTemp;
    public int todayMinTemp;
    public int tomorrowMaxTemp;
    public int tomorrowMinTemp;
    public int tomorrowConditionCode;
}
