/*  Copyright (C) 2024 Martin.JM

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
package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;

import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class TestHuaweiTruSleepParser {

    @Test
    public void testParseState() {
        byte[] test = GB.hexStringToByteArray("0100000002000000000000000000000003000000050000000000000000000000");

        HuaweiTruSleepParser.TruSleepStatus[] expected = new HuaweiTruSleepParser.TruSleepStatus[] {
                new HuaweiTruSleepParser.TruSleepStatus(1, 2),
                new HuaweiTruSleepParser.TruSleepStatus(3, 5)
        };

        HuaweiTruSleepParser.TruSleepStatus[] result = HuaweiTruSleepParser.parseState(test);

        Assert.assertArrayEquals(expected, result);
    }
    @Test
    public void testParseDataPpg() {
        byte[] test1 = GB.hexStringToByteArray("02B000403B0B672AAADA13C109A915E707BE130C06E3112104F80F84182F0888109900390805121004850E4317EC07C911D4037610C701580E2A02310F6B022B100E0448119B075B154F099916390A07191A0E9B00D10E2D02A7105405AAE8FDE8FDBDDA92B767943B71104EE52AB907EE06AC08BD06BE067C07DB06CC05E2079B06C206E906920639063807E0056406D905E2067807870660074D03720A8406AA0624064005CA07DF062D088706D2084B0A00");
        byte[] test2 = GB.hexStringToByteArray("027500F61E0B6724BB0C016968685756585753545050535354565B595B5B5B5F5C5A5E5B61635D5E5B5756585656AADD401138452F7826FC1D61221F27DD2BFC1A7B248B11260A7A221E1C9128FE185025C827D41C1B206223E31B73126210EC22B620D212B232DC0A93115A322E279B116717F30E2E1500");
        byte[] test3 = GB.hexStringToByteArray("025A00B2180B671BBB410174706A726F6D6D747B757B776E6E6D6E6F777D77777670757570AA543D0E52276956406837B63C103EB22C6929C7265326EA2FCC2F743CC9380239673B9739EA2FC92CC526BB2E652D6433DD3506397E2F00");

        // TODO: Unlike PPGs with 0xBB compression the timestamps are not linear
        // The data also looks different, hmmm....
        ArrayList<HuaweiTruSleepParser.TruSleepDataPpg> expected = new ArrayList<>();
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728789362820L, (short)0xfde8));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728789336970L, (short)0xfde8));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728789367450L, (short)0xdabd));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728789332230L, (short)0xb792));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728789362540L, (short)0x9467));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728789327480L, (short)0x713b));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728789357790L, (short)0x4e10));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728789322570L, (short)0x2ae5));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728789352880L, (short)0x07b9));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728789374760L, (short)0x06ee));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728789332950L, (short)0x08ac));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728789354320L, (short)0x06bd));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728789313530L, (short)0x06be));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728789333050L, (short)0x077c));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728789358130L, (short)0x06db));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728789322400L, (short)0x05cc));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728789349170L, (short)0x07e2));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728789371550L, (short)0x069b));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728789332280L, (short)0x06c2));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728789357530L, (short)0x06e9));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728789321800L, (short)0x0692));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728789354140L, (short)0x0639));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728789316550L, (short)0x0738));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728789348720L, (short)0x05e0));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728789317540L, (short)0x0664));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728789350890L, (short)0x05d9));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728789318190L, (short)0x06e2));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728789353390L, (short)0x0778));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728789322380L, (short)0x0687));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728789356240L, (short)0x0760));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728789331470L, (short)0x034d));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728789366670L, (short)0x0a72));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728789335830L, (short)0x0684));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728789369850L, (short)0x06aa));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728789338170L, (short)0x0624));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728789376070L, (short)0x0540));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728789348100L, (short)0x07ca));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728789313550L, (short)0x06df));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728789349930L, (short)0x082d));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728789317570L, (short)0x0687));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728789354630L, (short)0x08d2));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728789325640L, (short)0x0a4b));

        HuaweiTruSleepParser.TruSleepData result = HuaweiTruSleepParser.parseData(test1);
        Assert.assertEquals(expected, result.dataPPGs);

        expected = new ArrayList<>();
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728782072680L, (short)0x40dd));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728782073730L, (short)0x3811));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728782074770L, (short)0x2f45));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728782075810L, (short)0x2678));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728782076680L, (short)0x1dfc));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728782077540L, (short)0x2261));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728782078420L, (short)0x271f));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728782079290L, (short)0x2bdd));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728782080120L, (short)0x1afc));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728782080960L, (short)0x247b));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728782081760L, (short)0x118b));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728782082560L, (short)0x0a26));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728782083390L, (short)0x227a));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728782084220L, (short)0x1c1e));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728782085060L, (short)0x2891));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728782085920L, (short)0x18fe));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728782086830L, (short)0x2550));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728782087720L, (short)0x27c8));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728782088630L, (short)0x1cd4));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728782089540L, (short)0x201b));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728782090450L, (short)0x2362));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728782091400L, (short)0x1be3));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728782092320L, (short)0x1273));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728782093220L, (short)0x1062));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728782094160L, (short)0x22ec));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728782095070L, (short)0x20b6));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728782096040L, (short)0x12d2));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728782097030L, (short)0x32b2));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728782097960L, (short)0x0adc));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728782098900L, (short)0x1193));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728782099810L, (short)0x325a));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728782100680L, (short)0x272e));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728782101540L, (short)0x119b));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728782102420L, (short)0x1767));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728782103280L, (short)0x0ef3));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728782104140L, (short)0x152e));

        result = HuaweiTruSleepParser.parseData(test2);
        Assert.assertEquals(expected, result.dataPPGs);

        expected = new ArrayList<>();
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728780469210L, (short)0x3d54));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728780470370L, (short)0x520e));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728780471490L, (short)0x6927));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728780472550L, (short)0x4056));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728780473690L, (short)0x3768));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728780474800L, (short)0x3cb6));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728780475890L, (short)0x3e10));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728780476980L, (short)0x2cb2));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728780478140L, (short)0x2969));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728780479370L, (short)0x26c7));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728780480540L, (short)0x2653));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728780481770L, (short)0x2fea));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728780482960L, (short)0x2fcc));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728780484060L, (short)0x3c74));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728780485160L, (short)0x38c9));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728780486250L, (short)0x3902));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728780487350L, (short)0x3b67));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728780488460L, (short)0x3997));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728780489650L, (short)0x2fea));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728780490900L, (short)0x2cc9));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728780492090L, (short)0x26c5));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728780493280L, (short)0x2ebb));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728780494460L, (short)0x2d65));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728780495580L, (short)0x3364));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728780496750L, (short)0x35dd));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728780497920L, (short)0x3906));
        expected.add(new HuaweiTruSleepParser.TruSleepDataPpg (1728780499040L, (short)0x2f7e));

        result = HuaweiTruSleepParser.parseData(test3);
        Assert.assertEquals(expected, result.dataPPGs);
    }

    @Test
    public void testParseDataAcc() {
        byte[] test = GB.hexStringToByteArray("0107F02A0B670000B6");

        ArrayList<HuaweiTruSleepParser.TruSleepDataAcc> expected = new ArrayList<>();
        expected.add(new HuaweiTruSleepParser.TruSleepDataAcc (0x670b2af0, (short)0x0000));

        HuaweiTruSleepParser.TruSleepData result = HuaweiTruSleepParser.parseData(test);
        Assert.assertEquals(0, result.dataPPGs.size());
        Assert.assertEquals(expected, result.dataACCs);
    }

    @Test
    public void testParsePadding() {
        byte[] test1 = GB.hexStringToByteArray("000107F02A0B670000B6");
        byte[] test2 = GB.hexStringToByteArray("00000000000107F02A0B670000B6");

        ArrayList<HuaweiTruSleepParser.TruSleepDataAcc> expected = new ArrayList<>();
        expected.add(new HuaweiTruSleepParser.TruSleepDataAcc (0x670b2af0, (short)0x0000));

        HuaweiTruSleepParser.TruSleepData result = HuaweiTruSleepParser.parseData(test1);

        Assert.assertEquals(0, result.dataPPGs.size());
        Assert.assertEquals(expected, result.dataACCs);

        result = HuaweiTruSleepParser.parseData(test2);

        Assert.assertEquals(0, result.dataPPGs.size());
        Assert.assertEquals(expected, result.dataACCs);
    }

    @Test
    public void testParseFailure() {
        ArrayList<byte[]> tests = new ArrayList<byte[]>();
        // Length shorter than expected
        tests.add(GB.hexStringToByteArray("0105F02A0B670000B6"));
        // Zero length
        tests.add(GB.hexStringToByteArray("0100F02A0B670000B6"));
        // Length bigger than input data
        tests.add(GB.hexStringToByteArray("0108F02A0B670000B6"));

        // Length bigger than input data
        tests.add(GB.hexStringToByteArray("027600F61E0B6724BB0C016968685756585753545050535354565B595B5B5B5F5C5A5E5B61635D5E5B5756585656AADD401138452F7826FC1D61221F27DD2BFC1A7B248B11260A7A221E1C9128FE185025C827D41C1B206223E31B73126210EC22B620D212B232DC0A93115A322E279B116717F30E2E1500"));
        // Number of UINT16 less than provided
        tests.add(GB.hexStringToByteArray("027500F61E0B6723BB0C016968685756585753545050535354565B595B5B5B5F5C5A5E5B61635D5E5B5756585656AADD401138452F7826FC1D61221F27DD2BFC1A7B248B11260A7A221E1C9128FE185025C827D41C1B206223E31B73126210EC22B620D212B232DC0A93115A322E279B116717F30E2E1500"));
        // Wrong compression tag
        tests.add(GB.hexStringToByteArray("025A00B2180B671BCC410174706A726F6D6D747B757B776E6E6D6E6F777D77777670757570AA543D0E52276956406837B63C103EB22C6929C7265326EA2FCC2F743CC9380239673B9739EA2FC92CC526BB2E652D6433DD3506397E2F00"));
        // Wrong compression tag 2
        tests.add(GB.hexStringToByteArray("025A00B2180B671BBB410174706A726F6D6D747B757B776E6E6D6E6F777D77777670757570DD543D0E52276956406837B63C103EB22C6929C7265326EA2FCC2F743CC9380239673B9739EA2FC92CC526BB2E652D6433DD3506397E2F00"));
        // Wrong restart tags in compressed data
        tests.add(GB.hexStringToByteArray("025A00B2180B671BBB410FFFFFFA726F6D6D747B757B776E6E6D6E6F777D77777670757570AA543D0E52276956406837B63C103EB22C6929C7265326EA2FCC2F743CC9380239673B9739EA2FC92CC526BB2E652D6433DD3506397E2F00"));

        for (byte[] test: tests) {
            HuaweiTruSleepParser.TruSleepData result = HuaweiTruSleepParser.parseData(test);

            Assert.assertEquals("Test input data " + GB.hexdump(test), 0, result.dataPPGs.size());
            Assert.assertEquals("Test input data " + GB.hexdump(test),0, result.dataACCs.size());
        }
    }
}
