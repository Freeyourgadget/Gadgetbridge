/*  Copyright (C) 2023-2024 José Rebelo, Yoran Vulker

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventMusicControl;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.proto.xiaomi.XiaomiProto;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSupport;
import nodomain.freeyourgadget.gadgetbridge.util.MediaManager;

public class XiaomiMusicService extends AbstractXiaomiService {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiMusicService.class);

    public static final int COMMAND_TYPE = 18;

    private static final int CMD_MUSIC_GET = 0;
    private static final int CMD_MUSIC_SEND = 1;
    private static final int CMD_MUSIC_BUTTON = 2;

    private static final byte BUTTON_PLAY = 0x00;
    private static final byte BUTTON_PAUSE = 0x01;
    private static final byte BUTTON_PREVIOUS = 0x03;
    private static final byte BUTTON_NEXT = 0x04;
    private static final byte BUTTON_VOLUME = 0x05;

    private static final byte STATE_NOTHING = 0x00;
    private static final byte STATE_PLAYING = 0x01;
    private static final byte STATE_PAUSED = 0x02;

    protected MediaManager mediaManager = null;

    public XiaomiMusicService(final XiaomiSupport support) {
        super(support);
    }

    @Override
    public void setContext(Context context) {
        super.setContext(context);
        this.mediaManager = new MediaManager(context);
    }

    @Override
    public void handleCommand(final XiaomiProto.Command cmd) {
        final XiaomiProto.Music music = cmd.getMusic();

        switch (cmd.getSubtype()) {
            case CMD_MUSIC_GET:
                LOG.debug("Got music request from watch");
                mediaManager.refresh();
                sendMusicStateToDevice();
                return;
            case CMD_MUSIC_BUTTON:
                LOG.debug("Got music button from watch: {}", music.getMediaKey().getKey());
                final GBDeviceEventMusicControl deviceEventMusicControl = new GBDeviceEventMusicControl();
                switch (music.getMediaKey().getKey()) {
                    case BUTTON_PLAY:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PLAY;
                        break;
                    case BUTTON_PAUSE:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PAUSE;
                        break;
                    case BUTTON_PREVIOUS:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PREVIOUS;
                        break;
                    case BUTTON_NEXT:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.NEXT;
                        break;
                    case BUTTON_VOLUME: {
                        final int requestedVolume = music.getMediaKey().getVolume();
                        final int currentVolume = MediaManager.getPhoneVolume(getSupport().getContext());

                        if (requestedVolume > currentVolume) {
                            deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.VOLUMEUP;
                        } else {
                            deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.VOLUMEDOWN;
                        }

                        break;
                    }
                    default:
                        LOG.warn("Unexpected media button key {}", music.getMediaKey().getKey());
                        return;
                }
                getSupport().evaluateGBDeviceEvent(deviceEventMusicControl);
                return;
        }

        LOG.warn("Unknown music command {}", cmd.getSubtype());
    }

    public void onSetMusicState(final MusicStateSpec stateSpec) {
        if (mediaManager.onSetMusicState(stateSpec)) {
            sendMusicStateToDevice();
        }
    }

    public void onSetPhoneVolume(final float ignoredVolume) {
        sendMusicStateToDevice();
    }

    public void onSetMusicInfo(final MusicSpec musicSpec) {
        if (mediaManager.onSetMusicInfo(musicSpec)) {
            sendMusicStateToDevice();
        }
    }

    private void sendMusicStateToDevice() {
        final MusicSpec musicSpec = mediaManager.getBufferMusicSpec();
        final MusicStateSpec musicStateSpec = mediaManager.getBufferMusicStateSpec();

        final XiaomiProto.MusicInfo.Builder musicInfo = XiaomiProto.MusicInfo.newBuilder()
                .setVolume(mediaManager.getPhoneVolume());

        if (musicSpec == null || musicStateSpec == null) {
            musicInfo.setState(STATE_NOTHING);
        } else {
            if (musicStateSpec.state == MusicStateSpec.STATE_PLAYING) {
                musicInfo.setState(STATE_PLAYING);
            } else {
                musicInfo.setState(STATE_PAUSED);
            }

            musicInfo.setVolume(mediaManager.getPhoneVolume())
                    .setTrack(musicSpec.track != null ? musicSpec.track : "")
                    .setArtist(musicSpec.artist != null ? musicSpec.artist : "")
                    .setPosition(musicStateSpec.position)
                    .setDuration(musicSpec.duration);
        }

        final XiaomiProto.Music music = XiaomiProto.Music.newBuilder()
                .setMusicInfo(musicInfo.build())
                .build();

        getSupport().sendCommand(
                "send music",
                XiaomiProto.Command.newBuilder()
                        .setType(COMMAND_TYPE)
                        .setSubtype(CMD_MUSIC_SEND)
                        .setMusic(music)
                        .build()
        );
    }
}
