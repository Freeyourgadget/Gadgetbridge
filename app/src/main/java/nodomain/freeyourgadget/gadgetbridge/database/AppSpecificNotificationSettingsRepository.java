/*  Copyright (C) 2023-2024 akasaka / Genjitsu Labs

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
package nodomain.freeyourgadget.gadgetbridge.database;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.entities.AppSpecificNotificationSettingDao;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.AppSpecificNotificationSetting;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

// TODO: Distinguish by device ID as well to allow for different settings per device.
// I didn't know how to get a device ID from a settings activity, so I left it as is.
public class AppSpecificNotificationSettingsRepository {
    private static final Logger LOG = LoggerFactory.getLogger(AppSpecificNotificationSettingsRepository.class);
    private GBDevice mGbDevice;
    public AppSpecificNotificationSettingsRepository(GBDevice gbDevice) {
        mGbDevice = gbDevice;
    }

    @Nullable
    public AppSpecificNotificationSetting getSettingsForAppId(String appId) {
        try (DBHandler db = GBApplication.acquireDB()) {
            DaoSession session = db.getDaoSession();
            QueryBuilder<AppSpecificNotificationSetting> qb = session.getAppSpecificNotificationSettingDao().queryBuilder();
            return qb.where(
                    qb.and(AppSpecificNotificationSettingDao.Properties.PackageId.eq(appId),
                            AppSpecificNotificationSettingDao.Properties.DeviceId.eq(DBHelper.findDevice(mGbDevice, session).getId())
                    )).build().unique();
        } catch (Exception e) {
            LOG.error("Failed to get DB handle", e);
        }
        return null;
    }

    private void deleteForAppId(@NonNull String appId) {
        try (DBHandler db = GBApplication.acquireDB()) {
            DaoSession session = db.getDaoSession();
            QueryBuilder<AppSpecificNotificationSetting> qb = session.getAppSpecificNotificationSettingDao().queryBuilder();
            qb.where(
                    qb.and(AppSpecificNotificationSettingDao.Properties.PackageId.eq(appId),
                            AppSpecificNotificationSettingDao.Properties.DeviceId.eq(DBHelper.findDevice(mGbDevice, session).getId())
            )).buildDelete().executeDeleteWithoutDetachingEntities();
        } catch (Exception e) {
            LOG.error("Failed to get DB handle", e);
        }
    }

    public void setSettingsForAppId(@NonNull String appId, @Nullable AppSpecificNotificationSetting settings) {
        if (settings == null) {
            deleteForAppId(appId);
        } else {
            settings.setPackageId(appId);
            try (DBHandler db = GBApplication.acquireDB()) {
                DaoSession session = db.getDaoSession();
                Device dbDevice = DBHelper.findDevice(mGbDevice, session);
                settings.setDeviceId(dbDevice.getId());
                session.getAppSpecificNotificationSettingDao().insertOrReplace(settings);
            } catch (Exception e) {
                LOG.error("Failed to get DB handle", e);
            }
        }
    }
}
