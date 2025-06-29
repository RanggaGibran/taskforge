package id.rnggagib.taskforge.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.lang.reflect.Method;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;

import id.rnggagib.taskforge.TaskForgePlugin;
import id.rnggagib.taskforge.jobs.Job;
import id.rnggagib.taskforge.utils.TimeUtils;

/**
 * Professional Job Detail Hub GUI - Modern interface for job management
 */
public class JobDetailGUI implements Listener {
    
    private final TaskForgePlugin plugin;
    private final Player player;
    private final Job job;
    private final JobsGUI parentGUI;
    private final Inventory inventory;
    private boolean headDatabaseAvailable = false;
    
    public JobDetailGUI(TaskForgePlugin plugin, Player player, Job job, JobsGUI parentGUI) {
        this.plugin = plugin;
        this.player = player;
        this.job = job;
        this.parentGUI = parentGUI;
        
        // Check HeadDatabase availability
        Plugin headDbPlugin = Bukkit.getPluginManager().getPlugin("HeadDatabase");
        this.headDatabaseAvailable = headDbPlugin != null && headDbPlugin.isEnabled();
        
        String title = plugin.getConfigManager().translateColorCodes(
            "&8" + job.getDisplayName() + " &8- &eJob Hub");
        
        this.inventory = Bukkit.createInventory(null, 54, title);
        
        // Register event listener
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        setupGUI();
    }
    
    /**
     * Setup the professional job hub GUI with enhanced design
     */
    private void setupGUI() {
        // Clear inventory
        inventory.clear();
        
        // Fill professional background
        fillProfessionalBackground();
        
        // Header with job info
        setupHeaderSection();
        
        // Main information display
        setupMainInfo();
        
        // Action buttons with HeadDatabase
        setupActionButtons();
        
        // Quick access navigation
        setupQuickAccess();
        
        // Navigation with HeadDatabase
        setupNavigation();
    }
    
    /**
     * Fill background with professional glass panes layout
     */
    private void fillProfessionalBackground() {
        ItemStack background = createBackgroundItem();
        ItemStack accent = createAccentItem();
        
        // Top border
        for (int i = 0; i < 9; i++) {
            if (i != 4) { // Leave slot 4 for job header
                if (i == 1 || i == 7) {
                    inventory.setItem(i, accent);
                } else {
                    inventory.setItem(i, background);
                }
            }
        }
        
        // Side borders
        inventory.setItem(9, background);
        inventory.setItem(17, background);
        inventory.setItem(18, background); 
        inventory.setItem(26, background);
        inventory.setItem(27, background); 
        inventory.setItem(35, background);
        
        // Bottom section - leave space for navigation
        for (int i = 36; i < 45; i++) {
            if (i != 38 && i != 40 && i != 42) { // Leave space for nav buttons
                if (i == 37 || i == 39 || i == 41 || i == 43) {
                    inventory.setItem(i, accent);
                } else {
                    inventory.setItem(i, background);
                }
            }
        }
        
        // Fill last row completely with panes
        for (int i = 45; i < 54; i++) {
            if (i == 46 || i == 48 || i == 50 || i == 52) {
                inventory.setItem(i, accent);
            } else {
                inventory.setItem(i, background);
            }
        }
    }
    
    /**
     * Create background decoration item
     */
    private ItemStack createBackgroundItem() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * Create accent decoration item for better visual appeal
     */
    private ItemStack createAccentItem() {
        ItemStack item = new ItemStack(Material.CYAN_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * Setup header section with job information
     */
    private void setupHeaderSection() {
        // Try to use job's custom head or fallback to material icon
        ItemStack jobIcon;
        if (job.getPlayerHeadTexture() != null && !job.getPlayerHeadTexture().isEmpty()) {
            jobIcon = createPlayerHead(job.getPlayerHeadTexture());
        } else {
            jobIcon = new ItemStack(job.getItemIcon());
        }
        
        ItemMeta meta = jobIcon.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getConfigManager().translateColorCodes("&6&l" + job.getDisplayName()));
            
            List<String> lore = new ArrayList<>();
            lore.add(plugin.getConfigManager().translateColorCodes("&7" + job.getDescription()));
            lore.add("");
            lore.add(plugin.getConfigManager().translateColorCodes("&e&lJob Management Hub"));
            lore.add(plugin.getConfigManager().translateColorCodes("&7Use the buttons below to manage this job"));
            
            meta.setLore(lore);
            jobIcon.setItemMeta(meta);
        }
        inventory.setItem(4, jobIcon);
    }
    
