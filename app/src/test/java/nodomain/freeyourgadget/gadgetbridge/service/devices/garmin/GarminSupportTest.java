package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FieldDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitFile;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.GlobalFITMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;
import nodomain.freeyourgadget.gadgetbridge.test.TestBase;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class GarminSupportTest extends TestBase {
    @Test
    public void testBaseFields() {

        RecordDefinition recordDefinition = new RecordDefinition(new RecordHeader((byte) 6), ByteOrder.LITTLE_ENDIAN, GlobalFITMessage.WEATHER, null, null); //just some random data
        List<FieldDefinition> fieldDefinitionList = new ArrayList<>();
        for (BaseType baseType :
                BaseType.values()) {
            fieldDefinitionList.add(new FieldDefinition(baseType.getIdentifier(), baseType.getSize(), baseType, baseType.name()));

        }
        recordDefinition.setFieldDefinitions(fieldDefinitionList);

        RecordData test = new RecordData(recordDefinition, recordDefinition.getRecordHeader());

        for (BaseType baseType :
                BaseType.values()) {
            System.out.println(baseType.getIdentifier());
            Object startVal, endVal;

            switch (baseType.name()) {
                case "ENUM":
                case "UINT8":
                case "BASE_TYPE_BYTE":
                    startVal = 0;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = (int) 0xff - 1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = -1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    break;
                case "SINT8":
                    startVal = (int) Byte.MIN_VALUE;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = (int) Byte.MAX_VALUE - 1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = (int) Byte.MIN_VALUE - 1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    break;
                case "SINT16":
                    startVal = (int) Short.MIN_VALUE;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = (int) Short.MAX_VALUE - 1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = (int) Short.MIN_VALUE - 1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    break;
                case "UINT16":
                    startVal = 0;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = (int) 0xffff - 1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = -1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    break;
                case "SINT32":
                    startVal = (long) Integer.MIN_VALUE;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = (long) Integer.MAX_VALUE - 1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = (long) Integer.MIN_VALUE - 1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    break;
                case "UINT32":
                    startVal = 0L;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = (long) 0xffffffffL - 1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, (long) ((long) endVal & 0xffffffffL));
                    startVal = 0xffffffff;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    break;
                case "FLOAT32":
                    startVal = 0.0f;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = -Float.MAX_VALUE;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = Float.MAX_VALUE;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = (double) -Float.MAX_VALUE * 2;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    break;
                case "FLOAT64":
                    startVal = 0.0d;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = Double.MIN_VALUE;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = Double.MAX_VALUE;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = (double) -Double.MAX_VALUE * 2;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    break;
                case "UINT8Z":
                    startVal = 1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = (int) 0xff;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = 0;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    startVal = -1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    break;
                case "UINT16Z":
                    startVal = 1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = (int) 0xffff;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = -1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    startVal = 0;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    break;
                case "UINT32Z":
                    startVal = 1L;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = (long) 0xffffffffL;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, (long) ((long) endVal & 0xffffffffL));
                    startVal = -1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    startVal = 0;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    break;
                case "SINT64":
                    startVal = BigInteger.valueOf(Long.MIN_VALUE);
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(((BigInteger) startVal).longValue(), endVal);
                    startVal = BigInteger.valueOf(Long.MAX_VALUE - 1);
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(((BigInteger) startVal).longValue(), endVal);
                    startVal = BigInteger.valueOf(Long.MAX_VALUE);
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    break;
                case "UINT64":
                    startVal = 0L;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = BigInteger.valueOf(0xFFFFFFFFFFFFFFFFL - 1);
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(((BigInteger) startVal).longValue() & 0xFFFFFFFFFFFFFFFFL, endVal);
                    startVal = BigInteger.valueOf(0xFFFFFFFFFFFFFFFFL);
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    break;
                case "UINT64Z":
                    startVal = 1L;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = BigInteger.valueOf(0xFFFFFFFFFFFFFFFFL);
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(((BigInteger) startVal).longValue() & 0xFFFFFFFFFFFFFFFFL, endVal);
                    startVal = 0L;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    break;
                case "STRING":
                    //TODO
                    break;
                default:
                    System.out.println(baseType.name());
                    Assert.assertFalse(true); //we should not end up here, if it happen we forgot a case in the switch
            }

        }

    }

    @Test
    public void TestFitFileSettings2() {

        byte[] fileContents = GB.hexStringToByteArray("0e101405b90600002e464954b18b40000000000603048c04048601028402" +
                "028405028400010000ed2adce7ffffffff01001906ffff02410000310002" +
                "000284010102015401ff4200000200160104860204860001020301000401" +
                "000501010a01000b01000c01000d01020e01020f01021001001101001201" +
                "001501001601001a01001b01001d01003401003501000200000000000000" +
                "0000000000030002000032ffffff0100fe00000001430000030013000807" +
                "0402840101000201020301020501000601000701000801020a01020b0102" +
                "0c01000d01000e0100100100110100120100150100180102036564676535" +
                "3130000c030129b70000003cb9b901000001a80200ff440000040004fe02" +
                "8401028b00010203010a04000058c3010145000006002400040703048627" +
                "040a290c0afe028404028b05028b06028b07028b0802840902840a02840b" +
                "02842a028b0101000201000c01020d01020e01020f010210010211010212" +
                "010213010214010215010a16010a17010a18010a23030224010025010226" +
                "010a28010a2b010a2c01000545564f00849eb90227350000171513121110" +
                "0f0e0d0c0b00000000000000000001ba300800005000f4010000ffff0101" +
                "0000000001fe01000000050032ff04ff020b000046000006002400050703" +
                "048627040a290c0afe028404028b05028b06028b07028b0802840902840a" +
                "02840b02842a028b0101000201000c01020d01020e01020f010210010211" +
                "010212010213010214010215010a16010a17010a18010a23030224010025" +
                "010226010a28010a2b010a2c0100065032534c0000000000273500001715" +
                "131211100f0e0d0c0b000100000000000000316e300800005a00f4010000" +
                "ffff01010000000001fe01000000050076be04ff020b0000470000060024" +
                "00090703048627040a290c0afe028404028b05028b06028b07028b080284" +
                "0902840a02840b02842a028b0101000201000c01020d01020e01020f0102" +
                "10010211010212010213010214010215010a16010a17010a18010a230302" +
                "24010025010226010a28010a2b010a2c0100074c414e47535445520013cc" +
                "1200273500001715131211100f0e0d0c0b000200000000000000632a3008" +
                "00005f00f4010000ffff010100000000010001000000050032ff04ff020b" +
                "000048000006002400020703048627040a290c0afe028404028b05028b06" +
                "028b07028b0802840902840a02840b02842a028b0101000201000c01020d" +
                "01020e01020f010210010211010212010213010214010215010a16010a17" +
                "010a18010a23030224010025010226010a28010a2b010a2c0100084d0000" +
                "000000352700001715131211100f0e0d0c0b000300000000000000697a30" +
                "0800005f00f4010000ffff010100000000010001000000050032ff04ff02" +
                "0b000049000006002400070703048627040a290c0afe028404028b05028b" +
                "06028b07028b0802840902840a02840b02842a028b0101000201000c0102" +
                "0d01020e01020f010210010211010212010213010214010215010a16010a" +
                "17010a18010a23030224010025010226010a28010a2b010a2c0100094269" +
                "6b6520350000000000273500001715131211100f0e0d0c0b000400000000" +
                "0000000000300800005f00f4010000ffff01010000000000fe0000000000" +
                "0032ff04ff020b00000942696b6520360000000000273500001715131211" +
                "100f0e0d0c0b0005000000000000000000300800005f00f4010000ffff01" +
                "010000000000fe00000000000032ff04ff020b00000942696b6520370000" +
                "000000273500001715131211100f0e0d0c0b000600000000000000000030" +
                "0800005f00f4010000ffff01010000000000fe00000000000032ff04ff02" +
                "0b00000942696b6520380000000000273500001715131211100f0e0d0c0b" +
                "0007000000000000000000300800005f00f4010000ffff01010000000000" +
                "fe00000000000032ff04ff020b00000942696b6520390000000000273500" +
                "001715131211100f0e0d0c0b0008000000000000000000300800005f00f4" +
                "010000ffff01010000000000fe00000000000032ff04ff020b00004a0000" +
                "06002400080703048627040a290c0afe028404028b05028b06028b07028b" +
                "0802840902840a02840b02842a028b0101000201000c01020d01020e0102" +
                "0f010210010211010212010213010214010215010a16010a17010a18010a" +
                "23030224010025010226010a28010a2b010a2c01000a42696b6520313000" +
                "00000000273500001715131211100f0e0d0c0b0009000000000000000000" +
                "300800005f00f4010000ffff01010000000000fe00000000000032ff04ff" +
                "020b00004b00007f00090309070001000401000501000601000701000801" +
                "000901000a01000b45646765203531300000ffffffffffffff09ef");//https://github.com/polyvertex/fitdecode/blob/48b6554d8a3baf33f8b5b9b2fd079fcbe9ac8ce2/tests/files/Settings2.fit

        String expectedOutput = "[" +
                "FitFileId{serial_number=3889965805, manufacturer=1, product=1561, type=SETTINGS}, " +
                "FitFileCreator{software_version=340}, " +
                "FitDeviceSettings{utc_offset=0, time_offset=0, active_time_zone=0, unknown_3(ENUM/1)=0, time_mode=0, time_zone_offset=0, unknown_10(ENUM/1)=3, unknown_11(ENUM/1)=0, backlight_mode=2, unknown_13(UINT8/1)=0, unknown_14(UINT8/1)=0, unknown_15(UINT8/1)=50, unknown_21(ENUM/1)=1, unknown_22(ENUM/1)=0, unknown_26(ENUM/1)=254, unknown_27(ENUM/1)=0, unknown_29(ENUM/1)=0, unknown_52(ENUM/1)=0, unknown_53(ENUM/1)=1}, " +
                "FitUserProfile{friendly_name=edge510, weight=78.0, gender=1, age=41, height=183, language=english, elev_setting=metric, weight_setting=metric, resting_heart_rate=60, default_max_biking_heart_rate=185, default_max_heart_rate=185, hr_setting=1, speed_setting=metric, dist_setting=metric, power_setting=1, activity_class=168, position_setting=2, temperature_setting=metric}, " +
                "RecordData{UNK_4, unknown_254(UINT16/2)=0, unknown_1(UINT16Z/2)=50008, unknown_0(UINT8/1)=1, unknown_3(UINT8Z/1)=1}, " +
                "RecordData{UNK_6, unknown_0(STRING/4)=EVO, unknown_3(UINT32/4)=45719172, unknown_39(UINT8Z/4)=[39,53,,], unknown_41(UINT8Z/12)=[23,21,19,18,17,16,15,14,13,12,11,], unknown_254(UINT16/2)=0, unknown_7(UINT16Z/2)=47617, unknown_8(UINT16/2)=2096, unknown_9(UINT16/2)=0, unknown_10(UINT16/2)=80, unknown_11(UINT16/2)=500, unknown_12(UINT8/1)=1, unknown_13(UINT8/1)=1, unknown_14(UINT8/1)=0, unknown_15(UINT8/1)=0, unknown_16(UINT8/1)=0, unknown_17(UINT8/1)=0, unknown_18(UINT8/1)=1, unknown_19(UINT8/1)=254, unknown_20(UINT8/1)=1, unknown_24(UINT8Z/1)=5, unknown_35(UINT8/3)=[0,50,], unknown_36(ENUM/1)=4, unknown_38(UINT8Z/1)=2, unknown_40(UINT8Z/1)=11, unknown_44(ENUM/1)=0}, " +
                "RecordData{UNK_6, unknown_0(STRING/5)=P2SL, unknown_3(UINT32/4)=0, unknown_39(UINT8Z/4)=[39,53,,], unknown_41(UINT8Z/12)=[23,21,19,18,17,16,15,14,13,12,11,], unknown_254(UINT16/2)=1, unknown_7(UINT16Z/2)=28209, unknown_8(UINT16/2)=2096, unknown_9(UINT16/2)=0, unknown_10(UINT16/2)=90, unknown_11(UINT16/2)=500, unknown_12(UINT8/1)=1, unknown_13(UINT8/1)=1, unknown_14(UINT8/1)=0, unknown_15(UINT8/1)=0, unknown_16(UINT8/1)=0, unknown_17(UINT8/1)=0, unknown_18(UINT8/1)=1, unknown_19(UINT8/1)=254, unknown_20(UINT8/1)=1, unknown_24(UINT8Z/1)=5, unknown_35(UINT8/3)=[0,118,190], unknown_36(ENUM/1)=4, unknown_38(UINT8Z/1)=2, unknown_40(UINT8Z/1)=11, unknown_44(ENUM/1)=0}, " +
                "RecordData{UNK_6, unknown_0(STRING/9)=LANGSTER, unknown_3(UINT32/4)=1231891, unknown_39(UINT8Z/4)=[39,53,,], unknown_41(UINT8Z/12)=[23,21,19,18,17,16,15,14,13,12,11,], unknown_254(UINT16/2)=2, unknown_7(UINT16Z/2)=10851, unknown_8(UINT16/2)=2096, unknown_9(UINT16/2)=0, unknown_10(UINT16/2)=95, unknown_11(UINT16/2)=500, unknown_12(UINT8/1)=1, unknown_13(UINT8/1)=1, unknown_14(UINT8/1)=0, unknown_15(UINT8/1)=0, unknown_16(UINT8/1)=0, unknown_17(UINT8/1)=0, unknown_18(UINT8/1)=1, unknown_19(UINT8/1)=0, unknown_20(UINT8/1)=1, unknown_24(UINT8Z/1)=5, unknown_35(UINT8/3)=[0,50,], unknown_36(ENUM/1)=4, unknown_38(UINT8Z/1)=2, unknown_40(UINT8Z/1)=11, unknown_44(ENUM/1)=0}, " +
                "RecordData{UNK_6, unknown_0(STRING/2)=M, unknown_3(UINT32/4)=0, unknown_39(UINT8Z/4)=[53,39,,], unknown_41(UINT8Z/12)=[23,21,19,18,17,16,15,14,13,12,11,], unknown_254(UINT16/2)=3, unknown_7(UINT16Z/2)=31337, unknown_8(UINT16/2)=2096, unknown_9(UINT16/2)=0, unknown_10(UINT16/2)=95, unknown_11(UINT16/2)=500, unknown_12(UINT8/1)=1, unknown_13(UINT8/1)=1, unknown_14(UINT8/1)=0, unknown_15(UINT8/1)=0, unknown_16(UINT8/1)=0, unknown_17(UINT8/1)=0, unknown_18(UINT8/1)=1, unknown_19(UINT8/1)=0, unknown_20(UINT8/1)=1, unknown_24(UINT8Z/1)=5, unknown_35(UINT8/3)=[0,50,], unknown_36(ENUM/1)=4, unknown_38(UINT8Z/1)=2, unknown_40(UINT8Z/1)=11, unknown_44(ENUM/1)=0}, " +
                "RecordData{UNK_6, unknown_0(STRING/7)=Bike 5, unknown_3(UINT32/4)=0, unknown_39(UINT8Z/4)=[39,53,,], unknown_41(UINT8Z/12)=[23,21,19,18,17,16,15,14,13,12,11,], unknown_254(UINT16/2)=4, unknown_8(UINT16/2)=2096, unknown_9(UINT16/2)=0, unknown_10(UINT16/2)=95, unknown_11(UINT16/2)=500, unknown_12(UINT8/1)=1, unknown_13(UINT8/1)=1, unknown_14(UINT8/1)=0, unknown_15(UINT8/1)=0, unknown_16(UINT8/1)=0, unknown_17(UINT8/1)=0, unknown_18(UINT8/1)=0, unknown_19(UINT8/1)=254, unknown_20(UINT8/1)=0, unknown_35(UINT8/3)=[0,50,], unknown_36(ENUM/1)=4, unknown_38(UINT8Z/1)=2, unknown_40(UINT8Z/1)=11, unknown_44(ENUM/1)=0}, " +
                "RecordData{UNK_6, unknown_0(STRING/7)=Bike 6, unknown_3(UINT32/4)=0, unknown_39(UINT8Z/4)=[39,53,,], unknown_41(UINT8Z/12)=[23,21,19,18,17,16,15,14,13,12,11,], unknown_254(UINT16/2)=5, unknown_8(UINT16/2)=2096, unknown_9(UINT16/2)=0, unknown_10(UINT16/2)=95, unknown_11(UINT16/2)=500, unknown_12(UINT8/1)=1, unknown_13(UINT8/1)=1, unknown_14(UINT8/1)=0, unknown_15(UINT8/1)=0, unknown_16(UINT8/1)=0, unknown_17(UINT8/1)=0, unknown_18(UINT8/1)=0, unknown_19(UINT8/1)=254, unknown_20(UINT8/1)=0, unknown_35(UINT8/3)=[0,50,], unknown_36(ENUM/1)=4, unknown_38(UINT8Z/1)=2, unknown_40(UINT8Z/1)=11, unknown_44(ENUM/1)=0}, " +
                "RecordData{UNK_6, unknown_0(STRING/7)=Bike 7, unknown_3(UINT32/4)=0, unknown_39(UINT8Z/4)=[39,53,,], unknown_41(UINT8Z/12)=[23,21,19,18,17,16,15,14,13,12,11,], unknown_254(UINT16/2)=6, unknown_8(UINT16/2)=2096, unknown_9(UINT16/2)=0, unknown_10(UINT16/2)=95, unknown_11(UINT16/2)=500, unknown_12(UINT8/1)=1, unknown_13(UINT8/1)=1, unknown_14(UINT8/1)=0, unknown_15(UINT8/1)=0, unknown_16(UINT8/1)=0, unknown_17(UINT8/1)=0, unknown_18(UINT8/1)=0, unknown_19(UINT8/1)=254, unknown_20(UINT8/1)=0, unknown_35(UINT8/3)=[0,50,], unknown_36(ENUM/1)=4, unknown_38(UINT8Z/1)=2, unknown_40(UINT8Z/1)=11, unknown_44(ENUM/1)=0}, " +
                "RecordData{UNK_6, unknown_0(STRING/7)=Bike 8, unknown_3(UINT32/4)=0, unknown_39(UINT8Z/4)=[39,53,,], unknown_41(UINT8Z/12)=[23,21,19,18,17,16,15,14,13,12,11,], unknown_254(UINT16/2)=7, unknown_8(UINT16/2)=2096, unknown_9(UINT16/2)=0, unknown_10(UINT16/2)=95, unknown_11(UINT16/2)=500, unknown_12(UINT8/1)=1, unknown_13(UINT8/1)=1, unknown_14(UINT8/1)=0, unknown_15(UINT8/1)=0, unknown_16(UINT8/1)=0, unknown_17(UINT8/1)=0, unknown_18(UINT8/1)=0, unknown_19(UINT8/1)=254, unknown_20(UINT8/1)=0, unknown_35(UINT8/3)=[0,50,], unknown_36(ENUM/1)=4, unknown_38(UINT8Z/1)=2, unknown_40(UINT8Z/1)=11, unknown_44(ENUM/1)=0}, " +
                "RecordData{UNK_6, unknown_0(STRING/7)=Bike 9, unknown_3(UINT32/4)=0, unknown_39(UINT8Z/4)=[39,53,,], unknown_41(UINT8Z/12)=[23,21,19,18,17,16,15,14,13,12,11,], unknown_254(UINT16/2)=8, unknown_8(UINT16/2)=2096, unknown_9(UINT16/2)=0, unknown_10(UINT16/2)=95, unknown_11(UINT16/2)=500, unknown_12(UINT8/1)=1, unknown_13(UINT8/1)=1, unknown_14(UINT8/1)=0, unknown_15(UINT8/1)=0, unknown_16(UINT8/1)=0, unknown_17(UINT8/1)=0, unknown_18(UINT8/1)=0, unknown_19(UINT8/1)=254, unknown_20(UINT8/1)=0, unknown_35(UINT8/3)=[0,50,], unknown_36(ENUM/1)=4, unknown_38(UINT8Z/1)=2, unknown_40(UINT8Z/1)=11, unknown_44(ENUM/1)=0}, " +
                "RecordData{UNK_6, unknown_0(STRING/8)=Bike 10, unknown_3(UINT32/4)=0, unknown_39(UINT8Z/4)=[39,53,,], unknown_41(UINT8Z/12)=[23,21,19,18,17,16,15,14,13,12,11,], unknown_254(UINT16/2)=9, unknown_8(UINT16/2)=2096, unknown_9(UINT16/2)=0, unknown_10(UINT16/2)=95, unknown_11(UINT16/2)=500, unknown_12(UINT8/1)=1, unknown_13(UINT8/1)=1, unknown_14(UINT8/1)=0, unknown_15(UINT8/1)=0, unknown_16(UINT8/1)=0, unknown_17(UINT8/1)=0, unknown_18(UINT8/1)=0, unknown_19(UINT8/1)=254, unknown_20(UINT8/1)=0, unknown_35(UINT8/3)=[0,50,], unknown_36(ENUM/1)=4, unknown_38(UINT8Z/1)=2, unknown_40(UINT8Z/1)=11, unknown_44(ENUM/1)=0}, " +
                "FitConnectivity{name=Edge 510, bluetooth_enabled=0}" +
                "]";

        FitFile fitFile = FitFile.parseIncoming(fileContents);
        Assert.assertEquals(expectedOutput, fitFile.toString());

    }

    @Test
    public void TestFitFileDevelopersField() {
        byte[] fileContents = GB.hexStringToByteArray("0e206806a20000002e464954bed040000100000401028400010002028403048c00000f042329000006a540000100cf0201100d030102000101020305080d1522375990e97962db0040000100ce05000102010102020102031107080a0700000001646f7567686e7574735f6561726e656400646f7567686e7574730060000100140403010204010205048606028401000100008c580000c738b98001008f5a00032c808e400200905c0005a9388a1003d39e");//https://github.com/polyvertex/fitdecode/blob/48b6554d8a3baf33f8b5b9b2fd079fcbe9ac8ce2/tests/files/DeveloperData.fit

        String expectedOutput = "[" +
                "FitFileId{manufacturer=15, type=ACTIVITY, product=9001, serial_number=1701}, " +
                "FitDeveloperData{application_id=[1,1,2,3,5,8,13,21,34,55,89,144,233,121,98,219], developer_data_index=0}, " +
                "FitFieldDescription{developer_data_index=0, field_definition_number=0, fit_base_type_id=1, field_name=doughnuts_earned, units=doughnuts}, " +
                "FitRecord{heart_rate=140, cadence=88, distance=510.0, speed=47.488, doughnuts_earned=1}, " +
                "FitRecord{heart_rate=143, cadence=90, distance=2080.0, speed=36.416, doughnuts_earned=2}, " +
                "FitRecord{heart_rate=144, cadence=92, distance=3710.0, speed=35.344, doughnuts_earned=3}" +
                "]";

        FitFile fitFile = FitFile.parseIncoming(fileContents);
        Assert.assertEquals(expectedOutput, fitFile.toString());
    }
}
