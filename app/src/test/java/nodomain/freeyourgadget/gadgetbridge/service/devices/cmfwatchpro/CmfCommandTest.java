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
package nodomain.freeyourgadget.gadgetbridge.service.devices.cmfwatchpro;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class CmfCommandTest {
    @Test
    public void commandEnumCheckNoOverlap() {
        // Ensure that no 2 commands overlap in codes
        final Map<String, Boolean> knownCodes = new HashMap<>();
        for (final CmfCommand cmd : CmfCommand.values()) {
            final Boolean existingCode = knownCodes.put(
                    String.format("cmd1=0x%04x cmd2=0x%04x", cmd.getCmd1(), cmd.getCmd2()),
                    true
            );
            assertNull("Commands with overlapping codes", existingCode);
        }
    }
}
