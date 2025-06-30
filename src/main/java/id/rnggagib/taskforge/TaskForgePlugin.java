package id.rnggagib.taskforge;

import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import id.rnggagib.taskforge.commands.JobsCommand;
import id.rnggagib.taskforge.commands.TaskForgeAdminCommand;
import id.rnggagib.taskforge.config.ConfigManager;
import id.rnggagib.taskforge.database.DatabaseManager;
import id.rnggagib.taskforge.listeners.JobListener;
import id.rnggagib.taskforge.managers.JobManager;
import id.rnggagib.taskforge.managers.NotificationManager;
import id.rnggagib.taskforge.managers.PlayerDataManager;
import id.rnggagib.taskforge.managers.SalaryManager;
import id.rnggagib.taskforge.placeholders.TaskForgePlaceholderExpansion;
import net.milkbowl.vault.economy.Economy;

/**
 * TaskForge - A comprehensive Jobs plugin for Minecraft
 * Main plugin class that handles initialization and core functionality
 */
public class TaskForgePlugin extends JavaPlugin {
    
    private static final Logger LOGGER = Logger.getLogger("TaskForge");
    private static TaskForgePlugin instance;
    
    // Core components
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private JobManager jobManager;
    private PlayerDataManager playerDataManager;
    private NotificationManager notificationManager;
    private SalaryManager salaryManager;
    
    // Economy integration
    private Economy economy = null;
    private boolean economyEnabled = false;
    
    @Override
    public void onEnable() {
        instance = this;
        
        LOGGER.info("TaskForge is starting up...");
        
        // Initialize configuration
        initializeConfig();
        
        // Setup Vault economy
        setupEconomy();
        
        // Initialize database
        initializeDatabase();
        
        // Initialize managers
        initializeManagers();
        
        // Register commands
        registerCommands();
        
        // Register event listeners
        registerListeners();
        
        // Setup PlaceholderAPI if available
        setupPlaceholderAPI();
        
        // Setup scheduled tasks
        setupScheduledTasks();
        
        LOGGER.info("TaskForge has been enabled successfully!");
    }
    
    @Override
    public void onDisable() {
        LOGGER.info("TaskForge is shutting down...");
        
        // Shutdown salary manager
        if (salaryManager != null) {
            salaryManager.shutdown();
        }
        
        // Clean up notifications
        if (notificationManager != null) {
            notificationManager.shutdown();
        }
        
        // Save all player data
        if (playerDataManager != null) {
            playerDataManager.saveAllPlayerData();
        }
        
        // Close database connection
        if (databaseManager != null) {
            databaseManager.closeConnection();
        }
        
        LOGGER.info("TaskForge has been disabled.");
    }
    
    /**
     * Initialize configuration manager and load config files
     */
    private void initializeConfig() {
        try {
            configManager = new ConfigManager(this);
            configManager.loadConfigs();
            LOGGER.info("Configuration loaded successfully.");
        } catch (Exception e) {
            LOGGER.severe("Failed to load configuration: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    /**
     * Setup Vault economy integration
     */
    private void setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            LOGGER.warning("Vault not found! Economy features will be disabled.");
            return;
        }
        
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            LOGGER.warning("No economy plugin found! Economy features will be disabled.");
            return;
        }
        
        economy = rsp.getProvider();
        economyEnabled = true;
        LOGGER.info("Economy integration enabled with " + economy.getName());
    }
    
    /**
     * Initialize database manager and create tables
     */
    private void initializeDatabase() {
        try {
            databaseManager = new DatabaseManager(this);
            databaseManager.initializeDatabase();
            LOGGER.info("Database initialized successfully.");
        } catch (Exception e) {
            LOGGER.severe("Failed to initialize database: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    /**
     * Initialize all managers
     */
    private void initializeManagers() {
        try {
            jobManager = new JobManager(this);
            playerDataManager = new PlayerDataManager(this);
            notificationManager = new NotificationManager(this);
            salaryManager = new SalaryManager(this);
            
            // Load jobs from configuration
            jobManager.loadJobsFromConfig();
            
            // Initialize salary manager
            salaryManager.initialize();
            
            LOGGER.info("Managers initialized successfully.");
        } catch (Exception e) {
            LOGGER.severe("Failed to initialize managers: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    /**
     * Register plugin commands
     */
    private void registerCommands() {
        getCommand("jobs").setExecutor(new JobsCommand(this));
        getCommand("taskforgeadmin").setExecutor(new TaskForgeAdminCommand(this));
        LOGGER.info("Commands registered successfully.");
    }
    
    /**
     * Register event listeners
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new JobListener(this), this);
        LOGGER.info("Event listeners registered successfully.");
    }
    
    /**
     * Setup PlaceholderAPI integration if available
     */
    private void setupPlaceholderAPI() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new TaskForgePlaceholderExpansion(this).register();
            LOGGER.info("PlaceholderAPI integration enabled.");
        } else {
            LOGGER.info("PlaceholderAPI not found, placeholder features disabled.");
        }
    }
    
    /**
     * Setup scheduled tasks for plugin maintenance
     */
    private void setupScheduledTasks() {
        // Job cooldown cleanup task - runs every hour
        if (configManager.isJobCooldownEnabled()) {
            String cooldownString = configManager.getJobLeaveCooldown();
            long cooldownDuration = id.rnggagib.taskforge.utils.TimeUtils.parseTimeToMillis(cooldownString);
            
            // Schedule cleanup task to run every hour (72000 ticks)
            Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
                try {
                    databaseManager.cleanupExpiredCooldowns(cooldownDuration);
                } catch (Exception e) {
                    LOGGER.warning("Error during cooldown cleanup: " + e.getMessage());
                }
            }, 72000L, 72000L); // 1 hour = 72000 ticks
            
            LOGGER.info("Job cooldown cleanup task scheduled (every hour).");
        }
    }
    
    // Getters for other classes to access managers
    public static TaskForgePlugin getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public JobManager getJobManager() {
        return jobManager;
    }
    
    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }
    
    public NotificationManager getNotificationManager() {
        return notificationManager;
    }
    
    public SalaryManager getSalaryManager() {
        return salaryManager;
    }
    
    public Economy getEconomy() {
        return economy;
    }
    
    public boolean isEconomyEnabled() {
        return economyEnabled;
    }
    
    /**
     * Reload plugin configuration and data
     */
    public void reloadPlugin() {
        try {
            // Reload configuration
            configManager.loadConfigs();
            
            // Reload jobs
            jobManager.loadJobsFromConfig();
            
            LOGGER.info("Plugin reloaded successfully.");
        } catch (Exception e) {
            LOGGER.severe("Failed to reload plugin: " + e.getMessage());
        }
    }
}