    /**
     * Setup main information display with enhanced layout
     */
    private void setupMainInfo() {
        // Player progress (center position)
        ItemStack progressItem;
        if (headDatabaseAvailable) {
            progressItem = createPlayerHead("hdb:51667"); // Stats/chart icon
        } else {
            progressItem = new ItemStack(Material.EXPERIENCE_BOTTLE);
        }
        
        ItemMeta progMeta = progressItem.getItemMeta();
        if (progMeta != null) {
            progMeta.setDisplayName(plugin.getConfigManager().translateColorCodes("&b&l📊 Your Progress"));
            
            List<String> lore = new ArrayList<>();
            
            if (plugin.getPlayerDataManager().hasJob(player.getUniqueId(), job.getName())) {
                int level = plugin.getPlayerDataManager().getJobLevel(player.getUniqueId(), job.getName());
                double exp = plugin.getPlayerDataManager().getJobExperience(player.getUniqueId(), job.getName());
                double progress = plugin.getPlayerDataManager().getLevelProgress(player.getUniqueId(), job.getName());
                
                lore.add(plugin.getConfigManager().translateColorCodes("&7Status: &a&lJOINED"));
                lore.add("");
                lore.add(plugin.getConfigManager().translateColorCodes("&7Current Level: &e" + level));
                lore.add(plugin.getConfigManager().translateColorCodes("&7Total Experience: &b" + String.format("%.1f", exp)));
                lore.add(plugin.getConfigManager().translateColorCodes("&7Progress to Next: &a" + String.format("%.1f%%", progress * 100)));
                
                // Progress bar
                String progressBar = createProgressBar(progress, 20);
                lore.add(plugin.getConfigManager().translateColorCodes("&7" + progressBar));
                
                // Next crate key reward info
                int nextReward = ((level / 10) + 1) * 10;
                if (nextReward <= 100) {
                    lore.add("");
                    lore.add(plugin.getConfigManager().translateColorCodes("&6🗝️ Next Crate Key: &eLevel " + nextReward));
                }
            } else {
                lore.add(plugin.getConfigManager().translateColorCodes("&7Status: &c&lNOT JOINED"));
                lore.add("");
                lore.add(plugin.getConfigManager().translateColorCodes("&7Join this job to start"));
                lore.add(plugin.getConfigManager().translateColorCodes("&7earning experience and rewards!"));
            }
            
            progMeta.setLore(lore);
            progressItem.setItemMeta(progMeta);
        }
        inventory.setItem(13, progressItem);
    }
    
