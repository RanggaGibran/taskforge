package id.rnggagib.taskforge.jobs;

/**
 * Represents an objective within a job that can be completed for rewards
 */
public class JobObjective {
    
    private final double experience;
    private final double money;
    private final double chance;
    
    public JobObjective(double experience, double money, double chance) {
        this.experience = experience;
        this.money = money;
        this.chance = Math.max(0.0, Math.min(100.0, chance)); // Clamp between 0-100
    }
    
    /**
     * Get experience reward for this objective
     */
    public double getExperience() {
        return experience;
    }
    
    /**
     * Get money reward for this objective
     */
    public double getMoney() {
        return money;
    }
    
    /**
     * Get chance (0-100) that this objective will give rewards
     */
    public double getChance() {
        return chance;
    }
    
    /**
     * Check if the objective should trigger based on chance
     */
    public boolean shouldTrigger() {
        if (chance >= 100.0) return true;
        if (chance <= 0.0) return false;
        
        return Math.random() * 100.0 < chance;
    }
    
    @Override
    public String toString() {
        return String.format("JobObjective{exp=%.2f, money=%.2f, chance=%.1f%%}", 
                           experience, money, chance);
    }
}
