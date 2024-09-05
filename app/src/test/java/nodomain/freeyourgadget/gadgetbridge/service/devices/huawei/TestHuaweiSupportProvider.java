package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei;

import org.junit.Test;
import org.mockito.Mockito;

public class TestHuaweiSupportProvider {

    @Test
    public void testOnSocketReadExactPacket() {
        byte[] data1 = {(byte) 0x5A, (byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x04, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x99, (byte) 0x6B};

        HuaweiBRSupport support = new HuaweiBRSupport();

        HuaweiSupportProvider supportProvider = new HuaweiSupportProvider(support);
        supportProvider.responseManager = Mockito.mock(ResponseManager.class);

        supportProvider.onSocketRead(data1);

        Mockito.verify(supportProvider.responseManager, Mockito.times(1)).handleData(data1);
    }

//    @Test
//    public void testOnSocketReadMultiplePacket() {
//        byte[] expected = {(byte) 0x5A, (byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x04, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x99, (byte) 0x6B};
//        byte[] data1 = {(byte) 0x5A, (byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x04, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x99, (byte) 0x6B, (byte) 0x5A, (byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x04, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x99, (byte) 0x6B};
//
//        HuaweiBRSupport support = new HuaweiBRSupport();
//
//        HuaweiSupportProvider supportProvider = new HuaweiSupportProvider(support);
//        supportProvider.responseManager = Mockito.mock(ResponseManager.class);
//
//        supportProvider.onSocketRead(data1);
//
//        Mockito.verify(supportProvider.responseManager, Mockito.times(2)).handleData(expected);
//    }

    @Test
    public void testOnSocketReadPartialPacket() {
        byte[] data1 = {(byte) 0x5A, (byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x04, (byte) 0x01};
        byte[] data2 = {(byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x99, (byte) 0x6B};

        HuaweiBRSupport support = new HuaweiBRSupport();

        HuaweiSupportProvider supportProvider = new HuaweiSupportProvider(support);
        supportProvider.responseManager = Mockito.mock(ResponseManager.class);

        supportProvider.onSocketRead(data1);
        supportProvider.onSocketRead(data2);

        Mockito.verify(supportProvider.responseManager, Mockito.times(1)).handleData(data1);
        Mockito.verify(supportProvider.responseManager, Mockito.times(1)).handleData(data2);
    }

    @Test
    public void testOnSocketReadPartialPacket5a() {
        byte[] data1 = {(byte) 0x5A, (byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x04, (byte) 0x01};
        byte[] data2 = {(byte) 0x5A, (byte) 0x91, (byte) 0x92, (byte) 0x99, (byte) 0x6B};

        HuaweiBRSupport support = new HuaweiBRSupport();

        HuaweiSupportProvider supportProvider = new HuaweiSupportProvider(support);
        supportProvider.responseManager = Mockito.mock(ResponseManager.class);

        supportProvider.onSocketRead(data1);
        supportProvider.onSocketRead(data2);

        Mockito.verify(supportProvider.responseManager, Mockito.times(1)).handleData(data1);
        Mockito.verify(supportProvider.responseManager, Mockito.times(1)).handleData(data2);
    }

}
