/*  Copyright (C) 2020-2024 Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.um25.Data;

import java.io.Serializable;

public class MeasurementData implements Serializable {
    private int voltage; // voltage in millivolts
    private int current; // current in milliampere
    private int wattage; // wattage in milliwatt
    private int temperatureCelcius;
    private int temperatureFahreheit;
    private CaptureGroup[] captureGroups;
    private int voltageDataPositive;
    private int voltageDataNegative;
    private int chargedCurrent; // charged current in milliAmpereHours
    private int chargedWattage; // charged current in milliWattHours
    private int thresholdCurrent; // threshold current for charging detection
    private int chargingSeconds;
    private int cableResistance; // cable resistance in ohms

    public MeasurementData(int voltage, int current, int wattage, int temperatureCelcius, int temperatureFahreheit, CaptureGroup[] captureGroups, int voltageDataPositive, int voltageDataNegative, int chargedCurrent, int chargedWattage, int thresholdCurrent, int chargingSeconds, int cableResistance) {
        this.voltage = voltage;
        this.current = current;
        this.wattage = wattage;
        this.temperatureCelcius = temperatureCelcius;
        this.temperatureFahreheit = temperatureFahreheit;
        this.captureGroups = captureGroups;
        this.voltageDataPositive = voltageDataPositive;
        this.voltageDataNegative = voltageDataNegative;
        this.chargedCurrent = chargedCurrent;
        this.chargedWattage = chargedWattage;
        this.thresholdCurrent = thresholdCurrent;
        this.chargingSeconds = chargingSeconds;
        this.cableResistance = cableResistance;
    }

    public int getVoltage() {
        return voltage;
    }

    public int getCurrent() {
        return current;
    }

    public int getWattage() {
        return wattage;
    }

    public int getTemperatureCelcius() {
        return temperatureCelcius;
    }

    public int getTemperatureFahreheit() {
        return temperatureFahreheit;
    }

    public CaptureGroup[] getCaptureGroups() {
        return captureGroups;
    }

    public int getVoltageDataPositive() {
        return voltageDataPositive;
    }

    public int getVoltageDataNegative() {
        return voltageDataNegative;
    }

    public int getChargedCurrent() {
        return chargedCurrent;
    }

    public int getChargedWattage() {
        return chargedWattage;
    }

    public int getThresholdCurrent() {
        return thresholdCurrent * 10;
    }

    public int getChargingSeconds() {
        return chargingSeconds;
    }

    public int getCableResistance() {
        return cableResistance;
    }
}