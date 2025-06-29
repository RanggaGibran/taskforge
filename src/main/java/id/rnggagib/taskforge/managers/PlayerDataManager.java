package id.rnggagib.taskforge.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import id.rnggagib.taskforge.TaskForgePlugin;
import id.rnggagib.taskforge.database.DatabaseManager.PlayerJobData;
import id.rnggagib.taskforge.jobs.Job;
import id.rnggagib.taskforge.jobs.JobReward;

/**
 * Manages player data, including job progress and statistics
 */
public class PlayerDataManager {
    
    private final TaskForgePlugin plugin;
    private final Logger logger;
    
    // Cache of player job data
    private final Map<UUID, Map<String, PlayerJobData>> playerJobData;
    
    public PlayerDataManager(TaskForgePlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.playerJobData = new ConcurrentHashMap<>();
    }
    
    /**
     * Load player data when they join the server
     */
    public void loadPlayerData(UUID playerUUID) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<String, PlayerJobData> jobData = plugin.getDatabaseManager().loadPlayerJobData(playerUUID);
            playerJobData.put(playerUUID, jobData);
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                logger.info("Loaded data for player " + playerUUID + " with " + jobData.size() + " jobs");
            }
        });
    }
    
    /**
     * Save player data when they leave the server
     */
    public void savePlayerData(UUID playerUUID) {
        Map<String, PlayerJobData> jobData = playerJobData.get(playerUUID);
        if (jobData == null) return;
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            for (PlayerJobData data : jobData.values()) {
                plugin.getDatabaseManager().savePlayerJobData(
                    playerUUID, 
                    data.getJobName(), 
                    data.getLevel(), 
                    data.getExperience()
                );
            }
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                logger.info("Saved data for player " + playerUUID);
            }
        });
    }
    
    /**
     * Save all player data (for plugin shutdown)
     */
    public void saveAllPlayerData() {
        logger.info("Saving all player data...");
        
        for (UUID playerUUID : playerJobData.keySet()) {
            savePlayerData(playerUUID);
        }
        
        // Wait a bit for async tasks to complete
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        logger.info("All player data saved.");
    }
    
    /**
     * Unload player data when they leave
     */
    public void unloadPlayerData(UUID playerUUID) {
        savePlayerData(playerUUID);
        playerJobData.remove(playerUUID);
    }
    
    /**
     * Check if player has joined a specific job
     */
    public boolean hasJob(UUID playerUUID, String jobName) {
        Map<String, PlayerJobData> jobData = playerJobData.get(playerUUID);
        return jobData != null && jobData.containsKey(jobName.toLowerCase());
    }
    
    /**
     * Get player's jobs
     */
    public Set<String> getPlayerJobs(UUID playerUUID) {
        Map<String, PlayerJobData> jobData = playerJobData.get(playerUUID);
        return jobData != null ? jobData.keySet() : new HashMap<String, PlayerJobData>().keySet();
    }
    
    /**
     * Join a job
     */
    public boolean joinJob(UUID playerUUID, String jobName) {
        // Check if job exists
        if (!plugin.getJobManager().jobExists(jobName)) {
            return false;
        }
        
        // Check if already has job
        if (hasJob(playerUUID, jobName)) {
            return false;
        }
        
        // Check max jobs limit
        int maxJobs = plugin.getConfigManager().getMaxJobs();
        if (getPlayerJobs(playerUUID).size() >= maxJobs) {
            return false;
        }
        
        // Add to database
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().addPlayerJob(playerUUID, jobName);
        });
        
        // Add to cache
        Map<String, PlayerJobData> jobData = playerJobData.computeIfAbsent(playerUUID, k -> new HashMap<>());
        jobData.put(jobName.toLowerCase(), new PlayerJobData(jobName, 1, 0.0));
        
        return true;
    }
    
    /**
     * Leave a job
     */
    public boolean leaveJob(UUID playerUUID, String jobName) {
        if (!hasJob(playerUUID, jobName)) {
            return false;
        }
        
        // Remove from database
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().removePlayerJob(playerUUID, jobName);
        });
        
        // Remove from cache
        Map<String, PlayerJobData> jobData = playerJobData.get(playerUUID);
        if (jobData != null) {
            jobData.remove(jobName.toLowerCase());
        }
        
        return true;
    }
    
    /**
     * Remove a job (alias for leaveJob for consistency)
     */
    public boolean removeJob(UUID playerUUID, String jobName) {
        return leaveJob(playerUUID, jobName);
    }
    
    /**
     * Add a job (alias for joinJob for consistency)  
     */
    public boolean addJob(UUID playerUUID, String jobName) {
        return joinJob(playerUUID, jobName);
    }
    
    /**
     * Get player's level in a job
     */
    public int getJobLevel(UUID playerUUID, String jobName) {
        Map<String, PlayerJobData> jobData = playerJobData.get(playerUUID);
        if (jobData == null) return 0;
        
        PlayerJobData data = jobData.get(jobName.toLowerCase());
        return data != null ? data.getLevel() : 0;
    }
    
    /**
     * Get player's experience in a job
     */
    public double getJobExperience(UUID playerUUID, String jobName) {
        Map<String, PlayerJobData> jobData = playerJobData.get(playerUUID);
        if (jobData == null) return 0.0;
        
        PlayerJobData data = jobData.get(jobName.toLowerCase());
        return data != null ? data.getExperience() : 0.0;
    }
    
    /**
     * Add experience to a job and handle leveling
     */
    public void addJobExperience(UUID playerUUID, String jobName, double experience) {
        Map<String, PlayerJobData> jobData = playerJobData.get(playerUUID);
        if (jobData == null) return;
        
        PlayerJobData data = jobData.get(jobName.toLowerCase());
        if (data == null) return;
        
        double currentExp = data.getExperience();
        int currentLevel = data.getLevel();
        
        // Add experience
        double newExp = currentExp + experience;
        data.setExperience(newExp);
        
        // Check for level up
        int newLevel = calculateLevelFromExp(newExp);
        if (newLevel > currentLevel) {
            data.setLevel(newLevel);
            handleLevelUp(playerUUID, jobName, currentLevel, newLevel);
        }
        
        // Save to database
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().savePlayerJobData(playerUUID, jobName, data.getLevel(), data.getExperience());
        });
    }
    
    /**
     * Calculate level from total experience
     */
    public int calculateLevelFromExp(double totalExp) {
        double baseExp = plugin.getConfigManager().getBaseExp();
        double multiplier = plugin.getConfigManager().getLevelingMultiplier();
        int maxLevel = plugin.getConfigManager().getMaxLevel();
        
        int level = 1;
        double expRequired = baseExp;
        double expUsed = 0;
        
        while (expUsed + expRequired <= totalExp && level < maxLevel) {
            expUsed += expRequired;
            level++;
            expRequired *= multiplier;
        }
        
        return level;
    }
    
    /**
     * Calculate experience required for next level
     */
    public double getExpRequiredForNextLevel(int currentLevel) {
        if (currentLevel >= plugin.getConfigManager().getMaxLevel()) {
            return 0; // Max level reached
        }
        
        double baseExp = plugin.getConfigManager().getBaseExp();
        double multiplier = plugin.getConfigManager().getLevelingMultiplier();
        
        return baseExp * Math.pow(multiplier, currentLevel - 1);
    }
    
    /**
     * Calculate total experience required to reach a specific level
     */
    public double getExperienceForLevel(int targetLevel) {
        if (targetLevel <= 1) return 0.0;
        
        double totalExp = 0;
        double baseExp = plugin.getConfigManager().getBaseExp();
        double multiplier = plugin.getConfigManager().getLevelingMultiplier();
        
        for (int i = 1; i < targetLevel; i++) {
            totalExp += baseExp * Math.pow(multiplier, i - 1);
        }
        
        return totalExp;
    }
    
    /**
     * Get experience progress to next level (0.0 to 1.0)
     */
    public double getLevelProgress(UUID playerUUID, String jobName) {
        int level = getJobLevel(playerUUID, jobName);
        double currentExp = getJobExperience(playerUUID, jobName);
        
        // Calculate exp used for current level
        double expForCurrentLevel = 0;
        double baseExp = plugin.getConfigManager().getBaseExp();
        double multiplier = plugin.getConfigManager().getLevelingMultiplier();
        
        for (int i = 1; i < level; i++) {
            expForCurrentLevel += baseExp * Math.pow(multiplier, i - 1);
        }
        
        double expInCurrentLevel = currentExp - expForCurrentLevel;
        double expRequiredForNextLevel = getExpRequiredForNextLevel(level);
        
        if (expRequiredForNextLevel <= 0) return 1.0; // Max level
        
        return Math.min(1.0, expInCurrentLevel / expRequiredForNextLevel);
    }
    
    /**
     * Handle level up event
     */
    private void handleLevelUp(UUID playerUUID, String jobName, int oldLevel, int newLevel) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null) return;
        
        // Send level up message
        String message = plugin.getConfigManager().getPrefixedMessage("job_levelup", 
                                                                     "job", jobName, 
                                                                     "level", String.valueOf(newLevel));
        player.sendMessage(message);
        
        // Check for level rewards
        Job job = plugin.getJobManager().getJob(jobName);
        if (job != null && job.hasLevelReward(newLevel)) {
            JobReward reward = job.getLevelReward(newLevel);
            giveReward(player, reward);
        }
        
        // Log level up if debug enabled
        if (plugin.getConfigManager().isDebugEnabled()) {
            logger.info(player.getName() + " leveled up in " + jobName + " from " + oldLevel + " to " + newLevel);
        }
    }
    
    /**
     * Give a reward to a player
     */
    private void giveReward(Player player, JobReward reward) {
        switch (reward.getType()) {
            case MONEY:
                if (plugin.isEconomyEnabled()) {
                    double amount = reward.getNumericValue();
                    plugin.getEconomy().depositPlayer(player, amount);
                }
                break;
                
            case COMMAND:
                String command = reward.getValue().replace("%player%", player.getName());
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                });
                break;
                
            case EFFECT:
                // Handle particle effects, titles, etc.
                // This would be implemented based on the specific effect system
                break;
                
            case TITLE:
                // Handle title/prefix rewards
                // This would integrate with a permissions plugin
                break;
                
            case ITEM:
                // Handle item rewards
                // This would give items to the player
                break;
        }
        
        // Send reward message
        String message = plugin.getConfigManager().translateColorCodes(reward.getMessage());
        player.sendMessage(message);
    }
}
