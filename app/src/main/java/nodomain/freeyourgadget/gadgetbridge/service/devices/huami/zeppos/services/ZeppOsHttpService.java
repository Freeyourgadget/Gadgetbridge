/*  Copyright (C) 2023 José Rebelo

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Weather;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class ZeppOsHttpService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsHttpService.class);

    private static final short ENDPOINT = 0x0001;

    public static final byte CMD_REQUEST = 0x01;
    public static final byte CMD_RESPONSE = 0x02;

    public static final byte RESPONSE_SUCCESS = 0x01;
    public static final byte RESPONSE_NO_INTERNET = 0x02;

    public ZeppOsHttpService(final Huami2021Support support) {
        super(support);
    }

    @Override
    public short getEndpoint() {
        return ENDPOINT;
    }

    @Override
    public boolean isEncrypted() {
        return true;
    }

    @Override
    public void handlePayload(final byte[] payload) {
        switch (payload[0]) {
            case CMD_REQUEST:
                int pos = 1;
                final byte requestId = payload[pos++];
                final String method = StringUtils.untilNullTerminator(payload, pos);
                if (method == null) {
                    LOG.error("Failed to decode method from payload");
                    return;
                }
                pos += method.length() + 1;
                final String url = StringUtils.untilNullTerminator(payload, pos);
                if (url == null) {
                    LOG.error("Failed to decode method from payload");
                    return;
                }
                // headers after pos += url.length() + 1;

                LOG.info("Got HTTP {} request: {}", method, url);

                handleUrlRequest(requestId, method, url);
                return;
            default:
                LOG.warn("Unexpected HTTP payload byte {}", String.format("0x%02x", payload[0]));
        }
    }

    private void handleUrlRequest(final byte requestId, final String method, final String urlString) {
        if (!"GET".equals(method)) {
            LOG.error("Unable to handle HTTP method {}", method);
            // TODO: There's probably a "BAD REQUEST" response or similar
            replyHttpNoInternet(requestId);
            return;
        }

        final URL url;
        try {
            url = new URL(urlString);
        } catch (final MalformedURLException e) {
            LOG.error("Failed to parse url", e);
            replyHttpNoInternet(requestId);
            return;
        }

        final String path = url.getPath();
        final Map<String, String> query = urlQueryParameters(url);

        if (path.startsWith("/weather/")) {
            final Huami2021Weather.Response response = Huami2021Weather.handleHttpRequest(path, query);
            replyHttpSuccess(requestId, response.getHttpStatusCode(), response.toJson());
            return;
        }

        LOG.error("Unhandled URL {}", url);
        replyHttpNoInternet(requestId);
    }

    private Map<String, String> urlQueryParameters(final URL url) {
        final Map<String, String> queryParameters = new HashMap<>();
        final String[] pairs = url.getQuery().split("&");
        for (final String pair : pairs) {
            final String[] parts = pair.split("=", 2);
            try {
                final String key = URLDecoder.decode(parts[0], "UTF-8");
                if (parts.length == 2) {
                    queryParameters.put(key, URLDecoder.decode(parts[1], "UTF-8"));
                } else {
                    queryParameters.put(key, "");
                }
            } catch (final Exception e) {
                LOG.error("Failed to decode query", e);
            }
        }
        return queryParameters;
    }

    private void replyHttpNoInternet(final byte requestId) {
        LOG.info("Replying with no internet to http request {}", requestId);

        final byte[] cmd = new byte[]{CMD_RESPONSE, requestId, RESPONSE_NO_INTERNET, 0x00, 0x00, 0x00, 0x00};

        write("http reply no internet", cmd);
    }

    private void replyHttpSuccess(final byte requestId, final int status, final String content) {
        LOG.debug("Replying with http {} request {} with {}", status, requestId, content);

        final byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        final ByteBuffer buf = ByteBuffer.allocate(8 + contentBytes.length);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        buf.put(CMD_RESPONSE);
        buf.put(requestId);
        buf.put(RESPONSE_SUCCESS);
        buf.put((byte) status);
        buf.putInt(contentBytes.length);
        buf.put(contentBytes);

        write("http reply success", buf.array());
    }
}
