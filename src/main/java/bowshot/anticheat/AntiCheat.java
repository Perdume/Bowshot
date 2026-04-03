package bowshot.anticheat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import bowshot.Bowshot;
import bowshot.debug.DebugLogger;
import bowshot.debug.ErrorCode;
import bowshot.game.Game;
import bowshot.game.GameState;
import bowshot.player.GamePlayer;

/**
 * Anti-cheat system for Bowshot PvP minigame.
 *
 * Design references:
 * - GrimAC: buffer/decay violation system, latency-compensated reach
 * - NoCheatPlus: per-tick movement tracking, teleport/velocity exemptions
 * - Vulcan: CPS rolling window with autoclicker pattern detection
 *
 * Checks:
 * - Reach: eye-to-hitbox distance with ping buffer, cumulative overshoot buffer
 * - Speed: per-tick horizontal movement with dynamic max (potions, sprint, air), 5-tick average
 * - CPS: rolling 1s window, sustained high CPS + autoclicker stddev pattern
 *
 * All VLs are float-based with per-tick decay (0.005/tick = 0.1/sec).
 * Violations require sustained suspicious activity to accumulate.
 */
public class AntiCheat implements Listener {

    private final Bowshot plugin;
    private final Map<UUID, PlayerData> dataMap = new HashMap<>();
    private int tickCounter = 0;

    public AntiCheat(Bowshot plugin) {
        this.plugin = plugin;
        new BukkitRunnable() {
            @Override
            public void run() {
                tickAll();
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    public void removePlayer(UUID uuid) {
        dataMap.remove(uuid);
    }

    private PlayerData getData(Player player) {
        return dataMap.computeIfAbsent(player.getUniqueId(), k -> new PlayerData());
    }

    private void tickAll() {
        tickCounter++;
        Iterator<Map.Entry<UUID, PlayerData>> it = dataMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, PlayerData> entry = it.next();
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) {
                it.remove();
                continue;
            }
            entry.getValue().tick();
        }
    }

    // =========================================================================
    // Reach Check
    // =========================================================================

    @EventHandler(priority = EventPriority.LOW)
    public void onMeleeDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;

        Game game = plugin.getGameManager().getGameByPlayer(attacker);
        if (game == null || game.getState() != GameState.PLAYING) return;

        GamePlayer gp = plugin.getPlayerManager().getPlayer(attacker);
        if (gp == null || gp.isSpectator()) return;

        PlayerData data = getData(attacker);

        // Eye-to-victim-center distance
        Location eyeLoc = attacker.getEyeLocation();
        Location victimLoc = victim.getLocation();
        double dx = eyeLoc.getX() - victimLoc.getX();
        double dz = eyeLoc.getZ() - victimLoc.getZ();
        double dy = eyeLoc.getY() - (victimLoc.getY() + 0.9);
        double rawDistance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        // Subtract half hitbox width (player hitbox is 0.6 wide)
        double hitboxAdjusted = rawDistance - 0.3;

        // Latency buffer: ~0.05 extra per 25ms ping, capped at 1.0
        int ping = attacker.getPing();
        double pingBuffer = Math.min(1.0, (ping / 25.0) * 0.05);

        double maxReach = plugin.getSettings().getAcMaxReach() + pingBuffer;

        if (hitboxAdjusted > maxReach) {
            double overshoot = hitboxAdjusted - maxReach;
            data.reachBuffer += overshoot;

            if (data.reachBuffer > 0.5) {
                data.reachVL += 1.0;
                data.reachBuffer = 0;
                DebugLogger.report(ErrorCode.AC_REACH_DETECT,
                        attacker.getName() + " dist=" + String.format("%.2f", hitboxAdjusted)
                        + " max=" + String.format("%.2f", maxReach)
                        + " VL=" + String.format("%.1f", data.reachVL));
                event.setCancelled(true);
                handleViolation(attacker, game, data.getTotalVL(), "Reach");
            }
        } else {
            data.reachBuffer = Math.max(0, data.reachBuffer - 0.15);
        }
    }

    // =========================================================================
    // Speed Check — with game state, swimming, climbing, vehicle exemptions
    // =========================================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getGameByPlayer(player);
        if (game == null || game.getState() != GameState.PLAYING) return;

        GamePlayer gp = plugin.getPlayerManager().getPlayer(player);
        if (gp == null || gp.isSpectator()) return;

        // Vehicle exemption (boats, minecarts, etc.)
        if (player.isInsideVehicle()) return;

        // Swimming / climbing / gliding exemptions
        if (player.isSwimming() || player.isClimbing() || player.isGliding()) return;

        // Riptide exemption
        if (player.isRiptiding()) return;

