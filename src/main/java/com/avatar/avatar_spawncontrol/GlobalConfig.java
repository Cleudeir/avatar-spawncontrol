package com.avatar.avatar_spawncontrol;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class GlobalConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static ForgeConfigSpec CONFIG;

    public static ForgeConfigSpec.ConfigValue<Integer> DISTANT;
    public static ForgeConfigSpec.ConfigValue<Integer> FREQUENCY;
    static {
        setupConfig();
    }

    private static void setupConfig() {
        BUILDER.comment("Distance Configuration").push("distanceConfig");
        DISTANT = BUILDER
                .define("distant", 60);
        BUILDER.pop();

        BUILDER.comment(
                "Frequency Configuration is used to determine how often mobs despawn and chat show information")
                .push("frequencyConfig");
        FREQUENCY = BUILDER
                .define("frequency", 120);
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

    public static int loadFrequency() {
        // Load the config if not already loaded
        Integer data = 120;
        if (CONFIG.isLoaded()) {
            // Retrieve data from config
            data = FREQUENCY.get();
        }
        return data;
    }
}
