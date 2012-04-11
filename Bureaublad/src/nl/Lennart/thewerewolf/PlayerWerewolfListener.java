/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.Lennart.thewerewolf;

import nl.Lennart.rpgoverhead.TimeListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.craftbukkit.entity.CraftWolf;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.config.Configuration;
import org.getspout.spoutapi.SpoutManager;

/**
 *
 * @author Lennart
 */
public class PlayerWerewolfListener extends PlayerListener {

    public static int WOLF_DISTANCE = 10;
    public static double CURE_CHANCE = 0.25;
    public static String CHAT_TAG = ChatColor.YELLOW + "[Werewolf] ";
    public static boolean NO_TOOLS = false;
    public static HashSet<Material> disallowed = new HashSet();
    public static HashMap<Integer, String> wolfMessage = new HashMap();
    public static HashMap<String, String> skins;
    public static int totalChance = 0;
    public static boolean shouldUpdate = false;
    public static boolean INSTA_FRIENDLY = true;
    private Plugin plugin;

    public PlayerWerewolfListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPlayerJoin(final PlayerJoinEvent e) {
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {

            public void run() {
                Player player = e.getPlayer();
                Configuration config = plugin.getConfiguration();
                config.load();
                String wolfType = config.getString("Players." + player.getName() + ".Wolftype", "nowolf").toLowerCase();

                String skinName = config.getString("Players." + player.getName() + ".Skin", null);
                if (skinName == null || !skins.containsKey(skinName)) {
                    skinName = skins.get("default");
                    if (skinName == null || !skinName.equals("random") && !skins.containsKey(skinName)) {
                        skinName = "random";
                    }else if (skinName.equals("random")) {
                        Set<String> random = skins.keySet();
                        random.remove("default");
                        skinName = (String)random.toArray()[(int)(Math.random() * random.size())];
                    }
                    config.setProperty("Players." + player.getName() + ".Skin", skinName);
                    config.save();
                }
                
                if (wolfType.equals("fullwolf")) {
                    WerewolfEditor.makeWerewolf(player, true);
                }else if (wolfType.equals("infectedwolf")) {
                    WerewolfEditor.makeWerewolf(player, false);
                }
            }
        }, 100L);
    }

    @Override
    public void onPlayerRespawn(final PlayerRespawnEvent e) {
        if (!TheWerewolf.pluginEnabled) {
            return;
        }
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {

            public void run() {
                if (TimeListener.isNightInWorld(e.getRespawnLocation().getWorld()) && WerewolfEditor.isWerewolf(e.getPlayer(), true)) {
                    WerewolfEditor.setWerewolfSkin(e.getPlayer());
                }
            }
        }, 1L);
    }

    @Override
    public void onPlayerPortal(final PlayerPortalEvent e) {
        if (!TheWerewolf.pluginEnabled) {
            return;
        }
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {

            public void run() {
                if (TimeListener.isNightInWorld(e.getTo().getWorld()) && WerewolfEditor.isWerewolf(e.getPlayer(), true)) {
                    WerewolfEditor.setWerewolfSkin(e.getPlayer());
                }
            }
        }, 1L);
    }

    @Override
    public void onPlayerMove(PlayerMoveEvent e) {
        if (!TheWerewolf.pluginEnabled) {
            return;
        }
        Player player = e.getPlayer();
        if (WerewolfEditor.canTransform(player)) {
            if (player.getWorld().getHighestBlockYAt(player.getLocation()) <= player.getLocation().getBlockY()) {
                WerewolfEditor.makeWerewolf(player, true);
            }
        }

        if (!WerewolfEditor.hasWerewolfSkin(player)) {
            return;
        }

        if (shouldUpdate && INSTA_FRIENDLY) {
            shouldUpdate = false;
            int wolfsOwned = 0;
            for (Entity entity:player.getNearbyEntities(WOLF_DISTANCE, WOLF_DISTANCE, WOLF_DISTANCE)) {
                if (entity instanceof CraftWolf) {
                    CraftWolf wolf = (CraftWolf)entity;
                    if (wolf.isTamed() && wolf.getOwner() != null) {
                        if (((Player)wolf.getOwner()).equals(player)) {
                            wolfsOwned++;
                            continue;
                        }
                        if (WerewolfEditor.isWerewolf(player.getServer().getPlayer(((Player)wolf.getOwner()).getName()), true)) {
                            continue;
                        }
                        player.sendMessage("You tamed " + ((Player)wolf.getOwner()).getName() + "'s wolf!");
                    }else {
                        player.sendMessage("You added a wild wolf to your pack!");
                    }
                    wolf.setOwner(player);
                    wolf.setTamed(true);
                    wolfsOwned++;
                }
            }
            if (wolfsOwned >= 3) {
                TheWerewolf.awardAchievement(SpoutManager.getPlayer(player), "The Packleader", "Have some wolf companions", Material.JACK_O_LANTERN);
            }
        }
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (!TheWerewolf.pluginEnabled) {
            return;
        }
        if (e.getAction().equals(Action.PHYSICAL)) {
            return;
        }

        Player player = e.getPlayer();
        Material type = player.getItemInHand().getType();

        if (type.equals(TheWerewolf.result)) {
            if (TheWerewolf.permissionHandler != null && TheWerewolf.permissionHandler.permission(player, "thewerewolf.cure")
                    || TheWerewolf.permissionHandler == null && player.isOp()) {
                e.setCancelled(true);
                if (WerewolfEditor.isWerewolf(player, false)) {
                    if (!WerewolfEditor.hasWerewolfSkin(player)) {
                        if (Math.random() < CURE_CHANCE) {
                            WerewolfEditor.unmakeWerewolf(player);
                        }
                        player.sendMessage(ChatColor.LIGHT_PURPLE + "YUK! That tastes awfull! Why would you do that!?");
                    }else {
                        player.sendMessage(ChatColor.LIGHT_PURPLE + "AWOOooo! All that does is hurt my throat, I can't even digest it!");
                    }
                }else {
                    player.sendMessage(ChatColor.LIGHT_PURPLE + "YUK! That tastes awfull! Why would you do that!?");
                }

                if (player.getItemInHand().getAmount() == 1) {
                    player.getInventory().remove(new ItemStack(TheWerewolf.result, 1));
                }else {
                    player.getItemInHand().setAmount(player.getItemInHand().getAmount() - 1);
                }
                player.updateInventory();
            }
        }else if (disallowed.contains(type) && WerewolfEditor.hasWerewolfSkin(player)) {
            e.setCancelled(true);
        }else if (NO_TOOLS) {
            if (e.getMaterial().toString().endsWith("_PICKAXE")) {
                e.setCancelled(true);
            }
        }
    }

    @Override
    public void onPlayerChat(PlayerChatEvent e) {
        if (!TheWerewolf.pluginEnabled || !TheWerewolf.SCRAMBLE_CHAT) {
            return;
        }
        Player player = e.getPlayer();
        if (WerewolfEditor.hasWerewolfSkin(player)) {
            e.setCancelled(true);
            String alternativeMessage = "";
            int message = (int)(Math.random() * totalChance);
            for (Integer chance:wolfMessage.keySet()) {
                if (message < chance) {
                    alternativeMessage = wolfMessage.get(chance);
                    break;
                }
                message -= chance;
            }
            for (Player receiver:e.getRecipients()) {
                if (WerewolfEditor.hasWerewolfSkin(receiver)) {
                    receiver.sendMessage(CHAT_TAG + ChatColor.WHITE + player.getName() + ": " + e.getMessage());
                }else {
                    receiver.sendMessage(CHAT_TAG + ChatColor.WHITE + player.getName() + ": " + alternativeMessage);
                }
            }
        }
    }

    @Override
    public void onPlayerPickupItem(PlayerPickupItemEvent e) {
        if (!TheWerewolf.pluginEnabled || !TheWerewolf.DROP_ITEMS) {
            return;
        }
        Player player = e.getPlayer();
        if (WerewolfEditor.hasWerewolfSkin(player)) {
            e.setCancelled(true);
        }
    }
}
