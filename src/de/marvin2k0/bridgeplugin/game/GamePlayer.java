package de.marvin2k0.bridgeplugin.game;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.List;

public class GamePlayer
{
    private boolean inLobby = true;
    private boolean inGame = false;

    private Player player;
    private Game game;

    private String team;

    public GamePlayer (Player player, Game game)
    {
        this.player = player;
        this.game = game;
    }

    public boolean isInLobby()
    {
        return inLobby;
    }

    public boolean isInGame()
    {
        return inGame;
    }

    public void setInLobby(boolean flag)
    {
        this.inLobby = flag;
        this.inGame = !inLobby;
    }

    public void setInGame(boolean flag)
    {
        this.inGame = flag;
        this.inLobby = !inGame;
    }

    public String getTeam()
    {
        return team;
    }

    public void setTeam(String team, String color)
    {
        if (getTeam() != null)
        {
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            Team teamscorebard = scoreboard.registerNewTeam(team);
            teamscorebard.setPrefix(color);
            teamscorebard.addPlayer(player);

            //game.setScoreBoard(scoreboard);

            List<String> old = Game.getConfig().getStringList(game.getName() + ".spawns." + getTeam() + ".members");
            old.remove(player.getName());

            Game.getConfig().set(game.getName() + ".spawns." + getTeam() + ".members", old);
        }

        List<String> teamMembers = Game.getConfig().getStringList(game.getName() + ".spawns." + team + ".members");
        teamMembers.add(player.getName());

        Game.getConfig().set(game.getName() + ".spawns." + team + ".members", teamMembers);

        System.out.println(game.getName() + ".spawns." + team + ".members" + " to " + teamMembers);

        Game.saveConfig();

        this.team = team;
    }

    public Player getPlayer()
    {
        return player;
    }

    public Game getGame()
    {
        return game;
    }
}
