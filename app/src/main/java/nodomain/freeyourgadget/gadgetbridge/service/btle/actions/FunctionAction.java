/*  Copyright (C) 2023 Johannes Krude

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
package nodomain.freeyourgadget.gadgetbridge.service.btle.actions;

import android.bluetooth.BluetoothGatt;

import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.PlainAction;

/**
 * Invokes the given function
 */
public class FunctionAction extends PlainAction {

    public interface Function {
        public void apply(BluetoothGatt gatt);
    }
    private Function function;

    public FunctionAction(Function function) {
        this.function = function;
    }

    @Override
    public boolean run(BluetoothGatt gatt) {
        function.apply(gatt);
        return true;
    }

    @Override
    public boolean expectsResult() {
        return false;
    }
}
