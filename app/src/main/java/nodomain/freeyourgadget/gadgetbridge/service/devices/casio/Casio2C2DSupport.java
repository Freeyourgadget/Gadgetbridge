/*  Copyright (C) 2016-2023 Andreas Böhler, Andreas Shimokawa, Carsten
    Pfeiffer, Sebastian Kranz, Johannes Krude

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.casio;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.threeten.bp.ZonedDateTime;

import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.devices.casio.CasioConstants;


// this class is for those Casio watches which request reads on the 2C characteristic and write on the 2D characteristic

public abstract class Casio2C2DSupport extends CasioSupport {

    public Casio2C2DSupport(Logger logger) {
        super(logger);
    }

    public void writeAllFeatures(TransactionBuilder builder, byte[] arr) {
        builder.write(getCharacteristic(CasioConstants.CASIO_ALL_FEATURES_CHARACTERISTIC_UUID), arr);
    }

    public void writeAllFeaturesRequest(TransactionBuilder builder, byte[] arr) {
        builder.write(getCharacteristic(CasioConstants.CASIO_READ_REQUEST_FOR_ALL_FEATURES_CHARACTERISTIC_UUID), arr);
    }

    public void writeCurrentTime(TransactionBuilder builder, ZonedDateTime time) {
        byte[] arr = new byte[11];
        arr[0] = CasioConstants.characteristicToByte.get("CASIO_CURRENT_TIME");
        byte[] tmp = prepareCurrentTime(time);
        System.arraycopy(tmp, 0, arr, 1, 10);

        writeAllFeatures(builder, arr);
    }

}
