/*  Copyright (C) 2021 Arjan Schrijver, Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.devices.qhybrid;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;

import nodomain.freeyourgadget.gadgetbridge.util.CRC32C;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

/**
 * Writes watch apps to a file in the Fossil Hybrid HR .wapp format.
 */
public class FossilAppWriter {
    private final Logger LOG = LoggerFactory.getLogger(FossilAppWriter.class);
    private Context mContext;
    private String version;
    private LinkedHashMap<String, InputStream> code;
    private LinkedHashMap<String, InputStream> icons;
    private LinkedHashMap<String, String> layout;
    private LinkedHashMap<String, String> displayName;
    private LinkedHashMap<String, String> config;

    public FossilAppWriter(Context context, String version, LinkedHashMap<String, InputStream> code, LinkedHashMap<String, InputStream> icons, LinkedHashMap<String, String> layout, LinkedHashMap<String, String> displayName, LinkedHashMap<String, String> config) {
        this.mContext = context;
        if (this.mContext == null) throw new AssertionError("context cannot be null");
        this.version = version;
        if (!this.version.matches("^[0-9]\\.[0-9]\\.[0-9]\\.[0-9]$")) throw new AssertionError("Version must be in x.x.x.x format");
        this.code = code;
        if (this.code.size() == 0) throw new AssertionError("At least one code file InputStream must be supplied");
        this.icons = icons;
        if (this.icons == null) throw new AssertionError("icons cannot be null");
        this.layout = layout;
        if (this.layout == null) throw new AssertionError("layout cannot be null");
        this.displayName = displayName;
        if (this.displayName == null) throw new AssertionError("displayName cannot be null");
        this.config = config;
        if (this.config == null) throw new AssertionError("config cannot be null");
    }

    public byte[] getWapp() throws IOException {
        byte[] codeData = loadFiles(code);
        byte[] iconsData = loadFiles(icons);
        byte[] layoutData = loadStringFiles(layout);
        byte[] displayNameData = loadStringFiles(displayName);
        byte[] configData = loadStringFiles(config);

        int offsetCode = 88;
        int offsetIcons = offsetCode + codeData.length;
        int offsetLayout = offsetIcons + iconsData.length;
        int offsetDisplayName = offsetLayout + layoutData.length;
        int offsetConfig = offsetDisplayName + displayNameData.length;
        int offsetFileEnd = offsetConfig + configData.length;

        ByteArrayOutputStream filePart = new ByteArrayOutputStream();
        String[] versionParts = this.version.split("\\.");
        for (String versionPart : versionParts) {
            filePart.write(Integer.valueOf(versionPart).byteValue());
        }
        filePart.write(intToLEBytes(0));
        filePart.write(intToLEBytes(0));
        filePart.write(intToLEBytes(offsetCode));
        filePart.write(intToLEBytes(offsetIcons));
        filePart.write(intToLEBytes(offsetLayout));
        filePart.write(intToLEBytes(offsetDisplayName));
        filePart.write(intToLEBytes(offsetDisplayName));
        filePart.write(intToLEBytes(offsetConfig));
        filePart.write(intToLEBytes(offsetFileEnd));
        filePart.write(intToLEBytes(0));
        filePart.write(intToLEBytes(0));
        filePart.write(intToLEBytes(0));
        filePart.write(intToLEBytes(0));
        filePart.write(intToLEBytes(0));
        filePart.write(intToLEBytes(0));
        filePart.write(intToLEBytes(0));
        filePart.write(intToLEBytes(0));
        filePart.write(intToLEBytes(0));
        filePart.write(codeData);
        filePart.write(iconsData);
        filePart.write(layoutData);
        filePart.write(displayNameData);
        filePart.write(configData);
        byte[] filePartBytes = filePart.toByteArray();

        ByteArrayOutputStream wapp = new ByteArrayOutputStream();
        wapp.write(new byte[]{(byte)0xFE, (byte)0x15});  // file handle
        wapp.write(new byte[]{(byte)0x03, (byte)0x00});  // file version
        wapp.write(intToLEBytes(0));  // file offset
        wapp.write(intToLEBytes(filePartBytes.length));
        wapp.write(filePartBytes);

        CRC32C crc = new CRC32C();
        crc.update(filePartBytes,0,filePartBytes.length);
        wapp.write(intToLEBytes((int)crc.getValue()));

        return wapp.toByteArray();
    }

    public byte[] loadFiles(LinkedHashMap<String, InputStream> filesMap) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (String filename : filesMap.keySet()) {
            InputStream in = filesMap.get(filename);
            output.write((byte)filename.length() + 1);
            output.write(StringUtils.terminateNull(filename).getBytes(StandardCharsets.UTF_8));
            output.write(shortToLEBytes((short)in.available()));
            byte[] fileBytes = new byte[in.available()];
            in.read(fileBytes);
            output.write(fileBytes);
        }
        return output.toByteArray();
    }

    public byte[] loadStringFiles(LinkedHashMap<String, String> stringsMap) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (String filename : stringsMap.keySet()) {
            output.write((byte)filename.length() + 1);
            output.write(StringUtils.terminateNull(filename).getBytes(StandardCharsets.UTF_8));
            output.write(shortToLEBytes((short)(stringsMap.get(filename).length() + 1)));
            output.write(StringUtils.terminateNull(stringsMap.get(filename)).getBytes(StandardCharsets.UTF_8));
        }
        return output.toByteArray();
    }

    private static byte[] intToLEBytes(int number) {
        byte[] b = new byte[4];
        b[0] = (byte) (number & 0xFF);
        b[1] = (byte) ((number >> 8) & 0xFF);
        b[2] = (byte) ((number >> 16) & 0xFF);
        b[3] = (byte) ((number >> 24) & 0xFF);
        return b;
    }

    private static byte[] shortToLEBytes(short number) {
        byte[] b = new byte[2];
        b[0] = (byte) (number & 0xFF);
        b[1] = (byte) ((number >> 8) & 0xFF);
        return b;
    }
}
