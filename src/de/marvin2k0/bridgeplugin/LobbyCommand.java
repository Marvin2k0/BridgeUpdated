package de.marvin2k0.bridgeplugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class LobbyCommand implements CommandExecutor
{
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (!(sender instanceof Player))
        {
            sender.sendMessage("§cOnly for players!");
            return true;
        }

        Player player = (Player) sender;
        FileConfiguration config = Bridge.plugin.getConfig();

        if (label.equalsIgnoreCase("setlobby"))
        {
            Location location = player.getLocation();

            String world = location.getWorld().getName();

            double x = location.getX();
            double y = location.getY();
            double z = location.getZ();

            float yaw = location.getYaw();
            float pitch = location.getPitch();

            config.set("lobby.world", world);
            config.set("lobby.x", x);
            config.set("lobby.y", y);
            config.set("lobby.z", z);
            config.set("lobby.yaw", yaw);
            config.set("lobby.pitch", pitch);

            Bridge.plugin.saveConfig();

            player.sendMessage("§aLobby has been set!");
        }
        else if (label.equalsIgnoreCase("lobby"))
        {
            if (!config.isSet("lobby"))
            {
                player.sendMessage("§cLobby has not been set!");

                return true;
            }

            Location location = null;

            World world = Bukkit.getWorld(config.getString("lobby.world"));
            double x = config.getDouble("lobby.x");
            double y = config.getDouble("lobby.y");
            double z = config.getDouble("lobby.z");

            double yaw = config.getDouble("lobby.yaw");
            double pitch = config.getDouble("lobby.pitch");

            location = new Location(world, x, y, z, (float) yaw, (float) pitch);

            if (Bridge.gamePlayers.get(player) != null)
                Bridge.gamePlayers.get(player).getGame().leave(Bridge.gamePlayers.get(player).getGame().getName(), player);

            player.teleport(location);

            return true;
        }

        return true;
    }
}
