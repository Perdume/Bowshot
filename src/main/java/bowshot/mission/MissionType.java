package bowshot.mission;

public enum MissionType {

    // === 일일 미션 ===
    DAILY_PLAY_GAMES("게임 참여", "Play games", MissionPeriod.DAILY, 3, 50),
    DAILY_WIN("승리하기", "Win a game", MissionPeriod.DAILY, 1, 80),
    DAILY_KILL_3("3킬 달성", "Get 3 kills", MissionPeriod.DAILY, 3, 40),
    DAILY_KILL_5("5킬 달성", "Get 5 kills", MissionPeriod.DAILY, 5, 70),
    DAILY_FIRST_PLACE("1등 하기", "Get 1st place", MissionPeriod.DAILY, 1, 100),
    DAILY_TOP3("3등 이내", "Finish top 3", MissionPeriod.DAILY, 1, 60),
    DAILY_MVP("MVP 달성", "Become MVP", MissionPeriod.DAILY, 1, 90),
    DAILY_NO_DEATH_WIN("무피해 승리", "Win without dying", MissionPeriod.DAILY, 1, 120),
    DAILY_BOW_KILL_3("활 킬 3회", "Get 3 bow kills", MissionPeriod.DAILY, 3, 50),
    DAILY_MELEE_KILL("근접 킬", "Get a melee kill", MissionPeriod.DAILY, 1, 40),

    // === 주간 미션 ===
    WEEKLY_PLAY_GAMES("게임 15회 참여", "Play 15 games", MissionPeriod.WEEKLY, 15, 200),
    WEEKLY_WIN_5("5승", "Win 5 games", MissionPeriod.WEEKLY, 5, 300),
    WEEKLY_WIN_10("10승", "Win 10 games", MissionPeriod.WEEKLY, 10, 500),
    WEEKLY_KILL_30("30킬", "Get 30 kills", MissionPeriod.WEEKLY, 30, 250),
    WEEKLY_KILL_50("50킬", "Get 50 kills", MissionPeriod.WEEKLY, 50, 400),
    WEEKLY_FIRST_PLACE_3("1등 3회", "Get 1st place 3 times", MissionPeriod.WEEKLY, 3, 350),
    WEEKLY_MVP_3("MVP 3회", "Become MVP 3 times", MissionPeriod.WEEKLY, 3, 350),
    WEEKLY_TOP3_5("3등 이내 5회", "Finish top 3, 5 times", MissionPeriod.WEEKLY, 5, 250),
    WEEKLY_BOW_KILL_20("활 킬 20회", "Get 20 bow kills", MissionPeriod.WEEKLY, 20, 300),
    WEEKLY_STREAK_3("3연승", "Win 3 games in a row", MissionPeriod.WEEKLY, 3, 500);

    private final String displayKo;
    private final String displayEn;
    private final MissionPeriod period;
    private final int goal;
    private final int reward;

    MissionType(String displayKo, String displayEn, MissionPeriod period, int goal, int reward) {
        this.displayKo = displayKo;
        this.displayEn = displayEn;
        this.period = period;
        this.goal = goal;
        this.reward = reward;
    }

    public String getDisplayKo() { return displayKo; }
    public String getDisplayEn() { return displayEn; }
    public MissionPeriod getPeriod() { return period; }
    public int getGoal() { return goal; }
    public int getReward() { return reward; }

    public String getDisplay(String lang) {
        return "ko".equalsIgnoreCase(lang) ? displayKo : displayEn;
    }
}
