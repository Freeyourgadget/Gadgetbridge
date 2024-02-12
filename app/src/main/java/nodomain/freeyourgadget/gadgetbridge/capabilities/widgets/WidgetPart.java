/*  Copyright (C) 2023-2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.capabilities.widgets;

import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A widget part is a single widget in a widget screen.
 */
public class WidgetPart implements Serializable {
    // Null when not selected
    @Nullable
    private String id;

    // The human-readable part name
    private String name;

    private WidgetType type;

    // Null if it has no specific subtype
    @Nullable
    private WidgetPartSubtype subtype;

    // The list of subtypes supported by this part, if any
    private final List<WidgetPartSubtype> supportedSubtypes = new ArrayList<>();

    public WidgetPart(@Nullable final String id, final String name, final WidgetType type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    @Nullable
    public String getId() {
        return id;
    }

    public void setId(@Nullable final String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        if (subtype != null) {
            return String.format(Locale.ROOT, "%s (%s)", name, subtype.getName());
        }

        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public WidgetType getType() {
        return type;
    }

    public void setType(final WidgetType type) {
        this.type = type;
    }

    @Nullable
    public WidgetPartSubtype getSubtype() {
        return subtype;
    }

    public void setSubtype(@Nullable final WidgetPartSubtype subtype) {
        this.subtype = subtype;
    }

    public List<WidgetPartSubtype> getSupportedSubtypes() {
        return supportedSubtypes;
    }
}
