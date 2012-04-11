/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.Lennart.thewerewolf;

import java.util.HashMap;
import java.util.Set;
import java.util.logging.Logger;
import nl.Lennart.rpgoverhead.RPGPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.config.Configuration;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.player.SpoutPlayer;

/**
 *
 * @author Lennart
 */
public class WerewolfEditor {

    public static String WEREWOLF_SKIN = "http://i51.tinypic.com/2zimvd2.png";
    public static String WEREWOLF_HOWL = "http://dl.dropbox.com/u/34361500/66398__Robinhood76__00829_wolf_howl_one_shot.wav";
    public static int HOWL_DISTANCE = 100;
    public static double SPEED = 1;
    public static double JUMP = 1;

    public static RPGPlugin plugin;

    private static HashMap<Player, WerewolfAttributes> werewolfs = new HashMap();

    public static boolean isWerewolf(Player player, boolean fullWolf) {
        WerewolfAttributes attributes = werewolfs.get(player);
        if (attributes == null) {
            Configuration config = plugin.getConfiguration();
            config.load();
            String wolfType = config.getString("Players." + player.getName() + ".Wolftype");
            if (wolfType == null) return false;
            wolfType = wolfType.toLowerCase();
            if (fullWolf && wolfType.equals("fullwolf") || !fullWolf && wolfType.equals("infectedWolf")) return true;
            return false;
        }
        if (!fullWolf) return true;
        return attributes.isWerewolf;
    }

    public static boolean makeWerewolf(Player player, boolean fullWolf) {
        if (!plugin.makeCharacterType(player)) return false;
        
        WerewolfAttributes attributes = werewolfs.get(player);
        if (attributes == null)
            werewolfs.put(player, new WerewolfAttributes(TheWerewolf.isEnabled ?
                                SpoutManager.getAppearanceManager().getSkinUrl(SpoutManager.getPlayer(player), player):"", fullWolf));
        else {
            attributes.isWerewolf = fullWolf;
            attributes.infectedThisNight = true;
        }
        Configuration config = plugin.getConfiguration();
        config.load();
        config.setProperty("Players." + player.getName() + ".Wolftype", fullWolf?"fullwolf":"infectedwolf");
        config.save();

        if (fullWolf && plugin.isNightInWorld(player.getWorld())) {
            setWerewolfSkin(player);
        }
        return true;
    }

    public static void unmakeWerewolf(Player player) {
        plugin.unmakeCharacterType(player);
        
        unsetWerewolfSkin(player);
        werewolfs.remove(player);
        Configuration config = plugin.getConfiguration();
        config.load();
        config.setProperty("Players." + player.getName() + ".Wolftype", "nowolf");
        config.save();
    }

    public static boolean hasWerewolfSkin(Player player) {
        if (!TheWerewolf.pluginEnabled) return false;
        WerewolfAttributes attributes = werewolfs.get(player);
        if (attributes == null) return false;
        return attributes.hasWerewolfSkin;
    }

