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
package nodomain.freeyourgadget.gadgetbridge.voice;

import android.os.RemoteException;

import nodomain.freeyourgadget.voice.IOpusCodecService;

public class OpusCodec {
    private final IOpusCodecService mOpusCodecService;
    private final String codec;

    public OpusCodec(final IOpusCodecService opusCodecService) throws RemoteException {
        this.mOpusCodecService = opusCodecService;
        this.codec = opusCodecService.create();
    }

    public void destroy() throws RemoteException {
        this.mOpusCodecService.destroy(codec);
    }

    public int decoderInit(final int sampleRate, final int channels) throws RemoteException {
        return mOpusCodecService.decoderInit(codec, sampleRate, channels);
    }

    public int decode(final byte[] data, final int len, final byte[] pcm, final int frameSize, final int decodeFec) throws RemoteException {
        return mOpusCodecService.decode(codec, data, len, pcm, frameSize, decodeFec);
    }

    public void decoderDestroy() throws RemoteException {
        mOpusCodecService.decoderDestroy(codec);
    }
}
