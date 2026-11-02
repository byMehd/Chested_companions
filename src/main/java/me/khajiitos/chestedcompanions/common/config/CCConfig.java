package me.khajiitos.chestedcompanions.common.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.khajiitos.chestedcompanions.common.ChestedCompanions;
import me.khajiitos.chestedcompanions.common.util.InventoryCapacity;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;

public class CCConfig {
    private static File file = new File("config/chestedcompanions_revived.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @Entry
    public static final CCConfigValues.BooleanValue allowChestOnCats = new CCConfigValues.BooleanValue(true);

    @Entry
    public static final CCConfigValues.BooleanValue allowChestOnWolves = new CCConfigValues.BooleanValue(true);

    @Entry
    public static final CCConfigValues.BooleanValue invertShiftToOpen = new CCConfigValues.BooleanValue(false);

    @Entry
    public static final CCConfigValues.BooleanValue publicChest = new CCConfigValues.BooleanValue(false);

    @Entry
    public static final CCConfigValues.EnumValue<InventoryCapacity> catInventoryCapacity = new CCConfigValues.EnumValue<>(InventoryCapacity.ONE_ROW);

    @Entry
    public static final CCConfigValues.EnumValue<InventoryCapacity> wolfInventoryCapacity = new CCConfigValues.EnumValue<>(InventoryCapacity.ONE_ROW);

    @Entry(clientOnly = true)
    public static final CCConfigValues.BooleanValue hideCatChest = new CCConfigValues.BooleanValue(false);

    @Entry(clientOnly = true)
    public static final CCConfigValues.BooleanValue hideWolfChest = new CCConfigValues.BooleanValue(false);

    @Entry
    public static final CCConfigValues.BooleanValue allowChestOnBabyCat = new CCConfigValues.BooleanValue(false);

    @Entry
    public static final CCConfigValues.BooleanValue allowChestOnBabyWolf = new CCConfigValues.BooleanValue(false);

    @Entry(clientOnly = true)
    public static final CCConfigValues.BooleanValue showChestIconOnCats = new CCConfigValues.BooleanValue(false);

    @Entry(clientOnly = true)
    public static final CCConfigValues.BooleanValue showChestIconOnWolves = new CCConfigValues.BooleanValue(false);

    @Entry
    public static final CCConfigValues.BooleanValue feedingOverridesOpeningChest = new CCConfigValues.BooleanValue(true);

    public static void init() {
        init(java.nio.file.Path.of("config"));
    }

    public static void init(java.nio.file.Path configDirectory) {
        file = configDirectory.resolve("chestedcompanions_revived.json").toFile();
        java.nio.file.Path legacy = configDirectory.resolve("chestedcompanions.json");
        if (!file.exists() && java.nio.file.Files.isRegularFile(legacy)) {
            try {
                java.nio.file.Files.copy(legacy, file.toPath());
            } catch (IOException e) {
                ChestedCompanions.LOGGER.warn("Could not copy legacy configuration", e);
            }
        }
        if (!file.exists()) {
            save();
        } else {
            load();
        }
    }

    public static void save() {
        if (!file.getParentFile().isDirectory() && !file.getParentFile().mkdirs()) {
            ChestedCompanions.LOGGER.error("Failed to create config directory");
            return;
        }

        java.nio.file.Path temporary = null;
        try {
            temporary = java.nio.file.Files.createTempFile(file.toPath().toAbsolutePath().getParent(), "chestedcompanions-", ".tmp");
            JsonObject jsonObject = new JsonObject();

            for (Field field : CCConfig.class.getDeclaredFields()) {
                if (!field.isAnnotationPresent(Entry.class)) {
                    continue;
                }

                Object object = field.get(null);

                if (!(object instanceof CCConfigValues.Value<?> configValue)) {
                    continue;
                }

                jsonObject.add(field.getName(), configValue.write());
            }

            java.nio.file.Files.writeString(temporary, GSON.toJson(jsonObject), java.nio.charset.StandardCharsets.UTF_8);
            try {
                java.nio.file.Files.move(temporary, file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                java.nio.file.Files.move(temporary, file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            ChestedCompanions.LOGGER.error("Failed to save the Chested Companions config", e);
        } catch (IllegalAccessException e) {
            ChestedCompanions.LOGGER.error("Error while saving the Chested Companions config", e);
        } finally {
            if (temporary != null) {
                try { java.nio.file.Files.deleteIfExists(temporary); }
                catch (IOException e) { ChestedCompanions.LOGGER.warn("Could not remove temporary config", e); }
            }
        }
    }

    public static void load() {
        if (!file.exists()) {
            return;
        }

        try (var fileReader = java.nio.file.Files.newBufferedReader(file.toPath(), java.nio.charset.StandardCharsets.UTF_8)) {
            JsonObject jsonObject = GSON.fromJson(fileReader, JsonObject.class);
            if (jsonObject == null) {
                ChestedCompanions.LOGGER.warn("Empty configuration; keeping current values");
                return;
            }

            for (Field field : CCConfig.class.getDeclaredFields()) {
                if (!field.isAnnotationPresent(Entry.class)) {
                    continue;
                }

                String fieldName = field.getName();

                if (!jsonObject.has(fieldName)) {
                    continue;
                }

                Object object = field.get(null);

                if (!(object instanceof CCConfigValues.Value<?> configValue)) {
                    continue;
                }

                JsonElement jsonElement = jsonObject.get(fieldName);
                try {
                    configValue.setUnchecked(configValue.read(jsonElement));
                } catch (RuntimeException e) {
                    ChestedCompanions.LOGGER.warn("Invalid configuration value for {}", fieldName, e);
                }
            }
        } catch (IOException | com.google.gson.JsonParseException | IllegalStateException e) {
            ChestedCompanions.LOGGER.error("Failed to read the Chested Companions config", e);
        } catch (IllegalAccessException e) {
            ChestedCompanions.LOGGER.error("Error while reading the Chested Companions config", e);
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Entry {
        String category() default "general";
        boolean clientOnly() default false;
    }
}
