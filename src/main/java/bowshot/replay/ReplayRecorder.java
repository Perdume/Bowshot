package bowshot.replay;

import bowshot.Bowshot;
import bowshot.game.Game;
import bowshot.player.GamePlayer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class ReplayRecorder {

    private final Bowshot plugin;
    private final Game game;
    private final ReplayData data;
    private int currentTick;
    private BukkitRunnable recordTask;
    private boolean recording;

    public ReplayRecorder(Bowshot plugin, Game game, String arenaName) {
        this.plugin = plugin;
        this.game = game;
        this.data = new ReplayData(arenaName);
        this.currentTick = 0;
        this.recording = false;

        for (GamePlayer gp : game.getPlayers()) {
            data.addPlayer(gp.getUniqueId(), gp.getPlayer().getName());
        }
    }

    public void start() {
        if (recording) return;
        recording = true;

        data.addEvent(ReplayEvent.gameStart(0));

        int interval = plugin.getSettings().getReplayRecordingInterval();
        recordTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!recording || game.isEnded()) {
                    stop();
                    cancel();
                    return;
                }
                currentTick += interval;
                recordPositions();
            }
        };
        recordTask.runTaskTimer(plugin, interval, interval);
    }

    public void stop() {
        if (!recording) return;
        recording = false;
        data.addEvent(ReplayEvent.gameEnd(currentTick));
        data.setDurationTicks(currentTick);

        if (recordTask != null) {
            try { recordTask.cancel(); } catch (IllegalStateException ignored) {}
        }
    }

    private void recordPositions() {
        for (GamePlayer gp : game.getPlayers()) {
            if (gp.isSpectator()) continue;
            Player player = gp.getPlayer();
            int index = data.getPlayerIndex(gp.getUniqueId());
            if (index < 0) continue;

            byte pose = player.isSneaking() ? (byte) 1 : (byte) 0;
            ItemStack hand = player.getInventory().getItemInMainHand();
            String itemName = (hand != null && hand.getType() != Material.AIR)
                    ? hand.getType().name() : "";

            data.addEvent(ReplayEvent.position(currentTick, index,
                    (float) player.getLocation().getX(),
                    (float) player.getLocation().getY(),
                    (float) player.getLocation().getZ(),
                    player.getLocation().getYaw(),
                    player.getLocation().getPitch(),
                    pose, itemName));
        }
    }

    public void recordArrowShoot(Player shooter) {
        int index = data.getPlayerIndex(shooter.getUniqueId());
        if (index < 0) return;
        data.addEvent(ReplayEvent.arrowShoot(currentTick, index,
                (float) shooter.getLocation().getX(),
                (float) shooter.getLocation().getY(),
                (float) shooter.getLocation().getZ(),
                shooter.getLocation().getYaw(),
                shooter.getLocation().getPitch()));
    }

    public void recordKill(Player killer, Player victim) {
        int killerIdx = data.getPlayerIndex(killer.getUniqueId());
        int victimIdx = data.getPlayerIndex(victim.getUniqueId());
        if (killerIdx >= 0 && victimIdx >= 0) {
            data.addEvent(ReplayEvent.kill(currentTick, killerIdx, victimIdx));
        }
    }

    public void recordDeath(Player victim) {
        int index = data.getPlayerIndex(victim.getUniqueId());
        if (index >= 0) {
            data.addEvent(ReplayEvent.death(currentTick, index));
        }
    }

    public ReplayData getData() { return data; }
    public boolean isRecording() { return recording; }
}
