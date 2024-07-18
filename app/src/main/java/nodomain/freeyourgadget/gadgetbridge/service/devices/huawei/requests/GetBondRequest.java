/*  Copyright (C) 2024 Damien Gaignon, Martin.JM

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiCrypto;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.DeviceConfig;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class GetBondRequest extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(GetBondRequest.class);

    protected String macAddress;

    public GetBondRequest(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = DeviceConfig.id;
        this.commandId = DeviceConfig.Bond.id;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            byte[] iv = paramsProvider.getIv();
            huaweiCrypto = new HuaweiCrypto(paramsProvider.getAuthVersion(), paramsProvider.getAuthAlgo(), paramsProvider.getDeviceSupportType(), paramsProvider.getAuthMode());
            byte[] encryptionKey;
            if (paramsProvider.getAuthMode() == 0x02) { //HiChainLite
                encryptionKey = paramsProvider.getFirstKey();
            } else {
                encryptionKey = huaweiCrypto.createSecretKey(supportProvider.getDeviceMac());
            }
            byte[] key = huaweiCrypto.encryptBondingKey(paramsProvider.getEncryptMethod(), paramsProvider.getSecretKey(), encryptionKey, iv);
            LOG.debug("key: " + GB.hexdump(key));
            return new DeviceConfig.Bond.Request(
                    paramsProvider,
                    supportProvider.getSerial(),
                    key,
                    iv
            ).serialize();
        } catch (HuaweiPacket.CryptoException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException | InvalidKeyException | IllegalBlockSizeException |
                 BadPaddingException e) {
            throw new RequestCreationException(e.toString());
        }
    }

    @Override
    protected void processResponse() {
        LOG.debug("handle Bond");
    }
}
