package id.rnggagib.taskforge.listeners;

import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import id.rnggagib.taskforge.TaskForgePlugin;
import id.rnggagib.taskforge.jobs.Job.ActionType;
import id.rnggagib.taskforge.jobs.JobObjective;

/**
 * Listens for job-related events and processes them with anti-exploit protection
 */
public class JobListener implements Listener {
    
    private final TaskForgePlugin plugin;
    
    // Anti-exploit tracking - blocks placed by players
    private final Map<String, Long> placedBlocks = new ConcurrentHashMap<>();
    
    // Materials that should be tracked for anti-exploit (mainly farmable blocks)
    private final Set<Material> trackedMaterials = new HashSet<>();
    
    public JobListener(TaskForgePlugin plugin) {
        this.plugin = plugin;
        initializeTrackedMaterials();
        startCleanupTask();
    }
    
    /**
     * Get natural time threshold from config (in milliseconds)
     */
    private long getNaturalTimeThreshold() {
        int minutes = plugin.getConfigManager().getConfig().getInt("anti_exploit.natural_time_minutes", 30);
        return minutes * 60 * 1000L; // Convert to milliseconds
    }
    
    /**
     * Check if anti-exploit is enabled
     */
    private boolean isAntiExploitEnabled() {
        return plugin.getConfigManager().getConfig().getBoolean("anti_exploit.enabled", true);
    }
    
    /**
     * Check if debug messages are enabled for anti-exploit
     */
    private boolean isAntiExploitDebugEnabled() {
        return plugin.getConfigManager().getConfig().getBoolean("anti_exploit.debug_messages", false);
    }
    
    /**
     * Initialize materials that should be tracked for anti-exploit
     */
    private void initializeTrackedMaterials() {
        // Wood/Tree materials
        trackedMaterials.add(Material.OAK_LOG);
        trackedMaterials.add(Material.BIRCH_LOG);
        trackedMaterials.add(Material.SPRUCE_LOG);
        trackedMaterials.add(Material.JUNGLE_LOG);
        trackedMaterials.add(Material.ACACIA_LOG);
        trackedMaterials.add(Material.DARK_OAK_LOG);
        trackedMaterials.add(Material.MANGROVE_LOG);
        trackedMaterials.add(Material.CHERRY_LOG);
        trackedMaterials.add(Material.CRIMSON_STEM);
        trackedMaterials.add(Material.WARPED_STEM);
        
        // Stripped logs
        trackedMaterials.add(Material.STRIPPED_OAK_LOG);
        trackedMaterials.add(Material.STRIPPED_BIRCH_LOG);
        trackedMaterials.add(Material.STRIPPED_SPRUCE_LOG);
        trackedMaterials.add(Material.STRIPPED_JUNGLE_LOG);
        trackedMaterials.add(Material.STRIPPED_ACACIA_LOG);
        trackedMaterials.add(Material.STRIPPED_DARK_OAK_LOG);
        trackedMaterials.add(Material.STRIPPED_MANGROVE_LOG);
        trackedMaterials.add(Material.STRIPPED_CHERRY_LOG);
        trackedMaterials.add(Material.STRIPPED_CRIMSON_STEM);
        trackedMaterials.add(Material.STRIPPED_WARPED_STEM);
        
        // Ores and valuable blocks
        trackedMaterials.add(Material.COAL_ORE);
        trackedMaterials.add(Material.IRON_ORE);
        trackedMaterials.add(Material.GOLD_ORE);
        trackedMaterials.add(Material.DIAMOND_ORE);
        trackedMaterials.add(Material.EMERALD_ORE);
        trackedMaterials.add(Material.LAPIS_ORE);
        trackedMaterials.add(Material.REDSTONE_ORE);
        trackedMaterials.add(Material.COPPER_ORE);
        trackedMaterials.add(Material.DEEPSLATE_COAL_ORE);
        trackedMaterials.add(Material.DEEPSLATE_IRON_ORE);
        trackedMaterials.add(Material.DEEPSLATE_GOLD_ORE);
        trackedMaterials.add(Material.DEEPSLATE_DIAMOND_ORE);
        trackedMaterials.add(Material.DEEPSLATE_EMERALD_ORE);
        trackedMaterials.add(Material.DEEPSLATE_LAPIS_ORE);
        trackedMaterials.add(Material.DEEPSLATE_REDSTONE_ORE);
        trackedMaterials.add(Material.DEEPSLATE_COPPER_ORE);
        
        // Crops (untuk Farmer job)
        trackedMaterials.add(Material.WHEAT);
        trackedMaterials.add(Material.CARROTS);
        trackedMaterials.add(Material.POTATOES);
        trackedMaterials.add(Material.BEETROOTS);
        trackedMaterials.add(Material.PUMPKIN);
        trackedMaterials.add(Material.MELON);
        trackedMaterials.add(Material.SUGAR_CANE);
        trackedMaterials.add(Material.BAMBOO);
        trackedMaterials.add(Material.COCOA);
        trackedMaterials.add(Material.SWEET_BERRY_BUSH);
        
        // Building blocks (for Builder job anti-exploit)
        trackedMaterials.add(Material.COBBLESTONE);
        trackedMaterials.add(Material.STONE_BRICKS);
        trackedMaterials.add(Material.SMOOTH_STONE);
        trackedMaterials.add(Material.BRICKS);
        trackedMaterials.add(Material.NETHER_BRICKS);
        trackedMaterials.add(Material.QUARTZ_BLOCK);
        trackedMaterials.add(Material.PRISMARINE);
        trackedMaterials.add(Material.PRISMARINE_BRICKS);
        trackedMaterials.add(Material.DARK_PRISMARINE);
    }
    
