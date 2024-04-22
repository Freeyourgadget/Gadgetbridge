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
import java.util.zip.GZIPOutputStream;

import nodomain.freeyourgadget.gadgetbridge.proto.vivomovehr.GdiHttpService;
import nodomain.freeyourgadget.gadgetbridge.util.HttpUtils;

public class HttpHandler {
    private static final Logger LOG = LoggerFactory.getLogger(HttpHandler.class);

    private static final Gson GSON = new GsonBuilder()
            //.serializeNulls()
            .create();

    public static GdiHttpService.HttpService handle(final GdiHttpService.HttpService httpService) {
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

    public static GdiHttpService.HttpService.RawResponse handleRawRequest(final GdiHttpService.HttpService.RawRequest rawRequest) {
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
        final Map<String, String> requestHeaders = headersToMap(rawRequest.getHeaderList());

        final byte[] responseBody;
        final List<GdiHttpService.HttpService.Header> responseHeaders = new ArrayList<>();
        if (path.startsWith("/weather/")) {
            LOG.debug("Got weather request for {}", path);
            final Object obj = WeatherHandler.handleWeatherRequest(path, query);
            if (obj == null) {
                return null;
            }
            final String json = GSON.toJson(obj);
            LOG.debug("Weather response: {}", json);

            final byte[] stringBytes = json.getBytes(StandardCharsets.UTF_8);

            if ("gzip".equals(requestHeaders.get("accept-encoding"))) {
                responseHeaders.add(
                        GdiHttpService.HttpService.Header.newBuilder()
                                .setKey("Content-Encoding")
                                .setValue("gzip")
                                .build()
                );

                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
                    gzos.write(stringBytes);
                    gzos.finish();
                    gzos.flush();
                    responseBody = baos.toByteArray();
                } catch (final Exception e) {
                    LOG.error("Failed to compress response", e);
                    return null;
                }
            } else {
                responseBody = stringBytes;
            }

            responseHeaders.add(
                    GdiHttpService.HttpService.Header.newBuilder()
                            .setKey("Content-Type")
                            .setValue("application/json")
                            .build()
            );
        } else {
            LOG.warn("Unhandled path {}", urlString);
            return null;
        }

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
