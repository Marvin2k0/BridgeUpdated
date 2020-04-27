package de.marvin2k0.bridgeplugin.game;

import de.marvin2k0.bridgeplugin.Bridge;
import de.marvin2k0.bridgeplugin.utils.TextUtils;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Game
{
    public static ArrayList<String> games = new ArrayList<>();

    private static File file = new File(Bridge.plugin.getDataFolder().getPath() + "/games.yml");
    private static FileConfiguration config = YamlConfiguration.loadConfiguration(file);

    private ArrayList<GamePlayer> players;
    private ArrayList<String> teams;

    private String name;
    private Mode mode;
    private boolean withBed;

    public Game(String name, Mode mode, boolean bed)
    {
        this.name = name;
        this.mode = mode;
        this.withBed = bed;

        players = new ArrayList<>();
        teams = new ArrayList<>();
    }

    public Game(String name)
    {
        this.name = name;
    }

    public void start()
    {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(Bridge.plugin, new Runnable()
        {
            int timer = 20;

            @Override
            public void run()
            {
                if (timer == 0)
                {
                    List<String> p = getConfig().getStringList(name + ".players");

                    for (String str : p)
                    {
                        GamePlayer gp = Bridge.gamePlayers.get(Bukkit.getPlayer(str));
                        gp.setInLobby(false);

                        if (gp.getTeam() == null)
                        {
                            System.out.println("not in a team");
                            Map<String, Object> section2 = Game.getConfig().getConfigurationSection(getName() + ".spawns").getValues(false);

                            for (Map.Entry<String, Object> entry2 : section2.entrySet())
                            {
                                List<String> members = getConfig().getStringList(getName() + ".spawns." + entry2.getKey() + ".members");

                                if (members.size() < getMode().getPlayersPerTeam() + 1)
                                {
                                    String color = "§7";

                                    switch (entry2.getKey())
                                    {
                                        case "1":
                                            color = "§0";
                                            break;
                                        case "2":
                                            color = "§c";
                                            break;
                                        case "3":
                                            color = "§e";
                                            break;
                                        case "4":
                                            color = "§9";
                                            break;
                                    }

                                    gp.setTeam(entry2.getKey());
                                    gp.getPlayer().setDisplayName(color + gp.getPlayer().getName());
                                    gp.getPlayer().setPlayerListName(color + gp.getPlayer().getName());
                                    gp.getPlayer().sendMessage(TextUtils.get("joinedteam").replace("%team%", entry2.getKey()));
                                    gp.getPlayer().closeInventory();
                                    break;
                                }
                            }
                        }
                    }

                    Bukkit.getScheduler().cancelAllTasks();

                    tpToSpawn();
                }

                if (timer <= 5)
                {
                    sendMessage(TextUtils.get("countdown").replace("%seconds%", timer + ""));
                }
                else
                {
                    if (timer % 5 == 0)
                        sendMessage(TextUtils.get("countdown").replace("%seconds%", timer + ""));
                }

                timer = timer - 1;
            }
        }, 0, 20);
    }

    public void sendMessage(String msg)
    {
        for (GamePlayer gp : getPlayers())
        {
            gp.getPlayer().sendMessage(msg);
        }
    }

    public Location getLobby()
    {
        Location location = null;

        try
        {
            World world = Bukkit.getWorld(config.getString(name + ".lobby." + ".world"));

            double x = config.getDouble(name + ".lobby." + ".x");
            double y = config.getDouble(name + ".lobby." + ".y");
            double z = config.getDouble(name + ".lobby." + ".z");

            double yaw = config.getDouble(name + ".lobby." + ".yaw");
            double pitch = config.getDouble(name + ".lobby." + ".pitch");

            location = new Location(world, x, y, z, (float) yaw, (float) pitch);
        }
        catch (Exception e)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_RED + "No lobby set!");
        }

        return location;
    }

    public ArrayList<GamePlayer> getPlayers()
    {
        players = new ArrayList<>();

        List<String> list = getConfig().getStringList(getName() + ".players");

        for (String str : list)
        {
            Player player = Bukkit.getPlayer(str);

            if (!players.contains(Bridge.gamePlayers.get(player)))
            {
                if (Bridge.gamePlayers.containsKey(player))
                {
                    players.add(Bridge.gamePlayers.get(Bukkit.getPlayer(str)));
                }
                else
                {
                    players.add(new GamePlayer(player, this));
                }
            }
        }

        return players;
    }

    public String getName()
    {
        return name;
    }

    public Mode getMode()
    {
        return mode;
    }

    public void setMode(Mode mode)
    {
        this.mode = mode;
    }

    public boolean isWithBed()
    {
        this.withBed = getConfig().getBoolean(getName() + ".bed");

        return withBed;
    }

    public static void addGame(Game game)
    {
        if (!games.contains(game.getName()))
        {
            games.add(game.getName());

            Game.getConfig().set(game.getName() + ".spawn", game.getMode().getTeams());
            Game.getConfig().set(game.getName() + ".mode", game.getMode().getName());
            Game.getConfig().set(game.getName() + ".bed", game.isWithBed());
            Game.getConfig().set(game.getName() + ".players", new ArrayList<String>());
            Game.saveConfig();
        }
    }

    public static void join(String game, Player player)
    {
        /* When already in the game */
        if (Bridge.gamePlayers.containsKey(player))
            return;

        GamePlayer gp = new GamePlayer(player, Game.getGameFromName(game));
        gp.setInLobby(true);

        Bridge.gamePlayers.put(player, gp);

        List<String> players = getConfig().getStringList(game + ".players");
        players.add(player.getName());
        getConfig().set(game + ".players", players);

        saveConfig();

        Game gameObj = Game.getGameFromName(game);

        player.teleport(gameObj.getLobby());

        gameObj.sendMessage(TextUtils.get("gamejoin").replace("%player%", player.getName()));

        player.getInventory().clear();
        giveLobbyItems(player);

        if (gameObj.getMode().getPlayers() == gameObj.getPlayers().size())
            gameObj.start();
    }

    public static void leave(String game, Player player)
    {
        Bridge.gamePlayers.remove(player);

        List<String> players = getConfig().getStringList(game + ".players");
        players.remove(player.getName());
        getConfig().set(game + ".players", players);

        Map<String, Object> section = Game.getConfig().getConfigurationSection(game + ".spawns").getValues(false);

        for (Map.Entry<String, Object> entry : section.entrySet())
        {
            List<String> teamMembers = getConfig().getStringList(game + ".spawns." + entry.getKey() + ".members");

            if (teamMembers.contains(player.getName()))
            {
                teamMembers.remove(player.getName());
                getConfig().set(game + ".spawns." + entry.getKey() + ".members", teamMembers);
                saveConfig();
                break;
            }
        }

        player.setDisplayName(player.getName());
        player.setPlayerListName(player.getName());
        player.getInventory().clear();

        saveConfig();
    }

    private static void giveLobbyItems(Player player)
    {
        ItemStack item = new ItemStack(Material.WOOL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(TextUtils.get("chooseteam"));
        item.setItemMeta(meta);

        player.getInventory().setItem(4, item);
    }

    public static void setBed(String game, String team, Player player)
    {
        if (!Bridge.placeBed.containsKey(player))
            Bridge.placeBed.put(player, game + ":" + team);
    }

    public static Game getGameFromName(String name)
    {
        if (!exists(name))
            return null;

        Game game = new Game(name);
        game.setMode(Mode.getFromString(Game.getConfig().getString(name + ".mode")));

        return game;
    }

    public static void setSpawn(String game, Location location, String name)
    {
        String world = location.getWorld().getName();

        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        float yaw = location.getYaw();
        float pitch = location.getPitch();

        config.set(game + ".spawns." + name + ".world", world);
        config.set(game + ".spawns." + name + ".x", x);
        config.set(game + ".spawns." + name + ".y", y);
        config.set(game + ".spawns." + name + ".z", z);
        config.set(game + ".spawns." + name + ".yaw", yaw);
        config.set(game + ".spawns." + name + ".pitch", pitch);

        saveConfig();
    }

    public static void setLobby(String game, Location location)
    {
        String world = location.getWorld().getName();

        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        float yaw = location.getYaw();
        float pitch = location.getPitch();

        config.set(game + ".lobby.world", world);
        config.set(game + ".lobby.x", x);
        config.set(game + ".lobby.y", y);
        config.set(game + ".lobby.z", z);
        config.set(game + ".lobby.yaw", yaw);
        config.set(game + ".lobby.pitch", pitch);

        saveConfig();
    }

    public void tpToSpawn()
    {
        Map<String, Object> section = Game.getConfig().getConfigurationSection(getName() + ".spawns").getValues(false);

        for (Map.Entry<String, Object> entry : section.entrySet())
        {
            for (String str : getConfig().getStringList(getName() + ".spawns." + entry.getKey() + ".members"))
            {
                Player player = Bukkit.getPlayer(str);

                World world = Bukkit.getWorld(config.getString(name + ".lobby." + ".world"));

                double x = config.getDouble(name + ".spawns." + entry.getKey() + ".x");
                double y = config.getDouble(name + ".spawns." + entry.getKey() + ".y");
                double z = config.getDouble(name + ".spawns." + entry.getKey() + ".z");

                double yaw = config.getDouble(name + ".spawns." + entry.getKey() + ".yaw");
                double pitch = config.getDouble(name + ".spawns." + entry.getKey() + ".pitch");

                Location spawn = new Location(world, x, y, z, (float) yaw, (float) pitch);

                player.teleport(spawn);
                player.getInventory().clear();
                player.setHealth(player.getMaxHealth());
                player.setFoodLevel(20);
                getItems(player);

                Bridge.freeze.add(player);
                Bridge.plugin.freeze(player);
            }
        }
    }

    public void getItems(Player player)
    {
        Map<String, Object> section = Bridge.plugin.config.getConfigurationSection("kit").getValues(false);
        Inventory inv = Bukkit.createInventory(player, 36);

        for (Map.Entry<String, Object> entry : section.entrySet())
        {
            inv.setItem(Integer.valueOf(entry.getKey()), new ItemStack(Material.getMaterial(entry.getValue().toString())));
        }

        player.getInventory().setContents(inv.getContents());
    }

    public static boolean exists(String name)
    {
        return config.isSet(name);
    }

    public static FileConfiguration getConfig()
    {
        return config;
    }

    public static void saveConfig()
    {
        try
        {
            config.save(file);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public enum Mode
    {
        ONE_ON_ONE("1v1", 2, 2, 1),
        TWO_ON_TWO("2v2", 4, 2, 2),
        FOUR_x_ONE("4x1", 4, 4, 1);

        String name;
        int players;
        int teams;
        int playersPerTeam;

        Mode(String name, int players, int teams, int playersPerTeam)
        {
            this.name = name;
            this.players = players;
            this.teams = teams;
        }

        public String getName()
        {
            return name;
        }

        public int getPlayers()
        {
            return players;
        }

        public int getTeams()
        {
            return teams;
        }

        public int getPlayersPerTeam()
        {
            return playersPerTeam;
        }

        public static Mode getFromString(String str)
        {
            if (str.equalsIgnoreCase("1v1"))
                return Mode.ONE_ON_ONE;
            if (str.equalsIgnoreCase("2v2"))
                return Mode.TWO_ON_TWO;
            if (str.equalsIgnoreCase("4x1"))
                return Mode.FOUR_x_ONE;

            return null;
        }
    }
}
