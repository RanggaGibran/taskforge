package id.rnggagib.taskforge.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import id.rnggagib.taskforge.jobs.Job.ActionType;
import id.rnggagib.taskforge.jobs.JobObjective;

/**
 * Professional Objectives GUI - Dedicated interface for viewing job objectives
 */
public class ObjectivesGUI implements Listener {
    
    private final TaskForgePlugin plugin;
    private final Player player;
    private final Job job;
    private final JobDetailGUI parentGUI;
    private final Inventory inventory;
    private boolean headDatabaseAvailable = false;
    
    // Pagination support
    private List<ObjectiveEntry> allObjectives;
    private int currentPage = 0;
    private final int OBJECTIVES_PER_PAGE = 28; // 4 rows * 7 slots = 28 objectives
    
    public ObjectivesGUI(TaskForgePlugin plugin, Player player, Job job, JobDetailGUI parentGUI) {
        this.plugin = plugin;
        this.player = player;
        this.job = job;
        this.parentGUI = parentGUI;
        
        // Check HeadDatabase availability
        Plugin headDbPlugin = Bukkit.getPluginManager().getPlugin("HeadDatabase");
        this.headDatabaseAvailable = headDbPlugin != null && headDbPlugin.isEnabled();
        
        // Prepare all objectives for pagination
        prepareObjectives();
        
        String title = plugin.getConfigManager().translateColorCodes(
            "&6" + job.getDisplayName() + " &8- &eObjectives &7(" + (currentPage + 1) + "/" + getTotalPages() + ")");
        
        this.inventory = Bukkit.createInventory(null, 54, title);
        
        // Register event listener
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        setupGUI();
    }
    
    /**
     * Inner class to hold objective data for pagination
     */
    private static class ObjectiveEntry {
        final ActionType actionType;
        final Object target;
        final JobObjective objective;
        
        ObjectiveEntry(ActionType actionType, Object target, JobObjective objective) {
            this.actionType = actionType;
            this.target = target;
            this.objective = objective;
        }
    }
    
    /**
     * Prepare all objectives for pagination
     */
    private void prepareObjectives() {
        allObjectives = new ArrayList<>();
        Map<ActionType, Map<Object, JobObjective>> objectives = job.getObjectives();
        
        for (ActionType actionType : ActionType.values()) {
            Map<Object, JobObjective> actionObjectives = objectives.get(actionType);
            if (actionObjectives == null || actionObjectives.isEmpty()) continue;
            
            for (Map.Entry<Object, JobObjective> entry : actionObjectives.entrySet()) {
                allObjectives.add(new ObjectiveEntry(actionType, entry.getKey(), entry.getValue()));
            }
        }
    }
    
    /**
     * Get total number of pages
     */
    private int getTotalPages() {
        return Math.max(1, (int) Math.ceil((double) allObjectives.size() / OBJECTIVES_PER_PAGE));
    }
    
    /**
     * Setup the objectives GUI
     */
    private void setupGUI() {
        // Clear inventory
        inventory.clear();
        
        // Fill background
        fillBackground();
        
        // Title item (with page info)
        setupTitle();
        
        // Display objectives for current page
        setupObjectives();
        
        // Navigation (with pagination)
        setupNavigation();
    }
    
    /**
     * Fill background with glass panes
     */
    private void fillBackground() {
        ItemStack background = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = background.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            background.setItemMeta(meta);
        }
        
