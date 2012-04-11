/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.Lennart.thewerewolf;

import nl.Lennart.rpgoverhead.TimeListener;
import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;
import nl.Lennart.customachievements.*;
import nl.Lennart.rpgoverhead.RPGPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.config.Configuration;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.player.SpoutPlayer;

/**
 *
 * @author Lennart
 */
public class TheWerewolf extends RPGPlugin {

    public static boolean pluginEnabled = false;
    public static boolean isEnabled = false;
    public static PermissionHandler permissionHandler;
    public static boolean DROP_ITEMS = false;
    public static boolean SCRAMBLE_CHAT = false;
    public static int NIGHT_START = 13000;
    public static int NIGHT_END = 23000;
    private static final Material[] recipe = new Material[9];
    public static Material result;
    public static AchievementGrid werewolfGrid;
    private static Plugin plugin;
    private static String[] moons;
    private static HashMap<World, String> worldMoons = new HashMap();

    public static void awardAchievement(SpoutPlayer player, String achievementName, String description, Material mat) {
        if (TheWerewolf.werewolfGrid == null) {
            Configuration config = plugin.getConfiguration();
            config.load();
            if (!config.getBoolean("Players." + player.getName() + ".Achievements." + achievementName, false)) {
                SpoutManager.getPlayer(player).sendNotification(achievementName, description, mat);
                config.setProperty("Players." + player.getName() + ".Achievements." + achievementName, true);
            }
            config.save();
        }else {
            try {
                TheWerewolf.werewolfGrid.awardAchievement(achievementName, SpoutManager.getPlayer(player));
            }catch (NoSuchFieldException ex) {
                ex.printStackTrace();
            }

        }
    }

    @Override
    public void update(World world, int ticks) {
        PlayerWerewolfListener.shouldUpdate = true;
        AlternativeArmorListener.shouldUpdate = true;

        if (isNightInWorld(world)) {
            String moonUrl = worldMoons.get(world);
            if (moonUrl == null) {
                moonUrl = "!@!" + moons[0];
                worldMoons.put(world, moonUrl);
            }else if (!moonUrl.startsWith("!@!")) {
                int i = 0;
                for (; i < moons.length; i++) {
                    if (moons[i].equals(moonUrl)) break;
                }
                if (++i >= moons.length) i = 0;
                moonUrl = "!@!" + moons[i];
                worldMoons.put(world, moonUrl);
            }
        }else {
            String moonUrl = worldMoons.get(world);
            if (moonUrl == null || moonUrl.startsWith("!@!")) {
                if (moonUrl == null) moonUrl = moons[0];
                else moonUrl = moonUrl.substring(3);
                worldMoons.put(world, moonUrl);
                for (Player player:world.getPlayers()) {
                    SpoutPlayer spoutplayer = SpoutManager.getPlayer(player);
                    SpoutManager.getSkyManager().setMoonTextureUrl(spoutplayer, moonUrl.equals("default")?null:moonUrl);
                }
            }
        }
    }

    @Override
    public void onRPGDisable() {
        pluginEnabled = false;
        saveVariables();
    }

