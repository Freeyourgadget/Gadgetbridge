package nodomain.freeyourgadget.gadgetbridge.util;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.test.TestBase;

public class ImportExportSharedPreferencesTest extends TestBase {
    @Test
    public void testExportImport() throws Exception {
        final SharedPreferences sharedPreferences = getContext().getSharedPreferences("testExportImport", Context.MODE_PRIVATE);

        assertEquals(0, sharedPreferences.getAll().size());

        final HashSet<String> EXPECTED_SET = new HashSet<>(Arrays.asList(
                "item 1",
                "item 2"
        ));

        sharedPreferences.edit()
                .putString("a_string", "Hello")
                .putBoolean("a_boolean", true)
                .putLong("a_long", 42)
                .putInt("an_int", 1337)
                .putFloat("a_float", 7f)
                .putStringSet("bip_display_items", EXPECTED_SET).commit();

        assertEquals(6, sharedPreferences.getAll().size());

        final File file = File.createTempFile("gb-ImportExportSharedPreferencesTest", "xml");
        file.deleteOnExit();
        ImportExportSharedPreferences.exportToFile(sharedPreferences, file);

        sharedPreferences.edit()
                .putInt("an_int", 3)
                .putString("this extra string", "should be gone after import")
                .commit();

        assertEquals(7, sharedPreferences.getAll().size());
        ImportExportSharedPreferences.importFromFile(sharedPreferences, file);

        assertEquals(6, sharedPreferences.getAll().size());
        assertEquals("Hello", sharedPreferences.getString("a_string", null));
        assertTrue(sharedPreferences.getBoolean("a_boolean", false));
        assertEquals(42, sharedPreferences.getLong("a_long", 0));
        assertEquals(1337, sharedPreferences.getInt("an_int", 0));
        assertEquals(7f, sharedPreferences.getFloat("a_float", 0), 0.01);

        final Set<String> actualSet = sharedPreferences.getStringSet("bip_display_items", Collections.emptySet());
        assertEquals(EXPECTED_SET.size(), actualSet.size());
        assertTrue(EXPECTED_SET.containsAll(actualSet));
        assertTrue(actualSet.containsAll(EXPECTED_SET));
    }
}
