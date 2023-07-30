/** Copyright (C) 2022 Damien Gaignon
 *
 *  This file is part of Gadgetbridge.
 *
 *  Gadgetbridge is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published
 *  by the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Gadgetbridge is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package nodomain.freeyourgadget.gadgetbridge.service.btbr;

/**
 * Callback interface handling io events of a BluetoothSocket.
 * It is the counterpart of GattCallback interface designed for GB.
*/
public interface SocketCallback {

    void onConnectionEstablished();

    /**
     * Read data from InputStream of BluetoothSocket
     *
     * @param data
     */
    void onSocketRead(byte[] data);

}
