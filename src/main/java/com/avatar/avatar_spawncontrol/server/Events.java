package com.avatar.avatar_spawncontrol.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.avatar.avatar_spawncontrol.GlobalConfig;
import com.avatar.avatar_spawncontrol.Main;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = Main.MODID)
public class Events {

    private static long currentTime = 0;
    private static int frequencyChat = 240;
    private static int frequencyDespawn = 60;
    private static int distance = 80;
    private static int height = 15;
    private static int maxMonsterPerPlayer = 15;
    private static List<String> mobsBlocked = new ArrayList<>();
    private static List<String> mobsUnBlocked = new ArrayList<>();
    private static boolean start = true;
    private static Map<UUID, Integer> mobPerPlayer = new HashMap<>();

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
            ServerLevel world = event.getServer().getLevel(Level.OVERWORLD);       
            if (start) {
                frequencyChat = GlobalConfig.loadFrequencyChat();
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
                List<ServerPlayer> players = event.getServer().getPlayerList().getPlayers();
                Iterable<Entity> allEntities = world.getAllEntities();
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
                //
                if (checkPeriod(frequencyChat)) {
                    if (players == null || mobPerPlayer.size() == 0)
                        return;
                    int number = 0;
                    for (Entity entity : allEntities) {
                        if (entity instanceof Monster) {
                            number++;
                        }
                    }

                    for (ServerPlayer player : players) {
                        message(player, "Monsters around you: " + mobPerPlayer.get(player.getUUID()));
                        message(player, "Total monsters map: " + number);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingCheckSpawn(MobSpawnEvent event) {
        Entity entity = event.getEntity();
        ServerLevel world = (ServerLevel) entity.level();
        List<ServerPlayer> players = world.players();
        // check if mobs are blocked
        String entityName = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
        // whitelist mobs
        if (mobsUnBlocked.contains(entityName)) {
            entity.getPersistentData().putBoolean("wasRespawned", true);
            return;
        }
        // blacklist mobs
        if (mobsBlocked.contains(entityName)) {
            event.setResult(MobSpawnEvent.Result.DENY);
            return;
        }

        // check if was respawned
        if (entity.getPersistentData().getBoolean("wasRespawned")) {
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
            if (entity instanceof Monster || entity.getClass().getName().toString().contains("monster")) {
                if (mobPerPlayer.size() == 0) {
                    break;
                }
                int totalMonsters = mobPerPlayer.get(player.getUUID());
                if (totalMonsters >= maxMonsterPerPlayer) {
                    event.setResult(MobSpawnEvent.Result.DENY);
                    return;
                }

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
        if (!playerNearby) {
            event.setResult(MobSpawnEvent.Result.DENY);
        } else {
            entity.getPersistentData().putBoolean("wasRespawned", true);
        }
    }
}
