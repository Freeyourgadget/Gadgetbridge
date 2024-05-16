package nodomain.freeyourgadget.gadgetbridge.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class HttpUtils {
    private static final Logger LOG = LoggerFactory.getLogger(HttpUtils.class);

    private HttpUtils() {
        // utility class
    }

    public static Map<String, String> urlQueryParameters(final URL url) {
        final String query = url.getQuery();
        if (StringUtils.isBlank(query)) {
            return Collections.emptyMap();
        }
        final Map<String, String> queryParameters = new LinkedHashMap<>();
        final String[] pairs = query.split("&");
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
}