    public static void setWerewolfSkin(Player player) {
        if (!TheWerewolf.pluginEnabled) return;
        WerewolfAttributes attributes = werewolfs.get(player);
        if (attributes == null) return;
        if (!attributes.isWerewolf) {
            if (attributes.infectedThisNight) return;
            if (player.getWorld().getHighestBlockYAt(player.getLocation()) > player.getLocation().getBlockY()) {
                player.sendMessage(ChatColor.LIGHT_PURPLE + "You feel a strange urge to go outside!");
                return;
            }
            attributes.isWerewolf = true;
        }

        attributes.hasWerewolfSkin = true;

        PlayerInventory inventory = player.getInventory();
        if (TheWerewolf.DROP_ITEMS) {
            for (int i = 0; i < inventory.getSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (stack != null && !stack.getType().equals(Material.AIR) && stack.getAmount() != 0) {
                    player.getWorld().dropItemNaturally(player.getLocation(), stack);
                    inventory.remove(stack);
                }
            }
        }
        for (ItemStack stack:inventory.getArmorContents()) {
            if (stack != null && !stack.getType().equals(Material.AIR) && stack.getAmount() != 0)
                player.getWorld().dropItemNaturally(player.getLocation(), stack);
        }
        player.getInventory().setArmorContents(new ItemStack[]{new ItemStack(Material.AIR), new ItemStack(Material.AIR),
                                                               new ItemStack(Material.AIR), new ItemStack(Material.AIR)});

        SpoutPlayer spoutplayer = SpoutManager.getPlayer(player);
        spoutplayer.setWalkingMultiplier(SPEED);
        spoutplayer.setJumpingMultiplier(JUMP);
        
        if (!TheWerewolf.isEnabled || !SpoutManager.getPlayer(player).isSpoutCraftEnabled()) {
            player.sendMessage("It's night, you just turned into a werewolf!");
            return;
        }

        TheWerewolf.awardAchievement(spoutplayer, "The Werewolf", "Turn into a werewolf", Material.PUMPKIN);

        Configuration config = plugin.getConfiguration();
        config.load();
        String skinName = config.getString("Players." + player.getName() + ".Skin");
        if (skinName.equals("random")) {
            Set<String> random = PlayerWerewolfListener.skins.keySet();
            random.remove("default");
            skinName = (String)random.toArray()[(int)(Math.random() * random.size())];
        }
        if (PlayerWerewolfListener.skins.get(skinName) == null) throw new IllegalArgumentException("No valid skin name: " + skinName);
        SpoutManager.getAppearanceManager().setGlobalSkin(player, PlayerWerewolfListener.skins.get(skinName));

        SpoutManager.getSoundManager().playGlobalCustomSoundEffect(plugin, WEREWOLF_HOWL, false, player.getLocation(), HOWL_DISTANCE, 80);
    }

    public static void unsetWerewolfSkin(Player player) {
        WerewolfAttributes attributes = werewolfs.get(player);
        if (attributes == null) return;
        attributes.infectedThisNight = false;
        if (!attributes.isWerewolf) return;

        attributes.hasWerewolfSkin = false;

        SpoutPlayer spoutplayer = SpoutManager.getPlayer(player);
        spoutplayer.setWalkingMultiplier(1);
        spoutplayer.setJumpingMultiplier(1);
        
        if (!TheWerewolf.isEnabled || !spoutplayer.isSpoutCraftEnabled()) {
            player.sendMessage("It's day, you just returned to your human form!");
            return;
        }
        SpoutManager.getAppearanceManager().setGlobalSkin(player, attributes.getSkin());
    }

    public static boolean canTransform(Player player) {
        WerewolfAttributes attributes = werewolfs.get(player);
        if (attributes == null) return false;
        return !attributes.isWerewolf && !attributes.infectedThisNight;
    }

    public static void setForwardDown(Player player, boolean down) {
        WerewolfAttributes attributes = werewolfs.get(player);
        if (attributes == null) return;
        attributes.forwardDown = down;
    }

    public static boolean hasSpeedIncrease(Player player) {
        if (!TheWerewolf.pluginEnabled) return false;
        WerewolfAttributes attributes = werewolfs.get(player);
        if (attributes == null || !attributes.hasWerewolfSkin) return false;
        if (TheWerewolf.isEnabled && SpoutManager.getPlayer(player).isSpoutCraftEnabled())
            return attributes.forwardDown;
        else
            return true;
    }

    private static class WerewolfAttributes {

        public boolean hasWerewolfSkin, isWerewolf, infectedThisNight = true;
        private String skin;
        public boolean forwardDown;

        public WerewolfAttributes(String skin, boolean isWerewolf) {
            this.isWerewolf = isWerewolf;
            this.skin = skin;
        }

        public String getSkin() {
            return skin;
        }

    }

}
