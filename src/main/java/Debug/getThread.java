package Debug;

import Game.Game;
import bowshot.bowshot.Bowshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class getThread {
    private Bowshot bow;
    private List<Game> GameList = new ArrayList<>();
    private List<ThreadState> thrt = new ArrayList<>();
    private HashMap<Game, List<ThreadState>> ThreadList = new HashMap<>();
    public getThread(Bowshot main){
        this.bow = main;
    }
    public HashMap<Game, List<ThreadState>> getThreads(){
        return ThreadList;
    }
    public List<ThreadState> getGameThread(Game game){
        return ThreadList.get(game);
    }
    public List<Game> getGames(){
        return GameList;
    }

}
