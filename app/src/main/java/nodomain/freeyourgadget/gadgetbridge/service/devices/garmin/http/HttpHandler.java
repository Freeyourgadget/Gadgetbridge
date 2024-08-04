package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http;

import com.google.protobuf.ByteString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiHttpService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.agps.AgpsHandler;

public class HttpHandler {
    private static final Logger LOG = LoggerFactory.getLogger(HttpHandler.class);

    private final AgpsHandler agpsHandler;
    private final ContactsHandler contactsHandler;
    private final FakeOauthHandler fakeOauthHandler;

    public HttpHandler(GarminSupport deviceSupport) {
        agpsHandler = new AgpsHandler(deviceSupport);
        contactsHandler = new ContactsHandler(deviceSupport);
        fakeOauthHandler = new FakeOauthHandler(deviceSupport);
    }

    public GdiHttpService.HttpService handle(final GdiHttpService.HttpService httpService) {
        if (httpService.hasRawRequest()) {
            final GdiHttpService.HttpService.RawResponse rawResponse = handleRawRequest(httpService.getRawRequest());
            if (rawResponse != null) {
                return GdiHttpService.HttpService.newBuilder()
                        .setRawResponse(rawResponse)
                        .build();
            }
            return null;
        }

        LOG.warn("Unsupported http service request {}", httpService);

        return null;
    }

    public GdiHttpService.HttpService.RawResponse handleRawRequest(final GdiHttpService.HttpService.RawRequest rawRequest) {
        LOG.debug("Got rawRequest: {} - {}", rawRequest.getMethod(), rawRequest.getUrl());

        final GarminHttpRequest request = new GarminHttpRequest(rawRequest);

        final GarminHttpResponse response;
        if (request.getPath().startsWith("/weather/")) {
            LOG.info("Got weather request for {}", request.getPath());
            response = WeatherHandler.handleWeatherRequest(request);
        } else if (request.getPath().startsWith("/ephemeris/")) {
            LOG.info("Got AGPS request for {}", request.getPath());
            response = agpsHandler.handleAgpsRequest(request);
        } else if (request.getPath().startsWith("/device-gateway/usercontact/")) {
            LOG.info("Got contacts request for {}", request.getPath());
            response = contactsHandler.handleRequest(request);
        } else if (request.getPath().equalsIgnoreCase("/api/oauth/token")){
            LOG.info("Got oauth request for {}", request.getPath());
            response = fakeOauthHandler.handleOAuthRequest(request);
        } else if (request.getPath().equalsIgnoreCase("/oauthTokenExchangeService/connectToIT")){
            LOG.info("Got initial oauth request for {}", request.getPath());
            response = fakeOauthHandler.handleInitialOAuthRequest(request);
        } else {
            LOG.warn("Unhandled path {}", request.getPath());
            response = null;
        }

        if (response == null) {
            return GdiHttpService.HttpService.RawResponse.newBuilder()
                    .setStatus(GdiHttpService.HttpService.Status.UNKNOWN_STATUS)
                    .build();
        }

        LOG.debug("Http response status={}", response.getStatus());

        return createRawResponse(request, response);
    }

    private static GdiHttpService.HttpService.RawResponse createRawResponse(
            final GarminHttpRequest request,
            final GarminHttpResponse response
    ) {
        final List<GdiHttpService.HttpService.Header> responseHeaders = new ArrayList<>();
        for (final Map.Entry<String, String> h : response.getHeaders().entrySet()) {
            responseHeaders.add(
                    GdiHttpService.HttpService.Header.newBuilder()
                            .setKey(h.getKey())
                            .setValue(h.getValue())
                            .build()
            );
        }

        if (response.getStatus() == 200 && request.getRawRequest().hasUseDataXfer() && request.getRawRequest().getUseDataXfer()) {
            LOG.debug("Data will be returned using data_xfer");
            int id = DataTransferHandler.registerData(response.getBody());
            if (response.getOnDataSuccessfullySentListener() != null) {
                DataTransferHandler.addOnDataSuccessfullySentListener(id, response.getOnDataSuccessfullySentListener());
            }
            return GdiHttpService.HttpService.RawResponse.newBuilder()
                    .setStatus(GdiHttpService.HttpService.Status.OK)
                    .setHttpStatus(response.getStatus())
                    .addAllHeader(responseHeaders)
                    .setXferData(
                            GdiHttpService.HttpService.DataTransferItem.newBuilder()
                                    .setId(id)
                                    .setSize(response.getBody().length)
                                    .build()
                    )
                    .build();
        }

        final byte[] responseBody;
        if ("gzip".equals(request.getHeaders().get("accept-encoding"))) {
            LOG.debug("Compressing response");
            responseHeaders.add(
                    GdiHttpService.HttpService.Header.newBuilder()
                            .setKey("Content-Encoding")
                            .setValue("gzip")
                            .build()
            );

            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
                gzos.write(response.getBody());
                gzos.finish();
                gzos.flush();
                responseBody = baos.toByteArray();
            } catch (final Exception e) {
                LOG.error("Failed to compress response", e);
                return null;
            }
        } else {
            responseBody = response.getBody();
        }

        return GdiHttpService.HttpService.RawResponse.newBuilder()
                .setStatus(response.getStatus() / 100 == 2 ? GdiHttpService.HttpService.Status.OK : GdiHttpService.HttpService.Status.UNKNOWN_STATUS)
                .setHttpStatus(response.getStatus())
                .setBody(ByteString.copyFrom(responseBody))
                .addAllHeader(responseHeaders)
                .build();
    }
}
