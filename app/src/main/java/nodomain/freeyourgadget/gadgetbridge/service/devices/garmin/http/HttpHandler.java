package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.protobuf.ByteString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.zip.GZIPOutputStream;

import nodomain.freeyourgadget.gadgetbridge.proto.vivomovehr.GdiHttpService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.agps.AgpsHandler;
import nodomain.freeyourgadget.gadgetbridge.util.HttpUtils;

public class HttpHandler {
    private static final Logger LOG = LoggerFactory.getLogger(HttpHandler.class);

    private static final Gson GSON = new GsonBuilder()
            //.serializeNulls()
            .create();

    private final AgpsHandler agpsHandler;

    public HttpHandler(GarminSupport deviceSupport) {
        agpsHandler = new AgpsHandler(deviceSupport);
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
        // TODO Return status code 304 (Not Modified) when we don't have newer data and "if-none-match" is set.
        final String urlString = rawRequest.getUrl();
        LOG.debug("Got rawRequest: {} - {}", rawRequest.getMethod(), urlString);

        final URL url;
        try {
            url = new URL(urlString);
        } catch (final MalformedURLException e) {
            LOG.error("Failed to parse url", e);
            return null;
        }

        final String path = url.getPath();
        final Map<String, String> query = HttpUtils.urlQueryParameters(url);

        if (path.startsWith("/weather/")) {
            LOG.info("Got weather request for {}", path);
            final Object weatherData = WeatherHandler.handleWeatherRequest(path, query);
            if (weatherData == null) {
                return null;
            }
            final String json = GSON.toJson(weatherData);
            LOG.debug("Weather response: {}", json);
            return createRawResponse(rawRequest, json.getBytes(StandardCharsets.UTF_8), "application/json", null);
        } else if (path.startsWith("/ephemeris/")) {
            LOG.info("Got AGPS request for {}", path);
            final byte[] agpsData = agpsHandler.handleAgpsRequest(path, query);
            if (agpsData == null) {
                return null;
            }
            LOG.debug("Successfully obtained AGPS data (length: {})", agpsData.length);
            return createRawResponse(rawRequest, agpsData, "application/x-tar", agpsHandler.getOnDataSuccessfullySentListener());
        } else {
            LOG.warn("Unhandled path {}", urlString);
            return null;
        }
    }

    private static GdiHttpService.HttpService.RawResponse createRawResponse(
            final GdiHttpService.HttpService.RawRequest rawRequest,
            final byte[] data,
            final String contentType,
            final Callable<Void> onDataSuccessfullySentListener
            ) {
        if (rawRequest.hasUseDataXfer() && rawRequest.getUseDataXfer()) {
            LOG.debug("Data will be returned using data_xfer");
            int id = DataTransferHandler.registerData(data);
            if (onDataSuccessfullySentListener != null) {
                DataTransferHandler.addOnDataSuccessfullySentListener(id, onDataSuccessfullySentListener);
            }
            return GdiHttpService.HttpService.RawResponse.newBuilder()
                    .setStatus(GdiHttpService.HttpService.Status.OK)
                    .setHttpStatus(200)
                    .setXferData(
                            GdiHttpService.HttpService.DataTransferItem.newBuilder()
                                    .setId(id)
                                    .setSize(data.length)
                                    .build()
                    )
                    .build();
        }

        final Map<String, String> requestHeaders = headersToMap(rawRequest.getHeaderList());
        final List<GdiHttpService.HttpService.Header> responseHeaders = new ArrayList<>();
        final byte[] responseBody;
        if ("gzip".equals(requestHeaders.get("accept-encoding"))) {
            responseHeaders.add(
                    GdiHttpService.HttpService.Header.newBuilder()
                            .setKey("Content-Encoding")
                            .setValue("gzip")
                            .build()
            );

            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
                gzos.write(data);
                gzos.finish();
                gzos.flush();
                responseBody = baos.toByteArray();
            } catch (final Exception e) {
                LOG.error("Failed to compress response", e);
                return null;
            }
        } else {
            responseBody = data;
        }

        responseHeaders.add(
                GdiHttpService.HttpService.Header.newBuilder()
                        .setKey("Content-Type")
                        .setValue(contentType)
                        .build()
        );

        return GdiHttpService.HttpService.RawResponse.newBuilder()
                .setStatus(GdiHttpService.HttpService.Status.OK)
                .setHttpStatus(200)
                .setBody(ByteString.copyFrom(responseBody))
                .addAllHeader(responseHeaders)
                .build();
    }

    private static Map<String, String> headersToMap(final List<GdiHttpService.HttpService.Header> headers) {
        final Map<String, String> ret = new HashMap<>();
        for (final GdiHttpService.HttpService.Header header : headers) {
            ret.put(header.getKey().toLowerCase(Locale.ROOT), header.getValue());
        }
        return ret;
    }
}