    /**
     * Create location key for tracking
     */
    private String createLocationKey(Location location) {
        return location.getWorld().getName() + ":" + 
               location.getBlockX() + ":" + 
               location.getBlockY() + ":" + 
               location.getBlockZ();
    }
    
    /**
     * Check if a block is considered "natural" (not recently placed by player)
     */
    private boolean isNaturalBlock(Location location) {
        if (!isAntiExploitEnabled()) {
            return true; // Anti-exploit disabled, all blocks are natural
        }
        
        String locationKey = createLocationKey(location);
        Long placedTime = placedBlocks.get(locationKey);
        
        if (placedTime == null) {
            // Block not tracked = natural
            return true;
        }
        
        long currentTime = System.currentTimeMillis();
        long timeSincePlaced = currentTime - placedTime;
        long naturalTimeThreshold = getNaturalTimeThreshold();
        
        // If block was placed more than configured time ago, consider it natural
        if (timeSincePlaced >= naturalTimeThreshold) {
            // Remove from tracking since it's now natural
            placedBlocks.remove(locationKey);
            return true;
        }
        
        return false;
    }
    
    /**
     * Start cleanup task to remove old entries
     */
    private void startCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isAntiExploitEnabled()) {
                    return; // Don't cleanup if anti-exploit is disabled
                }
                
                long currentTime = System.currentTimeMillis();
                long naturalTimeThreshold = getNaturalTimeThreshold();
                
                int removedCount = 0;
                var iterator = placedBlocks.entrySet().iterator();
                while (iterator.hasNext()) {
                    var entry = iterator.next();
                    if ((currentTime - entry.getValue()) >= naturalTimeThreshold) {
                        iterator.remove();
                        removedCount++;
                    }
                }
                
                if (isAntiExploitDebugEnabled() && removedCount > 0) {
                    plugin.getLogger().info("[Anti-Exploit] Cleaned up " + removedCount + " old block entries");
                }
            }
        }.runTaskTimerAsynchronously(plugin, 12000L, 12000L); // Run every 10 minutes (12000 ticks)
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Load player data when they join
        plugin.getPlayerDataManager().loadPlayerData(event.getPlayer().getUniqueId());
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean up notifications
        plugin.getNotificationManager().onPlayerQuit(event.getPlayer());
        
        // Save and unload player data when they leave
        plugin.getPlayerDataManager().unloadPlayerData(event.getPlayer().getUniqueId());
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material material = event.getBlock().getType();
        Location location = event.getBlock().getLocation();
        
        // Check if this is a tracked material and if it's natural
        if (trackedMaterials.contains(material)) {
            if (!isNaturalBlock(location)) {
                // Block was recently placed by a player, don't give rewards
                if (isAntiExploitDebugEnabled()) {
                    player.sendMessage(plugin.getConfigManager().translateColorCodes(
                        "&7[Anti-Exploit] &cNo reward: Block was recently placed by a player"));
                }
                return;
            } else if (isAntiExploitDebugEnabled()) {
                player.sendMessage(plugin.getConfigManager().translateColorCodes(
                    "&7[Anti-Exploit] &aReward given: Natural block detected"));
            }
        }
        
        processJobAction(player, ActionType.BREAK, material);
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Material material = event.getBlockPlaced().getType();
        Location location = event.getBlockPlaced().getLocation();
        
        // Track placed blocks for anti-exploit
        if (isAntiExploitEnabled() && trackedMaterials.contains(material)) {
            String locationKey = createLocationKey(location);
            placedBlocks.put(locationKey, System.currentTimeMillis());
            
            if (isAntiExploitDebugEnabled()) {
                long naturalTime = getNaturalTimeThreshold() / (60 * 1000); // Convert to minutes
                player.sendMessage(plugin.getConfigManager().translateColorCodes(
                    "&7[Anti-Exploit] &eTracking placed block: " + material.name() + 
                    " (becomes natural in " + naturalTime + " minutes)"));
            }
        }
        
        processJobAction(player, ActionType.PLACE, material);
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityKill(EntityDeathEvent event) {
        if (event.getEntity().getKiller() instanceof Player) {
            Player player = event.getEntity().getKiller();
            EntityType entityType = event.getEntity().getType();
            
            processJobAction(player, ActionType.KILL, entityType);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityBreed(EntityBreedEvent event) {
        if (event.getBreeder() instanceof Player) {
            Player player = (Player) event.getBreeder();
            EntityType entityType = event.getEntity().getType();
            
            processJobAction(player, ActionType.BREED, entityType);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityTame(EntityTameEvent event) {
        if (event.getOwner() instanceof Player) {
            Player player = (Player) event.getOwner();
            EntityType entityType = event.getEntity().getType();
            
            processJobAction(player, ActionType.TAME, entityType);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH && event.getCaught() != null) {
            Player player = event.getPlayer();
            
            // Try to determine what was caught
            Material caughtMaterial = Material.COD; // Default
            if (event.getCaught().getType() == EntityType.ITEM) {
                org.bukkit.entity.Item item = (org.bukkit.entity.Item) event.getCaught();
                caughtMaterial = item.getItemStack().getType();
            }
            
            processJobAction(player, ActionType.FISH, caughtMaterial);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            Material craftedMaterial = event.getRecipe().getResult().getType();
            
            processJobAction(player, ActionType.CRAFT, craftedMaterial);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        Player player = event.getPlayer();
        Material smeltedMaterial = event.getItemType();
        
        // Process for each item extracted
        for (int i = 0; i < event.getItemAmount(); i++) {
            processJobAction(player, ActionType.SMELT, smeltedMaterial);
        }
    }
    
    /**
     * Process a job action for a player
     */
    private void processJobAction(Player player, ActionType actionType, Object target) {
        Set<String> playerJobs = plugin.getPlayerDataManager().getPlayerJobs(player.getUniqueId());
        
        if (playerJobs.isEmpty()) {
            return; // Player has no jobs
        }
        
        // Process each job the player has
        for (String jobName : playerJobs) {
            JobObjective objective = plugin.getJobManager().getObjective(jobName, actionType, target);
            
            if (objective != null && objective.shouldTrigger()) {
                // Give rewards
                giveJobRewards(player, jobName, objective);
                
                // Log action if debug enabled
                if (plugin.getConfigManager().isActionLoggingEnabled()) {
                    plugin.getLogger().info(String.format("%s performed %s %s for job %s (exp: %.2f, money: %.2f)", 
                                                        player.getName(), actionType, target, jobName, 
                                                        objective.getExperience(), objective.getMoney()));
                }
            }
        }
    }
    
    /**
     * Give rewards to player for completing a job objective
     */
    private void giveJobRewards(Player player, String jobName, JobObjective objective) {
        double experience = objective.getExperience();
        double money = objective.getMoney();
        
        StringBuilder rewardMessage = new StringBuilder();
        boolean hasRewards = false;
        
        // Add experience (always given if > 0)
        if (experience > 0) {
            plugin.getPlayerDataManager().addJobExperience(player.getUniqueId(), jobName, experience);
            
            // Build experience part of notification
            if (plugin.getConfigManager().isFeatureEnabled("exp_notifications")) {
                rewardMessage.append("+").append(String.format("%.1f", experience)).append(" EXP");
                hasRewards = true;
            }
        }
        
        // Handle money through salary system or direct payment
        if (money > 0) {
            if (plugin.getSalaryManager().isSalarySystemEnabled()) {
                // Accumulate money in salary system
                plugin.getSalaryManager().addPendingSalary(player.getUniqueId(), money);
                
                // Build money part of notification
                if (plugin.getConfigManager().isFeatureEnabled("money_notifications")) {
                    String currencySymbol = plugin.getConfigManager().getCurrencySymbol();
                    if (hasRewards) {
                        rewardMessage.append(" and ");
                    }
                    rewardMessage.append("+").append(currencySymbol).append(String.format("%.2f", money)).append(" (pending)");
                    hasRewards = true;
                }
                
                // Update statistics with money added to salary
                plugin.getDatabaseManager().updatePlayerStats(player.getUniqueId(), jobName, money, experience);
            } else {
                // Direct payment (salary system disabled)
                if (plugin.isEconomyEnabled()) {
                    plugin.getEconomy().depositPlayer(player, money);
                    
                    // Build money part of notification
                    if (plugin.getConfigManager().isFeatureEnabled("money_notifications")) {
                        String currencySymbol = plugin.getConfigManager().getCurrencySymbol();
                        if (hasRewards) {
                            rewardMessage.append(" and ");
                        }
                        rewardMessage.append("+").append(currencySymbol).append(String.format("%.2f", money));
                        hasRewards = true;
                    }
                    
                    // Update statistics with money given
                    plugin.getDatabaseManager().updatePlayerStats(player.getUniqueId(), jobName, money, experience);
                } else {
                    // Economy disabled, only update stats with 0 money
                    plugin.getDatabaseManager().updatePlayerStats(player.getUniqueId(), jobName, 0.0, experience);
                }
            }
        } else {
            // No money reward, update statistics with 0 money
            plugin.getDatabaseManager().updatePlayerStats(player.getUniqueId(), jobName, 0.0, experience);
        }
        
        // Send combined notification if there are rewards
        if (hasRewards && rewardMessage.length() > 0) {
            plugin.getNotificationManager().sendRewardNotification(player, rewardMessage.toString());
        }
    }
}
