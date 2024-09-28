/*  Copyright (C) 2024 José Rebelo

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

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitImporter;

/**
 * A small wrapper for a JSONObject, with helper methods to add activity summary data in the format
 * Gadgetbridge expects.
 */
public class ActivitySummaryData extends JSONObject {
    private static final Logger LOG = LoggerFactory.getLogger(FitImporter.class);

    public void add(final String key, final float value, final String unit) {
        add(null, key, value, unit);
    }

    public void add(final String key, final double value, final String unit) {
        add(null, key, value, unit);
    }

    public void add(final String key, final String value) {
        add(null, key, value);
    }

    public void add(final String group, final String key, final double value, final String unit) {
        if (value > 0) {
            try {
                final JSONObject innerData = new JSONObject();
                if (group != null) {
                    innerData.put("group", group);
                }
                innerData.put("value", value);
                innerData.put("unit", unit);
                put(key, innerData);
            } catch (final JSONException e) {
                LOG.error("This should never happen", e);
            }
        }
    }

    public void add(final String group, final String key, final String value) {
        if (key != null && !key.isEmpty() && value != null && !value.isEmpty()) {
            try {
                final JSONObject innerData = new JSONObject();
                if (group != null) {
                    innerData.put("group", group);
                }
                innerData.put("value", value);
                innerData.put("unit", "string");
                put(key, innerData);
            } catch (final JSONException e) {
                LOG.error("This should never happen", e);
            }
        }
    }
}
