package com.example.hpbar;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("hpbar.json");

    // Smeshchenie otnositel'no centra ekrana (dlya pricela) i niza centra (dlya khotbara)
    public int targetOffsetX = 0;
    public int targetOffsetY = 18;
    public float targetScale = 1.0f;

    public int playerOffsetX = 0;
    public int playerOffsetY = -44;
    public float playerScale = 1.15f;

    public boolean showDecimals = true;
    public boolean hideVanillaHearts = false;

    public static ModConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
                if (loaded != null) {
                    loaded.clampValues();
                    return loaded;
                }
            } catch (IOException e) {
                System.err.println("[HPBar] Failed to load config: " + e.getMessage());
            }
        }
        ModConfig config = new ModConfig();
        config.save();
        return config;
    }

    public void save() {
        clampValues();
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            System.err.println("[HPBar] Failed to save config: " + e.getMessage());
        }
    }

    public void resetDefaults() {
        targetOffsetX = 0;
        targetOffsetY = 18;
        targetScale = 1.0f;
        playerOffsetX = 0;
        playerOffsetY = -44;
        playerScale = 1.15f;
    }

    public void clampValues() {
        playerScale = Math.max(0.5f, Math.min(3.5f, playerScale));
        targetScale = Math.max(0.5f, Math.min(3.5f, targetScale));
        targetOffsetX = Math.max(-600, Math.min(600, targetOffsetX));
        targetOffsetY = Math.max(-400, Math.min(400, targetOffsetY));
        playerOffsetX = Math.max(-600, Math.min(600, playerOffsetX));
        playerOffsetY = Math.max(-500, Math.min(100, playerOffsetY));
    }
}
