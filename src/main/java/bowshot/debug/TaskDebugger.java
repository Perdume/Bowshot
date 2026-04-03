package bowshot.debug;

import bowshot.game.Game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskDebugger {

    private final List<Game> activeGames = new ArrayList<>();
    private final Map<Game, List<TaskState>> taskStates = new HashMap<>();

    public Map<Game, List<TaskState>> getTaskStates() {
        return taskStates;
    }

    public List<TaskState> getGameTasks(Game game) {
        return taskStates.get(game);
    }

    public List<Game> getActiveGames() {
        return activeGames;
    }

    public void register(Game game, List<TaskState> states) {
        activeGames.add(game);
        taskStates.put(game, states);
    }

    public void unregister(Game game) {
        activeGames.remove(game);
        taskStates.remove(game);
    }
}
