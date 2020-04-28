package de.marvin2k0.bridgeplugin.listener;

import de.marvin2k0.bridgeplugin.Bridge;
import de.marvin2k0.bridgeplugin.game.Game;
import de.marvin2k0.bridgeplugin.game.GamePlayer;
import de.marvin2k0.bridgeplugin.utils.TextUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class GameListener implements Listener
{
    ArrayList<Location> blocks = new ArrayList<>();
    ArrayList<Player> die = new ArrayList<>();

    @EventHandler
    public void onRespawn(PlayerDeathEvent event)
    {

    }

    @EventHandler
    public void onBed(BlockBreakEvent event)
    {
        Player player = event.getPlayer();
        GamePlayer gp = null;

        if ((gp = Bridge.gamePlayers.get(player)) != null && gp.isInGame())
        {
            String game = gp.getGame().getName();

            if (gp.getGame().getMode() == Game.Mode.ONE_ON_ONE)
            {
                if (event.getBlock().getType() == Material.BED_BLOCK)
                {
                    event.setCancelled(true);

                    if (getTeamSpawn(gp).distance(event.getBlock().getLocation()) < 15)
                    {
                        player.sendMessage("§ceigenes bett");
                        return;
                    }

                    int points = Game.getConfig().getInt(gp.getGame().getName() + ".spawns." + gp.getTeam() + ".points") + 1;

                    if (points >= 5)
                    {
                        gp.getGame().win(gp.getTeam());
                        return;
                    }

                    Game.getConfig().set(gp.getGame().getName() + ".spawns." + gp.getTeam() + ".points", points);
                    Game.saveConfig();

                    gp.getGame().sendMessage(TextUtils.get("score").replace("%player%", player.getDisplayName()).replace("%team%", gp.getTeam()));
                    gp.getGame().tpToSpawn();
                }
            }
            else
            {
                Player player1 = null;
                if (event.getBlock().getType() == Material.BED_BLOCK)
                {
                    event.setCancelled(true);

                    if (getTeamSpawn(gp).distance(event.getBlock().getLocation()) < 15)
                    {
                        player.sendMessage("§ceigenes bett");
                        return;
                    }

                    Map<String, Object> section = Game.getConfig().getConfigurationSection(gp.getGame().getName() + ".spawns").getValues(false);

                    for (Map.Entry<String, Object> entry : section.entrySet())
                    {
                        for (String str : Game.getConfig().getStringList(gp.getGame().getName() + ".spawns." + entry.getKey() + ".members"))
                        {
                            Player p = Bukkit.getPlayer(str);
                            FileConfiguration config = Game.getConfig();

                            World world = Bukkit.getWorld(config.getString(gp.getGame().getName() + ".lobby." + ".world"));

                            double x = config.getDouble(gp.getGame().getName() + ".spawns." + entry.getKey() + ".bed" + ".x");
                            double y = config.getDouble(gp.getGame().getName() + ".spawns." + entry.getKey() + ".bed" + ".y");
                            double z = config.getDouble(gp.getGame().getName() + ".spawns." + entry.getKey() + ".bed" + ".z");

                            double yaw = config.getDouble(gp.getGame().getName() + ".spawns." + entry.getKey() + ".bed" + ".yaw");
                            double pitch = config.getDouble(gp.getGame().getName() + ".spawns." + entry.getKey() + ".bed" + ".pitch");

                            Location spawn = new Location(world, x, y, z, (float) yaw, (float) pitch);

                            if (event.getBlock().getLocation().distance(spawn) <= 3)
                            {
                                die.add(p);
                                player1 = p;
                            }
                        }
                    }
                }

                gp.getGame().sendMessage("Bed from team " + Bridge.gamePlayers.get(player1).getTeam() + " was destroyed");
            }
        }
    }

    public Location getTeamSpawn(GamePlayer gp)
    {
        String game = gp.getGame().getName();
        String team = gp.getTeam();

        World world = Bukkit.getWorld(Game.getConfig().getString(game + ".spawns." + team + ".world"));

        double x = Game.getConfig().getDouble(game + ".spawns." + team + ".x");
        double y = Game.getConfig().getDouble(game + ".spawns." + team + ".y");
        double z = Game.getConfig().getDouble(game + ".spawns." + team + ".z");

        double yaw = Game.getConfig().getDouble(game + ".spawns." + team + ".yaw");
        double pitch = Game.getConfig().getDouble(game + ".spawns." + team + ".pitch");

        return new Location(world, x, y, z, (float) yaw, (float) pitch);
    }

    @EventHandler
    public void onFreeze(PlayerMoveEvent event)
    {
        if (Bridge.freeze.contains(event.getPlayer()))
        {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onKill(PlayerDeathEvent event)
    {
        Player killer = event.getEntity().getKiller();
        Player player = event.getEntity().getPlayer();

        GamePlayer gp = null;

        if (die.contains(player))
        {
            if ((gp = Bridge.gamePlayers.get(player)) != null)
            {
                List<String> members = Game.getConfig().getStringList(gp.getGame().getName() + ".spawns." + gp.getTeam() + ".members");

                if (members.size() - 1 == 0)
                {
                    gp.getGame().sendMessage(TextUtils.get("teameliminated").replace("%team%", gp.getTeam()));
                }

                player.setGameMode(GameMode.SPECTATOR);
                player.setHealth(player.getMaxHealth());
                members.remove(player.getName());
                Game.getConfig().set(gp.getGame().getName() + ".spawns." + gp.getTeam() + ".members", members);
                Game.saveConfig();

                List<String> players = Game.getConfig().getStringList(gp.getGame().getName() + ".members");
                players.remove(player.getName());
                Game.getConfig().set(gp.getGame().getName() + ".spawns." + gp.getTeam() + ".members", players);
                Game.saveConfig();

                player.teleport(getTeamSpawn(gp));
            }
        }
        else
        {
            if ((gp = Bridge.gamePlayers.get(player)) != null)
            {
                player.setHealth(player.getMaxHealth());
                player.teleport(getTeamSpawn(gp));
            }
        }

        if ((gp = Bridge.gamePlayers.get(player)) != null)
        {
            if (killer == null)
            {
                event.setDeathMessage(TextUtils.get("deathmessage_2").replace("%player%", player.getDisplayName()));
                return;
            }
        }

        event.setDeathMessage(TextUtils.get("deathmessage").replace("%player%", player.getDisplayName()).replace("%killer%", killer.getDisplayName()));

    }

    @EventHandler
    public void onTeam(PlayerInteractEvent event)
    {
        Player player = event.getPlayer();
        GamePlayer gp = null;
        ItemStack item = event.getItem();

        if ((gp = Bridge.gamePlayers.get(player)) != null && item != null && item.hasItemMeta() && item.getItemMeta().getDisplayName().equals(TextUtils.get("chooseteam")))
        {
            Inventory inv = Bukkit.createInventory(null, 9, TextUtils.get("chooseteam", false));

            Map<String, Object> section = Game.getConfig().getConfigurationSection(gp.getGame().getName() + ".spawns").getValues(false);

            int i = 0;
            byte[] colors = {15, 14, 4, 11};

            for (Map.Entry<String, Object> entry : section.entrySet())
            {
                ItemStack teamItem = new ItemStack(Material.WOOL, 1, colors[i]);
                ItemMeta teamMeta = teamItem.getItemMeta();
                List<String> lore = Game.getConfig().getStringList(gp.getGame().getName() + ".spawns." + entry.getKey() + ".members");

                teamMeta.setLore(lore);
                teamMeta.setDisplayName("§7" + entry.getKey());
                teamItem.setItemMeta(teamMeta);

                inv.setItem(i, teamItem);
                i++;
            }

            player.openInventory(inv);
        }
    }

    @EventHandler
    public void onInv(InventoryClickEvent event)
    {
        Player player = (Player) event.getWhoClicked();
        GamePlayer gp = null;
        Inventory inv = event.getInventory();
        ItemStack item = event.getCurrentItem();

        if (inv != null && item != null && item.hasItemMeta() && inv.getName().equals(TextUtils.get("chooseteam", false)))
        {
            event.setCancelled(true);

            if ((gp = Bridge.gamePlayers.get(player)) != null)
            {
                String team = item.getItemMeta().getDisplayName().replace("§7", "");
                List<String> members = Game.getConfig().getStringList(gp.getGame().getName() + ".spawns." + team + ".members");

                if (members.size() == gp.getGame().getMode().getPlayersPerTeam() + 1)
                {
                    player.sendMessage(TextUtils.get("teamfull"));
                    return;
                }

                String color = "§7";

                switch (team)
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

                gp.setTeam(team);
                player.setDisplayName(color + player.getName());
                player.setPlayerListName(color + player.getName());
                player.sendMessage(TextUtils.get("joinedteam").replace("%team%", team));
                player.closeInventory();
            }
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event)
    {
        if (event.getEntity().getType() == EntityType.PRIMED_TNT)
        {
            List destroyed = event.blockList();
            Iterator it = destroyed.iterator();

            while (it.hasNext())
            {
                Block block = (Block) it.next();
                if (!blocks.contains(block.getLocation()))
                    it.remove();
            }
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event)
    {
        Player player = event.getPlayer();
        GamePlayer gp = null;

        Material block = event.getBlock().getType();

        if ((gp = Bridge.gamePlayers.get(player)) != null)
        {
            if (gp.isInLobby())
            {
                event.setCancelled(true);
                return;
            }

            for (Location loc : getSpawnLocs(gp.getGame().getName()))
            {
                if (event.getBlock().getLocation().distance(loc) <= 3)
                {
                    event.setCancelled(true);
                    return;
                }
            }

            if (block == Material.TNT && !gp.isInLobby() && gp.isInGame())
            {
                Location loc = event.getBlock().getLocation();
                loc.getWorld().spawn(loc, TNTPrimed.class);
                event.setCancelled(true);
            }

            blocks.add(event.getBlock().getLocation());
            gp.getGame().blocks.add(event.getBlock().getLocation());
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event)
    {
        Player player = event.getPlayer();
        GamePlayer gp = null;

        if ((gp = Bridge.gamePlayers.get(player)) != null)
        {
            if (event.getBlock().getType() == Material.BED_BLOCK)
                return;

            if (!blocks.contains(event.getBlock().getLocation()))
                event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event)
    {
        Player player = event.getPlayer();
        GamePlayer gp = null;

        if ((gp = Bridge.gamePlayers.get(player)) != null && gp.isInLobby())
        {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPickUp(PlayerPickupItemEvent event)
    {
        Player player = event.getPlayer();
        GamePlayer gp = null;

        if ((gp = Bridge.gamePlayers.get(player)) != null && gp.isInLobby())
        {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event)
    {
        Player player = (Player) event.getEntity();
        GamePlayer gp = null;

        if ((gp = Bridge.gamePlayers.get(player)) != null && (gp.isInLobby() || gp.isInGame()))
        {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onVoid(EntityDamageEvent event)
    {
        if (event.getEntity() instanceof Player)
        {
            Player player = (Player) event.getEntity();
            GamePlayer gp = null;

            if ((gp = Bridge.gamePlayers.get(player)) != null && gp.isInGame())
            {
                if (event.getCause() == EntityDamageEvent.DamageCause.VOID)
                {
                    if (die.contains(player))
                    {
                        if ((gp = Bridge.gamePlayers.get(player)) != null)
                        {
                            List<String> members = Game.getConfig().getStringList(gp.getGame().getName() + ".spawns." + gp.getTeam() + ".members");

                            if (members.size() - 1 == 0)
                            {
                                gp.getGame().sendMessage(TextUtils.get("teameliminated").replace("%team%", gp.getTeam()));
                            }

                            player.setGameMode(GameMode.SPECTATOR);
                            player.setHealth(player.getMaxHealth());
                            members.remove(player.getName());
                            Game.getConfig().set(gp.getGame().getName() + ".spawns." + gp.getTeam() + ".members", members);
                            Game.saveConfig();

                            List<String> players = Game.getConfig().getStringList(gp.getGame().getName() + ".members");
                            players.remove(player.getName());
                            Game.getConfig().set(gp.getGame().getName() + ".spawns." + gp.getTeam() + ".members", players);
                            Game.saveConfig();

                            player.teleport(getTeamSpawn(gp));

                            return;
                        }
                    }

                    Map<String, Object> section = Game.getConfig().getConfigurationSection(gp.getGame().getName() + ".spawns").getValues(false);

                    for (Map.Entry<String, Object> entry : section.entrySet())
                    {
                        for (String str : Game.getConfig().getStringList(gp.getGame().getName() + ".spawns." + entry.getKey() + ".members"))
                        {
                            Player p = Bukkit.getPlayer(str);
                            FileConfiguration config = Game.getConfig();

                            World world = Bukkit.getWorld(config.getString(gp.getGame().getName() + ".lobby." + ".world"));

                            double x = config.getDouble(gp.getGame().getName() + ".spawns." + entry.getKey() + ".x");
                            double y = config.getDouble(gp.getGame().getName() + ".spawns." + entry.getKey() + ".y");
                            double z = config.getDouble(gp.getGame().getName() + ".spawns." + entry.getKey() + ".z");

                            double yaw = config.getDouble(gp.getGame().getName() + ".spawns." + entry.getKey() + ".yaw");
                            double pitch = config.getDouble(gp.getGame().getName() + ".spawns." + entry.getKey() + ".pitch");

                            Location spawn = new Location(world, x, y, z, (float) yaw, (float) pitch);

                            if (p.getName().equals(player.getName()))
                            {
                                event.setCancelled(true);
                                player.getInventory().clear();
                                player.setHealth(player.getMaxHealth());
                                player.setFallDistance(0);
                                player.teleport(spawn);
                                Game.getItemss(player);


                                break;
                            }
                        }
                    }
                }
            }
            else if ((gp = Bridge.gamePlayers.get(player)) != null && gp.isInLobby())
            {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event)
    {
        Player player = event.getPlayer();
        GamePlayer gp = null;

        if ((gp = Bridge.gamePlayers.get(player)) != null && (gp.isInLobby() || gp.isInGame()))
        {
            String msg = event.getMessage();

            event.setCancelled(true);

            gp.getGame().sendMessage(TextUtils.get("prefix") + player.getDisplayName() + " §7" + msg);
        }
    }

    public ArrayList<Location> getSpawnLocs(String game)
    {
        Map<String, Object> sections = Game.getConfig().getConfigurationSection(game + ".spawns").getValues(false);
        ArrayList<Location> locs = new ArrayList<>();

        Location spawn = null;

        for (Map.Entry entry : sections.entrySet())
        {
            World world = Bukkit.getWorld(Game.getConfig().getString(game + ".lobby." + ".world"));

            double x = Game.getConfig().getDouble(game + ".spawns." + entry.getKey() + ".x");
            double y = Game.getConfig().getDouble(game + ".spawns." + entry.getKey() + ".y");
            double z = Game.getConfig().getDouble(game + ".spawns." + entry.getKey() + ".z");

            double yaw = Game.getConfig().getDouble(game + ".spawns." + entry.getKey() + ".yaw");
            double pitch = Game.getConfig().getDouble(game + ".spawns." + entry.getKey() + ".pitch");

            spawn = new Location(world, x, y, z, (float) yaw, (float) pitch);
            locs.add(spawn);
        }

        return locs;
    }
}
