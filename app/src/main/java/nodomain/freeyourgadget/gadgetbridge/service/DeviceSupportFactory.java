/*  Copyright (C) 2015-2021 0nse, 115ek, Andreas Böhler, Andreas Shimokawa,
    angelpup, Carsten Pfeiffer, Cre3per, criogenic, DanialHanif, Daniel Dakhno,
    Daniele Gobbetti, Dmytro Bielik, Gordon Williams, Jean-François Greffier,
    João Paulo Barraca, José Rebelo, ladbsoft, Manuel Ruß, maxirnilian,
    mkusnierz, odavo32nof, opavlov, pangwalla, Pavel Elagin, protomors,
    Quallenauge, Sami Alaoui, Sebastian Kranz, Sergey Trofimov, Sophanimus,
    Taavi Eomäe, tiparega, Vadim Kaushan, Yukai Li

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
package nodomain.freeyourgadget.gadgetbridge.service;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.widget.Toast;

import java.lang.reflect.Constructor;
import java.util.EnumSet;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.fitpro.FitProDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.banglejs.BangleJSDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.casio.CasioGB6900DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.casio.CasioGBX100DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.domyos.DomyosT540Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.galaxy_buds.GalaxyBudsDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.hplus.HPlusSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitband5.AmazfitBand5Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitbip.AmazfitBipLiteSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitbip.AmazfitBipSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitbips.AmazfitBipSLiteSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitbips.AmazfitBipSSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitbipu.AmazfitBipUSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitbipupro.AmazfitBipUProSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitcor.AmazfitCorSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitcor2.AmazfitCor2Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitgtr.AmazfitGTRLiteSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitgtr.AmazfitGTRSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitgtr2.AmazfitGTR2Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitgtr2.AmazfitGTR2eSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitgts.AmazfitGTSSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitgts2.AmazfitGTS2MiniSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitgts2.AmazfitGTS2Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitgts2.AmazfitGTS2eSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitneo.AmazfitNeoSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitpop.AmazfitPopSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitpoppro.AmazfitPopProSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfittrex.AmazfitTRexSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfittrexpro.AmazfitTRexProSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitvergel.AmazfitVergeLSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitx.AmazfitXSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.miband2.MiBand2Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.miband3.MiBand3Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.miband4.MiBand4Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.miband5.MiBand5Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.miband6.MiBand6Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppe.ZeppESupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.id115.ID115Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.itag.ITagSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.jyou.BFH16DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.jyou.TeclastH30.TeclastH30Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.jyou.y5.Y5Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.lefun.LefunDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.lenovo.watchxplus.WatchXPlusDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.liveview.LiveviewSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.makibeshr3.MakibesHR3DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.MiBandSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.mijia_lywsd02.MijiaLywsd02Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miscale2.MiScale2DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.no1f1.No1F1Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.nothing.Ear1Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.nut.NutSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.pebble.PebbleSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.pinetime.PineTimeJFSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qc35.QC35BaseSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.roidmi.RoidmiSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.smaq2oss.SMAQ2OSSSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.SonyHeadphonesSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.SonySWR12DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.tlw64.TLW64Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.um25.Support.UM25Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vesc.VescDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vibratissimo.VibratissimoSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.waspos.WaspOSDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.watch9.Watch9DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xwatch.XWatchSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.zetime.ZeTimeDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class DeviceSupportFactory {
    private final BluetoothAdapter mBtAdapter;
    private final Context mContext;

    DeviceSupportFactory(Context context) {
        mContext = context;
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public synchronized DeviceSupport createDeviceSupport(GBDevice device) throws GBException {
        DeviceSupport deviceSupport;
        String deviceAddress = device.getAddress();
        int indexFirstColon = deviceAddress.indexOf(":");
        if (indexFirstColon > 0) {
            if (indexFirstColon == deviceAddress.lastIndexOf(":")) { // only one colon
                deviceSupport = createTCPDeviceSupport(device);
            } else {
                // multiple colons -- bt?
                deviceSupport = createBTDeviceSupport(device);
            }
        } else {
            // no colon at all, maybe a class name?
            deviceSupport = createClassNameDeviceSupport(device);
        }

        if (deviceSupport != null) {
            return deviceSupport;
        }

        // no device found, check transport availability and warn
        checkBtAvailability();
        return null;
    }

    private DeviceSupport createClassNameDeviceSupport(GBDevice device) throws GBException {
        String className = device.getAddress();
        try {
            Class<?> deviceSupportClass = Class.forName(className);
            Constructor<?> constructor = deviceSupportClass.getConstructor();
            DeviceSupport support = (DeviceSupport) constructor.newInstance();
            // has to create the device itself
            support.setContext(device, null, mContext);
            return support;
        } catch (ClassNotFoundException e) {
            return null; // not a class, or not known at least
        } catch (Exception e) {
            throw new GBException("Error creating DeviceSupport instance for " + className, e);
        }
    }

    private void checkBtAvailability() {
        if (mBtAdapter == null) {
            GB.toast(mContext.getString(R.string.bluetooth_is_not_supported_), Toast.LENGTH_SHORT, GB.WARN);
        } else if (!mBtAdapter.isEnabled()) {
            GB.toast(mContext.getString(R.string.bluetooth_is_disabled_), Toast.LENGTH_SHORT, GB.WARN);
        }
    }

    private ServiceDeviceSupport createServiceDeviceSupport(GBDevice device){
        switch (device.getType()) {
            case PEBBLE:
                return new ServiceDeviceSupport(new PebbleSupport());
            case MIBAND:
                return new ServiceDeviceSupport(new MiBandSupport(), ServiceDeviceSupport.Flags.THROTTLING, ServiceDeviceSupport.Flags.BUSY_CHECKING);
            case MIBAND2:
                return new ServiceDeviceSupport(new MiBand2Support(), ServiceDeviceSupport.Flags.THROTTLING, ServiceDeviceSupport.Flags.BUSY_CHECKING);
            case MIBAND3:
                return new ServiceDeviceSupport(new MiBand3Support());
            case MIBAND4:
                return new ServiceDeviceSupport(new MiBand4Support());
            case MIBAND5:
                return new ServiceDeviceSupport(new MiBand5Support());
            case MIBAND6:
                return new ServiceDeviceSupport(new MiBand6Support());
            case AMAZFITBIP:
                return new ServiceDeviceSupport(new AmazfitBipSupport());
            case AMAZFITBIP_LITE:
                return new ServiceDeviceSupport(new AmazfitBipLiteSupport());
            case AMAZFITBIPS:
                return new ServiceDeviceSupport(new AmazfitBipSSupport());
            case AMAZFITBIPS_LITE:
                return new ServiceDeviceSupport(new AmazfitBipSLiteSupport());
            case AMAZFITBIPU:
                return new ServiceDeviceSupport(new AmazfitBipUSupport());
            case AMAZFITBIPUPRO:
                return new ServiceDeviceSupport(new AmazfitBipUProSupport());
            case AMAZFITPOP:
                return new ServiceDeviceSupport(new AmazfitPopSupport());
            case AMAZFITPOPPRO:
                return new ServiceDeviceSupport(new AmazfitPopProSupport());
            case AMAZFITGTR:
                return new ServiceDeviceSupport(new AmazfitGTRSupport());
            case AMAZFITGTR_LITE:
                return new ServiceDeviceSupport(new AmazfitGTRLiteSupport());
            case AMAZFITGTR2:
                return new ServiceDeviceSupport(new AmazfitGTR2Support());
            case ZEPP_E:
                return new ServiceDeviceSupport(new ZeppESupport());
            case AMAZFITGTR2E:
                return new ServiceDeviceSupport(new AmazfitGTR2eSupport());
            case AMAZFITTREX:
                return new ServiceDeviceSupport(new AmazfitTRexSupport());
            case AMAZFITTREXPRO:
                return new ServiceDeviceSupport(new AmazfitTRexProSupport());
            case AMAZFITGTS:
                return new ServiceDeviceSupport(new AmazfitGTSSupport());
            case AMAZFITVERGEL:
                return new ServiceDeviceSupport(new AmazfitVergeLSupport());
            case AMAZFITGTS2:
                return new ServiceDeviceSupport(new AmazfitGTS2Support());
            case AMAZFITGTS2_MINI:
                return new ServiceDeviceSupport(new AmazfitGTS2MiniSupport());
            case AMAZFITGTS2E:
                return new ServiceDeviceSupport(new AmazfitGTS2eSupport());
            case AMAZFITCOR:
                return new ServiceDeviceSupport(new AmazfitCorSupport());
            case AMAZFITCOR2:
                return new ServiceDeviceSupport(new AmazfitCor2Support());
            case AMAZFITBAND5:
                return new ServiceDeviceSupport(new AmazfitBand5Support());
            case AMAZFITX:
                return new ServiceDeviceSupport(new AmazfitXSupport());
            case AMAZFITNEO:
                return new ServiceDeviceSupport(new AmazfitNeoSupport());
            case VIBRATISSIMO:
                return new ServiceDeviceSupport(new VibratissimoSupport(), ServiceDeviceSupport.Flags.THROTTLING, ServiceDeviceSupport.Flags.BUSY_CHECKING);
            case LIVEVIEW:
                return new ServiceDeviceSupport(new LiveviewSupport());
            case HPLUS:
            case MAKIBESF68:
            case EXRIZUK8:
            case Q8:
                return new ServiceDeviceSupport(new HPlusSupport(device.getType()));
            case NO1F1:
                return new ServiceDeviceSupport(new No1F1Support());
            case TECLASTH30:
                return new ServiceDeviceSupport(new TeclastH30Support());
            case XWATCH:
                return new ServiceDeviceSupport(new XWatchSupport());
            case FOSSILQHYBRID:
                return new ServiceDeviceSupport(new QHybridSupport());
            case ZETIME:
                return new ServiceDeviceSupport(new ZeTimeDeviceSupport());
            case ID115:
                return new ServiceDeviceSupport(new ID115Support());
            case WATCH9:
                return new ServiceDeviceSupport(new Watch9DeviceSupport(), ServiceDeviceSupport.Flags.THROTTLING, ServiceDeviceSupport.Flags.BUSY_CHECKING);
            case WATCHXPLUS:
                return new ServiceDeviceSupport(new WatchXPlusDeviceSupport(), ServiceDeviceSupport.Flags.THROTTLING, ServiceDeviceSupport.Flags.BUSY_CHECKING);
            case ROIDMI:
                return new ServiceDeviceSupport(new RoidmiSupport());
            case ROIDMI3:
                return new ServiceDeviceSupport(new RoidmiSupport());
            case Y5:
                return new ServiceDeviceSupport(new Y5Support());
            case CASIOGB6900:
                return new ServiceDeviceSupport(new CasioGB6900DeviceSupport());
            case CASIOGBX100:
                return new ServiceDeviceSupport(new CasioGBX100DeviceSupport());
            case MISCALE2:
                return new ServiceDeviceSupport(new MiScale2DeviceSupport());
            case BFH16:
                return new ServiceDeviceSupport(new BFH16DeviceSupport());
            case MIJIA_LYWSD02:
                return new ServiceDeviceSupport(new MijiaLywsd02Support());
            case MAKIBESHR3:
                return new ServiceDeviceSupport(new MakibesHR3DeviceSupport());
            case ITAG:
                return new ServiceDeviceSupport(new ITagSupport());
            case NUTMINI:
                return new ServiceDeviceSupport(new NutSupport());
            case BANGLEJS:
                return new ServiceDeviceSupport(new BangleJSDeviceSupport());
            case TLW64:
                return new ServiceDeviceSupport(new TLW64Support());
            case PINETIME_JF:
                return new ServiceDeviceSupport(new PineTimeJFSupport());
            case SG2:
                return new ServiceDeviceSupport(new HPlusSupport(DeviceType.SG2));
            case LEFUN:
                return new ServiceDeviceSupport(new LefunDeviceSupport());
            case SONY_SWR12:
                return new ServiceDeviceSupport(new SonySWR12DeviceSupport());
            case WASPOS:
                return new ServiceDeviceSupport(new WaspOSDeviceSupport());
            case SMAQ2OSS:
                return new ServiceDeviceSupport(new SMAQ2OSSSupport());
            case UM25:
                return new ServiceDeviceSupport(new UM25Support());
            case DOMYOS_T540:
                return new ServiceDeviceSupport(new DomyosT540Support(), ServiceDeviceSupport.Flags.THROTTLING, ServiceDeviceSupport.Flags.BUSY_CHECKING);
            case FITPRO:
                return new ServiceDeviceSupport(new FitProDeviceSupport(), ServiceDeviceSupport.Flags.THROTTLING, ServiceDeviceSupport.Flags.BUSY_CHECKING);
            case NOTHING_EAR1:
                return new ServiceDeviceSupport(new Ear1Support());
            case GALAXY_BUDS:
                return new ServiceDeviceSupport(new GalaxyBudsDeviceSupport());
            case GALAXY_BUDS_LIVE:
                return new ServiceDeviceSupport(new GalaxyBudsDeviceSupport());
            case GALAXY_BUDS_PRO:
                return new ServiceDeviceSupport(new GalaxyBudsDeviceSupport(), ServiceDeviceSupport.Flags.BUSY_CHECKING);
            case SONY_WH_1000XM3:
                return new ServiceDeviceSupport(new SonyHeadphonesSupport());
            case SONY_WH_1000XM4:
                return new ServiceDeviceSupport(new SonyHeadphonesSupport());
            case SONY_WF_SP800N:
                return new ServiceDeviceSupport(new SonyHeadphonesSupport(), ServiceDeviceSupport.Flags.BUSY_CHECKING);
            case SONY_WF_1000XM3:
                return new ServiceDeviceSupport(new SonyHeadphonesSupport());
            case VESC_NRF:
            case VESC_HM10:
                return new ServiceDeviceSupport(new VescDeviceSupport(device.getType()));
            case BOSE_QC35:
                return new ServiceDeviceSupport(new QC35BaseSupport());
        }
        return null;
    }

    private DeviceSupport createBTDeviceSupport(GBDevice gbDevice) throws GBException {
        if (mBtAdapter != null && mBtAdapter.isEnabled()) {
            try {
                DeviceSupport deviceSupport = createServiceDeviceSupport(gbDevice);
                if (deviceSupport != null) {
                    deviceSupport.setContext(gbDevice, mBtAdapter, mContext);
                    return deviceSupport;
                }
            } catch (Exception e) {
                throw new GBException(mContext.getString(R.string.cannot_connect_bt_address_invalid_), e);
            }
        }
        return null;
    }

    private DeviceSupport createTCPDeviceSupport(GBDevice gbDevice) throws GBException {
        try {
            DeviceSupport deviceSupport = new ServiceDeviceSupport(new PebbleSupport(), ServiceDeviceSupport.Flags.BUSY_CHECKING);
            deviceSupport.setContext(gbDevice, mBtAdapter, mContext);
            return deviceSupport;
        } catch (Exception e) {
            throw new GBException("cannot connect to " + gbDevice, e); // FIXME: localize
        }
    }

}
