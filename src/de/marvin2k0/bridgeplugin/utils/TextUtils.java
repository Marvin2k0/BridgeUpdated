package de.marvin2k0.bridgeplugin.utils;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

public class TextUtils
{
    private static Plugin plugin;

    public static String get(String path)
    {
        return get(path, true);
    }

    public static String get(String path, boolean prefix)
    {
        String msg = null;

        try
        {
            msg = !path.equals("prefix") && prefix ? getConfig().get("prefix") + getConfig().getString(path) : getConfig().getString(path);
        }
        catch (NullPointerException e)
        {
            e.printStackTrace();
        }

        return msg.contains("&") ? ChatColor.translateAlternateColorCodes('&', msg) : msg;
    }

    public static void setUpConfig(Plugin p)
    {
        plugin = p;

        getConfig().options().copyDefaults(true);
        getConfig().addDefault("prefix", "&c[&6BridgeWars&c] &f");
        getConfig().addDefault("gamejoin", "&7[&a+&7] %player%");
        getConfig().addDefault("gameleave", "&7[&c-&7] %player%");
        getConfig().addDefault("onlyplayers", "&cThis command is only for players!");
        getConfig().addDefault("noperm", "&cYou don't have permission to do that!");
        getConfig().addDefault("nonum", "&cPlease only enter numbers for the slots.");
        getConfig().addDefault("gamealreadyexists", "&cError: %game% already exists!");
        getConfig().addDefault("gamecreated", "&aGame created!");
        getConfig().addDefault("nogame", "&cError: %game% does not exist.");
        getConfig().addDefault("spawnset", "&aSpawn %spawn% set. There are %left% spawns left");
        getConfig().addDefault("nospawnsleft", "&cThere are no spawns left to be set");
        getConfig().addDefault("spawnremove", "&aSpawn %spawn% has been removed.");
        getConfig().addDefault("modes", "&cModes are: 1v1, 2v2, 4x1. You entered &4%input%");
        getConfig().addDefault("lobbyset", "&aLobby has been set!");
        getConfig().addDefault("leavecommand", "lobby");
        getConfig().addDefault("notingame", "&cYour are not in a game!");
        getConfig().addDefault("countdown", "&7Game starts in &c%seconds% seconds");
        getConfig().addDefault("bedplace", "&7Place the bed for team %team%.");
        getConfig().addDefault("blockplaced", "&7You placed the bed for team %team%");
        getConfig().addDefault("noteam", "&cThis team does not exist!");
        getConfig().addDefault("chooseteam", "&cChoose Team");
        getConfig().addDefault("joinedteam", "&aYou joined team %team%");
        getConfig().addDefault("deathmessage", "&c%player% &7has been killed by &c%killer%");
        getConfig().addDefault("deathmessage_2", "&c%player% &7died");
        getConfig().addDefault("teamfull", "&7This team is already full. Please choose another.");
        getConfig().addDefault("countdown_2", "&6You can move in %time% seconds!");
        getConfig().addDefault("fight", "&cFight!");
        getConfig().addDefault("win", "&7Team %team% has won the games");
        getConfig().addDefault("win_title", "&7Team &a%team%");
        getConfig().addDefault("win_sub", "&7won the game");
        getConfig().addDefault("score", "&7%player% &7scored for team %team%");
        getConfig().addDefault("teameliminated", "&7%team% was eliminated!");
        getConfig().addDefault("started", "&cGame has already started");
        getConfig().addDefault("spawnerstick", "&cRight-click &7with that stick to set the spawner");
        getConfig().addDefault("irondur", 1);
        getConfig().addDefault("golddur", 4);
        getConfig().addDefault("diadur", 30);
        getConfig().addDefault("scoreboard_title", "&6Points");
        getConfig().addDefault("inventoryfull", "&7Your inventory is full!");
        getConfig().addDefault("beddestroyed", "&7Team %team%'s bed was destroyed");
        getConfig().addDefault("ownbed",  "&cWhy would you wanna do that?");

        plugin.saveConfig();
    }

    private static FileConfiguration getConfig()
    {
        return plugin.getConfig();
    }
}
