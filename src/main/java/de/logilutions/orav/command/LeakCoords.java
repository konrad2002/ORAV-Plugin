package de.logilutions.orav.command;

import de.logilutions.orav.discord.DiscordUtil;
import de.logilutions.orav.util.MessageManager;
import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.awt.*;

@AllArgsConstructor
public class LeakCoords implements CommandExecutor {
    private MessageManager messageManager;
    private DiscordUtil discordUtil;

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (!(commandSender instanceof Player)) {
            return false;
        }
        Player sender = (Player) commandSender;
        if (!sender.isOp()) {
            this.messageManager.sendMessage(sender, "%ec%Du bist nicht berechtigt, diesen Befehl auszuführen.");
            return true;
        }

        Player player = Bukkit.getPlayer(args[0]);
        if (player == null) {
            this.messageManager.sendMessage(sender, "%ec%Bitte einen korrekten Spielernamen eingeben!");
            return true;
        }

        leakCoords(player);

        return true;
    }

    public void leakCoords(Player player) {
        Location location = player.getLocation();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        String message = player.getName() + " befindet sich bei x: " + x + "; y: " + y + "; z: " + z + ".";
        String message_colored = "§e" + player.getName() + "§7 befindet sich bei x: §b" + x + "§7; y: §b" + y + "§7; z: §b" + z + "§7.";
        this.messageManager.broadcast(Bukkit.getOnlinePlayers(), message_colored);

        this.discordUtil.send("Koordinaten-Veröffentlichung", message, null, Color.ORANGE, null, null, null);
    }
}
