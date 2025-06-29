package id.rnggagib.taskforge.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

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

/**
 * Professional Jobs Browser GUI - Main hub for job discovery and management
 */
public class JobsGUI implements Listener {
    
    private final TaskForgePlugin plugin;
    private final Player player;
    private final Inventory inventory;
    private boolean headDatabaseAvailable = false;
    
    public JobsGUI(TaskForgePlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        
        // Check if HeadDatabase is available
        Plugin headDbPlugin = Bukkit.getPluginManager().getPlugin("HeadDatabase");
        headDatabaseAvailable = headDbPlugin != null && headDbPlugin.isEnabled();
        
        String title = plugin.getConfigManager().translateColorCodes(
            "&8⚒ &eTaskForge &8⚒ &7Jobs Browser");
        
        this.inventory = Bukkit.createInventory(null, 54, title);
        
        // Register event listener
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        setupGUI();
    }
    
    /**
     * Setup the professional jobs browser GUI with compact design
     */
    private void setupGUI() {
        // Clear inventory
        inventory.clear();
        
        // Fill decorative background for compact design
        fillCompactBackground();
        
        // Add header info panel
        setupHeaderInfo();
        
        // Setup job slots in compact grid
        setupJobSlots();
        
        // Add navigation and utility buttons
        setupNavigationButtons();
    }
    
