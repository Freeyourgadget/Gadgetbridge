package nodomain.freeyourgadget.gadgetbridge.service.devices.id115;

import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.id115.ID115Constants;
import nodomain.freeyourgadget.gadgetbridge.devices.id115.ID115SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.ID115ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class FetchActivityOperation extends AbstractID115Operation {
    private static final Logger LOG = LoggerFactory.getLogger(FetchActivityOperation.class);
    private byte expectedCmd;
    private byte expectedSeq;
    private ArrayList<byte[]> packets;

    protected FetchActivityOperation(ID115Support support) {
        super(support);
    }

    @Override
    boolean isHealthOperation() {
        return true;
    }

    @Override
    protected void doPerform() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(ID115Constants.CMD_ID_HEALTH_DATA);
        outputStream.write(ID115Constants.CMD_KEY_FETCH_ACTIVITY_TODAY);
        outputStream.write(0x01);
        outputStream.write(0x00);
        outputStream.write(0x00);
        byte cmd[] = outputStream.toByteArray();

        expectedCmd = ID115Constants.CMD_KEY_FETCH_ACTIVITY_TODAY;
        expectedSeq = 1;
        packets = new ArrayList<>();

        TransactionBuilder builder = performInitialized("send activity fetch request");
        builder.write(controlCharacteristic, cmd);
        builder.queue(getQueue());
    }

    @Override
    void handleResponse(byte[] data) {
        if (!isOperationRunning()) {
            LOG.error("ignoring notification because operation is not running. Data length: " + data.length);
            getSupport().logMessageContent(data);
            return;
        }

        if (data.length < 4) {
            LOG.warn("short GATT response");
            return;
        }

        if (data[0] == ID115Constants.CMD_ID_HEALTH_DATA) {
            if (data[1] == (byte)0xEE) {
                LOG.info("Activity data transfer has finished.");
                parseAndStore();
                operationFinished();
            } else {
                if ((data[1] != expectedCmd) || (data[2] != expectedSeq)) {
                    GB.toast(getContext(), "Error fetching ID115 activity data, you may need to connect and disconnect", Toast.LENGTH_LONG, GB.ERROR);
                    operationFinished();
                    return;
                }
                expectedSeq += 1;

                byte payload[] = new byte[data.length - 4];
                System.arraycopy(data, 4, payload, 0, payload.length);
                packets.add(payload);
            }
        }
    }

    void parseAndStore() {
        if (packets.size() <= 1) {
            return;
        }

        byte[] header = packets.get(0);
        int year = ((header[1] & 0xFF) << 8) | (header[0] & 0xFF);
        int month = header[2] & 0xFF;
        int day = header[3] & 0xFF;
        int sampleDurationMinutes = header[6] & 0xFF;
        Calendar calendar = new GregorianCalendar(year, month - 1, day);
        int ts = (int)(calendar.getTimeInMillis() / 1000);
        int dt = sampleDurationMinutes * 60;

        ArrayList<ID115ActivitySample> samples = new ArrayList<>();

        for (int i = 2; i < packets.size(); i++) {
            byte[] packet = packets.get(i);
            for (int j = 0; j <= packet.length - 5; j += 5) {
                byte[] sampleData = new byte[5];
                System.arraycopy(packet, j, sampleData, 0, sampleData.length);

                ID115ActivitySample sample = parseSample(sampleData);
                if (sample != null) {
                    sample.setTimestamp(ts);
                    sample.setRawKind(ActivityKind.TYPE_ACTIVITY);
                    samples.add(sample);
                }
                ts += dt;
            }
        }

        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            ID115ActivitySample[] sampleArray = samples.toArray(new ID115ActivitySample[0]);
            long userId = DBHelper.getUser(dbHandler.getDaoSession()).getId();
            long deviceId = DBHelper.getDevice(getDevice(), dbHandler.getDaoSession()).getId();
            for (ID115ActivitySample sample: sampleArray) {
                sample.setUserId(userId);
                sample.setDeviceId(deviceId);
            }

            ID115SampleProvider provider = new ID115SampleProvider(getDevice(), dbHandler.getDaoSession());
            provider.addGBActivitySamples(sampleArray);
        } catch (Exception ex) {
            GB.toast(getContext(), "Error saving activity data: " + ex.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    ID115ActivitySample parseSample(byte[] data) {
        int d01 = ((data[1] & 0xFF) << 8) | (data[0] & 0xFF);
        int d12 = ((data[2] & 0xFF) << 8) | (data[1] & 0xFF);
        int d23 = ((data[3] & 0xFF) << 8) | (data[2] & 0xFF);
        int d34 = ((data[4] & 0xFF) << 8) | (data[3] & 0xFF);
        int stepCount = (d01 >> 2) & 0xFFF;
        int activeTime = (d12 >> 6) & 0xF;
        int calories = (d23 >> 2) & 0x3FF;
        int distance = (d34 >> 4);

        if (stepCount == 0) {
            return null;
        }

        ID115ActivitySample sample = new ID115ActivitySample();
        sample.setSteps(stepCount);
        sample.setActiveTimeMinutes(activeTime);
        sample.setCaloriesBurnt(calories);
        sample.setDistanceMeters(distance);
        return sample;
    }
}
