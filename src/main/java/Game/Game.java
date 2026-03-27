package Game;

import Area.SubArena;
import Debug.ThreadState;
import Manager.MessageManager;
import Score.Rank;
import Score.score;
import User.User;
import bowshot.bowshot.Bowshot;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.*;

import static Debug.ThreadState.RUNNING;
import static Debug.ThreadState.STOPPED;

public class Game {
    private SubArena aaa;
    private Bowshot bs = Bowshot.getPlugin(Bowshot.class);
    public Boolean end = false;
    private Game inc;
    private HashMap<Player, Integer> kill = new HashMap<>();
    private HashMap<Player, Double> killrating = new HashMap<>();
    private timer tim = new timer(this);
    private ScoreboardManager manager = Bukkit.getScoreboardManager();
    private Scoreboard board = manager.getNewScoreboard();
    private List<ThreadState> thrt = new ArrayList<>(Arrays.asList(RUNNING, RUNNING)); // scoreboard, TimeEvent
    private score sc;
    private Integer AverageScore;


    public Game(SubArena area){
        this.aaa = area;
        this.sc = new score(this);
    }
    private void endo(){
        int remainplayer = aaa.getPlayers().size() - aaa.getSpectators().size();
        if (remainplayer <= 1) {
            if (!end) {
                end();
            }
        }
    }

