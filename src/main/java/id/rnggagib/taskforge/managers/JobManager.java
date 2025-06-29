package id.rnggagib.taskforge.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;

import id.rnggagib.taskforge.TaskForgePlugin;
import id.rnggagib.taskforge.jobs.Job;
import id.rnggagib.taskforge.jobs.Job.ActionType;
import id.rnggagib.taskforge.jobs.JobObjective;
import id.rnggagib.taskforge.jobs.JobReward;
import id.rnggagib.taskforge.jobs.JobReward.RewardType;

/**
 * Manages all jobs and their configuration
 */
public class JobManager {
    
    private final TaskForgePlugin plugin;
    private final Logger logger;
    private final Map<String, Job> jobs;
    
    public JobManager(TaskForgePlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.jobs = new HashMap<>();
    }
    
    /**
     * Load all jobs from the jobs.yml configuration file
     */
    public void loadJobsFromConfig() {
        jobs.clear();
        
        FileConfiguration jobsConfig = plugin.getConfigManager().getJobsConfig();
        ConfigurationSection jobsSection = jobsConfig.getConfigurationSection("jobs");
        
        if (jobsSection == null) {
            logger.warning("No jobs section found in jobs.yml!");
            return;
        }
        
        for (String jobName : jobsSection.getKeys(false)) {
            try {
                Job job = loadJob(jobName, jobsSection.getConfigurationSection(jobName));
                if (job != null) {
                    jobs.put(jobName.toLowerCase(), job);
                    logger.info("Loaded job: " + jobName);
                }
            } catch (Exception e) {
                logger.severe("Failed to load job '" + jobName + "': " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        logger.info("Loaded " + jobs.size() + " jobs successfully.");
    }
    
    /**
     * Load a single job from configuration
     */
    private Job loadJob(String jobName, ConfigurationSection jobSection) {
        if (jobSection == null) return null;
        
        // Basic job info
        String displayName = jobSection.getString("display_name", jobName);
        String description = jobSection.getString("description", "No description available.");
        String playerHeadTexture = jobSection.getString("player_head_texture", "");
        
        // Item icon fallback
        Material itemIcon = Material.STONE;
        String itemIconString = jobSection.getString("item_icon", "STONE");
        try {
            itemIcon = Material.valueOf(itemIconString.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid item icon '" + itemIconString + "' for job " + jobName + ", using STONE");
        }
        
        Job job = new Job(jobName, displayName, description, playerHeadTexture, itemIcon);
        
        // Load objectives
        loadJobObjectives(job, jobSection.getConfigurationSection("objectives"));
        
        // Load level rewards
        loadLevelRewards(job, jobSection.getConfigurationSection("level_rewards"));
        
        return job;
    }
    
    /**
     * Load objectives for a job
     */
    private void loadJobObjectives(Job job, ConfigurationSection objectivesSection) {
        if (objectivesSection == null) return;
        
        for (String actionTypeName : objectivesSection.getKeys(false)) {
            ActionType actionType;
            try {
                actionType = ActionType.valueOf(actionTypeName.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warning("Unknown action type: " + actionTypeName);
                continue;
            }
            
            ConfigurationSection actionSection = objectivesSection.getConfigurationSection(actionTypeName);
            if (actionSection == null) continue;
            
            for (String targetName : actionSection.getKeys(false)) {
                ConfigurationSection targetSection = actionSection.getConfigurationSection(targetName);
                if (targetSection == null) continue;
                
                double exp = targetSection.getDouble("exp", 0.0);
                double money = targetSection.getDouble("money", 0.0);
                double chance = targetSection.getDouble("chance", 100.0);
                
                JobObjective objective = new JobObjective(exp, money, chance);
                
                // Determine target object type
                Object target = parseTarget(actionType, targetName);
                if (target != null) {
                    job.addObjective(actionType, target, objective);
                }
            }
        }
    }
    
    /**
     * Parse target string to appropriate object type
     */
    private Object parseTarget(ActionType actionType, String targetName) {
        switch (actionType) {
            case BREAK:
            case PLACE:
            case CRAFT:
            case SMELT:
                // Try to parse as Material
                try {
                    return Material.valueOf(targetName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    logger.warning("Unknown material: " + targetName);
                    return null;
                }
                
            case KILL:
            case BREED:
            case TAME:
                // Try to parse as EntityType
                try {
                    return EntityType.valueOf(targetName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    logger.warning("Unknown entity type: " + targetName);
                    return null;
                }
                
            case FISH:
            case ENCHANT:
            case BREW:
            case BRUSH:
                // For these, use the string directly or try Material
                try {
                    return Material.valueOf(targetName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    // Return as string if not a material
                    return targetName.toUpperCase();
                }
                
            default:
                return targetName.toUpperCase();
        }
    }
    
    /**
     * Load level rewards for a job
     */
    private void loadLevelRewards(Job job, ConfigurationSection rewardsSection) {
        if (rewardsSection == null) return;
        
        for (String levelString : rewardsSection.getKeys(false)) {
            try {
                int level = Integer.parseInt(levelString);
                ConfigurationSection rewardSection = rewardsSection.getConfigurationSection(levelString);
                if (rewardSection == null) continue;
                
                String typeString = rewardSection.getString("type", "MONEY");
                String value = rewardSection.getString("value", "0");
                String message = rewardSection.getString("message", "Congratulations!");
                
                RewardType rewardType;
                try {
                    rewardType = RewardType.valueOf(typeString.toUpperCase());
                } catch (IllegalArgumentException e) {
                    logger.warning("Unknown reward type: " + typeString);
                    continue;
                }
                
                JobReward reward = new JobReward(rewardType, value, message);
                job.addLevelReward(level, reward);
                
            } catch (NumberFormatException e) {
                logger.warning("Invalid level number: " + levelString);
            }
        }
    }
    
    /**
     * Get a job by name (case-insensitive)
     */
    public Job getJob(String jobName) {
        return jobs.get(jobName.toLowerCase());
    }
    
    /**
     * Get all available jobs
     */
    public Map<String, Job> getAllJobs() {
        return new HashMap<>(jobs);
    }
    
    /**
     * Get all job names
     */
    public Set<String> getJobNames() {
        return jobs.keySet();
    }
    
    /**
     * Check if a job exists
     */
    public boolean jobExists(String jobName) {
        return jobs.containsKey(jobName.toLowerCase());
    }
    
    /**
     * Get the number of loaded jobs
     */
    public int getJobCount() {
        return jobs.size();
    }
    
    /**
     * Get an objective for a specific job action
     */
    public JobObjective getObjective(String jobName, ActionType actionType, Object target) {
        Job job = getJob(jobName);
        if (job == null) return null;
        
        return job.getObjective(actionType, target);
    }
}