        // Fill entire background
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, background);
        }
    }
    
    /**
     * Setup title section
     */
    private void setupTitle() {
        ItemStack title = new ItemStack(Material.BOOK);
        ItemMeta meta = title.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getConfigManager().translateColorCodes("&6&l" + job.getDisplayName() + " OBJECTIVES"));
            
            List<String> lore = new ArrayList<>();
            lore.add(plugin.getConfigManager().translateColorCodes("&7Complete these actions to earn experience and money"));
            lore.add(plugin.getConfigManager().translateColorCodes("&7Rewards are chance-based - not guaranteed every time"));
            lore.add("");
            lore.add(plugin.getConfigManager().translateColorCodes("&e&lPage: &f" + (currentPage + 1) + " / " + getTotalPages()));
            lore.add(plugin.getConfigManager().translateColorCodes("&e&lTotal Objectives: &f" + allObjectives.size()));
            lore.add("");
            lore.add(plugin.getConfigManager().translateColorCodes("&e&lLegend:"));
            lore.add(plugin.getConfigManager().translateColorCodes("&b⭐ Experience &8- Always given when successful"));
            lore.add(plugin.getConfigManager().translateColorCodes("&a💰 Money &8- Given based on chance"));
            lore.add(plugin.getConfigManager().translateColorCodes("&e🎲 Chance &8- Probability of getting money"));
            
            meta.setLore(lore);
            title.setItemMeta(meta);
        }
        inventory.setItem(4, title);
    }
    
    /**
     * Setup objectives display with pagination
     */
    private void setupObjectives() {
        int startIndex = currentPage * OBJECTIVES_PER_PAGE;
        int endIndex = Math.min(startIndex + OBJECTIVES_PER_PAGE, allObjectives.size());
        
        // Display area: slots 10-16, 19-25, 28-34, 37-43 (4 rows, 7 columns each)
        int[] displaySlots = {
            10, 11, 12, 13, 14, 15, 16,  // Row 1
            19, 20, 21, 22, 23, 24, 25,  // Row 2
            28, 29, 30, 31, 32, 33, 34,  // Row 3
            37, 38, 39, 40, 41, 42, 43   // Row 4
        };
        
        int slotIndex = 0;
        for (int i = startIndex; i < endIndex && slotIndex < displaySlots.length; i++, slotIndex++) {
            ObjectiveEntry entry = allObjectives.get(i);
            ItemStack objectiveItem = createObjectiveItem(entry.actionType, entry.target, entry.objective);
            inventory.setItem(displaySlots[slotIndex], objectiveItem);
        }
    }
    
    /**
     * Create objective item with action type display
     */
    private ItemStack createObjectiveItem(ActionType actionType, Object target, JobObjective objective) {
        // Determine material based on target
        Material material = getMaterialForTarget(target);
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String targetName = getTargetDisplayName(target);
            String actionIcon = getActionTypeIcon(actionType);
            meta.setDisplayName(plugin.getConfigManager().translateColorCodes("&e" + targetName + " " + actionIcon));
            
            List<String> lore = new ArrayList<>();
            lore.add(plugin.getConfigManager().translateColorCodes("&7Action: &6" + actionType.name()));
            lore.add("");
            lore.add(plugin.getConfigManager().translateColorCodes("&b⭐ Experience: &f" + String.format("%.1f", objective.getExperience())));
            lore.add(plugin.getConfigManager().translateColorCodes("&a💰 Money: &f" + objective.getMoneyDisplay()));
            lore.add(plugin.getConfigManager().translateColorCodes("&e🎲 Chance: &f" + String.format("%.1f%%", objective.getChance())));
            lore.add("");
            
            // Add chance explanation
            if (objective.getChance() >= 80) {
                lore.add(plugin.getConfigManager().translateColorCodes("&a&l✓ HIGH CHANCE"));
            } else if (objective.getChance() >= 50) {
                lore.add(plugin.getConfigManager().translateColorCodes("&e&l⚡ MEDIUM CHANCE"));
            } else if (objective.getChance() >= 20) {
                lore.add(plugin.getConfigManager().translateColorCodes("&6&l⚠ LOW CHANCE"));
            } else {
                lore.add(plugin.getConfigManager().translateColorCodes("&c&l💀 VERY LOW CHANCE"));
            }
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * Get action type icon
     */
    private String getActionTypeIcon(ActionType actionType) {
        switch (actionType) {
            case BREAK: return "&c⛏";
            case PLACE: return "&e🏗";
            case KILL: return "&c⚔";
            case BREED: return "&a🐾";
            case TAME: return "&6🦴";
            case FISH: return "&b🎣";
            case CRAFT: return "&e🔨";
            case SMELT: return "&6🔥";
            default: return "&7⭐";
        }
    }
    
    /**
     * Get material for target object
     */
    private Material getMaterialForTarget(Object target) {
        if (target instanceof Material) {
            return (Material) target;
        } else if (target instanceof String) {
            try {
                return Material.valueOf((String) target);
            } catch (IllegalArgumentException e) {
                return Material.PAPER;
            }
        } else {
            return Material.PAPER;
        }
    }
    
    /**
     * Get display name for target
     */
    private String getTargetDisplayName(Object target) {
        String name = target.toString().replace("_", " ");
        String[] words = name.split(" ");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            if (result.length() > 0) result.append(" ");
            result.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                result.append(word.substring(1).toLowerCase());
            }
        }
        
        return result.toString();
    }
    
    /**
     * Setup navigation with pagination support
     */
    private void setupNavigation() {
        // Previous page button (slot 0)
        if (currentPage > 0) {
            ItemStack prevItem;
            if (headDatabaseAvailable) {
                prevItem = createPlayerHead("hdb:8254"); // Left arrow
            } else {
                prevItem = new ItemStack(Material.ARROW);
            }
            
            ItemMeta meta = prevItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(plugin.getConfigManager().translateColorCodes("&e&l← Previous Page"));
                List<String> lore = new ArrayList<>();
                lore.add(plugin.getConfigManager().translateColorCodes("&7Go to page " + currentPage));
                lore.add("");
                lore.add(plugin.getConfigManager().translateColorCodes("&e▶ Click to go to previous page"));
                meta.setLore(lore);
                prevItem.setItemMeta(meta);
            }
            inventory.setItem(0, prevItem);
        }
        
        // Next page button (slot 8)
        if (currentPage < getTotalPages() - 1) {
            ItemStack nextItem;
            if (headDatabaseAvailable) {
                nextItem = createPlayerHead("hdb:8255"); // Right arrow
            } else {
                nextItem = new ItemStack(Material.SPECTRAL_ARROW);
            }
            
            ItemMeta meta = nextItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(plugin.getConfigManager().translateColorCodes("&e&lNext Page →"));
                List<String> lore = new ArrayList<>();
                lore.add(plugin.getConfigManager().translateColorCodes("&7Go to page " + (currentPage + 2)));
                lore.add("");
                lore.add(plugin.getConfigManager().translateColorCodes("&e▶ Click to go to next page"));
                meta.setLore(lore);
                nextItem.setItemMeta(meta);
            }
            inventory.setItem(8, nextItem);
        }
        
        // Back button (slot 45)
        ItemStack backItem;
        if (headDatabaseAvailable) {
            backItem = createPlayerHead("hdb:94736"); // Back arrow
        } else {
            backItem = new ItemStack(Material.ARROW);
        }
        
        ItemMeta meta = backItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getConfigManager().translateColorCodes("&c&l← &cBack to Job Hub"));
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(plugin.getConfigManager().translateColorCodes("&7Return to " + job.getDisplayName() + " hub"));
            lore.add(plugin.getConfigManager().translateColorCodes("&7for job management and navigation."));
            lore.add("");
            lore.add(plugin.getConfigManager().translateColorCodes("&e▶ Click to go back"));
            meta.setLore(lore);
            backItem.setItemMeta(meta);
        }
        inventory.setItem(45, backItem);
        
        // Close button (slot 49)
        ItemStack closeItem;
        if (headDatabaseAvailable) {
            closeItem = createPlayerHead("hdb:69025"); // Close X icon
        } else {
            closeItem = new ItemStack(Material.BARRIER);
        }
        
        meta = closeItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getConfigManager().translateColorCodes("&c&l✕ &cClose"));
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(plugin.getConfigManager().translateColorCodes("&7Close this menu and return"));
            lore.add(plugin.getConfigManager().translateColorCodes("&7to the game world."));
            lore.add("");
            lore.add(plugin.getConfigManager().translateColorCodes("&e▶ Click to close"));
            meta.setLore(lore);
            closeItem.setItemMeta(meta);
        }
        inventory.setItem(49, closeItem);
        
        // Stats button (slot 46)
        ItemStack statsItem = new ItemStack(Material.GOLD_INGOT);
        ItemMeta statsMeta = statsItem.getItemMeta();
        if (statsMeta != null) {
            statsMeta.setDisplayName(plugin.getConfigManager().translateColorCodes("&6&l📊 Your Statistics"));
            
            List<String> lore = new ArrayList<>();
            if (plugin.getPlayerDataManager().hasJob(player.getUniqueId(), job.getName())) {
                int level = plugin.getPlayerDataManager().getJobLevel(player.getUniqueId(), job.getName());
                double exp = plugin.getPlayerDataManager().getJobExperience(player.getUniqueId(), job.getName());
                double progress = plugin.getPlayerDataManager().getLevelProgress(player.getUniqueId(), job.getName());
                
                lore.add(plugin.getConfigManager().translateColorCodes("&7Current Level: &e" + level));
                lore.add(plugin.getConfigManager().translateColorCodes("&7Experience: &b" + String.format("%.1f", exp)));
                lore.add(plugin.getConfigManager().translateColorCodes("&7Progress: &a" + String.format("%.1f%%", progress * 100)));
            } else {
                lore.add(plugin.getConfigManager().translateColorCodes("&7You haven't joined this job yet"));
                lore.add(plugin.getConfigManager().translateColorCodes("&7Join to start tracking statistics"));
            }
            
            statsMeta.setLore(lore);
            statsItem.setItemMeta(statsMeta);
        }
        inventory.setItem(46, statsItem);
        
        // Level rewards button (slot 53)
        ItemStack rewardsItem;
        if (headDatabaseAvailable) {
            rewardsItem = createPlayerHead("hdb:66374"); // Level rewards icon
        } else {
            rewardsItem = new ItemStack(Material.CHEST);
        }
        
        ItemMeta rewardsMeta = rewardsItem.getItemMeta();
        if (rewardsMeta != null) {
            rewardsMeta.setDisplayName(plugin.getConfigManager().translateColorCodes("&6&l🏆 Level Rewards"));
            List<String> lore = new ArrayList<>();
            lore.add(plugin.getConfigManager().translateColorCodes("&7Click to view all level rewards"));
            lore.add(plugin.getConfigManager().translateColorCodes("&7See what you get for leveling up!"));
            lore.add("");
            lore.add(plugin.getConfigManager().translateColorCodes("&e▶ Click to view rewards"));
            rewardsMeta.setLore(lore);
            rewardsItem.setItemMeta(rewardsMeta);
        }
        inventory.setItem(53, rewardsItem);
    }
    
    /**
     * Handle inventory clicks with pagination support
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        event.setCancelled(true); // Prevent item movement
        
        Player clicker = (Player) event.getWhoClicked();
        if (!clicker.equals(player)) return;
        
        int slot = event.getSlot();
        
        switch (slot) {
            case 0: // Previous page
                if (currentPage > 0) {
                    currentPage--;
                    setupGUI();
                }
                break;
            case 8: // Next page
                if (currentPage < getTotalPages() - 1) {
                    currentPage++;
                    setupGUI();
                }
                break;
            case 45: // Back button
                player.closeInventory();
                if (parentGUI != null) {
                    parentGUI.open();
                }
                break;
            case 49: // Close button
                player.closeInventory();
                break;
            case 53: // Level rewards button
                player.closeInventory();
                new LevelRewardsGUI(plugin, player, job, parentGUI).open();
                break;
        }
    }
    
    /**
     * Open the GUI for the player
     */
    public void open() {
        player.openInventory(inventory);
    }
    
    /**
     * Navigate to a specific page
     */
    public void goToPage(int page) {
        if (page >= 0 && page < getTotalPages()) {
            this.currentPage = page;
            setupGUI();
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