    public HashMap<Player, Integer> killlist(){
        return kill;
    }
    public void addkilllist(Player p){
        if (kill.get(p) == null){
            kill.put(p, 0);
        }
        kill.put(p, kill.get(p) + 1);
    }
    public HashMap<Player, Double> killscore(){
        return killrating;
    }
    public Double getkillscore(Player p){
        if (killrating.get(p) == null){
            killrating.put(p, 0.0);
        }
        return killrating.get(p);
    }
    public void start(List<Player> li){
        inc = this;
        bs.gamemanager.getGames().add(this);
        for (Player p: li){
            aaa.join(p);
        }
        tim.getTimer();
        end = false;
        AverageScore = sc.getAveragedScore();
        MessageManager msg = bs.getMessageManager();
        for (User p: aaa.getPlayers()){
            p.getPlayer().setHealth(20);
            p.getPlayer().setFoodLevel(20);
            p.getPlayer().getInventory().clear();
            p.setSpectator(false);
            p.getPlayer().teleport(aaa.getSpawnLocation());
            p.getPlayer().setGameMode(GameMode.ADVENTURE);
            createScoreboard(p.getPlayer());
            for (PotionEffect effect : p.getPlayer().getActivePotionEffects()) {
                p.getPlayer().removePotionEffect(effect.getType());
            }
            p.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 10*20, 2, true));
            int playerScore = sc.getscore(p);
            Rank playerRank = sc.getRank(p);
            p.getPlayer().sendMessage(ChatColor.GREEN + "SCORE: " + playerScore + " " + playerRank.getDisplayName(msg));
            int gamesPlayed = sc.getGamesPlayed(p);
            int placementTotal = bs.getConfigManager().getPlacementMatches();
            if (gamesPlayed < placementTotal) {
                p.getPlayer().sendMessage(msg.get("rank.placement", "{current}", String.valueOf(gamesPlayed), "{total}", String.valueOf(placementTotal)));
            }
            bs.sc.jointeam(p.getPlayer(), board);

        }
        start1();
    }
    private void start1(){
        MessageManager msg = bs.getMessageManager();
        bs.gth.getGames().add(this);
        bs.gth.getThreads().put(this, thrt);
        broadcast(msg.get("game.start-countdown", "{seconds}", String.valueOf(bs.getConfigManager().getPrepTime())));
        for (User u1: aaa.getPlayers()){
            for (User u2: aaa.getPlayers()){
                if (u1 != u2){
                    u1.getPlayer().setCollidable(true);
                    u1.getPlayer().hidePlayer(u2.getPlayer());
                }
            }
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                if (end){
                    thrt.set(1, STOPPED);
                    this.cancel();
                }
                TimeEvent();

            }

        }.runTaskTimer(bs, 1L, 1L);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (end){
                    thrt.set(0, STOPPED);
                    this.cancel();
                }
                for(User p : aaa.getPlayers()) {
                    Player player = p.getPlayer();
                    updateScoreboard(player);
                }

            }

        }.runTaskTimer(bs, 4L, 4L);
        new BukkitRunnable() {
            int i = bs.getConfigManager().getPrepTime() + 1;
            @Override
            public void run() {
                i--;
                if (end){
                    broadcast(msg.get("game.force-ended"));
                    cancel();
                }
                if (i<6&&i>0){
                    String str = ChatColor.RED + String.valueOf(i);
                    broadcast(str);
                }
                if (i == 0) {
                    broadcast(msg.get("game.start"));
                    for (User u1: aaa.getPlayers()){
                        for (User u2: aaa.getPlayers()){
                            if (u1 != u2){
                                if (!u1.isSpectator()&&!u2.isSpectator()) {
                                    u1.getPlayer().showPlayer(u2.getPlayer());
                                }
                            }
                        }
                    }
                    for (User p: aaa.getPlayers()){
                        if (!p.isSpectator()) {
                            p.getPlayer().getInventory().addItem(new ItemStack(Material.ARROW, 64));
                            p.getPlayer().getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 64));
                            p.getPlayer().getInventory().addItem(bow());
                            p.getPlayer().getInventory().addItem(sw());
                        }
                    }
                    cancel();
                }
            }
        }.runTaskTimer(bs, 0L, 20L);
    }

    public void end(){
        end = true;
        Player winner = null;
        for (User p: aaa.getPlayers()){
            p.getPlayer().teleport(aaa.getSpawnLocation());
            if (!p.isSpectator()){
                winner = p.getPlayer();
            }
        }
        finishGame(winner);
    }

    public void Customend(Player win){
        end = true;
        finishGame(win);
    }

    private void finishGame(Player winner) {
        MessageManager msg = bs.getMessageManager();
        for (User u1: aaa.getPlayers()){
            for (User u2: aaa.getPlayers()){
                if (u1 != u2){
                    bs.sc.leaveteam(u1.getPlayer(), board);
                    u1.getPlayer().showPlayer(u2.getPlayer());
                    u1.getPlayer().setAllowFlight(false);
                    for (PotionEffect effect : u1.getPlayer().getActivePotionEffects()) {
                        u1.getPlayer().removePotionEffect(effect.getType());
                    }
                }
            }
        }
        for (User p: aaa.getPlayers()) {
            p.getPlayer().sendMessage(sendCenteredMessage.getCenteredMessage(msg.get("result.game-set")));
            if (winner != null) {
                p.getPlayer().sendMessage(sendCenteredMessage.getCenteredMessage(msg.get("result.winner", "{name}", winner.getName())));
            } else {
                p.getPlayer().sendMessage(sendCenteredMessage.getCenteredMessage(msg.get("result.no-winner")));
            }
            if (getkill() != null) {
                p.getPlayer().sendMessage(sendCenteredMessage.getCenteredMessage(msg.get("result.kill", "{name}", getkill().getName())));
            } else {
                p.getPlayer().sendMessage(sendCenteredMessage.getCenteredMessage(msg.get("result.no-kill")));
            }
            p.getPlayer().setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            p.getPlayer().sendMessage(sendCenteredMessage.getCenteredMessage(msg.get("result.game-set")));
            p.getPlayer().getInventory().clear();
            tim.resetTimer();
            p.getPlayer().sendMessage(msg.get("game.returning"));
            sc.addscore(p, winner, AverageScore);
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask(bs, new Runnable() {
            @Override
            public void run() {
                for (Player pl: aaa.getSpawnLocation().getWorld().getPlayers()){
                    User p = bs.usermanager.getUser(pl);
                    p.setSpectator(false);
                    bs.usermanager.removeuser(p.getPlayer());
                }
                aaa.ClearPlayers();
                bs.gamemanager.removegame(getArena());
                bs.gamemanager.getGames().remove(inc);
                bs.vw.remove(Bukkit.getWorld(aaa.getArenaName()));
            }
        }, 60);
    }

    private void updateScoreboard(Player player) {
        MessageManager msg = bs.getMessageManager();
        User usr = bs.usermanager.getUser(player);
        int remainplayer = usr.getArena().getPlayers().size() - usr.getArena().getSpectators().size();
        String rpKey = msg.get("scoreboard.remaining-players");
        String rtKey = msg.get("scoreboard.remaining-time");
        bs.sc.updatePerLine(board, rpKey, rpKey + remainplayer, 3);
        bs.sc.updatePerLine(board, rtKey, rtKey + tim.getTime(), 2);
        endo();
    }

    public List<User> GetPlayers(){
        return aaa.getPlayers();
    }

    private void createScoreboard(Player player) {
        MessageManager msg = bs.getMessageManager();
        User usr = bs.usermanager.getUser(player);
        int remainplayer = usr.getArena().getPlayers().size() - usr.getArena().getSpectators().size();
        String separator = msg.get("scoreboard.separator");
        bs.sc.SetTitle(msg.get("scoreboard.title"), player.getWorld().getName(), board);
        bs.sc.SetScore(4, separator, player.getWorld().getName(), board);
        bs.sc.SetScore(3, msg.get("scoreboard.remaining-players") + remainplayer, player.getWorld().getName(), board);
        bs.sc.SetScore(2, msg.get("scoreboard.remaining-time") + tim.getTime(), player.getWorld().getName(), board);
        bs.sc.SetScore(1, separator, player.getWorld().getName(), board);
    }


    private void TimeEvent(){
        MessageManager msg = bs.getMessageManager();
        int t = (int) (System.currentTimeMillis() - tim.getTimer());
        int alltime = 6300 - t/50;
        if (alltime == 3600){
            broadcast(msg.get("game.glow-warning", "{minutes}", "3"));
            for (User u: aaa.getPlayers()){
                if (!u.isSpectator()){
                    u.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 120000, 2, false));
                }
            }
        }
        if (alltime == 0){
            broadcast(msg.get("game.time-up"));
            Customend(getkill());
        }
    }
    private Player getkill(){
        Double max = 0.0;
        Player p = null;
        for (User u: aaa.getPlayers()){
            if (killrating.get(u.getPlayer()) != null){
                if (killrating.get(u.getPlayer()) > max){
                    max = killrating.get(u.getPlayer());
                    p = u.getPlayer();
                }
            }
        }
        if (max == 0){
            return null;
        }
        else{
            return p;
        }
    }


    public void broadcast(String str){
        for (User p: aaa.getPlayers()){
            p.getPlayer().sendMessage(str);
        }
    }
    private ItemStack bow(){
        ItemStack bow = new ItemStack(Material.BOW);
        bow.addUnsafeEnchantment(Enchantment.ARROW_DAMAGE, 6);
        return bow;
    }
    private ItemStack sw(){
        ItemStack sw = new ItemStack(Material.WOODEN_SWORD);
        sw.addEnchantment(Enchantment.KNOCKBACK, 1);
        return sw;
    }

    //THIS IS STATUS ZONE!//
    public Boolean isPlayerisPlaying(Player p){
        for (User u: aaa.getPlayers()){
            if (u.getPlayer() == p){
                return true;
            }
        }
        return false;
    }
    public SubArena getArena(){
        return aaa;
    }
}
