/*  Copyright (C) 2023 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventMusicControl;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;

public class ZeppOsMusicService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsMusicService.class);

    private static final short ENDPOINT = 0x001b;

    private static final byte CMD_MEDIA_INFO = 0x03;
    private static final byte CMD_APP_STATE = 0x04;
    private static final byte CMD_BUTTON_PRESS = 0x05;
    private static final byte MUSIC_APP_OPEN = 0x01;
    private static final byte MUSIC_APP_CLOSE = 0x02;
    private static final byte BUTTON_PLAY = 0x00;
    private static final byte BUTTON_PAUSE = 0x01;
    private static final byte BUTTON_NEXT = 0x03;
    private static final byte BUTTON_PREVIOUS = 0x04;
    private static final byte BUTTON_VOLUME_UP = 0x05;
    private static final byte BUTTON_VOLUME_DOWN = 0x06;

    public ZeppOsMusicService(final Huami2021Support support) {
        super(support);
    }

    @Override
    public short getEndpoint() {
        return ENDPOINT;
    }

    @Override
    public boolean isEncrypted() {
        return false;
    }

    @Override
    public void handlePayload(final byte[] payload) {
        switch (payload[0]) {
            case CMD_APP_STATE:
                switch (payload[1]) {
                    case MUSIC_APP_OPEN:
                        onMusicAppOpen();
                        break;
                    case MUSIC_APP_CLOSE:
                        onMusicAppClosed();
                        break;
                    default:
                        LOG.warn("Unexpected music app state {}", String.format("0x%02x", payload[1]));
                        break;
                }
                return;

            case CMD_BUTTON_PRESS:
                LOG.info("Got music button press");
                final GBDeviceEventMusicControl deviceEventMusicControl = new GBDeviceEventMusicControl();
                switch (payload[1]) {
                    case BUTTON_PLAY:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PLAY;
                        break;
                    case BUTTON_PAUSE:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PAUSE;
                        break;
                    case BUTTON_NEXT:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.NEXT;
                        break;
                    case BUTTON_PREVIOUS:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PREVIOUS;
                        break;
                    case BUTTON_VOLUME_UP:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.VOLUMEUP;
                        break;
                    case BUTTON_VOLUME_DOWN:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.VOLUMEDOWN;
                        break;
                    default:
                        LOG.warn("Unexpected music button {}", String.format("0x%02x", payload[1]));
                        return;
                }
                evaluateGBDeviceEvent(deviceEventMusicControl);
                return;
            default:
                LOG.warn("Unexpected music byte {}", String.format("0x%02x", payload[0]));
        }
    }

    private void onMusicAppOpen() {
        getSupport().onMusicAppOpen();
    }

    private void onMusicAppClosed() {
        getSupport().onMusicAppClosed();
    }

    public void sendMusicState(final MusicSpec musicSpec,
                               final MusicStateSpec musicStateSpec) {
        LOG.info("Sending music: {}, {}", musicSpec, musicStateSpec);

        // TODO: Encode not playing state (flag 0x20, single 0x01 byte before volume)
        final byte[] cmd = ArrayUtils.addAll(
                new byte[]{CMD_MEDIA_INFO},
                HuamiSupport.encodeMusicState(getContext(), musicSpec, musicStateSpec, false)
        );

        write("send music state", cmd);
    }

    public void sendVolume(final float volume) {
        LOG.info("Sending volume: {}", volume);

        final byte[] cmd = ArrayUtils.addAll(
                new byte[]{CMD_MEDIA_INFO},
                HuamiSupport.encodeMusicState(getContext(), null, null, true)
        );

        write("send volume", cmd);
    }
}