    /**
     * Setup action buttons using HeadDatabase for enhanced appearance
     */
    private void setupActionButtons() {
        boolean hasJob = plugin.getPlayerDataManager().hasJob(player.getUniqueId(), job.getName());
        
        if (hasJob) {
            // Leave button with HeadDatabase and cooldown check
            ItemStack leaveButton;
            if (headDatabaseAvailable) {
                leaveButton = createPlayerHead("hdb:69026"); // Red X or exit icon
            } else {
                leaveButton = new ItemStack(Material.RED_CONCRETE);
            }
            
            ItemMeta meta = leaveButton.getItemMeta();
            if (meta != null) {
                // Check cooldown status
                long joinTimestamp = plugin.getDatabaseManager().getJobJoinTimestamp(player.getUniqueId(), job.getName());
                boolean canLeave = true;
                String remainingTimeFormatted = "";
                
                if (joinTimestamp > 0) {
                    String cooldownString = plugin.getConfigManager().getJobLeaveCooldown();
                    long cooldownDuration = TimeUtils.parseTimeToMillis(cooldownString);
                    
                    if (!TimeUtils.isCooldownExpired(joinTimestamp, cooldownDuration)) {
                        canLeave = false;
                        long remainingTime = TimeUtils.getRemainingCooldown(joinTimestamp, cooldownDuration);
                        remainingTimeFormatted = TimeUtils.formatTime(remainingTime);
                    }
                }
                
                if (canLeave) {
                    meta.setDisplayName(plugin.getConfigManager().translateColorCodes("&c&l✕ &cLEAVE JOB"));
                    List<String> lore = new ArrayList<>();
                    lore.add("");
                    lore.add(plugin.getConfigManager().translateColorCodes("&7Click to leave this job"));
                    lore.add(plugin.getConfigManager().translateColorCodes("&c&lWARNING: &7All progress will be lost!"));
                    lore.add("");
                    lore.add(plugin.getConfigManager().translateColorCodes("&e▶ Click to leave"));
                    meta.setLore(lore);
                } else {
                    meta.setDisplayName(plugin.getConfigManager().translateColorCodes("&c&l✕ &cLEAVE JOB &7(ON COOLDOWN)"));
                    List<String> lore = new ArrayList<>();
                    lore.add("");
                    lore.add(plugin.getConfigManager().translateColorCodes("&7You must wait before leaving this job"));
                    lore.add(plugin.getConfigManager().translateColorCodes("&c&lWARNING: &7All progress will be lost!"));
                    lore.add("");
                    lore.add(plugin.getConfigManager().translateColorCodes("&c⏰ Cooldown: &e" + remainingTimeFormatted));
                    lore.add(plugin.getConfigManager().translateColorCodes("&7You can leave after the cooldown expires"));
                    meta.setLore(lore);
                }
                
                leaveButton.setItemMeta(meta);
            }
            inventory.setItem(10, leaveButton);
            
            // Stats button with HeadDatabase
            ItemStack statsButton;
            if (headDatabaseAvailable) {
                statsButton = createPlayerHead("hdb:6258"); // Chart/analytics icon
            } else {
                statsButton = new ItemStack(Material.BOOK);
            }
            
            meta = statsButton.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(plugin.getConfigManager().translateColorCodes("&e&l📈 &eSTATISTICS"));
                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add(plugin.getConfigManager().translateColorCodes("&7View your detailed job statistics,"));
                lore.add(plugin.getConfigManager().translateColorCodes("&7progress tracking, and achievements."));
                lore.add("");
                lore.add(plugin.getConfigManager().translateColorCodes("&e▶ Click to view stats"));
                meta.setLore(lore);
                statsButton.setItemMeta(meta);
            }
            inventory.setItem(16, statsButton);
            
        } else {
            // Join button with HeadDatabase
            ItemStack joinButton;
            if (headDatabaseAvailable) {
                joinButton = createPlayerHead("hdb:15641"); // Green plus or check icon
            } else {
                joinButton = new ItemStack(Material.GREEN_CONCRETE);
            }
            
            ItemMeta meta = joinButton.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(plugin.getConfigManager().translateColorCodes("&a&l✓ &aJOIN JOB"));
                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add(plugin.getConfigManager().translateColorCodes("&7Click to join this job and start"));
                lore.add(plugin.getConfigManager().translateColorCodes("&7earning experience and rewards!"));
                lore.add("");
                
                // Check if player can join more jobs
                int currentJobs = plugin.getPlayerDataManager().getPlayerJobs(player.getUniqueId()).size();
                int maxJobs = plugin.getConfigManager().getMaxJobs();
                if (currentJobs >= maxJobs) {
                    lore.add(plugin.getConfigManager().translateColorCodes("&c&lCANNOT JOIN: &7Max jobs reached (" + currentJobs + "/" + maxJobs + ")"));
                } else {
                    lore.add(plugin.getConfigManager().translateColorCodes("&aYou can join this job (&7" + currentJobs + "/" + maxJobs + "&a)"));
                    lore.add("");
                    lore.add(plugin.getConfigManager().translateColorCodes("&e▶ Click to join"));
                }
                
                meta.setLore(lore);
                joinButton.setItemMeta(meta);
            }
            inventory.setItem(13, joinButton);
        }
    }
    
    /**
     * Setup quick access navigation buttons with HeadDatabase
     */
    private void setupQuickAccess() {
        // Objectives button with HeadDatabase
        ItemStack objectivesButton;
        if (headDatabaseAvailable) {
            objectivesButton = createPlayerHead("hdb:48927"); // Target/bullseye icon
        } else {
            objectivesButton = new ItemStack(Material.TARGET);
        }
        
        ItemMeta meta = objectivesButton.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getConfigManager().translateColorCodes("&6&l⚡ &6View Objectives"));
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(plugin.getConfigManager().translateColorCodes("&7See all job objectives with detailed"));
            lore.add(plugin.getConfigManager().translateColorCodes("&7information about rewards and chances."));
            lore.add("");
            lore.add(plugin.getConfigManager().translateColorCodes("&b📋 &7Total Objectives: &e" + getTotalObjectivesCount()));
            lore.add(plugin.getConfigManager().translateColorCodes("&a⚡ &7Action Types: &e" + getActionTypesCount()));
            lore.add("");
            lore.add(plugin.getConfigManager().translateColorCodes("&e▶ Click to view objectives"));
            meta.setLore(lore);
            objectivesButton.setItemMeta(meta);
        }
        inventory.setItem(20, objectivesButton);
        
        // Level rewards button with HeadDatabase
        ItemStack rewardsButton;
        if (headDatabaseAvailable) {
            rewardsButton = createPlayerHead("hdb:66374"); // Treasure chest icon
        } else {
            rewardsButton = new ItemStack(Material.CHEST);
        }
        
        meta = rewardsButton.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getConfigManager().translateColorCodes("&d&l🏆 &dLevel Rewards"));
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(plugin.getConfigManager().translateColorCodes("&7View all level rewards including"));
            lore.add(plugin.getConfigManager().translateColorCodes("&7dynamic crate keys and special perks."));
            lore.add("");
            lore.add(plugin.getConfigManager().translateColorCodes("&6🗝️ &7Crate Keys: &eEvery 10 levels!"));
            lore.add(plugin.getConfigManager().translateColorCodes("&d💎 &7Static Rewards: &e" + job.getLevelRewards().size()));
            lore.add(plugin.getConfigManager().translateColorCodes("&c🎯 &7Max Level: &e100"));
            lore.add("");
            lore.add(plugin.getConfigManager().translateColorCodes("&e▶ Click to view rewards"));
            meta.setLore(lore);
            rewardsButton.setItemMeta(meta);
        }
        inventory.setItem(24, rewardsButton);
    }
    
    /**
     * Setup quick information panels
     */
    private void setupQuickInfoPanels() {
        // Quick objectives overview
        ItemStack quickObjectives = new ItemStack(Material.PAPER);
        ItemMeta meta = quickObjectives.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getConfigManager().translateColorCodes("&e&lQuick Overview"));
            List<String> lore = new ArrayList<>();
            lore.add(plugin.getConfigManager().translateColorCodes("&7Total Objectives: &b" + getTotalObjectivesCount()));
            lore.add(plugin.getConfigManager().translateColorCodes("&7Action Types: &a" + getActionTypesCount()));
            lore.add("");
            lore.add(plugin.getConfigManager().translateColorCodes("&8Click 'View Objectives' for details"));
            meta.setLore(lore);
            quickObjectives.setItemMeta(meta);
        }
        inventory.setItem(29, quickObjectives);
        
        // Quick rewards overview
        ItemStack quickRewards = new ItemStack(Material.GOLD_INGOT);
        meta = quickRewards.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getConfigManager().translateColorCodes("&6&lReward Summary"));
            List<String> lore = new ArrayList<>();
            lore.add(plugin.getConfigManager().translateColorCodes("&7Level Rewards: &d" + job.getLevelRewards().size()));
            lore.add(plugin.getConfigManager().translateColorCodes("&7Crate Keys: &610 (Every 10 levels)"));
            lore.add(plugin.getConfigManager().translateColorCodes("&7Max Level: &e100"));
            lore.add("");
            lore.add(plugin.getConfigManager().translateColorCodes("&8Click 'Level Rewards' for details"));
            meta.setLore(lore);
            quickRewards.setItemMeta(meta);
        }
        inventory.setItem(33, quickRewards);
    }
    
    /**
     * Setup navigation buttons with HeadDatabase
     */
    private void setupNavigation() {
        // Back button with HeadDatabase
        ItemStack backButton;
        if (headDatabaseAvailable) {
            backButton = createPlayerHead("hdb:69026"); // Back arrow as requested
        } else {
            backButton = new ItemStack(Material.ARROW);
        }
        
        ItemMeta meta = backButton.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getConfigManager().translateColorCodes("&c&l← &cBack to Jobs"));
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(plugin.getConfigManager().translateColorCodes("&7Return to the main jobs browser"));
            lore.add(plugin.getConfigManager().translateColorCodes("&7to explore other available jobs."));
            lore.add("");
            lore.add(plugin.getConfigManager().translateColorCodes("&e▶ Click to go back"));
            meta.setLore(lore);
            backButton.setItemMeta(meta);
        }
        inventory.setItem(38, backButton);
        
        // Refresh button with HeadDatabase
        ItemStack refreshButton;
        if (headDatabaseAvailable) {
            refreshButton = createPlayerHead("hdb:67690"); // Refresh icon
        } else {
            refreshButton = new ItemStack(Material.LIME_DYE);
        }
        
        meta = refreshButton.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getConfigManager().translateColorCodes("&a&l⟲ &aRefresh"));
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(plugin.getConfigManager().translateColorCodes("&7Refresh your job data and"));
            lore.add(plugin.getConfigManager().translateColorCodes("&7update progress information."));
            lore.add("");
            lore.add(plugin.getConfigManager().translateColorCodes("&e▶ Click to refresh"));
            meta.setLore(lore);
            refreshButton.setItemMeta(meta);
        }
        inventory.setItem(40, refreshButton);
        
        // Close button with HeadDatabase
        ItemStack closeButton;
        if (headDatabaseAvailable) {
            closeButton = createPlayerHead("hdb:69026"); // Close X icon
        } else {
            closeButton = new ItemStack(Material.BARRIER);
        }
        
        meta = closeButton.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getConfigManager().translateColorCodes("&c&l✕ &cClose"));
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(plugin.getConfigManager().translateColorCodes("&7Close this menu and return"));
            lore.add(plugin.getConfigManager().translateColorCodes("&7to the game world."));
            lore.add("");
            lore.add(plugin.getConfigManager().translateColorCodes("&e▶ Click to close"));
            meta.setLore(lore);
            closeButton.setItemMeta(meta);
        }
        inventory.setItem(42, closeButton);
    }
    
    /**
     * Get total objectives count
     */
    private int getTotalObjectivesCount() {
        int total = 0;
        for (Object objectiveMap : job.getObjectives().values()) {
            if (objectiveMap instanceof java.util.Map) {
                total += ((java.util.Map<?, ?>) objectiveMap).size();
            }
        }
        return total;
    }
    
    /**
     * Get action types count
     */
    private int getActionTypesCount() {
        int count = 0;
        for (Object objectiveMap : job.getObjectives().values()) {
            if (objectiveMap instanceof java.util.Map && !((java.util.Map<?, ?>) objectiveMap).isEmpty()) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Open the GUI for the player
     */
    public void open() {
        player.openInventory(inventory);
    }
    
    /**
     * Handle inventory clicks
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        event.setCancelled(true); // Prevent item movement
        
        Player clicker = (Player) event.getWhoClicked();
        if (!clicker.equals(player)) return;
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        
        int slot = event.getSlot();
        
        // Handle different button clicks
        if (slot == 38) { // Back button
            parentGUI.open();
            return;
        }
        
        if (slot == 40) { // Refresh button
            setupGUI(); // Refresh the GUI
            return;
        }
        
        if (slot == 42) { // Close button
            player.closeInventory();
            return;
        }
        
        if (slot == 20) { // Objectives button
            new ObjectivesGUI(plugin, player, job, this).open();
            return;
        }
        
        if (slot == 24) { // Level rewards button
            new LevelRewardsGUI(plugin, player, job, this).open();
            return;
        }
        
        boolean hasJob = plugin.getPlayerDataManager().hasJob(player.getUniqueId(), job.getName());
        
        if (hasJob) {
            if (slot == 10) { // Leave button
                handleLeaveJob();
                return;
            }
            
            if (slot == 16) { // Stats button
                handleShowStats();
                return;
            }
        } else {
            if (slot == 13) { // Join button
                handleJoinJob();
                return;
            }
        }
    }
    
    /**
     * Handle joining the job
     */
    private void handleJoinJob() {
        // Check max jobs limit
        int currentJobs = plugin.getPlayerDataManager().getPlayerJobs(player.getUniqueId()).size();
        int maxJobs = plugin.getConfigManager().getMaxJobs();
        
        if (currentJobs >= maxJobs) {
            player.sendMessage(plugin.getConfigManager().getPrefixedMessage("max_jobs_reached"));
            return;
        }
        
        if (plugin.getPlayerDataManager().joinJob(player.getUniqueId(), job.getName())) {
            // Record join timestamp for cooldown
            plugin.getDatabaseManager().recordJobJoin(player.getUniqueId(), job.getName());
            
            String message = plugin.getConfigManager().getPrefixedMessage("job_joined", "job", job.getName());
            player.sendMessage(message);
            
            // Refresh GUI
            setupGUI();
        } else {
            player.sendMessage(plugin.getConfigManager().getPrefixedMessage("error_occurred"));
        }
    }
    
    /**
     * Handle leaving the job
     */
    private void handleLeaveJob() {
        // Check join-to-leave cooldown
        long joinTimestamp = plugin.getDatabaseManager().getJobJoinTimestamp(player.getUniqueId(), job.getName());
        
        if (joinTimestamp > 0) {
            String cooldownString = plugin.getConfigManager().getJobLeaveCooldown();
            long cooldownDuration = TimeUtils.parseTimeToMillis(cooldownString);
            
            if (!TimeUtils.isCooldownExpired(joinTimestamp, cooldownDuration)) {
                // Cooldown still active
                long remainingTime = TimeUtils.getRemainingCooldown(joinTimestamp, cooldownDuration);
                String remainingTimeFormatted = TimeUtils.formatTime(remainingTime);
                
                String cooldownMessage = plugin.getConfigManager().getMessage("job_leave_cooldown", "time", remainingTimeFormatted);
                player.sendMessage(cooldownMessage);
                return;
            }
        }
        
        // Proceed with leaving the job
        if (plugin.getPlayerDataManager().leaveJob(player.getUniqueId(), job.getName())) {
            String message = plugin.getConfigManager().getPrefixedMessage("job_left", "job", job.getName());
            player.sendMessage(message);
            
            // Refresh GUI
            setupGUI();
        } else {
            player.sendMessage(plugin.getConfigManager().getPrefixedMessage("error_occurred"));
        }
    }
    
    /**
     * Handle showing detailed statistics
     */
    private void handleShowStats() {
        player.closeInventory();
        
        // Show detailed stats in chat
        player.sendMessage(plugin.getConfigManager().translateColorCodes("&8&m----------&r " + 
                          plugin.getConfigManager().getPrefix() + "&e" + job.getDisplayName() + " Statistics &8&m----------"));
        
        if (plugin.getPlayerDataManager().hasJob(player.getUniqueId(), job.getName())) {
            int level = plugin.getPlayerDataManager().getJobLevel(player.getUniqueId(), job.getName());
            double exp = plugin.getPlayerDataManager().getJobExperience(player.getUniqueId(), job.getName());
            double progress = plugin.getPlayerDataManager().getLevelProgress(player.getUniqueId(), job.getName());
            double nextLevelExp = plugin.getPlayerDataManager().getExpRequiredForNextLevel(level);
            
            String progressBar = createProgressBar(progress, 20);
            
            player.sendMessage(plugin.getConfigManager().translateColorCodes("&7Current Level: &e" + level));
            player.sendMessage(plugin.getConfigManager().translateColorCodes("&7Experience: &b" + String.format("%.1f", exp)));
            player.sendMessage(plugin.getConfigManager().translateColorCodes("&7Next Level Requires: &d" + String.format("%.1f", nextLevelExp) + " EXP"));
            player.sendMessage(plugin.getConfigManager().translateColorCodes("&7Progress: " + progressBar + " &a" + String.format("%.1f%%", progress * 100)));
            
            // Show next crate key info
            int nextCrateLevel = ((level / 10) + 1) * 10;
            if (nextCrateLevel <= 100) {
                player.sendMessage(plugin.getConfigManager().translateColorCodes("&6🗝️ Next Crate Key: &eLevel " + nextCrateLevel));
            }
        } else {
            player.sendMessage(plugin.getConfigManager().translateColorCodes("&cYou haven't joined this job yet!"));
        }
    }
    
    /**
     * Create a progress bar
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
    
    /**
     * Create a player head from texture value or HeadDatabase ID
     * Supports both HeadDatabase format (hdb:ID) and texture values
     */
    private ItemStack createPlayerHead(String textureOrId) {
        if (textureOrId == null || textureOrId.isEmpty()) {
            return new ItemStack(Material.PLAYER_HEAD);
        }
        
        // Check if it's a HeadDatabase ID
        if (textureOrId.startsWith("hdb:") && headDatabaseAvailable) {
            try {
                String headId = textureOrId.substring(4); // Remove "hdb:" prefix
                return getHeadDatabaseHead(headId);
            } catch (Exception e) {
                // Fallback to vanilla player head if HeadDatabase fails
                return new ItemStack(Material.PLAYER_HEAD);
            }
        }
        
        // Create vanilla player head with texture
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        
        if (meta != null) {
            try {
                // Try to set texture via player profile (for texture values)
                UUID randomUuid = UUID.randomUUID();
                setPlayerHeadTexture(meta, textureOrId, randomUuid);
            } catch (Exception e) {
                // If texture setting fails, just return basic player head
            }
            
            head.setItemMeta(meta);
        }
        
        return head;
    }
    
    /**
     * Get head from HeadDatabase plugin using reflection
     */
    private ItemStack getHeadDatabaseHead(String headId) throws Exception {
        Plugin headDbPlugin = Bukkit.getPluginManager().getPlugin("HeadDatabase");
        if (headDbPlugin == null) {
            throw new Exception("HeadDatabase not found");
        }
        
        // Get HeadDatabaseAPI instance
        Class<?> apiClass = Class.forName("me.arcaniax.hdb.api.HeadDatabaseAPI");
        Object api = apiClass.getDeclaredConstructor().newInstance();
        
        // Call getItemHead(String id) method
        Method getItemHeadMethod = apiClass.getMethod("getItemHead", String.class);
        ItemStack head = (ItemStack) getItemHeadMethod.invoke(api, headId);
        
        return head != null ? head : new ItemStack(Material.PLAYER_HEAD);
    }
    
    /**
     * Set player head texture using reflection for compatibility
     */
    private void setPlayerHeadTexture(SkullMeta meta, String texture, UUID uuid) throws Exception {
        // Create GameProfile
        Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
        java.lang.reflect.Constructor<?> gameProfileConstructor = gameProfileClass.getConstructor(UUID.class, String.class);
        Object gameProfile = gameProfileConstructor.newInstance(uuid, "");
        
        // Create Property for texture
        Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
        java.lang.reflect.Constructor<?> propertyConstructor = propertyClass.getConstructor(String.class, String.class);
        Object property = propertyConstructor.newInstance("textures", texture);
        
        // Add property to profile
        Method getPropertiesMethod = gameProfileClass.getMethod("getProperties");
        Object properties = getPropertiesMethod.invoke(gameProfile);
        Method putMethod = properties.getClass().getMethod("put", Object.class, Object.class);
        putMethod.invoke(properties, "textures", property);
        
        // Set profile to skull meta using reflection
        java.lang.reflect.Field profileField = meta.getClass().getDeclaredField("profile");
        profileField.setAccessible(true);
        profileField.set(meta, gameProfile);
    }
}
