package id.rnggagib.taskforge.placeholders;

import org.bukkit.OfflinePlayer;

import id.rnggagib.taskforge.TaskForgePlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

/**
 * PlaceholderAPI expansion for TaskForge
 */
public class TaskForgePlaceholderExpansion extends PlaceholderExpansion {
    
    private final TaskForgePlugin plugin;
    
    public TaskForgePlaceholderExpansion(TaskForgePlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getAuthor() {
        return "rnggagib";
    }
    
    @Override
    public String getIdentifier() {
        return "taskforge";
    }
    
    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true; // Required for PlaceholderAPI to keep this expansion loaded
    }
    
    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) {
            return "";
        }
        
        // Split parameters
        String[] args = params.split("_");
        
        if (args.length < 2) {
            return "";
        }
        
        String jobName = args[0];
        String type = args[1];
        
        // Check if job exists
        if (!plugin.getJobManager().jobExists(jobName)) {
            return "";
        }
        
        // Check if player has the job
        if (!plugin.getPlayerDataManager().hasJob(player.getUniqueId(), jobName)) {
            return "0";
        }
        
        switch (type.toLowerCase()) {
            case "level":
                return String.valueOf(plugin.getPlayerDataManager().getJobLevel(player.getUniqueId(), jobName));
                
            case "exp":
            case "experience":
                double exp = plugin.getPlayerDataManager().getJobExperience(player.getUniqueId(), jobName);
                return String.format("%.1f", exp);
                
            case "exprequired":
            case "exp_required":
                int level = plugin.getPlayerDataManager().getJobLevel(player.getUniqueId(), jobName);
                double required = plugin.getPlayerDataManager().getExpRequiredForNextLevel(level);
                return String.format("%.1f", required);
                
            case "progress":
                double progress = plugin.getPlayerDataManager().getLevelProgress(player.getUniqueId(), jobName);
                return String.format("%.1f", progress * 100);
                
            case "progressbar":
            case "progress_bar":
                double progressBar = plugin.getPlayerDataManager().getLevelProgress(player.getUniqueId(), jobName);
                return createProgressBar(progressBar, 20);
                
            case "name":
                return plugin.getJobManager().getJob(jobName).getDisplayName();
                
            default:
                return "";
        }
    }
    
    /**
     * Create a progress bar for placeholders
     */
    private String createProgressBar(double progress, int length) {
        int filled = (int) (progress * length);
        StringBuilder bar = new StringBuilder();
        
        for (int i = 0; i < length; i++) {
            if (i < filled) {
                bar.append("■");
            } else {
                bar.append("□");
            }
        }
        
        return bar.toString();
    }
}
