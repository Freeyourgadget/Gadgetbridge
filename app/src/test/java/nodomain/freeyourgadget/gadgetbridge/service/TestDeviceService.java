package nodomain.freeyourgadget.gadgetbridge.service;

import android.content.Intent;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceService;
import nodomain.freeyourgadget.gadgetbridge.test.GBMockIntent;

public class TestDeviceService extends GBDeviceService {
    private final AbstractServiceTestCase<?> mTestCase;

    public TestDeviceService(AbstractServiceTestCase<?> testCase) throws Exception {
        super(testCase.getContext());
        mTestCase = testCase;
    }

    @Override
    protected Intent createIntent() {
        return new GBMockIntent();
    }

    @Override
    protected void invokeService(Intent intent) {
        mTestCase.startService(intent);
    }
}
