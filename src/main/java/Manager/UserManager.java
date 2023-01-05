package Manager;

import Area.SubArena;
import User.User;
import bowshot.bowshot.Bowshot;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserManager {
    private Bowshot bs;
    private final List<User> users = new ArrayList<>();
    public UserManager(Bowshot plugin){
        this.bs = plugin;
    }
    public User getUser(Player player){
        for(User user: users){
            if (user.getUniqueId() == player.getUniqueId()){
                return user;
            }
        }
        User user = new User(player.getUniqueId());
        users.add(user);
        return user;
    }
    public User getUser(UUID player){
        for(User user: users){
            if (user.getUniqueId() == player){
                return user;
            }
        }
        User user = new User(player);
        users.add(user);
        return user;
    }
    public List<User> getUsers(SubArena arena) {
        return arena.getPlayers();
    }
    public void removeuser(Player player){
        users.remove(player);
    }
    public void removeuser(UUID user){
        users.remove(user);
    }
}
