package de.logilutions.orav.teamchest;

import de.logilutions.orav.database.DatabaseHandler;
import de.logilutions.orav.player.OravPlayer;
import de.logilutions.orav.player.OravPlayerManager;
import de.logilutions.orav.team.OravTeam;
import de.logilutions.orav.util.Helper;
import de.logilutions.orav.util.MessageManager;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;

@RequiredArgsConstructor
public class TeamChestListener implements Listener {
    private final OravPlayerManager oravPlayerManager;
    private final TeamChestManager teamChestManager;
    private final MessageManager messageManager;
    private final DatabaseHandler databaseHandler;
    private final Helper helper;
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        OravPlayer oravPlayer = oravPlayerManager.getPlayer(event.getPlayer().getUniqueId());
        if (oravPlayer == null) {
            return;
        }
        if (event.getHand() == EquipmentSlot.OFF_HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock.getType() != Material.CHEST) {
            return;
        }
        Long teamId = getTeamIdOfBlock(clickedBlock);
        if (!hasChestAccess(oravPlayer, teamId)) {
            event.setCancelled(true);
            OravTeam oravTeam = databaseHandler.readTeam(teamId);
            event.getPlayer().sendMessage(messageManager.getPrefix() + " " + "Das ist die Teamkiste von "
                    + oravTeam.getTeamColor().getChatColor().toString() + oravTeam.getName());
        }
    }

    @EventHandler
    private void onSignChange(SignChangeEvent event) {
        OravPlayer oravPlayer = oravPlayerManager.getPlayer(event.getPlayer().getUniqueId());
        if (oravPlayer == null) {
            return;
        }
        String line0 = event.getLine(0).toLowerCase();
        if (!(line0.contains("team") && line0.contains("kiste"))) {
            return;
        }
        Sign sign = (Sign) event.getBlock().getState();
        if (!(sign.getBlockData() instanceof WallSign)) {
            return;
        }
        WallSign wallSign = (WallSign) sign.getBlockData();
        Block behind = event.getBlock().getRelative(wallSign.getFacing().getOppositeFace());
        if (behind.getType() != Material.CHEST) {
            return;
        }
        if (teamChestManager.setChest(oravPlayer.getOravTeam(), behind.getLocation())) {
            messageManager.sendMessage(event.getPlayer(), "Du hast die Teamkiste erfolgreich gesetzt!");
        } else {
            messageManager.sendMessage(event.getPlayer(), "Ihr habt bereits eine Teamkiste!");
        }
    }

    @EventHandler
    private void onBlockBreak(BlockBreakEvent event) {
        OravPlayer oravPlayer = oravPlayerManager.getPlayer(event.getPlayer().getUniqueId());
        if (oravPlayer == null) {
            event.setCancelled(true);
            return;
        }
        Block block = event.getBlock();
        if (block.getType() != Material.CHEST) {
            return;
        }
        Long teamId = getTeamIdOfBlock(block);
        if (hasChestAccess(oravPlayer, teamId)) {
            teamChestManager.removeChest(block.getLocation());
        } else {
            event.setCancelled(true);
            OravTeam oravTeam = databaseHandler.readTeam(teamId);
            event.getPlayer().sendMessage(messageManager.getPrefix() + " " + "Das ist die Teamkiste von "
                    + oravTeam.getTeamColor().getChatColor().toString() + oravTeam.getName());
        }
    }


    @EventHandler
    private void onBlockBreak(BlockBurnEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.CHEST) {
            return;
        }
        teamChestManager.removeChest(block.getLocation());
    }

    @EventHandler
    private void onExplosion(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> {
            if (block.getType() == Material.CHEST) {
                if (getTeamIdOfBlock(block) != null) {
                    return true;
                }
            }
            return false;
        });
    }

    private boolean hasChestAccess(OravPlayer oravPlayer, Long teamId) {
        if (teamId == null) {
            return true;
        }
        if(helper.teamIsDroppedOut(teamId)){
            return true;
        }

        return oravPlayer.getOravTeam().getId().equals(teamId) || oravPlayer.isOravAdmin();
    }

    private boolean hasChestAccess(OravPlayer oravPlayer, Block block) {
        Long teamId = getTeamIdOfBlock(block);
        return hasChestAccess(oravPlayer, teamId);
    }

    private Long getTeamIdOfBlock(Block block) {
        Long teamId = null;
        BlockState blockState = block.getState();
        if (!(blockState instanceof Chest)) {
            return null;
        }
        Chest chest = (Chest) blockState;
        InventoryHolder inventoryHolder = chest.getInventory().getHolder();
        if (inventoryHolder instanceof DoubleChest) {
            DoubleChest doubleChest = (DoubleChest) inventoryHolder;
            Chest left = (Chest) doubleChest.getLeftSide();
            Chest right = (Chest) doubleChest.getRightSide();
            teamId = teamChestManager.getTeamOfChest(left.getLocation());
            if (teamId == null) {
                teamId = teamChestManager.getTeamOfChest(right.getLocation());
            }
        } else {
            teamId = teamChestManager.getTeamOfChest(chest.getLocation());
        }
        return teamId;
    }

}
