/*  Copyright (C) 2015-2023 Andreas Shimokawa, boun, Carsten Pfeiffer, Daniel
    Dakhno, Daniele Gobbetti, JohnnySun, jonnsoft, José Rebelo, Lem Dulfo, Taavi
    Eomäe, Uwe Hermann

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
package nodomain.freeyourgadget.gadgetbridge.activities.discovery;

import android.os.ParcelUuid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;

/**
 * A dedicated thread to process {@link GBScanEvent}s. This class keeps a map from mac address to
 * GBDeviceCandidate, with the current known state of each candidate.
 * <p>
 * Processing works as follows:
 * - The processor consumes mac addresses from the eventsToProcessQueue
 * - Mac addresses are placed on the queue when there are one or more new GBScanEvents to process in
 *   the eventsToProcessMap map
 * - The eventsToProcessMap contains a list of events per device, so that they can be processed in batch
 * - The GBDeviceEvent for the corresponding mac address in candidatesByAddress gets updated with the new
 *   information, and matched against the coordinators.
 */
public final class GBScanEventProcessor implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(GBScanEventProcessor.class);

    private static final ParcelUuid ZERO_UUID = ParcelUuid.fromString("00000000-0000-0000-0000-000000000000");

    // Devices that can be ignored by just the address (eg. already bonded)
    private final Set<String> devicesToIgnore = new HashSet<>();
    private final Map<String, GBDeviceCandidate> candidatesByAddress = new LinkedHashMap<>();

    private final BlockingQueue<String> eventsToProcessQueue = new LinkedBlockingQueue<>();
    private final Map<String, List<GBScanEvent>> eventsToProcessMap = new HashMap<>();

    private boolean ignoreBonded = true;
    private boolean discoverUnsupported = false;

    private volatile boolean running = false;
    private Thread thread = null;

    private final Callback callback;

    public GBScanEventProcessor(final Callback callback) {
        this.callback = callback;
    }

    @Override
    public void run() {
        LOG.info("Device Found Processor Thread started.");

        while (running) {
            try {
                LOG.debug("Polling found devices queue, current size = {}", eventsToProcessQueue.size());
                final String candidateAddress = eventsToProcessQueue.take();
                if (candidateAddress != null) {
                    if (processAllScanEvents(candidateAddress)) {
                        callback.onDeviceChanged();
                    }
                }
            } catch (final InterruptedException e) {
                LOG.warn("Processing thread interrupted");
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void start() {
        if (running) {
            LOG.warn("Already running!");
            return;
        }

        running = true;
        thread = new Thread("Gadgetbridge Device Found Processor Thread") {
            @Override
            public void run() {
                GBScanEventProcessor.this.run();
            }
        };
        thread.start();
    }

    public void stop() {
        running = false;

        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
    }

    public void clear() {
        devicesToIgnore.clear();
        candidatesByAddress.clear();
        eventsToProcessMap.clear();
        eventsToProcessQueue.clear();
    }

    public void setIgnoreBonded(boolean ignoreBonded) {
        this.ignoreBonded = ignoreBonded;
    }

    public void setDiscoverUnsupported(boolean discoverUnsupported) {
        this.discoverUnsupported = discoverUnsupported;
    }

    /**
     * Returns the current list of GBDeviceCandidates. The candidates are cloned, since they can be
     * modified concurrently by the processor.
     */
    public List<GBDeviceCandidate> getDevices() {
        final List<GBDeviceCandidate> ret = new ArrayList<>();
        // candidatesByAddress keeps insertion order, so newer devices will be at the end
        synchronized (candidatesByAddress) {
            for (final Map.Entry<String, GBDeviceCandidate> entry : candidatesByAddress.entrySet()) {
                ret.add(entry.getValue().clone());
            }
        }
        return ret;
    }

    /**
     * Schedule a {@link GBScanEvent} to be processed asynchronously.
     */
    public void scheduleProcessing(final GBScanEvent event) {
        LOG.debug("Scheduling {} for processing ({})", event.getDevice().getAddress(), event.getServiceUuids());

        final String address = event.getDevice().getAddress();
        synchronized (eventsToProcessMap) {
            if (!eventsToProcessMap.containsKey(address)) {
                eventsToProcessMap.put(address, new LinkedList<>());
            }
            Objects.requireNonNull(eventsToProcessMap.get(address)).add(event);
        }

        try {
            eventsToProcessQueue.put(address);
        } catch (final InterruptedException e) {
            LOG.error("Failed to put device on processing queue", e);
        }
    }

    private boolean processCandidate(final GBDeviceCandidate candidate) {
        LOG.debug("found device: {}, {}", candidate.getName(), candidate.getMacAddress());
        if (LOG.isDebugEnabled()) {
            final ParcelUuid[] uuids = candidate.getServiceUuids();
            if (uuids != null && uuids.length > 0) {
                for (ParcelUuid uuid : uuids) {
                    LOG.debug("  supports uuid: " + uuid.toString());
                }
            }
        }

        final DeviceType deviceType = DeviceHelper.getInstance().resolveDeviceType(candidate, false);

        if (deviceType.isSupported() || discoverUnsupported) {
            synchronized (candidatesByAddress) {
                candidatesByAddress.put(candidate.getMacAddress(), candidate);
            }
        }

        return deviceType.isSupported();
    }

    private boolean processAllScanEvents(final String address) {
        final List<GBScanEvent> events;
        synchronized (eventsToProcessMap) {
            events = eventsToProcessMap.remove(address);
        }
        if (events == null || events.isEmpty()) {
            LOG.warn("Attempted to process {}, but found no events", address);
            return false;
        }

        if (devicesToIgnore.contains(address)) {
            LOG.trace("Ignoring {} events for {}", events.size(), address);
            return false;
        }

        LOG.debug("Processing {} events for {}", events.size(), address);

        GBDeviceCandidate candidate = candidatesByAddress.get(address);

        String previousName = null;
        ParcelUuid[] previousUuids = null;
        boolean firstTime = false;

        if (candidate == null) {
            // First time we see this device
            LOG.debug("Found {} for the first time", address);
            firstTime = true;
            final GBScanEvent firstEvent = events.get(0);
            events.remove(0);
            candidate = new GBDeviceCandidate(firstEvent.getDevice(), firstEvent.getRssi(), firstEvent.getServiceUuids());
        } else {
            previousName = candidate.getName();
            previousUuids = candidate.getServiceUuids();
        }

        if (candidate.isBonded() && ignoreBonded) {
            LOG.trace("Ignoring already bonded device {}", address);
            devicesToIgnore.add(address);
            return false;
        }

        // Update the device with the remaining events
        for (final GBScanEvent event : events) {
            candidate.setRssi(event.getRssi());
            candidate.addUuids(event.getServiceUuids());
        }

        candidate.refreshNameIfUnknown();
        try {
            candidate.addUuids(candidate.getDevice().getUuids());
        } catch (final SecurityException e) {
            LOG.error("SecurityException on candidate.getDevice().getUuids()");
        }

        if (!firstTime) {
            if (Objects.equals(candidate.getName(), previousName) && Arrays.equals(candidate.getServiceUuids(), previousUuids)) {
                // Neither name nor uuids changed, do not reprocess
                LOG.trace("Not reprocessing {} due to no changes", address);
                return false;
            }
        }

        if (processCandidate(candidate)) {
            LOG.info(
                    "Device {} ({}) is supported as '{}' without scanning services",
                    candidate.getDevice(),
                    candidate.getName(),
                    DeviceHelper.getInstance().resolveDeviceType(candidate, false)
            );
            return true;
        }

        if (candidate.getServiceUuids().length == 0 || (candidate.getServiceUuids().length == 1 && candidate.getServiceUuids()[0].equals(ZERO_UUID))) {
            LOG.debug("Fetching uuids for {} with sdp", candidate.getDevice().getAddress());
            try {
                candidate.getDevice().fetchUuidsWithSdp();
            } catch (final SecurityException e) {
                LOG.error("SecurityException on candidate.getDevice().fetchUuidsWithSdp()");
            }
        }

        return true;
    }

    public interface Callback {
        void onDeviceChanged();
    }
}
