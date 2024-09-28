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

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.DeviceConfig;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.DeviceConfig.HiChain;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;
import nodomain.freeyourgadget.gadgetbridge.util.CryptoUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class GetHiChainRequest extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(GetHiChainRequest.class);
    // Attributs used along all operation
    private HiChain.Request req = null;
    private byte operationCode = 0x02;
    private byte step;
    private byte[] authIdSelf = null;
    private byte[] authIdPeer = null;
    private byte[] randSelf = null;
    private byte[] randPeer = null;
    private long requestId = 0x00;
    private JSONObject json = null;
    private byte[] sessionKey = null;
    // Attributs used once
    private byte[] seed = null;
    private byte[] challenge = null;
    private byte[] psk = null;

    // The user needs to confirm the pairing request on the device itself.
    private final int firstAuthenticateTimeout = 30 * 1000;
    private final int authenticateTimeout = 5000;

    public GetHiChainRequest(HuaweiSupportProvider support, boolean firstConnection) {
        super(support);
        this.serviceId = DeviceConfig.id;
        this.commandId = HiChain.id;
        if (firstConnection) {
            setupTimeoutUntilNext(firstAuthenticateTimeout);
            operationCode = 0x01;
        } else {
            setupTimeoutUntilNext(authenticateTimeout);
        }
        this.step = 0x01;
    }

    public GetHiChainRequest(Request prevReq) {
        super(prevReq.supportProvider);
        this.serviceId = DeviceConfig.id;
        this.commandId = HiChain.id;
        GetHiChainRequest hcReq = (GetHiChainRequest)prevReq;
        this.req = hcReq.req;
        this.requestId = (Long)hcReq.requestId;
        this.operationCode = (byte)hcReq.operationCode;
        this.step = (byte)hcReq.step;
        this.authIdSelf = hcReq.authIdSelf;
        this.authIdPeer = hcReq.authIdPeer;
        this.randSelf = hcReq.randSelf;
        this.randPeer = hcReq.randPeer;
        this.psk = hcReq.psk;
        this.json = hcReq.json;
        this.sessionKey = hcReq.sessionKey;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        if (requestId == 0x00) {
            requestId = System.currentTimeMillis();
        }

        LOG.debug("Request operationCode: " + operationCode + " - step: " + step);
        if (req == null) req = new HiChain.Request(
                operationCode,
                requestId,
                supportProvider.getAndroidId(),
                HuaweiConstants.GROUP_ID
        );
        HuaweiPacket packet = null;
        int messageId = step;
        try {
            if (step == 0x01) {
                seed = new byte[32];
                new Random().nextBytes(seed);
                randSelf = new byte[16];
                new Random().nextBytes(randSelf);
                HiChain.Request.StepOne stepOne = req.new StepOne(paramsProvider, messageId, randSelf, seed );
                packet = stepOne;
            } else if (step == 0x02) {
                byte[] message = ByteBuffer
                        .allocate(randPeer.length + randSelf.length + authIdSelf.length + authIdPeer.length)
                        .put(randSelf)
                        .put(randPeer)
                        .put(authIdPeer)
                        .put(authIdSelf)
                        .array();
                byte[] selfToken = CryptoUtils.calcHmacSha256(psk, message);
                HiChain.Request.StepTwo stepTwo = req.new StepTwo(paramsProvider, messageId, selfToken);
                packet = stepTwo;
            } else if (step == 0x03) {
                byte[] salt = ByteBuffer
                    .allocate( randSelf.length + randPeer.length)
                    .put(randSelf)
                    .put(randPeer)
                    .array();
                byte[] info = "hichain_iso_session_key".getBytes(StandardCharsets.UTF_8);
                sessionKey = CryptoUtils.hkdfSha256(psk, salt, info, 32);
                LOG.debug("sessionKey: " + GB.hexdump(sessionKey));
                if (operationCode == 0x01) {
                    byte[] nonce = new byte[12];
                    new Random().nextBytes(nonce);
                    challenge = new byte[16];
                    new Random().nextBytes(challenge);
                    byte[] aad = "hichain_iso_exchange".getBytes(StandardCharsets.UTF_8);
                    byte[] encData = CryptoUtils.encryptAES_GCM_NoPad(challenge, sessionKey, nonce, aad); //aesGCMNoPadding encrypt(sessionKey as key, challenge to encrypt, nonce as iv)
                    HiChain.Request.StepThree stepThree = req.new StepThree(paramsProvider, messageId, nonce, encData);
                    packet = stepThree;
                } else {
                    step += 0x01;
                }
            }
            if (step == 0x04) {
                LOG.debug("Step " + step);
                byte[] nonce = new byte[12];
                new Random().nextBytes(nonce);
                byte[] input = new byte[]{0x00, 0x00, 0x00, 0x00};
                byte[] aad = "hichain_iso_result".getBytes(StandardCharsets.UTF_8);
                byte[] encResult = CryptoUtils.encryptAES_GCM_NoPad(input, sessionKey, nonce, aad);
                HiChain.Request.StepFour stepFour = req.new StepFour(paramsProvider, messageId, nonce, encResult);
                packet = stepFour;

            }
            LOG.debug("JSONObject on creation:" + (new JSONObject(packet.getTlv().getString(1))).getJSONObject("payload").toString());
            return packet.serialize();
        } catch (Exception e) {
            // TODO: Make exception explicit
            throw new RequestCreationException("HiChain exception", e);
        }
        //return null;
    }

    @Override
    protected void processResponse() throws ResponseParseException {
        if (!(receivedPacket instanceof HiChain.Response))
            throw new ResponseTypeMismatchException(receivedPacket, HiChain.Response.class);

        HiChain.Response response = (HiChain.Response)receivedPacket;
        if (response.errorCode != 0) {
            throw new ResponseParseException("Got errorCode " +  response.errorCode);
        }
        step = response.step;

        LOG.debug("Response operationCode: " + operationCode + " - step: " + step);
        try {
            if (step == 0x04) {
                if (operationCode == 0x01) {
                    LOG.debug("Finished auth operation, go to bind");
                    GetHiChainRequest nextRequest = new GetHiChainRequest(supportProvider, false);
                    nextRequest.setFinalizeReq(this.finalizeReq);
                    this.nextRequest(nextRequest);
                } else {
                    LOG.debug("Finished bind operation");
                    byte[] salt = ByteBuffer
                        .allocate( randSelf.length + randPeer.length)
                        .put(randSelf)
                        .put(randPeer)
                        .array();
                    byte[] info = "hichain_return_key".getBytes(StandardCharsets.UTF_8);
                    byte[] key = CryptoUtils.hkdfSha256(sessionKey, salt, info, 32);
                    LOG.debug("Final sessionKey:" + GB.hexdump(key));
                    paramsProvider.setSecretKey(key);
                }
            } else {
                if (step == 0x01) {
                    byte[] key = null;
                    authIdSelf = supportProvider.getAndroidId();
                    authIdPeer = response.step1Data.peerAuthId;
                    randPeer = response.step1Data.isoSalt;
                    byte[] peerToken = response.step1Data.token;
                    // GeneratePsk
                    if (operationCode == 0x01) {
                        String pinCodeHexStr = StringUtils.bytesToHex(paramsProvider.getPinCode());
                        byte[] pinCode = pinCodeHexStr.getBytes(StandardCharsets.UTF_8);
                        key = CryptoUtils.digest(pinCode);
                    } else {
                        key = supportProvider.getSecretKey();
                    }
                    psk = CryptoUtils.calcHmacSha256(key, seed);
                    byte[] message = ByteBuffer
                        .allocate(randPeer.length + randSelf.length + authIdSelf.length + authIdPeer.length)
                        .put(randPeer)
                        .put(randSelf)
                        .put(authIdSelf)
                        .put(authIdPeer)
                        .array();
                    byte[] tokenCheck = CryptoUtils.calcHmacSha256(psk, message);
                    if (!Arrays.equals(peerToken, tokenCheck)) {
                        LOG.debug("tokenCheck: " + GB.hexdump(tokenCheck) + " is different than " + GB.hexdump(peerToken));
                        throw new RequestCreationException("tokenCheck: " + GB.hexdump(tokenCheck) + " is different than " + GB.hexdump(peerToken));
                    } else {
                        LOG.debug("Token check passes");
                    }
                } else if (step == 0x02) {
                    byte[] returnCodeMac = response.step2Data.returnCodeMac;
                    byte[] returnCodeMacCheck = CryptoUtils.calcHmacSha256(psk, new byte[]{0x00, 0x00, 0x00, 0x00});
                    if (!Arrays.equals(returnCodeMacCheck, returnCodeMac)) {
                        LOG.debug("returnCodeMacCheck: " + GB.hexdump(returnCodeMacCheck) + " is different than " + GB.hexdump(returnCodeMac));
                        throw new RequestCreationException("returnCodeMacCheck: " + GB.hexdump(returnCodeMacCheck) + " is different than " + GB.hexdump(returnCodeMac));
                    } else {
                        LOG.debug("returnCodeMac check passes");
                    }
                } else if (step == 0x03) {
                    if (operationCode == 0x01) {
                        byte[] nonce = response.step3Data.nonce;
                        byte[] encAuthToken = response.step3Data.encAuthToken;
                        byte[] authToken = CryptoUtils.decryptAES_GCM_NoPad(encAuthToken, sessionKey, nonce, challenge);
                        supportProvider.setSecretKey(authToken);
                        LOG.debug("Set secret key");
                    }
                }
                this.step += 0x01;
                GetHiChainRequest nextRequest = new GetHiChainRequest(this);
                nextRequest.setFinalizeReq(this.finalizeReq);
                this.nextRequest(nextRequest);
            }
        } catch (Exception e) {
            // TODO: Specify exceptions
            throw new ResponseParseException(e);
        }
    }
}