    @Override
    public void onRPGEnable() {
        pluginEnabled = true;
        PluginManager pm = this.getServer().getPluginManager();

        Logger logger = Logger.getLogger("minecraft");
        if (!pm.isPluginEnabled("Spout")) {
            logger.info("[TheWerewolf] Spout is not enabled on this server, disabling spout parts...");
        }else {
            isEnabled = true;
        }

        WerewolfEditor.plugin = this;
        plugin = this;

        PlayerWerewolfListener listener = new PlayerWerewolfListener(this);
        pm.registerEvent(Type.PLAYER_JOIN, listener, Priority.Normal, this);
        pm.registerEvent(Type.PLAYER_RESPAWN, listener, Priority.Normal, this);
        pm.registerEvent(Type.PLAYER_PORTAL, listener, Priority.Normal, this);
        pm.registerEvent(Type.PLAYER_MOVE, listener, Priority.Normal, this);
        pm.registerEvent(Type.PLAYER_INTERACT, listener, Priority.Normal, this);
        pm.registerEvent(Type.PLAYER_CHAT, listener, Priority.Normal, this);
        pm.registerEvent(Type.PLAYER_PICKUP_ITEM, listener, Priority.Normal, this);

        MonsterProtection monsters = new MonsterProtection(this);
        pm.registerEvent(Type.ENTITY_DAMAGE, monsters, Priority.Normal, this);
        if (TheWerewolf.isEnabled) {
            pm.registerEvent(Type.ENTITY_TARGET, monsters, Priority.Normal, this);
            pm.registerEvent(Type.CUSTOM_EVENT, new WerewolfKeyListener(), Priority.High, this);
            pm.registerEvent(Type.CUSTOM_EVENT, new PlayerArmorListener(), Priority.Normal, this);
            pm.registerEvent(Type.CUSTOM_EVENT, new WerewolfCraftListener(this), Priority.Low, this);
        }else {
            pm.registerEvent(Type.PLAYER_MOVE, new AlternativeArmorListener(), Priority.Low, this);
        }

        if (permissionHandler == null) {
            Plugin permissionsPlugin = this.getServer().getPluginManager().getPlugin("Permissions");
            if (permissionsPlugin == null) {
                logger.info("[TheWerewolf] Permission system not detected, defaulting to OP");
            }else {
                permissionHandler = ((Permissions)permissionsPlugin).getHandler();
                logger.info("[TheWerewolf] Found and will use plugin " + ((Permissions)permissionsPlugin).getDescription().getFullName());
            }
        }

        loadVariables();
        addRecipe();

        if (getServer().getPluginManager().getPlugin("CustomAchievements") != null) {
            try {
                werewolfGrid = new AchievementGrid(25, 13, null, "The Werewolf Achievements");
                ConnectableAchievement center = werewolfGrid.addAchievement("The Infection", "Contract the infection", new ItemStack(Material.DEAD_BUSH), new Point(12, 6), null);
                ConnectableAchievement werewolf = werewolfGrid.addAchievement("The Werewolf", "Turn into a werewolf", new ItemStack(Material.PUMPKIN), new Point(15, 6), center);
                werewolfGrid.addAchievement("The Cure", "Make a werewolfism cure", new ItemStack(Material.SLIME_BALL), new Point(9, 6), center);
                werewolfGrid.addAchievement("The Packleader", "Have some wolf companions", new ItemStack(Material.JACK_O_LANTERN), new Point(19, 2), werewolf);
                werewolfGrid.addAchievement("The Spreader", "Infect a human", new ItemStack(Material.SEEDS), new Point(19, 6), werewolf);
                werewolfGrid.addAchievement("New Sherrif in Town", "Kill a fellow werewolf", new ItemStack(Material.IRON_SWORD), new Point(19, 10), werewolf);
                AchievementWindow.addAchievementGrid(werewolfGrid);
            }catch (Exception e) {
                e.printStackTrace();
                werewolfGrid = null;
            }
        }
    }

    @Override
    public boolean hasTransformation() {
        return true;
    }

    @Override
    public int getTimeStart() {
        return NIGHT_START;
    }

    @Override
    public int getTimeEnd() {
        return NIGHT_END;
    }

    @Override
    public void transform(Player player) {
        if (!moons[0].equals(worldMoons.get(player.getWorld()).substring(3))) {
            if (WerewolfEditor.isWerewolf(player, true)) {
                WerewolfEditor.setWerewolfSkin(player);
            }
        }
    }

    @Override
    public void untransform(Player player) {
        WerewolfEditor.unsetWerewolfSkin(player);
    }

