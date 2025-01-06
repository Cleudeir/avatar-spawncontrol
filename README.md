## Summary

This project comprises a Minecraft Forge mod, "avatar_spawncontrol," designed to manage mob spawning.  The mod uses Forge's event system and configuration capabilities.  The `GlobalConfig` class handles configuration settings (spawn radius, mob blacklists/whitelists, maximum mob counts, despawn frequency).  The `Events` class listens for server ticks, registers a `/countmonster` command, and intercepts mob spawning events to apply the configured restrictions. The `Main` class serves as the mod's entry point, initializing the configuration, event listeners, and command registration.  A comprehensive changelog documents changes across several versions,  and project documentation details licensing, authorship, and third-party components.


## Tech Stack

Java, Minecraft Forge, Forge Config Spec, Brigadier (for command handling), ASM (likely for bytecode manipulation).
