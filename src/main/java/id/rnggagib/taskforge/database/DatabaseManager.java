package id.rnggagib.taskforge.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import id.rnggagib.taskforge.TaskForgePlugin;

/**
 * Manages database connections and operations
 */
public class DatabaseManager {
    
    private final TaskForgePlugin plugin;
    private final Logger logger;
    private Connection connection;
    
    public DatabaseManager(TaskForgePlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    /**
     * Initialize database connection and create tables
     */
    public void initializeDatabase() throws SQLException {
        // Create database connection
        createConnection();
        
        // Create tables
        createTables();
        
        logger.info("Database initialized successfully.");
    }
    
    /**
     * Create database connection
     */
    private void createConnection() throws SQLException {
        String databaseType = plugin.getConfigManager().getDatabaseType();
        
        if ("SQLITE".equals(databaseType)) {
            // SQLite connection
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            
            String fileName = plugin.getConfigManager().getSQLiteFileName();
            File databaseFile = new File(dataFolder, fileName);
            String url = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
            
            connection = DriverManager.getConnection(url);
            logger.info("Connected to SQLite database: " + fileName);
        } else {
            throw new SQLException("Unsupported database type: " + databaseType);
        }
    }
    
    /**
     * Create necessary database tables
     */
    private void createTables() throws SQLException {
        // Player jobs table
        String createPlayerJobsTable = 
            "CREATE TABLE IF NOT EXISTS player_jobs (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "player_uuid TEXT NOT NULL," +
            "job_name TEXT NOT NULL," +
            "level INTEGER DEFAULT 1," +
            "experience REAL DEFAULT 0.0," +
            "joined_date INTEGER DEFAULT (strftime('%s', 'now'))," +
            "UNIQUE(player_uuid, job_name)" +
            ")";
        
        // Player stats table
        String createPlayerStatsTable = 
            "CREATE TABLE IF NOT EXISTS player_stats (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "player_uuid TEXT NOT NULL," +
            "job_name TEXT NOT NULL," +
            "total_money_earned REAL DEFAULT 0.0," +
            "total_exp_earned REAL DEFAULT 0.0," +
            "actions_completed INTEGER DEFAULT 0," +
            "UNIQUE(player_uuid, job_name)" +
            ")";
        
        // Player settings table
        String createPlayerSettingsTable = 
            "CREATE TABLE IF NOT EXISTS player_settings (" +
            "player_uuid TEXT PRIMARY KEY," +
            "particles_enabled BOOLEAN DEFAULT 1," +
            "titles_enabled BOOLEAN DEFAULT 1," +
            "level_notifications BOOLEAN DEFAULT 1," +
            "money_notifications BOOLEAN DEFAULT 1," +
            "exp_notifications BOOLEAN DEFAULT 1" +
            ")";
        
        // Job cooldowns table
        String createJobCooldownsTable = 
            "CREATE TABLE IF NOT EXISTS job_cooldowns (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "player_uuid TEXT NOT NULL," +
            "job_name TEXT NOT NULL," +
            "join_timestamp INTEGER NOT NULL," +
            "UNIQUE(player_uuid, job_name)" +
            ")";
        
        // Pending salaries table
        String createPendingSalariesTable = 
            "CREATE TABLE IF NOT EXISTS pending_salaries (" +
            "player_uuid TEXT PRIMARY KEY," +
            "amount REAL NOT NULL DEFAULT 0.0," +
            "last_updated INTEGER DEFAULT (strftime('%s', 'now'))" +
            ")";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createPlayerJobsTable);
            stmt.execute(createPlayerStatsTable);
            stmt.execute(createPlayerSettingsTable);
            stmt.execute(createJobCooldownsTable);
            stmt.execute(createPendingSalariesTable);
            logger.info("Database tables created successfully.");
        }
    }
    
    /**
     * Get database connection
     */
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                createConnection();
            }
        } catch (SQLException e) {
            logger.severe("Failed to get database connection: " + e.getMessage());
        }
        return connection;
    }
    
    /**
     * Close database connection
     */
    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                logger.info("Database connection closed.");
            } catch (SQLException e) {
                logger.severe("Failed to close database connection: " + e.getMessage());
            }
        }
    }
    
    /**
     * Load player's job data from database
     */
    public Map<String, PlayerJobData> loadPlayerJobData(UUID playerUUID) {
        Map<String, PlayerJobData> jobData = new HashMap<>();
        
        String query = "SELECT job_name, level, experience FROM player_jobs WHERE player_uuid = ?";
        
        try (PreparedStatement stmt = getConnection().prepareStatement(query)) {
            stmt.setString(1, playerUUID.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String jobName = rs.getString("job_name");
                    int level = rs.getInt("level");
                    double experience = rs.getDouble("experience");
                    
                    jobData.put(jobName, new PlayerJobData(jobName, level, experience));
                }
            }
        } catch (SQLException e) {
            logger.severe("Failed to load player job data for " + playerUUID + ": " + e.getMessage());
        }
        
        return jobData;
    }
    
    /**
     * Save player's job data to database
     */
    public void savePlayerJobData(UUID playerUUID, String jobName, int level, double experience) {
        String query = "INSERT OR REPLACE INTO player_jobs (player_uuid, job_name, level, experience) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement stmt = getConnection().prepareStatement(query)) {
            stmt.setString(1, playerUUID.toString());
            stmt.setString(2, jobName);
            stmt.setInt(3, level);
            stmt.setDouble(4, experience);
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.severe("Failed to save player job data for " + playerUUID + ": " + e.getMessage());
        }
    }
    
    /**
     * Add job to player
     */
    public void addPlayerJob(UUID playerUUID, String jobName) {
        String query = "INSERT OR IGNORE INTO player_jobs (player_uuid, job_name, level, experience) VALUES (?, ?, 1, 0.0)";
        
        try (PreparedStatement stmt = getConnection().prepareStatement(query)) {
            stmt.setString(1, playerUUID.toString());
            stmt.setString(2, jobName);
            
            stmt.executeUpdate();
            
            // Also initialize stats
            initializePlayerStats(playerUUID, jobName);
        } catch (SQLException e) {
            logger.severe("Failed to add job " + jobName + " for player " + playerUUID + ": " + e.getMessage());
        }
    }
    
    /**
     * Remove job from player
     */
    public void removePlayerJob(UUID playerUUID, String jobName) {
        String deleteJobQuery = "DELETE FROM player_jobs WHERE player_uuid = ? AND job_name = ?";
        String deleteStatsQuery = "DELETE FROM player_stats WHERE player_uuid = ? AND job_name = ?";
        
        try (PreparedStatement stmt1 = getConnection().prepareStatement(deleteJobQuery);
             PreparedStatement stmt2 = getConnection().prepareStatement(deleteStatsQuery)) {
            
            stmt1.setString(1, playerUUID.toString());
            stmt1.setString(2, jobName);
            stmt1.executeUpdate();
            
            stmt2.setString(1, playerUUID.toString());
            stmt2.setString(2, jobName);
            stmt2.executeUpdate();
            
        } catch (SQLException e) {
            logger.severe("Failed to remove job " + jobName + " for player " + playerUUID + ": " + e.getMessage());
        }
    }
    
    /**
     * Initialize player stats for a job
     */
    private void initializePlayerStats(UUID playerUUID, String jobName) {
        String query = "INSERT OR IGNORE INTO player_stats (player_uuid, job_name) VALUES (?, ?)";
        
        try (PreparedStatement stmt = getConnection().prepareStatement(query)) {
            stmt.setString(1, playerUUID.toString());
            stmt.setString(2, jobName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.severe("Failed to initialize stats for " + playerUUID + " job " + jobName + ": " + e.getMessage());
        }
    }
    
    /**
     * Update player statistics
     */
    public void updatePlayerStats(UUID playerUUID, String jobName, double moneyEarned, double expEarned) {
        String query = "UPDATE player_stats SET total_money_earned = total_money_earned + ?, " +
                      "total_exp_earned = total_exp_earned + ?, actions_completed = actions_completed + 1 " +
                      "WHERE player_uuid = ? AND job_name = ?";
        
        try (PreparedStatement stmt = getConnection().prepareStatement(query)) {
            stmt.setDouble(1, moneyEarned);
            stmt.setDouble(2, expEarned);
            stmt.setString(3, playerUUID.toString());
            stmt.setString(4, jobName);
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.severe("Failed to update stats for " + playerUUID + " job " + jobName + ": " + e.getMessage());
        }
    }
    
    /**
     * Get total money earned by player in a specific job
     */
    public double getPlayerEarnings(UUID playerUUID, String jobName) {
        String query = "SELECT total_money_earned FROM player_stats WHERE player_uuid = ? AND job_name = ?";
        
        try (PreparedStatement stmt = getConnection().prepareStatement(query)) {
            stmt.setString(1, playerUUID.toString());
            stmt.setString(2, jobName);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("total_money_earned");
                }
            }
        } catch (SQLException e) {
            logger.severe("Failed to get player earnings for " + playerUUID + " job " + jobName + ": " + e.getMessage());
        }
        
        return 0.0;
    }
    
    /**
     * Get top players for a specific job
     */
    public Map<String, Integer> getTopPlayers(String jobName, int limit) {
        Map<String, Integer> topPlayers = new HashMap<>();
        
        String query = "SELECT player_uuid, level FROM player_jobs WHERE job_name = ? ORDER BY level DESC, experience DESC LIMIT ?";
        
        try (PreparedStatement stmt = getConnection().prepareStatement(query)) {
            stmt.setString(1, jobName);
            stmt.setInt(2, limit);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String playerUUID = rs.getString("player_uuid");
                    int level = rs.getInt("level");
                    topPlayers.put(playerUUID, level);
                }
            }
        } catch (SQLException e) {
            logger.severe("Failed to get top players for job " + jobName + ": " + e.getMessage());
        }
        
        return topPlayers;
    }
    
    /**
     * Record job join timestamp for cooldown tracking
     */
    public void recordJobJoin(UUID playerUUID, String jobName) {
        String sql = "INSERT OR REPLACE INTO job_cooldowns (player_uuid, job_name, join_timestamp) VALUES (?, ?, ?)";
        
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            stmt.setString(2, jobName.toLowerCase());
            stmt.setLong(3, System.currentTimeMillis());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.severe("Failed to record job join for " + playerUUID + ": " + e.getMessage());
        }
    }
    
    /**
     * Get job join timestamp for cooldown checking
     */
    public long getJobJoinTimestamp(UUID playerUUID, String jobName) {
        String sql = "SELECT join_timestamp FROM job_cooldowns WHERE player_uuid = ? AND job_name = ?";
        
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            stmt.setString(2, jobName.toLowerCase());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("join_timestamp");
                }
            }
        } catch (SQLException e) {
            logger.severe("Failed to get job join timestamp for " + playerUUID + ": " + e.getMessage());
        }
        
        return 0; // No timestamp found
    }
    
    /**
     * Remove job cooldown record (when cooldown expires or for cleanup)
     */
    public void removeJobCooldown(UUID playerUUID, String jobName) {
        String sql = "DELETE FROM job_cooldowns WHERE player_uuid = ? AND job_name = ?";
        
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            stmt.setString(2, jobName.toLowerCase());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.severe("Failed to remove job cooldown for " + playerUUID + ": " + e.getMessage());
        }
    }
    
    /**
     * Clean up expired job cooldowns
     */
    public void cleanupExpiredCooldowns(long cooldownDuration) {
        long expiredBefore = System.currentTimeMillis() - cooldownDuration;
        String sql = "DELETE FROM job_cooldowns WHERE join_timestamp < ?";
        
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setLong(1, expiredBefore);
            int deleted = stmt.executeUpdate();
            if (deleted > 0) {
                logger.info("Cleaned up " + deleted + " expired job cooldowns.");
            }
        } catch (SQLException e) {
            logger.severe("Failed to cleanup expired cooldowns: " + e.getMessage());
        }
    }
    
    /**
     * Save pending salary for a player
     */
    public void savePendingSalary(UUID playerUUID, double amount) {
        String sql = "INSERT OR REPLACE INTO pending_salaries (player_uuid, amount, last_updated) VALUES (?, ?, ?)";
        
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            stmt.setDouble(2, amount);
            stmt.setLong(3, System.currentTimeMillis() / 1000); // Unix timestamp
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.severe("Failed to save pending salary for " + playerUUID + ": " + e.getMessage());
        }
    }
    
    /**
     * Load pending salary for a player
     */
    public double loadPendingSalary(UUID playerUUID) {
        String sql = "SELECT amount FROM pending_salaries WHERE player_uuid = ?";
        
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("amount");
                }
            }
        } catch (SQLException e) {
            logger.severe("Failed to load pending salary for " + playerUUID + ": " + e.getMessage());
        }
        
        return 0.0;
    }
    
    /**
     * Load all pending salaries
     */
    public Map<UUID, Double> loadAllPendingSalaries() {
        Map<UUID, Double> pendingSalaries = new HashMap<>();
        String sql = "SELECT player_uuid, amount FROM pending_salaries WHERE amount > 0";
        
        try (PreparedStatement stmt = getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                UUID playerUUID = UUID.fromString(rs.getString("player_uuid"));
                double amount = rs.getDouble("amount");
                pendingSalaries.put(playerUUID, amount);
            }
        } catch (SQLException e) {
            logger.severe("Failed to load all pending salaries: " + e.getMessage());
        }
        
        return pendingSalaries;
    }
    
    /**
     * Delete pending salary for a player (after payout)
     */
    public void deletePendingSalary(UUID playerUUID) {
        String sql = "DELETE FROM pending_salaries WHERE player_uuid = ?";
        
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.severe("Failed to delete pending salary for " + playerUUID + ": " + e.getMessage());
        }
    }

    /**
     * Data class for player job information
     */
    public static class PlayerJobData {
        private final String jobName;
        private int level;
        private double experience;
        
        public PlayerJobData(String jobName, int level, double experience) {
            this.jobName = jobName;
            this.level = level;
            this.experience = experience;
        }
        
        public String getJobName() { return jobName; }
        public int getLevel() { return level; }
        public double getExperience() { return experience; }
        
        public void setLevel(int level) { this.level = level; }
        public void setExperience(double experience) { this.experience = experience; }
    }
}
