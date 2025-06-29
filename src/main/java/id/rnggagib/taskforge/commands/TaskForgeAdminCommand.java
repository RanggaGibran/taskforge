package id.rnggagib.taskforge.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import id.rnggagib.taskforge.TaskForgePlugin;

/**
 * Admin command handler for /taskforgeadmin command
 */
public class TaskForgeAdminCommand implements CommandExecutor, TabCompleter {
    
    private final TaskForgePlugin plugin;
    
    public TaskForgeAdminCommand(TaskForgePlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("taskforge.admin")) {
            sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("no_permission"));
            return true;
        }
        
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "reload":
                reloadPlugin(sender);
                break;
                
            case "setlevel":
                if (args.length < 4) {
                    sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("invalid_usage") + 
                                     " /taskforgeadmin setlevel <player> <job> <level>");
                    return true;
                }
                setPlayerLevel(sender, args[1], args[2], args[3]);
                break;
                
            case "addexp":
                if (args.length < 4) {
                    sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("invalid_usage") + 
                                     " /taskforgeadmin addexp <player> <job> <amount>");
                    return true;
                }
                addPlayerExp(sender, args[1], args[2], args[3]);
                break;
                
            case "resetjob":
                if (args.length < 3) {
                    sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("invalid_usage") + 
                                     " /taskforgeadmin resetjob <player> <job>");
                    return true;
                }
                resetPlayerJob(sender, args[1], args[2]);
                break;
                
            case "info":
                if (args.length < 2) {
                    sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("invalid_usage") + 
                                     " /taskforgeadmin info <player>");
                    return true;
                }
                showPlayerInfo(sender, args[1]);
                break;
                
            default:
                showHelp(sender);
                break;
        }
        
        return true;
    }
    
    /**
     * Reload the plugin configuration
     */
    private void reloadPlugin(CommandSender sender) {
        try {
            plugin.reloadPlugin();
            sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("config_reloaded"));
        } catch (Exception e) {
            sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("error_occurred"));
            plugin.getLogger().severe("Failed to reload plugin: " + e.getMessage());
        }
    }
    
    /**
     * Set a player's level in a specific job
     */
    private void setPlayerLevel(CommandSender sender, String playerName, String jobName, String levelString) {
        // Find player
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
            sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("player_not_found"));
            return;
        }
        
        // Check if job exists
        if (!plugin.getJobManager().jobExists(jobName)) {
            sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("job_not_found"));
            return;
        }
        
        // Parse level
        int level;
        try {
            level = Integer.parseInt(levelString);
            if (level < 1) {
                sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("invalid_number"));
                return;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("invalid_number"));
            return;
        }
        
        UUID playerUUID = offlinePlayer.getUniqueId();
        
        // Make sure player has the job first
        if (!plugin.getPlayerDataManager().hasJob(playerUUID, jobName)) {
            plugin.getPlayerDataManager().joinJob(playerUUID, jobName);
        }
        
        // Calculate required experience for the level
        double requiredExp = calculateExpForLevel(level);
        
        // Set the experience to match the level
        plugin.getPlayerDataManager().addJobExperience(playerUUID, jobName, 
                                                      requiredExp - plugin.getPlayerDataManager().getJobExperience(playerUUID, jobName));
        
        sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("level_set", 
                                                                       "player", playerName,
                                                                       "job", jobName,
                                                                       "level", String.valueOf(level)));
    }
    
    /**
     * Add experience to a player's job
     */
    private void addPlayerExp(CommandSender sender, String playerName, String jobName, String expString) {
        // Find player
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
            sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("player_not_found"));
            return;
        }
        
        // Check if job exists
        if (!plugin.getJobManager().jobExists(jobName)) {
            sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("job_not_found"));
            return;
        }
        
        // Parse experience
        double exp;
        try {
            exp = Double.parseDouble(expString);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("invalid_number"));
            return;
        }
        
        UUID playerUUID = offlinePlayer.getUniqueId();
        
        // Make sure player has the job first
        if (!plugin.getPlayerDataManager().hasJob(playerUUID, jobName)) {
            plugin.getPlayerDataManager().joinJob(playerUUID, jobName);
        }
        
        // Add experience
        plugin.getPlayerDataManager().addJobExperience(playerUUID, jobName, exp);
        
        sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("exp_added", 
                                                                       "player", playerName,
                                                                       "job", jobName,
                                                                       "exp", String.valueOf(exp)));
    }
    
    /**
     * Reset a player's job progress
     */
    private void resetPlayerJob(CommandSender sender, String playerName, String jobName) {
        // Find player
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
            sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("player_not_found"));
            return;
        }
        
        // Check if job exists
        if (!plugin.getJobManager().jobExists(jobName)) {
            sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("job_not_found"));
            return;
        }
        
        UUID playerUUID = offlinePlayer.getUniqueId();
        
        // Check if player has the job
        if (!plugin.getPlayerDataManager().hasJob(playerUUID, jobName)) {
            sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("job_not_joined"));
            return;
        }
        
        // Reset by leaving and rejoining
        plugin.getPlayerDataManager().leaveJob(playerUUID, jobName);
        plugin.getPlayerDataManager().joinJob(playerUUID, jobName);
        
        sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("job_reset", 
                                                                       "player", playerName,
                                                                       "job", jobName));
    }
    
    /**
     * Show information about a player's jobs
     */
    private void showPlayerInfo(CommandSender sender, String playerName) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
            sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("player_not_found"));
            return;
        }
        
        UUID playerUUID = offlinePlayer.getUniqueId();
        
        sender.sendMessage(plugin.getConfigManager().translateColorCodes("&8&m----------&r " + 
                          plugin.getConfigManager().getPrefix() + "&ePlayer Info: " + playerName + " &8&m----------"));
        
        // Show all jobs
        for (String jobName : plugin.getPlayerDataManager().getPlayerJobs(playerUUID)) {
            int level = plugin.getPlayerDataManager().getJobLevel(playerUUID, jobName);
            double exp = plugin.getPlayerDataManager().getJobExperience(playerUUID, jobName);
            
            sender.sendMessage(plugin.getConfigManager().translateColorCodes(
                String.format("&7%s: &eLv.%d &8| &b%.1f EXP", jobName, level, exp)));
        }
    }
    
    /**
     * Calculate total experience required for a specific level
     */
    private double calculateExpForLevel(int targetLevel) {
        double baseExp = plugin.getConfigManager().getBaseExp();
        double multiplier = plugin.getConfigManager().getLevelingMultiplier();
        double totalExp = 0;
        
        for (int i = 1; i < targetLevel; i++) {
            totalExp += baseExp * Math.pow(multiplier, i - 1);
        }
        
        return totalExp;
    }
    
    /**
     * Show admin command help
     */
    private void showHelp(CommandSender sender) {
        sender.sendMessage(plugin.getConfigManager().translateColorCodes("&8&m----------&r " + 
                          plugin.getConfigManager().getPrefix() + "&eAdmin Commands &8&m----------"));
        sender.sendMessage(plugin.getConfigManager().translateColorCodes("&e/taskforgeadmin reload &8- &7Reload plugin configuration"));
        sender.sendMessage(plugin.getConfigManager().translateColorCodes("&e/taskforgeadmin setlevel <player> <job> <level> &8- &7Set player's job level"));
        sender.sendMessage(plugin.getConfigManager().translateColorCodes("&e/taskforgeadmin addexp <player> <job> <amount> &8- &7Add experience to player"));
        sender.sendMessage(plugin.getConfigManager().translateColorCodes("&e/taskforgeadmin resetjob <player> <job> &8- &7Reset player's job progress"));
        sender.sendMessage(plugin.getConfigManager().translateColorCodes("&e/taskforgeadmin info <player> &8- &7Show player's job information"));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!sender.hasPermission("taskforge.admin")) {
            return completions;
        }
        
        if (args.length == 1) {
            // First argument - subcommands
            String[] subCommands = {"reload", "setlevel", "addexp", "resetjob", "info"};
            for (String subCommand : subCommands) {
                if (subCommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            if ("setlevel".equals(subCommand) || "addexp".equals(subCommand) || 
                "resetjob".equals(subCommand) || "info".equals(subCommand)) {
                // Player names
                for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                    String playerName = player.getName();
                    if (playerName != null && playerName.toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(playerName);
                    }
                }
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            
            if ("setlevel".equals(subCommand) || "addexp".equals(subCommand) || "resetjob".equals(subCommand)) {
                // Job names
                for (String jobName : plugin.getJobManager().getJobNames()) {
                    if (jobName.toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(jobName);
                    }
                }
            }
        }
        
        return completions;
    }
}
