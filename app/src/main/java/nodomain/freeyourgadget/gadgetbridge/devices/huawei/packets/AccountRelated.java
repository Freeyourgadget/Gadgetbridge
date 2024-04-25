/*  Copyright (C) 2024 Damien Gaignon, Vitalii Tomin

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
package nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;

public class AccountRelated {
	public static final byte id = 0x1A;

	public static class SendAccountToDevice {
		public static final byte id = 0x01;

		public static class Request extends HuaweiPacket {
			public Request (ParamsProvider paramsProvider, String account) {
				super(paramsProvider);

				this.serviceId = AccountRelated.id;
				this.commandId = id;

				this.tlv = new HuaweiTLV();
				if (account.length() > 0) {
					tlv.put(0x01, account);
				} else {
					tlv.put(0x01);
				}
				this.complete = true;
			}
		}

		public static class Response extends HuaweiPacket {
			public Response (ParamsProvider paramsProvider) {
				super(paramsProvider);
			}
		}
	}

	public static class SendExtendedAccountToDevice {
		public static final byte id = 0x05;

		public static class Request extends HuaweiPacket {
			public Request (ParamsProvider paramsProvider, boolean accountPairingOptimization, String account) {
				super(paramsProvider);

				this.serviceId = AccountRelated.id;
				this.commandId = id;

				this.tlv = new HuaweiTLV();
				if (account.length() > 0) {
					tlv.put(0x01, account);
				} else {
					tlv.put(0x01, (byte)0x00);
				}

				if (accountPairingOptimization) {
					this.tlv.put(0x03, (byte)0x01);
				}
				this.complete = true;
			}
		}

		public static class Response extends HuaweiPacket {
			public Response (ParamsProvider paramsProvider) {
				super(paramsProvider);
			}
		}
	}
}
