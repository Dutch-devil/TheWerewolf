/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.Lennart.thewerewolf;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.event.inventory.InventoryCraftEvent;
import org.getspout.spoutapi.event.inventory.InventoryListener;
import org.getspout.spoutapi.event.inventory.InventorySlotType;

/**
 *
 * @author Lennart
 */
public class WerewolfCraftListener extends InventoryListener {

    private Plugin plugin;

    public WerewolfCraftListener(Plugin plugin) {
        this.plugin = plugin;
    }

    public void onInventoryCraft(InventoryCraftEvent e) {
        Player player = e.getPlayer();
        if (e.isLeftClick() && e.getSlotType().equals(InventorySlotType.RESULT) && e.getResult() != null && e.getResult().getType().equals(TheWerewolf.result)) {
            TheWerewolf.awardAchievement(SpoutManager.getPlayer(player), "The Cure", "Make a werewolfism-cure", Material.DEAD_BUSH);
        }
    }

}
