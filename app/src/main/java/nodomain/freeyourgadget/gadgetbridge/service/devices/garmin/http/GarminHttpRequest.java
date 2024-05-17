package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiHttpService;
import nodomain.freeyourgadget.gadgetbridge.util.HttpUtils;

public class GarminHttpRequest {
    private final GdiHttpService.HttpService.RawRequest rawRequest;

    private final String url;
    private final String path;
    private final Map<String, String> query;
    private final Map<String, String> headers;

    public GarminHttpRequest(final GdiHttpService.HttpService.RawRequest rawRequest) {
        this.rawRequest = rawRequest;

        final URL netUrl;
        try {
            netUrl = new URL(rawRequest.getUrl());
        } catch (final MalformedURLException e) {
            throw new IllegalArgumentException("Failed to parse url", e);
        }

        this.url = rawRequest.getUrl();
        this.path = netUrl.getPath();
        this.query = HttpUtils.urlQueryParameters(netUrl);
        this.headers = headersToMap(rawRequest.getHeaderList());
    }

    public GdiHttpService.HttpService.RawRequest getRawRequest() {
        return rawRequest;
    }

    public String getUrl() {
        return url;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getQuery() {
        return query;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    private static Map<String, String> headersToMap(final List<GdiHttpService.HttpService.Header> headers) {
        final Map<String, String> ret = new HashMap<>();
        for (final GdiHttpService.HttpService.Header header : headers) {
            ret.put(header.getKey().toLowerCase(Locale.ROOT), header.getValue());
        }
        return ret;
    }
}