        Location to = event.getTo();
        Location from = event.getFrom();
        if (to == null) return;

        PlayerData data = getData(player);

        // Teleport exemption (20 ticks)
        if (tickCounter - data.lastTeleportTick < 20) return;

        // Velocity/knockback exemption (15 ticks, max 25 ticks extended)
        if (tickCounter - data.lastVelocityTick < 15) {
            data.velocityExemptTicks++;
            if (data.velocityExemptTicks < 25) return;
        }

        // Damage knockback exemption (10 ticks)
        if (tickCounter - data.lastDamageTick < 10) return;

        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        if (horizontalDist < 0.01) return;

        data.pushMovement(horizontalDist);

        // Dynamic max speed per tick
        double baseSpeed = 0.2873;
        double sprintFactor = player.isSprinting() ? 1.3 : 1.0;
        double sneakFactor = player.isSneaking() ? 0.3 : 1.0;

        double potionFactor = 1.0;
        PotionEffect speedEffect = player.getPotionEffect(PotionEffectType.SPEED);
        if (speedEffect != null) {
            potionFactor += (speedEffect.getAmplifier() + 1) * 0.2;
        }

        boolean onGround = player.isOnGround();
        double airFactor = onGround ? 1.0 : 1.6;

        // Ice boost factor
        Material belowBlock = player.getLocation().subtract(0, 1, 0).getBlock().getType();
        double iceFactor = (belowBlock == Material.ICE || belowBlock == Material.PACKED_ICE
                || belowBlock == Material.BLUE_ICE || belowBlock == Material.FROSTED_ICE)
                ? 2.5 : 1.0;

        // 60% safety margin (was 50%)
        double safetyMargin = 1.6;

        double maxPerTick = baseSpeed * sprintFactor * sneakFactor * potionFactor
                * airFactor * iceFactor * safetyMargin;

        // 5-tick running average smooths lag spikes
        double avgSpeed = data.getAverageMovement(5);

