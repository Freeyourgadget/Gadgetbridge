package nodomain.freeyourgadget.gadgetbridge.service.devices.soundcore.liberty;

enum TapFunction {
    VOLUME_DOWN(1),
    VOLUME_UP(0),
    MEDIA_NEXT( 3),
    MEDIA_PREV(2),
    PLAYPAUSE(6),
    VOICE_ASSISTANT(5),
    AMBIENT_SOUND_CONTROL(4)
    ;

    private final int code;

    TapFunction(final int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
