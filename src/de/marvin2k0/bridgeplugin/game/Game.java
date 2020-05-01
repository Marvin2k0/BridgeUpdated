package de.marvin2k0.bridgeplugin.game;

import com.avaje.ebeaninternal.server.text.csv.CsvUtilReader;
import de.marvin2k0.bridgeplugin.Bridge;
import de.marvin2k0.bridgeplugin.listener.GameListener;
import de.marvin2k0.bridgeplugin.utils.TextUtils;
import de.marvin2k0.bridgeplugin.utils.Title;
import org.bukkit.*;
import org.bukkit.block.Chest;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.*;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Game implements Listener
{
    public static ArrayList<String> games = new ArrayList<>();

    private static File file = new File(Bridge.plugin.getDataFolder().getPath() + "/games.yml");
    private static FileConfiguration config = YamlConfiguration.loadConfiguration(file);

    private ArrayList<GamePlayer> players;
    private ArrayList<String> teams;
    public ArrayList<Location> bloecke = new ArrayList<>();
    private Scoreboard board;

    private String name;
    private Mode mode;
    private boolean withBed;
    public boolean hasStarted;

    public Game()
    {
    }

    public Game(String name, Mode mode, boolean bed)
    {
        this.name = name;
        this.mode = mode;
        this.withBed = bed;
        this.hasStarted = false;

        players = new ArrayList<>();
        teams = new ArrayList<>();
    }

    public Game(String name)
    {
        this.name = name;

        players = new ArrayList<>();
        teams = new ArrayList<>();
    }

    public void setHasStarted(boolean flag)
    {
        getConfig().set(getName() + ".started", flag);
        saveConfig();
    }

    public void saveBlock(Location loc)
    {
        String random = System.currentTimeMillis() + "";

        getConfig().set(getName() + ".blocks." + random + ".world", loc.getWorld().getName());
        getConfig().set(getName() + ".blocks." + random + ".x", loc.getX());
        getConfig().set(getName() + ".blocks." + random + ".y", loc.getY());
        getConfig().set(getName() + ".blocks." + random + ".z", loc.getZ());

        saveConfig();
    }

    public ArrayList<Location> getBlock()
    {
        if (getConfig().getConfigurationSection(getName() + ".blocks") == null) return new ArrayList<Location>();

        Map<String, Object> sections = getConfig().getConfigurationSection(getName() + ".blocks").getValues(false);

        ArrayList<Location> locs = new ArrayList<>();

        for (Map.Entry entry : sections.entrySet())
        {
            World world = Bukkit.getWorld(Game.getConfig().getString(getName() + ".blocks." + entry.getKey() + ".world"));

            double x = Game.getConfig().getDouble(getName() + ".blocks." + entry.getKey() + ".x");
            double y = Game.getConfig().getDouble(getName() + ".blocks." + entry.getKey() + ".y");
            double z = Game.getConfig().getDouble(getName() + ".blocks." + entry.getKey() + ".z");

            locs.add(new Location(world, x, y, z));
        }

        return locs;
    }

    public boolean hasStarted()
    {
        return getConfig().getBoolean(getName() + ".started");
    }

    public void checkWin(String from)
    {
        Map<String, Object> section = getConfig().getConfigurationSection(getName() + ".spawns").getValues(false);
        List<String> activeTeams = new ArrayList<>();

        for (Map.Entry<String, Object> entry : section.entrySet())
        {
            List<String> members = getConfig().getStringList(getName() + ".spawns." + entry.getKey() + ".members");

            if (members.size() >= 1)
            {
                activeTeams.add(entry.getKey());
            }
        }

        if (activeTeams.size() == 1)
        {
            resetBlocks();
            reset();
            win(activeTeams.get(0));
            return;
        }
        else if (activeTeams.size() == 0)
        {
            win("win weil kein team mehr");
            resetBlocks();
            reset();
        }
    }

    public void reset()
    {
        setHasStarted(false);
        GameListener.des.clear();
        ArrayList<Location> locs = new ArrayList<>();

        if (getConfig().isSet(getName() + ".chest"))
        {
            System.out.println("reset chest");
            double x = getConfig().getDouble(getName() + ".chest.x");
            double y = getConfig().getDouble(getName() + ".chest.y");
            double z = getConfig().getDouble(getName() + ".chest.z");
            String world = getConfig().getString(getName() + ".chest.world");

            System.out.println(Bukkit.getWorld(world).getBlockAt(new Location(Bukkit.getWorld(world), x, y - 1, z)).getType());

            if (Bukkit.getWorld(world).getBlockAt(new Location(Bukkit.getWorld(world), x, y, z)).getType() == Material.CHEST)
            {
                System.out.println("is chest");
                Chest chest = (Chest) Bukkit.getWorld(world).getBlockAt(new Location(Bukkit.getWorld(world), x, y, z)).getState();
                chest.getInventory().setContents(new ItemStack[]{});
            }
        }

        try
        {

            if (getConfig().isSet(getName() + ".spawns"))
            {
                Map<String, Object> section = Game.getConfig().getConfigurationSection(getName() + ".spawns").getValues(false);

                for (Map.Entry<String, Object> entry : section.entrySet())
                {
                    Game.getConfig().set(getName() + ".spawns." + entry.getKey() + ".points", 0);
                    Game.saveConfig();
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        Game.saveConfig();
    }

    public void win(String team)
    {
        reset();
        sendMessage(TextUtils.get("win").replace("%team%", team));

        for (GamePlayer gp : getPlayers())
        {
            if (Bridge.freeze.contains(gp.getPlayer()))
            {
                Bridge.freeze.remove(gp.getPlayer());
            }
            Title.send(gp.getPlayer(), TextUtils.get("win_title", false).replace("%team%", team), TextUtils.get("win_sub", false), 1, 5, 1);
            leave(getName(), gp.getPlayer());
        }
    }

    static Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
    static Objective objective = scoreboard.registerNewObjective("aaa", "bbb");

    static Team team1 = scoreboard.registerNewTeam("1");
    static Team team2 = scoreboard.registerNewTeam("2");
    static Team team3 = scoreboard.registerNewTeam("3");
    static Team team4 = scoreboard.registerNewTeam("4");

    public void setScoreboards()
    {
        String[] colors = {"§0", "§c", "§e", "§9"};
        boolean bed = getConfig().getBoolean(getName() + ".bed");

        objective.setDisplayName(bed ? TextUtils.get("scoreboard_title") : TextUtils.get("prefix"));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        team1.setPrefix("§0");
        team2.setPrefix("§c");
        team3.setPrefix("§e");
        team4.setPrefix("§9");

        Map<String, Object> section = getConfig().getConfigurationSection(getName() + ".spawns").getValues(false);
        int i = 0;

        for (Map.Entry<String, Object> entry : section.entrySet())
        {
            int points = getConfig().getInt(getName() + ".spawns." + entry.getKey() + ".points");

            objective.getScore(colors[i] + "Team " + entry.getKey()).setScore(points);
            i++;
        }

        List<String> playerList = getConfig().getStringList(getName() + ".players");

        for (String str : playerList)
        {
            Player player = Bukkit.getPlayer(str);
            GamePlayer gp = Bridge.gamePlayers.get(player);
            String team = gp.getTeam();

            if (team.equals("1"))
                team1.addPlayer(player);
            else if (team.equals("2"))
                team2.addPlayer(player);
            else if (team.equals("3"))
                team3.addPlayer(player);
            else if (team.equals("4"))
                team4.addPlayer(player);

            player.setScoreboard(scoreboard);
        }
    }

    public void start()
    {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(Bridge.plugin, new Runnable()
        {
            int timer = 5;

            @Override
            public void run()
            {
                if (timer == 0)
                {
                    setHasStarted(true);

                    List<String> p = getConfig().getStringList(name + ".players");

                    for (String str : p)
                    {
                        if (Bukkit.getPlayer(str) == null)
                            continue;

                        GamePlayer gp = Bridge.gamePlayers.get(Bukkit.getPlayer(str));
                        gp.setInLobby(false);

                        if (gp.getTeam() == null)
                        {
                            Map<String, Object> section2 = Game.getConfig().getConfigurationSection(getName() + ".spawns").getValues(false);

                            for (Map.Entry<String, Object> entry2 : section2.entrySet())
                            {
                                List<String> members = getConfig().getStringList(getName() + ".spawns." + entry2.getKey() + ".members");

                                try
                                {
                                    getConfig().load(file);
                                }
                                catch (IOException e)
                                {
                                    e.printStackTrace();
                                }
                                catch (InvalidConfigurationException e)
                                {
                                    e.printStackTrace();
                                }

                                if (members.size() < (getMode().getPlayersPerTeam()))
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

                                    gp.setTeam(entry2.getKey(), color);
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

                    activateSpawners();
                    setScoreboards();
                    checkWin("start");
                    return;
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

    private void activateSpawners()
    {
        if (getConfig().isSet(getName() + ".spawner.iron"))
        {
            Bukkit.getScheduler().scheduleSyncRepeatingTask(Bridge.plugin, new Runnable()
            {
                @Override
                public void run()
                {
                    checkWin("iron spawner");
                    if (!hasStarted())
                        Bukkit.getScheduler().cancelAllTasks();

                    Map<String, Object> section = getConfig().getConfigurationSection(getName() + ".spawner.iron").getValues(false);

                    for (Map.Entry<String, Object> entry : section.entrySet())
                    {
                        String world = getConfig().getString(getName() + ".spawner.iron." + entry.getKey() + ".world");
                        double x = getConfig().getDouble(getName() + ".spawner.iron." + entry.getKey() + ".x");
                        double y = getConfig().getDouble(getName() + ".spawner.iron." + entry.getKey() + ".y");
                        double z = getConfig().getDouble(getName() + ".spawner.iron." + entry.getKey() + ".z");

                        Location loc = new Location(Bukkit.getWorld(world), x, y, z);

                        Bukkit.getWorld(world).dropItem(loc.add(0.5, 1, 0.5), new ItemStack(Material.IRON_INGOT)).setVelocity(new Vector(0, 0, 0));
                    }

                    checkWin("iron spawner 2");
                }
            }, Long.valueOf(TextUtils.get("irondur", false)) * 20, Long.valueOf(TextUtils.get("irondur", false)) * 20);
        }

        if (getConfig().isSet(getName() + ".spawner.gold"))
        {
            Bukkit.getScheduler().scheduleSyncRepeatingTask(Bridge.plugin, new Runnable()
            {
                @Override
                public void run()
                {
                    checkWin("gold spawner");

                    if (!hasStarted())
                        Bukkit.getScheduler().cancelAllTasks();

                    Map<String, Object> section = getConfig().getConfigurationSection(getName() + ".spawner.gold").getValues(false);

                    for (Map.Entry<String, Object> entry : section.entrySet())
                    {
                        String world = getConfig().getString(getName() + ".spawner.gold." + entry.getKey() + ".world");
                        double x = getConfig().getDouble(getName() + ".spawner.gold." + entry.getKey() + ".x");
                        double y = getConfig().getDouble(getName() + ".spawner.gold." + entry.getKey() + ".y");
                        double z = getConfig().getDouble(getName() + ".spawner.gold." + entry.getKey() + ".z");

                        Location loc = new Location(Bukkit.getWorld(world), x, y, z);

                        Bukkit.getWorld(world).dropItem(loc.add(0.5, 1, 0.5), new ItemStack(Material.GOLD_INGOT)).setVelocity(new Vector(0, 0, 0));
                    }
                }
            }, Long.valueOf(TextUtils.get("golddur", false)) * 20, Long.valueOf(TextUtils.get("golddur", false)) * 20);
        }

        if (getConfig().isSet(getName() + ".spawner.dia"))
        {
            Bukkit.getScheduler().scheduleSyncRepeatingTask(Bridge.plugin, new Runnable()
            {
                @Override
                public void run()
                {
                    checkWin("diamond spawner");
                    if (!hasStarted())
                        Bukkit.getScheduler().cancelAllTasks();

                    Map<String, Object> section = getConfig().getConfigurationSection(getName() + ".spawner.dia").getValues(false);

                    for (Map.Entry<String, Object> entry : section.entrySet())
                    {
                        String world = getConfig().getString(getName() + ".spawner.dia." + entry.getKey() + ".world");
                        double x = getConfig().getDouble(getName() + ".spawner.dia." + entry.getKey() + ".x");
                        double y = getConfig().getDouble(getName() + ".spawner.dia." + entry.getKey() + ".y");
                        double z = getConfig().getDouble(getName() + ".spawner.dia." + entry.getKey() + ".z");

                        Location loc = new Location(Bukkit.getWorld(world), x, y, z);

                        Bukkit.getWorld(world).dropItem(loc.add(0.5, 1, 0.5), new ItemStack(Material.DIAMOND)).setVelocity(new Vector(0, 0, 0));
                    }
                    checkWin("diamond spawner");
                }
            }, Long.valueOf(TextUtils.get("diadur", false)) * 20, Long.valueOf(TextUtils.get("diadur", false)) * 20);
        }
    }

    public void resetBlocks()
    {

        for (Location loc : getBlock())
            loc.getBlock().setType(Material.AIR);

        getConfig().set(getName() + ".blocks", null);
        saveConfig();
        /*
        for (Location loc : bloecke)
        {
            loc.getBlock().setType(Material.AIR);
        }



        Iterator it = bloecke.iterator();

        while (it.hasNext())
        {
            System.out.println("blocks " + bloecke);
            it.remove();
        }*/
    }

    public void sendMessage(String msg)
    {
        List<String> players = getConfig().getStringList(getName() + ".players");

        for (String str : players)
        {
            if (Bukkit.getPlayer(str) == null)
                continue;

            Bukkit.getPlayer(str).sendMessage(msg);
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
        this.mode = getConfig().getString(getName() + ".mode") == null ? mode : Mode.getFromString(getConfig().getString(getName() + ".mode"));

        return mode;
    }

    public void setMode(Mode mode)
    {
        this.mode = mode;
    }

    public boolean isWithBed()
    {
        if (getConfig().isSet(getName() + ".bed"))
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
        GamePlayer gp = new GamePlayer(player, Game.getGameFromName(game));
        gp.setInLobby(true);

        Bridge.gamePlayers.put(player, gp);

        List<String> players = getConfig().getStringList(game + ".players");

        if (!players.contains(player.getName()))
        {
            players.add(player.getName());
        }

        getConfig().set(game + ".players", players);

        saveConfig();

        Game gameObj = Game.getGameFromName(game);

        player.teleport(gameObj.getLobby());

        gameObj.sendMessage(TextUtils.get("gamejoin").replace("%player%", player.getName()) + " (" + players.size() + "/" + gp.getGame().getMode().getPlayers() + ")");

        player.getInventory().clear();
        giveLobbyItems(player);
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);

        if (gameObj.getMode().getPlayers() == gameObj.getPlayers().size())
            gameObj.start();
    }

    public static void leave(String game, OfflinePlayer player)
    {
        Bridge.gamePlayers.remove(player);
        if (player == null)
        {
            return;
        }

        if (!player.hasPlayedBefore())
            return;

        List<String> players = getConfig().getStringList(game + ".players");
        List<String> players2 = players;

        for (String str : players)
        {
            if (!players.contains(str))
                players2.add(str);
        }

        players = players2;

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

        if (player.isOnline())
        {
            Player p = Bukkit.getPlayer(player.getName());

            players.remove(player.getName());
            p.setDisplayName(player.getName());
            p.setPlayerListName(player.getName());
            p.getInventory().clear();
            p.getInventory().setArmorContents(null);
            p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            p.setGameMode(GameMode.SURVIVAL);
            p.chat("/" + TextUtils.get("leavecommand", false));
        }

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
        config.set(game + ".spawns." + name + ".bed", true);

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

                if (getConfig().getBoolean(getName() + ".bed"))
                {
                    getItems(player);
                }

                Bridge.freeze.add(player);
                Bridge.plugin.freeze(player);
            }
        }
    }

    public void getItems(Player player)
    {
        if (!Bridge.plugin.config.isSet("kit"))
            return;

        Map<String, Object> section = Bridge.plugin.config.getConfigurationSection("kit").getValues(false);
        Inventory inv = player.getInventory();

        for (Map.Entry<String, Object> entry : section.entrySet())
        {
            int amount = Bridge.plugin.config.getInt("kit." + entry.getKey() + ".amount");
            String type = Bridge.plugin.config.getString("kit." + entry.getKey() + ".type");

            if (Material.getMaterial(type) != null)
            {
                ItemStack item = new ItemStack(Material.getMaterial(type));
                item.setAmount(amount);

                if (!Bridge.plugin.config.isSet("kit." + entry.getKey() + "." + type + ".ench"))
                {
                    inv.setItem(Integer.valueOf(entry.getKey()), item);
                    continue;
                }


                Map<String, Object> ench = Bridge.plugin.config.getConfigurationSection("kit." + entry.getKey() + "." + type + ".ench").getValues(false);

                for (Map.Entry<String, Object> eintrag : ench.entrySet())
                {
                    item.addUnsafeEnchantment(Enchantment.getByName(eintrag.getKey()), Integer.valueOf(eintrag.getValue().toString()));
                }

                inv.setItem(Integer.valueOf(entry.getKey()), item);
            }
        }

        player.updateInventory();
    }

    public static void getItemss(Player player)
    {
        if (!Bridge.plugin.config.isSet("kit"))
            return;

        Map<String, Object> section = Bridge.plugin.config.getConfigurationSection("kit").getValues(false);
        Inventory inv = player.getInventory();

        for (Map.Entry<String, Object> entry : section.entrySet())
        {
            int amount = Bridge.plugin.config.getInt("kit." + entry.getKey() + ".amount");
            String type = Bridge.plugin.config.getString("kit." + entry.getKey() + ".type");


            if (Material.getMaterial(type) != null)
            {
                ItemStack item = new ItemStack(Material.getMaterial(type));
                item.setAmount(amount);

                if (!Bridge.plugin.config.isSet("kit." + entry.getKey() + "." + type + ".ench"))
                {
                    inv.setItem(Integer.valueOf(entry.getKey()), item);
                    continue;
                }


                Map<String, Object> ench = Bridge.plugin.config.getConfigurationSection("kit." + entry.getKey() + "." + type + ".ench").getValues(false);

                for (Map.Entry<String, Object> eintrag : ench.entrySet())
                {
                    item.addUnsafeEnchantment(Enchantment.getByName(eintrag.getKey()), Integer.valueOf(eintrag.getValue().toString()));
                }

                inv.setItem(Integer.valueOf(entry.getKey()), item);
            }
        }

        player.updateInventory();
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
            this.playersPerTeam = playersPerTeam;
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
