package id.rnggagib.taskforge.managers;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
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
     * Load all jobs from configuration files
     * Supports both separate files (new method) and single jobs.yml (legacy)
     */
    public void loadJobsFromConfig() {
        jobs.clear();
        
        // Check if jobs_index.yml exists and use_separate_files is enabled
        File jobsIndexFile = new File(plugin.getDataFolder(), "jobs_index.yml");
        boolean useSeparateFiles = false;
        
        if (jobsIndexFile.exists()) {
            FileConfiguration jobsIndexConfig = YamlConfiguration.loadConfiguration(jobsIndexFile);
            useSeparateFiles = jobsIndexConfig.getBoolean("settings.use_separate_files", false);
            boolean debugLoading = jobsIndexConfig.getBoolean("settings.debug_loading", false);
            
            if (useSeparateFiles) {
                logger.info("Loading jobs from separate files...");
                loadJobsFromSeparateFiles(jobsIndexConfig, debugLoading);
                return;
            }
        }
        
        // Fall back to legacy single file method
        logger.info("Loading jobs from single jobs.yml file...");
        loadJobsFromSingleFile();
    }
    
    /**
     * Load jobs from separate files (new method)
     */
    private void loadJobsFromSeparateFiles(FileConfiguration jobsIndexConfig, boolean debugLoading) {
        List<String> jobFileNames = jobsIndexConfig.getStringList("jobs");
        if (jobFileNames.isEmpty()) {
            logger.warning("No job files specified in jobs_index.yml!");
            return;
        }
        
        File jobsFolder = new File(plugin.getDataFolder(), "jobs");
        if (!jobsFolder.exists()) {
            logger.severe("Jobs folder does not exist: " + jobsFolder.getPath());
            return;
        }
        
        int loadedCount = 0;
        for (String jobFileName : jobFileNames) {
            File jobFile = new File(jobsFolder, jobFileName + ".yml");
            
            if (!jobFile.exists()) {
                logger.warning("Job file not found: " + jobFile.getPath());
                continue;
            }
            
            try {
                if (debugLoading) {
                    logger.info("Loading job file: " + jobFile.getName());
                }
                
                FileConfiguration jobConfig = YamlConfiguration.loadConfiguration(jobFile);
                Job job = loadJobFromConfig(jobFileName, jobConfig);
                
                if (job != null) {
                    jobs.put(jobFileName.toLowerCase(), job);
                    loadedCount++;
                    logger.info("Loaded job: " + jobFileName);
                } else {
                    logger.warning("Failed to load job from file: " + jobFile.getName());
                }
                
            } catch (Exception e) {
                logger.severe("Error loading job file '" + jobFile.getName() + "': " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        logger.info("Loaded " + loadedCount + "/" + jobFileNames.size() + " jobs from separate files.");
    }
    
    /**
     * Load jobs from single jobs.yml file (legacy method)
     */
    private void loadJobsFromSingleFile() {
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
        
        logger.info("Loaded " + jobs.size() + " jobs from single file.");
    }
    
    /**
     * Load a job from a separate configuration file
     */
    private Job loadJobFromConfig(String jobName, FileConfiguration jobConfig) {
        // Basic job info - read directly from root of config
        String displayName = jobConfig.getString("display_name", jobName);
        String description = jobConfig.getString("description", "No description available.");
        String playerHeadTexture = jobConfig.getString("player_head_texture", "");
        
        // Item icon fallback
        Material itemIcon = Material.STONE;
        String itemIconString = jobConfig.getString("item_icon", "STONE");
        try {
            itemIcon = Material.valueOf(itemIconString.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid item icon '" + itemIconString + "' for job " + jobName + ", using STONE");
        }
        
        Job job = new Job(jobName, displayName, description, playerHeadTexture, itemIcon);
        
        // Load objectives
        loadJobObjectives(job, jobConfig.getConfigurationSection("objectives"));
        
        // Load level rewards
        loadLevelRewards(job, jobConfig.getConfigurationSection("level_rewards"));
        
        return job;
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
                double chance = targetSection.getDouble("chance", 100.0);
                
                // Parse money - support both single value and range format
                JobObjective objective;
                if (targetSection.contains("money")) {
                    Object moneyValue = targetSection.get("money");
                    if (moneyValue instanceof String) {
                        String moneyStr = (String) moneyValue;
                        if (moneyStr.contains("-")) {
                            // Range format: "0.25-0.50"
                            String[] parts = moneyStr.split("-");
                            if (parts.length == 2) {
                                try {
                                    double minMoney = Double.parseDouble(parts[0].trim());
                                    double maxMoney = Double.parseDouble(parts[1].trim());
                                    objective = new JobObjective(exp, minMoney, maxMoney, chance);
                                } catch (NumberFormatException e) {
                                    logger.warning("Invalid money range format in " + job.getName() + 
                                                 " for " + actionTypeName + "." + targetName + ": " + moneyStr);
                                    objective = new JobObjective(exp, 0.0, chance);
                                }
                            } else {
                                logger.warning("Invalid money range format in " + job.getName() + 
                                             " for " + actionTypeName + "." + targetName + ": " + moneyStr);
                                objective = new JobObjective(exp, 0.0, chance);
                            }
                        } else {
                            // Single value as string
                            try {
                                double money = Double.parseDouble(moneyStr);
                                objective = new JobObjective(exp, money, chance);
                            } catch (NumberFormatException e) {
                                logger.warning("Invalid money value in " + job.getName() + 
                                             " for " + actionTypeName + "." + targetName + ": " + moneyStr);
                                objective = new JobObjective(exp, 0.0, chance);
                            }
                        }
                    } else {
                        // Traditional numeric value
                        double money = targetSection.getDouble("money", 0.0);
                        objective = new JobObjective(exp, money, chance);
                    }
                } else {
                    // No money specified
                    objective = new JobObjective(exp, 0.0, chance);
                }
                
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
