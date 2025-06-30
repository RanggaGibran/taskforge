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
    
    public ObjectivesGUI(TaskForgePlugin plugin, Player player, Job job, JobDetailGUI parentGUI) {
        this.plugin = plugin;
        this.player = player;
        this.job = job;
        this.parentGUI = parentGUI;
        
        // Check HeadDatabase availability
        Plugin headDbPlugin = Bukkit.getPluginManager().getPlugin("HeadDatabase");
        this.headDatabaseAvailable = headDbPlugin != null && headDbPlugin.isEnabled();
        
        String title = plugin.getConfigManager().translateColorCodes(
            "&6" + job.getDisplayName() + " &8- &eObjectives");
        
        this.inventory = Bukkit.createInventory(null, 54, title);
        
        // Register event listener
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        setupGUI();
    }
    
    /**
     * Setup the objectives GUI
     */
    private void setupGUI() {
        // Clear inventory
        inventory.clear();
        
        // Fill background
        fillBackground();
        
        // Title item
        setupTitle();
        
        // Display objectives by action type
        setupObjectives();
        
        // Navigation
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
     * Setup objectives display
     */
    private void setupObjectives() {
        Map<ActionType, Map<Object, JobObjective>> objectives = job.getObjectives();
        
        int startSlot = 10;
        int currentSlot = startSlot;
        
        for (ActionType actionType : ActionType.values()) {
            Map<Object, JobObjective> actionObjectives = objectives.get(actionType);
            if (actionObjectives == null || actionObjectives.isEmpty()) continue;
            
            // Action type header
            ItemStack header = createActionTypeHeader(actionType, actionObjectives.size());
            inventory.setItem(currentSlot, header);
            currentSlot++;
            
            // Show objectives for this action type (limit to show nicely)
            int objectiveCount = 0;
            for (Map.Entry<Object, JobObjective> entry : actionObjectives.entrySet()) {
                if (objectiveCount >= 7) break; // Limit per row
                if (currentSlot >= 44) break; // Don't go beyond visible area
                
                Object target = entry.getKey();
                JobObjective objective = entry.getValue();
                
                ItemStack objectiveItem = createObjectiveItem(actionType, target, objective);
                inventory.setItem(currentSlot, objectiveItem);
                currentSlot++;
                objectiveCount++;
            }
            
            // Move to next row if we have more action types
            if (currentSlot % 9 != 1) {
                currentSlot = ((currentSlot / 9) + 1) * 9 + 1; // Next row, slot 1
            }
            
            if (currentSlot >= 45) break; // Stop if we're running out of space
        }
    }
    
    /**
     * Create action type header item
     */
    private ItemStack createActionTypeHeader(ActionType actionType, int objectiveCount) {
        Material material;
        switch (actionType) {
            case BREAK: material = Material.DIAMOND_PICKAXE; break;
            case PLACE: material = Material.STONE_BRICKS; break;
            case KILL: material = Material.DIAMOND_SWORD; break;
            case BREED: material = Material.WHEAT; break;
            case TAME: material = Material.BONE; break;
            case FISH: material = Material.FISHING_ROD; break;
            case CRAFT: material = Material.CRAFTING_TABLE; break;
            case SMELT: material = Material.FURNACE; break;
            default: material = Material.EMERALD;
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getConfigManager().translateColorCodes("&6&l" + actionType.name() + " ACTIONS"));
            
            List<String> lore = new ArrayList<>();
            lore.add(plugin.getConfigManager().translateColorCodes("&7" + objectiveCount + " objectives available"));
            lore.add("");
            
            String description = getActionTypeDescription(actionType);
            lore.add(plugin.getConfigManager().translateColorCodes("&e" + description));
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * Get description for action type
     */
    private String getActionTypeDescription(ActionType actionType) {
        switch (actionType) {
            case BREAK: return "Break blocks to earn rewards";
            case PLACE: return "Place blocks to earn rewards";
            case KILL: return "Kill entities to earn rewards";
            case BREED: return "Breed animals to earn rewards";
            case TAME: return "Tame animals to earn rewards";
            case FISH: return "Catch fish to earn rewards";
            case CRAFT: return "Craft items to earn rewards";
            case SMELT: return "Smelt items to earn rewards";
            default: return "Perform actions to earn rewards";
        }
    }
    
    /**
     * Create objective item
     */
    private ItemStack createObjectiveItem(ActionType actionType, Object target, JobObjective objective) {
        // Determine material based on target
        Material material = getMaterialForTarget(target);
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String targetName = getTargetDisplayName(target);
            meta.setDisplayName(plugin.getConfigManager().translateColorCodes("&e" + targetName));
            
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
     * Setup navigation with HeadDatabase support
     */
    private void setupNavigation() {
        // Back button with HeadDatabase
        ItemStack backItem;
        if (headDatabaseAvailable) {
            backItem = createPlayerHead("hdb:94738"); // Back arrow as requested
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
        inventory.setItem(38, backItem);
        
        // Close button with HeadDatabase
        ItemStack closeItem;
        if (headDatabaseAvailable) {
            closeItem = createPlayerHead("hdb:11834"); // Close X icon
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
        inventory.setItem(42, closeItem);
        
        // Stats button
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
        inventory.setItem(49, statsItem);
        
        // Level rewards button with HeadDatabase
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
        
        int slot = event.getSlot();
        
        switch (slot) {
            case 38: // Back button
                player.closeInventory();
                if (parentGUI != null) {
                    parentGUI.open();
                }
                break;
            case 42: // Close button
                player.closeInventory();
                break;
            case 53: // Level rewards button
                player.closeInventory();
                new LevelRewardsGUI(plugin, player, job, parentGUI).open();
                break;
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
