package com.avatar.avatar_spawncontrol.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.avatar.avatar_spawncontrol.GlobalConfig;
import com.avatar.avatar_spawncontrol.Main;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = Main.MODID)
public class Events {

    private static long currentTime = 0;
    private static int frequencyChat = 240;
    private static int frequencyDespawn = 120;
    private static int distance = 80;
    private static int height = 30;
    private static int maxMonsterPerPlayer = 50;
    private static List<String> mobsBlocked = new ArrayList<>();
    private static List<String> mobsUnBlocked = new ArrayList<>();
    private static boolean start = true;
    private static AtomicInteger total = new AtomicInteger(0);
    private static AtomicInteger monster = new AtomicInteger(0);
    private static AtomicInteger ambientCreature = new AtomicInteger(0);
    private static AtomicInteger animal = new AtomicInteger(0);
    private static AtomicInteger item = new AtomicInteger(0);
    private static AtomicInteger npcCount = new AtomicInteger(0);
    private static AtomicInteger playerCount = new AtomicInteger(0);

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
                Iterable<Entity> allUnits = world.getAllEntities();
                if (checkPeriod(1)) {
                    total = new AtomicInteger(0);
                    monster = new AtomicInteger(0);
                    ambientCreature = new AtomicInteger(0);
                    animal = new AtomicInteger(0);
                    item = new AtomicInteger(0);
                    npcCount = new AtomicInteger(0);
                    playerCount = new AtomicInteger(0);
                    allUnits.forEach(entity -> {
                        if (entity instanceof ItemEntity) {
                            item.incrementAndGet();
                            total.incrementAndGet();
                        } else if (entity instanceof AmbientCreature) {
                            ambientCreature.incrementAndGet();
                            total.incrementAndGet();
                        } else if (entity instanceof Animal) {
                            animal.incrementAndGet();
                            total.incrementAndGet();
                        } else if (entity instanceof Npc) {
                            npcCount.incrementAndGet();
                            total.incrementAndGet();
                        } else if (entity instanceof ServerPlayer) {
                            playerCount.incrementAndGet();
                            total.incrementAndGet();
                        } else if (entity instanceof Monster
                                || entity.getClass().getName().toString().contains("monster")) {
                            monster.incrementAndGet();
                            total.incrementAndGet();
                        }
                    });
                }
                if (checkPeriod(frequencyChat)) {
                    if (players == null)
                        return;
                    int totalMonsters = monster.get();
                    int totalCount = total.get();
                    /*
                     * int totalAmbientCreatures = ambientCreature.get();
                     * int totalAnimals = animal.get();
                     * int totalItems = item.get();
                     * int totalNpcs = npcCount.get();
                     * int totalPlayers = playerCount.get();
                     */
                    for (ServerPlayer player : players) {
                        message(player, "Total entities: " + totalCount);
                        message(player, "Monsters: " + totalMonsters);
                        /*
                         * message(player, "Ambient Creatures: " + totalAmbientCreatures);
                         * message(player, "Animals: " + totalAnimals);
                         * message(player, "Items: " + totalItems);
                         * message(player, "Npcs: " + totalNpcs);
                         * message(player, "Players: " + totalPlayers);
                         */
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
            System.err.println(entityName);
            entity.getPersistentData().putBoolean("wasRespawned", true);
            return;
        }
        // blacklist mobs
        if (mobsBlocked.contains(entityName)) {
            System.err.println(entityName);
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
            if (!playerNearby && checkPeriod(frequencyDespawn)) {
                entity.discard();
            }
            return;
        }
        // check max monsters
        if (entity instanceof Monster || entity.getClass().getName().toString().contains("monster")) {
            int totalMonsters = monster.get();
            int totalPlayers = playerCount.get();
            if (totalMonsters >= (maxMonsterPerPlayer * totalPlayers)) {
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
        if (!playerNearby) {
            event.setResult(MobSpawnEvent.Result.DENY);
        } else {
            entity.getPersistentData().putBoolean("wasRespawned", true);
        }
    }
}
