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
import java.util.List;

public class WidgetScreen implements Serializable {
    public static final String EXTRA_WIDGET_SCREEN = "widget_screen";

    // Null when creating a new screen
    @Nullable
    private String id;
    private WidgetLayout layout;

    // The list of parts must match what the WidgetLayout expects
    private List<WidgetPart> parts;

    public WidgetScreen(@Nullable final String id, final WidgetLayout layout, final List<WidgetPart> parts) {
        this.id = id;
        this.layout = layout;
        this.parts = parts;
    }

    @Nullable
    public String getId() {
        return id;
    }

    public void setId(@Nullable final String id) {
        this.id = id;
    }

    public WidgetLayout getLayout() {
        return layout;
    }

    public void setLayout(final WidgetLayout layout) {
        this.layout = layout;
    }

    public List<WidgetPart> getParts() {
        return parts;
    }

    public void setParts(final List<WidgetPart> parts) {
        this.parts = parts;
    }
}
