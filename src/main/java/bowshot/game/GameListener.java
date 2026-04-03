package bowshot.game;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import bowshot.Bowshot;
import bowshot.arena.ArenaInstance;
import bowshot.debug.DebugLogger;
import bowshot.debug.ErrorCode;
import bowshot.lang.MessageKey;
import bowshot.player.GamePlayer;

public class GameListener implements Listener {

    private final Bowshot plugin;

    public GameListener(Bowshot plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        if (plugin.getGameManager().isPlayerPlaying(player)) {
            event.getProjectile().setGravity(false);

            // Record arrow shoot for replay
            Game game = plugin.getGameManager().getGameByPlayer(player);
            if (game != null && game.getReplayRecorder() != null) {
                game.getReplayRecorder().recordArrowShoot(player);
            }
        }

        GamePlayer gp = plugin.getPlayerManager().getPlayer(player);
        if (gp.isSpectator() || plugin.getReplayManager().isViewing(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damaged = event.getEntity();
        if (!(damaged instanceof Player)) return;
        Player victimPlayer = (Player) damaged;

        // Block all damage for replay viewers
        if (plugin.getReplayManager().isViewing(victimPlayer)) {
            event.setCancelled(true);
            return;
        }

        // Block replay viewers from dealing damage
        Player attackerPlayer = getAttacker(event);
        if (attackerPlayer != null && plugin.getReplayManager().isViewing(attackerPlayer)) {
            event.setCancelled(true);
            return;
        }

        GamePlayer victim = plugin.getPlayerManager().getPlayer(damaged.getUniqueId());
        ArenaInstance arena = victim.getArenaInstance();
        if (arena == null) return;

        if (!arena.getPlayers().contains(victim)) return;

        // Cancel damage from spectators (including arrows shot by spectators)
        if (attackerPlayer != null) {
            GamePlayer attacker = plugin.getPlayerManager().getPlayer(attackerPlayer);
            if (arena.getPlayers().contains(attacker) && attacker.isSpectator()) {
                event.setCancelled(true);
                return;
            }
        }

        // If victim is spectator, ignore
        if (victim.isSpectator()) {
            victimPlayer.setCollidable(false);
            event.setCancelled(true);
            return;
        }

        // Check lethal damage
        if (event.getDamage() >= victimPlayer.getHealth()) {
            event.setDamage(0);
            convertToSpectator(victim, arena);

            Game game = plugin.getGameManager().getGame(arena);
            if (game == null) return;

            // Record death for placement tracking
            game.recordDeath(victimPlayer);

            // Track killer
            if (event.getDamager() instanceof Arrow) {
                Arrow arrow = (Arrow) event.getDamager();
                if (arrow.getShooter() instanceof Player) {
                    Player killer = (Player) arrow.getShooter();
                    game.addBowKill(killer);
                    processKill(game, killer, victimPlayer);
                }
            } else if (event.getDamager() instanceof Player) {
                Player killer = (Player) event.getDamager();
                game.addMeleeKill(killer);
                processKill(game, killer, victimPlayer);
            }

            // Check if game should end
            if (arena.getAliveCount() <= 1) {
                plugin.getGameManager().endGame(arena);
            }
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();

        // Block all damage for replay viewers
        if (plugin.getReplayManager().isViewing(player)) {
            event.setCancelled(true);
            return;
        }

        GamePlayer gp = plugin.getPlayerManager().getPlayer(player);
        ArenaInstance arena = gp.getArenaInstance();
        if (arena == null) return;

        // Skip if already handled by entity damage event
        if (event.getCause() == EntityDamageEvent.DamageCause.PROJECTILE
                || event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            return;
        }

        if (gp.isSpectator()) {
            event.setCancelled(true);
            return;
        }

        // Environmental kill
        if (event.getDamage() >= player.getHealth()) {
            if (arena.getPlayers().contains(gp)) {
                event.setDamage(0);
                player.getInventory().clear();
                player.getInventory().addItem(createSpectatorItem());
                player.teleport(arena.getSpawnLocation());
                convertToSpectator(gp, arena);

                Game game = plugin.getGameManager().getGame(arena);
                if (game != null) {
                    // Record death for placement tracking
                    game.recordDeath(player);

                    game.broadcast(plugin.getMessageManager().get(MessageKey.KILL_ENVIRONMENT, player.getName()));

                    // Record death for replay
                    if (game.getReplayRecorder() != null) {
                        game.getReplayRecorder().recordDeath(player);
                    }
                }

                if (arena.getAliveCount() <= 1) {
                    plugin.getGameManager().endGame(arena);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Handle in-game quit — 사망 처리
        for (Game game : plugin.getGameManager().getActiveGames()) {
            if (game.isPlayerPlaying(player)) {
                DebugLogger.report(ErrorCode.GAME_PLAYER_DISCONNECT, player.getName());
                GamePlayer gp = plugin.getPlayerManager().getPlayer(player);

                // 아직 살아있으면 사망 처리
                if (!gp.isSpectator()) {
                    gp.setSpectator(true);

                    // 사망 기록
                    game.recordDeath(player);
                    game.recordDisconnect(player);

                    // 킬피드 (환경 사망으로 표시)
                    game.broadcast(plugin.getMessageManager().get(MessageKey.KILL_ENVIRONMENT, player.getName()));

                    // 리플레이 기록
                    if (game.getReplayRecorder() != null) {
                        game.getReplayRecorder().recordDeath(player);
                    }
                } else {
                    // 이미 관전자면 disconnect만 기록
                    game.recordDisconnect(player);
                }

                Scoreboard score = Bukkit.getScoreboardManager().getMainScoreboard();
                Team team = score.getTeam("nhide");
                if (team != null) {
                    team.removeEntry(player.getName());
                }

                if (game.getArenaInstance().getAliveCount() <= 1) {
                    game.end();
                }
                break;
            }
        }

        // Handle queue quit
        if (plugin.getMatchQueue().isInQueue(player)) {
            plugin.getMatchQueue().leave(player);
        }

        // Handle visited world cleanup
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            if (plugin.getInstanceWorldManager().isInstanceWorld(player.getWorld())) {
                plugin.getInstanceWorldManager().removeWorld(player.getWorld());
            }
        }, 20);
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        World from = event.getFrom();
        if (!plugin.getInstanceWorldManager().isInstanceWorld(from)) return;

        // Delay 1 tick to let the player fully leave
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (from.getPlayers().isEmpty()) {
                bowshot.world.InstanceType type = plugin.getInstanceWorldManager().getInstanceType(from);
                if (type != bowshot.world.InstanceType.EDIT) {
                    plugin.getInstanceWorldManager().removeWorld(from);
                }
            }
        }, 1L);
    }

    @EventHandler
    public void onSpectatorStick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getItem() == null) return;
        if (event.getItem().getType() != Material.STICK) return;
        ItemMeta meta = event.getItem().getItemMeta();
        if (meta == null) return;

        String tpName = plugin.getMessageManager().get(MessageKey.SPECTATOR_PLAYER_TP);
        if (!meta.getDisplayName().equals(tpName)) return;

        plugin.getSpectatorGui().openInventory(player);
    }

