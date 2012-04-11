/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.Lennart.thewerewolf;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 *
 * @author Lennart
 */
public class AlternativeArmorListener extends PlayerListener {

    public static boolean shouldUpdate = false;

    public void onPlayerMove(PlayerMoveEvent e) {
        if (!TheWerewolf.pluginEnabled || !shouldUpdate) return;
        shouldUpdate = false;
        Player player = e.getPlayer();
        ItemStack[] armor = player.getInventory().getArmorContents();
        outer:if (true) {
            for (ItemStack armorItem:armor) {
                if (!armorItem.getType().equals(Material.AIR)) break outer;
            }
            return;
        }
        if (WerewolfEditor.hasWerewolfSkin(player)) {
            PlayerInventory inventory = player.getInventory();
            for (ItemStack stack:inventory.getArmorContents()) {
                if (stack != null && !stack.getType().equals(Material.AIR) && stack.getAmount() != 0)
                    player.getWorld().dropItemNaturally(player.getLocation(), stack);
            }
            player.getInventory().setArmorContents(new ItemStack[]{new ItemStack(Material.AIR), new ItemStack(Material.AIR),
                                                                   new ItemStack(Material.AIR), new ItemStack(Material.AIR)});
        }
    }

}
