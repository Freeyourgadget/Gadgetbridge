package nodomain.freeyourgadget.voice;

interface IOpusCodecService {
    int version();

    String create();
    void destroy(String codec);

    int decoderInit(String codec, int sampleRate, int channels);
    int decode(String codec, in byte[] data, int len, out byte[] pcm, int frameSize, int decodeFec);
    void decoderDestroy(String codec);
}
