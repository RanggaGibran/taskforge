package id.rnggagib.taskforge.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import id.rnggagib.taskforge.TaskForgePlugin;
import id.rnggagib.taskforge.gui.JobsGUI;

/**
 * Main command handler for /jobs command
 */
public class JobsCommand implements CommandExecutor, TabCompleter {
    
    private final TaskForgePlugin plugin;
    
    public JobsCommand(TaskForgePlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player_only"));
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            // Show help or open GUI
            openJobsGUI(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "browse":
            case "list":
                openJobsGUI(player);
                break;
                
            case "join":
                if (args.length < 2) {
                    player.sendMessage(plugin.getConfigManager().getPrefixedMessage("invalid_usage") + " /jobs join <job>");
                    return true;
                }
                joinJob(player, args[1]);
                break;
                
            case "leave":
                if (args.length < 2) {
                    player.sendMessage(plugin.getConfigManager().getPrefixedMessage("invalid_usage") + " /jobs leave <job>");
                    return true;
                }
                leaveJob(player, args[1]);
                break;
                
            case "stats":
            case "info":
                showPlayerStats(player);
                break;
                
            case "top":
            case "leaderboard":
                if (args.length >= 2) {
                    showLeaderboard(player, args[1]);
                } else {
                    showGeneralLeaderboard(player);
                }
                break;
                
            case "toggle":
                if (args.length < 2) {
                    player.sendMessage(plugin.getConfigManager().getPrefixedMessage("invalid_usage") + " /jobs toggle <particle|title>");
                    return true;
                }
                toggleFeature(player, args[1]);
                break;
                
            default:
                showHelp(player);
                break;
        }
        
        return true;
    }
    
    /**
     * Open the jobs GUI for browsing available jobs
     */
    private void openJobsGUI(Player player) {
        new JobsGUI(plugin, player).open();
    }
    
    /**
     * Handle joining a job
     */
    private void joinJob(Player player, String jobName) {
        // Check if job exists
        if (!plugin.getJobManager().jobExists(jobName)) {
            player.sendMessage(plugin.getConfigManager().getPrefixedMessage("job_not_found"));
            return;
        }
        
        // Check if already has job
        if (plugin.getPlayerDataManager().hasJob(player.getUniqueId(), jobName)) {
            player.sendMessage(plugin.getConfigManager().getPrefixedMessage("job_already_joined"));
            return;
        }
        
        // Check max jobs limit
        Set<String> playerJobs = plugin.getPlayerDataManager().getPlayerJobs(player.getUniqueId());
        int maxJobs = plugin.getConfigManager().getMaxJobs();
        if (playerJobs.size() >= maxJobs) {
            player.sendMessage(plugin.getConfigManager().getPrefixedMessage("max_jobs_reached"));
            return;
        }
        
        // Join the job
        if (plugin.getPlayerDataManager().joinJob(player.getUniqueId(), jobName)) {
            String message = plugin.getConfigManager().getPrefixedMessage("job_joined", "job", jobName);
            player.sendMessage(message);
        } else {
            player.sendMessage(plugin.getConfigManager().getPrefixedMessage("error_occurred"));
        }
    }
    
    /**
     * Handle leaving a job
     */
    private void leaveJob(Player player, String jobName) {
        if (!plugin.getPlayerDataManager().hasJob(player.getUniqueId(), jobName)) {
            player.sendMessage(plugin.getConfigManager().getPrefixedMessage("job_not_joined"));
            return;
        }
        
        if (plugin.getPlayerDataManager().leaveJob(player.getUniqueId(), jobName)) {
            String message = plugin.getConfigManager().getPrefixedMessage("job_left", "job", jobName);
            player.sendMessage(message);
        } else {
            player.sendMessage(plugin.getConfigManager().getPrefixedMessage("error_occurred"));
        }
    }
    
