package id.rnggagib.taskforge.managers;

import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BoosterManager {
    
    // Global boosters by type
    private final Map<String, Double> globalBoosters = new HashMap<>();
    
    // Personal boosters by player UUID and type
    private final Map<UUID, Map<String, Double>> personalBoosters = new HashMap<>();
    
    public BoosterManager() {
        // Initialize default boosters
        globalBoosters.put("exp", 1.0);
        globalBoosters.put("money", 1.0);
    }
    
    /**
     * Set a global booster multiplier for a specific type
     * @param type The booster type (exp, money, etc.)
     * @param multiplier The multiplier value (1.0 = no boost, 2.0 = 2x, etc.)
     */
    public void setGlobalBooster(String type, double multiplier) {
        globalBoosters.put(type.toLowerCase(), multiplier);
    }
    
    /**
     * Set a personal booster multiplier for a specific player and type
     * @param player The player to set the booster for
     * @param type The booster type (exp, money, etc.)
     * @param multiplier The multiplier value (1.0 = no boost, 2.0 = 2x, etc.)
     */
    public void setPersonalBooster(Player player, String type, double multiplier) {
        setPersonalBooster(player.getUniqueId(), type, multiplier);
    }
    
    /**
     * Set a personal booster multiplier for a specific player UUID and type
     * @param playerUUID The player UUID to set the booster for
     * @param type The booster type (exp, money, etc.)
     * @param multiplier The multiplier value (1.0 = no boost, 2.0 = 2x, etc.)
     */
    public void setPersonalBooster(UUID playerUUID, String type, double multiplier) {
        personalBoosters.computeIfAbsent(playerUUID, k -> new HashMap<>())
                       .put(type.toLowerCase(), multiplier);
    }
    
    /**
     * Remove a global booster for a specific type
     * @param type The booster type to remove
     */
    public void removeGlobalBooster(String type) {
        globalBoosters.put(type.toLowerCase(), 1.0);
    }
    
    /**
     * Remove a personal booster for a specific player and type
     * @param player The player to remove the booster from
     * @param type The booster type to remove
     */
    public void removePersonalBooster(Player player, String type) {
        removePersonalBooster(player.getUniqueId(), type);
    }
    
    /**
     * Remove a personal booster for a specific player UUID and type
     * @param playerUUID The player UUID to remove the booster from
     * @param type The booster type to remove
     */
    public void removePersonalBooster(UUID playerUUID, String type) {
        Map<String, Double> playerBoosters = personalBoosters.get(playerUUID);
        if (playerBoosters != null) {
            playerBoosters.remove(type.toLowerCase());
            if (playerBoosters.isEmpty()) {
                personalBoosters.remove(playerUUID);
            }
        }
    }
    
    /**
     * Get the global booster multiplier for a specific type
     * @param type The booster type
     * @return The multiplier value (1.0 if no booster is set)
     */
    public double getGlobalBooster(String type) {
        return globalBoosters.getOrDefault(type.toLowerCase(), 1.0);
    }
    
    /**
     * Get the personal booster multiplier for a specific player and type
     * @param player The player to get the booster for
     * @param type The booster type
     * @return The multiplier value (1.0 if no booster is set)
     */
    public double getPersonalBooster(Player player, String type) {
        return getPersonalBooster(player.getUniqueId(), type);
    }
    
    /**
     * Get the personal booster multiplier for a specific player UUID and type
     * @param playerUUID The player UUID to get the booster for
     * @param type The booster type
     * @return The multiplier value (1.0 if no booster is set)
     */
    public double getPersonalBooster(UUID playerUUID, String type) {
        Map<String, Double> playerBoosters = personalBoosters.get(playerUUID);
        if (playerBoosters != null) {
            return playerBoosters.getOrDefault(type.toLowerCase(), 1.0);
        }
        return 1.0;
    }
    
    /**
     * Get the total booster multiplier for a specific player and type (personal * global)
     * @param player The player to get the total booster for
     * @param type The booster type
     * @return The total multiplier value
     */
    public double getTotalBooster(Player player, String type) {
        return getTotalBooster(player.getUniqueId(), type);
    }
    
    /**
     * Get the total booster multiplier for a specific player UUID and type (personal * global)
     * @param playerUUID The player UUID to get the total booster for
     * @param type The booster type
     * @return The total multiplier value
     */
    public double getTotalBooster(UUID playerUUID, String type) {
        double globalMultiplier = getGlobalBooster(type);
        double personalMultiplier = getPersonalBooster(playerUUID, type);
        return globalMultiplier * personalMultiplier;
    }
    
    /**
     * Apply booster to a value
     * @param player The player to apply boosters for
     * @param type The booster type
     * @param value The original value
     * @return The boosted value
     */
    public double applyBooster(Player player, String type, double value) {
        return applyBooster(player.getUniqueId(), type, value);
    }
    
    /**
     * Apply booster to a value
     * @param playerUUID The player UUID to apply boosters for
     * @param type The booster type
     * @param value The original value
     * @return The boosted value
     */
    public double applyBooster(UUID playerUUID, String type, double value) {
        return value * getTotalBooster(playerUUID, type);
    }
    
    /**
     * Apply booster to an integer value and return as integer
     * @param player The player to apply boosters for
     * @param type The booster type
     * @param value The original value
     * @return The boosted value as integer
     */
    public int applyBoosterInt(Player player, String type, int value) {
        return applyBoosterInt(player.getUniqueId(), type, value);
    }
    
    /**
     * Apply booster to an integer value and return as integer
     * @param playerUUID The player UUID to apply boosters for
     * @param type The booster type
     * @param value The original value
     * @return The boosted value as integer
     */
    public int applyBoosterInt(UUID playerUUID, String type, int value) {
        return (int) Math.round(value * getTotalBooster(playerUUID, type));
    }
    
    /**
     * Get all global boosters
     * @return Map of global boosters
     */
    public Map<String, Double> getGlobalBoosters() {
        return new HashMap<>(globalBoosters);
    }
    
    /**
     * Get all personal boosters for a player
     * @param player The player to get boosters for
     * @return Map of personal boosters for the player
     */
    public Map<String, Double> getPersonalBoosters(Player player) {
        return getPersonalBoosters(player.getUniqueId());
    }
    
    /**
     * Get all personal boosters for a player UUID
     * @param playerUUID The player UUID to get boosters for
     * @return Map of personal boosters for the player
     */
    public Map<String, Double> getPersonalBoosters(UUID playerUUID) {
        Map<String, Double> playerBoosters = personalBoosters.get(playerUUID);
        return playerBoosters != null ? new HashMap<>(playerBoosters) : new HashMap<>();
    }
    
    /**
     * Clear all personal boosters for a player
     * @param player The player to clear boosters for
     */
    public void clearPersonalBoosters(Player player) {
        clearPersonalBoosters(player.getUniqueId());
    }
    
    /**
     * Clear all personal boosters for a player UUID
     * @param playerUUID The player UUID to clear boosters for
     */
    public void clearPersonalBoosters(UUID playerUUID) {
        personalBoosters.remove(playerUUID);
    }
    
    /**
     * Clear all global boosters (reset to 1.0)
     */
    public void clearGlobalBoosters() {
        globalBoosters.clear();
        globalBoosters.put("exp", 1.0);
        globalBoosters.put("money", 1.0);
    }
    
    /**
     * Check if a player has any personal boosters
     * @param player The player to check
     * @return True if the player has any personal boosters
     */
    public boolean hasPersonalBoosters(Player player) {
        return hasPersonalBoosters(player.getUniqueId());
    }
    
    /**
     * Check if a player UUID has any personal boosters
     * @param playerUUID The player UUID to check
     * @return True if the player has any personal boosters
     */
    public boolean hasPersonalBoosters(UUID playerUUID) {
        Map<String, Double> playerBoosters = personalBoosters.get(playerUUID);
        return playerBoosters != null && !playerBoosters.isEmpty();
    }
}
