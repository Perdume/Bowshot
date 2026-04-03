package bowshot.mission;

public enum AchievementType {

    // === 킬 업적 ===
    KILL_10("첫 발자국", "First Steps", "총 10킬 달성", "Get 10 total kills", 10, 100),
    KILL_50("킬링머신", "Killing Machine", "총 50킬 달성", "Get 50 total kills", 50, 300),
    KILL_100("학살자", "Slayer", "총 100킬 달성", "Get 100 total kills", 100, 500),
    KILL_500("전설의 사냥꾼", "Legendary Hunter", "총 500킬 달성", "Get 500 total kills", 500, 1000),
    KILL_1000("죽음의 신", "God of Death", "총 1000킬 달성", "Get 1000 total kills", 1000, 2000),

    // === 승리 업적 ===
    WIN_1("첫 승리", "First Victory", "첫 승리", "Win your first game", 1, 50),
    WIN_10("10승 달성", "10 Victories", "10승 달성", "Win 10 games", 10, 200),
    WIN_50("50승 달성", "50 Victories", "50승 달성", "Win 50 games", 50, 500),
    WIN_100("100승 달성", "Centurion", "100승 달성", "Win 100 games", 100, 1000),
    WIN_500("무적의 전사", "Invincible Warrior", "500승 달성", "Win 500 games", 500, 3000),

    // === 게임 플레이 업적 ===
    GAMES_10("입문자", "Beginner", "10판 플레이", "Play 10 games", 10, 50),
    GAMES_50("단골손님", "Regular", "50판 플레이", "Play 50 games", 50, 150),
    GAMES_100("중독자", "Addict", "100판 플레이", "Play 100 games", 100, 300),
    GAMES_500("만렙", "Veteran", "500판 플레이", "Play 500 games", 500, 800),
    GAMES_1000("평생회원", "Lifetime Member", "1000판 플레이", "Play 1000 games", 1000, 2000),

    // === 점수 업적 ===
    SCORE_100("브론즈", "Bronze", "점수 100 달성", "Reach 100 score", 100, 50),
    SCORE_300("실버", "Silver", "점수 300 달성", "Reach 300 score", 300, 150),
    SCORE_500("골드", "Gold", "점수 500 달성", "Reach 500 score", 500, 300),
    SCORE_1000("다이아몬드", "Diamond", "점수 1000 달성", "Reach 1000 score", 1000, 700),
    SCORE_2000("마스터", "Master", "점수 2000 달성", "Reach 2000 score", 2000, 1500),

    // === 특수 업적 ===
    MVP_1("첫 MVP", "First MVP", "첫 MVP", "Become MVP for the first time", 1, 80),
    MVP_10("MVP 마니아", "MVP Maniac", "10회 MVP", "Become MVP 10 times", 10, 300),
    MVP_50("스타 플레이어", "Star Player", "50회 MVP", "Become MVP 50 times", 50, 800),
    STREAK_3("3연승", "3 Win Streak", "3연승 달성", "Win 3 games in a row", 3, 150),
    STREAK_5("5연승", "5 Win Streak", "5연승 달성", "Win 5 games in a row", 5, 400),
    STREAK_10("10연승", "10 Win Streak", "10연승 달성", "Win 10 games in a row", 10, 1000),
    PERFECT_WIN("완벽한 승리", "Perfect Victory", "무피해 1등", "Win 1st place without dying", 1, 200),
    KILL_SPREE_5("5킬 스프리", "5 Kill Spree", "한 게임 5킬", "Get 5 kills in a single game", 5, 200),
    KILL_SPREE_8("8킬 스프리", "8 Kill Spree", "한 게임 8킬", "Get 8 kills in a single game", 8, 500);

    private final String nameKo;
    private final String nameEn;
    private final String descKo;
    private final String descEn;
    private final int goal;
    private final int reward;

    AchievementType(String nameKo, String nameEn, String descKo, String descEn, int goal, int reward) {
        this.nameKo = nameKo;
        this.nameEn = nameEn;
        this.descKo = descKo;
        this.descEn = descEn;
        this.goal = goal;
        this.reward = reward;
    }

    public String getNameKo() { return nameKo; }
    public String getNameEn() { return nameEn; }
    public String getDescKo() { return descKo; }
    public String getDescEn() { return descEn; }
    public int getGoal() { return goal; }
    public int getReward() { return reward; }

    public String getName(String lang) {
        return "ko".equalsIgnoreCase(lang) ? nameKo : nameEn;
    }

    public String getDesc(String lang) {
        return "ko".equalsIgnoreCase(lang) ? descKo : descEn;
    }

    public enum Category {
        KILL, WIN, GAMES, SCORE, SPECIAL
    }

    public Category getCategory() {
        String n = name();
        if (n.startsWith("KILL_") && !n.startsWith("KILL_SPREE")) return Category.KILL;
        if (n.startsWith("WIN_")) return Category.WIN;
        if (n.startsWith("GAMES_")) return Category.GAMES;
        if (n.startsWith("SCORE_")) return Category.SCORE;
        return Category.SPECIAL;
    }
}
