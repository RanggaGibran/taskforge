package id.rnggagib.taskforge.managers;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import id.rnggagib.taskforge.TaskForgePlugin;

/**
 * Manages accumulated salary payments for job rewards
 * Pays out accumulated earnings at configurable intervals instead of instant payments
 */
public class SalaryManager {
    
    private final TaskForgePlugin plugin;
    
    // Map to store pending salary amounts per player
    private final Map<UUID, Double> pendingSalaries = new ConcurrentHashMap<>();
    
    // Task for periodic salary payouts
    private BukkitTask payoutTask;
    
    public SalaryManager(TaskForgePlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Initialize the salary manager and start periodic payouts
     */
    public void initialize() {
        if (!isSalarySystemEnabled()) {
            plugin.getLogger().info("Salary system is disabled");
            return;
        }
        
        // Load pending salaries from database
        loadPendingSalaries();
        
        // Start periodic payout task
        startPayoutTask();
        
        plugin.getLogger().info("Salary system initialized with " + getPayoutIntervalMinutes() + " minute payout interval");
    }
    
    /**
     * Shutdown the salary manager
     */
    public void shutdown() {
        // Cancel payout task
        if (payoutTask != null) {
            payoutTask.cancel();
            payoutTask = null;
        }
        
        // Save all pending salaries to database
        savePendingSalaries();
        
        plugin.getLogger().info("Salary system shutdown - all pending salaries saved");
    }
    
    /**
     * Add money to a player's pending salary
     */
    public void addPendingSalary(UUID playerUUID, double amount) {
        if (!isSalarySystemEnabled()) {
            // If salary system is disabled, pay immediately
            Player player = Bukkit.getPlayer(playerUUID);
            if (player != null && plugin.isEconomyEnabled()) {
                plugin.getEconomy().depositPlayer(player, amount);
                
                // Send notification for immediate payment
                String currencySymbol = plugin.getConfigManager().getCurrencySymbol();
                String message = "+" + currencySymbol + String.format("%.2f", amount);
                if (plugin.getConfigManager().isFeatureEnabled("money_notifications")) {
                    plugin.getNotificationManager().sendRewardNotification(player, message);
                }
            }
            return;
        }
        
        if (amount <= 0) return;
        
        // Add to pending salary
        pendingSalaries.merge(playerUUID, amount, Double::sum);
        
        // Save to database for persistence
        plugin.getDatabaseManager().savePendingSalary(playerUUID, pendingSalaries.get(playerUUID));
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Added $" + String.format("%.2f", amount) + 
                " to pending salary for " + playerUUID + 
                " (total pending: $" + String.format("%.2f", pendingSalaries.get(playerUUID)) + ")");
        }
    }
    
    /**
     * Get a player's pending salary amount
     */
    public double getPendingSalary(UUID playerUUID) {
        return pendingSalaries.getOrDefault(playerUUID, 0.0);
    }
    
    /**
     * Clear a player's pending salary (used after payout)
     */
    private void clearPendingSalary(UUID playerUUID) {
        pendingSalaries.remove(playerUUID);
        plugin.getDatabaseManager().deletePendingSalary(playerUUID);
    }
    
    /**
     * Check if salary system is enabled
     */
    public boolean isSalarySystemEnabled() {
        return plugin.getConfigManager().getConfig().getBoolean("salary_system.enabled", true);
    }
    
    /**
     * Get payout interval in minutes
     */
    private int getPayoutIntervalMinutes() {
        return plugin.getConfigManager().getConfig().getInt("salary_system.payout_interval_minutes", 5);
    }
    
    /**
     * Check if salary notifications are enabled
     */
    private boolean isSalaryNotificationsEnabled() {
        return plugin.getConfigManager().getConfig().getBoolean("salary_system.notifications", true);
    }
    
    /**
     * Start periodic payout task
     */
    private void startPayoutTask() {
        int intervalMinutes = getPayoutIntervalMinutes();
        long intervalTicks = intervalMinutes * 60 * 20L; // Convert minutes to ticks
        
        payoutTask = new BukkitRunnable() {
            @Override
            public void run() {
                processAllPayouts();
            }
        }.runTaskTimer(plugin, intervalTicks, intervalTicks);
        
        plugin.getLogger().info("Salary payout task started with " + intervalMinutes + " minute interval");
    }
    
    /**
     * Process payouts for all players with pending salaries
     */
    private void processAllPayouts() {
        if (pendingSalaries.isEmpty()) {
            return;
        }
        
        int payoutsProcessed = 0;
        double totalPaidOut = 0.0;
        
        // Process each player's pending salary
        for (Map.Entry<UUID, Double> entry : pendingSalaries.entrySet()) {
            UUID playerUUID = entry.getKey();
            double amount = entry.getValue();
            
            if (amount <= 0) continue;
            
            Player player = Bukkit.getPlayer(playerUUID);
            if (player != null && player.isOnline()) {
                // Player is online, pay them
                if (plugin.isEconomyEnabled()) {
                    plugin.getEconomy().depositPlayer(player, amount);
                    
                    // Send salary notification
                    if (isSalaryNotificationsEnabled()) {
                        sendSalaryNotification(player, amount);
                    }
                    
                    payoutsProcessed++;
                    totalPaidOut += amount;
                    
                    if (plugin.getConfigManager().isDebugEnabled()) {
                        plugin.getLogger().info("Paid salary of $" + String.format("%.2f", amount) + 
                            " to " + player.getName());
                    }
                }
                
                // Clear the pending salary
                clearPendingSalary(playerUUID);
            }
            // If player is offline, keep their pending salary for next payout
        }
        
        if (payoutsProcessed > 0) {
            plugin.getLogger().info("Processed " + payoutsProcessed + " salary payouts totaling $" + 
                String.format("%.2f", totalPaidOut));
        }
    }
    
    /**
     * Send salary notification to player
     */
    private void sendSalaryNotification(Player player, double amount) {
        String currencySymbol = plugin.getConfigManager().getCurrencySymbol();
        String salaryMessage = plugin.getConfigManager().getConfig().getString("salary_system.payout_message", 
            "&a💰 Salary Payout: &e%amount%");

        // Replace placeholders
        salaryMessage = salaryMessage.replace("%amount%", currencySymbol + String.format("%.2f", amount));
        // FIX: Translate color codes before sending!
        salaryMessage = plugin.getConfigManager().translateColorCodes(salaryMessage);

        String notificationType = plugin.getConfigManager().getConfig().getString("salary_system.notification_type", "chat");
        if ("bossbar".equalsIgnoreCase(notificationType)) {
            plugin.getNotificationManager().sendBossbarNotification(player, salaryMessage);
        } else {
            player.sendMessage(salaryMessage); // Sudah berwarna!
        }
    }
    
    /**
     * Load pending salaries from database
     */
    private void loadPendingSalaries() {
        Map<UUID, Double> loadedSalaries = plugin.getDatabaseManager().loadAllPendingSalaries();
        pendingSalaries.putAll(loadedSalaries);
        
        if (!loadedSalaries.isEmpty()) {
            plugin.getLogger().info("Loaded " + loadedSalaries.size() + " pending salaries from database");
        }
    }
    
    /**
     * Save all pending salaries to database
     */
    private void savePendingSalaries() {
        for (Map.Entry<UUID, Double> entry : pendingSalaries.entrySet()) {
            plugin.getDatabaseManager().savePendingSalary(entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * Force payout for a specific player (admin command)
     */
    public boolean forcePayoutPlayer(UUID playerUUID) {
        Double pendingAmount = pendingSalaries.get(playerUUID);
        if (pendingAmount == null || pendingAmount <= 0) {
            return false; // No pending salary
        }
        
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null || !player.isOnline()) {
            return false; // Player not online
        }
        
        if (plugin.isEconomyEnabled()) {
            plugin.getEconomy().depositPlayer(player, pendingAmount);
            
            // Send notification
            if (isSalaryNotificationsEnabled()) {
                sendSalaryNotification(player, pendingAmount);
            }
            
            // Clear pending salary
            clearPendingSalary(playerUUID);
            
            plugin.getLogger().info("Force paid salary of $" + String.format("%.2f", pendingAmount) + 
                " to " + player.getName());
            return true;
        }
        
        return false;
    }
    
    /**
     * Get total pending salaries across all players (for admin info)
     */
    public double getTotalPendingSalaries() {
        return pendingSalaries.values().stream().mapToDouble(Double::doubleValue).sum();
    }
    
    /**
     * Get number of players with pending salaries
     */
    public int getPendingPlayersCount() {
        return (int) pendingSalaries.entrySet().stream().filter(entry -> entry.getValue() > 0).count();
    }
}
