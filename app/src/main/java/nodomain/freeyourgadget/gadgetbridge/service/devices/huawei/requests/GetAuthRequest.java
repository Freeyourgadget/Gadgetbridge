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

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiCrypto;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.DeviceConfig;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class GetAuthRequest extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(GetAuthRequest.class);

    protected final byte[] clientNonce;
    protected short authVersion;
    protected byte authAlgo;
    protected byte[] doubleNonce;
    protected byte[] key = null;
    protected byte deviceSupportType;
    protected byte authMode;

    public GetAuthRequest(HuaweiSupportProvider support,
            Request linkParamsReq) {
        super(support);
        this.serviceId = DeviceConfig.id;
        this.commandId = DeviceConfig.Auth.id;
        this.clientNonce = HuaweiCrypto.generateNonce();
        doubleNonce = ByteBuffer.allocate(32)
                .put(((GetLinkParamsRequest)linkParamsReq).serverNonce)
                .put(clientNonce)
                .array();
        this.authVersion = paramsProvider.getAuthVersion();
        this.authAlgo = paramsProvider.getAuthAlgo();
        this.deviceSupportType = paramsProvider.getDeviceSupportType();
        this.authMode = paramsProvider.getAuthMode();
        this.huaweiCrypto = new HuaweiCrypto(authVersion, authAlgo, deviceSupportType, authMode);
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        byte[] nonce;

        try {
            if (authMode == 0x02) {
                key = paramsProvider.getPinCode();
                if (authVersion == 0x02)
                    key = paramsProvider.getSecretKey();
            }
            nonce = ByteBuffer.allocate(18)
                    .putShort(authVersion)
                    .put(clientNonce)
                    .array();
            ByteBuffer digestedChallenge = ByteBuffer.wrap(huaweiCrypto.digestChallenge(key, doubleNonce));
            byte[] challenge = new byte[0x20];
            digestedChallenge.get(challenge, 0x00, 0x20);
            LOG.debug("challenge: " + GB.hexdump(challenge));
            byte[] firstKey = new byte[0x10];
            digestedChallenge.get(firstKey, 0x00, 0x10);
            paramsProvider.setFirstKey(firstKey);
            if (challenge == null)
                throw new RequestCreationException("Challenge null");
            return new DeviceConfig.Auth.Request(paramsProvider, challenge, nonce).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        } catch (NoSuchAlgorithmException | InvalidKeyException | InvalidKeySpecException | UnsupportedEncodingException e) {
            throw new RequestCreationException("Digest exception", e);
        }
    }

    @Override
    protected void processResponse() throws ResponseParseException {
        LOG.debug("handle Auth");

        if (!(receivedPacket instanceof DeviceConfig.Auth.Response))
            throw new ResponseTypeMismatchException(receivedPacket, DeviceConfig.Auth.Response.class);

        try {
            ByteBuffer digestedChallenge = ByteBuffer.wrap(huaweiCrypto.digestResponse(key, doubleNonce));
            byte[] expectedAnswer = new byte[0x20];
            digestedChallenge.get(expectedAnswer, 0x00, 0x20);
            LOG.debug("challenge: " + GB.hexdump(expectedAnswer));
            if (expectedAnswer == null)
                throw new ResponseParseException("Challenge null");
            byte[] actualAnswer = ((DeviceConfig.Auth.Response) receivedPacket).challengeResponse;
            if (!Arrays.equals(expectedAnswer, actualAnswer)) {
                throw new ResponseParseException("Challenge answer mismatch : "
                        + StringUtils.bytesToHex(actualAnswer)
                        + " != "
                        + StringUtils.bytesToHex(expectedAnswer)
                );
            }
        } catch (NoSuchAlgorithmException | InvalidKeyException | InvalidKeySpecException | UnsupportedEncodingException e) {
            throw new ResponseParseException("Challenge response digest exception");
        }
    }
}
