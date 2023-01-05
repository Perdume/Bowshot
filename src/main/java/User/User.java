package User;


import Area.SubArena;
import Game.Game;
import bowshot.bowshot.Bowshot;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class User {
    private UUID user;
    private boolean Spect = false;
    private Bowshot bs = Bowshot.getPlugin(Bowshot.class);
    public User(UUID uid){
        this.user = uid;
    }

    public User(Player player){
        this.user = player.getUniqueId();
    }
    public SubArena getArena(){
        for(String name: bs.subarenaManager.getArenas()){
            assert bs.subarenaManager.getArena(name).getPlayers() != null;
            if(bs.subarenaManager.getArena(name).getPlayers().contains(bs.usermanager.getUser(this.user))){
                return bs.subarenaManager.getArena(name);
            }
        }
        return null;
    }
    public Game getGame(){
        for(Game game: bs.gamemanager.getGames()){
            assert game.GetPlayers() != null;
            if(game.GetPlayers().contains(bs.usermanager.getUser(this.user))){
                return game;
            }
        }
        return null;
    }
    public UUID getUniqueId() {
        return user;
    }
    public Player getPlayer(){
        return Bukkit.getPlayer(user);
    }
    public Boolean isSpectator() {
        return Spect;
    }
    public void setSpectator(Boolean bool){
        Spect = bool;
    }
}
