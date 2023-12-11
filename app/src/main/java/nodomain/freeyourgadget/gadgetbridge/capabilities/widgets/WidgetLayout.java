/*  Copyright (C) 2023 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.capabilities.widgets;

import androidx.annotation.StringRes;

import nodomain.freeyourgadget.gadgetbridge.R;

public enum WidgetLayout {
    TOP_1_BOT_2(R.string.widget_layout_top_1_bot_2, WidgetType.WIDE, WidgetType.SMALL, WidgetType.SMALL),
    TOP_2_BOT_1(R.string.widget_layout_top_2_bot_1, WidgetType.SMALL, WidgetType.SMALL, WidgetType.SMALL),
    TOP_2_BOT_2(R.string.widget_layout_top_2_bot_2, WidgetType.SMALL, WidgetType.SMALL, WidgetType.SMALL, WidgetType.SMALL),
    SINGLE(R.string.widget_layout_single, WidgetType.TALL),
    TWO(R.string.widget_layout_two, WidgetType.SMALL, WidgetType.SMALL),
    ;

    @StringRes
    private final int name;
    private final WidgetType[] widgetTypes;

    WidgetLayout(final int name, final WidgetType... widgetTypes) {
        this.name = name;
        this.widgetTypes = widgetTypes;
    }

    @StringRes
    public int getName() {
        return name;
    }

    public WidgetType[] getWidgetTypes() {
        return widgetTypes;
    }
}
