package bowshot.score;

import java.util.List;

import org.bukkit.entity.Player;

import bowshot.Bowshot;
import bowshot.debug.DebugLogger;
import bowshot.debug.ErrorCode;
import bowshot.game.Game;
import bowshot.lang.MessageKey;
import bowshot.player.GamePlayer;
import bowshot.stats.PlayerStats;

public class MmrCalculator {

    private final Bowshot plugin;
    private final Game game;

    public MmrCalculator(Bowshot plugin, Game game) {
        this.plugin = plugin;
        this.game = game;
    }

    /**
     * 배틀로얄 방식 점수 계산 및 적용.
     *
     * @param player       대상 플레이어
     * @param placement    등수 (1 = 1등)
     * @param totalPlayers 전체 인원
     * @param isTimeout    시간초과 종료 여부
     * @param isDisconnect 접속끊김 여부
     */
    public void calculateAndApply(GamePlayer player, int placement, int totalPlayers,
                                   boolean isTimeout, boolean isDisconnect) {
        int currentScore = plugin.getStatsManager().getMmr(player.getUniqueId());
        PlayerStats stats = plugin.getStatsManager().getStats(player.getUniqueId());
        int kills = game.getKillCount(player.getPlayer());

        // === 1. 배치 점수 ===
        double placementPts = calculatePlacementScore(placement, totalPlayers);

        // === 2. 킬 점수 (킬 퀄리티 반영) ===
        double killPts = calculateKillScore(player.getPlayer(), currentScore);

        // === 3. 보너스/패널티 ===
        double bonusPts = 0;

        // MVP 보너스 (최다킬, 2킬 이상)
        Player mvp = findGameMvp();
        if (mvp != null && player.getPlayer().equals(mvp) && kills >= 2) {
            bonusPts += plugin.getSettings().getMvpBonus();
        }

        // 접속끊김 패널티: 꼴등 처리 + 추가 감점
        if (isDisconnect) {
            bonusPts -= plugin.getSettings().getDisconnectPenalty();
        }

        // === 4. 존버 패널티 (0킬 + 하위 50%) ===
        double passiveMultiplier = 1.0;
        if (kills == 0 && totalPlayers >= 3 && placement > totalPlayers / 2) {
            passiveMultiplier = plugin.getSettings().getPassivePenalty();
            placementPts *= passiveMultiplier;
        }

        // === 5. 소규모 게임 배율 ===
        double sizeMultiplier = 1.0;
        if (totalPlayers <= 3) {
            sizeMultiplier = plugin.getSettings().getSmallGameFactor();
        } else if (totalPlayers >= 6) {
            sizeMultiplier = 1.0 + (totalPlayers - 5) * 0.1; // 6인=1.1, 7인=1.2, 8인=1.3
        }

        // === 6. 시간초과 배율 (생존자만 보상 감소) ===
        double timeoutMult = 1.0;
        if (isTimeout && !player.isSpectator()) {
            timeoutMult = plugin.getSettings().getTimeoutFactor();
        }

        // === 7. 총 획득량 계산 ===
        double rawGain = (placementPts + killPts + bonusPts) * sizeMultiplier * timeoutMult;

        // === 8. 진입 비용 차감 ===
        int entryCost = getEntryCost(currentScore);
        double rawChange = rawGain - entryCost;

        // === 9. 고점수 추가 감소 (진입비용 외 추가 스케일링) ===
        int finalChange;
        if (rawChange > 0) {
            double dimFactor = getDiminishingFactor(currentScore);
            finalChange = (int) Math.max(1, rawChange * dimFactor);
        } else if (rawChange < 0) {
            // 뉴비 보호: 점수 낮으면 손실 감소
            if (currentScore < plugin.getSettings().getNewbieThreshold()) {
                rawChange *= plugin.getSettings().getNewbieLossReduction();
            }
            finalChange = (int) Math.min(-1, rawChange);
            // 현재 점수 이상 잃지 않음
            finalChange = Math.max(-currentScore, finalChange);
        } else {
            finalChange = 0;
        }

        // === 10. 스탯 업데이트 ===
        if (placement == 1) {
            stats.addWin();
        } else {
            stats.addLoss();
        }
        if (kills > 0) stats.addKills(kills);
        if (player.isSpectator()) stats.addDeaths(1);

        int prevScore = stats.getMmr();
        stats.addMmr(finalChange);

        if (prevScore + finalChange < 0) {
            DebugLogger.report(ErrorCode.SCORE_NEGATIVE_RESULT,
                    player.getPlayer().getName() + " prev=" + prevScore + " change=" + finalChange);
        }

        try {
            plugin.getStatsManager().saveStats(player.getUniqueId(), stats);
        } catch (Exception e) {
            DebugLogger.report(ErrorCode.SCORE_SAVE_FAIL,
                    player.getPlayer().getName() + ": " + e.getMessage());
        }

        // 점수 변동 메시지
        if (player.getPlayer().isOnline()) {
            String placementMsg = placement + "/" + totalPlayers;
            if (finalChange >= 0) {
                player.getPlayer().sendMessage(plugin.getMessageManager().get(
                        MessageKey.SCORE_GAINED, "+" + finalChange));
            } else {
                player.getPlayer().sendMessage(plugin.getMessageManager().get(
                        MessageKey.SCORE_LOST, String.valueOf(finalChange)));
            }
        }

        DebugLogger.log("Score", player.getPlayer().getName()
                + " #" + placement + "/" + totalPlayers
                + " kills=" + kills
                + " place=" + String.format("%.1f", placementPts)
                + " kill=" + String.format("%.1f", killPts)
                + " bonus=" + String.format("%.1f", bonusPts)
                + " entry=" + entryCost
                + " raw=" + String.format("%.1f", rawChange)
                + " final=" + finalChange
                + " score=" + prevScore + "→" + stats.getMmr());
    }

