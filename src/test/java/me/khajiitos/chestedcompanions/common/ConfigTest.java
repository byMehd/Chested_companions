package me.khajiitos.chestedcompanions.common;

import com.google.gson.JsonNull;
import com.google.gson.JsonParser;
import me.khajiitos.chestedcompanions.common.config.CCConfig;
import me.khajiitos.chestedcompanions.common.config.CCConfigValues;
import me.khajiitos.chestedcompanions.common.util.InventoryCapacity;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {
    @TempDir Path directory;

    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @BeforeEach void resetValues() throws Exception {
        for (var field : CCConfig.class.getDeclaredFields()) {
            if (field.isAnnotationPresent(CCConfig.Entry.class)) {
                var value = (CCConfigValues.Value<?>) field.get(null);
                value.setUnchecked(value.getDefault());
            }
        }
    }

    @Test void createsConfigurationAndRoundTripsEverySetting() throws Exception {
        CCConfig.init(directory.resolve("nested"));
        Path file = directory.resolve("nested/chestedcompanions_revived.json");
        assertTrue(Files.isRegularFile(file));
        CCConfig.publicChest.set(true);
        CCConfig.catInventoryCapacity.set(InventoryCapacity.THREE_ROWS);
        CCConfig.save();
        resetValues();
        CCConfig.load();
        assertTrue(CCConfig.publicChest.get());
        assertEquals(InventoryCapacity.THREE_ROWS, CCConfig.catInventoryCapacity.get());
        long entries = java.util.Arrays.stream(CCConfig.class.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(CCConfig.Entry.class)).count();
        assertEquals(entries, JsonParser.parseString(Files.readString(file)).getAsJsonObject().size());
        try (var files = Files.list(file.getParent())) {
            assertEquals(1, files.count(), "Atomic save must clean temporary files");
        }
    }

    @Test void migratesLegacyConfigurationWithoutChangingTheOriginal() throws Exception {
        String legacy = "{\"publicChest\":true,\"wolfInventoryCapacity\":\"two_rows\"}";
        Files.writeString(directory.resolve("chestedcompanions.json"), legacy);
        CCConfig.init(directory);
        assertTrue(CCConfig.publicChest.get());
        assertEquals(InventoryCapacity.TWO_ROWS, CCConfig.wolfInventoryCapacity.get());
        assertEquals(legacy, Files.readString(directory.resolve("chestedcompanions.json")));
    }

    @Test void preservesMalformedFilesAndDoesNotCrash() throws Exception {
        Path file = directory.resolve("chestedcompanions_revived.json");
        for (String input : new String[]{"", "null", "{oops", "[]"}) {
            Files.writeString(file, input);
            assertDoesNotThrow(() -> CCConfig.init(directory));
            assertEquals(input, Files.readString(file));
        }
    }

    @Test void invalidFieldsDoNotPreventOtherSettingsFromLoading() throws Exception {
        Files.writeString(directory.resolve("chestedcompanions_revived.json"),
                "{\"catInventoryCapacity\":{},\"publicChest\":true,\"hideCatChest\":null}");
        CCConfig.init(directory);
        assertEquals(InventoryCapacity.ONE_ROW, CCConfig.catInventoryCapacity.get());
        assertTrue(CCConfig.publicChest.get());
        assertFalse(CCConfig.hideCatChest.get());
    }

    @Test void booleanSettingsRejectStringsNumbersAndNull() {
        var value = new CCConfigValues.BooleanValue(true);
        assertTrue(value.read(JsonParser.parseString("\"false\"")));
        assertTrue(value.read(JsonParser.parseString("0")));
        assertTrue(value.read(JsonNull.INSTANCE));
        assertFalse(value.read(JsonParser.parseString("false")));
    }

    @Test void enumSettingsAreCaseInsensitiveAndLocaleIndependent() {
        var value = new CCConfigValues.EnumValue<>(InventoryCapacity.ONE_ROW);
        assertEquals(InventoryCapacity.TWO_ROWS, value.read(JsonParser.parseString("\"TWO_ROWS\"")));
        assertEquals(InventoryCapacity.ONE_ROW, value.read(JsonParser.parseString("\"invalid\"")));
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals("one_row", value.write().getAsString());
        } finally { Locale.setDefault(previous); }
    }
}
