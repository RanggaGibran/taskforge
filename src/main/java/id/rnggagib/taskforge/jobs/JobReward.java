package id.rnggagib.taskforge.jobs;

/**
 * Represents a reward given when reaching a certain job level
 */
public class JobReward {
    
    private final RewardType type;
    private final String value;
    private final String message;
    
    public JobReward(RewardType type, String value, String message) {
        this.type = type;
        this.value = value;
        this.message = message;
    }
    
    /**
     * Get the type of this reward
     */
    public RewardType getType() {
        return type;
    }
    
    /**
     * Get the value of this reward (amount, command, etc.)
     */
    public String getValue() {
        return value;
    }
    
    /**
     * Get the message to display when this reward is given
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * Get numeric value for money/exp rewards
     */
    public double getNumericValue() {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    /**
     * Get numeric amount (alias for getNumericValue for consistency)
     */
    public double getAmount() {
        return getNumericValue();
    }
    
    /**
     * Get command value for command rewards
     */
    public String getCommand() {
        return value;
    }
    
    @Override
    public String toString() {
        return String.format("JobReward{type=%s, value='%s', message='%s'}", 
                           type, value, message);
    }
    
    /**
     * Types of rewards that can be given
     */
    public enum RewardType {
        MONEY,      // Give money to player
        COMMAND,    // Execute a command
        EFFECT,     // Give a special effect (particles, etc.)
        TITLE,      // Give a special title/prefix
        ITEM        // Give an item
    }
}
