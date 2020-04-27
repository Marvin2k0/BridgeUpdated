package de.marvin2k0.bridgeplugin.listener;

import de.marvin2k0.bridgeplugin.Bridge;
import de.marvin2k0.bridgeplugin.game.Game;
import de.marvin2k0.bridgeplugin.utils.TextUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class PlaceBedListener implements Listener
{
    @EventHandler
    public void onPlace(BlockPlaceEvent event)
    {
        Player player = event.getPlayer();

        if (Bridge.placeBed.containsKey(player))
        {
            Block block = event.getBlock();

            if (block.getType() == Material.BED_BLOCK)
            {
                String game = Bridge.placeBed.get(player).split(":")[0];
                String team = Bridge.placeBed.get(player).split(":")[1];

                Location loc = player.getLocation();

                String world = loc.getWorld().getName();
                double x = loc.getX();
                double y = loc.getY();
                double z = loc.getZ();

                Game.getConfig().set(game + ".spawns." + team + ".bed.world", world);
                Game.getConfig().set(game + ".spawns." + team + ".bed.x", x);
                Game.getConfig().set(game + ".spawns." + team + ".bed.y", y);
                Game.getConfig().set(game + ".spawns." + team + ".bed.z", z);

                player.sendMessage(TextUtils.get("blockplaced").replace("%team%", team));

                Game.saveConfig();

                Bridge.placeBed.remove(player);
            }
        }
    }
}
