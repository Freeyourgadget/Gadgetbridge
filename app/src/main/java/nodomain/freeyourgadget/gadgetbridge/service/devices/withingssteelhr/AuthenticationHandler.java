/*  Copyright (C) 2023-2024 Frank Ertl

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr;

import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Build;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;

import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.WithingsUUID;
import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.conversation.AbstractResponseHandler;
import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.datastructures.Challenge;
import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.datastructures.ChallengeResponse;
import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.datastructures.Probe;
import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.datastructures.ProbeOsVersion;
import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.datastructures.ProbeReply;
import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.datastructures.WithingsStructure;
import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.message.Message;
import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.message.WithingsMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.message.WithingsMessageType;

public class AuthenticationHandler extends AbstractResponseHandler {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationHandler.class);

    // TODO: Save this somewhere if we actually decide to use te secret for more security:
    private final String secret = "2EM5zNP37QzM00hmP6BFTD92nG15XwNd";
    private WithingsSteelHRDeviceSupport support;
    private Challenge challengeToSend;

    public AuthenticationHandler(WithingsSteelHRDeviceSupport support) {
        super(support);
        this.support = support;
    }

    @Override
    public void handleResponse(Message response) {
        short messageType = response.getType();
        if (messageType == WithingsMessageType.PROBE) {
            handleProbeReply(response);
        } else if (messageType == WithingsMessageType.CHALLENGE) {
            handleChallenge(response);
        } else {
            logger.warn("Received unkown message: " + messageType + ", will ignore this.");
        }
    }

    private void handleChallenge(Message challengeMessage) {
        try {
            Challenge challenge = getTypeFromReply(Challenge.class, challengeMessage);
            ChallengeResponse challengeResponse = new ChallengeResponse();
            challengeResponse.setResponse(createResponse(challenge));
            Message message = new WithingsMessage(WithingsMessageType.CHALLENGE);
            message.addDataStructure(challengeResponse);
            challengeToSend = new Challenge();
            challengeToSend.setMacAddress(challenge.getMacAddress());
            byte[] bArr = new byte[16];
            new Random().nextBytes(bArr);
            challengeToSend.setChallenge(bArr);
            message.addDataStructure(challengeToSend);
            support.sendToDevice(message);
        } catch (Exception e) {
            logger.error("Failed to create response to challenge: " + e.getMessage());
        }
    }

    private void handleProbeReply(Message message) {
        ProbeReply probeReply = getTypeFromReply(ProbeReply.class, message);
        if (probeReply == null) {
            throw new IllegalArgumentException("Message does not contain the required datastructure ProbeReply");
        }

        ChallengeResponse response = getTypeFromReply(ChallengeResponse.class, message);

        if (response == null || Arrays.equals(response.getResponse(), createResponse(challengeToSend))) {
            support.getDevice().setFirmwareVersion(String.valueOf(probeReply.getFirmwareVersion()));
        } else {
            throw new SecurityException("Response is not the one expected!");
        }

        support.onAuthenticationFinished();
    }

    private byte[] createResponse(Challenge challenge) {
        try {
            ByteBuffer allocate = ByteBuffer.allocate(challenge.getChallenge().length + challenge.getMacAddress().getBytes().length + secret.getBytes().length);
            allocate.put(challenge.getChallenge());
            allocate.put(challenge.getMacAddress().getBytes());
            allocate.put(secret.getBytes());
            return MessageDigest.getInstance("SHA1").digest(allocate.array());
        } catch (NoSuchAlgorithmException e) {
            logger.error("Failed to create response to challenge: " + e.getMessage());
        }

        return new byte[0];
    }

    private <T extends WithingsStructure> T getTypeFromReply(Class<T> type, Message message) {
        for (WithingsStructure structure : message.getDataStructures()) {
            if (type.isInstance(structure)) {
                return (T)structure;
            }
        }

        return null;
    }
}