        if (avgSpeed > maxPerTick && avgSpeed > 0.8) {
            data.speedBuffer += (avgSpeed - maxPerTick);

            if (data.speedBuffer > maxPerTick * 5) {
                data.speedVL += 1.0;
                data.speedBuffer = 0;
                DebugLogger.report(ErrorCode.AC_SPEED_DETECT,
                        player.getName() + " avg=" + String.format("%.3f", avgSpeed)
                        + " max=" + String.format("%.3f", maxPerTick)
                        + " VL=" + String.format("%.1f", data.speedVL));
                handleViolation(player, game, data.getTotalVL(), "Speed");
            }
        } else {
            data.speedBuffer = Math.max(0, data.speedBuffer - 0.01);
        }
    }

    // =========================================================================
    // CPS Check — block interaction exemption + stricter autoclicker detection
    // =========================================================================

    @EventHandler
    public void onBlockInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            PlayerData data = getData(event.getPlayer());
            data.lastBlockInteractTick = tickCounter;
        }
    }

    @EventHandler
    public void onAnimation(PlayerAnimationEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getGameByPlayer(player);
        if (game == null || game.getState() != GameState.PLAYING) return;

        GamePlayer gp = plugin.getPlayerManager().getPlayer(player);
        if (gp == null || gp.isSpectator()) return;

        PlayerData data = getData(player);

        // === Block interaction exemption ===
        // LEFT_CLICK_BLOCK fires alongside animation — exempt recent block interactions
        if (tickCounter - data.lastBlockInteractTick < 5) return;

        // Also exempt if player is looking at a solid block (attempted mining in adventure mode)
        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock != null && !targetBlock.getType().isAir()) return;

        long now = System.currentTimeMillis();
        data.clickTimes.add(now);
        data.clickTimes.removeIf(t -> now - t > 1000);

        int cps = data.clickTimes.size();
        int maxCps = plugin.getSettings().getAcMaxCps();

        if (cps > maxCps) {
            data.highCpsTicks++;

            // Require sustained high CPS for 5 ticks (was 3)
            if (data.highCpsTicks >= 5) {
                boolean autoPattern = checkAutoClickPattern(data.clickTimes);

                // Only flag for clear autoclicker pattern OR extreme CPS (maxCps + 10)
                if (autoPattern || cps > maxCps + 10) {
                    data.cpsVL += 1.0;
                    DebugLogger.report(ErrorCode.AC_CPS_DETECT,
                            player.getName() + " CPS=" + cps
                            + " pattern=" + autoPattern
                            + " VL=" + String.format("%.1f", data.cpsVL));
                    handleViolation(player, game, data.getTotalVL(), "CPS");
                } else {
                    // Borderline detection — log but don't add VL
                    DebugLogger.report(ErrorCode.AC_FALSE_POSITIVE,
                            player.getName() + " CPS=" + cps + " (borderline, no VL added)");
                }
            }
        } else {
            data.highCpsTicks = Math.max(0, data.highCpsTicks - 1);
        }
    }

    private boolean checkAutoClickPattern(List<Long> clickTimes) {
        if (clickTimes.size() < 10) return false;

        List<Long> intervals = new ArrayList<>();
        for (int i = 1; i < clickTimes.size(); i++) {
            intervals.add(clickTimes.get(i) - clickTimes.get(i - 1));
        }

        double mean = intervals.stream().mapToLong(Long::longValue).average().orElse(0);
        double variance = intervals.stream()
                .mapToDouble(i -> (i - mean) * (i - mean))
                .average().orElse(0);
        double stdDev = Math.sqrt(variance);

        // Stricter pattern detection — only flag obvious bot patterns
        // stdDev < 3.0 = machine-level consistency, mean < 80ms = superhuman speed
        return stdDev < 3.0 && mean < 80;
    }

    // =========================================================================
    // Exemptions
    // =========================================================================

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        PlayerData data = getData(event.getPlayer());
        data.lastTeleportTick = tickCounter;
        data.speedBuffer = 0;
        data.clearMovements();
    }

    @EventHandler
    public void onVelocity(PlayerVelocityEvent event) {
        PlayerData data = getData(event.getPlayer());
        data.lastVelocityTick = tickCounter;
        data.velocityExemptTicks = 0;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageReceived(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player victim) {
            PlayerData data = getData(victim);
            data.lastDamageTick = tickCounter;
        }
    }

    // =========================================================================
    // Cleanup
    // =========================================================================

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        dataMap.remove(event.getPlayer().getUniqueId());
    }

    // =========================================================================
    // Violation Handler
    // =========================================================================

    private void handleViolation(Player player, Game game, double totalVL, String type) {
        int threshold = plugin.getSettings().getAcViolationThreshold();

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("bowshot.admin")) {
                online.sendMessage(ChatColor.RED + "[AC] " + player.getName()
                        + " " + type + " (VL: " + String.format("%.1f", totalVL) + ")");
            }
        }

        if (totalVL >= threshold) {
            DebugLogger.report(ErrorCode.AC_PLAYER_REMOVED,
                    player.getName() + " VL=" + String.format("%.1f", totalVL));
            player.sendMessage(ChatColor.RED + "[AntiCheat] 부정행위 감지로 게임에서 제외되었습니다.");

            GamePlayer gp = plugin.getPlayerManager().getPlayer(player);
            gp.setSpectator(true);
            player.setCollidable(false);
            player.setAllowFlight(true);
            player.setFlying(true);
            player.getInventory().clear();

            for (GamePlayer other : game.getPlayers()) {
                if (!other.getPlayer().equals(player)) {
                    other.getPlayer().hidePlayer(plugin, player);
                }
            }

            dataMap.remove(player.getUniqueId());
        }
    }

    // =========================================================================
    // Player Data
    // =========================================================================

    private static class PlayerData {
        double reachVL = 0;
        double speedVL = 0;
        double cpsVL = 0;

        double reachBuffer = 0;
        double speedBuffer = 0;
        int highCpsTicks = 0;

        int lastTeleportTick = -100;
        int lastVelocityTick = -100;
        int lastDamageTick = -100;
        int lastBlockInteractTick = -100;
        int velocityExemptTicks = 0;

        final List<Long> clickTimes = new ArrayList<>();

        private final double[] movements = new double[20];
        private int movementIndex = 0;
        private int movementCount = 0;

        void pushMovement(double dist) {
            movements[movementIndex] = dist;
            movementIndex = (movementIndex + 1) % movements.length;
            if (movementCount < movements.length) movementCount++;
        }

        double getAverageMovement(int ticks) {
            if (movementCount == 0) return 0;
            int count = Math.min(ticks, movementCount);
            double sum = 0;
            for (int i = 0; i < count; i++) {
                int idx = (movementIndex - 1 - i + movements.length) % movements.length;
                sum += movements[idx];
            }
            return sum / count;
        }

        void clearMovements() {
            Arrays.fill(movements, 0);
            movementIndex = 0;
            movementCount = 0;
        }

        void tick() {
            // Doubled decay rate: 0.01/tick = 0.2/sec (was 0.005/tick = 0.1/sec)
            reachVL = Math.max(0, reachVL - 0.01);
            speedVL = Math.max(0, speedVL - 0.01);
            cpsVL = Math.max(0, cpsVL - 0.01);
        }

        double getTotalVL() {
            return reachVL + speedVL + cpsVL;
        }
    }
}
