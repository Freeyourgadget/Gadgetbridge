/*  Copyright (C) 2024 Vitalii Tomin

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */

package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventAppInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Watchface;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Watchface.WatchfaceDeviceParams;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceApp;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetWatchfacesList;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetWatchfacesNames;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.Request;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendWatchfaceOperation;

public class HuaweiWatchfaceManager
{
    Logger LOG = LoggerFactory.getLogger(HuaweiCoordinator.class);

    public static class Resolution {

        Map<String, Object> map = new HashMap<>();
        public Resolution() {
            //             "height*width"
            map.put("HWHD01", "390*390");
            map.put("HWHD02", "454*454");
            map.put("HWHD03", "240*120");
            map.put("HWHD04", "160*80");
            map.put("HWHD05", "460*188");
            map.put("HWHD06", "456*280");
            map.put("HWHD07", "368*194");
            map.put("HWHD08", "320*320");
            map.put("HWHD09", "466*466");
            map.put("HWHD10", "360*320");
            map.put("HWHD11", "480*336");
            map.put("HWHD12", "240*240");
            map.put("HWHD13", "480*408");
        }

        public boolean  isValid(String themeVersion, String screenResolution) {
            if(!map.containsKey(themeVersion))
                return false;
            String screen = map.get(themeVersion).toString();
            if (screenResolution.equals(screen)) {
                return true;
            } else {
                return false;
            }
        }

        public String screenByThemeVersion(String themeVersion) {
            String screen = map.get(themeVersion).toString();
            return screen;
        }

    }

    public static class WatchfaceDescription {

        public String title;
        public String title_cn;
        public String author;
        public String designer;
        public String screen;
        public String version;
        public String font;
        public String font_cn;

