package de.logilutions.orav.scoreboard;

import de.logilutions.orav.Orav;
import de.logilutions.orav.database.DatabaseHandler;
import de.logilutions.orav.player.OravPlayer;
import de.logilutions.orav.player.OravPlayerManager;
import de.logilutions.orav.tablist.TabList;
import de.logilutions.orav.team.OravTeam;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class ScoreboardHandler {

    private final OravPlayerManager oravPlayerManager;
    private final TabList tabList;
    private final Orav orav;
    private final DatabaseHandler databaseHandler;

    public ScoreboardHandler(OravPlayerManager oravPlayerManager, TabList tabList, Orav orav, JavaPlugin javaPlugin, DatabaseHandler databaseHandler) {
        this.tabList = tabList;
        this.orav = orav;
        this.databaseHandler = databaseHandler;
        for (Team team : Bukkit.getScoreboardManager().getMainScoreboard().getTeams()) {
            team.unregister();
        }
        this.oravPlayerManager = oravPlayerManager;

        registerObjectives();
        Bukkit.getScheduler().runTaskTimer(javaPlugin, this::updateObjectives, 0, 20 * 30);
    }

    private void registerObjectives() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Objective sidebar = scoreboard.getObjective(DisplaySlot.SIDEBAR);
        if (sidebar != null) {
            sidebar.unregister();
        }
//            sidebar = scoreboard.registerNewObjective("Sidebar", "Sidebar", "Sidebar");
        // TODO: quick fixed
        sidebar = scoreboard.registerNewObjective("Sidebar1", new Criteria() {
            @Override
            public String getName() {
                return "Sidebar";
            }

            @Override
            public boolean isReadOnly() {
                return false;
            }

            @Override
            public RenderType getDefaultRenderType() {
                return RenderType.INTEGER;
            }
        }, "Sidebar2");
        sidebar.setDisplayName("§2Minecraft §5Orav 5");
        sidebar.getScore("§5Spieltag").setScore(7);
        sidebar.getScore("§1   §8>> §7" + getDay()).setScore(6);
        sidebar.getScore("§5Weltgröße").setScore(5);
        sidebar.getScore("§2   §8>> §7" + getBorderSize()).setScore(4);
        sidebar.getScore("§5Spieler").setScore(3);
        sidebar.getScore("§4  §8 >> §7" + getAllPlayers()).setScore(2);
        sidebar.getScore("§5Verbl. Spieler").setScore(1);
        sidebar.getScore("§5   §8>> §7" + getRemainingPlayers()).setScore(0);
        sidebar.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    private String getRemainingPlayers() {
        return databaseHandler.getRemainingPlayers(orav.getId()).size() + "";
    }

    private String getAllPlayers() {
        return databaseHandler.getAllPlayers(orav.getId()).size() + "";
    }

    private String getBorderSize() {
        return Bukkit.getWorlds().get(0).getWorldBorder().getSize() + "";
    }

    private String getDay() {
        return (ChronoUnit.DAYS.between(orav.getStartDate().toLocalDate(), LocalDate.now()) + 1) + "";
    }

    private void updateObjectives() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Objective sidebar = scoreboard.getObjective("Sidebar");
        if (sidebar == null) {
            registerObjectives();
            sidebar = scoreboard.getObjective("Sidebar");
        }
        for (String entry : scoreboard.getEntries()) {
            if (sidebar.getScore(entry).getScore() == 6) {
                scoreboard.resetScores(entry);
                sidebar.getScore("§1   §8>> §7" + getDay()).setScore(6);
            }
            if (sidebar.getScore(entry).getScore() == 4) {
                scoreboard.resetScores(entry);
                sidebar.getScore("§2   §8>> §7" + getBorderSize()).setScore(4);
            }
            if (sidebar.getScore(entry).getScore() == 2) {
                scoreboard.resetScores(entry);
                sidebar.getScore("§4  §8 >> §7" + getAllPlayers()).setScore(2);
            }
            if (sidebar.getScore(entry).getScore() == 0) {
                scoreboard.resetScores(entry);
                sidebar.getScore("§5   §8>> §7" + getRemainingPlayers()).setScore(0);
            }
        }
    }

    public void playerSpawned(Player player) {
        OravPlayer oravPlayer = oravPlayerManager.getPlayer(player.getUniqueId());
        if (oravPlayer == null) {
            return;
        }
        Scoreboard scoreboard = player.getScoreboard();
        if (scoreboard != Bukkit.getScoreboardManager().getMainScoreboard()) {
            scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        }
        addToScoreboard(oravPlayer, scoreboard, true);
        for (OravPlayer oravPlayer1 : oravPlayerManager.getAll()) {
            if (oravPlayer1.equals(oravPlayer)) {
                continue;
            }
            Player player1 = oravPlayer1.getPlayer();
            if (player1 == null) {
                continue;
            }
            addToScoreboard(oravPlayer1, scoreboard, false);
            addToScoreboard(oravPlayer, player1.getScoreboard(), false);
        }
        updateObjectives();
        player.setScoreboard(scoreboard);

        sendTabList(player);
    }

    private void sendTabList(Player player) {
        tabList.send(player);
    }

    public void playerQuit(Player player) {
        OravPlayer oravPlayer = oravPlayerManager.getPlayer(player.getUniqueId());
        if (oravPlayer == null) {
            return;
        }
        OravTeam oravTeam = oravPlayer.getOravTeam();
        if (oravTeam == null) {
            return;
        }
        String teamName = oravTeam.getShortName();
        for (Player p : Bukkit.getOnlinePlayers()) {
            Team team = p.getScoreboard().getTeam(teamName);
            if (team != null) {
                team.removePlayer(player);
            }
        }
    }

    private void addToScoreboard(OravPlayer oravPlayer, Scoreboard scoreboard, boolean displayname) {
        Player player = oravPlayer.getPlayer();
        if (player == null) {
            return;
        }
        OravTeam oravTeam = oravPlayer.getOravTeam();
        if (oravTeam == null) {
            return;
        }
        net.md_5.bungee.api.ChatColor color = oravTeam.getTeamColor().getChatColor();
        String teamName = oravTeam.getShortName();
        Team team = scoreboard.getTeam(teamName);
        String prefix = color.toString() + oravTeam.getShortName() + " §7| ";
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
            team.setPrefix(prefix);
            team.setDisplayName(prefix + player.getName());
            team.setColor(ChatColor.GRAY);
            team.setAllowFriendlyFire(false);
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        }
        team.addEntry(player.getName());

        if (displayname) {
            player.setDisplayName(prefix + player.getName());
            player.setCustomName(prefix + player.getName());
            player.setPlayerListName(prefix + player.getName());
        }
    }
}