    /**
     * Show player's job statistics
     */
    private void showPlayerStats(Player player) {
        Set<String> playerJobs = plugin.getPlayerDataManager().getPlayerJobs(player.getUniqueId());
        
        if (playerJobs.isEmpty()) {
            player.sendMessage(plugin.getConfigManager().getPrefixedMessage("no_jobs"));
            return;
        }
        
        player.sendMessage(plugin.getConfigManager().translateColorCodes("&8&m----------&r " + 
                          plugin.getConfigManager().getPrefix() + "&eYour Job Statistics &8&m----------"));
        
        for (String jobName : playerJobs) {
            int level = plugin.getPlayerDataManager().getJobLevel(player.getUniqueId(), jobName);
            double exp = plugin.getPlayerDataManager().getJobExperience(player.getUniqueId(), jobName);
            double progress = plugin.getPlayerDataManager().getLevelProgress(player.getUniqueId(), jobName);
            
            String progressBar = createProgressBar(progress, 20);
            
            player.sendMessage(plugin.getConfigManager().translateColorCodes(
                String.format("&7%s: &eLv.%d &8| &b%.1f EXP &8| %s &8(%.1f%%)", 
                             jobName, level, exp, progressBar, progress * 100)));
        }
    }
    
    /**
     * Show leaderboard for a specific job
     */
    private void showLeaderboard(Player player, String jobName) {
        if (!plugin.getJobManager().jobExists(jobName)) {
            player.sendMessage(plugin.getConfigManager().getPrefixedMessage("job_not_found"));
            return;
        }
        
        // This would be implemented to show top players for the job
        player.sendMessage(plugin.getConfigManager().getPrefixedMessage("feature_not_implemented"));
    }
    
    /**
     * Show general leaderboard
     */
    private void showGeneralLeaderboard(Player player) {
        // This would be implemented to show overall top players
        player.sendMessage(plugin.getConfigManager().getPrefixedMessage("feature_not_implemented"));
    }
    
    /**
     * Toggle cosmetic features like particles or titles
     */
    private void toggleFeature(Player player, String feature) {
        // This would be implemented to toggle player preferences
        player.sendMessage(plugin.getConfigManager().getPrefixedMessage("feature_not_implemented"));
    }
    
    /**
     * Show command help
     */
    private void showHelp(Player player) {
        player.sendMessage(plugin.getConfigManager().translateColorCodes("&8&m----------&r " + 
                          plugin.getConfigManager().getPrefix() + "&eJob Commands &8&m----------"));
        player.sendMessage(plugin.getConfigManager().translateColorCodes("&e/jobs browse &8- &7Browse available jobs"));
        player.sendMessage(plugin.getConfigManager().translateColorCodes("&e/jobs join <job> &8- &7Join a job"));
        player.sendMessage(plugin.getConfigManager().translateColorCodes("&e/jobs leave <job> &8- &7Leave a job"));
        player.sendMessage(plugin.getConfigManager().translateColorCodes("&e/jobs stats &8- &7View your job statistics"));
        player.sendMessage(plugin.getConfigManager().translateColorCodes("&e/jobs top [job] &8- &7View leaderboards"));
        player.sendMessage(plugin.getConfigManager().translateColorCodes("&e/jobs toggle <feature> &8- &7Toggle cosmetic features"));
    }
    
    /**
     * Create a progress bar for experience
     */
    private String createProgressBar(double progress, int length) {
        int filled = (int) (progress * length);
        StringBuilder bar = new StringBuilder("&8[");
        
        for (int i = 0; i < length; i++) {
            if (i < filled) {
                bar.append("&a■");
            } else {
                bar.append("&7■");
            }
        }
        
        bar.append("&8]");
        return plugin.getConfigManager().translateColorCodes(bar.toString());
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // First argument - subcommands
            String[] subCommands = {"browse", "join", "leave", "stats", "top", "toggle"};
            for (String subCommand : subCommands) {
                if (subCommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            if ("join".equals(subCommand) || "top".equals(subCommand)) {
                // Job names
                for (String jobName : plugin.getJobManager().getJobNames()) {
                    if (jobName.toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(jobName);
                    }
                }
            } else if ("leave".equals(subCommand)) {
                // Player's current jobs
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    Set<String> playerJobs = plugin.getPlayerDataManager().getPlayerJobs(player.getUniqueId());
                    for (String jobName : playerJobs) {
                        if (jobName.toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(jobName);
                        }
                    }
                }
            } else if ("toggle".equals(subCommand)) {
                // Toggle options
                String[] toggleOptions = {"particle", "title"};
                for (String option : toggleOptions) {
                    if (option.toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(option);
                    }
                }
            }
        }
        
        return completions;
    }
}
