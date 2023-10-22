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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        return StringUtils.replaceEach(inputString, EMOJI_SOURCE, EMOJI_TARGET);
    }

    private static final String[] EMOJI_SOURCE = new String[] {
            "\uD83D\uDE0D", // 😍
            "\uD83D\uDE18", // 😘
            "\uD83D\uDE02", // 😂
            "\uD83D\uDE0A", // 😊
            "\uD83D\uDE0E", // 😎
            "\uD83D\uDE09", // 😉
            "\uD83D\uDC8B", // 💋
            "\uD83D\uDC4D", // 👍
            "\uD83E\uDD23", // 🤣
            "\uD83D\uDC95", // 💕
            "\uD83D\uDE00", // 😀
            "\uD83D\uDE04", // 😄
            "\uD83D\uDE2D", // 😭
            "\uD83E\uDD7A", // 🥺
            "\uD83D\uDE4F", // 🙏
            "\uD83E\uDD70", // 🥰
            "\uD83E\uDD14", // 🤔
            "\uD83D\uDD25", // 🔥
            "\uD83D\uDE29", // 😩
            "\uD83D\uDE14", // 😔
            "\uD83D\uDE01", // 😁
            "\uD83D\uDC4C", // 👌
            "\uD83D\uDE0F", // 😏
            "\uD83D\uDE05", // 😅
            "\uD83E\uDD0D", // 🤍
            "\uD83D\uDC94", // 💔
            "\uD83D\uDE0C", // 😌
            "\uD83D\uDE22", // 😢
            "\uD83D\uDC99", // 💙
            "\uD83D\uDC9C", // 💜
            "\uD83C\uDFB6", // 🎶
            "\uD83D\uDE33", // 😳
            "\uD83D\uDC96", // 💖
            "\uD83D\uDE4C", // 🙌
            "\uD83D\uDCAF", // 💯
            "\uD83D\uDE48", // 🙈
            "\uD83D\uDE0B", // 😋
            "\uD83D\uDE11", // 😑
            "\uD83D\uDE34", // 😴
            "\uD83D\uDE2A", // 😪
            "\uD83D\uDE1C", // 😜
            "\uD83D\uDE1B", // 😛
            "\uD83D\uDE1D", // 😝
            "\uD83D\uDE1E", // 😞
            "\uD83D\uDE15", // 😕
            "\uD83D\uDC97", // 💗
            "\uD83D\uDC4F", // 👏
            "\uD83D\uDE10", // 😐
            "\uD83D\uDC49", // 👉
            "\uD83D\uDC9B", // 💛
            "\uD83D\uDC9E", // 💞
            "\uD83D\uDCAA", // 💪
            "\uD83C\uDF39", // 🌹
            "\uD83D\uDC80", // 💀
            "\uD83D\uDE31", // 😱
            "\uD83D\uDC98", // 💘
            "\uD83E\uDD1F", // 🤟
            "\uD83D\uDE21", // 😡
            "\uD83D\uDCF7", // 📷
            "\uD83C\uDF38", // 🌸
            "\uD83D\uDE08", // 😈
            "\uD83D\uDC48", // 👈
            "\uD83C\uDF89", // 🎉
            "\uD83D\uDC81", // 💁
            "\uD83D\uDE4A", // 🙊
            "\uD83D\uDC9A", // 💚
            "\uD83D\uDE2B", // 😫
            "\uD83D\uDE24", // 😤
            "\uD83D\uDC93", // 💓
            "\uD83C\uDF1A", // 🌚
            "\uD83D\uDC47", // 👇
            "\uD83D\uDE07", // 😇
            "\uD83D\uDC4A", // 👊
            "\uD83D\uDC51", // 👑
            "\uD83D\uDE13", // 😓
            "\uD83D\uDE3B", // 😻
            "\uD83D\uDD34", // 🔴
            "\uD83D\uDE25", // 😥
            "\uD83E\uDD29", // 🤩
            "\uD83D\uDE1A", // 😚
            "\uD83D\uDE37", // 😷
            "\uD83D\uDC4B", // 👋
            "\uD83D\uDCA5", // 💥
            "\uD83E\uDD2D", // 🤭
            "\uD83C\uDF1F", // 🌟
            "\uD83E\uDD71", // 🥱
            "\uD83D\uDCA9", // 💩
            "\uD83D\uDE80", // 🚀
    };

    private static final String[] EMOJI_TARGET = new String[] {
            "ꀂ", // 😍
            "ꀃ", // 😘
            "ꀄ", // 😂
            "ꀅ", // 😊
            "ꀆ", // 😎
            "ꀇ", // 😉
            "ꀈ", // 💋
            "ꀉ", // 👍
            "ꀊ", // 🤣
            "ꀋ", // 💕
            "ꀌ", // 😀
            "ꀍ", // 😄
            "ꀎ", // 😭
            "ꀏ", // 🥺
            "ꀑ", // 🙏
            "ꀒ", // 🥰
            "ꀓ", // 🤔
            "ꀔ", // 🔥
            "ꀗ", // 😩
            "ꀘ", // 😔
            "ꀙ", // 😁
            "ꀚ", // 👌
            "ꀛ", // 😏
            "ꀜ", // 😅
            "ꀝ", // 🤍
            "ꀞ", // 💔
            "ꀟ", // 😌
            "ꀠ", // 😢
            "ꀡ", // 💙
            "ꀢ", // 💜
            "ꀤ", // 🎶
            "ꀥ", // 😳
            "ꀦ", // 💖
            "ꀧ", // 🙌
            "ꀨ", // 💯
            "ꀩ", // 🙈
            "ꀫ", // 😋
            "ꀬ", // 😑
            "ꀭ", // 😴
            "ꀮ", // 😪
            "ꀯ", // 😜
            "ꀰ", // 😛
            "ꀱ", // 😝
            "ꀲ", // 😞
            "ꀳ", // 😕
            "ꀴ", // 💗
            "ꀵ", // 👏
            "ꀶ", // 😐
            "ꀷ", // 👉
            "ꀸ", // 💛
            "ꀹ", // 💞
            "ꀺ", // 💪
            "ꀻ", // 🌹
            "ꀼ", // 💀
            "ꀽ", // 😱
            "ꀾ", // 💘
            "ꀿ", // 🤟
            "ꁀ", // 😡
            "ꁁ", // 📷
            "ꁂ", // 🌸
            "ꁃ", // 😈
            "ꁄ", // 👈
            "ꁅ", // 🎉
            "ꁆ", // 💁
            "ꁇ", // 🙊
            "ꁈ", // 💚
            "ꁉ", // 😫
            "ꁊ", // 😤
            "ꁍ", // 💓
            "ꁎ", // 🌚
            "ꁏ", // 👇
            "ꁒ", // 😇
            "ꁓ", // 👊
            "ꁔ", // 👑
            "ꁕ", // 😓
            "ꁖ", // 😻
            "ꁗ", // 🔴
            "ꁘ", // 😥
            "ꁙ", // 🤩
            "ꁚ", // 😚
            "ꁜ", // 😷
            "ꁝ", // 👋
            "ꁞ", // 💥
            "ꁠ", // 🤭
            "ꁡ", // 🌟
            "ꁢ", // 🥱
            "ꁣ", // 💩
            "ꁤ", // 🚀
    };
}
