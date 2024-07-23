package com.avatar.avatar_spawncontrol;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class GlobalConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static ForgeConfigSpec CONFIG;

    public static ForgeConfigSpec.ConfigValue<Integer> DISTANT;
    public static ForgeConfigSpec.ConfigValue<Integer> HEIGHT;
    public static ForgeConfigSpec.ConfigValue<Integer> FREQUENCYCHAT;
    public static ForgeConfigSpec.ConfigValue<Integer> FREQUENCYDESPAWN;
    static {
        setupConfig();
    }

    private static void setupConfig() {
        // Configure the distance mobs will spawn from players
        BUILDER.comment("Minimum distance mobs will spawn away from players").push("distanceConfig");
        DISTANT = BUILDER.define("distant", 60);
        BUILDER.pop();

        // Configure the height mobs will spawn from players
        BUILDER.comment("Minimum height mobs will spawn above or below players").push("heightConfig");
        HEIGHT = BUILDER.define("height", 30);
        BUILDER.pop();

        // Configure the frequency of mob despawn and chat information updates
        BUILDER.comment("Frequency (in seconds) for chat information updates").push("frequencyConfig");
        FREQUENCYCHAT = BUILDER.define("frequencyChat", 120);
        BUILDER.pop();

        // Configure the frequency of mob despawn
        BUILDER.comment("Frequency (in seconds) for mob despawn").push("frequencyDespawn");
        FREQUENCYDESPAWN = BUILDER.define("frequencyDespawn", 120);
        BUILDER.pop();

        CONFIG = BUILDER.build();
    }

    public static void init() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CONFIG);
    }

    public static int loadDistant() {
        // Load the config if not already loaded
        Integer data = 60;
        if (CONFIG.isLoaded()) {
            // Retrieve data from config
            data = DISTANT.get();
        }
        return data;
    }

    public static int loadFrequencyChat() {
        // Load the config if not already loaded
        Integer data = 120;
        if (CONFIG.isLoaded()) {
            // Retrieve data from config
            data = FREQUENCYCHAT.get();
        }
        return data;
    }

    public static int loadFrequencyDespawn() {
        // Load the config if not already loaded
        Integer data = 120;
        if (CONFIG.isLoaded()) {
            // Retrieve data from config
            data = FREQUENCYDESPAWN.get();
        }
        return data;
    }

    public static int loadHeight() {
        // Load the config if not already loaded
        Integer data = 30;
        if (CONFIG.isLoaded()) {
            // Retrieve data from config
            data = HEIGHT.get();
        }
        return data;
    }
}
