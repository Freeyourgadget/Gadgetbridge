package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminSupport;

public class FakeOauthHandler {
    private static final Logger LOG = LoggerFactory.getLogger(FakeOauthHandler.class);
    private final GarminSupport deviceSupport;

    public FakeOauthHandler(GarminSupport deviceSupport) {
        this.deviceSupport = deviceSupport;
    }

    private GarminHttpResponse createGarminHttpResponse(String logMessage, String body) {
        LOG.info(logMessage);
        final GarminHttpResponse response = new GarminHttpResponse();
        response.setStatus(200);
        response.setBody(body.getBytes(StandardCharsets.UTF_8));
        return response;
    }

    public GarminHttpResponse handleInitialOAuthRequest(GarminHttpRequest request) {
        final String fakeResponse = "{ \"accessToken\": \"t1\", \"tokenType\": \"Bearer\", \"refreshToken\": \"r1\", \"expiresIn\": 7776000, \"scope\": \"GCS_EPHEMERIS_SONY_READ GCS_CIQ_APPSTORE_MOBILE_READ GCS_EMERGENCY_ASSISTANCE_CREATE GCS_GEOLOCATION_ELEVATION_READ GCS_IMAGE_READ GCS_LIVETRACK_FIT_CREATE GCS_LIVETRACK_FIT_READ GCS_LIVETRACK_FIT_UPDATE OMT_GOLF_SUBSCRIPTION_READ\", \"refreshTokenExpiresIn\": \"31536000\", \"customerId\": \"c1\" }";
        return createGarminHttpResponse("Sending fake initial oauth response", fakeResponse);
    }

    public GarminHttpResponse handleOAuthRequest(GarminHttpRequest request) {
        final String fakeOauth = "{\"access_token\":\"t\",\"token_type\":\"Bearer\",\"expires_in\":7776000,\"scope\":\"GCS_EPHEMERIS_SONY_READ GCS_CIQ_APPSTORE_MOBILE_READ GCS_EMERGENCY_ASSISTANCE_CREATE GCS_GEOLOCATION_ELEVATION_READ GCS_IMAGE_READ GCS_LIVETRACK_FIT_CREATE GCS_LIVETRACK_FIT_READ GCS_LIVETRACK_FIT_UPDATE OMT_GOLF_SUBSCRIPTION_READ\",\"refresh_token\":\"r\",\"refresh_token_expires_in\":\"31536000\",\"customerId\":\"c\"}";
        return createGarminHttpResponse("Sending fake oauth", fakeOauth);
    }
}