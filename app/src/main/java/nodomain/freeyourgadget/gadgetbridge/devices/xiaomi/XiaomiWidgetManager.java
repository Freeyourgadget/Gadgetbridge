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
package nodomain.freeyourgadget.gadgetbridge.devices.xiaomi;

import androidx.annotation.Nullable;

import com.google.protobuf.InvalidProtocolBufferException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.capabilities.widgets.WidgetLayout;
import nodomain.freeyourgadget.gadgetbridge.capabilities.widgets.WidgetManager;
import nodomain.freeyourgadget.gadgetbridge.capabilities.widgets.WidgetPart;
import nodomain.freeyourgadget.gadgetbridge.capabilities.widgets.WidgetPartSubtype;
import nodomain.freeyourgadget.gadgetbridge.capabilities.widgets.WidgetScreen;
import nodomain.freeyourgadget.gadgetbridge.capabilities.widgets.WidgetType;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.proto.xiaomi.XiaomiProto;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiPreferences;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class XiaomiWidgetManager implements WidgetManager {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiWidgetManager.class);

    private final GBDevice device;

    public XiaomiWidgetManager(final GBDevice device) {
        this.device = device;
    }

    @Override
    public List<WidgetLayout> getSupportedWidgetLayouts() {
        final List<WidgetLayout> layouts = new ArrayList<>();
        final Set<WidgetType> partTypes = new HashSet<>();

        final XiaomiProto.WidgetParts rawWidgetParts = getRawWidgetParts();

        for (final XiaomiProto.WidgetPart widgetPart : rawWidgetParts.getWidgetPartList()) {
            partTypes.add(fromRawWidgetType(widgetPart.getType()));
        }

        if (partTypes.contains(WidgetType.WIDE) && partTypes.contains(WidgetType.SMALL)) {
            layouts.add(WidgetLayout.TOP_1_BOT_2);
            layouts.add(WidgetLayout.TOP_2_BOT_1);
            layouts.add(WidgetLayout.TOP_2_BOT_2);
        }

        if (partTypes.contains(WidgetType.TALL)) {
            layouts.add(WidgetLayout.SINGLE);

            if (partTypes.contains(WidgetType.SMALL)) {
                layouts.add(WidgetLayout.TWO);
            }
        }

        return layouts;
    }

    @Override
    public List<WidgetPart> getSupportedWidgetParts(final WidgetType targetWidgetType) {
        final List<WidgetPart> parts = new LinkedList<>();

        final XiaomiProto.WidgetParts rawWidgetParts = getRawWidgetParts();

        final Set<String> seenNames = new HashSet<>();
        final Set<String> duplicatedNames = new HashSet<>();

        for (final XiaomiProto.WidgetPart widgetPart : rawWidgetParts.getWidgetPartList()) {
            final WidgetType type = fromRawWidgetType(widgetPart.getType());

            if (type != null && type.equals(targetWidgetType)) {
                final WidgetPart newPart = new WidgetPart(
                        String.valueOf(widgetPart.getId()),
                        widgetPart.getTitle(),
                        type
                );

                // FIXME are there others?
                if (widgetPart.getId() == 2321) {
                    if (StringUtils.isBlank(newPart.getName())) {
                        newPart.setName(GBApplication.getContext().getString(R.string.menuitem_workout));
                    }

                    final List<XiaomiWorkoutType> workoutTypes = XiaomiPreferences.getWorkoutTypes(getDevice());
                    for (final XiaomiWorkoutType workoutType : workoutTypes) {
                        newPart.getSupportedSubtypes().add(
                                new WidgetPartSubtype(
                                        String.valueOf(workoutType.getCode()),
                                        workoutType.getName()
                                )
                        );
                        Collections.sort(newPart.getSupportedSubtypes(), (p1, p2) -> p1.getName().compareToIgnoreCase(p2.getName()));
                    }
                }

                if (seenNames.contains(newPart.getFullName())) {
                    duplicatedNames.add(newPart.getFullName());
                } else {
                    seenNames.add(newPart.getFullName());
                }

                parts.add(newPart);
            }
        }

        // Ensure that all names are unique
        for (final WidgetPart part : parts) {
            if (duplicatedNames.contains(part.getFullName())) {
                part.setName(String.format(Locale.ROOT, "%s (%s)", part.getName(), part.getId()));
            }
        }

        return parts;
    }

    @Override
    public List<WidgetScreen> getWidgetScreens() {
        final XiaomiProto.WidgetScreens rawWidgetScreens = getRawWidgetScreens();

        final List<WidgetScreen> ret = new ArrayList<>(rawWidgetScreens.getWidgetScreenCount());

        final List<XiaomiWorkoutType> workoutTypes = XiaomiPreferences.getWorkoutTypes(getDevice());

        for (final XiaomiProto.WidgetScreen widgetScreen : rawWidgetScreens.getWidgetScreenList()) {
            final WidgetLayout layout = fromRawLayout(widgetScreen.getLayout());

            final List<WidgetPart> parts = new ArrayList<>(widgetScreen.getWidgetPartCount());

            for (final XiaomiProto.WidgetPart widgetPart : widgetScreen.getWidgetPartList()) {
                final WidgetType type = fromRawWidgetType(widgetPart.getType());

                final WidgetPart newPart = new WidgetPart(
                        String.valueOf(widgetPart.getId()),
                        "Unknown (" + widgetPart.getId() + ")",
                        type
                );

                // Find the name
                final XiaomiProto.WidgetPart rawPart1 = findRawPart(widgetPart.getType(), widgetPart.getId());
                if (rawPart1 != null) {
                    newPart.setName(rawPart1.getTitle());
                }

                // FIXME are there others?
                if (widgetPart.getId() == 2321) {
                    if (StringUtils.isBlank(newPart.getName())) {
                        newPart.setName(GBApplication.getContext().getString(R.string.menuitem_workout));
                    }
                }

                // Get the proper subtype, if any
                if (widgetPart.getSubType() != 0) {
                    for (final XiaomiWorkoutType workoutType : workoutTypes) {
                        if (workoutType.getCode() == widgetPart.getSubType()) {
                            newPart.setSubtype(new WidgetPartSubtype(
                                    String.valueOf(workoutType.getCode()),
                                    workoutType.getName()
                            ));
                        }
                    }
                }

                parts.add(newPart);
            }

            ret.add(new WidgetScreen(
                    String.valueOf(widgetScreen.getId()),
                    layout,
                    parts
            ));
        }

        return ret;
    }

    @Override
    public GBDevice getDevice() {
        return device;
    }

    @Override
    public int getMinScreens() {
        return getRawWidgetScreens().getWidgetsCapabilities().getMinWidgets();
    }

    @Override
    public int getMaxScreens() {
        return getRawWidgetScreens().getWidgetsCapabilities().getMaxWidgets();
    }

    @Override
    public void saveScreen(final WidgetScreen widgetScreen) {
        final XiaomiProto.WidgetScreens rawWidgetScreens = getRawWidgetScreens();

        final int layoutNum;
        switch (widgetScreen.getLayout()) {
            case TOP_2_BOT_2:
                layoutNum = 1;
                break;
            case TOP_1_BOT_2:
                layoutNum = 2;
                break;
            case TOP_2_BOT_1:
                layoutNum = 4;
                break;
            case TWO:
                layoutNum = 256;
                break;
            case SINGLE:
                layoutNum = 512;
                break;
            default:
                LOG.warn("Unknown widget screens layout {}", widgetScreen.getLayout());
                return;
        }

        XiaomiProto.WidgetScreen.Builder rawScreen = null;
        if (widgetScreen.getId() == null) {
            // new screen
            rawScreen = XiaomiProto.WidgetScreen.newBuilder()
                    .setId(rawWidgetScreens.getWidgetScreenCount() + 1); // ids start at 1
        } else {
            for (final XiaomiProto.WidgetScreen screen : rawWidgetScreens.getWidgetScreenList()) {
                if (String.valueOf(screen.getId()).equals(widgetScreen.getId())) {
                    rawScreen = XiaomiProto.WidgetScreen.newBuilder(screen);
                    break;
                }

                LOG.warn("Failed to find original screen for {}", widgetScreen.getId());
            }

            if (rawScreen == null) {
                rawScreen = XiaomiProto.WidgetScreen.newBuilder()
                        .setId(rawWidgetScreens.getWidgetScreenCount() + 1);
            }
        }

        rawScreen.setLayout(layoutNum);
        rawScreen.clearWidgetPart();

        for (final WidgetPart newPart : widgetScreen.getParts()) {
            // Find the existing raw part
            final XiaomiProto.WidgetPart knownRawPart = findRawPart(
                    toRawWidgetType(newPart.getType()),
                    Integer.parseInt(Objects.requireNonNull(newPart.getId()))
            );

            final XiaomiProto.WidgetPart.Builder newRawPartBuilder = XiaomiProto.WidgetPart.newBuilder(knownRawPart);

            if (newPart.getSubtype() != null) {
                // Get the workout type as subtype
                final List<XiaomiWorkoutType> workoutTypes = XiaomiPreferences.getWorkoutTypes(getDevice());
                for (final XiaomiWorkoutType workoutType : workoutTypes) {
                    if (newPart.getSubtype().getId().equals(String.valueOf(workoutType.getCode()))) {
                        newRawPartBuilder.setSubType(workoutType.getCode());
                        break;
                    }
                }
            }

            rawScreen.addWidgetPart(newRawPartBuilder);
        }

        final XiaomiProto.WidgetScreens.Builder builder = XiaomiProto.WidgetScreens.newBuilder(rawWidgetScreens);
        if (rawScreen.getId() == rawWidgetScreens.getWidgetScreenCount() + 1) {
            // Append at the end
            builder.addWidgetScreen(rawScreen);
        } else {
            // Replace existing
            builder.clearWidgetScreen();

            for (final XiaomiProto.WidgetScreen screen : rawWidgetScreens.getWidgetScreenList()) {
                if (screen.getId() == rawScreen.getId()) {
                    builder.addWidgetScreen(rawScreen);
                } else {
                    builder.addWidgetScreen(screen);
                }
            }
        }

        builder.setIsFullList(1);

        getPrefs().getPreferences().edit()
                .putString(XiaomiPreferences.PREF_WIDGET_SCREENS, GB.hexdump(builder.build().toByteArray()))
                .apply();
    }

    @Override
    public void deleteScreen(final WidgetScreen widgetScreen) {
        if (widgetScreen.getId() == null) {
            LOG.warn("Can't delete screen without id");
            return;
        }

        final XiaomiProto.WidgetScreens rawWidgetScreens = getRawWidgetScreens();

        final XiaomiProto.WidgetScreens.Builder builder = XiaomiProto.WidgetScreens.newBuilder(rawWidgetScreens)
                .clearWidgetScreen();

        for (final XiaomiProto.WidgetScreen screen : rawWidgetScreens.getWidgetScreenList()) {
            if (String.valueOf(screen.getId()).equals(widgetScreen.getId())) {
                continue;
            }

            builder.addWidgetScreen(screen);
        }

        getPrefs().getPreferences().edit()
                .putString(XiaomiPreferences.PREF_WIDGET_SCREENS, GB.hexdump(builder.build().toByteArray()))
                .apply();
    }

    @Override
    public void sendToDevice() {
        GBApplication.deviceService(getDevice()).onSendConfiguration(DeviceSettingsPreferenceConst.PREF_WIDGETS);
    }

    private Prefs getPrefs() {
        return new Prefs(GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress()));
    }

    @Nullable
    private WidgetType fromRawWidgetType(final int rawType) {
        switch (rawType) {
            case 1:
                return WidgetType.SMALL;
            case 2:
                return WidgetType.WIDE;
            case 3:
                return WidgetType.TALL;
            default:
                LOG.warn("Unknown widget type {}", rawType);
                return null;
        }
    }

    private int toRawWidgetType(final WidgetType widgetType) {
        switch (widgetType) {
            case SMALL:
                return 1;
            case WIDE:
                return 2;
            case TALL:
                return 3;
            default:
                throw new IllegalArgumentException("Unknown widget type " + widgetType);
        }
    }

    @Nullable
    private WidgetLayout fromRawLayout(final int rawLayout) {
        switch (rawLayout) {
            case 1:
                return WidgetLayout.TOP_2_BOT_2;
            case 2:
                return WidgetLayout.TOP_1_BOT_2;
            case 4:
                return WidgetLayout.TOP_2_BOT_1;
            case 256:
                return WidgetLayout.TWO;
            case 512:
                return WidgetLayout.SINGLE;
            default:
                LOG.warn("Unknown widget screens layout {}", rawLayout);
                return null;
        }
    }

    @Nullable
    private XiaomiProto.WidgetPart findRawPart(final int type, final int id) {
        final XiaomiProto.WidgetParts rawWidgetParts = getRawWidgetParts();

        for (final XiaomiProto.WidgetPart rawPart : rawWidgetParts.getWidgetPartList()) {
            if (rawPart.getType() == type && rawPart.getId() == id) {
                return rawPart;
            }
        }

        return null;
    }

    private XiaomiProto.WidgetScreens getRawWidgetScreens() {
        final String hex = getPrefs().getString(XiaomiPreferences.PREF_WIDGET_SCREENS, null);
        if (hex == null) {
            LOG.warn("raw widget screens hex is null");
            return XiaomiProto.WidgetScreens.newBuilder().build();
        }

        try {
            return XiaomiProto.WidgetScreens.parseFrom(GB.hexStringToByteArray(hex));
        } catch (final InvalidProtocolBufferException e) {
            LOG.warn("failed to parse raw widget screns hex");
            return XiaomiProto.WidgetScreens.newBuilder().build();
        }
    }

    private XiaomiProto.WidgetParts getRawWidgetParts() {
        final String hex = getPrefs().getString(XiaomiPreferences.PREF_WIDGET_PARTS, null);
        if (hex == null) {
            LOG.warn("raw widget parts hex is null");
            return XiaomiProto.WidgetParts.newBuilder().build();
        }

        try {
            return XiaomiProto.WidgetParts.parseFrom(GB.hexStringToByteArray(hex));
        } catch (final InvalidProtocolBufferException e) {
            LOG.warn("failed to parse raw widget parts hex");
            return XiaomiProto.WidgetParts.newBuilder().build();
        }
    }
}
