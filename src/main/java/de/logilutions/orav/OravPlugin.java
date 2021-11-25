package de.logilutions.orav;

import de.logilutions.orav.command.LeakCoords;
import de.logilutions.orav.command.OravCommand;
import de.logilutions.orav.config.PlayerFightLogoutConfig;
import de.logilutions.orav.config.PlayerLogoutsConfig;
import de.logilutions.orav.database.DatabaseConnectionHolder;
import de.logilutions.orav.database.DatabaseHandler;
import de.logilutions.orav.discord.DiscordUtil;
import de.logilutions.orav.discord.DiscordWebhook;
import de.logilutions.orav.exception.DatabaseConfigException;
import de.logilutions.orav.fighting.FightingObserver;
import de.logilutions.orav.listener.*;
import de.logilutions.orav.player.OravPlayer;
import de.logilutions.orav.player.OravPlayerManager;
import de.logilutions.orav.scoreboard.ScoreboardHandler;
import de.logilutions.orav.session.SessionObserver;
import de.logilutions.orav.spawn.SpawnCycleGenerator;
import de.logilutions.orav.spawn.SpawnGenerator;
import de.logilutions.orav.util.Helper;
import de.logilutions.orav.util.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
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
    private Helper helper;

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
        this.discordUtil = new DiscordUtil("https://discord.com/api/webhooks/912863008508760095/PYhV2onPsh-geKovWFeOSIWUt7_kh8rO27gTV796jtOIFHNyQz6kXEpxZPRxC2-dKDUh");
        this.scoreboardHandler = new ScoreboardHandler();
        this.fightingObserver = new FightingObserver(this, oravPlayerManager, this.messageManager, playerFightLogoutConfig);

        initCommands();
        registerListener();
//        DiscordWebhook webhook = new DiscordWebhook("https://discord.com/api/webhooks/912863008508760095/PYhV2onPsh-geKovWFeOSIWUt7_kh8rO27gTV796jtOIFHNyQz6kXEpxZPRxC2-dKDUh");
//        webhook.setContent("----------------------------\nDer Server wurde gestartet!");
//        try {
//            webhook.execute();
//            System.out.println("executed dc send");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    private void registerListener() {
        PluginManager pm = Bukkit.getPluginManager();
        if (orav != null) {
            pm.registerEvents(new PlayerDeathListener(discordUtil, oravPlayerManager, databaseHandler, helper), this);
            pm.registerEvents(new PlayerJoinQuitListener(discordUtil, oravPlayerManager, orav, sessionObserver, scoreboardHandler, playerLogoutsConfig, this.helper, this.messageManager),this);
            pm.registerEvents(new PlayerSessionListener(oravPlayerManager, this.helper), this);
            pm.registerEvents(new PortalListener(), this);
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
        getCommand("generatespawn").setExecutor(new SpawnGenerator(messageManager));
        getCommand("generatespawncycle").setExecutor(new SpawnCycleGenerator(messageManager));
        getCommand("leakcoords").setExecutor(new LeakCoords(messageManager, discordUtil));
        if (sessionObserver != null) {
            OravCommand oravCommand = new OravCommand(this.messageManager, oravPlayerManager, sessionObserver);
            getCommand("orav").setExecutor(oravCommand);
            getCommand("orav").setTabCompleter(oravCommand);

        }

    }

    @Override
    public void onDisable() {

        this.discordUtil.send("\":octagonal_sign: Der Server wurde gestoppt!\"", null, null, Color.CYAN, null, null, null);

        super.onDisable();
        if (sessionObserver != null) {
            sessionObserver.cancel();
        }
    }
}
