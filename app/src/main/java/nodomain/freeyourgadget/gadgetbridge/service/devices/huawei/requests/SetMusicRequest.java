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

import android.content.Context;
import android.media.AudioManager;
import android.media.session.PlaybackState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.MusicControl;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SetMusicRequest extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(SetMusicRequest.class);

    private final MusicStateSpec musicStateSpec;
    private final MusicSpec musicSpec;

    public SetMusicRequest(HuaweiSupportProvider support, MusicStateSpec musicStateSpec, MusicSpec musicSpec) {
        super(support);
        this.serviceId = MusicControl.id;
        this.commandId = MusicControl.MusicInfo.id;
        this.musicStateSpec = musicStateSpec;
        this.musicSpec = musicSpec;
    }

    private byte convertMusicState(int in) {
        switch (in) {
            case MusicStateSpec.STATE_PLAYING:
                return PlaybackState.STATE_PLAYING;
            case MusicStateSpec.STATE_PAUSED:
                return PlaybackState.STATE_PAUSED;
            case MusicStateSpec.STATE_STOPPED:
                return PlaybackState.STATE_STOPPED;
            case MusicStateSpec.STATE_UNKNOWN:
            default:
                return PlaybackState.STATE_NONE;
        }
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        String artistName = "";
        String songName = "";
        byte playState = convertMusicState(MusicStateSpec.STATE_UNKNOWN);
        if (this.musicSpec != null) {
            artistName = this.musicSpec.artist;
            songName = this.musicSpec.track;
        }
        if (this.musicStateSpec != null)
            playState = convertMusicState(this.musicStateSpec.state);
        AudioManager audioManager = (AudioManager) this.supportProvider.getContext().getSystemService(Context.AUDIO_SERVICE);
        byte maxVolume = (byte) audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        byte currentVolume = (byte) audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        try {
            return new MusicControl.MusicInfo.Request(
                    paramsProvider,
                    artistName,
                    songName,
                    playState,
                    maxVolume,
                    currentVolume
            ).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() {
        if (receivedPacket instanceof MusicControl.MusicInfo.Response) {
            if (((MusicControl.MusicInfo.Response) receivedPacket).ok) {
                LOG.debug("Music information acknowledged by band");
            } else {
                LOG.warn(((MusicControl.MusicInfo.Response) receivedPacket).error);
            }
        } else {
            LOG.error("MusicInfo response is not of type MusicInfo response");
        }
    }
}
