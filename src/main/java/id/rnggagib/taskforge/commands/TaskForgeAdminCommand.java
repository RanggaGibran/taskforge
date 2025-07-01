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
import id.rnggagib.taskforge.utils.TimeUtils;

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
                
            case "clearcooldown":
                if (args.length < 3) {
                    sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("invalid_usage") + 
                                     " /taskforgeadmin clearcooldown <player> <job>");
                    return true;
                }
                clearJobCooldown(sender, args[1], args[2]);
                break;
                
            case "checkcooldown":
                if (args.length < 3) {
                    sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("invalid_usage") + 
                                     " /taskforgeadmin checkcooldown <player> <job>");
                    return true;
                }
                checkJobCooldown(sender, args[1], args[2]);
                break;
                
            case "salary":
                if (args.length < 2) {
                    showSalaryHelp(sender);
                    return true;
                }
                handleSalaryCommand(sender, args);
                break;
                
            case "booster":
                if (args.length < 2) {
                    showBoosterHelp(sender);
                    return true;
                }
                handleBoosterCommand(sender, args);
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
        sender.sendMessage(plugin.getConfigManager().translateColorCodes("&e/taskforgeadmin salary &8- &7Salary system commands"));
        sender.sendMessage(plugin.getConfigManager().translateColorCodes("&e/taskforgeadmin booster &8- &7Booster system commands"));
        sender.sendMessage(plugin.getConfigManager().translateColorCodes("&e/taskforgeadmin clearcooldown <player> <job> &8- &7Clear job cooldown"));
        sender.sendMessage(plugin.getConfigManager().translateColorCodes("&e/taskforgeadmin checkcooldown <player> <job> &8- &7Check job cooldown"));
    }
    
    /**
     * Clear job cooldown for a player
     */
    private void clearJobCooldown(CommandSender sender, String playerName, String jobName) {
        @SuppressWarnings("deprecation")
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        
        if (!offlinePlayer.hasPlayedBefore()) {
            sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("player_not_found"));
            return;
        }
        
        if (!plugin.getJobManager().jobExists(jobName)) {
            sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("job_not_found"));
            return;
        }
        
        // Remove cooldown from database
        plugin.getDatabaseManager().removeJobCooldown(offlinePlayer.getUniqueId(), jobName);
        
        sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                          "&aCleared job cooldown for &e" + playerName + "&a in job &e" + jobName + "&a!");
    }
    
    /**
     * Check job cooldown status for a player
     */
    private void checkJobCooldown(CommandSender sender, String playerName, String jobName) {
        @SuppressWarnings("deprecation")
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        
        if (!offlinePlayer.hasPlayedBefore()) {
            sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("player_not_found"));
            return;
        }
        
        if (!plugin.getJobManager().jobExists(jobName)) {
            sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("job_not_found"));
            return;
        }
        
        // Check if player has the job first
        if (!plugin.getPlayerDataManager().hasJob(offlinePlayer.getUniqueId(), jobName)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                              "&e" + playerName + "&a doesn't have the &e" + jobName + "&a job.");
            return;
        }
        
        long joinTimestamp = plugin.getDatabaseManager().getJobJoinTimestamp(offlinePlayer.getUniqueId(), jobName);
        
        if (joinTimestamp == 0) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                              "&e" + playerName + "&a has no cooldown record for job &e" + jobName + "&a.");
            return;
        }
        
        // Parse cooldown duration from config
        String cooldownString = plugin.getConfigManager().getJobLeaveCooldown();
        long cooldownDuration = TimeUtils.parseTimeToMillis(cooldownString);
        
        if (TimeUtils.isCooldownExpired(joinTimestamp, cooldownDuration)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                              "&e" + playerName + "&a can leave the &e" + jobName + "&a job (cooldown expired).");
        } else {
            long remainingTime = TimeUtils.getRemainingCooldown(joinTimestamp, cooldownDuration);
            String remainingTimeFormatted = TimeUtils.formatTime(remainingTime);
            
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                              "&e" + playerName + "&a must wait &c" + remainingTimeFormatted + "&a before leaving the &e" + jobName + "&a job.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!sender.hasPermission("taskforge.admin")) {
            return completions;
        }
        
        if (args.length == 1) {
            // First argument - subcommands
            String[] subCommands = {"reload", "setlevel", "addexp", "resetjob", "info", "clearcooldown", "checkcooldown", "salary", "booster"};
            for (String subCommand : subCommands) {
                if (subCommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            if ("setlevel".equals(subCommand) || "addexp".equals(subCommand) || 
                "resetjob".equals(subCommand) || "info".equals(subCommand) ||
                "clearcooldown".equals(subCommand) || "checkcooldown".equals(subCommand)) {
                // Player names
                for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                    String playerName = player.getName();
                    if (playerName != null && playerName.toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(playerName);
                    }
                }
            } else if ("salary".equals(subCommand)) {
                // Salary subcommands
                String[] salarySubCommands = {"info", "payout", "check"};
                for (String salarySubCommand : salarySubCommands) {
                    if (salarySubCommand.toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(salarySubCommand);
                    }
                }
            } else if ("booster".equals(subCommand)) {
                // Booster subcommands
                String[] boosterSubCommands = {"player", "global", "remove", "list", "clear"};
                for (String boosterSubCommand : boosterSubCommands) {
                    if (boosterSubCommand.toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(boosterSubCommand);
                    }
                }
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            
            if ("setlevel".equals(subCommand) || "addexp".equals(subCommand) || "resetjob".equals(subCommand) ||
                "clearcooldown".equals(subCommand) || "checkcooldown".equals(subCommand)) {
                // Job names
                for (String jobName : plugin.getJobManager().getJobNames()) {
                    if (jobName.toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(jobName);
                    }
                }
            } else if ("salary".equals(subCommand)) {
                String salarySubCommand = args[1].toLowerCase();
                if ("payout".equals(salarySubCommand) || "check".equals(salarySubCommand)) {
                    // Player names for salary payout/check
                    for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                        String playerName = player.getName();
                        if (playerName != null && playerName.toLowerCase().startsWith(args[2].toLowerCase())) {
                            completions.add(playerName);
                        }
                    }
                }
            } else if ("booster".equals(subCommand)) {
                String boosterSubCommand = args[1].toLowerCase();
                if ("player".equals(boosterSubCommand)) {
                    // Multiplier suggestions
                    String[] multipliers = {"1.0", "1.5", "2.0", "2.5", "3.0", "4.0", "5.0"};
                    for (String multiplier : multipliers) {
                        if (multiplier.startsWith(args[2])) {
                            completions.add(multiplier);
                        }
                    }
                } else if ("global".equals(boosterSubCommand)) {
                    // Multiplier suggestions
                    String[] multipliers = {"1.0", "1.5", "2.0", "2.5", "3.0", "4.0", "5.0"};
                    for (String multiplier : multipliers) {
                        if (multiplier.startsWith(args[2])) {
                            completions.add(multiplier);
                        }
                    }
                } else if ("remove".equals(boosterSubCommand)) {
                    // Target type (player/global)
                    String[] targets = {"player", "global"};
                    for (String target : targets) {
                        if (target.toLowerCase().startsWith(args[2].toLowerCase())) {
                            completions.add(target);
                        }
                    }
                } else if ("list".equals(boosterSubCommand)) {
                    // Player names for listing specific player boosters
                    for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                        String playerName = player.getName();
                        if (playerName != null && playerName.toLowerCase().startsWith(args[2].toLowerCase())) {
                            completions.add(playerName);
                        }
                    }
                } else if ("clear".equals(boosterSubCommand)) {
                    // Target type (player/global)
                    String[] targets = {"player", "global"};
                    for (String target : targets) {
                        if (target.toLowerCase().startsWith(args[2].toLowerCase())) {
                            completions.add(target);
                        }
                    }
                }
            }
        } else if (args.length == 4) {
            String subCommand = args[0].toLowerCase();
            
            if ("booster".equals(subCommand)) {
                String boosterSubCommand = args[1].toLowerCase();
                if ("player".equals(boosterSubCommand)) {
                    // Player names
                    for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                        String playerName = player.getName();
                        if (playerName != null && playerName.toLowerCase().startsWith(args[3].toLowerCase())) {
                            completions.add(playerName);
                        }
                    }
                } else if ("global".equals(boosterSubCommand)) {
                    // Booster types
                    String[] types = {"exp", "money"};
                    for (String type : types) {
                        if (type.toLowerCase().startsWith(args[3].toLowerCase())) {
                            completions.add(type);
                        }
                    }
                } else if ("remove".equals(boosterSubCommand)) {
                    String target = args[2].toLowerCase();
                    if ("player".equals(target)) {
                        // Player names
                        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                            String playerName = player.getName();
                            if (playerName != null && playerName.toLowerCase().startsWith(args[3].toLowerCase())) {
                                completions.add(playerName);
                            }
                        }
                    } else if ("global".equals(target)) {
                        // Booster types
                        String[] types = {"exp", "money"};
                        for (String type : types) {
                            if (type.toLowerCase().startsWith(args[3].toLowerCase())) {
                                completions.add(type);
                            }
                        }
                    }
                } else if ("clear".equals(boosterSubCommand)) {
                    String target = args[2].toLowerCase();
                    if ("player".equals(target)) {
                        // Player names
                        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                            String playerName = player.getName();
                            if (playerName != null && playerName.toLowerCase().startsWith(args[3].toLowerCase())) {
                                completions.add(playerName);
                            }
                        }
                    }
                }
            }
        } else if (args.length == 5) {
            String subCommand = args[0].toLowerCase();
            
            if ("booster".equals(subCommand)) {
                String boosterSubCommand = args[1].toLowerCase();
                if ("player".equals(boosterSubCommand)) {
                    // Booster types
                    String[] types = {"exp", "money"};
                    for (String type : types) {
                        if (type.toLowerCase().startsWith(args[4].toLowerCase())) {
                            completions.add(type);
                        }
                    }
                } else if ("remove".equals(boosterSubCommand)) {
                    String target = args[2].toLowerCase();
                    if ("player".equals(target)) {
                        // Booster types
                        String[] types = {"exp", "money"};
                        for (String type : types) {
                            if (type.toLowerCase().startsWith(args[4].toLowerCase())) {
                                completions.add(type);
                            }
                        }
                    }
                }
            }
        }
        
        return completions;
    }
    
    /**
     * Handle salary-related commands
     */
    private void handleSalaryCommand(CommandSender sender, String[] args) {
        String salarySubCommand = args[1].toLowerCase();
        
        switch (salarySubCommand) {
            case "info":
                showSalaryInfo(sender);
                break;
                
            case "payout":
                if (args.length < 3) {
                    sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("invalid_usage") + 
                                     " /taskforgeadmin salary payout <player>");
                    return;
                }
                forcePlayerPayout(sender, args[2]);
                break;
                
            case "check":
                if (args.length < 3) {
                    sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("invalid_usage") + 
                                     " /taskforgeadmin salary check <player>");
                    return;
                }
                checkPlayerSalary(sender, args[2]);
                break;
                
            default:
                showSalaryHelp(sender);
                break;
        }
    }
    
    /**
     * Show salary command help
     */
    private void showSalaryHelp(CommandSender sender) {
        sender.sendMessage(plugin.getConfigManager().translateColorCodes("&6&l=== TaskForge Salary Commands ==="));
        sender.sendMessage(plugin.getConfigManager().translateColorCodes("&e/taskforgeadmin salary info &7- Show salary system info"));
        sender.sendMessage(plugin.getConfigManager().translateColorCodes("&e/taskforgeadmin salary payout <player> &7- Force payout for player"));
        sender.sendMessage(plugin.getConfigManager().translateColorCodes("&e/taskforgeadmin salary check <player> &7- Check player's pending salary"));
    }
    
    /**
     * Show salary system information
     */
    private void showSalaryInfo(CommandSender sender) {
        boolean enabled = plugin.getSalaryManager().isSalarySystemEnabled();
        int intervalMinutes = plugin.getConfigManager().getConfig().getInt("salary_system.payout_interval_minutes", 5);
        double totalPending = plugin.getSalaryManager().getTotalPendingSalaries();
        int pendingPlayers = plugin.getSalaryManager().getPendingPlayersCount();
        
        sender.sendMessage(plugin.getConfigManager().translateColorCodes("&6&l=== Salary System Information ==="));
        sender.sendMessage(plugin.getConfigManager().translateColorCodes("&eStatus: " + (enabled ? "&aEnabled" : "&cDisabled")));
        sender.sendMessage(plugin.getConfigManager().translateColorCodes("&ePayout Interval: &a" + intervalMinutes + " minutes"));
        sender.sendMessage(plugin.getConfigManager().translateColorCodes("&eTotal Pending: &a$" + String.format("%.2f", totalPending)));
        sender.sendMessage(plugin.getConfigManager().translateColorCodes("&ePlayers with Pending Salary: &a" + pendingPlayers));
    }
    
    /**
     * Force payout for a specific player
     */
    private void forcePlayerPayout(CommandSender sender, String playerName) {
        // Find player
        @SuppressWarnings("deprecation")
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
            sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("player_not_found"));
            return;
        }
        
        UUID playerUUID = offlinePlayer.getUniqueId();
        double pendingAmount = plugin.getSalaryManager().getPendingSalary(playerUUID);
        
        if (pendingAmount <= 0) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                              "&e" + playerName + "&a has no pending salary.");
            return;
        }
        
        if (offlinePlayer.getPlayer() == null || !offlinePlayer.isOnline()) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                              "&e" + playerName + "&c is not online. Cannot force payout.");
            return;
        }
        
        boolean success = plugin.getSalaryManager().forcePayoutPlayer(playerUUID);
        if (success) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                              "&aSuccessfully paid out &e$" + String.format("%.2f", pendingAmount) + "&a to &e" + playerName + "&a.");
        } else {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                              "&cFailed to payout salary to &e" + playerName + "&c. Check console for errors.");
        }
    }
    
    /**
     * Check a player's pending salary
     */
    private void checkPlayerSalary(CommandSender sender, String playerName) {
        // Find player
        @SuppressWarnings("deprecation")
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
            sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("player_not_found"));
            return;
        }
        
        UUID playerUUID = offlinePlayer.getUniqueId();
        double pendingAmount = plugin.getSalaryManager().getPendingSalary(playerUUID);
        
        if (pendingAmount <= 0) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                              "&e" + playerName + "&a has no pending salary.");
        } else {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                              "&e" + playerName + "&a has &e$" + String.format("%.2f", pendingAmount) + "&a pending.");
        }
    }
    
    /**
     * Handle booster command
     */
    private void handleBoosterCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            showBoosterHelp(sender);
            return;
        }
        
        String subCommand = args[1].toLowerCase();
        
        switch (subCommand) {
            case "player":
                if (args.length < 5) {
                    sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("invalid_usage") + 
                                     " /taskforgeadmin booster player <multiplier> <playerName> <type>");
                    return;
                }
                setPlayerBooster(sender, args[2], args[3], args[4]);
                break;
                
            case "global":
                if (args.length < 4) {
                    sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("invalid_usage") + 
                                     " /taskforgeadmin booster global <multiplier> <type>");
                    return;
                }
                setGlobalBooster(sender, args[2], args[3]);
                break;
                
            case "remove":
                if (args.length < 3) {
                    showBoosterHelp(sender);
                    return;
                }
                removeBooster(sender, args);
                break;
                
            case "list":
                listBoosters(sender, args);
                break;
                
            case "clear":
                clearBoosters(sender, args);
                break;
                
            default:
                showBoosterHelp(sender);
                break;
        }
    }
    
    /**
     * Show booster command help
     */
    private void showBoosterHelp(CommandSender sender) {
        sender.sendMessage(plugin.getConfigManager().translateColorCodes("&8&m----------&r " + 
                          plugin.getConfigManager().getPrefix() + "&eBooster Commands &8&m----------"));
        sender.sendMessage(plugin.getConfigManager().translateColorCodes("&e/taskforgeadmin booster player <multiplier> <playerName> <type> &8- &7Set personal booster"));
        sender.sendMessage(plugin.getConfigManager().translateColorCodes("&e/taskforgeadmin booster global <multiplier> <type> &8- &7Set global booster"));
        sender.sendMessage(plugin.getConfigManager().translateColorCodes("&e/taskforgeadmin booster remove player <playerName> <type> &8- &7Remove personal booster"));
        sender.sendMessage(plugin.getConfigManager().translateColorCodes("&e/taskforgeadmin booster remove global <type> &8- &7Remove global booster"));
        sender.sendMessage(plugin.getConfigManager().translateColorCodes("&e/taskforgeadmin booster list [player] &8- &7List boosters"));
        sender.sendMessage(plugin.getConfigManager().translateColorCodes("&e/taskforgeadmin booster clear player <playerName> &8- &7Clear all personal boosters"));
        sender.sendMessage(plugin.getConfigManager().translateColorCodes("&e/taskforgeadmin booster clear global &8- &7Clear all global boosters"));
        sender.sendMessage(plugin.getConfigManager().translateColorCodes("&7Types: &eexp&7, &emoney&7, etc. Multipliers: &e1.0&7=normal, &e2.0&7=2x, &e3.0&7=3x"));
    }
    
    /**
     * Set a personal booster for a player
     */
    private void setPlayerBooster(CommandSender sender, String multiplierString, String playerName, String type) {
        // Parse multiplier
        double multiplier;
        try {
            multiplier = Double.parseDouble(multiplierString);
            if (multiplier < 0) {
                sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("invalid_number"));
                return;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("invalid_number"));
            return;
        }
        
        // Find player
        @SuppressWarnings("deprecation")
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
            sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("player_not_found"));
            return;
        }
        
        UUID playerUUID = offlinePlayer.getUniqueId();
        plugin.getBoosterManager().setPersonalBooster(playerUUID, type, multiplier);
        
        sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                          "&aSet personal &e" + type + "&a booster for &e" + playerName + "&a to &e" + 
                          String.format("%.1f", multiplier) + "x&a.");
    }
    
    /**
     * Set a global booster
     */
    private void setGlobalBooster(CommandSender sender, String multiplierString, String type) {
        // Parse multiplier
        double multiplier;
        try {
            multiplier = Double.parseDouble(multiplierString);
            if (multiplier < 0) {
                sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("invalid_number"));
                return;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("invalid_number"));
            return;
        }
        
        plugin.getBoosterManager().setGlobalBooster(type, multiplier);
        
        sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                          "&aSet global &e" + type + "&a booster to &e" + 
                          String.format("%.1f", multiplier) + "x&a.");
    }
    
    /**
     * Remove boosters
     */
    private void removeBooster(CommandSender sender, String[] args) {
        if (args.length < 3) {
            showBoosterHelp(sender);
            return;
        }
        
        String target = args[2].toLowerCase();
        
        if ("player".equals(target)) {
            if (args.length < 5) {
                sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("invalid_usage") + 
                                 " /taskforgeadmin booster remove player <playerName> <type>");
                return;
            }
            removePlayerBooster(sender, args[3], args[4]);
        } else if ("global".equals(target)) {
            if (args.length < 4) {
                sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("invalid_usage") + 
                                 " /taskforgeadmin booster remove global <type>");
                return;
            }
            removeGlobalBooster(sender, args[3]);
        } else {
            showBoosterHelp(sender);
        }
    }
    
    /**
     * Remove a personal booster from a player
     */
    private void removePlayerBooster(CommandSender sender, String playerName, String type) {
        // Find player
        @SuppressWarnings("deprecation")
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
            sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("player_not_found"));
            return;
        }
        
        UUID playerUUID = offlinePlayer.getUniqueId();
        plugin.getBoosterManager().removePersonalBooster(playerUUID, type);
        
        sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                          "&aRemoved personal &e" + type + "&a booster from &e" + playerName + "&a.");
    }
    
    /**
     * Remove a global booster
     */
    private void removeGlobalBooster(CommandSender sender, String type) {
        plugin.getBoosterManager().removeGlobalBooster(type);
        
        sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                          "&aRemoved global &e" + type + "&a booster.");
    }
    
    /**
     * List boosters
     */
    private void listBoosters(CommandSender sender, String[] args) {
        if (args.length >= 3) {
            // List specific player's boosters
            String playerName = args[2];
            @SuppressWarnings("deprecation")
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
            if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
                sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("player_not_found"));
                return;
            }
            
            UUID playerUUID = offlinePlayer.getUniqueId();
            java.util.Map<String, Double> personalBoosters = plugin.getBoosterManager().getPersonalBoosters(playerUUID);
            
            sender.sendMessage(plugin.getConfigManager().translateColorCodes("&8&m----------&r " + 
                              plugin.getConfigManager().getPrefix() + "&e" + playerName + "'s Boosters &8&m----------"));
            
            if (personalBoosters.isEmpty()) {
                sender.sendMessage(plugin.getConfigManager().translateColorCodes("&e" + playerName + "&a has no personal boosters."));
            } else {
                for (java.util.Map.Entry<String, Double> entry : personalBoosters.entrySet()) {
                    sender.sendMessage(plugin.getConfigManager().translateColorCodes("&e" + entry.getKey() + "&7: &a" + 
                                      String.format("%.1f", entry.getValue()) + "x"));
                }
            }
        } else {
            // List global boosters
            java.util.Map<String, Double> globalBoosters = plugin.getBoosterManager().getGlobalBoosters();
            
            sender.sendMessage(plugin.getConfigManager().translateColorCodes("&8&m----------&r " + 
                              plugin.getConfigManager().getPrefix() + "&eGlobal Boosters &8&m----------"));
            
            for (java.util.Map.Entry<String, Double> entry : globalBoosters.entrySet()) {
                if (entry.getValue() != 1.0) {
                    sender.sendMessage(plugin.getConfigManager().translateColorCodes("&e" + entry.getKey() + "&7: &a" + 
                                      String.format("%.1f", entry.getValue()) + "x"));
                }
            }
        }
    }
    
    /**
     * Clear boosters
     */
    private void clearBoosters(CommandSender sender, String[] args) {
        if (args.length < 3) {
            showBoosterHelp(sender);
            return;
        }
        
        String target = args[2].toLowerCase();
        
        if ("player".equals(target)) {
            if (args.length < 4) {
                sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("invalid_usage") + 
                                 " /taskforgeadmin booster clear player <playerName>");
                return;
            }
            clearPlayerBoosters(sender, args[3]);
        } else if ("global".equals(target)) {
            clearGlobalBoosters(sender);
        } else {
            showBoosterHelp(sender);
        }
    }
    
    /**
     * Clear all personal boosters from a player
     */
    private void clearPlayerBoosters(CommandSender sender, String playerName) {
        // Find player
        @SuppressWarnings("deprecation")
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
            sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("player_not_found"));
            return;
        }
        
        UUID playerUUID = offlinePlayer.getUniqueId();
        plugin.getBoosterManager().clearPersonalBoosters(playerUUID);
        
        sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                          "&aCleared all personal boosters from &e" + playerName + "&a.");
    }
    
    /**
     * Clear all global boosters
     */
    private void clearGlobalBoosters(CommandSender sender) {
        plugin.getBoosterManager().clearGlobalBoosters();
        
        sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                          "&aCleared all global boosters.");
    }
}
