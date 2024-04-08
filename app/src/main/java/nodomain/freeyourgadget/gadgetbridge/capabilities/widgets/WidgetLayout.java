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

import androidx.annotation.StringRes;

import nodomain.freeyourgadget.gadgetbridge.R;

public enum WidgetLayout {
    // Square screen layouts, 2x2
    TOP_1_BOT_2(R.string.widget_layout_top_1_bot_2, WidgetType.WIDE, WidgetType.SMALL, WidgetType.SMALL),
    TOP_2_BOT_1(R.string.widget_layout_top_2_bot_1, WidgetType.SMALL, WidgetType.SMALL, WidgetType.SMALL),
    TOP_2_BOT_2(R.string.widget_layout_top_2_bot_2, WidgetType.SMALL, WidgetType.SMALL, WidgetType.SMALL, WidgetType.SMALL),
    TWO_BY_TWO_SINGLE(R.string.widget_layout_single, WidgetType.LARGE),

    // Narrow screen layouts, 2x1
    ONE_BY_TWO_SINGLE(R.string.widget_layout_single, WidgetType.TALL),
    TWO(R.string.widget_layout_two, WidgetType.SMALL, WidgetType.SMALL),

    // Portrait 2x3 screen layouts
    TOP_2_BOT_2X2(R.string.widget_layout_top_2_bot_1, WidgetType.SMALL, WidgetType.SMALL, WidgetType.LARGE),
    TOP_2X2_BOT_2(R.string.widget_layout_top_1_bot_2, WidgetType.LARGE, WidgetType.SMALL, WidgetType.SMALL),
    TOP_1_BOT_2X2(R.string.widget_layout_top_wide_bot_large, WidgetType.WIDE, WidgetType.LARGE),
    TOP_2X2_BOT_1(R.string.widget_layout_top_large_bot_wide, WidgetType.LARGE, WidgetType.WIDE),
    TWO_BY_THREE_SINGLE(R.string.widget_layout_single, WidgetType.PORTRAIT_LARGE),
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
