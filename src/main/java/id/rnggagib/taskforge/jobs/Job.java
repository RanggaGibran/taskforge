package id.rnggagib.taskforge.jobs;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

/**
 * Represents a job that players can join
 */
public class Job {
    
    private final String name;
    private final String displayName;
    private final String description;
    private final String playerHeadTexture;
    private final Material itemIcon;
    
    // Job objectives organized by action type
    private final Map<ActionType, Map<Object, JobObjective>> objectives;
    
    // Level rewards
    private final Map<Integer, JobReward> levelRewards;
    
    public Job(String name, String displayName, String description, String playerHeadTexture, Material itemIcon) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.playerHeadTexture = playerHeadTexture;
        this.itemIcon = itemIcon;
        this.objectives = new HashMap<>();
        this.levelRewards = new HashMap<>();
        
        // Initialize objective maps for each action type
        for (ActionType actionType : ActionType.values()) {
            objectives.put(actionType, new HashMap<>());
        }
    }
    
    /**
     * Add an objective to this job
     */
    public void addObjective(ActionType actionType, Object target, JobObjective objective) {
        objectives.get(actionType).put(target, objective);
    }
    
    /**
     * Add a level reward to this job
     */
    public void addLevelReward(int level, JobReward reward) {
        levelRewards.put(level, reward);
    }
    
    /**
     * Get objective for a specific action and target
     */
    public JobObjective getObjective(ActionType actionType, Object target) {
        Map<Object, JobObjective> actionObjectives = objectives.get(actionType);
        if (actionObjectives == null) return null;
        
        // Try direct match first
        JobObjective objective = actionObjectives.get(target);
        if (objective != null) return objective;
        
        // For entity types, try string representation
        if (target instanceof EntityType) {
            return actionObjectives.get(((EntityType) target).name());
        }
        
        // For materials, try string representation
        if (target instanceof Material) {
            return actionObjectives.get(((Material) target).name());
        }
        
        return null;
    }
    
    /**
     * Get level reward for a specific level
     */
    public JobReward getLevelReward(int level) {
        return levelRewards.get(level);
    }
    
    /**
     * Check if this job has a reward for the specified level
     */
    public boolean hasLevelReward(int level) {
        return levelRewards.containsKey(level);
    }
    
    // Getters
    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public String getPlayerHeadTexture() { return playerHeadTexture; }
    public Material getItemIcon() { return itemIcon; }
    public Map<ActionType, Map<Object, JobObjective>> getObjectives() { return objectives; }
    public Map<Integer, JobReward> getLevelRewards() { return levelRewards; }
    
    /**
     * Action types that can trigger job rewards
     */
    public enum ActionType {
        BREAK,    // Breaking blocks
        PLACE,    // Placing blocks
        KILL,     // Killing entities
        BREED,    // Breeding animals
        TAME,     // Taming animals
        FISH,     // Fishing
        CRAFT,    // Crafting items
        SMELT,    // Smelting in furnace
        ENCHANT,  // Enchanting items
        BREW,     // Brewing potions
        BRUSH     // Brushing suspicious blocks
    }
}
