package nodomain.freeyourgadget.gadgetbridge.miband;

import nodomain.freeyourgadget.gadgetbridge.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.GBCommand;

public class MiBandSupport extends AbstractBTLEDeviceSupport {

    @Override
    public void dispose() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean connect() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void onSMS(String from, String body) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onEmail(String from, String subject, String body) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSetTime(long ts) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSetCallState(String number, String name, GBCommand command) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSetMusicInfo(String artist, String album, String track) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onFirmwareVersionReq() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onAppInfoReq() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onAppDelete(int id, int index) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onPhoneVersion(byte os) {
        // TODO Auto-generated method stub

    }
}
