package de.logilutions.orav.start;

import de.logilutions.orav.player.OravPlayer;
import de.logilutions.orav.util.MessageManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.Consumer;

public class ProtectionTimeRunnable implements Runnable {
    private final long startMillis;
    private long remainingMillis;
    private long lastSystemMillis = System.currentTimeMillis();
    private final MessageManager messageManager;
    private final Consumer<Void> onEnd;
    private BossBar bossBar;
    private final Collection<OravPlayer> oravPlayers;

    public ProtectionTimeRunnable(long remainingMillis, MessageManager messageManager, Collection<OravPlayer> oravPlayers, Consumer<Void> onEnd) {
        this.remainingMillis = remainingMillis + 1;
        this.startMillis = this.remainingMillis;
        this.messageManager = messageManager;
        this.onEnd = onEnd;
        this.oravPlayers = oravPlayers;
        this.bossBar = Bukkit.createBossBar("§7Verbleibende Schutzzeit: §6" + (remainingMillis / (60 * 1000)) + " Minuten", BarColor.RED, BarStyle.SEGMENTED_20);
        for (OravPlayer oravPlayer : oravPlayers) {
            addPlayer(oravPlayer);
        }
    }

    public void addPlayer(OravPlayer oravPlayer) {
        Player player = oravPlayer.getPlayer();
        if(player == null){
            return;
        }
        this.oravPlayers.add(oravPlayer);
        oravPlayer.setFightProtected(true);
        this.bossBar.addPlayer(player);
    }

    @Override
    public void run() {
        long lastRemaining = remainingMillis;
        long currentSystemMillis = System.currentTimeMillis();
        long delta = currentSystemMillis - lastSystemMillis;
        lastSystemMillis = currentSystemMillis;
        remainingMillis -= delta;
        if (remainingMillis <= 30 * 60 * 1000 && lastRemaining > 30 * 60 * 1000) {
            sendMessage("Die Schutzzeit endet in %hc%30 Minuten§7!");
        }

        if (remainingMillis <= 15 * 60 * 1000 && lastRemaining > 15 * 60 * 1000) {
            sendMessage("Die Schutzzeit endet in %hc%15 Minuten§7!");
        }

        if (remainingMillis <= 10 * 60 * 1000 && lastRemaining > 10 * 60 * 1000) {
            sendMessage("Die Schutzzeit endet in %hc%10 Minuten§7!");
        }

        if (remainingMillis <= 5 * 60 * 1000 && lastRemaining > 5 * 60 * 1000) {
            sendMessage("Die Schutzzeit endet in %hc%5 Minuten§7!");
        }

        if (remainingMillis <= 60 * 1000 && lastRemaining > 60 * 1000) {
            sendMessage("Die Schutzzeit endet in %hc%1 Minute§7!");
        }

        if (remainingMillis <= 30 * 1000 && lastRemaining > 30 * 1000) {
            sendMessage("Die Schutzzeit endet in %hc%30 Sekunden§7!");
        }

        if (remainingMillis <= 10 * 1000 && lastRemaining > 10 * 1000) {
            sendMessage("Die Schutzzeit endet in %hc%10 Sekunden§7!");
        }

        if (remainingMillis <= 3 * 1000 && lastRemaining > 3 * 1000) {
            sendMessage("Die Schutzzeit endet in %hc%3 Sekunden§7!");
        }

        if (remainingMillis <= 2 * 1000 && lastRemaining > 2 * 1000) {
            sendMessage("Die Schutzzeit endet in %hc%2 Sekunden§7!");
        }

        if (remainingMillis <= 1000 && lastRemaining > 1000) {
            sendMessage("Die Schutzzeit endet in %hc%1 Sekunde§7!");
        }
        if (remainingMillis <= 0) {
            removeBossBar();
            for(OravPlayer oravPlayer:oravPlayers){
                oravPlayer.setFightProtected(false);
            }
            sendMessage("Die Schutzzeit ist nun vorbei!");
            onEnd.accept(null);
        }else{
        updateBossBar();
        }
    }

    private void sendMessage(String s) {
        for (OravPlayer oravPlayer : oravPlayers) {
            Player player = oravPlayer.getPlayer();
            if (player == null) {
                oravPlayers.remove(oravPlayer);
            } else {
                messageManager.sendMessage(player, s);
            }
        }
    }

    private void removeBossBar() {
        bossBar.removeAll();
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
            bossBar.setTitle("§7Verbleibende Schutzzeit: §6" + builder.toString());
        } else {
            StringBuilder builder = new StringBuilder(seconds + " Sekunde");
            if (seconds > 1) {
                builder.append("n");
            }
            bossBar.setTitle("§7Verbleibende Schutzzeit: §6" + builder.toString());
        }
        bossBar.setProgress((float) remainingMillis / (float) startMillis);
    }
}
