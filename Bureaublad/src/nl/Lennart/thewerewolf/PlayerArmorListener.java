/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.Lennart.thewerewolf;

import org.bukkit.inventory.ItemStack;
import org.getspout.spoutapi.event.inventory.InventoryClickEvent;
import org.getspout.spoutapi.event.inventory.InventoryListener;
import org.getspout.spoutapi.event.inventory.InventorySlotType;

/**
 *
 * @author Lennart
 */
public class PlayerArmorListener extends InventoryListener {

    public void onInventoryClick(InventoryClickEvent e) {
        if (!TheWerewolf.pluginEnabled) return;
        if (!WerewolfEditor.hasWerewolfSkin(e.getPlayer())) return;
        if (TheWerewolf.DROP_ITEMS) {
            e.setCancelled(true);
        }else {
            InventorySlotType type = e.getSlotType();
            if (type.equals(InventorySlotType.ARMOR) || type.equals(InventorySlotType.BOOTS) ||
                    type.equals(InventorySlotType.HELMET) || type.equals(InventorySlotType.LEGGINGS)) {
                ItemStack item = e.getCursor();
                if (item == null) return;
                e.setCancelled(true);
            }
        }
    }

}
