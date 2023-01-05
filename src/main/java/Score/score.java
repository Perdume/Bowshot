package Score;

import Game.Game;
import User.User;
import bowshot.bowshot.Bowshot;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;

public class score {
    private Bowshot bs = Bowshot.getPlugin(Bowshot.class);
    private Game game;
    public score(Game game){
        this.game = game;
    }
    public int getAveragedScore(){
        int all = 0;
        int plc = 0;
        for(User u: game.GetPlayers()){
            all += getscore(u);
            plc += 1;
        }
        return all/plc;
    }
    public int getscore(User u){
        if (bs.scoremanager.getConfig().get(u.getPlayer().getUniqueId().toString()) == null){
            bs.scoremanager.getConfig().set(u.getPlayer().getUniqueId().toString(), 1500);
            bs.scoremanager.saveconfig();
        }
        return (int) bs.scoremanager.getConfig().get(u.getPlayer().getUniqueId().toString());
    }
    public double getELO(User u, int Averager){
        double userscore = Double.valueOf(getscore(u));
        double temp = (((double) Averager - userscore)/400);
        return (1 / (Math.pow(10, temp) + 1));
    }
    public void addscore(User u, Player winner, int Averager) {
        int ELO;
        if (u.getPlayer() == winner) {
            ELO = (int) Math.max(3, 30 * getELO(u, Averager));
        }
        else{
            ELO =  - (int) Math.max(3, getscore(u)/25 * getELO(u, Averager));
        }
        int mvpscore = (int) (game.getkillscore(u.getPlayer()) * 20);
        int rating = (ELO + mvpscore)/2;
        if (rating >= 0) {
            u.getPlayer().sendMessage(ChatColor.GREEN + "SCORE GET: +" + rating);
        }
        else{
            u.getPlayer().sendMessage(ChatColor.RED + "SCORE LOST: " + rating);
        }
        bs.scoremanager.getConfig().set(u.getPlayer().getUniqueId().toString(), (int) bs.scoremanager.getConfig().get(u.getPlayer().getUniqueId().toString()) + rating);
        bs.scoremanager.saveconfig();
    }
}
