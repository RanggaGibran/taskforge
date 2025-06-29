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
}
