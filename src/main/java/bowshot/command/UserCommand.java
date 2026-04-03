package bowshot.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

import bowshot.Bowshot;
import bowshot.cosmetic.TitleEffectManager;
import bowshot.debug.DebugLogger;
import bowshot.lang.MessageKey;
import bowshot.mission.AchievementManager;
import bowshot.mission.AchievementType;
import bowshot.mission.MissionManager;
import bowshot.mission.MissionType;

public class UserCommand implements CommandExecutor {

    private final Bowshot plugin;

    public UserCommand(Bowshot plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        DebugLogger.log("Command", "/bs executed by " + player.getName() + " args=" + java.util.Arrays.toString(args));

        if (plugin.getGameManager().isPlayerPlaying(player)) {
            DebugLogger.log("Command", player.getName() + " blocked - currently playing");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(plugin.getMessageManager().get(MessageKey.PLUGIN_INFO));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "join":
                handleJoin(player);
                break;
            case "leave":
                handleLeave(player);
                break;
            case "visit":
                plugin.getVisitArenaGui().openInventory(player);
                break;
            case "spect":
                plugin.getJoinSpectatorGui().openInventory((HumanEntity) player);
                break;
            case "stats":
                showStats(player);
                break;
            case "mission":
                handleMission(player, args);
                break;
            case "achievement":
                handleAchievement(player, args);
                break;
            case "title":
                handleTitleCommand(player, args);
                break;
            case "effect":
                handleEffectCommand(player, args);
                break;
            case "replay":
                handleReplay(player, args);
                break;
            default:
                player.sendMessage(plugin.getMessageManager().get(MessageKey.PLUGIN_INFO));
                break;
        }
        return true;
    }

    private void handleJoin(Player player) {
        plugin.getMatchQueue().join(player);
        int queueSize = plugin.getMatchQueue().getQueueSize();
        Bukkit.broadcastMessage(plugin.getMessageManager().get(MessageKey.MATCH_PLAYER_COUNT, queueSize));
    }

