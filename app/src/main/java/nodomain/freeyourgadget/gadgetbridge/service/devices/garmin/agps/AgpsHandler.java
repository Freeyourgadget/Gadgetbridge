package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.agps;

import androidx.documentfile.provider.DocumentFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Callable;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminPreferences;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http.GarminHttpRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http.GarminHttpResponse;
import nodomain.freeyourgadget.gadgetbridge.util.CheckSums;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class AgpsHandler {
    private static final Logger LOG = LoggerFactory.getLogger(AgpsHandler.class);
    private static final String QUERY_CONSTELLATIONS = "constellations";
    private final GarminSupport deviceSupport;

    public AgpsHandler(GarminSupport deviceSupport) {
        this.deviceSupport = deviceSupport;
    }

    public GarminHttpResponse handleAgpsRequest(final GarminHttpRequest request) {
        saveKnownUrl(request.getUrl());

        try {
            final DocumentFile agpsFile = deviceSupport.getAgpsFile(request.getUrl());
            if (agpsFile == null) {
                LOG.warn("File with AGPS data for {} does not exist.", request.getUrl());
                return null;
            }
            try (InputStream agpsIn = deviceSupport.getContext().getContentResolver().openInputStream(agpsFile.getUri())) {
                if (agpsIn == null) {
                    LOG.error("Failed to open input stream for agps file {}", agpsFile.getUri());
                    return null;
                }

                final GarminHttpResponse response = new GarminHttpResponse();

                final byte[] rawBytes = FileUtils.readAll(agpsIn, 1024 * 1024); // 1MB, they're usually ~60KB
                final String fileHash = GB.hexdump(CheckSums.md5(rawBytes)).toLowerCase(Locale.ROOT);
                final String etag = "\"" + fileHash + "\"";
                response.getHeaders().put("etag", etag);

                if (request.getHeaders().containsKey("if-none-match")) {
                    // Check checksum
                    final String ifNoneMatch = request.getHeaders().get("if-none-match");
                    LOG.debug("agps request hash = {}, file hash = {}", ifNoneMatch, etag);

                    if (etag.equals(ifNoneMatch)) {
                        response.setBody(new byte[0]);
                        response.setStatus(304);
                        return response;
                    }
                }
                response.getHeaders().put("cache-control", "max-age=14400");

                // Run some sanity checks on known agps file formats
                final GarminAgpsFile garminAgpsFile = new GarminAgpsFile(rawBytes);
                if (request.getQuery().containsKey(QUERY_CONSTELLATIONS)) {
                    final String[] requestedConstellations = Objects.requireNonNull(request.getQuery().get(QUERY_CONSTELLATIONS)).split(",");
                    if (!garminAgpsFile.isValidTar(requestedConstellations)) {
                        reportError(request.getUrl());
                        return null;
                    }
                } else if (request.getPath().contains(("/rxnetworks/"))) {
                    if (!garminAgpsFile.isValidRxNetworks()) {
                        reportError(request.getUrl());
                        return null;
                    }
                } else if (request.getPath().startsWith(("/ephemeris/cpe/sony"))) {
                    if (!garminAgpsFile.isValidSonyCpe()) {
                        reportError(request.getUrl());
                        return null;
                    }
                } else {
                    LOG.warn("Refusing to send agps for unknown url");
                    return null;
                }

                LOG.info("Sending new AGPS data (length: {}) to the device from {}", rawBytes.length, agpsFile.getUri());

                if (request.getHeaders().containsKey("accept")) {
                    response.getHeaders().put("Content-Type", request.getHeaders().get("accept"));
                } else {
                    response.getHeaders().put("Content-Type", "application/octet-stream");
                }

                response.setStatus(200);
                response.setBody(rawBytes);
                response.setOnDataSuccessfullySentListener(getOnDataSuccessfullySentListener(request.getUrl()));

                return response;
            }
        } catch (final IOException e) {
            LOG.error("Unable to obtain AGPS data", e);
            reportError(request.getUrl());
            return null;
        }
    }

    public void saveKnownUrl(final String url) {
        final Prefs devicePrefs = deviceSupport.getDevicePrefs();
        final List<String> knownAgpsUrls = new ArrayList<>(devicePrefs.getList(GarminPreferences.PREF_AGPS_KNOWN_URLS, Collections.emptyList(), "\n"));
        if (!knownAgpsUrls.contains(url)) {
            knownAgpsUrls.add(url);
            devicePrefs.getPreferences().edit()
                    .putString(GarminPreferences.PREF_AGPS_KNOWN_URLS, String.join("\n", knownAgpsUrls))
                    .apply();
        }
    }

    private void reportError(final String url) {
        deviceSupport.evaluateGBDeviceEvent(new GBDeviceEventUpdatePreferences(
                GarminPreferences.agpsStatus(url), GarminAgpsStatus.ERROR.name()
        ));
    }

    public Callable<Void> getOnDataSuccessfullySentListener(final String urlString) {
        return () -> {
            LOG.info("AGPS data successfully sent to the device.");
            deviceSupport.evaluateGBDeviceEvent(new GBDeviceEventUpdatePreferences()
                    .withPreference(GarminPreferences.agpsStatus(urlString), GarminAgpsStatus.CURRENT.name())
                    .withPreference(GarminPreferences.agpsUpdateTime(urlString), Instant.now().toEpochMilli())
            );
            return null;
        };
    }
}
