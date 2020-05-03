package de.marvin2k0.bridgeplugin.listener;

import de.marvin2k0.bridgeplugin.Bridge;
import de.marvin2k0.bridgeplugin.game.Game;
import de.marvin2k0.bridgeplugin.game.GamePlayer;
import de.marvin2k0.bridgeplugin.utils.TextUtils;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class GameListener implements Listener
{
    ArrayList<Location> blocks = new ArrayList<>();
    ArrayList<Player> die = new ArrayList<>();

    @EventHandler
    public void onBed(BlockBreakEvent event)
    {
        Player player = event.getPlayer();
        GamePlayer gp = null;

        if ((gp = Bridge.gamePlayers.get(player)) != null && gp.isInGame())
        {
            String game = gp.getGame().getName();

            System.out.println(Game.getConfig().getBoolean(gp.getGame().getName() + ".bed") + " for " + gp.getGame().getName());

            if (Game.getConfig().getBoolean(gp.getGame().getName() + ".bed"))
            {
                if (event.getBlock().getType() == Material.BED_BLOCK)
                {
                    event.setCancelled(true);

                    if (getTeamSpawn(gp).distance(event.getBlock().getLocation()) < 15)
                    {
                        player.sendMessage(TextUtils.get("ownbed"));
                        return;
                    }

                    int points = Game.getConfig().getInt(gp.getGame().getName() + ".spawns." + gp.getTeam() + ".points") + 1;

                    if (points >= 5)
                    {
                        gp.getGame().reset();
                        gp.getGame().win(gp.getTeam());
                        return;
                    }

                    Game.getConfig().set(gp.getGame().getName() + ".spawns." + gp.getTeam() + ".points", points);
                    Game.saveConfig();

                    gp.getGame().sendMessage(TextUtils.get("score").replace("%player%", player.getDisplayName()).replace("%team%", gp.getTeam()));
                    gp.getGame().tpToSpawn();
                    gp.getGame().resetBlocks();
                    gp.getGame().setScoreboards();
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

                            if (event.getBlock().getLocation().distance(spawn) <= 10)
                            {
                                die.add(p);

                                player1 = p;
                            }
                        }
                    }

                    if (!des.contains(Bridge.gamePlayers.get(player1).getTeam()))
                    {
                        des.add(Bridge.gamePlayers.get(player1).getTeam());
                        gp.getGame().sendMessage(TextUtils.get("beddestroyed").replace("%team%", Bridge.gamePlayers.get(player1).getTeam()));
                    }
                }
            }
        }
    }

    public static ArrayList<String> des = new ArrayList<>();

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
        Player player = event.getPlayer();
        GamePlayer gp = null;
        if ((gp = Bridge.gamePlayers.get(player)) != null && gp.isInGame())
        {
            gp.getGame().checkWin("move event");

            if (player.getLocation().getY() <= (getTeamSpawn(gp).getY() - 12))
            {
                if (die.contains(player) && !Game.getConfig().getBoolean(gp.getGame().getName() + ".bed"))
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
                        gp.getGame().checkWin("void");

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

                            if (Game.getConfig().getBoolean(gp.getGame().getName() + ".bed"))
                            {
                                Game.getItemss(player);
                            }
                            player.updateInventory();
                            gp.getGame().checkWin("void 2");


                            break;
                        }
                    }
                }
            }
        }

        if (Bridge.freeze.contains(event.getPlayer()))
        {
            Location from = event.getFrom();
            Location to = event.getTo();

            if (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ())
                return;

            event.setTo(event.getFrom());
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
                event.getDrops().clear();

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

                gp.getGame().checkWin("player death event");
            }
        }
        else
        {
            if ((gp = Bridge.gamePlayers.get(player)) != null)
            {
                event.getDrops().clear();
                Game game = gp.getGame();

                player.setHealth(player.getMaxHealth());
                player.teleport(getTeamSpawn(gp));
                player.setVelocity(new Vector(0, 0, 0));
                Bukkit.getScheduler().scheduleSyncDelayedTask(Bridge.plugin, new Runnable()
                {

                    @Override
                    public void run()
                    {
                        if (Game.getConfig().getBoolean(game.getName() + ".bed"))
                        {
                            Game.getItemss(player);
                        }
                    }
                }, 5);
                player.updateInventory();
            }
        }

        if ((gp = Bridge.gamePlayers.get(player)) != null)
        {
            gp.getGame().checkWin("player death event");
            if (killer == null)
            {
                event.setDeathMessage(TextUtils.get("deathmessage_2").replace("%player%", player.getDisplayName()));
                return;
            }

            event.setDeathMessage(TextUtils.get("deathmessage").replace("%player%", player.getDisplayName()).replace("%killer%", killer.getDisplayName()));
            return;
        }

        event.setDeathMessage(null);
    }

    @EventHandler
    public void onTeam(PlayerInteractEvent event)
    {
        Player player = event.getPlayer();
        GamePlayer gp = null;
        ItemStack item = event.getItem();

        if (Bridge.placeSpawner.containsKey(player))
        {
            if (player.getItemInHand() != null && player.getItemInHand().hasItemMeta() && event.getClickedBlock() != null)
            {
                String name = player.getItemInHand().getItemMeta().getDisplayName();
                String game = Bridge.placeSpawner.get(player);
                Location loc = event.getClickedBlock().getLocation();
                String random = System.currentTimeMillis() + "";

                if (name.equals("§fClick for Iron"))
                {

                    Game.getConfig().set(game + ".spawner.iron." + random + ".world", loc.getWorld().getName());
                    Game.getConfig().set(game + ".spawner.iron." + random + ".x", loc.getX());
                    Game.getConfig().set(game + ".spawner.iron." + random + ".y", loc.getY());
                    Game.getConfig().set(game + ".spawner.iron." + random + ".z", loc.getZ());

                    event.getClickedBlock().setType(Material.IRON_BLOCK);
                }
                else if (name.equals("§6Click for Gold"))
                {
                    Game.getConfig().set(game + ".spawner.gold." + random + ".world", loc.getWorld().getName());
                    Game.getConfig().set(game + ".spawner.gold." + random + ".x", loc.getX());
                    Game.getConfig().set(game + ".spawner.gold." + random + ".y", loc.getY());
                    Game.getConfig().set(game + ".spawner.gold." + random + ".z", loc.getZ());

                    event.getClickedBlock().setType(Material.GOLD_BLOCK);
                }
                else if (name.equals("§bClick for Diamond"))
                {
                    Game.getConfig().set(game + ".spawner.dia." + random + ".world", loc.getWorld().getName());
                    Game.getConfig().set(game + ".spawner.dia." + random + ".x", loc.getX());
                    Game.getConfig().set(game + ".spawner.dia." + random + ".y", loc.getY());
                    Game.getConfig().set(game + ".spawner.dia." + random + ".z", loc.getZ());

                    event.getClickedBlock().setType(Material.DIAMOND_BLOCK);
                }

                Game.saveConfig();
                player.setItemInHand(null);
                Bridge.placeSpawner.remove(player);
            }
        }

        try
        {
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
        catch (Exception e)
        {
        }
    }

    public boolean hasInventoryFull(Player p)
    {
        int slot = 0;
        ItemStack[] arrayOfItemStack;
        int x = (arrayOfItemStack = p.getInventory().getContents()).length;
        for (int i = 0; i < x; i++)
        {
            ItemStack contents = arrayOfItemStack[i];
            if (contents == null)
                slot++;
        }
        return (slot == 0);
    }

    public Inventory villagerShop()
    {
        Inventory s = Bukkit.createInventory(null, 54, "§6Shop");

        ItemStack fillUP = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 10);
        ItemMeta glassMeta = fillUP.getItemMeta();
        glassMeta.setDisplayName(" ");
        fillUP.setItemMeta(glassMeta);

        for (int i = 0; i < s.getSize(); i++)
            s.setItem(i, fillUP);

        s.setItem(10, createItem(Material.SANDSTONE, 4, null, new int[]{0}, "§l§f2 iron"));
        s.setItem(19, createItem(Material.ENDER_STONE, 4, null, new int[]{0}, "§l§f8 iron"));
        s.setItem(28, createItem(Material.WOOD, 4, null, new int[]{0}, "§l§64 gold"));
        s.setItem(11, createItem(Material.STICK, 1, new Enchantment[]{Enchantment.KNOCKBACK}, new int[]{1}, "§l§f5 iron"));
        s.setItem(20, createItem(Material.STICK, 1, new Enchantment[]{Enchantment.KNOCKBACK, Enchantment.DAMAGE_ALL}, new int[]{1, 1}, "§l§f10 iron"));
        s.setItem(29, createItem(Material.STICK, 1, new Enchantment[]{Enchantment.KNOCKBACK, Enchantment.DAMAGE_ALL}, new int[]{1, 2}, "§l§65 gold"));
        s.setItem(12, createItem(Material.WOOD_SWORD, 1, new Enchantment[]{Enchantment.DAMAGE_ALL}, new int[]{1}, "§l§f20 iron"));
        s.setItem(21, createItem(Material.STONE_SWORD, 1, new Enchantment[]{Enchantment.DAMAGE_ALL}, new int[]{1}, "§l§f40 iron"));
        s.setItem(30, createItem(Material.GOLD_SWORD, 1, new Enchantment[]{Enchantment.DAMAGE_ALL}, new int[]{1}, "§l§620 gold"));
        s.setItem(13, createItem(Material.TNT, 1, null, new int[]{0}, "§l§610 gold"));
        s.setItem(22, createItem(Material.GOLDEN_APPLE, 1, null, new int[]{0}, "§l§65 gold"));
        s.setItem(31, createItem(Material.ENDER_PEARL, 1, null, new int[]{0}, "§l§b5 dia"));
        s.setItem(32, createItem(Material.GOLD_AXE, 1, new Enchantment[]{Enchantment.DIG_SPEED}, new int[]{1}, "§l§b3 dia"));
        s.setItem(14, createItem(Material.LADDER, 4, null, new int[]{0}, "§l§f30 iron"));
        s.setItem(23, createItem(Material.WEB, 4, null, new int[]{0}, "§l§f30 iron"));
        s.setItem(15, createItem(Material.LEATHER_HELMET, 1, new Enchantment[]{Enchantment.PROTECTION_ENVIRONMENTAL}, new int[]{1}, "§l§f15 iron"));
        s.setItem(24, createItem(Material.LEATHER_CHESTPLATE, 1, new Enchantment[]{Enchantment.PROTECTION_ENVIRONMENTAL}, new int[]{1}, "§l§f15 iron"));
        s.setItem(33, createItem(Material.LEATHER_LEGGINGS, 1, new Enchantment[]{Enchantment.PROTECTION_ENVIRONMENTAL}, new int[]{1}, "§l§f15 iron"));
        s.setItem(42, createItem(Material.LEATHER_BOOTS, 1, new Enchantment[]{Enchantment.PROTECTION_ENVIRONMENTAL}, new int[]{1}, "§l§f15 iron"));
        s.setItem(16, createItem(Material.CHAINMAIL_HELMET, 1, new Enchantment[]{Enchantment.PROTECTION_ENVIRONMENTAL}, new int[]{1}, "§l§b2 dia"));
        s.setItem(25, createItem(Material.CHAINMAIL_CHESTPLATE, 1, new Enchantment[]{Enchantment.PROTECTION_ENVIRONMENTAL}, new int[]{1}, "§l§b2 dia"));
        s.setItem(34, createItem(Material.CHAINMAIL_LEGGINGS, 1, new Enchantment[]{Enchantment.PROTECTION_ENVIRONMENTAL}, new int[]{1}, "§l§b2 dia"));
        s.setItem(43, createItem(Material.CHAINMAIL_BOOTS, 1, new Enchantment[]{Enchantment.PROTECTION_ENVIRONMENTAL}, new int[]{1}, "§l§b2 dia"));
        s.setItem(41, createItem(Material.STONE_AXE, 1, new Enchantment[]{Enchantment.DIG_SPEED}, new int[]{1}, "§l§64 gold"));
        s.setItem(40, createItem(Material.WOOD_AXE, 1, new Enchantment[]{Enchantment.DIG_SPEED}, new int[]{1}, "§l§f5 iron"));
        s.setItem(39, createItem(Material.GOLD_PICKAXE, 1, new Enchantment[]{Enchantment.DIG_SPEED}, new int[]{1}, "§l§b3 dia"));
        s.setItem(38, createItem(Material.STONE_PICKAXE, 1, new Enchantment[]{Enchantment.DIG_SPEED}, new int[]{1}, "§l§64 gold"));
        s.setItem(37, createItem(Material.WOOD_PICKAXE, 1, new Enchantment[]{Enchantment.DIG_SPEED}, new int[]{1}, "§l§64 gold"));

        return s;
    }

    @EventHandler
    public void onGui(PlayerInteractEntityEvent event)
    {
        Player player = event.getPlayer();
        GamePlayer gp = null;


        if ((gp = Bridge.gamePlayers.get(player)) != null && gp.isInGame() && event.getRightClicked() instanceof Villager)
        {
            event.setCancelled(true);

            player.openInventory(villagerShop());
        }
    }

    public void buy(ItemStack item, Player player, boolean shift)
    {
        if (item.getItemMeta().getLore() == null)
            return;

        if (hasInventoryFull(player))
        {
            player.sendMessage(TextUtils.get("inventoryfull"));

            return;
        }

        int amount = shift ? 8 * Integer.valueOf(item.getItemMeta().getLore().get(0).split(" ")[0].substring(4)) : Integer.valueOf(item.getItemMeta().getLore().get(0).split(" ")[0].substring(4));
        String ingot = item.getItemMeta().getLore().get(0).split(" ")[1];

        if (ingot.equalsIgnoreCase("iron"))
        {
            if (hasItem(Material.IRON_INGOT, (byte) 0, amount, player))
            {
                removeItem(amount, Material.IRON_INGOT, (byte) 0, player);
                Enchantment[] enchantments = new Enchantment[9];
                int[] level = new int[9];

                for (int i = 0; i < item.getEnchantments().keySet().size(); i++)
                {
                    enchantments[i] = (Enchantment) item.getEnchantments().keySet().toArray()[i];
                }

                for (int i = 0; i < item.getEnchantments().values().size(); i++)
                {
                    level[i] = (int) item.getEnchantments().values().toArray()[i];
                }

                ItemStack i = createItem(item.getType(), shift ? 8 * item.getAmount() : item.getAmount(), enchantments, level, " ");
                ItemMeta meta = i.getItemMeta();
                meta.setLore(new ArrayList<String>());
                i.setItemMeta(meta);

                player.getInventory().addItem(i);
            }
        }
        else if (ingot.equalsIgnoreCase("gold"))
        {
            if (hasItem(Material.GOLD_INGOT, (byte) 0, amount, player))
            {
                removeItem(amount, Material.GOLD_INGOT, (byte) 0, player);
                Enchantment[] enchantments = new Enchantment[9];
                int[] level = new int[9];

                for (int i = 0; i < item.getEnchantments().keySet().size(); i++)
                {
                    enchantments[i] = (Enchantment) item.getEnchantments().keySet().toArray()[i];
                }

                for (int i = 0; i < item.getEnchantments().values().size(); i++)
                {
                    level[i] = (int) item.getEnchantments().values().toArray()[i];
                }

                ItemStack i = createItem(item.getType(), shift ? 8 * item.getAmount() : item.getAmount(), enchantments, level, " ");
                ItemMeta meta = i.getItemMeta();
                meta.setLore(new ArrayList<String>());
                i.setItemMeta(meta);

                player.getInventory().addItem(i);
            }
        }
        else if (ingot.equalsIgnoreCase("dia"))
        {
            if (hasItem(Material.DIAMOND, (byte) 0, amount, player))
            {
                removeItem(amount, Material.DIAMOND, (byte) 0, player);
                Enchantment[] enchantments = new Enchantment[9];
                int[] level = new int[9];

                for (int i = 0; i < item.getEnchantments().keySet().size(); i++)
                {
                    enchantments[i] = (Enchantment) item.getEnchantments().keySet().toArray()[i];
                }

                for (int i = 0; i < item.getEnchantments().values().size(); i++)
                {
                    level[i] = (int) item.getEnchantments().values().toArray()[i];
                }

                ItemStack i = createItem(item.getType(), shift ? 8 * item.getAmount() : item.getAmount(), enchantments, level, " ");
                ItemMeta meta = i.getItemMeta();
                meta.setLore(new ArrayList<String>());
                i.setItemMeta(meta);

                player.getInventory().addItem(i);
            }
        }
    }

    private boolean hasItem(Material material, byte data, int quantity, Player p)
    {
        int item = 0;
        ItemStack[] arrayOfItemStack;
        int x = (arrayOfItemStack = p.getInventory().getContents()).length;
        for (int i = 0; i < x; i++)
        {
            ItemStack contents = arrayOfItemStack[i];
            if (contents != null && contents.getType() != Material.AIR && contents.getType() == material && contents
                    .getData().getData() == data)
                item += contents.getAmount();
        }
        if (item < quantity)
            return false;
        return true;
    }

    private void removeItem(int q, Material material, byte data, Player p)
    {
        int item = q;
        ItemStack[] arrayOfItemStack;
        int x = (arrayOfItemStack = p.getInventory().getContents()).length;
        for (int i = 0; i < x; i++)
        {
            ItemStack contents = arrayOfItemStack[i];
            if (contents != null && contents.getType() != Material.AIR && contents.getType() == material && contents
                    .getData().getData() == data)
                if ((item >= 64 && contents.getAmount() == 64) || item == contents.getAmount())
                {
                    p.getInventory().remove(contents);
                }
                else
                {
                    ItemStack n = new ItemStack(contents.getType(), contents.getAmount() - item);
                    p.getInventory().remove(contents);
                    p.getInventory().addItem(new ItemStack[]{n});
                }
        }
    }

    @EventHandler
    public void onInv(InventoryClickEvent event)
    {
        Player player = (Player) event.getWhoClicked();
        GamePlayer gp = null;
        Inventory inv = event.getInventory();
        ItemStack item = event.getCurrentItem();

        if (inv != null && item != null && item.hasItemMeta() && inv.getName().equals("§6Shop"))
        {
            event.setCancelled(true);

            buy(event.getCurrentItem(), player, event.isShiftClick());

            return;
        }
        else if (inv != null && item != null && item.hasItemMeta() && inv.getName().equals(TextUtils.get("chooseteam", false)))
        {
            event.setCancelled(true);

            if ((gp = Bridge.gamePlayers.get(player)) != null)
            {
                String team = item.getItemMeta().getDisplayName().replace("§7", "");
                List<String> members = Game.getConfig().getStringList(gp.getGame().getName() + ".spawns." + team + ".members");

                if (members.size() >= (gp.getGame().getMode().getPlayersPerTeam()))
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

                gp.setTeam(team, color);
                player.setDisplayName(color + player.getName());
                player.setPlayerListName(color + player.getName());
                player.sendMessage(TextUtils.get("joinedteam").replace("%team%", team));
                player.closeInventory();
                System.out.println("Full " + (members.size() >= (gp.getGame().getMode().getPlayersPerTeam() + 1)));
                System.out.println(team + " (" + (members.size() + 1) + "/" + gp.getGame().getMode().getPlayersPerTeam() + ")");
            }
        }
    }

    public ItemStack createItem(Material item, int amount, Enchantment[] ench, int[] lvl, String price)
    {
        ArrayList<String> lore = new ArrayList<>();
        lore.add(price);

        ItemStack i = new ItemStack(item);

        if (ench != null)
            for (int j = 0; j < ench.length; j++)
                if (!(ench[j] == null))
                    i.addUnsafeEnchantment(ench[j], lvl[j]);

        ItemMeta itemMeta = i.getItemMeta();
        //itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        itemMeta.setLore(lore);
        i.setItemMeta(itemMeta);
        i.setAmount(amount);

        return i;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event)
    {
        Player player = event.getPlayer();
        GamePlayer gp = null;

        if ((gp = Bridge.gamePlayers.get(player)) != null)
            gp.getGame().leave(gp.getGame().getName(), player);
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

            blocks.add(event.getBlock().getLocation());
            gp.getGame().saveBlock(event.getBlock().getLocation());

            for (Location loc : getSpawnLocs(gp.getGame().getName()))
            {
                if (event.getBlock().getLocation().distance(loc) <= 2)
                {
                    event.setCancelled(true);
                    return;
                }
            }

            for (Location loc : getBedLocs(gp.getGame().getName()))
            {
                if (event.getBlock().getLocation().distance(loc) <= 3 && Game.getConfig().getBoolean(gp.getGame().getName() + ".bed"))
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
                removeItem(1, Material.TNT, (byte) 0, player);
            }
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

        if ((gp = Bridge.gamePlayers.get(player)) != null && (gp.isInLobby() || gp.isInGame()))
        {
            if (Game.getConfig().getBoolean(gp.getGame().getName() + ".bed"))
            {
                event.setCancelled(true);
            }
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
    public void onDamage(EntityDamageByEntityEvent event)
    {
        if (event.getEntityType() == EntityType.VILLAGER)
        {
            event.setCancelled(true);
        }

    }

    @EventHandler
    public void onVoid(EntityDamageEvent event)
    {
        GamePlayer gp = null;

        if (event.getEntity() instanceof Player)
        {
            Player player = (Player) event.getEntity();

            if ((gp = Bridge.gamePlayers.get(player)) != null && gp.isInGame())
            {
                if (event.getCause() == EntityDamageEvent.DamageCause.VOID)
                {

                }
            }
            else if ((gp = Bridge.gamePlayers.get(player)) != null && gp.isInLobby())
            {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event)
    {
        if (event.getEntityType() == EntityType.VILLAGER)
        {
            Location loc = event.getLocation();

            Villager v = (Villager) event.getEntity();
            v.setCanPickupItems(false);
            v.setNoDamageTicks(20);
            v.setRemoveWhenFarAway(false);
            noAI(v);
        }
    }

    void noAI(Entity bukkitEntity)
    {
        net.minecraft.server.v1_8_R3.Entity nmsEntity = ((CraftEntity) bukkitEntity).getHandle();
        NBTTagCompound tag = nmsEntity.getNBTTag();
        if (tag == null)
        {
            tag = new NBTTagCompound();
        }
        nmsEntity.c(tag);
        tag.setInt("NoAI", 1);
        nmsEntity.f(tag);
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

            gp.getGame().sendMessage(TextUtils.get("prefix") + player.getDisplayName() + " >§f" + msg);
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

    public ArrayList<Location> getBedLocs(String game)
    {
        Map<String, Object> sections = Game.getConfig().getConfigurationSection(game + ".spawns").getValues(false);
        ArrayList<Location> locs = new ArrayList<>();

        Location spawn = null;

        for (Map.Entry entry : sections.entrySet())
        {
            World world = Bukkit.getWorld(Game.getConfig().getString(game + ".spawns." + entry.getKey() + ".bed.world"));

            double x = Game.getConfig().getDouble(game + ".spawns." + entry.getKey() + ".bed.x");
            double y = Game.getConfig().getDouble(game + ".spawns." + entry.getKey() + ".bed.y");
            double z = Game.getConfig().getDouble(game + ".spawns." + entry.getKey() + ".bed.z");


            spawn = new Location(world, x, y, z);
            locs.add(spawn);
        }

        return locs;
    }
}