    public void loadVariables() {
        Configuration config = this.getConfiguration();
        config.load();
        MonsterProtection.ITEM_DAMAGE = config.getInt("Modifiers.Wolf.Item_Damage", 2);
        MonsterProtection.HAND_DAMAGE = config.getInt("Modifiers.Wolf.Hand_Damage", 6);
        MonsterProtection.INFECT_CHANCE = config.getDouble("Modifiers.Wolf.Infect_Chance", 0.05);
        MonsterProtection.SILVER_MULTIPLIER = config.getDouble("Modifiers.Wolf.Silver_Multiplier", 2);
        MonsterProtection.ARMOR_MULTIPLIER = config.getDouble("Modifiers.Wolf.Armor_Multiplier", 0.8);
        WerewolfEditor.SPEED = config.getDouble("Modifiers.Wolf.Speed_Multiplier", 1.5);
        WerewolfEditor.JUMP = config.getDouble("Modifiers.Wolf.Jump_Multiplier", 1.5);
        PlayerWerewolfListener.WOLF_DISTANCE = config.getInt("Modifiers.Wolf.Wolf_Distance", 10);
        PlayerWerewolfListener.CURE_CHANCE = config.getDouble("Modifiers.Wolf.Cure_Chance", 0.5);
        PlayerWerewolfListener.INSTA_FRIENDLY = config.getBoolean("Modifiers.Wolf.Insta_Friendly", true);
        String[] disallowedItems = config.getString("Disallowed.Items", "WOOD_SWORD, STONE_SWORD, IRON_SWORD, DIAMOND_SWORD, BOW").split(",");
        PlayerWerewolfListener.disallowed = new HashSet();
        for (String item:disallowedItems) {
            try {
                Material mat = null;
                item = item.trim().toUpperCase();
                try {
                    mat = Material.getMaterial(Integer.parseInt(item));
                }catch (NumberFormatException e) {
                    mat = Material.getMaterial(item);
                }
                if (mat == null) {
                    Logger.getLogger("minecraft").info("[TheWerewolf] '" + item + "' is an invalid material or id for weapon!");
                }else {
                    PlayerWerewolfListener.disallowed.add(mat);
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
        String[] disallowedDamage = config.getString("Disallowed.Damage", "DROWNING, FIRE, FIRE_TICK, SUFFOCATION, VOID, SUICIDE").split(",");
        MonsterProtection.excludedDamage = new ArrayList();
        for (String item:disallowedDamage) {
            try {
                DamageCause cause = null;
                item = item.trim();
                cause = DamageCause.valueOf(item.toUpperCase());
                if (cause == null) {
                    Logger.getLogger("minecraft").info("[TheWerewolf] '" + item + "' is an invalid material or id for damage cause!");
                }else {
                    MonsterProtection.excludedDamage.add(cause);
                }
            }catch (Exception e) {
                e.printStackTrace();
                Logger.getLogger("minecraft").info("[TheWerewolf] '" + item + "' is invalid!");
            }
        }
        String[] multipliedItems = config.getString("Multiplied.Weapons", "IRON_AXE, IRON_BOOTS, IRON_CHESTPLATE, IRON_DOOR, "
                + "IRON_HELMET, IRON_HOE, IRON_INGOT, IRON_LEGGINGS, "
                + "IRON_PICKAXE, IRON_SPADE, IRON_SWORD").split(",");
        MonsterProtection.multipliedWeapons = new ArrayList();
        for (String item:multipliedItems) {
            try {
                Material mat = null;
                item = item.trim();
                try {
                    mat = Material.getMaterial(Integer.parseInt(item));
                }catch (NumberFormatException e) {
                    mat = Material.getMaterial(item.toUpperCase());
                }
                if (mat == null) {
                    Logger.getLogger("minecraft").info("[TheWerewolf] '" + item + "' is an invalid material or id for weapon!");
                }else {
                    MonsterProtection.multipliedWeapons.add(mat);
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
        WerewolfEditor.WEREWOLF_SKIN = config.getString("Files.Skin", "http://i51.tinypic.com/2zimvd2.png");
        WerewolfEditor.WEREWOLF_HOWL = config.getString("Files.Howl", "http://dl.dropbox.com/u/34361500/66398__Robinhood76__00829_wolf_howl_one_shot.wav");
        MonsterProtection.WEREWOLF_GROWL = config.getString("Files.Growl", "");
        WerewolfEditor.HOWL_DISTANCE = config.getInt("Modifiers.Wolf.Howl_Distance", 100);
        NIGHT_START = config.getInt("Modifiers.Night.Start", 13000);
        NIGHT_END = config.getInt("Modifiers.Night.End", 23000);
        DROP_ITEMS = config.getBoolean("Modifiers.Hardcore.Drop_Items", false);
        SCRAMBLE_CHAT = config.getBoolean("Modifiers.Hardcore.Scramble_Chat", false);
        PlayerWerewolfListener.NO_TOOLS = config.getBoolean("Modifiers.Hardcore.No_Tools", false);

        String[] recipeMaterials = config.getString("Recipe.Materials", "SLIME_BALL, null, SLIME_BALL, null, SAPLING, null, "
                + "SLIME_BALL, MUSHROOM_SOUP, SLIME_BALL").split(",");
        for (int i = 0; i < 9; i++) {
            try {
                if (i >= recipeMaterials.length) {
                    continue;
                }
                Material mat = null;
                String item = recipeMaterials[i].trim();
                if (item.equalsIgnoreCase("null")) {
                    continue;
                }
                try {
                    mat = Material.getMaterial(Integer.parseInt(item));
                }catch (NumberFormatException e) {
                    mat = Material.getMaterial(item.toUpperCase());
                }
                if (mat == null) {
                    Logger.getLogger("minecraft").info("[TheWerewolf] '" + recipeMaterials[i] + "' is an invalid material or id for the recipe!");
                }else {
                    recipe[i] = mat;
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
        String recipeResult = config.getString("Recipe.Result", "DEAD_BUSH");
        Material mat = null;
        try {
            mat = Material.getMaterial(Integer.parseInt(recipeResult.trim()));
        }catch (NumberFormatException e) {
            mat = Material.getMaterial(recipeResult.trim().toUpperCase());
        }
        if (mat == null) {
            Logger.getLogger("minecraft").info("[TheWerewolf] '" + recipeResult + "' is an invalid material or id for the recipe!");
        }else {
            result = mat;
        }
        PlayerWerewolfListener.CHAT_TAG = config.getString("WolfMessageTag", ChatColor.YELLOW + "[Werewolf] ");
        List<String> messages = config.getKeys("WolfMessage");
        PlayerWerewolfListener.totalChance = 0;
        if (messages != null) {
            for (String message:messages) {
                int chance = config.getInt("WolfMessage." + message, 10);
                PlayerWerewolfListener.wolfMessage.put(chance, message);
                PlayerWerewolfListener.totalChance += chance;
            }
        }else {
            PlayerWerewolfListener.wolfMessage.put(35, "*Grunting noise*");
            PlayerWerewolfListener.wolfMessage.put(40, "*Growling noise*");
            PlayerWerewolfListener.wolfMessage.put(20, "*HOWLLLLLLLLL!*");
            PlayerWerewolfListener.wolfMessage.put(5, "*Crying sound*");
            PlayerWerewolfListener.totalChance = 100;
        }

        moons = config.getString("Moon_Cycle", "default, http://astronomyonline.org/Moon.png").split(",");
        for (int i = 0; i < moons.length; i++) moons[i] = moons[i].trim();

        PlayerWerewolfListener.skins = new HashMap();
        List<String> skinUrls = config.getKeys("Skins");
        if (skinUrls != null) {
            for (String skinName:skinUrls) {
                String skinUrl = config.getString("Skins." + skinName);
                PlayerWerewolfListener.skins.put(skinName, skinUrl);
            }
        }else {
            PlayerWerewolfListener.skins.put("default", "random");
            PlayerWerewolfListener.skins.put("brown", "http://s3.amazonaws.com/squirt/i4e4d559f4f0440762692015907277237181.png");
            PlayerWerewolfListener.skins.put("grey", "http://i51.tinypic.com/2zimvd2.png");
            PlayerWerewolfListener.skins.put("black", "http://s3.amazonaws.com/squirt/i4e4d53898a6ff62892015877902827282328181.png");
        }
    }

    public void saveVariables() {
        Configuration config = this.getConfiguration();
        config.setProperty("Modifiers.Wolf.Item_Damage", MonsterProtection.ITEM_DAMAGE);
        config.setProperty("Modifiers.Wolf.Hand_Damage", MonsterProtection.HAND_DAMAGE);
        config.setProperty("Modifiers.Wolf.Infect_Chance", MonsterProtection.INFECT_CHANCE);
        config.setProperty("Modifiers.Wolf.Silver_Multiplier", MonsterProtection.SILVER_MULTIPLIER);
        config.setProperty("Modifiers.Wolf.Armor_Multiplier", MonsterProtection.ARMOR_MULTIPLIER);
        config.setProperty("Modifiers.Wolf.Speed_Multiplier", WerewolfEditor.SPEED);
        config.setProperty("Modifiers.Wolf.Jump_Multiplier", WerewolfEditor.JUMP);
        config.setProperty("Modifiers.Wolf.Wolf_Distance", PlayerWerewolfListener.WOLF_DISTANCE);
        config.setProperty("Modifiers.Wolf.Cure_Chance", PlayerWerewolfListener.CURE_CHANCE);
        config.setProperty("Modifiers.Wolf.Insta_Friendly", PlayerWerewolfListener.INSTA_FRIENDLY);
        String disallowedItems = "";
        if (PlayerWerewolfListener.disallowed.size() > 0) {
            for (Material mat:PlayerWerewolfListener.disallowed) {
                disallowedItems += mat + ", ";
            }
            disallowedItems = disallowedItems.substring(0, disallowedItems.length() - 2);
        }
        config.setProperty("Disallowed.Items", disallowedItems);
        String disallowedDamage = "";
        if (MonsterProtection.excludedDamage.size() > 0) {
            for (DamageCause cause:MonsterProtection.excludedDamage) {
                disallowedDamage += cause + ", ";
            }
            disallowedDamage = disallowedDamage.substring(0, disallowedDamage.length() - 2);
        }
        config.setProperty("Disallowed.Damage", disallowedDamage);
        String multipliedWeapons = "";
        if (MonsterProtection.multipliedWeapons.size() > 0) {
            for (Material mat:MonsterProtection.multipliedWeapons) {
                multipliedWeapons += mat + ", ";
            }
            multipliedWeapons = multipliedWeapons.substring(0, multipliedWeapons.length() - 2);
        }
        config.setProperty("Multiplied.Weapons", multipliedWeapons);
        config.setProperty("Files.Skin", WerewolfEditor.WEREWOLF_SKIN);
        config.setProperty("Files.Howl", WerewolfEditor.WEREWOLF_HOWL);
        config.setProperty("Files.Growl", MonsterProtection.WEREWOLF_GROWL);
        config.setProperty("Modifiers.Wolf.Howl_Distance", WerewolfEditor.HOWL_DISTANCE);
        config.setProperty("Modifiers.Night.Start", NIGHT_START);
        config.setProperty("Modifiers.Night.End", NIGHT_END);
        config.setProperty("Modifiers.Hardcore.Drop_Items", DROP_ITEMS);
        config.setProperty("Modifiers.Hardcore.Scramble_Chat", SCRAMBLE_CHAT);
        config.setProperty("Modifiers.Hardcore.No_Tools", PlayerWerewolfListener.NO_TOOLS);

        String recipeMaterials = recipe[0].toString();
        for (int i = 1; i < 9; i++) {
            recipeMaterials += ", " + recipe[i];
        }
        config.setProperty("Recipe.Materials", recipeMaterials);
        config.setProperty("Recipe.Result", result.toString());
        config.setProperty("WolfMessageTag", PlayerWerewolfListener.CHAT_TAG);
        for (int chance:PlayerWerewolfListener.wolfMessage.keySet()) {
            String message = PlayerWerewolfListener.wolfMessage.get(chance);
            config.setProperty("WolfMessage." + message, chance);
        }
        if (moons.length != 0) {
            String total = "";
            for (String url:moons) {
                total += (url.startsWith("!@!")?url.substring(3):url) + ", ";
            }
            config.setProperty("Moon_Cycle", total.substring(0, total.length() - 2));
        }

        for (String skinName:PlayerWerewolfListener.skins.keySet()) {
            String skinUrl = PlayerWerewolfListener.skins.get(skinName);
            config.setProperty("Skins." + skinName, skinUrl);
        }
        config.save();
    }

    public void addRecipe() {
        ShapedRecipe addRecipe = new ShapedRecipe(new ItemStack(result, 1));
        addRecipe.shape("abc", "def", "ghi");
        for (int i = 0; i < 9; i++) {
            if (recipe[i] != null) {
                addRecipe.setIngredient((char)('a' + i), recipe[i]);
            }
        }
        Logger.getLogger("minecraft").info("[TheWerewolf] added recipe for cure");
        this.getServer().addRecipe(addRecipe);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (label.equalsIgnoreCase("thewerewolf")) {
            Player player = null;
            if (sender instanceof Player) {
                player = (Player)sender;
            }
            if (player == null
                    || (permissionHandler != null && permissionHandler.permission(player, "thewerewolf.thewerewolf")
                    || permissionHandler == null && player.isOp())) {
                pluginEnabled = !pluginEnabled;
                Logger.getLogger("minecraft").info("[TheWerewolf] has been " + (pluginEnabled ? "enabled" : "disabled"));
                for (World world:this.getServer().getWorlds()) {
                    if (!pluginEnabled) {
                        saveVariables();
                        for (Player werewolf:world.getPlayers()) {
                            WerewolfEditor.unsetWerewolfSkin(werewolf);
                        }
                    }else {
                        loadVariables();
                        if (TimeListener.isNightInWorld(world)) {
                            for (Player werewolf:world.getPlayers()) {
                                WerewolfEditor.setWerewolfSkin(werewolf);
                            }
                        }
                    }
                }
            }
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }
        Player player = (Player)sender;
        if (label.equalsIgnoreCase("togglewerewolf")) {
            if (permissionHandler != null && permissionHandler.permission(player, "thewerewolf.togglewerewolf")
                    || permissionHandler == null && player.isOp()) {
                if (args.length > 0) {
                    try {
                        Player newWerewolf = this.getServer().getPlayer(args[0]);
                        if (newWerewolf == null) {
                            throw new Exception();
                        }
                        if (!WerewolfEditor.isWerewolf(newWerewolf, false)) {
                            if (WerewolfEditor.makeWerewolf(newWerewolf, true)) {
                                player.sendMessage(ChatColor.GREEN + args[0] + " now is a werewolf! ");
                                newWerewolf.sendMessage(ChatColor.YELLOW + "You have been turned into a werewolf by " + player.getName() + "!");
                            }else {
                                player.sendMessage(ChatColor.RED + args[0] + " is already an incompatible class!");
                            }
                        }else {
                            WerewolfEditor.unmakeWerewolf(newWerewolf);
                            player.sendMessage(ChatColor.GREEN + args[0] + " now isn't a werewolf anymore....");
                            newWerewolf.sendMessage(ChatColor.YELLOW + "You have been unturned from a werewolf by " + player.getName() + "!");
                        }
                    }catch (Exception e) {
                        player.sendMessage(ChatColor.RED + "Invalid player name...");
                    }
                }else {
                    player.sendMessage(ChatColor.RED + "Please specify a name!");
                }
            }
        }else if (label.equalsIgnoreCase("togglewolfself")) {
            if (permissionHandler != null && permissionHandler.permission(player, "thewerewolf.toggleself")
                    || permissionHandler == null && player.isOp()) {
                if (!WerewolfEditor.isWerewolf(player, false)) {
                    if (WerewolfEditor.makeWerewolf(player, true)) {
                        player.sendMessage(ChatColor.GREEN + "You now are a werewolf!");
                    }else {
                        player.sendMessage(ChatColor.RED + "You are already an incompatible class!");
                    }
                }else {
                    WerewolfEditor.unmakeWerewolf(player);
                    player.sendMessage(ChatColor.GREEN + "You now aren't a werewolf anymore....");
                }
            }
        }else if (label.equalsIgnoreCase("infectwerewolf")) {
            if (permissionHandler != null && permissionHandler.permission(player, "thewerewolf.infectwerewolf")
                    || permissionHandler == null && player.isOp()) {
                if (args.length > 0) {
                    try {
                        Player newInfection = this.getServer().getPlayer(args[0]);
                        if (newInfection == null) {
                            throw new Exception();
                        }
                        if (!WerewolfEditor.isWerewolf(newInfection, false)) {
                            if (WerewolfEditor.makeWerewolf(newInfection, false)) {
                                player.sendMessage(ChatColor.GREEN + args[0] + " now has a werewolf infection!");
                                newInfection.sendMessage(ChatColor.YELLOW + "You have been infected by " + player.getName() + "!");
                            }else {
                                player.sendMessage(ChatColor.RED + args[0] + " is already an incompatible class!");
                            }
                        }else {
                            player.sendMessage(ChatColor.RED + args[0] + " already is a werewolf! Please toggle his status first...");
                        }
                    }catch (Exception e) {
                        player.sendMessage(ChatColor.RED + "Invalid player name...");
                    }
                }else {
                    player.sendMessage(ChatColor.RED + "Please specify a name!");
                }
            }
        }else if (label.equalsIgnoreCase("infectwolfself")) {
            if (permissionHandler != null && permissionHandler.permission(player, "thewerewolf.infectself")
                    || permissionHandler == null && player.isOp()) {
                if (!WerewolfEditor.isWerewolf(player, false)) {
                    if (WerewolfEditor.makeWerewolf(player, false)) {
                        player.sendMessage(ChatColor.GREEN + "You now have a werewolf infection!");
                    }else {
                        player.sendMessage(ChatColor.RED + "You are already an incompatible class!");
                    }
                }else {
                    player.sendMessage(ChatColor.RED + "You are already a werewolf! Please toggle your status first...");
                }
            }
        }
        return false;
    }
}