    private void handleLeave(Player player) {
        // Check if in match queue
        if (plugin.getMatchQueue().isInQueue(player)) {
            plugin.getMatchQueue().leave(player);
            return;
        }

        // Check if in an instance world (VISIT or EDIT)
        org.bukkit.World world = player.getWorld();
        if (plugin.getInstanceWorldManager().isInstanceWorld(world)) {
            bowshot.world.InstanceType type = plugin.getInstanceWorldManager().getInstanceType(world);
            if (type == bowshot.world.InstanceType.VISIT || type == bowshot.world.InstanceType.EDIT) {
                // Clean up edit mapping if EDIT
                if (type == bowshot.world.InstanceType.EDIT) {
                    plugin.getWorldEditorGui().getEditWorldMapping().remove(world.getName());
                }
                org.bukkit.Location lobbyLoc = plugin.getSpawnManager().getLobbyLocation();
                if (lobbyLoc != null) {
                    player.teleport(lobbyLoc);
                }
                player.sendMessage(plugin.getMessageManager().get(MessageKey.VISIT_LEFT));

                org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (world.getPlayers().isEmpty()) {
                        plugin.getInstanceWorldManager().removeWorld(world);
                    }
                }, 5L);
                return;
            }
        }

        // Check if viewing replay
        if (plugin.getReplayManager().isViewing(player)) {
            plugin.getReplayManager().stopViewing(player);
            player.sendMessage(plugin.getMessageManager().get(MessageKey.REPLAY_STOPPED));
            return;
        }

        player.sendMessage(plugin.getMessageManager().get(MessageKey.MATCH_NOT_IN));
    }

    private void handleReplay(Player player, String[] args) {
        if (!plugin.getSettings().isReplayEnabled()) {
            player.sendMessage(plugin.getMessageManager().get(MessageKey.PLUGIN_INFO));
            return;
        }

        // /bs replay stop
        if (args.length >= 2 && args[1].equalsIgnoreCase("stop")) {
            if (plugin.getReplayManager().isViewing(player)) {
                plugin.getReplayManager().stopViewing(player);
                player.sendMessage(plugin.getMessageManager().get(MessageKey.REPLAY_STOPPED));
            }
            return;
        }

        // /bs replay - open GUI
        if (plugin.getReplayManager().isViewing(player)) {
            player.sendMessage(plugin.getMessageManager().get(MessageKey.REPLAY_STOPPED));
            plugin.getReplayManager().stopViewing(player);
            return;
        }

        java.util.List<String> replays = plugin.getReplayManager().listReplays();
        if (replays.isEmpty()) {
            player.sendMessage(plugin.getMessageManager().get(MessageKey.REPLAY_LIST_EMPTY));
            return;
        }

        plugin.getReplayGui().openInventory(player);
    }

    private void showStats(Player player) {
        bowshot.stats.PlayerStats stats = plugin.getStatsManager().getStats(player.getUniqueId());
        player.sendMessage(plugin.getMessageManager().get(MessageKey.STATS_HEADER));
        player.sendMessage(plugin.getMessageManager().get(MessageKey.STATS_WINS, stats.getWins()));
        player.sendMessage(plugin.getMessageManager().get(MessageKey.STATS_LOSSES, stats.getLosses()));
        player.sendMessage(plugin.getMessageManager().get(MessageKey.STATS_KILLS, stats.getKills()));
        player.sendMessage(plugin.getMessageManager().get(MessageKey.STATS_DEATHS, stats.getDeaths()));
        player.sendMessage(plugin.getMessageManager().get(MessageKey.STATS_GAMES_PLAYED, stats.getGamesPlayed()));
        if (player.hasPermission("bowshot.admin")) {
            player.sendMessage(plugin.getMessageManager().get(MessageKey.STATS_MMR, stats.getMmr()));
        }
        player.sendMessage(plugin.getMessageManager().get(MessageKey.STATS_FOOTER));
    }

    private void handleMission(Player player, String[] args) {
        MissionManager mm = plugin.getMissionManager();
        java.util.UUID uuid = player.getUniqueId();
        String lang = plugin.getSettings().getLanguage();

        // /bs mission claim <type>
        if (args.length >= 3 && args[1].equalsIgnoreCase("claim")) {
            MissionType type;
            try {
                type = MissionType.valueOf(args[2].toUpperCase());
            } catch (IllegalArgumentException e) {
                player.sendMessage(plugin.getMessageManager().get(MessageKey.MISSION_NOT_FOUND));
                return;
            }
            if (mm.isClaimed(uuid, type)) {
                player.sendMessage(plugin.getMessageManager().get(MessageKey.MISSION_ALREADY_CLAIMED));
                return;
            }
            if (mm.getProgress(uuid, type) < type.getGoal()) {
                player.sendMessage(plugin.getMessageManager().get(MessageKey.MISSION_NOT_DONE));
                return;
            }
            mm.claimReward(uuid, type);
            player.sendMessage(plugin.getMessageManager().get(MessageKey.MISSION_REWARD_CLAIMED, type.getReward()));
            return;
        }

        // /bs mission - show missions
        player.sendMessage(plugin.getMessageManager().get(MessageKey.MISSION_HEADER));

        // Daily
        player.sendMessage(plugin.getMessageManager().get(MessageKey.MISSION_DAILY_HEADER));
        java.util.List<MissionType> dailyMissions = mm.getActiveDailyMissions(uuid);
        for (MissionType m : dailyMissions) {
            int progress = mm.getProgress(uuid, m);
            if (progress >= m.getGoal()) {
                if (mm.isClaimed(uuid, m)) {
                    player.sendMessage(plugin.getMessageManager().get(MessageKey.MISSION_ENTRY_DONE,
                            m.getDisplay(lang), m.name().toLowerCase()));
                } else {
                    player.sendMessage(plugin.getMessageManager().get(MessageKey.MISSION_ENTRY_DONE,
                            m.getDisplay(lang), m.name().toLowerCase()));
                }
            } else {
                player.sendMessage(plugin.getMessageManager().get(MessageKey.MISSION_ENTRY_PROGRESS,
                        m.getDisplay(lang), progress, m.getGoal(), m.getReward()));
            }
        }

        // Weekly
        player.sendMessage(plugin.getMessageManager().get(MessageKey.MISSION_WEEKLY_HEADER));
        java.util.List<MissionType> weeklyMissions = mm.getActiveWeeklyMissions(uuid);
        for (MissionType m : weeklyMissions) {
            int progress = mm.getProgress(uuid, m);
            if (progress >= m.getGoal()) {
                player.sendMessage(plugin.getMessageManager().get(MessageKey.MISSION_ENTRY_DONE,
                        m.getDisplay(lang), m.name().toLowerCase()));
            } else {
                player.sendMessage(plugin.getMessageManager().get(MessageKey.MISSION_ENTRY_PROGRESS,
                        m.getDisplay(lang), progress, m.getGoal(), m.getReward()));
            }
        }

        player.sendMessage(plugin.getMessageManager().get(MessageKey.MISSION_FOOTER));
    }

    private void handleAchievement(Player player, String[] args) {
        AchievementManager am = plugin.getAchievementManager();
        java.util.UUID uuid = player.getUniqueId();
        String lang = plugin.getSettings().getLanguage();

        // /bs achievement claim <type>
        if (args.length >= 3 && args[1].equalsIgnoreCase("claim")) {
            AchievementType type;
            try {
                type = AchievementType.valueOf(args[2].toUpperCase());
            } catch (IllegalArgumentException e) {
                player.sendMessage(plugin.getMessageManager().get(MessageKey.ACHIEVEMENT_NOT_FOUND));
                return;
            }
            if (!am.isUnlocked(uuid, type)) {
                player.sendMessage(plugin.getMessageManager().get(MessageKey.ACHIEVEMENT_NOT_UNLOCKED));
                return;
            }
            if (am.isClaimed(uuid, type)) {
                player.sendMessage(plugin.getMessageManager().get(MessageKey.ACHIEVEMENT_ALREADY_CLAIMED));
                return;
            }
            am.claimReward(uuid, type);
            player.sendMessage(plugin.getMessageManager().get(MessageKey.ACHIEVEMENT_REWARD_CLAIMED, type.getReward()));
            return;
        }

        // /bs achievement - show all
        int unlocked = am.getUnlockedCount(uuid);
        int total = am.getTotalCount();
        player.sendMessage(plugin.getMessageManager().get(MessageKey.ACHIEVEMENT_HEADER, unlocked, total));

        for (AchievementType type : AchievementType.values()) {
            int progress = am.getProgress(uuid, type);
            if (am.isClaimed(uuid, type)) {
                player.sendMessage(plugin.getMessageManager().get(MessageKey.ACHIEVEMENT_ENTRY_CLAIMED,
                        type.getName(lang)));
            } else if (am.isUnlocked(uuid, type)) {
                player.sendMessage(plugin.getMessageManager().get(MessageKey.ACHIEVEMENT_ENTRY_UNLOCKED,
                        type.getName(lang), type.getReward(), type.name().toLowerCase()));
            } else {
                player.sendMessage(plugin.getMessageManager().get(MessageKey.ACHIEVEMENT_ENTRY_PROGRESS,
                        type.getName(lang), progress, type.getGoal(), type.getDesc(lang)));
            }
        }

        player.sendMessage(plugin.getMessageManager().get(MessageKey.ACHIEVEMENT_FOOTER));
    }

    private void handleTitleCommand(Player player, String[] args) {
        TitleEffectManager tem = plugin.getTitleEffectManager();
        java.util.UUID uuid = player.getUniqueId();

        // /bs title equip <id>
        if (args.length >= 3 && args[1].equalsIgnoreCase("equip")) {
            String id = args[2].toLowerCase();
            if (!tem.getRegisteredTitles().containsKey(id)) {
                player.sendMessage(plugin.getMessageManager().get(MessageKey.TITLE_NOT_FOUND));
                return;
            }
            if (!tem.getOwnedTitles(uuid).contains(id)) {
                player.sendMessage(plugin.getMessageManager().get(MessageKey.TITLE_NOT_OWNED));
                return;
            }
            tem.equipTitle(uuid, id);
            String display = org.bukkit.ChatColor.translateAlternateColorCodes('&',
                    tem.getRegisteredTitles().get(id).display());
            player.sendMessage(plugin.getMessageManager().get(MessageKey.TITLE_EQUIP, display));
            return;
        }

        // /bs title unequip
        if (args.length >= 2 && args[1].equalsIgnoreCase("unequip")) {
            tem.unequipTitle(uuid);
            player.sendMessage(plugin.getMessageManager().get(MessageKey.TITLE_UNEQUIP));
            return;
        }

        // /bs title - 보유 칭호 목록
        java.util.List<String> owned = tem.getOwnedTitles(uuid);
        if (owned.isEmpty()) {
            player.sendMessage(plugin.getMessageManager().get(MessageKey.TITLE_NONE));
            return;
        }

        String equipped = tem.getEquippedTitle(uuid);
        player.sendMessage(plugin.getMessageManager().get(MessageKey.TITLE_HEADER));
        for (String id : owned) {
            var def = tem.getRegisteredTitles().get(id);
            if (def == null) continue;
            String display = org.bukkit.ChatColor.translateAlternateColorCodes('&', def.display());
            if (id.equals(equipped)) {
                player.sendMessage(plugin.getMessageManager().get(MessageKey.TITLE_ITEMEQUIPPED, display, id));
            } else {
                player.sendMessage(plugin.getMessageManager().get(MessageKey.TITLE_ITEM, display, id));
            }
        }
    }

    private void handleEffectCommand(Player player, String[] args) {
        TitleEffectManager tem = plugin.getTitleEffectManager();
        java.util.UUID uuid = player.getUniqueId();

        // /bs effect equip <id>
        if (args.length >= 3 && args[1].equalsIgnoreCase("equip")) {
            String id = args[2].toLowerCase();
            if (!tem.getRegisteredEffects().containsKey(id)) {
                player.sendMessage(plugin.getMessageManager().get(MessageKey.EFFECT_NOT_FOUND));
                return;
            }
            if (!tem.getOwnedEffects(uuid).contains(id)) {
                player.sendMessage(plugin.getMessageManager().get(MessageKey.EFFECT_NOT_OWNED));
                return;
            }
            tem.equipEffect(uuid, id);
            player.sendMessage(plugin.getMessageManager().get(MessageKey.EFFECT_EQUIP, id));
            return;
        }

        // /bs effect unequip
        if (args.length >= 2 && args[1].equalsIgnoreCase("unequip")) {
            tem.unequipEffect(uuid);
            player.sendMessage(plugin.getMessageManager().get(MessageKey.EFFECT_UNEQUIP));
            return;
        }

        // /bs effect - 보유 이펙트 목록
        java.util.List<String> owned = tem.getOwnedEffects(uuid);
        if (owned.isEmpty()) {
            player.sendMessage(plugin.getMessageManager().get(MessageKey.EFFECT_NONE));
            return;
        }

        String equipped = tem.getEquippedEffect(uuid);
        player.sendMessage(plugin.getMessageManager().get(MessageKey.EFFECT_HEADER));
        for (String id : owned) {
            var def = tem.getRegisteredEffects().get(id);
            if (def == null) continue;
            if (id.equals(equipped)) {
                player.sendMessage(plugin.getMessageManager().get(MessageKey.EFFECT_ITEMEQUIPPED, id, def.particle().name()));
            } else {
                player.sendMessage(plugin.getMessageManager().get(MessageKey.EFFECT_ITEM, id, def.particle().name()));
            }
        }
    }
}
