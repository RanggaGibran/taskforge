package id.rnggagib.taskforge.jobs;

import java.util.Random;

/**
 * Represents an objective within a job that can be completed for rewards
 */
public class JobObjective {
    
    private final double experience;
    private final double moneyMin;
    private final double moneyMax;
    private final double chance;
    private static final Random random = new Random();
    
    /**
     * Constructor for single money value (backwards compatibility)
     */
    public JobObjective(double experience, double money, double chance) {
        this.experience = experience;
        this.moneyMin = money;
        this.moneyMax = money;
        this.chance = Math.max(0.0, Math.min(100.0, chance)); // Clamp between 0-100
    }
    
    /**
     * Constructor for money range
     */
    public JobObjective(double experience, double moneyMin, double moneyMax, double chance) {
        this.experience = experience;
        this.moneyMin = Math.min(moneyMin, moneyMax);
        this.moneyMax = Math.max(moneyMin, moneyMax);
        this.chance = Math.max(0.0, Math.min(100.0, chance)); // Clamp between 0-100
    }
    
    /**
     * Get experience reward for this objective
     */
    public double getExperience() {
        return experience;
    }
    
    /**
     * Get money reward for this objective (backwards compatibility - returns random value in range)
     */
    public double getMoney() {
        return getRandomMoney();
    }
    
    /**
     * Get minimum money reward
     */
    public double getMoneyMin() {
        return moneyMin;
    }
    
    /**
     * Get maximum money reward
     */
    public double getMoneyMax() {
        return moneyMax;
    }
    
    /**
     * Get a random money value within the range
     */
    public double getRandomMoney() {
        if (moneyMin == moneyMax) {
            return moneyMin;
        }
        return moneyMin + (random.nextDouble() * (moneyMax - moneyMin));
    }
    
    /**
     * Check if this objective uses a money range
     */
    public boolean hasMoneyRange() {
        return moneyMin != moneyMax;
    }
    
    /**
     * Get formatted money string for display
     */
    public String getMoneyDisplay() {
        if (hasMoneyRange()) {
            return String.format("$%.2f - $%.2f", moneyMin, moneyMax);
        } else {
            return String.format("$%.2f", moneyMin);
        }
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
        if (hasMoneyRange()) {
            return String.format("JobObjective{exp=%.2f, money=%.2f-%.2f, chance=%.1f%%}", 
                               experience, moneyMin, moneyMax, chance);
        } else {
            return String.format("JobObjective{exp=%.2f, money=%.2f, chance=%.1f%%}", 
                               experience, moneyMin, chance);
        }
    }
}