    /**
     * Fill background with decorative glass panes for compact design
     */
    private void fillCompactBackground() {
        ItemStack background = createBackgroundItem();
        ItemStack accent = createAccentItem();
        
        // Create elegant border pattern - only fill non-functional slots
        // Top border
        for (int i = 0; i < 9; i++) {
            if (i != 4) { // Leave slot 4 for player info
                if (i == 1 || i == 7) {
                    inventory.setItem(i, accent);
                } else {
                    inventory.setItem(i, background);
                }
            }
        }
        
        // Middle section borders (left and right edges only)
        inventory.setItem(9, background);
        inventory.setItem(17, background);
        inventory.setItem(18, background); 
        inventory.setItem(26, background);
        
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
        
        // Add separator line between jobs and navigation
        for (int i = 27; i < 36; i++) {
            if (i == 30 || i == 32) {
                inventory.setItem(i, accent);
            } else {
                inventory.setItem(i, background);
            }
        }
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
     * Create visual separator item for layout balance
     */
    private ItemStack createSeparatorItem() {
        ItemStack item = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getConfigManager().translateColorCodes("&b&l◆"));
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * Setup header information panel
     */
    private void setupHeaderInfo() {
        // Player stats summary
        ItemStack playerInfo = createPlayerInfoItem();
        inventory.setItem(4, playerInfo);
    }
    
    /**
     * Create player information display item
     */
    private ItemStack createPlayerInfoItem() {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(plugin.getConfigManager().translateColorCodes(
                "&e&l⚑ " + player.getName() + "'s Job Overview"));
            
            List<String> lore = new ArrayList<>();
            lore.add("");
            
            // Count active jobs
            int activeJobs = 0;
            int totalLevel = 0;
            Map<String, Job> allJobs = plugin.getJobManager().getAllJobs();
            
            for (Job job : allJobs.values()) {
                if (plugin.getPlayerDataManager().hasJob(player.getUniqueId(), job.getName())) {
                    activeJobs++;
                    totalLevel += plugin.getPlayerDataManager().getJobLevel(player.getUniqueId(), job.getName());
                }
            }
            
            lore.add(plugin.getConfigManager().translateColorCodes("&7Active Jobs: &a" + activeJobs + "&7/&e" + allJobs.size()));
            lore.add(plugin.getConfigManager().translateColorCodes("&7Combined Level: &b" + totalLevel));
            lore.add("");
            lore.add(plugin.getConfigManager().translateColorCodes("&f&o\"Choose your path, forge your destiny\""));
            lore.add("");
            lore.add(plugin.getConfigManager().translateColorCodes("&7Click on any job below to view details,"));
            lore.add(plugin.getConfigManager().translateColorCodes("&7objectives, rewards, and manage membership."));
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Setup job slots in optimized grid layout
     */
    private void setupJobSlots() {
        Map<String, Job> jobs = plugin.getJobManager().getAllJobs();
        
        // Optimized layout for jobs - 2 rows, more slots used efficiently
        // Row 1: slots 10, 11, 12, 14, 15, 16 (6 jobs)
        // Row 2: slots 19, 20, 21, 23, 24, 25 (6 more jobs)
        // Total: 12 jobs can be displayed
        
        int[] jobSlots = {
            10, 11, 12, 14, 15, 16,     // Top row (skip slot 13 for visual balance)
            19, 20, 21, 23, 24, 25      // Bottom row (skip slot 22 for visual balance)  
        };
        
        int slotIndex = 0;
        for (Job job : jobs.values()) {
            if (slotIndex >= jobSlots.length) break; // Max 12 jobs displayed
            
            ItemStack jobItem = createJobItem(job);
            inventory.setItem(jobSlots[slotIndex], jobItem);
            slotIndex++;
        }
        
        // Add visual separators in the center slots for balance
        if (jobs.size() <= 12) { // Only add separators if we have space
            ItemStack separator = createSeparatorItem();
            inventory.setItem(13, separator); // Center of top row
            inventory.setItem(22, separator); // Center of bottom row
        }
    }
    
    /**
     * Setup navigation and utility buttons with HeadDatabase heads
     */
    private void setupNavigationButtons() {
        // Refresh button (slot 38 - bottom left area)
        ItemStack refreshItem = createRefreshButton();
        inventory.setItem(38, refreshItem);
        
        // Help button (slot 40 - bottom center)
        ItemStack helpItem = createHelpButton();
        inventory.setItem(40, helpItem);
        
        // Close button (slot 42 - bottom right area)
        ItemStack closeItem = createCloseButton();
        inventory.setItem(42, closeItem);
    }
    
    /**
     * Create an enhanced item representing a job
     */
    private ItemStack createJobItem(Job job) {
        ItemStack item;
        
        // Try to use player head if texture is provided
        if (job.getPlayerHeadTexture() != null && !job.getPlayerHeadTexture().isEmpty()) {
            item = createPlayerHead(job.getPlayerHeadTexture());
        } else {
            // Fallback to material icon
            item = new ItemStack(job.getItemIcon());
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            boolean hasJob = plugin.getPlayerDataManager().hasJob(player.getUniqueId(), job.getName());
            
            // Enhanced display name with status indicators
            String statusIcon = hasJob ? "&a&l✓ " : "&7▶ ";
            meta.setDisplayName(plugin.getConfigManager().translateColorCodes(
                statusIcon + job.getDisplayName()));
            
            // Create enhanced lore
            List<String> lore = new ArrayList<>();
            
            // Job description with formatting
            lore.add(plugin.getConfigManager().translateColorCodes("&f" + job.getDescription()));
            lore.add("");
            
            // Status section
            if (hasJob) {
                int level = plugin.getPlayerDataManager().getJobLevel(player.getUniqueId(), job.getName());
                double exp = plugin.getPlayerDataManager().getJobExperience(player.getUniqueId(), job.getName());
                double progress = plugin.getPlayerDataManager().getLevelProgress(player.getUniqueId(), job.getName());
                
                lore.add(plugin.getConfigManager().translateColorCodes("&a&l⚡ ACTIVE JOB"));
                lore.add(plugin.getConfigManager().translateColorCodes("&8├ &7Level: &e&l" + level));
                lore.add(plugin.getConfigManager().translateColorCodes("&8├ &7Experience: &b" + String.format("%.1f", exp)));
                lore.add(plugin.getConfigManager().translateColorCodes("&8└ &7Progress: &a" + 
                    createProgressBar(progress, 10) + " &a" + String.format("%.1f%%", progress * 100)));
                lore.add("");
                
                // Quick stats preview
                lore.add(plugin.getConfigManager().translateColorCodes("&e&l⚒ QUICK ACTIONS"));
                lore.add(plugin.getConfigManager().translateColorCodes("&8├ &7View Objectives & Progress"));
                lore.add(plugin.getConfigManager().translateColorCodes("&8├ &7Check Level Rewards"));
                lore.add(plugin.getConfigManager().translateColorCodes("&8└ &7Manage Job Settings"));
                lore.add("");
                lore.add(plugin.getConfigManager().translateColorCodes("&e&l➤ &eClick to open Job Hub"));
                
            } else {
                lore.add(plugin.getConfigManager().translateColorCodes("&7&l○ AVAILABLE JOB"));
                lore.add("");
                
                // Preview some key objectives
                if (!job.getObjectives().isEmpty()) {
                    lore.add(plugin.getConfigManager().translateColorCodes("&e&l⚒ SAMPLE OBJECTIVES"));
                    int count = 0;
                    
                    // Iterate through all action types and their objectives
                    for (var actionTypeEntry : job.getObjectives().entrySet()) {
                        for (var objectiveEntry : actionTypeEntry.getValue().entrySet()) {
                            if (count >= 2) break; // Show max 2 objectives as preview
                            
                            var actionType = actionTypeEntry.getKey();
                            var target = objectiveEntry.getKey();
                            var objective = objectiveEntry.getValue();
                            
                            String actionDesc = formatObjectiveDescription(actionType, target);
                            lore.add(plugin.getConfigManager().translateColorCodes(
                                "&8├ &7" + actionDesc + " &8(+" + objective.getExperience() + " exp)"));
                            count++;
                        }
                        if (count >= 2) break;
                    }
                    
                    // Count total objectives
                    int totalObjectives = job.getObjectives().values().stream()
                        .mapToInt(map -> map.size()).sum();
                    
                    if (totalObjectives > 2) {
                        lore.add(plugin.getConfigManager().translateColorCodes(
                            "&8└ &7... and " + (totalObjectives - 2) + " more objectives"));
                    }
                    lore.add("");
                }
                
                lore.add(plugin.getConfigManager().translateColorCodes("&e&l➤ &eClick to view details & join"));
            }
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Create a visual progress bar
     */
    private String createProgressBar(double progress, int length) {
        int filled = (int) (progress * length);
        StringBuilder bar = new StringBuilder();
        
        for (int i = 0; i < length; i++) {
            if (i < filled) {
                bar.append("&a▰");
            } else {
                bar.append("&7▱");
            }
        }
        
        return bar.toString();
    }
    
    /**
     * Create refresh button using HeadDatabase
     */
    private ItemStack createRefreshButton() {
        ItemStack item;
        
        // Try to use HeadDatabase for refresh icon
        if (headDatabaseAvailable) {
            item = createPlayerHead("hdb:67690"); // Refresh/reload icon
        } else {
            item = new ItemStack(Material.EMERALD);
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getConfigManager().translateColorCodes("&a&l⟲ &aRefresh"));
            
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(plugin.getConfigManager().translateColorCodes("&7Click to refresh job data"));
            lore.add(plugin.getConfigManager().translateColorCodes("&7and update your progress."));
            lore.add("");
            lore.add(plugin.getConfigManager().translateColorCodes("&e▶ Click to refresh"));
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Create help button using HeadDatabase
     */
    private ItemStack createHelpButton() {
        ItemStack item;
        
        // Try to use HeadDatabase for help icon
        if (headDatabaseAvailable) {
            item = createPlayerHead("hdb:109620"); // Question mark icon
        } else {
            item = new ItemStack(Material.BOOK);
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getConfigManager().translateColorCodes("&b&l? &bHelp & Guide"));
            
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(plugin.getConfigManager().translateColorCodes("&e&lHow to use TaskForge:"));
            lore.add(plugin.getConfigManager().translateColorCodes("&7• Click any job to view details"));
            lore.add(plugin.getConfigManager().translateColorCodes("&7• Join jobs to start earning rewards"));
            lore.add(plugin.getConfigManager().translateColorCodes("&7• Complete objectives to gain experience"));
            lore.add(plugin.getConfigManager().translateColorCodes("&7• Level up to unlock better rewards"));
            lore.add("");
            lore.add(plugin.getConfigManager().translateColorCodes("&a✓ &7Active jobs show your progress"));
            lore.add(plugin.getConfigManager().translateColorCodes("&7○ &7Available jobs can be joined"));
            lore.add("");
            lore.add(plugin.getConfigManager().translateColorCodes("&e▶ Click for more info"));
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Create close button using HeadDatabase
     */
    private ItemStack createCloseButton() {
        ItemStack item;
        
        // Try to use HeadDatabase for close icon
        if (headDatabaseAvailable) {
            item = createPlayerHead("hdb:69026"); // Red X close icon
        } else {
            item = new ItemStack(Material.BARRIER);
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getConfigManager().translateColorCodes("&c&l✕ &cClose"));
            
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(plugin.getConfigManager().translateColorCodes("&7Close the Jobs Browser"));
            lore.add(plugin.getConfigManager().translateColorCodes("&7and return to the game."));
            lore.add("");
            lore.add(plugin.getConfigManager().translateColorCodes("&e▶ Click to close"));
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Open the GUI for the player
     */
    public void open() {
        player.openInventory(inventory);
    }
    
    /**
     * Handle inventory clicks with enhanced navigation
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
        
        // Handle navigation buttons
        if (slot == 38) { // Refresh button
            setupGUI(); // Refresh the GUI
            return;
        }
        
        if (slot == 42) { // Close button
            player.closeInventory();
            return;
        }
        
        if (slot == 40) { // Help button
            // Help info is already shown in the item lore
            return;
        }
        
        // Check if player info was clicked
        if (slot == 4) {
            // Could open a player stats GUI in the future
            return;
        }
        
        // Find which job was clicked
        Job clickedJob = findJobBySlot(slot);
        if (clickedJob == null) return;
        
        // Open detailed job GUI
        new JobDetailGUI(plugin, player, clickedJob, this).open();
    }
    
    /**
     * Find job by inventory slot using the optimized grid layout
     */
    private Job findJobBySlot(int slot) {
        // Define the same job slots used in setupJobSlots - now supports 12 jobs
        int[] jobSlots = {
            10, 11, 12, 14, 15, 16,     // Top row (skip slot 13)
            19, 20, 21, 23, 24, 25      // Bottom row (skip slot 22)
        };
        
        // Find slot index
        int slotIndex = -1;
        for (int i = 0; i < jobSlots.length; i++) {
            if (jobSlots[i] == slot) {
                slotIndex = i;
                break;
            }
        }
        
        if (slotIndex == -1) return null; // Slot not found in job slots
        
        // Get job by index
        Map<String, Job> jobs = plugin.getJobManager().getAllJobs();
        int currentIndex = 0;
        
        for (Job job : jobs.values()) {
            if (currentIndex == slotIndex) {
                return job;
            }
            currentIndex++;
        }
        
        return null;
    }
    
    /**
     * Format objective description for display
     */
    private String formatObjectiveDescription(Job.ActionType actionType, Object target) {
        switch (actionType) {
            case BREAK:
                return "Break " + target.toString().toLowerCase().replace("_", " ");
            case PLACE:
                return "Place " + target.toString().toLowerCase().replace("_", " ");
            case KILL:
                return "Kill " + target.toString().toLowerCase().replace("_", " ");
            case FISH:
                return "Catch " + target.toString().toLowerCase().replace("_", " ");
            case CRAFT:
                return "Craft " + target.toString().toLowerCase().replace("_", " ");
            case ENCHANT:
                return "Enchant items";
            case BREED:
                return "Breed " + target.toString().toLowerCase().replace("_", " ");
            case TAME:
                return "Tame " + target.toString().toLowerCase().replace("_", " ");
            case SMELT:
                return "Smelt " + target.toString().toLowerCase().replace("_", " ");
            case BREW:
                return "Brew potions";
            case BRUSH:
                return "Brush suspicious blocks";
            default:
                return actionType.toString().toLowerCase().replace("_", " ") + " " + target.toString().toLowerCase().replace("_", " ");
        }
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
        Constructor<?> gameProfileConstructor = gameProfileClass.getConstructor(UUID.class, String.class);
        Object gameProfile = gameProfileConstructor.newInstance(uuid, "");
        
        // Create Property for texture
        Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
        Constructor<?> propertyConstructor = propertyClass.getConstructor(String.class, String.class);
        Object property = propertyConstructor.newInstance("textures", texture);
        
        // Add property to profile
        Method getPropertiesMethod = gameProfileClass.getMethod("getProperties");
        Object properties = getPropertiesMethod.invoke(gameProfile);
        Method putMethod = properties.getClass().getMethod("put", Object.class, Object.class);
        putMethod.invoke(properties, "textures", property);
        
        // Set profile to skull meta using reflection
        Field profileField = meta.getClass().getDeclaredField("profile");
        profileField.setAccessible(true);
        profileField.set(meta, gameProfile);
    }
}
