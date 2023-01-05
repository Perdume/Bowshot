package Game;

import java.util.HashMap;

public class timer{
    private Game game;
    private long timer;
    private HashMap<Game, Long> times = new HashMap<>();
    public timer(Game gm){
        game = gm;
    }
    String getTime(){
        int t = (int) (System.currentTimeMillis() -getTimer());
        int alltime = 6300 - t/50;
        int tick = Math.floorMod(alltime, 20);
        int sec = Math.floorMod(alltime, 1200)/20;
        int min = alltime/1200;
        String ti = min + ":" + sec + ":"+tick;
        return ti;
    }
    public Long getTimer(){
        if (times.get(game) == null){
            times.put(game, System.currentTimeMillis());
        }
        return times.get(game);
    }
    public void resetTimer(){
        times.put(game, null);
    }
}
