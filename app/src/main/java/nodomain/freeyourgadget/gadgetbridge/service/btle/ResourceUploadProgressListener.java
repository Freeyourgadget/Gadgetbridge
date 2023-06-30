/*  Copyright (C) 2016-2022 Andreas Shimokawa, Carsten Pfeiffer, JF, Sebastian
    Kranz, Taavi Eomäe

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
package nodomain.freeyourgadget.gadgetbridge.service.btle;

public class ResourceUploadProgressListener {
    public void onDeviceConnecting(final String mac) { }
    public void onDeviceConnected(final String mac) { }
    public void onUploadStarting(final String mac) { }
    public void onDeviceDisconnecting(final String mac) { }
    public void onDeviceDisconnected(final String mac) { }
    public void onUploadCompleted(final String mac) { }
    public void onUploadAborted(final String mac) { }
    public void onError(final String mac, int error, int errorType, final String message) { }
    public void onProgressChanged(final String mac,
                                  int percent,
                                  float speed,
                                  float averageSpeed,
                                  int segment,
                                  int totalSegments) { }
}
