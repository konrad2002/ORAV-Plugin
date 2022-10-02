package de.logilutions.orav;

import de.logilutions.orav.command.LeakCoords;
import de.logilutions.orav.command.OravCommand;
import de.logilutions.orav.config.PlayerFightLogoutConfig;
import de.logilutions.orav.config.PlayerLogoutsConfig;
import de.logilutions.orav.database.DatabaseConnectionHolder;
import de.logilutions.orav.database.DatabaseHandler;
import de.logilutions.orav.discord.DiscordUtil;
import de.logilutions.orav.exception.DatabaseConfigException;
import de.logilutions.orav.fighting.FightingObserver;
import de.logilutions.orav.listener.*;
import de.logilutions.orav.player.OravPlayerManager;
import de.logilutions.orav.scoreboard.ScoreboardHandler;
import de.logilutions.orav.session.SessionObserver;
import de.logilutions.orav.start.OravStart;
import de.logilutions.orav.start.SpawnGenerator;
import de.logilutions.orav.tablist.TabList;
import de.logilutions.orav.teamchest.TeamChestListener;
import de.logilutions.orav.teamchest.TeamChestManager;
import de.logilutions.orav.util.Helper;
import de.logilutions.orav.util.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.awt.*;
import java.io.File;
import java.sql.SQLException;
import java.util.logging.Level;

public class OravPlugin extends JavaPlugin {

    private Orav orav;

    private DatabaseConnectionHolder databaseConnectionHolder;
    private DatabaseHandler databaseHandler;
    private OravPlayerManager oravPlayerManager;
    private SessionObserver sessionObserver;
    private MessageManager messageManager;
    private DiscordUtil discordUtil;
    private ScoreboardHandler scoreboardHandler;
    private FightingObserver fightingObserver;
    /* Config */
    private PlayerLogoutsConfig playerLogoutsConfig;
    private PlayerFightLogoutConfig playerFightLogoutConfig;
    private TeamChestManager teamChestManager;
    private Helper helper;
    private OravStart oravStart;

    @Override
    public void onEnable() {
        super.onEnable();
        FileConfiguration config = getConfig();
        try {
            initDatabase(config);
        } catch (DatabaseConfigException | SQLException e) {
            getLogger().log(Level.WARNING, "Error while enabling ORAV, Disabling itself!");
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        this.databaseHandler = new DatabaseHandler(databaseConnectionHolder);
        this.helper = new Helper(databaseHandler);
        this.messageManager = new MessageManager();
        this.playerLogoutsConfig = new PlayerLogoutsConfig(new File(getDataFolder(), "playerLogouts.yml"));
        this.playerFightLogoutConfig = new PlayerFightLogoutConfig(new File(getDataFolder(), "playerLogoutWhileFight.yml"));
        teamChestManager = new TeamChestManager(new File(getDataFolder(), "teamChestLocations.yml"));
        if (config.contains("current-orav")) {
            int oravID = config.getInt("current-orav");
            if (oravID > 0) {
                this.orav = databaseHandler.readOrav(oravID);
                this.oravPlayerManager = new OravPlayerManager(databaseHandler, orav);
                this.sessionObserver = new SessionObserver(
                        databaseHandler,
                        this,
                        messageManager,
                        oravPlayerManager,
                        orav);
            }
        }
        TabList tabList = new TabList();
        ConfigurationSection discordSection = config.getConfigurationSection("discord");

        if (discordSection == null) {
            getLogger().warning("Discord webhook not configurated! Please fill out the config.yml!");
        } else {
            this.discordUtil = new DiscordUtil(discordSection.getString("url"));
        }
        this.scoreboardHandler = new ScoreboardHandler(this.oravPlayerManager, tabList, orav,this,databaseHandler);
        this.fightingObserver = new FightingObserver(this, oravPlayerManager, this.messageManager, playerFightLogoutConfig, orav);
        //TODO radius per config
        int x = config.getInt("start-spawn.x");
        int y = config.getInt("start-spawn.y");
        int z = config.getInt("start-spawn.z");
        Location middle = new Location(Bukkit.getWorlds().get(0), x, y, z);
        this.oravStart = new OravStart(new File(getDataFolder(), "oravStartLocations.yml"), orav, middle, new SpawnGenerator(), 25, databaseHandler, this, messageManager, oravPlayerManager, sessionObserver, discordUtil);

        initCommands();
        registerListener();
        this.discordUtil.send(":green_circle: Der Server wurde gestartet!", null, null, Color.CYAN, null, null, null);
        tabList.start(this);
    }

    private void registerListener() {
        PluginManager pm = Bukkit.getPluginManager();
        if (orav != null) {
            pm.registerEvents(new PlayerDeathListener(discordUtil, oravPlayerManager, databaseHandler, helper), this);
            pm.registerEvents(new PlayerJoinQuitListener(discordUtil, oravPlayerManager, orav, sessionObserver, scoreboardHandler, playerLogoutsConfig, this.helper, this.messageManager, this.databaseHandler, oravStart), this);
            pm.registerEvents(new PlayerSessionListener(oravPlayerManager, this.helper), this);
            pm.registerEvents(new PortalListener(oravPlayerManager), this);
            pm.registerEvents(new PlayerChatListener(), this);
            pm.registerEvents(new TeamChestListener(oravPlayerManager, teamChestManager, messageManager, databaseHandler,helper), this);
            pm.registerEvents(new PreparingListener(orav), this);

            fightingObserver.start();
        }
    }


    private void initDatabase(FileConfiguration config) throws DatabaseConfigException, SQLException {
        ConfigurationSection databaseSection = config.getConfigurationSection("database");
        if (databaseSection == null) {
            getLogger().warning("Database not configurated! Please fill out the config.yml!");
        } else {
            databaseConnectionHolder = new DatabaseConnectionHolder(databaseSection);
        }
    }

    private void initCommands() {
        getCommand("leakcoords").setExecutor(new LeakCoords(messageManager, discordUtil));
        if (sessionObserver != null) {
            OravCommand oravCommand = new OravCommand(this.messageManager, oravPlayerManager, sessionObserver, oravStart, orav, databaseHandler);
            getCommand("orav").setExecutor(oravCommand);
            getCommand("orav").setTabCompleter(oravCommand);
        }

    }

    @Override
    public void onDisable() {

        this.discordUtil.send(":red_circle: Der Server wurde gestoppt!", null, null, Color.CYAN, null, null, null);

        super.onDisable();
        if (sessionObserver != null) {
            sessionObserver.cancel();
        }
    }
}
