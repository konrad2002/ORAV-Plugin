package de.logilutions.orav.start;

import de.logilutions.orav.player.OravPlayer;
import de.logilutions.orav.util.MessageManager;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.Statistic;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class StartCountDownRunnable implements Runnable {

    private long remainingMillis =  5 * 60 * 1000 + 1; //5 Minutes
    private final long startMillis;
    private long lastSystemMillis = System.currentTimeMillis();
    private final MessageManager messageManager;
    private final Consumer<Void> onEnd;
    private BossBar bossBar;

    public StartCountDownRunnable(MessageManager messageManager, Consumer<Void> onEnd) {
        this.messageManager = messageManager;
        this.onEnd = onEnd;
        this.bossBar = Bukkit.createBossBar("§2Minecraft §5Orav 6 §7startet in: §65 Minuten", BarColor.PURPLE, BarStyle.SEGMENTED_20);
        for (Player player : Bukkit.getOnlinePlayers()) {
            addPlayer(player);
        }
        this.startMillis = remainingMillis;
    }

    @Override
    public void run() {
        long lastRemaining = remainingMillis;
        long currentSystemMillis = System.currentTimeMillis();
        long delta = currentSystemMillis - lastSystemMillis;
        lastSystemMillis = currentSystemMillis;
        remainingMillis -= delta;


        if (remainingMillis <= 5 * 60 * 1000 && lastRemaining > 5 * 60 * 1000) {
            sendInfo("5 Minuten", Sound.BLOCK_NOTE_BLOCK_PLING, 1);
        }

        if (remainingMillis <= 4 * 60 * 1000 && lastRemaining > 4 * 60 * 1000) {
            sendInfo("4 Minuten", Sound.BLOCK_NOTE_BLOCK_PLING, 1);
        }

        if (remainingMillis <= 3 * 60 * 1000 && lastRemaining > 3 * 60 * 1000) {
            sendInfo("3 Minuten", Sound.BLOCK_NOTE_BLOCK_PLING, 1);
        }

        if (remainingMillis <= 2 * 60 * 1000 && lastRemaining > 52 * 60 * 1000) {
            sendInfo("2 Minuten", Sound.BLOCK_NOTE_BLOCK_PLING, 1);
        }


        if (remainingMillis <= 60 * 1000 && lastRemaining > 60 * 1000) {
            sendInfo("1 Minute", Sound.BLOCK_NOTE_BLOCK_PLING, 1);
        }

        if (remainingMillis <= 30 * 1000 && lastRemaining > 30 * 1000) {
            sendInfo("30 Sekunden", Sound.BLOCK_NOTE_BLOCK_PLING, 1);
        }

        if (remainingMillis <= 10 * 1000 && lastRemaining > 10 * 1000) {
            sendInfo("10 Sekunden", Sound.BLOCK_NOTE_BLOCK_PLING, 1);
        }

        if (remainingMillis <= 3 * 1000 && lastRemaining > 3 * 1000) {
            sendInfo("3 Sekunden", Sound.BLOCK_NOTE_BLOCK_PLING, 1);
        }

        if (remainingMillis <= 2 * 1000 && lastRemaining > 2 * 1000) {
            sendInfo("2 Sekunden", Sound.BLOCK_NOTE_BLOCK_PLING, 1);
        }

        if (remainingMillis <= 1000 && lastRemaining > 1000) {
            sendInfo("1 Sekunde", Sound.BLOCK_NOTE_BLOCK_PLING, 1);
        }

        if (remainingMillis <= 0) {
            onEnd.accept(null);
            removeBossBar();
        } else {
            updateBossBar();
        }

    }

    private void sendInfo(String time, Sound sound, float pitch) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player != null && player.isOnline()) {
                messageManager.sendMessage(player, "Orav 6 startet in %hc%" + time + "§7!");
                player.playSound(player.getLocation(), sound, 1, pitch);
                messageManager.sendTitle(player, "§5Orav 6 §6startet in", "§7" + time);
            }
        }
    }

    private void removeBossBar() {
        bossBar.removeAll();
    }

    public void addPlayer(Player player) {
        this.bossBar.addPlayer(player);
    }

    private void updateBossBar() {
        Duration duration = Duration.of(remainingMillis, ChronoUnit.MILLIS);
        long minutes = duration.toMinutes();
        int seconds = duration.toSecondsPart();
        if (minutes > 0) {
            StringBuilder builder = new StringBuilder(minutes + " Minute");
            if (minutes > 1) {
                builder.append("n");
            }
            bossBar.setTitle("§2Minecraft §5Orav 6 §7startet in: §6" + builder.toString());
        } else {
            StringBuilder builder = new StringBuilder(seconds + " Sekunde");
            if (seconds > 1) {
                builder.append("n");
            }
            bossBar.setTitle("§2Minecraft §5Orav 6 §7startet in: §6" + builder.toString());
        }
        bossBar.setProgress((float) remainingMillis / (float) startMillis);
    }

}
