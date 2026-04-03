package bowshot.replay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.scheduler.BukkitRunnable;

import bowshot.Bowshot;
import bowshot.lang.MessageKey;

public class ReplayViewer {

    private final Bowshot plugin;
    private final Player viewer;
    private final ReplayData data;
    private final World replayWorld;
    private final List<Mannequin> ghosts = new ArrayList<>();
    private final Map<Integer, Mannequin> playerGhosts = new HashMap<>();

    private int currentTick;
    private int speed;
    private boolean paused;
    private boolean active;
    private BukkitRunnable playbackTask;
    private Location returnLocation;

    public ReplayViewer(Bowshot plugin, Player viewer, ReplayData data, World replayWorld) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.data = data;
        this.replayWorld = replayWorld;
        this.currentTick = 0;
        this.speed = 1;
        this.paused = false;
        this.active = false;
    }

    public void start() {
        if (active) return;
        active = true;
        returnLocation = viewer.getLocation().clone();

        // Spawn mannequins for each player
        for (int i = 0; i < data.getPlayerCount(); i++) {
            Mannequin mannequin = replayWorld.spawn(
                    new Location(replayWorld, 0, -100, 0), Mannequin.class);
            mannequin.setGravity(false);
            mannequin.setInvulnerable(true);
            mannequin.setAI(false);
            mannequin.setImmovable(true);
            mannequin.setCustomName(ChatColor.GREEN + data.getPlayerNames().get(i));
            mannequin.setCustomNameVisible(true);

            // Set player skin
            OfflinePlayer target = Bukkit.getOfflinePlayer(data.getPlayerUuids().get(i));
            PlayerProfile profile = target.getPlayerProfile();
            if (profile != null) {
                mannequin.setPlayerProfile(profile);
            }

            ghosts.add(mannequin);
            playerGhosts.put(i, mannequin);
        }

        // Teleport viewer to world
        Location spawnLoc = new Location(replayWorld, 0, 70, 0);
        ReplayEvent firstPos = findFirstPosition();
        if (firstPos != null) {
            spawnLoc = new Location(replayWorld, firstPos.getX(), firstPos.getY() + 5, firstPos.getZ());
        }
        viewer.teleport(spawnLoc);
        viewer.setGameMode(GameMode.ADVENTURE);
        viewer.setAllowFlight(true);
        viewer.setFlying(true);
        viewer.setCollidable(false);
        viewer.setInvisible(true);

        // Give control info
        viewer.sendMessage(plugin.getMessageManager().get(MessageKey.REPLAY_STARTED));
        giveControlItems();

        // Start playback
        playbackTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active || !viewer.isOnline()) {
                    stop();
                    cancel();
                    return;
                }
                if (paused) return;

                for (int s = 0; s < speed; s++) {
                    currentTick++;
                    processEventsAtTick(currentTick);
                }

                if (currentTick >= data.getDurationTicks()) {
                    viewer.sendMessage(plugin.getMessageManager().get(MessageKey.REPLAY_ENDED));
                    stop();
                    cancel();
                }
            }
        };
        playbackTask.runTaskTimer(plugin, 1L, 1L);
    }

    public void stop() {
        if (!active) return;
        active = false;

        // Remove all ghosts
        for (Mannequin m : ghosts) {
            m.remove();
        }
        ghosts.clear();
        playerGhosts.clear();

        if (playbackTask != null) {
            try { playbackTask.cancel(); } catch (IllegalStateException ignored) {}
        }

        // Return viewer
        if (viewer.isOnline()) {
            viewer.setGameMode(GameMode.ADVENTURE);
            viewer.setCollidable(true);
            viewer.setInvisible(false);
            viewer.setAllowFlight(false);
            viewer.setFlying(false);
            viewer.getInventory().clear();
            if (returnLocation != null) {
                viewer.teleport(returnLocation);
            }
        }

        // Clean up world
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            plugin.getInstanceWorldManager().removeWorld(replayWorld);
        }, 20);

        plugin.getReplayManager().removeViewer(viewer);
    }

    private void processEventsAtTick(int tick) {
        for (ReplayEvent event : data.getEvents()) {
            if (event.getTick() != tick) continue;

            switch (event.getType()) {
                case POSITION:
                    moveGhost(event.getPlayerIndex(),
                            event.getX(), event.getY(), event.getZ(),
                            event.getYaw(), event.getPitch(),
                            event.getPose(), event.getMainHandItem());
                    break;
                case ARROW_SHOOT:
                    showArrow(event);
                    break;
                case KILL:
                    announceKill(event);
                    break;
                case DEATH:
                    announceDeath(event);
                    hideGhost(event.getPlayerIndex());
                    break;
                case GAME_END:
                    break;
                default:
                    break;
            }
        }
    }

    private void moveGhost(int playerIndex, float x, float y, float z, float yaw, float pitch,
                            byte pose, String mainHandItem) {
        Mannequin mannequin = playerGhosts.get(playerIndex);
        if (mannequin == null || mannequin.isDead()) return;
        mannequin.teleport(new Location(replayWorld, x, y, z, yaw, pitch));

        // Apply pose
        mannequin.setPose(pose == 1 ? Pose.SNEAKING : Pose.STANDING);

        // Apply held item
        if (mainHandItem != null && !mainHandItem.isEmpty()) {
            try {
                Material mat = Material.valueOf(mainHandItem);
                mannequin.getEquipment().setItemInMainHand(new ItemStack(mat));
            } catch (IllegalArgumentException ignored) {
                mannequin.getEquipment().setItemInMainHand(null);
            }
        } else {
            mannequin.getEquipment().setItemInMainHand(null);
        }
    }

    private void hideGhost(int playerIndex) {
        Mannequin mannequin = playerGhosts.get(playerIndex);
        if (mannequin == null || mannequin.isDead()) return;
        mannequin.setCustomName(ChatColor.RED + ChatColor.STRIKETHROUGH.toString() +
                data.getPlayerNames().get(playerIndex));
        mannequin.setInvisible(true);
    }

    private void showArrow(ReplayEvent event) {
        Location loc = new Location(replayWorld, event.getX(), event.getY() + 1.5, event.getZ());
        loc.setYaw(event.getYaw());
        loc.setPitch(event.getPitch());
        replayWorld.spawnArrow(loc, loc.getDirection(), 3.0f, 0.0f);
    }

    private void announceKill(ReplayEvent event) {
        String killer = data.getPlayerNames().get(event.getPlayerIndex());
        String victim = event.getTargetIndex() >= 0 ? data.getPlayerNames().get(event.getTargetIndex()) : "?";
        viewer.sendMessage(plugin.getMessageManager().get(MessageKey.KILL_MESSAGE, killer, victim));
    }

    private void announceDeath(ReplayEvent event) {
        String victim = data.getPlayerNames().get(event.getPlayerIndex());
        viewer.sendMessage(plugin.getMessageManager().get(MessageKey.KILL_ENVIRONMENT, victim));
    }

    private ReplayEvent findFirstPosition() {
        for (ReplayEvent event : data.getEvents()) {
            if (event.getType() == ReplayEventType.POSITION) {
                return event;
            }
        }
        return null;
    }

    private void giveControlItems() {
        viewer.getInventory().clear();

        ItemStack pauseItem = new ItemStack(Material.CLOCK);
        ItemMeta pauseMeta = pauseItem.getItemMeta();
        pauseMeta.setDisplayName(ChatColor.YELLOW + "Pause/Resume");
        pauseItem.setItemMeta(pauseMeta);
        viewer.getInventory().setItem(3, pauseItem);

        ItemStack speedItem = new ItemStack(Material.FEATHER);
        ItemMeta speedMeta = speedItem.getItemMeta();
        speedMeta.setDisplayName(ChatColor.AQUA + "Speed: x" + speed);
        speedItem.setItemMeta(speedMeta);
        viewer.getInventory().setItem(4, speedItem);

        ItemStack stopItem = new ItemStack(Material.BARRIER);
        ItemMeta stopMeta = stopItem.getItemMeta();
        stopMeta.setDisplayName(ChatColor.RED + "Stop");
        stopItem.setItemMeta(stopMeta);
        viewer.getInventory().setItem(5, stopItem);
    }

    public void togglePause() {
        paused = !paused;
        if (paused) {
            viewer.sendMessage(plugin.getMessageManager().get(MessageKey.REPLAY_PAUSED));
        } else {
            viewer.sendMessage(plugin.getMessageManager().get(MessageKey.REPLAY_RESUMED));
        }
    }

    public void cycleSpeed() {
        speed = speed >= 4 ? 1 : speed + 1;
        viewer.sendMessage(plugin.getMessageManager().get(MessageKey.REPLAY_SPEED, speed));
        giveControlItems();
    }

    public boolean isActive() { return active; }
    public boolean isPaused() { return paused; }
    public int getSpeed() { return speed; }
    public int getCurrentTick() { return currentTick; }
}
