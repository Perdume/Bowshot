package Game;

import Area.SubArena;
import Debug.ThreadState;
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
            p.getPlayer().sendMessage(ChatColor.GREEN + "SCORE: " + sc.getscore(p));
            bs.sc.jointeam(p.getPlayer(), board);

        }
        start1();
    }
    private void start1(){
        bs.gth.getGames().add(this);
        bs.gth.getThreads().put(this, thrt);
        broadcast("15초후 게임이 시작됩니다 준비해주세요");
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
            int i = 16;
            @Override
            public void run() {
                i--;
                if (end){
                    Bukkit.broadcastMessage("게임이 강제로 종료되었습니다");
                    cancel();
                }
                if (i<6&&i>0){
                    String str = ChatColor.RED + String.valueOf(i);
                    broadcast(str);
                }
                if (i == 0) {
                    String str = ChatColor.GREEN + "Start";
                    broadcast(str);
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
            p.getPlayer().sendMessage(sendCenteredMessage.getCenteredMessage("&e==========GAME SET=========="));
            p.getPlayer().sendMessage(sendCenteredMessage.getCenteredMessage("winner:" + winner.getName()));
            if (getkill() != null) {
                p.getPlayer().sendMessage(sendCenteredMessage.getCenteredMessage("&6kill:" + getkill().getName()));
            }
            else{
                p.getPlayer().sendMessage(sendCenteredMessage.getCenteredMessage("&6kill: X"));
            }
            p.getPlayer().setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            p.getPlayer().sendMessage(sendCenteredMessage.getCenteredMessage("&e============================"));
            p.getPlayer().getInventory().clear();
            tim.resetTimer();
            p.getPlayer().sendMessage("3초뒤 되돌아갑니다...");
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
        User usr = bs.usermanager.getUser(player);
        int remainplayer = usr.getArena().getPlayers().size() - usr.getArena().getSpectators().size();
        bs.sc.updatePerLine(board,"&e남은인원&f: ", "&e남은인원&f: " + remainplayer, 3);
        bs.sc.updatePerLine(board,"&e남은시간&f: ", "&e남은시간&f: " + tim.getTime(), 2);
        endo();
    }

    public List<User> GetPlayers(){
        return aaa.getPlayers();
    }

    private void createScoreboard(Player player) {
        User usr = bs.usermanager.getUser(player);
        int remainplayer = usr.getArena().getPlayers().size() - usr.getArena().getSpectators().size();
        bs.sc.SetTitle("&6&oBOWSHOT", player.getWorld().getName(), board);
        bs.sc.SetScore(4, "&7&m-------------------------", player.getWorld().getName(), board);
        bs.sc.SetScore(3, "&e남은인원&f: " + remainplayer, player.getWorld().getName(), board);
        bs.sc.SetScore(2, "&e남은시간&f: " + tim.getTime(), player.getWorld().getName(), board);
        bs.sc.SetScore(1, "&7&m-------------------------", player.getWorld().getName(), board);
    }


    private void TimeEvent(){
        int t = (int) (System.currentTimeMillis() - tim.getTimer());
        int alltime = 6300 - t/50;
        if (alltime == 3600){
            broadcast(ChatColor.RED + "3분 남았습니다, 빠른 진행을 위해 발광을 켭니다");
            for (User u: aaa.getPlayers()){
                if (!u.isSpectator()){
                    u.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 120000, 2, false));
                }
            }
        }
        if (alltime == 0){
            broadcast(ChatColor.RED + "시간 초과로 최대 킬을 한 사람이 우승합니다");
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
    public void Customend(Player win){
        end = true;
        Player winner = win;
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
            if (win != null){
                p.getPlayer().sendMessage(ChatColor.GREEN + "winner: X");
                p.getPlayer().sendMessage(ChatColor.GOLD + "kill:" + winner.getName());
            }
            else{
                p.getPlayer().sendMessage(ChatColor.GREEN + "winner: X");
                p.getPlayer().sendMessage(ChatColor.GOLD + "kill: X");
            }
            p.getPlayer().setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            p.getPlayer().getInventory().clear();
            tim.resetTimer();
            p.getPlayer().sendMessage("3초뒤 되돌아갑니다...");
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
