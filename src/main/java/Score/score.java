package Score;

import Game.Game;
import Manager.MessageManager;
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
        int baseElo = bs.getConfigManager().getBaseElo();
        if (bs.scoremanager.getConfig().get(u.getPlayer().getUniqueId().toString()) == null){
            bs.scoremanager.getConfig().set(u.getPlayer().getUniqueId().toString(), baseElo);
            bs.scoremanager.saveconfig();
        }
        return (int) bs.scoremanager.getConfig().get(u.getPlayer().getUniqueId().toString());
    }
    public int getGamesPlayed(User u) {
        String key = u.getPlayer().getUniqueId().toString() + ".games";
        return bs.scoremanager.getConfig().getInt(key, 0);
    }
    private void incrementGamesPlayed(User u) {
        String key = u.getPlayer().getUniqueId().toString() + ".games";
        int current = bs.scoremanager.getConfig().getInt(key, 0);
        bs.scoremanager.getConfig().set(key, current + 1);
    }
    public boolean isInPlacement(User u) {
        return getGamesPlayed(u) < bs.getConfigManager().getPlacementMatches();
    }
    public Rank getRank(User u) {
        if (isInPlacement(u)) {
            return Rank.UNRANKED;
        }
        return Rank.fromElo(getscore(u));
    }
    public double getELO(User u, int Averager){
        double userscore = Double.valueOf(getscore(u));
        double temp = (((double) Averager - userscore)/400);
        return (1 / (Math.pow(10, temp) + 1));
    }
    public void addscore(User u, Player winner, int Averager) {
        MessageManager msg = bs.getMessageManager();
        int ELO;
        double placementMultiplier = isInPlacement(u) ? 1.5 : 1.0;
        if (u.getPlayer() == winner) {
            ELO = (int) Math.max(3, 30 * getELO(u, Averager) * placementMultiplier);
        }
        else{
            ELO = -(int) Math.max(3, getscore(u)/25 * getELO(u, Averager) * placementMultiplier);
        }
        int mvpscore = (int) (game.getkillscore(u.getPlayer()) * 20);
        int rating = (ELO + mvpscore)/2;
        if (rating >= 0) {
            u.getPlayer().sendMessage(msg.get("result.score-gain", "{score}", String.valueOf(rating)));
        }
        else{
            u.getPlayer().sendMessage(msg.get("result.score-loss", "{score}", String.valueOf(rating)));
        }
        bs.scoremanager.getConfig().set(u.getPlayer().getUniqueId().toString(), (int) bs.scoremanager.getConfig().get(u.getPlayer().getUniqueId().toString()) + rating);
        incrementGamesPlayed(u);
        bs.scoremanager.saveconfig();

        Rank newRank = getRank(u);
        if (newRank != Rank.UNRANKED) {
            u.getPlayer().sendMessage(newRank.getDisplayName(msg));
        }
    }
}
