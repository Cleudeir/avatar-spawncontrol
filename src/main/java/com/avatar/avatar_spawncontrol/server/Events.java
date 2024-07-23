package com.avatar.avatar_spawncontrol.server;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.avatar.avatar_spawncontrol.GlobalConfig;
import com.avatar.avatar_spawncontrol.Main;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Main.MODID)
public class Events {

    private static long currentTime = 0;
    private static int frequencyChat = 120;
    private static int frequencyDespawn = 120;
    private static boolean start = true;

    public static boolean checkPeriod(double seconds) {
        double divisor = (double) (seconds * 20);
        return currentTime % divisor == 0;
    }

    public static void message(ServerPlayer player, String message) {
        player.sendSystemMessage(
                Component.translatable(message));
    }

    @SubscribeEvent
    public static void ticksServer(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            ServerLevel world = event.getServer().getLevel(Level.OVERWORLD);
            if (start) {
                frequencyChat = GlobalConfig.loadFrequencyChat();
                frequencyDespawn = GlobalConfig.loadFrequencyDespawn();
                start = false;
            }
            if (world != null) {
                long time = world.getDayTime();
                currentTime = time;
                List<ServerPlayer> players = event.getServer().getPlayerList().getPlayers();
                System.err.println("Current time: " + frequencyChat);
                if (checkPeriod(frequencyChat)) {
                    if (players == null)
                        return;
                    Iterable<Entity> allUnits = world.getAllEntities();
                    AtomicInteger total = new AtomicInteger(0);
                    AtomicInteger monster = new AtomicInteger(0);
                    AtomicInteger ambientCreature = new AtomicInteger(0);
                    AtomicInteger animal = new AtomicInteger(0);
                    AtomicInteger item = new AtomicInteger(0);
                    AtomicInteger npcCount = new AtomicInteger(0);
                    AtomicInteger playerCount = new AtomicInteger(0);

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
                        } else if (entity instanceof Mob) {
                            monster.incrementAndGet();
                            total.incrementAndGet();
                        } else {
                            System.out.println("Entity type: " + entity.getClass().getName().toString());
                        }

                    });
                    int totalCount = total.get();
                    int totalMonsters = monster.get();
                    int totalAmbientCreatures = ambientCreature.get();
                    int totalAnimals = animal.get();
                    int totalItems = item.get();
                    int totalNpcs = npcCount.get();
                    int totalPlayers = playerCount.get();

                    for (ServerPlayer player : players) {
                        message(player, "Total mobs: " + totalCount);
                        message(player, "Monsters: " + totalMonsters);
                        message(player, "Ambient Creatures: " + totalAmbientCreatures);
                        message(player, "Animals: " + totalAnimals);
                        message(player, "Items: " + totalItems);
                        message(player, "Npcs: " + totalNpcs);
                        message(player, "Players: " + totalPlayers);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingCheckSpawn(MobSpawnEvent event) {
        Entity entity = event.getEntity();
        ServerLevel world = (ServerLevel) entity.level();
        int distance = GlobalConfig.loadDistant();
        int height = GlobalConfig.loadHeight();
        List<ServerPlayer> players = world.players();
        if (entity.getPersistentData().getBoolean("wasRespawned")) {
            boolean playerNearby = false;
            for (ServerPlayer player : players) {
                if (player.distanceToSqr(entity.getX(), entity.getY(), entity.getZ()) <= distance &&
                        Math.abs(player.getY() - entity.getY()) <= height) {
                    playerNearby = true;
                }
            }
            if (!playerNearby && checkPeriod(frequencyDespawn)) {
                entity.discard();
            }
            return;
        }
        boolean playerNearby = false;
        for (ServerPlayer player : players) {
            if (player.distanceToSqr(entity.getX(), entity.getY(), entity.getZ()) <= distance
                    * distance &&
                    Math.abs(player.getY() - entity.getY()) <= height) {
                playerNearby = true;
            }
        }
        if (!playerNearby) {
            event.setResult(MobSpawnEvent.Result.DENY);
        } else {
            entity.getPersistentData().putBoolean("wasRespawned", true);
        }

    }
}
