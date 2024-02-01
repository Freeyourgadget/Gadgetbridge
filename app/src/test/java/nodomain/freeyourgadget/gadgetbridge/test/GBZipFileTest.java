package nodomain.freeyourgadget.gadgetbridge.test;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import nodomain.freeyourgadget.gadgetbridge.util.GBZipFile;
import nodomain.freeyourgadget.gadgetbridge.util.ZipFileException;

public class GBZipFileTest extends TestBase {
    private static final String TEST_FILE_NAME = "manifest.json";
    private static final String TEST_NESTED_FILE_NAME = "directory/manifest.json";
    private static final String TEST_FILE_CONTENTS_1 = "{ \"mykey\": \"myvalue\", \"myarr\": [0, 1, 2, 3] }";
    private static final String TEST_FILE_CONTENTS_2 = "{\n" +
        "    \"manifest\": {\n" +
        "        \"application\": {\n" +
        "            \"bin_file\": \"pinetime-mcuboot-app-image-1.10.0.bin\",\n" +
        "            \"dat_file\": \"pinetime-mcuboot-app-image-1.10.0.dat\",\n" +
        "            \"init_packet_data\": {\n" +
        "                \"application_version\": 4294967295,\n" +
        "                \"device_revision\": 65535,\n" +
        "                \"device_type\": 82,\n" +
        "                \"firmware_crc16\": 21770,\n" +
        "                \"softdevice_req\": [\n" +
        "                    65534\n" +
        "                ]\n" +
        "            }\n" +
        "        },\n" +
        "        \"dfu_version\": 0.5\n" +
        "    }\n" +
        "}";

    @Test
    public void testZipSize1() throws IOException, ZipFileException {
        final String contents = TEST_FILE_CONTENTS_1;

        byte[] zipArchive = createZipArchive(TEST_FILE_NAME, contents);

        GBZipFile zipFile = new GBZipFile(zipArchive);
        String readContents = new String(zipFile.getFileFromZip(TEST_FILE_NAME));

        Assert.assertEquals(contents, readContents);
    }

    @Test
    public void testZipSize2() throws IOException, ZipFileException {
        final String contents = TEST_FILE_CONTENTS_2;

        byte[] zipArchive = createZipArchive(TEST_FILE_NAME, contents);

        GBZipFile zipFile = new GBZipFile(zipArchive);
        String readContents = new String(zipFile.getFileFromZip(TEST_FILE_NAME));

        Assert.assertEquals(contents, readContents);
    }

    @Test
    public void testZipSize3() throws IOException, ZipFileException, JSONException {
        String contents = makeLargeJsonObject(new JSONObject(TEST_FILE_CONTENTS_2), 4).toString(4);

        byte[] zipArchive = createZipArchive(TEST_FILE_NAME, contents);

        GBZipFile zipFile = new GBZipFile(zipArchive);
        String readContents = new String(zipFile.getFileFromZip(TEST_FILE_NAME));

        Assert.assertEquals(contents, readContents);
    }

    @Test
    public void testZipSize4() throws IOException, ZipFileException, JSONException {
        String contents = makeLargeJsonObject(new JSONObject(TEST_FILE_CONTENTS_2), 32).toString(4);

        byte[] zipArchive = createZipArchive(TEST_FILE_NAME, contents);

        GBZipFile zipFile = new GBZipFile(zipArchive);
        String readContents = new String(zipFile.getFileFromZip(TEST_FILE_NAME));

        Assert.assertEquals(contents, readContents);
    }

    @Test
    public void testZipFileInDir() throws IOException, ZipFileException {
        String contents = TEST_FILE_CONTENTS_1;

        byte[] zipArchive = createZipArchive(TEST_NESTED_FILE_NAME, contents);

        GBZipFile zipFile = new GBZipFile(zipArchive);
        String readContents = new String(zipFile.getFileFromZip(TEST_NESTED_FILE_NAME));

        Assert.assertEquals(contents, readContents);
    }

