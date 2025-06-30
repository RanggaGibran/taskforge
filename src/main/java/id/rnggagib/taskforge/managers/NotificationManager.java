package id.rnggagib.taskforge.managers;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import id.rnggagib.taskforge.TaskForgePlugin;
import id.rnggagib.taskforge.jobs.JobObjective;

/**
 * Manages job reward notifications (chat and bossbar)
 * Implements anti-spam for bossbar notifications
 */
public class NotificationManager {
    
    private final TaskForgePlugin plugin;
    
    // Bossbar management
    private final Map<UUID, BossBar> playerBossBars = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> bossBarTasks = new ConcurrentHashMap<>();
    
    // Reward accumulation for anti-spam
    private final Map<UUID, AccumulatedRewards> pendingRewards = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> rewardTasks = new ConcurrentHashMap<>();
    
    public NotificationManager(TaskForgePlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Get accumulation time from config in milliseconds
     */
    private long getAccumulationTimeMs() {
        int seconds = plugin.getConfigManager().getConfig().getInt("notifications.bossbar.accumulation_time", 2);
        return seconds * 1000L;
    }
    
    /**
     * Get bossbar display time from config in milliseconds
     */
    private long getBossBarDisplayTimeMs() {
        int seconds = plugin.getConfigManager().getConfig().getInt("notifications.bossbar.display_time", 3);
        return seconds * 1000L;
    }
    
    /**
     * Send job reward notification to player
     */
    public void sendJobRewardNotification(Player player, String jobName, JobObjective objective) {
        double experience = objective.getExperience();
        double money = objective.getRandomMoney();
        
        String notificationType = plugin.getConfigManager().getConfig().getString("notifications.type", "bossbar");
        
        if ("chat".equalsIgnoreCase(notificationType)) {
            sendChatNotification(player, jobName, experience, money);
        } else {
            sendBossBarNotification(player, jobName, experience, money);
        }
    }
    
    /**
     * Send chat notification
     */
    private void sendChatNotification(Player player, String jobName, double experience, double money) {
        if (experience > 0 && money > 0) {
            String currencySymbol = plugin.getConfigManager().getCurrencySymbol();
            String message = plugin.getConfigManager().translateColorCodes(
                "&a+&f" + String.format("%.1f", experience) + " &aEXP &7| &e+" + currencySymbol + String.format("%.2f", money) + " &7(&6" + jobName + "&7)");
            player.sendMessage(message);
        } else if (experience > 0) {
            String message = plugin.getConfigManager().translateColorCodes(
                "&a+&f" + String.format("%.1f", experience) + " &aEXP &7(&6" + jobName + "&7)");
            player.sendMessage(message);
        }
    }
    
    /**
     * Send bossbar notification with anti-spam (accumulate rewards)
     */
    private void sendBossBarNotification(Player player, String jobName, double experience, double money) {
        UUID playerUUID = player.getUniqueId();
        
        // Cancel existing task if any
        BukkitTask existingTask = rewardTasks.get(playerUUID);
        if (existingTask != null) {
            existingTask.cancel();
        }
        
        // Accumulate rewards
        AccumulatedRewards accumulated = pendingRewards.computeIfAbsent(playerUUID, 
            k -> new AccumulatedRewards());
        accumulated.addReward(experience, money);
        
        // Schedule task to display accumulated rewards
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                AccumulatedRewards rewards = pendingRewards.remove(playerUUID);
                if (rewards != null) {
                    displayBossBar(player, jobName, rewards.totalExperience, rewards.totalMoney);
                }
                rewardTasks.remove(playerUUID);
            }
        }.runTaskLater(plugin, getAccumulationTimeMs() / 50); // Convert ms to ticks (20 ticks = 1 second)
        
