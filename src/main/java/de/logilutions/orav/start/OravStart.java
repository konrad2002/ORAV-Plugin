package de.logilutions.orav.start;

import de.logilutions.orav.Orav;
import de.logilutions.orav.config.Config;
import de.logilutions.orav.database.DatabaseHandler;
import de.logilutions.orav.discord.DiscordUtil;
import de.logilutions.orav.player.OravPlayer;
import de.logilutions.orav.player.OravPlayerManager;
import de.logilutions.orav.session.SessionObserver;
import de.logilutions.orav.util.MessageManager;
import de.logilutions.orav.util.Scheduler;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.awt.Color;
import java.io.File;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

public class OravStart extends Config {
    private final Orav orav;
    private final Location middleLocation;
    private final SpawnGenerator spawnGenerator;
    private final int radius;
    private final DatabaseHandler databaseHandler;
    private Map<UUID, Location> startLocation = new HashMap<>();
    private final JavaPlugin javaPlugin;
    private final MessageManager messageManager;
    private final OravPlayerManager oravPlayerManager;

    private final Map<UUID, LocalDateTime> protectedUntil = new HashMap<>();
    private final SessionObserver sessionObserver;
    private final DiscordUtil discordUtil;
    private Scheduler countdownScheduler;
    private Scheduler protectionScheduler;
    private ProtectionTimeRunnable protectionTimeRunnable;
    private StartCountDownRunnable startCountDownRunnable;

    public OravStart(File file, Orav orav, Location middleLocation, SpawnGenerator spawnGenerator, int radius, DatabaseHandler databaseHandler, JavaPlugin javaPlugin, MessageManager messageManager, OravPlayerManager oravPlayerManager, SessionObserver sessionObserver, DiscordUtil discordUtil) {
        super(file);
        this.orav = orav;
        this.middleLocation = middleLocation;
        this.spawnGenerator = spawnGenerator;
        this.radius = radius;
        this.databaseHandler = databaseHandler;
        this.javaPlugin = javaPlugin;
        this.messageManager = messageManager;
        this.oravPlayerManager = oravPlayerManager;
        this.sessionObserver = sessionObserver;
        this.discordUtil = discordUtil;
        readLocations();
    }

    private void readLocations() {
        for (String key : getConfiguration().getKeys(false)) {
            UUID uuid = UUID.fromString(key);
            startLocation.put(uuid, getConfiguration().getLocation(key));
        }
    }

    public void startOrav(Player player) {
        OravPlayer oravPlayer = oravPlayerManager.getPlayer(player.getUniqueId());
        if (oravPlayer == null) {
            return;
        }
        player.getInventory().clear();
        player.teleport(getLocation(oravPlayer));
        if (orav.getState() == Orav.State.COUNTDOWN) {
            player.setGameMode(GameMode.ADVENTURE);
            if (startCountDownRunnable != null) {
                startCountDownRunnable.addPlayer(player);
            }
        } else if (orav.getState() == Orav.State.PROTECTION) {
            player.setGameMode(GameMode.SURVIVAL);
            startProtectionTime(player);
        } else {
            player.setGameMode(GameMode.SURVIVAL);
            protectedUntil.remove(player.getUniqueId());
            startProtectionTime(player);
        }
    }

