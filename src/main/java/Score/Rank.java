package Score;

import Manager.MessageManager;

public enum Rank {
    UNRANKED(0, "rank.unranked"),
    BRONZE(0, "rank.bronze"),
    SILVER(1200, "rank.silver"),
    GOLD(1400, "rank.gold"),
    PLATINUM(1600, "rank.platinum"),
    DIAMOND(1800, "rank.diamond"),
    MASTER(2000, "rank.master"),
    GRANDMASTER(2200, "rank.grandmaster");

    private final int minElo;
    private final String messageKey;

    Rank(int minElo, String messageKey) {
        this.minElo = minElo;
        this.messageKey = messageKey;
    }

    public int getMinElo() {
        return minElo;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public String getDisplayName(MessageManager msg) {
        return msg.get(messageKey);
    }

    public static Rank fromElo(int elo) {
        Rank result = BRONZE;
        for (Rank rank : values()) {
            if (rank == UNRANKED) continue;
            if (elo >= rank.minElo) {
                result = rank;
            }
        }
        return result;
    }
}
