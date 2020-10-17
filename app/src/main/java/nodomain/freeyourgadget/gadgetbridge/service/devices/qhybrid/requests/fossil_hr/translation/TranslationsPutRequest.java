package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.translation;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.file.FileHandle;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FilePutRequest;

public class TranslationsPutRequest extends FilePutRequest {
    public TranslationsPutRequest(TranslationData translationData, FossilWatchAdapter adapter) {
        super(FileHandle.ASSET_TRANSLATIONS, createPayload(translationData), adapter);
    }

    private static byte[] createPayload(TranslationData translationData){
        TranslationItem[] translations = translationData.getTranslations();
        int fileLength = 6 + translations.length * 6; // locale + one null-byte per translation + lengths per translation
        for(TranslationItem translation : translations){
            fileLength += translation.getOriginal().getBytes().length + translation.getTranslated().getBytes().length;
        }

        ByteBuffer buffer = ByteBuffer.allocate(fileLength);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        buffer.put(translationData.getLocale().getBytes())
                .put((byte)0);

        for(TranslationItem translation : translations){
            byte[] originalBytes = translation.getOriginal().getBytes();
            byte[] translatedBytes = translation.getTranslated().getBytes();
            buffer.putShort((short)(originalBytes.length + 1))
                    .put(originalBytes)
                    .put((byte)0) // null terminator
                    .putShort((short)(translatedBytes.length + 1))
                    .put(translatedBytes)
                    .put((byte)0); // null terminator
        }

        return buffer.array();
    }
}
