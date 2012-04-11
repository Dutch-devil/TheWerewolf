/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.Lennart.thewerewolf;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.craftbukkit.entity.CraftWolf;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.config.Configuration;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.player.SpoutPlayer;
import org.getspout.spoutapi.sound.SoundEffect;

/**
 *
 * @author Lennart
 */
public class MonsterProtection extends EntityListener {

    public static int HAND_DAMAGE = 6;
    public static int ITEM_DAMAGE = 2;
    public static double INFECT_CHANCE = 0.05;
    public static double SILVER_MULTIPLIER = 2;
    public static double ARMOR_MULTIPLIER = 0.7;
    public static String WEREWOLF_GROWL = "";
    public static ArrayList<Material> multipliedWeapons = new ArrayList();
    public static ArrayList<DamageCause> excludedDamage = new ArrayList();
    private Plugin plugin;

    public MonsterProtection(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onEntityDamage(EntityDamageEvent e) {
        if (!TheWerewolf.pluginEnabled) {
            return;
        }
        if (e.getEntity() instanceof CraftWolf) {
            CraftWolf wolf = (CraftWolf)e.getEntity();
            Player owner = (Player)wolf.getOwner();
            if (wolf.isTamed() && owner == null || owner != null && !owner.isOnline()) {
                e.setCancelled(true);
                return;
            }
        }
        if (e.getEntity() instanceof Player) {
            Player player = (Player)e.getEntity();
            if (WerewolfEditor.hasWerewolfSkin(player)) {
                if (e.getCause().equals(DamageCause.FALL)) {
                    e.setDamage((int)Math.max(player.getFallDistance() - Math.pow(SpoutManager.getPlayer(player).getJumpingMultiplier(), 2), 0));
                    return;
                }else if (!excludedDamage.contains(e.getCause())) {
                    e.setDamage((int)(e.getDamage() * ARMOR_MULTIPLIER));
                }
            }
        }
        if (e instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent ev = (EntityDamageByEntityEvent)e;
            if (ev.getDamager() instanceof Player) {
                Player player = (Player)ev.getDamager();
                if (WerewolfEditor.hasWerewolfSkin(player)) {
                    if (PlayerWerewolfListener.disallowed.contains(player.getItemInHand().getType())) {
                        e.setCancelled(true);
                        e.setDamage(0);
                        return;
                    }else if (player.getItemInHand().getType().equals(Material.AIR)) {
                        e.setDamage(HAND_DAMAGE);
                    }else {
                        e.setDamage(ITEM_DAMAGE);
                    }
                    if (ev.getEntity() instanceof Player) {
                        Player damagee = (Player)ev.getEntity();
                        if (TheWerewolf.permissionHandler != null && TheWerewolf.permissionHandler.permission(player, "thewerewolf.infectother") && TheWerewolf.permissionHandler.permission(damagee, "thewerewolf.becominfected")
                                || TheWerewolf.permissionHandler == null && player.isOp()) {
                            if (!WerewolfEditor.isWerewolf(damagee, false)) {
                                if (Math.random() < INFECT_CHANCE) {
                                    WerewolfEditor.makeWerewolf(damagee, false);
                                    TheWerewolf.awardAchievement(SpoutManager.getPlayer(damagee), "The Infection", "Contract the infection", Material.DEAD_BUSH);
                                    TheWerewolf.awardAchievement(SpoutManager.getPlayer(player), "The Spreader", "Infect a human", Material.SEEDS);
                                }
                            }
                        }
                    }
                }
                if (ev.getEntity() instanceof Player) {
                    Player damagee = (Player)ev.getEntity();
                    if (WerewolfEditor.hasWerewolfSkin(damagee)) {
                        Material damager = player.getItemInHand().getType();
                        if (multipliedWeapons.contains(damager)) {
                            e.setDamage((int)(e.getDamage() * SILVER_MULTIPLIER));
                        }
                        if (damagee.getHealth() - e.getDamage() <= 0) {
                            TheWerewolf.awardAchievement(SpoutManager.getPlayer(player), "New Sherrif in Town", "Kill a fellow werewolf", Material.IRON_SWORD);
                            player.sendMessage(ChatColor.YELLOW + "You took over " + damagee.getDisplayName() + "'s wolves!");
                            damagee.sendMessage(ChatColor.RED + player.getDisplayName() + " took over your wolves!");
                            for (Entity ent:damagee.getWorld().getEntities()) {
                                if (ent instanceof CraftWolf) {
                                    CraftWolf wolf = (CraftWolf)ent;
                                    if (((Player)wolf.getOwner()).equals(damagee)) {
                                        wolf.setOwner(player);
                                        wolf.setTamed(true);
                                        wolf.setSitting(false);
                                        wolf.setAngry(false);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (e.getEntity() != null && e.getEntity() instanceof Player) {
                Player player = (Player)e.getEntity();
                if (!WerewolfEditor.hasWerewolfSkin(player) && TheWerewolf.permissionHandler != null && TheWerewolf.permissionHandler.permission(player, "thewerewolf.becomeinfected")
                        || TheWerewolf.permissionHandler == null && player.isOp()) {
                    if (!WerewolfEditor.isWerewolf(player, false)) {
                        if (ev.getDamager() instanceof CraftWolf) {
                            if (Math.random() < INFECT_CHANCE) {
                                WerewolfEditor.makeWerewolf(player, false);
                                TheWerewolf.awardAchievement(SpoutManager.getPlayer(player), "The Infection", "Contract the infection", Material.DEAD_BUSH);
                            }
                        }
                    }
                }
            }
        }
        if (e.getDamage() <= 0) {
            e.setDamage(1);
        }
    }

    public void onEntityTarget(EntityTargetEvent e) {
        Entity target = e.getTarget();
        if (target instanceof Player) {
            Player player = (Player)target;
            if (WerewolfEditor.hasWerewolfSkin(player)) {
                if (WEREWOLF_GROWL.equals("")) {
                    SpoutManager.getSoundManager().playGlobalSoundEffect(SoundEffect.WOLF_GROWL, player.getLocation(), 20, 200);
                }else {
                    SpoutManager.getSoundManager().playGlobalCustomSoundEffect(plugin, WEREWOLF_GROWL, false, player.getLocation(), 20);
                }
            }
        }
    }
}
