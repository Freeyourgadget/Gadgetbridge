/*  Copyright (C) 2020-2024 Andreas Shimokawa

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

import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;

public interface ActivitySummaryParser {
    /**
     * Re-parse an existing {@link BaseActivitySummary}, updating it from the existing binary data.
     *
     * @param summary    the existing {@link BaseActivitySummary}. It's not guaranteed that it
     *                   contains any raw binary data.
     * @param forDetails whether the parsing is for the details page. If this is false, the parser
     *                   should avoid slow operations such as reading and parsing raw files from
     *                   storage.
     * @return the update {@link BaseActivitySummary}
     */
    BaseActivitySummary parseBinaryData(BaseActivitySummary summary, final boolean forDetails);
}
