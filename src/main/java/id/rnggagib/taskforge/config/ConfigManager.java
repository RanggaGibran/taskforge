package id.rnggagib.taskforge.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Logger;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import id.rnggagib.taskforge.TaskForgePlugin;

/**
 * Manages plugin configuration files
 */
public class ConfigManager {
    
    private final TaskForgePlugin plugin;
    private final Logger logger;
    
    private FileConfiguration config;
    private FileConfiguration jobsConfig;
    
    public ConfigManager(TaskForgePlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    /**
     * Load all configuration files
     */
    public void loadConfigs() {
        // Save default config files if they don't exist
        saveDefaultConfig("config.yml");
        saveDefaultConfig("jobs.yml");
        saveDefaultConfig("jobs_index.yml");
        
        // Save default jobs folder
        saveDefaultJobsFolder();
        
        // Load configuration files
        loadConfig();
        loadJobsConfig();
        
        logger.info("Configuration files loaded successfully.");
    }
    
    /**
     * Load main config.yml file
     */
    private void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        config = YamlConfiguration.loadConfiguration(configFile);
    }
    
    /**
     * Load jobs.yml file
     */
    private void loadJobsConfig() {
        File jobsFile = new File(plugin.getDataFolder(), "jobs.yml");
        jobsConfig = YamlConfiguration.loadConfiguration(jobsFile);
    }
    
    /**
     * Save default configuration file from resources
     */
    private void saveDefaultConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            
            try (InputStream inputStream = plugin.getResource(fileName)) {
                if (inputStream != null) {
                    Files.copy(inputStream, file.toPath());
                    logger.info("Created default " + fileName);
                } else {
                    logger.warning("Could not find default " + fileName + " in plugin resources!");
                }
            } catch (IOException e) {
                logger.severe("Failed to save default " + fileName + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Save default jobs folder and individual job files from resources
     */
    private void saveDefaultJobsFolder() {
        File jobsFolder = new File(plugin.getDataFolder(), "jobs");
        
        if (!jobsFolder.exists()) {
            jobsFolder.mkdirs();
            logger.info("Created jobs folder");
        }
        
        // Get job files list from jobs_index.yml
        String[] jobFiles = getJobFilesFromIndex();
        
        for (String jobFile : jobFiles) {
            File file = new File(jobsFolder, jobFile);
            
            if (!file.exists()) {
                try (InputStream inputStream = plugin.getResource("jobs/" + jobFile)) {
                    if (inputStream != null) {
                        Files.copy(inputStream, file.toPath());
                        logger.info("Created default jobs/" + jobFile);
                    } else {
                        logger.warning("Could not find default jobs/" + jobFile + " in plugin resources!");
                    }
                } catch (IOException e) {
                    logger.severe("Failed to save default jobs/" + jobFile + ": " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Get list of job files from jobs_index.yml
     */
    private String[] getJobFilesFromIndex() {
        // Default fallback list
        String[] defaultJobFiles = {"miner.yml", "lumberjack.yml", "farmer.yml", "hunter.yml", "fisherman.yml", "blacksmith.yml"};
        
        File jobsIndexFile = new File(plugin.getDataFolder(), "jobs_index.yml");
        if (!jobsIndexFile.exists()) {
            logger.info("jobs_index.yml not found, using default job files list");
            return defaultJobFiles;
        }
        
        try {
            FileConfiguration jobsIndexConfig = YamlConfiguration.loadConfiguration(jobsIndexFile);
            java.util.List<String> jobNames = jobsIndexConfig.getStringList("jobs");
            
            if (jobNames.isEmpty()) {
                logger.warning("No jobs found in jobs_index.yml, using default list");
                return defaultJobFiles;
            }
            
            // Convert job names to file names (add .yml extension)
            return jobNames.stream()
                    .map(name -> name + ".yml")
                    .toArray(String[]::new);
                    
        } catch (Exception e) {
            logger.warning("Error reading jobs_index.yml, using default list: " + e.getMessage());
            return defaultJobFiles;
        }
    }
    
    /**
     * Get main configuration
     */
    public FileConfiguration getConfig() {
        return config;
    }
    
    /**
     * Get jobs configuration
     */
    public FileConfiguration getJobsConfig() {
        return jobsConfig;
    }
    
    /**
     * Get a message from config with color codes translated
     */
    public String getMessage(String path) {
        String message = config.getString("settings.language." + path, "Message not found: " + path);
        return translateColorCodes(message);
    }
    
    /**
     * Get a message from config with placeholders replaced
     */
    public String getMessage(String path, String... placeholders) {
        String message = getMessage(path);
        
        // Replace placeholders
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                message = message.replace("%" + placeholders[i] + "%", placeholders[i + 1]);
            }
        }
        
        return message;
    }
    
    /**
     * Get prefix from config
     */
    public String getPrefix() {
        return translateColorCodes(config.getString("settings.prefix", "&8[&bTaskForge&8] &r"));
    }
    
    /**
     * Get prefixed message
     */
    public String getPrefixedMessage(String path) {
        return getPrefix() + getMessage(path);
    }
    
    /**
     * Get prefixed message with placeholders
     */
    public String getPrefixedMessage(String path, String... placeholders) {
        return getPrefix() + getMessage(path, placeholders);
    }
    
    /**
     * Translate color codes in a string
     */
    public String translateColorCodes(String text) {
        if (text == null) return "";
        return text.replace('&', '§');
    }
    
    /**
     * Get base EXP required for leveling
     */
    public double getBaseExp() {
        return config.getDouble("leveling.base_exp", 100.0);
    }
    
    /**
     * Get leveling multiplier
     */
    public double getLevelingMultiplier() {
        return config.getDouble("leveling.multiplier", 1.2);
    }
    
    /**
     * Get maximum level
     */
    public int getMaxLevel() {
        return config.getInt("leveling.max_level", 100);
    }
    
    /**
     * Get maximum jobs a player can join
     */
    public int getMaxJobs() {
        return config.getInt("settings.max_jobs", 3);
    }
    
    /**
     * Check if a feature is enabled
     */
    public boolean isFeatureEnabled(String feature) {
        return config.getBoolean("settings.features." + feature, true);
    }
    
    /**
     * Get database type
     */
    public String getDatabaseType() {
        return config.getString("database.type", "SQLITE").toUpperCase();
    }
    
    /**
     * Get SQLite file name
     */
    public String getSQLiteFileName() {
        return config.getString("database.sqlite_file", "taskforge.db");
    }
    
    /**
     * Check if economy is enabled
     */
    public boolean isEconomyEnabled() {
        return config.getBoolean("economy.enabled", true);
    }
    
    /**
     * Get currency symbol
     */
    public String getCurrencySymbol() {
        return config.getString("economy.currency_symbol", "$");
    }
    
    /**
     * Check if debug is enabled
     */
    public boolean isDebugEnabled() {
        return config.getBoolean("debug.enabled", false);
    }
    
    /**
     * Check if action logging is enabled
     */
    public boolean isActionLoggingEnabled() {
        return config.getBoolean("debug.log_actions", false);
    }
    
    /**
     * Check if job cooldown is enabled
     */
    public boolean isJobCooldownEnabled() {
        return config.getBoolean("settings.job_cooldown.enabled", true);
    }
    
    /**
     * Get job leave cooldown time string
     */
    public String getJobLeaveCooldown() {
        return config.getString("settings.job_cooldown.leave_cooldown", "1h");
    }
    
    /**
     * Check if remaining time should be shown in cooldown messages
     */
    public boolean shouldShowRemainingTime() {
        return config.getBoolean("settings.job_cooldown.show_remaining_time", true);
    }
}
