package de.logilutions.orav.fighting;

import de.logilutions.orav.Orav;
import de.logilutions.orav.config.PlayerFightLogoutConfig;
import de.logilutions.orav.config.PlayerLogoutsConfig;
import de.logilutions.orav.player.OravPlayer;
import de.logilutions.orav.player.OravPlayerManager;
import de.logilutions.orav.util.MessageManager;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor
public class FightingObserver implements Listener {
    private final Set<OravPlayerFighting> oravPlayerFightingSet = new HashSet<>();
    private final JavaPlugin javaPlugin;
    private final OravPlayerManager oravPlayerManager;
    private final MessageManager messageManager;
    private final PlayerFightLogoutConfig playerFightLogoutConfig;
    private final Orav orav;
    private BukkitTask removeTask;
    private BukkitTask messageTask;

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, javaPlugin);
        removeTask = Bukkit.getScheduler().runTaskTimerAsynchronously(javaPlugin, () -> {
            Collection<OravPlayerFighting> toRemove = new HashSet<>();
            synchronized (oravPlayerFightingSet) {
                for (OravPlayerFighting oravPlayerFighting : oravPlayerFightingSet) {
                    if (oravPlayerFighting.isOver()) {
                        toRemove.add(oravPlayerFighting);
                    }
                }
                removeFightings(toRemove);
            }
        }, 0, 10);

        messageTask = Bukkit.getScheduler().runTaskTimerAsynchronously(javaPlugin, () -> {
            for (OravPlayerFighting oravPlayerFighting : oravPlayerFightingSet) {
                if (!oravPlayerFighting.isOver()) {
                    sendInFightMessage(oravPlayerFighting.getOravPlayer());
                    sendInFightMessage(oravPlayerFighting.getOtherPlayer());
                }
            }
        }, 0, 20 * 20);
    }

    private void sendEndFightMessage(OravPlayer oravPlayer) {
        sendMessage(oravPlayer, "Du bist nun nicht mehr in einem Fight und darfst disconnecten!");
    }

    private void sendInFightMessage(OravPlayer oravPlayer) {
        sendMessage(oravPlayer, "Du befindest dich noch in einem Fight und darfst nicht disconnecten!");
    }

    private void sendMessage(OravPlayer oravPlayer, String message) {
        Player player1 = oravPlayer.getPlayer();
        if (player1 != null) {
            messageManager.sendMessage(player1, message);
        }
    }

    public void end() {
        if (removeTask != null) {
            removeTask.cancel();
        }
        if (messageTask != null) {
            messageTask.cancel();
        }
    }

    public void addFight(OravPlayer oravPlayer1, OravPlayer oravPlayer2) {
        for (OravPlayerFighting oravPlayerFighting : oravPlayerFightingSet) {
            if (oravPlayerFighting.contains(oravPlayer1, oravPlayer2)) {
                oravPlayerFighting.addHit();
                return;
            }
        }
        OravPlayerFighting oravPlayerFighting = new OravPlayerFighting(oravPlayer1, oravPlayer2);
        this.oravPlayerFightingSet.add(oravPlayerFighting);
    }

    private void fightOver(OravPlayer oravPlayer) {
        removeFightings(getFightings(oravPlayer));
    }

    private void removeFightings(Collection<OravPlayerFighting> toRemove) {
        for (OravPlayerFighting oravPlayerFighting : toRemove) {
            sendEndFightMessage(oravPlayerFighting.getOravPlayer());
            sendEndFightMessage(oravPlayerFighting.getOtherPlayer());
            oravPlayerFighting.endFighting();
        }
        oravPlayerFightingSet.removeAll(toRemove);
    }

    private void quit(OravPlayer oravPlayer) {
        Collection<OravPlayerFighting> oravPlayerFightings = getFightings(oravPlayer);
        if (oravPlayerFightings.isEmpty()) {
            return;
        }
        playerFightLogoutConfig.addLogoutInFight(oravPlayer, oravPlayerFightings);
    }

    public Collection<OravPlayerFighting> getFightings(OravPlayer oravPlayer) {
        Collection<OravPlayerFighting> toRemove = new HashSet<>();
        for (OravPlayerFighting oravPlayerFighting : oravPlayerFightingSet) {
            if (oravPlayerFighting.contains(oravPlayer)) {
                toRemove.add(oravPlayerFighting);
            }
        }
        return toRemove;
    }

    @EventHandler
    public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
        if (orav == null || orav.getState() == Orav.State.PREPARATION) {
            event.setCancelled(true);
            return;
        }
        Entity damagedEntity = event.getEntity();
        if (damagedEntity.getType() != EntityType.PLAYER) {
            return;
        }
        Entity damager = event.getDamager();
        Player damagerPlayer = null;
        if (damager instanceof Projectile) {
            ProjectileSource projectileSource = ((Projectile) damager).getShooter();
            if (projectileSource instanceof Player) {
                damagerPlayer = (Player) projectileSource;
            }
        } else if (damager.getType() == EntityType.PLAYER) {
            damagerPlayer = (Player) damager;
        }
        if (damagerPlayer == null) {
            return;
        }
        OravPlayer damagedOravPlayer = oravPlayerManager.getPlayer(damagedEntity.getUniqueId());
        OravPlayer damagerOravPlayer = oravPlayerManager.getPlayer(damagerPlayer.getUniqueId());
        if (damagedOravPlayer == null || damagerOravPlayer == null) {
            return;
        }
        if (damagedOravPlayer.getOravTeam().equals(damagerOravPlayer.getOravTeam())) {
            this.messageManager.sendMessage(damagerPlayer, "Du darfst deinen Teamkollegen nicht Schaden!");
            event.setCancelled(true);
            return;
        }
        if (damagedOravPlayer.isFightProtected()) {
            this.messageManager.sendMessage(damagerPlayer, "Dein Opfer ist noch in Schutzzeit!");
            event.setCancelled(true);
            return;
        }
        if (damagerOravPlayer.isFightProtected()) {
            this.messageManager.sendMessage(damagerPlayer, "Du bist noch in Schutzzeit!");
            event.setCancelled(true);
            return;
        }

        addFight(damagedOravPlayer, damagerOravPlayer);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        OravPlayer oravPlayer = oravPlayerManager.getPlayer(player.getUniqueId());
        if (oravPlayer == null) {
            return;
        }
        fightOver(oravPlayer);
    }

    @EventHandler
    public void onPlayerDeath(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        OravPlayer oravPlayer = oravPlayerManager.getPlayer(player.getUniqueId());
        if (oravPlayer == null) {
            return;
        }
        quit(oravPlayer);
    }

}