    // === 배치 점수 ===

    private double calculatePlacementScore(int placement, int totalPlayers) {
        if (totalPlayers <= 1) return 0;
        int base = plugin.getSettings().getPlacementBase();
        // 1등: +base, 꼴등: 0
        return base * (double) (totalPlayers - placement) / (totalPlayers - 1);
    }

    // === 킬 점수 (킬 퀄리티 반영) ===

    private double calculateKillScore(Player killer, int killerScore) {
        List<Player> victims = game.getKillVictims(killer);
        if (victims.isEmpty()) return 0;

        double total = 0;
        int killBase = plugin.getSettings().getKillBase();

        for (Player victim : victims) {
            int victimScore = game.getStartingScore(victim.getUniqueId());
            double multiplier;

            if (victimScore > killerScore + 100) {
                // 상위 점수 상대 처치: 보너스
                multiplier = 1.5;
            } else if (victimScore < killerScore - 200) {
                // 하위 점수 상대 처치: 감소
                multiplier = 0.7;
            } else {
                // 비슷한 점수: 기본
                multiplier = 1.0;
            }

            total += killBase * multiplier;
        }

        return total;
    }

    // === 진입 비용 (현재 점수 기반 티어 시스템) ===

    private int getEntryCost(int currentScore) {
        // 높은 점수일수록 진입 비용 증가 → 상위 랭크 유지가 어려움
        int entryRate = plugin.getSettings().getEntryRate();
        return currentScore * entryRate / 100;
        // 0점=0, 100점=3, 300점=9, 600점=18, 1000점=30, 1500점=45
    }

    // === 고점수 추가 감소 ===

    private double getDiminishingFactor(int currentScore) {
        int gainScaling = plugin.getSettings().getGainScaling();
        if (gainScaling <= 0) return 1.0;
        int threshold = 500;
        if (currentScore <= threshold) return 1.0;
        return (double) gainScaling / (gainScaling + (currentScore - threshold));
        // 500점 이하: 1.0, 800점: 0.625, 1000점: 0.5, 1500점: 0.33
    }

    // === MVP 찾기 ===

    private Player findGameMvp() {
        double maxScore = 0.0;
        Player mvp = null;
        for (GamePlayer gp : game.getPlayers()) {
            double score = game.getKillScore(gp.getPlayer());
            if (score > maxScore) {
                maxScore = score;
                mvp = gp.getPlayer();
            }
        }
        return mvp;
    }
}