    @EventHandler
    public void onBlockInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getGameManager().isPlayerPlaying(player)) return;
        if (event.getClickedBlock() == null) return;

        Material type = event.getClickedBlock().getType();
        if (type == Material.CHEST || type == Material.BARREL) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        if (plugin.getGameManager().isPlayerPlaying(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    // === Helpers ===

    private void convertToSpectator(GamePlayer gp, ArenaInstance arena) {
        Player player = gp.getPlayer();
        DebugLogger.log("Game", player.getName() + " converted to spectator in " + arena.getWorldName());
        gp.setSpectator(true);
        player.setCollidable(false);
        player.setAllowFlight(true);
        player.setFlying(true);

        float flySpeed = Math.max(0.0f, Math.min(1.0f, plugin.getSettings().getSpectatorFlySpeed() * 0.1f));
        player.setFlySpeed(flySpeed);

        player.getInventory().clear();
        if (plugin.getSettings().isSpectatorShowItems()) {
            player.getInventory().addItem(createSpectatorItem());
        }
        player.setNoDamageTicks(0);
        player.setMaximumNoDamageTicks(0);

        if (plugin.getSettings().isSpectatorNightVision()) {
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
        }

        if (plugin.getSettings().isSpectatorTeleportOnDeath()) {
            player.teleport(arena.getSpawnLocation());
        }

        if (plugin.getSettings().isSpectatorInvisible()) {
            // 투명 포션 적용 (hidePlayer 백업)
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
            for (GamePlayer other : arena.getPlayers()) {
                other.getPlayer().hidePlayer(plugin, player);
            }
        }

        // 관전자가 살아있는 플레이어를 볼 수 있도록 보장
        for (GamePlayer other : arena.getPlayers()) {
            if (!other.isSpectator() && !other.equals(gp)) {
                player.showPlayer(plugin, other.getPlayer());
            }
        }
    }

    private void processKill(Game game, Player killer, Player victim) {
        game.addKill(killer);
        game.addKillLog(killer, victim);
        double newScore = 1 + game.getKillScore(victim) * 0.5;
        game.setKillScore(killer, newScore);

        // 칭호 프리픽스 + 이펙트
        String killerPrefix = plugin.getTitleEffectManager().getDisplayPrefix(killer.getUniqueId());
        String killerName = killerPrefix.isEmpty() ? killer.getName()
                : org.bukkit.ChatColor.translateAlternateColorCodes('&', killerPrefix) + killer.getName();
        game.broadcast(plugin.getMessageManager().get(MessageKey.KILL_MESSAGE, victim.getName(), killerName));
        plugin.getTitleEffectManager().playEffect(killer);

        // Record kill for replay
        if (game.getReplayRecorder() != null) {
            game.getReplayRecorder().recordKill(killer, victim);
        }
    }

    private Player getAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            return (Player) event.getDamager();
        }
        if (event.getDamager() instanceof Arrow arrow) {
            if (arrow.getShooter() instanceof Player) {
                return (Player) arrow.getShooter();
            }
        }
        return null;
    }

    private ItemStack createSpectatorItem() {
        ItemStack item = new ItemStack(Material.STICK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(plugin.getMessageManager().get(MessageKey.SPECTATOR_PLAYER_TP));
        item.setItemMeta(meta);
        return item;
    }
}