    @Test
    public void testZipFilesUnorderedAccess() throws IOException, ZipFileException {
        String contents1 = TEST_FILE_CONTENTS_1;
        String contents2 = TEST_FILE_CONTENTS_2;
        String contents3 = "zbuMyWvIxeKgcWnsSYOd8CTLgjc9x7ti21OlLlGduMJVXlKc835WEUKJ3xR6GDA5d0tHSXnYxZkDlznFQVyueHhwYywsMO9PlkJqjOCA2Mn8uTuTliIKUNPBraFipOodb6rW31HdKLOd7gmniLF5mvdRPHOUKIXSMciqogOsZnvGXylMx6TegesGBWAeHFhTSQ5xXOrOEUsDHK78M3A0yFXzLE0XgwI90Tl87OHyWfE0y0yINv5PjxgGCLUB7mHYFpgPW1C5yyIkb2JA6CePE3hHv369khwmLumW7P9ErZhzdGgeskz6Os0p5HMrTFuySc0PWxsIfru1HldIH9TZTSMCbd91G5jCCikyx2zrzDKaasuQZyBGZcMjr1zcCLpPQiKT7ELSoUBCKhiFODxbFA06MC5bLXh2WvyP8W2kVxT2T4AnDX6pwf1BKs4nbHpAjvMmHrzlhQp7Q6VWBEiniY5M9QW4ExRcMGIBYXvY7vu5p";

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ZipOutputStream zipWriteStream = new ZipOutputStream(baos);

        writeFileToZip(contents1, "file1", zipWriteStream);
        writeFileToZip(contents2, "file2", zipWriteStream);
        writeFileToZip(contents3, "file3", zipWriteStream);
        zipWriteStream.close();

        GBZipFile zipFile = new GBZipFile(baos.toByteArray());
        String readContents2 = new String(zipFile.getFileFromZip("file2"));
        String readContents1 = new String(zipFile.getFileFromZip("file1"));
        String readContents3 = new String(zipFile.getFileFromZip("file3"));

        Assert.assertEquals(contents1, readContents1);
        Assert.assertEquals(contents2, readContents2);
        Assert.assertEquals(contents3, readContents3);
    }

    @Test
    public void testZipFilesFileExists() throws IOException, ZipFileException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ZipOutputStream zipWriteStream = new ZipOutputStream(baos);

        writeFileToZip(TEST_FILE_CONTENTS_1, "file1", zipWriteStream);
        writeFileToZip(TEST_FILE_CONTENTS_2, "file2", zipWriteStream);
        writeFileToZip("Hello, World!", "folder1/file3", zipWriteStream);
        zipWriteStream.close();

        final GBZipFile zipFile = new GBZipFile(baos.toByteArray());
        Assert.assertTrue(zipFile.fileExists("file2"));
        Assert.assertTrue(zipFile.fileExists("file1"));
        Assert.assertTrue(zipFile.fileExists("folder1/file3"));
        Assert.assertFalse(zipFile.fileExists("folder1"));
        Assert.assertFalse(zipFile.fileExists("file4"));
    }

    /**
     * Create a ZIP archive with a single text file.
     * The archive will not be saved to a file, it is kept in memory.
     *
     * @return the ZIP archive
     */
    private byte[] createZipArchive(String path, String fileContents) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ZipOutputStream zipFile = new ZipOutputStream(baos);

        writeFileToZip(fileContents, path, zipFile);
        zipFile.close();

        return baos.toByteArray();
    }

    /**
     * Make a larger JSON object for testing purposes, based on a preexisting JSON object.
     */
    private JSONObject makeLargeJsonObject(JSONObject base, int repetitions) throws JSONException {
        JSONObject manifestObj = base.getJSONObject("manifest");
        JSONArray array = new JSONArray();

        for (int i = 0; i < repetitions; i++) {
            array.put(manifestObj);
        }

        return base.put("array", array);
    }

    /**
     * Write given data to file at given path into an already opened ZIP archive.
     * Allows to create an archive with multiple files.
     */
    private void writeFileToZip(String fileContents, String path, ZipOutputStream zipFile) throws IOException {
        byte[] data = fileContents.getBytes(StandardCharsets.UTF_8);

        ZipEntry zipEntry = new ZipEntry(path);
        zipFile.putNextEntry(zipEntry);
        zipFile.write(data, 0, fileContents.length());
        zipFile.closeEntry();
    }
}
