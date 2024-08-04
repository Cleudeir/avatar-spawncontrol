package com.avatar.avatar_spawncontrol.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.avatar.avatar_spawncontrol.GlobalConfig;
import com.avatar.avatar_spawncontrol.Main;
import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = Main.MODID)
public class Events {

    private static long currentTime = 0;
    private static int frequencyDespawn = 60;
    private static int distance = 80;
    private static int height = 15;
    private static int maxMonsterPerPlayer = 15;
    private static List<String> mobsBlocked = new ArrayList<>();
    private static List<String> mobsUnBlocked = new ArrayList<>();
    private static boolean start = true;
    private static Map<UUID, Integer> mobPerPlayer = new HashMap<>();
    private static List<ServerPlayer> players = new ArrayList<>();
    private static ServerLevel world = null;

    public static boolean checkPeriod(double seconds) {
        double divisor = (double) (seconds * 20);
        return currentTime % divisor == 0;
    }

    public static void message(ServerPlayer player, String message) {
        // Example of applying a style to the message
        Component styledMessage = Component.translatable(message)
                .setStyle(Style.EMPTY
                        .withColor(TextColor.fromRgb(0xFFFFFF)));
        player.sendSystemMessage(styledMessage);
    }

    @SubscribeEvent
    public static void ticksServer(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            world = event.getServer().getLevel(Level.OVERWORLD);
            if (start) {
                frequencyDespawn = GlobalConfig.loadFrequencyDespawn();
                distance = GlobalConfig.loadDistant();
                height = GlobalConfig.loadHeight();
                maxMonsterPerPlayer = GlobalConfig.loadMaxMonsterPerPlayer();
                mobsBlocked = GlobalConfig.loadMobsBlocked();
                mobsUnBlocked = GlobalConfig.loadMobsUnBlocked();
                start = false;
            }
            if (world != null) {
                long time = world.getDayTime();
                currentTime = time;
                players = event.getServer().getPlayerList().getPlayers();
                if (checkPeriod(1)) {
                    for (ServerPlayer player : players) {
                        double px = player.getX();
                        double py = player.getY();
                        double pz = player.getZ();

                        double minX = px - distance;
                        double minY = py - height;
                        double minZ = pz - distance;
                        double maxX = px + distance;
                        double maxY = py + height;
                        double maxZ = pz + distance;

                        // Create a new AxisAlignedBB (bounding box)
                        AABB boundingBox = new AABB(minX, minY, minZ, maxX, maxY, maxZ);

                        List<Mob> mobs = world.getEntitiesOfClass(Mob.class, boundingBox);
                        int count = 0;
                        for (Mob mob : mobs) {
                            if (mob.getClass().getName().toString().contains("monster")) {
                                count++;
                            }
                        }
                        mobPerPlayer.put(player.getUUID(), count);
                    }
                }
            }
        }
    }

    public static void monsterCount(CommandSourceStack source) {
        if (players == null || mobPerPlayer.isEmpty()) {
            return;
        }
        for (ServerPlayer player : players) {
            int nearbyMonsters = mobPerPlayer.getOrDefault(player.getUUID(), 0);
            message(player, "Monsters around you: " + nearbyMonsters);
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(
                Commands.literal("countmonster")
                        .requires(cs -> cs.hasPermission(2))
                        .executes(ctx -> {
                            monsterCount(ctx.getSource());
                            return 1;
                        }));
    }

    @SubscribeEvent
    public static void onLivingCheckSpawn(MobSpawnEvent event) {
        Entity entity = event.getEntity();
        ServerLevel world = (ServerLevel) entity.level();
        List<ServerPlayer> players = world.players();
        // check if mobs are blocked
        String entityName = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
        Boolean isMonster = entity.getClass().getName().toString().contains("monster");
        if (!isMonster) {
            event.setResult(MobSpawnEvent.Result.ALLOW);
            return;
        }

        // whitelist mobs
        if (mobsUnBlocked.contains(entityName)) {
            entity.getPersistentData().putBoolean("wasRespawned", true);
            event.setResult(MobSpawnEvent.Result.ALLOW);
            return;
        }
        // blacklist mobs
        if (mobsBlocked.contains(entityName)) {
            event.setResult(MobSpawnEvent.Result.DENY);
            return;
        }

        Boolean isRespawned = entity.getPersistentData().getBoolean("wasRespawned");
        // System.err.println("entityName: " + entityName);
        // System.err.println("isRespawned: " + isRespawned);

        // check if was respawned
        if (isRespawned) {

            boolean playerNearby = true;
            for (ServerPlayer player : players) {
                playerNearby = Math.abs(player.getX() - entity.getX()) <= distance &&
                        Math.abs(player.getZ() - entity.getZ()) <= distance &&
                        Math.abs(player.getY() - entity.getY()) <= height;
                if (playerNearby) {
                    break;
                }
            }
            //
            if (!playerNearby && checkPeriod(frequencyDespawn)) {
                entity.discard();
            }
            return;
        }
        // check max monsters
        for (ServerPlayer player : players) {
            if (mobPerPlayer.size() == 0) {
                break;
            }
            int totalMonsters = mobPerPlayer.getOrDefault(player.getUUID(), 0);
            // System.out.println("totalMonsters: " + totalMonsters + " maxMonsterPerPlayer:
            // " + maxMonsterPerPlayer);
            if (totalMonsters >= maxMonsterPerPlayer) {
                event.setResult(MobSpawnEvent.Result.DENY);
                return;
            }
        }
        // check players nearby
        boolean playerNearby = true;
        for (ServerPlayer player : players) {
            playerNearby = Math.abs(player.getX() - entity.getX()) <= distance &&
                    Math.abs(player.getZ() - entity.getZ()) <= distance &&
                    Math.abs(player.getY() - entity.getY()) <= height;
            if (playerNearby) {
                break;
            }
        }
        if (playerNearby) {
            entity.getPersistentData().putBoolean("wasRespawned", true);
            event.setResult(MobSpawnEvent.Result.ALLOW);
            return;
        } else {
            event.setResult(MobSpawnEvent.Result.DENY);
        }
    }
}
