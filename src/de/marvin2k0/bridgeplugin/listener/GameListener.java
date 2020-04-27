package de.marvin2k0.bridgeplugin.listener;

import de.marvin2k0.bridgeplugin.Bridge;
import de.marvin2k0.bridgeplugin.game.Game;
import de.marvin2k0.bridgeplugin.game.GamePlayer;
import de.marvin2k0.bridgeplugin.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
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

    @EventHandler
    public void onBed(BlockBreakEvent event)
    {
        Player player = event.getPlayer();
        GamePlayer gp = null;

        if ((gp = Bridge.gamePlayers.get(player)) != null)
        {
            if (gp.getGame().getMode() == Game.Mode.ONE_ON_ONE)
            {
                if (event.getBlock().getType() == Material.BED_BLOCK)
                {
                    event.setCancelled(true);
                    System.out.println("punkt für " + player.getName() + "'s team (" + gp.getTeam() + ")");
                    //TODO: score team
                }
            }
        }
    }

    @EventHandler
    public void onFreeze(PlayerMoveEvent event)
    {
        if (Bridge.freeze.contains(event.getPlayer()))
        {
            event.setCancelled(true);
            System.out.println(Bridge.freeze);
        }
    }

    @EventHandler
    public void onKill(PlayerDeathEvent event)
    {
        Player killer = event.getEntity().getKiller();
        Player player = event.getEntity().getPlayer();
        GamePlayer gp = null;

        if ((gp = Bridge.gamePlayers.get(player)) != null)
            if (killer == null)
            {
                event.setDeathMessage(TextUtils.get("deathmessage_2").replace("%player%", player.getName()));
                return;
            }

            event.setDeathMessage(TextUtils.get("deathmessage").replace("%player%", player.getName()).replace("%killer%", killer.getName()));

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

            if (block == Material.TNT && !gp.isInLobby() && gp.isInGame())
            {
                Location loc = event.getBlock().getLocation();
                loc.getWorld().spawn(loc, TNTPrimed.class);
                event.setCancelled(true);
            }

            blocks.add(event.getBlock().getLocation());
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event)
    {
        Player player = event.getPlayer();
        GamePlayer gp = null;

        if ((gp = Bridge.gamePlayers.get(player)) != null)
        {
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
                                //TODO: prüfen ob respawnen kann
                                event.setCancelled(true);
                                player.getInventory().clear();
                                player.setHealth(player.getMaxHealth());
                                player.setFallDistance(0);
                                player.teleport(spawn);

                                break;
                            }
                        }
                    }
                }
            }
            else if ((gp = Bridge.gamePlayers.get(player)) != null && gp.isInLobby())
            {
                System.out.println("damage in lobby");
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
}
