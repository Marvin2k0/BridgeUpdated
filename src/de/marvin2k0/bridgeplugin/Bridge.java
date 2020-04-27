package de.marvin2k0.bridgeplugin;

import de.marvin2k0.bridgeplugin.game.Game;
import de.marvin2k0.bridgeplugin.game.GamePlayer;
import de.marvin2k0.bridgeplugin.listener.GameListener;
import de.marvin2k0.bridgeplugin.listener.PlaceBedListener;
import de.marvin2k0.bridgeplugin.listener.SignListener;
import de.marvin2k0.bridgeplugin.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Bridge extends JavaPlugin implements CommandExecutor
{
    public static HashMap<Player, GamePlayer> gamePlayers = new HashMap<>();
    public static HashMap<Player, String> placeBed = new HashMap<>();
    public static ArrayList<Player> freeze = new ArrayList<>();
    public static Bridge plugin;

    File file = new File(getDataFolder().getPath() + "/kits.yml");
    public FileConfiguration config = YamlConfiguration.loadConfiguration(file);

    @Override
    public void onEnable()
    {
        super.onEnable();

        plugin = this;

        getCommand("setlobby").setExecutor(new LobbyCommand());
        getCommand("lobby").setExecutor(new LobbyCommand());

        getServer().getPluginManager().registerEvents(new PlaceBedListener(), this);
        getServer().getPluginManager().registerEvents(new SignListener(), this);
        getServer().getPluginManager().registerEvents(new GameListener(), this);

        Map<String, Object> section = Game.getConfig().getConfigurationSection("").getValues(false);

        for (Map.Entry<String, Object> entry : section.entrySet())
        {
            if (!Game.games.contains(entry.getKey()))
                Game.games.add(entry.getKey());
        }

        TextUtils.setUpConfig(this);
    }

    @Override
    public void onDisable()
    {
        try
        {
            for (String str : Game.games)
            {
                Game game = Game.getGameFromName(str);

                if (game.getPlayers() == null)
                    continue;

                for (GamePlayer gp : game.getPlayers())
                {
                    Bukkit.getConsoleSender().sendMessage(game.getName() + ": " + game.getPlayers());
                    Game.leave(game.getName(), gp.getPlayer());
                }
            }

            for (String g : Game.games)
            {
                Map<String, Object> section = Game.getConfig().getConfigurationSection(g + ".spawns").getValues(false);

                for (Map.Entry<String, Object> entry : section.entrySet())
                {
                    Game.getConfig().set(g + ".spawns." + entry.getKey() + ".members", new ArrayList<String>());
                }
            }

            Game.saveConfig();
        }
        catch (Exception e) {}
    }

    public void freeze(Player player)
    {
        if (freeze.contains(player))
        {
            player.sendMessage(TextUtils.get("fight"));

            Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable()
            {
                @Override
                public void run()
                {
                    freeze.remove(player);
                }
            }, 100L);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if (!(sender instanceof Player))
        {
            sender.sendMessage("§cOnly for players");

            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0)
        {
            player.sendMessage("§cInvalid command!");

            return true;
        }

        if (args[0].equalsIgnoreCase("create"))
        {
            if (args.length != 4)
            {
                player.sendMessage("§cUsage: /bw create <name> <mode> <bed?>");

                return true;
            }

            String name = args[1];
            Game.Mode mode = Game.Mode.getFromString(args[2]);
            boolean withBed = Boolean.valueOf(args[3]);

            if (mode == null)
            {
                player.sendMessage(TextUtils.get("modes").replace("%input%", args[2]));

                return true;
            }

            Game game = new Game(name, mode, withBed);

            Game.addGame(game);
            player.sendMessage(TextUtils.get("gamecreated"));

            return true;
        }

        else if (args[0].equalsIgnoreCase("lobby"))
        {
            if (args.length != 2)
            {
                player.sendMessage("§cUsage: /bw lobby <game>");

                return true;
            }

            if (!Game.exists(args[1]))
            {
                player.sendMessage(TextUtils.get("nogame").replace("%game%", args[1]));

                return true;
            }

            Game.setLobby(args[1], player.getLocation());
            player.sendMessage(TextUtils.get("lobbyset"));

            return true;
        }

        else if (args[0].equalsIgnoreCase("spawn"))
        {
            if (args.length != 3)
            {
                player.sendMessage("§cUsage: /bw spawn <game> <spawn>");

                return true;
            }

            if (!Game.exists(args[1]))
            {
                player.sendMessage(TextUtils.get("nogame").replace("%game%", args[1]));

                return true;
            }

            Game.Mode mode = Game.Mode.getFromString(Game.getConfig().getString(args[1] + ".mode"));
            int left = Game.getConfig().getInt(args[1] + ".spawn") - 1;

            if (left < 0 && !Game.getConfig().isSet(args[1] + ".spawns." + args[2]))
            {
                player.sendMessage(TextUtils.get("nospawnsleft"));

                return true;
            }

            if (!Game.getConfig().isSet(args[1] + ".spawns." + args[2]))
            {
                Game.getConfig().set(args[1] + ".spawn", left);
            }

            Game.saveConfig();

            if (Game.getConfig().getInt(args[1] + ".spawn") <= mode.getTeams())
            {
                Game.setSpawn(args[1], player.getLocation(), args[2]);
                player.sendMessage(TextUtils.get("spawnset").replace("%spawn%", args[2]).replace("%left%", Game.getConfig().getInt(args[1] + ".spawn") + ""));

                if (Game.getConfig().getBoolean(args[1] + ".bed"))
                {
                    player.sendMessage(TextUtils.get("bedplace").replace("%team%", args[2]));
                    Game.setBed(args[1], args[2], player);
                }

                return true;
            }
            else
            {
                player.sendMessage("§cMax. spawns reached for this game!");

                return true;
            }
        }

        else if (args[0].equalsIgnoreCase("leave"))
        {
            if (!gamePlayers.containsKey(player))
            {
                player.sendMessage(TextUtils.get("notingame"));

                return true;
            }

            Game.leave(gamePlayers.get(player).getGame().getName(), player);
            gamePlayers.remove(player);

            player.chat("/" + TextUtils.get("leavecommand", false));

            return true;
        }

        else if (args[0].equalsIgnoreCase("start"))
        {
            if (!gamePlayers.containsKey(player))
            {
                player.sendMessage(TextUtils.get("notingame"));

                return true;
            }

            GamePlayer gp = gamePlayers.get(player);
            gp.getGame().start();

            return true;
        }

        else if (args[0].equalsIgnoreCase("kit"))
        {


            int i = 0;

            for (ItemStack item : player.getInventory().getContents())
            {
                if (item != null)
                    config.set("kit." + i, item.getType().toString());
                i++;
            }

            try
            {
                config.save(file);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            player.sendMessage("§aKit saved!");

            return true;
        }

        player.sendMessage("§cInvalid command!");

        return true;
    }
}
