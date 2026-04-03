package bowshot.stats;

public class PlayerStats {

    private int wins;
    private int losses;
    private int kills;
    private int deaths;
    private int gamesPlayed;
    private int mmr;
    private int winStreak;
    private int maxWinStreak;

    public PlayerStats(int wins, int losses, int kills, int deaths, int gamesPlayed, int mmr,
                       int winStreak, int maxWinStreak) {
        this.wins = wins;
        this.losses = losses;
        this.kills = kills;
        this.deaths = deaths;
        this.gamesPlayed = gamesPlayed;
        this.mmr = mmr;
        this.winStreak = winStreak;
        this.maxWinStreak = maxWinStreak;
    }

    public int getWins() { return wins; }
    public int getLosses() { return losses; }
    public int getKills() { return kills; }
    public int getDeaths() { return deaths; }
    public int getGamesPlayed() { return gamesPlayed; }
    public int getMmr() { return mmr; }
    public int getWinStreak() { return winStreak; }
    public int getMaxWinStreak() { return maxWinStreak; }

    public void addWin() {
        wins++;
        gamesPlayed++;
        winStreak++;
        if (winStreak > maxWinStreak) maxWinStreak = winStreak;
    }
    public void addLoss() {
        losses++;
        gamesPlayed++;
        winStreak = 0;
    }
    public void addKills(int amount) { kills += amount; }
    public void addDeaths(int amount) { deaths += amount; }
    public void setMmr(int mmr) { this.mmr = mmr; }
    public void addMmr(int amount) {
        this.mmr = Math.max(0, this.mmr + amount);
    }

    public double getKDRatio() {
        return deaths == 0 ? kills : (double) kills / deaths;
    }

    public double getWinRate() {
        return gamesPlayed == 0 ? 0 : (double) wins / gamesPlayed * 100;
    }
}