        rewardTasks.put(playerUUID, task);
    }
    
    /**
     * Display bossbar with rewards
     */
    private void displayBossBar(Player player, String jobName, double experience, double money) {
        UUID playerUUID = player.getUniqueId();
        
        // Remove existing bossbar if any
        removeBossBar(player);
        
        // Create message
        String message;
        if (experience > 0 && money > 0) {
            String currencySymbol = plugin.getConfigManager().getCurrencySymbol();
            message = plugin.getConfigManager().translateColorCodes(
                "&a+&f" + String.format("%.1f", experience) + " &aEXP &7| &e+" + currencySymbol + String.format("%.2f", money) + " &7(&6" + jobName + "&7)");
        } else if (experience > 0) {
            message = plugin.getConfigManager().translateColorCodes(
                "&a+&f" + String.format("%.1f", experience) + " &aEXP &7(&6" + jobName + "&7)");
        } else {
            return; // No rewards to show
        }
        
        // Create bossbar
        BossBar bossBar = Bukkit.createBossBar(message, BarColor.GREEN, BarStyle.SOLID);
        bossBar.setProgress(1.0);
        bossBar.addPlayer(player);
        
        playerBossBars.put(playerUUID, bossBar);
        
        // Schedule removal
        BukkitTask removeTask = new BukkitRunnable() {
            @Override
            public void run() {
                removeBossBar(player);
            }
        }.runTaskLater(plugin, getBossBarDisplayTimeMs() / 50); // Convert ms to ticks
        
        bossBarTasks.put(playerUUID, removeTask);
    }
    
    /**
     * Remove bossbar for player
     */
    public void removeBossBar(Player player) {
        UUID playerUUID = player.getUniqueId();
        
        BossBar bossBar = playerBossBars.remove(playerUUID);
        if (bossBar != null) {
            bossBar.removeAll();
        }
        
        BukkitTask task = bossBarTasks.remove(playerUUID);
        if (task != null) {
            task.cancel();
        }
    }
    
    /**
     * Clean up when player leaves
     */
    public void onPlayerQuit(Player player) {
        UUID playerUUID = player.getUniqueId();
        
        removeBossBar(player);
        pendingRewards.remove(playerUUID);
        
        BukkitTask rewardTask = rewardTasks.remove(playerUUID);
        if (rewardTask != null) {
            rewardTask.cancel();
        }
    }
    
    /**
     * Clean up all bossbars (for plugin shutdown)
     */
    public void shutdown() {
        // Cancel all tasks
        rewardTasks.values().forEach(BukkitTask::cancel);
        bossBarTasks.values().forEach(BukkitTask::cancel);
        
        // Remove all bossbars
        playerBossBars.values().forEach(BossBar::removeAll);
        
        // Clear maps
        rewardTasks.clear();
        bossBarTasks.clear();
        playerBossBars.clear();
        pendingRewards.clear();
    }
    
    /**
     * Inner class to accumulate rewards for anti-spam
     */
    private static class AccumulatedRewards {
        double totalExperience = 0.0;
        double totalMoney = 0.0;
        
        void addReward(double experience, double money) {
            this.totalExperience += experience;
            this.totalMoney += money;
        }
    }

    /**
     * Send reward notification to player with a simple message
     */
    public void sendRewardNotification(Player player, String rewardMessage) {
        String notificationType = plugin.getConfigManager().getConfig().getString("notifications.type", "bossbar");
        
        if ("chat".equalsIgnoreCase(notificationType)) {
            sendSimpleChatNotification(player, rewardMessage);
        } else {
            sendSimpleBossBarNotification(player, rewardMessage);
        }
    }
    
    /**
     * Send simple chat notification
     */
    private void sendSimpleChatNotification(Player player, String rewardMessage) {
        String message = plugin.getConfigManager().translateColorCodes("&a" + rewardMessage);
        player.sendMessage(message);
    }
    
    /**
     * Send simple bossbar notification
     */
    private void sendSimpleBossBarNotification(Player player, String rewardMessage) {
        UUID playerUUID = player.getUniqueId();
        
        // Remove existing bossbar if any
        removeBossBar(player);
        
        // Create message
        String message = plugin.getConfigManager().translateColorCodes("&a" + rewardMessage);
        
        // Create bossbar
        BossBar bossBar = Bukkit.createBossBar(message, BarColor.GREEN, BarStyle.SOLID);
        bossBar.setProgress(1.0);
        bossBar.addPlayer(player);
        
        playerBossBars.put(playerUUID, bossBar);
        
        // Schedule removal
        BukkitTask removeTask = new BukkitRunnable() {
            @Override
            public void run() {
                removeBossBar(player);
            }
        }.runTaskLater(plugin, getBossBarDisplayTimeMs() / 50); // Convert ms to ticks
        
        bossBarTasks.put(playerUUID, removeTask);
    }
    
    /**
     * Send bossbar notification directly (for salary system and other special notifications)
     */
    public void sendBossbarNotification(Player player, String message) {
        sendSimpleBossBarNotification(player, message);
    }
}
