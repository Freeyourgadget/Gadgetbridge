package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.agps;

import androidx.documentfile.provider.DocumentFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.bp.Instant;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminPreferences;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminSupport;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class AgpsHandler {
    private static final Logger LOG = LoggerFactory.getLogger(AgpsHandler.class);
    private static final String QUERY_CONSTELLATIONS = "constellations";
    private final GarminSupport deviceSupport;

    public AgpsHandler(GarminSupport deviceSupport) {
        this.deviceSupport = deviceSupport;
    }

    public byte[] handleAgpsRequest(final String url, final String path, final Map<String, String> query) {
        saveKnownUrl(url);

        try {
            final DocumentFile agpsFile = deviceSupport.getAgpsFile(url);
            if (agpsFile == null) {
                LOG.warn("File with AGPS data for {} does not exist.", url);
                return null;
            }
            try (InputStream agpsIn = deviceSupport.getContext().getContentResolver().openInputStream(agpsFile.getUri())) {
                if (agpsIn == null) {
                    LOG.error("Failed to open input stream for agps file {}", agpsFile.getUri());
                    return null;
                }

                // Run some sanity checks on known agps file formats
                final byte[] rawBytes = FileUtils.readAll(agpsIn, 1024 * 1024); // 1MB, they're usually ~60KB
                final GarminAgpsFile garminAgpsFile = new GarminAgpsFile(rawBytes);
                if (query.containsKey(QUERY_CONSTELLATIONS)) {
                    final String[] requestedConstellations = Objects.requireNonNull(query.get(QUERY_CONSTELLATIONS)).split(",");
                    if (!garminAgpsFile.isValidTar(requestedConstellations)) {
                        reportError(url);
                        return null;
                    }
                } else if (path.contains(("/rxnetworks/"))) {
                    if (!garminAgpsFile.isValidRxNetworks()) {
                        reportError(url);
                        return null;
                    }
                } else {
                    LOG.warn("Refusing to send agps for unknown url");
                    return null;
                }

                LOG.info("Sending new AGPS data to the device from {}", agpsFile.getUri());
                return rawBytes;
            }
        } catch (final IOException e) {
            LOG.error("Unable to obtain AGPS data", e);
            reportError(url);
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
