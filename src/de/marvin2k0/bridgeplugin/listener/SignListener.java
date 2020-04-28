package de.marvin2k0.bridgeplugin.listener;

import de.marvin2k0.bridgeplugin.game.Game;
import de.marvin2k0.bridgeplugin.utils.TextUtils;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class SignListener implements Listener
{
    @EventHandler
    public void onSign(SignChangeEvent event)
    {
        Player player = event.getPlayer();

        if (!player.hasPermission("bw.sign"))
            return;

        String line1 = event.getLine(0);
        String line2 = event.getLine(1);

        if (line1 != null && line1.equalsIgnoreCase("[BridgeWars]") && line2 != null && Game.exists(line2))
        {
            event.setLine(0, TextUtils.get("prefix"));
        }
    }

    @EventHandler
    public void onSign(PlayerInteractEvent event)
    {
        Player player = event.getPlayer();

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && (event.getClickedBlock().getType() == Material.SIGN || event.getClickedBlock().getType() == Material.SIGN_POST || event.getClickedBlock().getType() == Material.WALL_SIGN) && player.hasPermission("bridge.game"))
        {
            Sign sign = (Sign) event.getClickedBlock().getState();
            String game = sign.getLine(1);

            if (game != null && Game.exists(game))
            {
                if (Game.getGameFromName(game).hasStarted())
                {
                    player.sendMessage(TextUtils.get("started"));

                    return;
                }
                Game.join(game, player);
            }
        }

    }
}