        public WatchfaceDescription(String xmlStr) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder;
            try {
                builder = factory.newDocumentBuilder();
                Document doc = builder.parse(new InputSource(new StringReader(
                        xmlStr)));

                this.title = doc.getElementsByTagName("title").item(0).getTextContent();
                this.title_cn = doc.getElementsByTagName("title-cn").item(0).getTextContent();
                this.author = doc.getElementsByTagName("author").item(0).getTextContent();
                this.designer = doc.getElementsByTagName("designer").item(0).getTextContent();
                this.screen = doc.getElementsByTagName("screen").item(0).getTextContent();
                this.version = doc.getElementsByTagName("version").item(0).getTextContent();
                this.font = doc.getElementsByTagName("font").item(0).getTextContent();
                this.font_cn = doc.getElementsByTagName("font-cn").item(0).getTextContent();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private List<Watchface.InstalledWatchfaceInfo> installedWatchfaceInfoList;
    private HashMap<String, String> watchfacesNames;

    private HuaweiSupportProvider support;

    public HuaweiWatchfaceManager(HuaweiSupportProvider support) {
        this.support = support;
    }

    public void setInstalledWatchfaceInfoList(List<Watchface.InstalledWatchfaceInfo> list) {
        this.installedWatchfaceInfoList = list;
    }

    public List<Watchface.InstalledWatchfaceInfo> getInstalledWatchfaceInfoList()
    {
        return installedWatchfaceInfoList;
    }

    public void  setWatchfacesNames(HashMap<String, String> map) {
        this.watchfacesNames = map;
    }


    public String getRandomName() {
        Random random = new Random();

        String res="";
        for (int i = 0; i < 9; i++) {
            int ran = random.nextInt(9);
            res += String.valueOf(ran);
        }

        res += "_1.0.0";
        return res;
    }

    public static UUID toWatchfaceUUID(final String id) {
        // Watchface IDs are numbers as strings - pad them to the right with F
        // and encode as UUID
        final String padded = String.format("%-32s", id).replace(' ', 'F');
        return UUID.fromString(
                padded.substring(0, 8) + "-" +
                        padded.substring(8, 12) + "-" +
                        padded.substring(12, 16) + "-" +
                        padded.substring(16, 20) + "-" +
                        padded.substring(20, 32)
        );
    }

    public static String toWatchfaceId(final UUID uuid) {
        return uuid.toString()
                .replaceAll("-", "")
                .replaceAll("f", "")
                .replaceAll("F", "");
    }

    public void handleWatchfaceList() {

        final List<GBDeviceApp> gbDeviceApps = new ArrayList<>();

        for (final Watchface.InstalledWatchfaceInfo watchfaceInfo : installedWatchfaceInfoList) {
            final UUID uuid = toWatchfaceUUID(watchfaceInfo.fileName);
            GBDeviceApp gbDeviceApp = new GBDeviceApp(
                    uuid,
                    watchfacesNames.get(watchfaceInfo.fileName),
                    "",
                    "",
                    GBDeviceApp.Type.WATCHFACE
            );
            gbDeviceApps.add(gbDeviceApp);
        }

        final GBDeviceEventAppInfo appInfoCmd = new GBDeviceEventAppInfo();
        appInfoCmd.apps = gbDeviceApps.toArray(new GBDeviceApp[0]);
        support.evaluateGBDeviceEvent(appInfoCmd);
    }

    public void updateWatchfaceNames() {
        Request.RequestCallback finalizeReq = new Request.RequestCallback() {
            @Override
            public void call() {
                handleWatchfaceList();
            }

            @Override
            public void handleException(Request.ResponseParseException e) {
                LOG.error("Watchface update list exception", e);
            }
        };

        try {
            GetWatchfacesNames getWatchfacesNames = new GetWatchfacesNames(support, installedWatchfaceInfoList);
            getWatchfacesNames.setFinalizeReq(finalizeReq);
            getWatchfacesNames.doPerform();
        } catch (IOException e) {
            LOG.error("Could not get watchface names", e);
        }

    }

    public void requestWatchfaceList() {
        Request.RequestCallback finalizeReq = new Request.RequestCallback() {
            @Override
            public void call() {
                updateWatchfaceNames();
            }

            @Override
            public void handleException(Request.ResponseParseException e) {
                LOG.error("Watchface update list exception", e);
            }
        };


        try {
            GetWatchfacesList getWatchfacesList = new GetWatchfacesList(support);
            getWatchfacesList.setFinalizeReq(finalizeReq);
            getWatchfacesList.doPerform();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    };

    public void setWatchface(UUID uuid) {
        Request.RequestCallback finalizeReq = new Request.RequestCallback() {
            @Override
            public void call() {
                requestWatchfaceList();
            }

            @Override
            public void handleException(Request.ResponseParseException e) {
                LOG.error("Watchface update list exception", e);
            }
        };

        try {
            SendWatchfaceOperation sendWatchfaceOperation = new SendWatchfaceOperation(support,
                    getFullFileName(uuid),
                    Watchface.WatchfaceOperation.operationActive);
            sendWatchfaceOperation.setFinalizeReq(finalizeReq);
            sendWatchfaceOperation.doPerform();
        } catch (IOException e) {
            LOG.error("Could not set watchface ", getFullFileName(uuid), e );
        }
    }

    public void deleteWatchface(UUID uuid) {
        Request.RequestCallback finalizeReq = new Request.RequestCallback() {
            @Override
            public void call() {
                requestWatchfaceList();
            }

            @Override
            public void handleException(Request.ResponseParseException e) {
                LOG.error("Watchface update list exception", e);
            }
        };

        try {
            SendWatchfaceOperation sendWatchfaceOperation = new SendWatchfaceOperation(support,
                    getFullFileName(uuid),
                    Watchface.WatchfaceOperation.operationDelete);
            sendWatchfaceOperation.setFinalizeReq(finalizeReq);
            sendWatchfaceOperation.doPerform();
        } catch (IOException e) {
            LOG.error("Could not delete watchface", getFullFileName(uuid), e);
        }
    }

    private String getFullFileName(UUID uuid) {
        String name = toWatchfaceId(uuid);
        String version = "";
        for (final Watchface.InstalledWatchfaceInfo watchfaceInfo : installedWatchfaceInfoList) {
            if (watchfaceInfo.fileName.equals(name)) {
                version = watchfaceInfo.version;
                break;
            }
        }
        String filename = name + "_" + version;
        return filename;
    }
}