    public void startOrav() {
        if (orav.getState() == Orav.State.RUNNING || orav.getState() == Orav.State.OVER) {
            return;
        }
        generateLocations();
        for (OravPlayer oravPlayer : oravPlayerManager.getAll()) {
            Player player = oravPlayer.getPlayer();
            if (player == null) {
                continue;
            }
            player.getInventory().clear();
            player.teleport(getLocation(oravPlayer));
        }
        orav.setState(Orav.State.COUNTDOWN);
        databaseHandler.updateOrav(orav);

        countdownScheduler = new Scheduler();
        startCountDownRunnable = new StartCountDownRunnable(messageManager, (player1) -> {
            Bukkit.getScheduler().runTask(javaPlugin, () -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.setGameMode(GameMode.SURVIVAL);
                    player.setHealth(20);
                    player.setFoodLevel(20);
                    player.setStatistic(Statistic.TIME_SINCE_REST, 0);
                    player.getInventory().clear();
                    player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1, 1);
                    messageManager.sendMessage(player, "Orav 6 ist gestartet!");
                    messageManager.sendTitle(player, "§5Orav 6", "§7gestartet!");
                }
                startProtectionTime();
                discordUtil.send(
                        "Minecraft ORAV #6", "Das Projekt Minecraft Orav #6 hat soeben begonnen!", "https://logilutions.de/Minecraft/ORAV-5/images/start.png",
                        Color.GREEN, null, null, null);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "weather clear");
            });
            countdownScheduler.cancel();
        });

        countdownScheduler.setBukkitTask(Bukkit.getScheduler().runTaskTimerAsynchronously(
                javaPlugin,
                startCountDownRunnable,
                0, 20));
    }

    private Location getLocation(OravPlayer oravPlayer) {
        return this.startLocation.get(oravPlayer.getUuid()).clone().add(0.5, 0, 0.5);
    }

    public void startProtectionTime() {
        World world = Bukkit.getWorlds().get(0);
        world.setDifficulty(Difficulty.NORMAL);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, true);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
        world.setClearWeatherDuration(20 * 60 * 20);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "advancement revoke @a everything");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "weather clear");

        for (OravPlayer oravPlayer : oravPlayerManager.getAll()) {
            oravPlayer.setFightProtected(true);
            sessionObserver.startSession(oravPlayer);
        }
        orav.setState(Orav.State.PROTECTION);
        databaseHandler.updateOrav(orav);
        long millis = orav.getProtectionTime() * 60 * 1000;
        protectionTimeRunnable = new ProtectionTimeRunnable(
                millis,
                messageManager,
                new HashSet<>(oravPlayerManager.getAll()),
                v -> {
                    orav.setState(Orav.State.RUNNING);
                    databaseHandler.updateOrav(orav);
                    protectionScheduler.cancel();
                }
        );

        protectionScheduler = new Scheduler();
        protectionScheduler.setBukkitTask(Bukkit.getScheduler().runTaskTimerAsynchronously(javaPlugin, protectionTimeRunnable, 0, 20));
    }

    public void startProtectionTime(Player player) {
        OravPlayer oravPlayer = oravPlayerManager.getPlayer(player.getUniqueId());
        if (orav.getState() == Orav.State.PROTECTION && protectionTimeRunnable != null) {
            protectionTimeRunnable.addPlayer(oravPlayer);
            return;
        }
        if (oravPlayer == null) {
            return;
        }

        long millis = 0;
        if (protectedUntil.containsKey(player.getUniqueId())) {
            Duration remaining = Duration.between(LocalDateTime.now(), protectedUntil.get(player.getUniqueId()));
            if (remaining.isNegative()) {
                return;
            }
            millis = remaining.toMillis();
        } else {
            if (databaseHandler.getSessions(oravPlayer).size() == 1 && !orav.getStartDate().toLocalDate().isEqual(LocalDate.now())) {
                millis = orav.getProtectionTimeAfterDayOne() * 60 * 1000;
            } else if (databaseHandler.getSessions(oravPlayer).size() == 1 && orav.getStartDate().toLocalDate().isEqual(LocalDate.now())) {
                millis = orav.getPlayTimeDayOne() * 60 * 1000;
            }
            protectedUntil.put(player.getUniqueId(), LocalDateTime.now().plus(millis, ChronoUnit.MILLIS));
        }
        if (millis == 0) {
            return;
        }

        Scheduler scheduler = new Scheduler();
        scheduler.setBukkitTask(Bukkit.getScheduler().runTaskTimerAsynchronously(javaPlugin, new ProtectionTimeRunnable(
                millis,
                messageManager,
                new HashSet<>(oravPlayerManager.getAll()),
                v -> scheduler.cancel()
        ), 0, 20));
    }

    public void generateLocations() {
        clear();
        this.startLocation = spawnGenerator.generateLocations(databaseHandler.getAllPlayers(orav.getId()), middleLocation, radius);
        for (Map.Entry<UUID, Location> entry : startLocation.entrySet()) {
            getConfiguration().set(entry.getKey().toString(), entry.getValue());
        }
        save();
    }


}
