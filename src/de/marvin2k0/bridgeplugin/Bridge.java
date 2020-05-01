package de.marvin2k0.bridgeplugin;

import de.marvin2k0.bridgeplugin.game.Game;
import de.marvin2k0.bridgeplugin.game.GamePlayer;
import de.marvin2k0.bridgeplugin.listener.GameListener;
import de.marvin2k0.bridgeplugin.listener.PlaceBedListener;
import de.marvin2k0.bridgeplugin.listener.SignListener;
import de.marvin2k0.bridgeplugin.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Bridge extends JavaPlugin implements CommandExecutor
{
    public static HashMap<Player, GamePlayer> gamePlayers = new HashMap<>();
    public static HashMap<Player, String> placeBed = new HashMap<>();
    public static HashMap<Player, String> placeSpawner = new HashMap<>();
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
        getServer().getPluginManager().registerEvents(new Game(), this);

        Map<String, Object> section = Game.getConfig().getConfigurationSection("").getValues(false);

        for (Map.Entry<String, Object> entry : section.entrySet())
        {
            if (!Game.games.contains(entry.getKey()))
            {
                Game.games.add(entry.getKey());
                List<String> players = Game.getConfig().getStringList(entry.getKey() + ".players");
                List<String> players2 = new ArrayList<>();

                for (String p : players)
                {
                    if (!Bukkit.getOfflinePlayer(p).hasPlayedBefore())
                        continue;

                    if (!players2.contains(p))
                        players2.add(p);
                }

                Game.getConfig().set(entry.getKey() + ".players", players2);
                Game.saveConfig();
            }
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
                List<String> players = Game.getConfig().getStringList(str + ".players");
                List<String> players2 = new ArrayList<>();

                for (String p : players)
                {
                    if (!Bukkit.getOfflinePlayer(p).hasPlayedBefore())
                        continue;

                    if (!players2.contains(p))
                        players2.add(p);
                }

                Game.getConfig().set(str + ".players", players2);
                Game.saveConfig();

                Game game = Game.getGameFromName(str);
                game.reset();

                for (GamePlayer gp : game.getPlayers())
                {
                    if (gp.getPlayer() == null)
                        continue;

                    System.out.println(gp.getPlayer().getName());
                    Game.leave(game.getName(), gp.getPlayer());

                }

                Map<String, Object> section = Game.getConfig().getConfigurationSection(game.getName() + ".spawns").getValues(false);

                for (Map.Entry<String, Object> entry : section.entrySet())
                {
                    Game.getConfig().set(game.getName() + ".spawns." + entry.getKey() + ".members", null);
                    Game.saveConfig();
                }
            }

            Game.saveConfig();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void freeze(Player player)
    {
        if (freeze.contains(player))
        {
            Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable()
            {
                int timer = 5;

                @Override
                public void run()
                {
                    if (timer == 0)
                    {
                        player.sendMessage(TextUtils.get("fight"));
                        timer = -1;
                        return;
                    }
                    else if (timer == -1)
                        return;

                    player.sendMessage(TextUtils.get("countdown_2").replace("%time%", timer + ""));
                    timer--;
                }
            }, 0, 20);

            Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable()
            {
                @Override
                public void run()
                {
                    freeze.remove(player);
                    player.setFallDistance(0);
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

        if (args[0].equalsIgnoreCase("leave"))
        {
            if (!gamePlayers.containsKey(player))
            {
                player.sendMessage(TextUtils.get("notingame"));

                return true;
            }

            Game.leave(gamePlayers.get(player).getGame().getName(), player);
            gamePlayers.remove(player);

            return true;
        }

        if (!player.hasPermission("bw.admin"))
        {
            player.sendMessage(TextUtils.get("noperm"));

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
            System.out.println(withBed);

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

                player.sendMessage(TextUtils.get("bedplace").replace("%team%", args[2]));
                Game.setBed(args[1], args[2], player);

                return true;
            }
            else
            {
                player.sendMessage("§cMax. spawns reached for this game!");

                return true;
            }
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
                String path = item == null ? "" : item.getType().toString();
                System.out.println(path + " in " + i + " " + path);

                config.set("kit." + i + ".type", path);
                config.set("kit." + i + ".amount", item == null ? 0 : item.getAmount());

                if (item != null && !(item.getEnchantments().isEmpty()))
                {
                    for (Map.Entry<Enchantment, Integer> ench : item.getEnchantments().entrySet())
                    {
                        config.set("kit." + i + "." + path + ".ench." + ench.getKey().getName(), ench.getValue());
                    }
                }

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

        else if (args[0].equalsIgnoreCase("spawner"))
        {
            if (!(args.length >= 2))
            {
                player.sendMessage("§cUsage: /bw spawner <game> <iron|gold|diamond>");

                return true;
            }

            if (!Game.exists(args[1]))
            {
                player.sendMessage(TextUtils.get("nogame").replace("%game%", args[1]));

                return true;
            }

            ItemStack item = new ItemStack(Material.BLAZE_ROD);
            ItemMeta meta = item.getItemMeta();

            if (args[2].equalsIgnoreCase("iron"))
            {
                meta.setDisplayName("§fClick for Iron");
            }
            else if (args[2].equalsIgnoreCase("gold"))
            {
                meta.setDisplayName("§6Click for Gold");
            }
            else if (args[2].equalsIgnoreCase("diamond") || args[2].equalsIgnoreCase("dia"))
            {
                meta.setDisplayName("§bClick for Diamond");
            }
            else
            {
                player.sendMessage("§cUsage: /bw spawner <game> <iron|gold|diamond>");

                return true;
            }

            item.setItemMeta(meta);

            player.getInventory().addItem(item);
            player.sendMessage(TextUtils.get("spawnerstick"));
            placeSpawner.put(player, args[1]);

            return true;
        }

        else if (args[0].equalsIgnoreCase("chest"))
        {
            if (args.length != 2)
            {
                player.sendMessage("§cUsage: /bw chest <game>");
                return true;
            }

            if (!Game.exists(args[1]))
            {
                player.sendMessage(TextUtils.get("nogame"));
                return true;
            }

            String game = args[1];
            Location loc = player.getLocation();
            Game.getConfig().set(game + ".chest.world", loc.getWorld().getName());
            Game.getConfig().set(game + ".chest.x", loc.getX());
            Game.getConfig().set(game + ".chest.y", loc.getY());
            Game.getConfig().set(game + ".chest.z", loc.getZ());

            Game.saveConfig();
            loc.getWorld().getBlockAt(loc).setType(Material.CHEST);
            player.sendMessage("§aChest has been set!");
        }

        player.sendMessage("§cInvalid command!");

        return true;
    }
}
