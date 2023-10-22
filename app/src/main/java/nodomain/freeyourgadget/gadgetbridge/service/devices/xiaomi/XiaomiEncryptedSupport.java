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
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;

public class XiaomiEncryptedSupport extends XiaomiSupport {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiEncryptedSupport.class);

    public static final String BASE_UUID = "0000%s-0000-1000-8000-00805f9b34fb";

    public static final UUID UUID_SERVICE_XIAOMI_FE95 = UUID.fromString((String.format(BASE_UUID, "fe95")));
    public static final UUID UUID_CHARACTERISTIC_XIAOMI_UNKNOWN_0050 = UUID.fromString((String.format(BASE_UUID, "0050")));
    public static final UUID UUID_CHARACTERISTIC_XIAOMI_COMMAND_READ = UUID.fromString((String.format(BASE_UUID, "0051")));
    public static final UUID UUID_CHARACTERISTIC_XIAOMI_COMMAND_WRITE = UUID.fromString((String.format(BASE_UUID, "0052")));
    public static final UUID UUID_CHARACTERISTIC_XIAOMI_ACTIVITY_DATA = UUID.fromString((String.format(BASE_UUID, "0053")));
    public static final UUID UUID_CHARACTERISTIC_XIAOMI_UNKNOWN_0054 = UUID.fromString((String.format(BASE_UUID, "0054")));
    public static final UUID UUID_CHARACTERISTIC_XIAOMI_DATA_UPLOAD = UUID.fromString((String.format(BASE_UUID, "0055")));
    public static final UUID UUID_CHARACTERISTIC_XIAOMI_UNKNOWN_0056 = UUID.fromString((String.format(BASE_UUID, "0056")));
    public static final UUID UUID_CHARACTERISTIC_XIAOMI_UNKNOWN_0057 = UUID.fromString((String.format(BASE_UUID, "0057")));
    public static final UUID UUID_CHARACTERISTIC_XIAOMI_UNKNOWN_0058 = UUID.fromString((String.format(BASE_UUID, "0058")));
    public static final UUID UUID_CHARACTERISTIC_XIAOMI_UNKNOWN_0059 = UUID.fromString((String.format(BASE_UUID, "0059")));
    public static final UUID UUID_CHARACTERISTIC_XIAOMI_UNKNOWN_005A = UUID.fromString((String.format(BASE_UUID, "005a")));

    public static final UUID UUID_SERVICE_XIAOMI_FDAB = UUID.fromString((String.format(BASE_UUID, "fdab")));
    public static final UUID UUID_CHARACTERISTIC_XIAOMI_UNKNOWN_0001 = UUID.fromString((String.format(BASE_UUID, "0001")));
    public static final UUID UUID_CHARACTERISTIC_XIAOMI_UNKNOWN_0002 = UUID.fromString((String.format(BASE_UUID, "0002")));
    public static final UUID UUID_CHARACTERISTIC_XIAOMI_UNKNOWN_0003 = UUID.fromString((String.format(BASE_UUID, "0003")));

    public XiaomiEncryptedSupport() {
        super();
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ACCESS);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ATTRIBUTE);
        addSupportedService(GattService.UUID_SERVICE_DEVICE_INFORMATION);
        addSupportedService(GattService.UUID_SERVICE_HUMAN_INTERFACE_DEVICE);
        addSupportedService(UUID_SERVICE_XIAOMI_FE95);
        addSupportedService(UUID_SERVICE_XIAOMI_FDAB);
    }

    @Override
    protected boolean isEncrypted() {
        return true;
    }

    @Override
    protected UUID getCharacteristicCommandRead() {
        return UUID_CHARACTERISTIC_XIAOMI_COMMAND_READ;
    }

    @Override
    protected UUID getCharacteristicCommandWrite() {
        return UUID_CHARACTERISTIC_XIAOMI_COMMAND_WRITE;
    }

    @Override
    protected UUID getCharacteristicActivityData() {
        return UUID_CHARACTERISTIC_XIAOMI_ACTIVITY_DATA;
    }

    @Override
    protected UUID getCharacteristicDataUpload() {
        return UUID_CHARACTERISTIC_XIAOMI_DATA_UPLOAD;
    }

    @Override
    protected void startAuthentication(final TransactionBuilder builder) {
        authService.startEncryptedHandshake(builder);
    }

    @Override
    public String customStringFilter(final String inputString) {
        // TODO: Do this more efficiently - it iterates the input string 88 times...
        String customString = inputString;
        for (Map.Entry<String, String> emoji : EMOJI_MAP.entrySet()) {
            customString = customString.replaceAll(emoji.getKey(), emoji.getValue());
        }
        return customString;
    }

    private static final Map<String, String> EMOJI_MAP = new LinkedHashMap<String, String>() {{
        put("\uD83D\uDE0D", "ꀂ"); // 😍
        put("\uD83D\uDE18", "ꀃ"); // 😘
        put("\uD83D\uDE02", "ꀄ"); // 😂
        put("\uD83D\uDE0A", "ꀅ"); // 😊
        put("\uD83D\uDE0E", "ꀆ"); // 😎
        put("\uD83D\uDE09", "ꀇ"); // 😉
        put("\uD83D\uDC8B", "ꀈ"); // 💋
        put("\uD83D\uDC4D", "ꀉ"); // 👍
        put("\uD83E\uDD23", "ꀊ"); // 🤣
        put("\uD83D\uDC95", "ꀋ"); // 💕
        put("\uD83D\uDE00", "ꀌ"); // 😀
        put("\uD83D\uDE04", "ꀍ"); // 😄
        put("\uD83D\uDE2D", "ꀎ"); // 😭
        put("\uD83E\uDD7A", "ꀏ"); // 🥺
        put("\uD83D\uDE4F", "ꀑ"); // 🙏
        put("\uD83E\uDD70", "ꀒ"); // 🥰
        put("\uD83E\uDD14", "ꀓ"); // 🤔
        put("\uD83D\uDD25", "ꀔ"); // 🔥
        put("\uD83D\uDE29", "ꀗ"); // 😩
        put("\uD83D\uDE14", "ꀘ"); // 😔
        put("\uD83D\uDE01", "ꀙ"); // 😁
        put("\uD83D\uDC4C", "ꀚ"); // 👌
        put("\uD83D\uDE0F", "ꀛ"); // 😏
        put("\uD83D\uDE05", "ꀜ"); // 😅
        put("\uD83E\uDD0D", "ꀝ"); // 🤍
        put("\uD83D\uDC94", "ꀞ"); // 💔
        put("\uD83D\uDE0C", "ꀟ"); // 😌
        put("\uD83D\uDE22", "ꀠ"); // 😢
        put("\uD83D\uDC99", "ꀡ"); // 💙
        put("\uD83D\uDC9C", "ꀢ"); // 💜
        put("\uD83C\uDFB6", "ꀤ"); // 🎶
        put("\uD83D\uDE33", "ꀥ"); // 😳
        put("\uD83D\uDC96", "ꀦ"); // 💖
        put("\uD83D\uDE4C", "ꀧ"); // 🙌
        put("\uD83D\uDCAF", "ꀨ"); // 💯
        put("\uD83D\uDE48", "ꀩ"); // 🙈
        put("\uD83D\uDE0B", "ꀫ"); // 😋
        put("\uD83D\uDE11", "ꀬ"); // 😑
        put("\uD83D\uDE34", "ꀭ"); // 😴
        put("\uD83D\uDE2A", "ꀮ"); // 😪
        put("\uD83D\uDE1C", "ꀯ"); // 😜
        put("\uD83D\uDE1B", "ꀰ"); // 😛
        put("\uD83D\uDE1D", "ꀱ"); // 😝
        put("\uD83D\uDE1E", "ꀲ"); // 😞
        put("\uD83D\uDE15", "ꀳ"); // 😕
        put("\uD83D\uDC97", "ꀴ"); // 💗
        put("\uD83D\uDC4F", "ꀵ"); // 👏
        put("\uD83D\uDE10", "ꀶ"); // 😐
        put("\uD83D\uDC49", "ꀷ"); // 👉
        put("\uD83D\uDC9B", "ꀸ"); // 💛
        put("\uD83D\uDC9E", "ꀹ"); // 💞
        put("\uD83D\uDCAA", "ꀺ"); // 💪
        put("\uD83C\uDF39", "ꀻ"); // 🌹
        put("\uD83D\uDC80", "ꀼ"); // 💀
        put("\uD83D\uDE31", "ꀽ"); // 😱
        put("\uD83D\uDC98", "ꀾ"); // 💘
        put("\uD83E\uDD1F", "ꀿ"); // 🤟
        put("\uD83D\uDE21", "ꁀ"); // 😡
        put("\uD83D\uDCF7", "ꁁ"); // 📷
        put("\uD83C\uDF38", "ꁂ"); // 🌸
        put("\uD83D\uDE08", "ꁃ"); // 😈
        put("\uD83D\uDC48", "ꁄ"); // 👈
        put("\uD83C\uDF89", "ꁅ"); // 🎉
        put("\uD83D\uDC81", "ꁆ"); // 💁
        put("\uD83D\uDE4A", "ꁇ"); // 🙊
        put("\uD83D\uDC9A", "ꁈ"); // 💚
        put("\uD83D\uDE2B", "ꁉ"); // 😫
        put("\uD83D\uDE24", "ꁊ"); // 😤
        put("\uD83D\uDC93", "ꁍ"); // 💓
        put("\uD83C\uDF1A", "ꁎ"); // 🌚
        put("\uD83D\uDC47", "ꁏ"); // 👇
        put("\uD83D\uDE07", "ꁒ"); // 😇
        put("\uD83D\uDC4A", "ꁓ"); // 👊
        put("\uD83D\uDC51", "ꁔ"); // 👑
        put("\uD83D\uDE13", "ꁕ"); // 😓
        put("\uD83D\uDE3B", "ꁖ"); // 😻
        put("\uD83D\uDD34", "ꁗ"); // 🔴
        put("\uD83D\uDE25", "ꁘ"); // 😥
        put("\uD83E\uDD29", "ꁙ"); // 🤩
        put("\uD83D\uDE1A", "ꁚ"); // 😚
        put("\uD83D\uDE37", "ꁜ"); // 😷
        put("\uD83D\uDC4B", "ꁝ"); // 👋
        put("\uD83D\uDCA5", "ꁞ"); // 💥
        put("\uD83E\uDD2D", "ꁠ"); // 🤭
        put("\uD83C\uDF1F", "ꁡ"); // 🌟
        put("\uD83E\uDD71", "ꁢ"); // 🥱
        put("\uD83D\uDCA9", "ꁣ"); // 💩
        put("\uD83D\uDE80", "ꁤ"); // 🚀
    }};
}
